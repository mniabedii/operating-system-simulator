# Operating System Simulator — Implementation Details

This document explains how the Java project implements the required concurrent operating-system simulator. It is intentionally concise and organized by subsystem so that a reader can understand the design without reading every source file first.

## 1. Project Architecture

The simulator is tick-driven and uses four real Java threads:

| Thread | Responsibility |
|---|---|
| `ProcessGenerator` | Creates random processes and places them in the shared input buffer. |
| `MemoryManager` | Creates page tables, registers processes, and moves them from `NEW` to `READY`. |
| `CPUScheduler` | Selects and runs processes, applies scheduling policies, and advances the logical clock. |
| `DiskIO` | Handles page faults, page loading, page replacement, and waking blocked processes. |

Only `CPUScheduler` calls `SimulationClock.advanceOneTick()`. Other threads wait for the logical clock and react to shared state. This guarantees one authoritative system clock.

Main package:

```text
src/com/github/mniabedii/
```

`Main.java` creates the shared objects, starts the four threads, waits for them with `join()`, and prints the final report.

## 2. Configuration

`SimulationConfig` contains the values that control the simulation, including:

- maximum process count;
- random generation interval;
- burst-time and priority ranges;
- 12 physical frames;
- TLB capacity of 3;
- Round Robin quantum of 3 ticks;
- context-switch cost of 2 ticks;
- Disk page-loading cost of 5 ticks;
- Aging threshold;
- resource totals `[2, 1, 3]`;
- page-replacement policy;
- runtime resource-request probability.

Values not fixed by the specification, such as the Aging threshold and resource-request probability, are kept configurable instead of being hard-coded in scheduling logic.

## 3. Process Model

`PCB` represents one simulated process. It stores:

- PID and original `ProcessType`;
- arrival time, priority, total burst, and remaining burst;
- process state: `NEW`, `READY`, `RUNNING`, `WAITING`, or `TERMINATED`;
- current `SchedulingLevel`;
- required page count and page-reference string;
- page table;
- maximum resource-demand vector;
- ready-queue Aging counter;
- optional waiting reason such as page fault or resource request.

`ProcessType` never changes. `SchedulingLevel` may change because of Aging. For example, a process can remain a `BACKGROUND` process while being promoted to the `INTERACTIVE` scheduling level.

A successful CPU burst calls `executeOneTick()`, which decreases the remaining burst by one. A page fault does not decrease the burst, so the same page reference is retried after Disk I/O.

## 4. Process Generation and Input Buffer

`ProcessGenerator` creates a limited number of PCBs at random logical intervals. The approximate type distribution is:

- 20% System;
- 40% Interactive;
- 40% Background.

Each PCB receives random burst time, priority, required pages, page-reference string, and maximum resource demand. New processes begin in `NEW` and enter `ProcessBuffer` through `putOnBuffer(PCB)`.

`ProcessBuffer` is a bounded synchronized producer-consumer buffer. It uses `wait()` and `notifyAll()` so the Generator waits when the buffer is full and the Memory Manager waits when it is empty. The buffer is closed after generation finishes, allowing the consumer thread to terminate cleanly.

## 5. Admission and Ready Queues

`MemoryManager` removes PCBs through `takeFromBuffer()`. For each process it:

1. creates a `PageTable` with one entry per virtual page;
2. attaches the page table to the PCB;
3. registers the process in `ResourceManager`;
4. changes the process state to `READY`;
5. inserts it into `ReadyQueue`.

`ReadyQueue` is a multilevel queue containing:

- System: FIFO `ArrayDeque`;
- Interactive: FIFO `ArrayDeque`;
- Background: `PriorityQueue` ordered by remaining burst, arrival time, then PID.

All queue methods are synchronized because the Memory Manager, Disk thread, and CPU Scheduler can access the queues concurrently.

## 6. CPU Scheduling

`CPUScheduler` implements fixed-priority MLQ selection:

```text
SYSTEM > INTERACTIVE > BACKGROUND
```

The first nonempty level is selected.

### System — FCFS

System processes use FIFO order and are not preempted by later System arrivals.

### Interactive — Round Robin

Interactive-level processes receive a configurable quantum, normally 3 successful CPU ticks. Context-switch ticks, TLB-miss penalties, and Disk waiting do not consume the quantum.

When the quantum expires, the process returns to the end of the Interactive queue if another System or Interactive process is ready. Otherwise, it continues with a fresh quantum to avoid a context switch from a process back to itself.

### Background — SRTF

Background processes are ordered by shortest remaining burst. A running Background process is preempted when:

- a System process becomes ready;
- an Interactive process becomes ready;
- a shorter Background process becomes ready.

Equal remaining times do not cause preemption because that would add context-switch cost without selecting a shorter job.

### Context Switching

Each dispatch consumes the configured context-switch cost, normally 2 ticks. No process executes during those ticks.

### Aging

Every logical tick increments the waiting counter of PCBs currently in a ready queue. When the configured threshold is reached:

```text
BACKGROUND -> INTERACTIVE -> SYSTEM
```

A process can move only one level during one tick. Promotion is permanent for the remainder of its lifetime.

## 7. Paging, Page Tables, and Physical Memory

Each process has a `PageTable` containing `PageTableEntry` objects. An entry records whether the page is present, its frame number, and whether it is dirty.

`PhysicalMemory` contains a fixed list of `Frame` objects. A frame records the owning PCB and virtual page number. This owner reference allows replacement code to update the victim process's page table correctly.

On normal termination or deadlock recovery, all frames owned by the process are released.

## 8. TLB and MMU

`TLB` stores translations as `(PID, page, frame)` and has limited capacity. Replacement inside the TLB uses FIFO.

`MMU.translate()` performs:

1. TLB lookup;
2. page-table lookup after a TLB miss;
3. page-fault creation if the page is absent.

Timing behavior:

- TLB hit: no additional tick;
- TLB miss: one penalty tick;
- successful memory access: the process may then execute one CPU burst tick;
- absent page: the process enters `WAITING` and does not consume a burst tick.

The MMU also counts TLB hits, misses, and page faults. Stale translations are invalidated when pages are replaced or processes terminate.

## 9. Page Faults and Disk I/O

A page fault creates a `PageFaultRequest` and places it in `PageFaultQueue`. The process changes from `RUNNING` to `WAITING` with wait reason `PAGE_FAULT`.

`DiskIO` removes requests and waits until five logical ticks have elapsed. It then:

1. loads the page into a free frame, or selects a replacement victim;
2. updates the victim and incoming page-table entries;
3. invalidates the victim's TLB translation;
4. reports dirty-page write-back when necessary;
5. changes the blocked process to `READY`;
6. returns it to the correct ready queue.

`PageFaultQueue` tracks both queued and active requests so the simulator does not terminate while the Disk thread is still processing an operation.

## 10. Page Replacement

The simulator implements two selectable policies:

- FIFO;
- Random.

FIFO tracks frame load order and replaces the oldest resident page. Random selects one occupied frame. After replacement, the reused frame becomes a newly loaded frame for future FIFO decisions.

If the victim page is dirty, the simulator records a conceptual write-back before eviction. The specification gives five ticks for page loading but no separate dirty-write duration, so dirty write-back is reported without inventing an additional timing constant.

## 11. Resource Management

`ResourceManager` stores:

- total resource vector;
- available resource vector;
- registered processes;
- current allocation vector for every process.

For each process:

```text
Remaining Need = Maximum Demand - Current Allocation
```

A request is granted only when it is within the process's remaining need and the requested instances are currently available. This is detection-based resource management, not Banker's avoidance algorithm.

During execution, the Scheduler occasionally creates a valid request for one unit of a resource the process still needs. The result is:

- `GRANTED`: allocation and available vectors are updated atomically;
- `NOT_AVAILABLE`: the process enters `WAITING/RESOURCE` and its request enters `ResourceWaitQueue`;
- `EXCEEDS_MAXIMUM`: treated as an implementation error because generated requests use remaining need.

Blocked processes keep resources they already own, allowing hold-and-wait and real deadlocks to occur.

## 12. Deadlock Detection and Recovery

`DeadlockDetector` uses the standard detection structure:

- `Work` starts as the available-resource vector;
- `Allocation` contains resources held by each process;
- `Request` contains each blocked process's outstanding request;
- `Finish` records whether a process can theoretically complete.

A process whose request is less than or equal to `Work` can finish and release its allocation into `Work`. When no further progress is possible, unfinished resource-holding processes are deadlocked.

Recovery terminates one victim. The deterministic victim policy prefers:

1. the process holding the most resource instances;
2. then the largest remaining burst;
3. then the highest PID.

Recovery removes the victim's pending request, invalidates its TLB entries, releases its frames and resources, marks it `TERMINATED`, and retries the remaining resource waiters.

## 13. Synchronization

Shared mutable structures use Java monitor synchronization:

- `ProcessBuffer`;
- `ReadyQueue`;
- `PageFaultQueue`;
- `ResourceWaitQueue`;
- `ResourceManager`;
- physical memory and TLB operations;
- mutable PCB operations where atomic visibility is needed.

`wait()` and `notifyAll()` are used for blocking producer-consumer behavior. `volatile` is used for cross-thread lifecycle and state visibility where appropriate.

## 14. Per-Tick Output

Every call that advances the logical clock also prints the required system status:

- CPU activity;
- System, Interactive, and Background ready queues;
- current running process;
- memory/frame usage;
- TLB hit/miss counters and rates;
- page-fault count;
- Disk queue/activity/completed operations;
- resource availability and resource waiters;
- deadlock detections and victims;
- scheduling statistics such as quantum usage, context switches, preemptions, and completed processes.

`Main` prints a separate final summary after all four threads terminate.

## 15. Termination Conditions

The CPU Scheduler ends the simulation only when:

- all generated processes are terminated, including recovery victims;
- the Generator and Memory Manager have finished;
- no process is running;
- all ready queues are empty;
- the resource wait queue is empty;
- no queued or active page-fault request remains;
- the Disk thread is not busy.

Normal termination and deadlock recovery both release memory, TLB entries, and resources. A clean final state should show all frames free and resources restored to `[2, 1, 3]`.

## 16. Build, Run, and Capture Output

Compile:

```bash
rm -rf out
mkdir out
javac -d out $(find src -name "*.java")
```

Run normally:

```bash
java -cp out com.github.mniabedii.Main
```

Print to the terminal **and** save the complete output to a file:

```bash
java -cp out com.github.mniabedii.Main 2>&1 | tee simulation-output.log
```

Append another run instead of replacing the file:

```bash
java -cp out com.github.mniabedii.Main 2>&1 | tee -a simulation-output.log
```

PowerShell equivalent:

```powershell
java -cp out com.github.mniabedii.Main 2>&1 |
    Tee-Object -FilePath simulation-output.log
```

The generated `simulation-output.log` can be uploaded for verification.

## 17. Requirement Mapping

| Specification requirement | Implementation |
|---|---|
| At least four Java threads | Generator, Memory Manager, CPU Scheduler, Disk I/O |
| Tick-driven clock | `SimulationClock`; advanced only by `CPUScheduler` |
| PCB and process states | `PCB`, `ProcessState`, `WaitReason` |
| MLQ scheduling | `ReadyQueue`, `CPUScheduler` |
| FCFS / RR / SRTF | Per-level scheduling logic |
| Fixed queue priority and Aging | `determinePreemptionReason()`, `applyAging()` |
| Paging and MMU | `PageTable`, `PhysicalMemory`, `MMU` |
| Limited FIFO TLB | `TLB` |
| Page faults and five-tick Disk I/O | `PageFaultQueue`, `DiskIO` |
| Two page-replacement algorithms | FIFO and Random |
| Resource allocation | `ResourceManager` |
| Deadlock detection and recovery | `DeadlockDetector`, victim termination |
| Per-tick status output | `CPUScheduler.printTickStatus()` |
| Clean termination | Scheduler termination predicate and thread joins |
