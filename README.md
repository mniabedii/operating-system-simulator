# Concurrent Operating System Simulator

**A tick-driven, multithreaded operating-system simulator written in Java.**

Simulates CPU scheduling, virtual memory, TLB behavior, page faults, disk I/O, resource allocation, and deadlock recovery in a single coordinated environment.

![Java](https://img.shields.io/badge/Java-Concurrent%20Simulation-orange?logo=openjdk\&logoColor=white)
![Operating Systems](https://img.shields.io/badge/Operating%20Systems-Simulator-blue)
![Architecture](https://img.shields.io/badge/Architecture-Tick--Driven-success)


---

## Overview

This project is a conceptual operating-system simulator designed to model the interaction between several core OS subsystems. It is **not a real kernel**; instead, it provides an observable simulation of process execution, scheduling, memory management, disk activity, synchronization, and deadlock handling.

The simulator is driven by a logical system clock and uses multiple real Java threads. The CPU scheduler is the only component allowed to advance time, while the remaining subsystems coordinate their work around the same clock.

## Key Features

* **Four concurrent Java threads** for process generation, memory admission, CPU scheduling, and disk I/O
* **Tick-driven simulation** with one authoritative logical clock
* **Multilevel Queue scheduling**

  * System → FCFS
  * Interactive → Round Robin
  * Background → SRTF
* **Fixed-priority queue selection** with Aging to reduce starvation
* **Configurable context-switch cost** and Round Robin quantum
* **Virtual memory simulation** using paging and per-process page tables
* **FIFO TLB** with hit/miss tracking
* **Demand paging** with page faults and simulated Disk I/O
* **FIFO and Random page-replacement policies**
* **Dirty-page tracking** and conceptual write-back on eviction
* **Resource allocation** using maximum-demand and allocation vectors
* **Deadlock detection and recovery** through deterministic victim termination
* **Detailed per-tick status output** and a final simulation report

## Architecture

The system is built around four main worker threads:

```text
ProcessGenerator
      |
      v
 ProcessBuffer
      |
      v
MemoryManager -----> ReadyQueue
                         |
                         v
                    CPUScheduler
                     /        \
                    v          v
                  MMU     ResourceManager
                    |
             Page Fault
                    |
                    v
              PageFaultQueue
                    |
                    v
                  DiskIO
                    |
                    +-----> ReadyQueue
```

| Component          | Responsibility                                                                                    |
| ------------------ | ------------------------------------------------------------------------------------------------- |
| `ProcessGenerator` | Creates randomized PCBs and places them in the shared input buffer.                               |
| `MemoryManager`    | Creates page tables, registers processes, and moves them to `READY`.                              |
| `CPUScheduler`     | Advances the clock, dispatches processes, applies scheduling policies, and coordinates execution. |
| `DiskIO`           | Handles page-fault requests, page loading, replacement, and process wake-up.                      |
| `MMU`              | Performs TLB/page-table translation and detects page faults.                                      |
| `ResourceManager`  | Tracks available resources, allocations, and remaining process needs.                             |
| `DeadlockDetector` | Detects deadlocked processes and supports recovery.                                               |

## Scheduling Model

The scheduler uses a fixed-priority Multilevel Queue:

```text
SYSTEM > INTERACTIVE > BACKGROUND
```

### System — FCFS

System-level processes are handled in FIFO order.

### Interactive — Round Robin

Interactive-level processes use Round Robin with a default quantum of **3 CPU ticks**.

### Background — SRTF

Background processes are ordered by shortest remaining burst time. A running Background process can be preempted when a higher-priority process becomes ready or when a shorter Background process appears.

### Aging

Processes waiting for too long are promoted through the scheduling levels:

```text
BACKGROUND -> INTERACTIVE -> SYSTEM
```

This reduces starvation while preserving the original process type stored in the PCB.

## Memory and Disk I/O

Each process receives its own page table and generated page-reference string. Memory access follows this path:

```text
CPU -> MMU -> TLB -> Page Table -> Physical Memory
```

A TLB hit adds no timing penalty. A TLB miss adds one logical tick before the page table is checked.

If the requested page is not resident:

1. the process moves to `WAITING`;
2. a page-fault request is submitted to the Disk thread;
3. Disk I/O takes **5 logical ticks**;
4. a free frame is used or a page-replacement victim is selected;
5. relevant page-table and TLB entries are updated;
6. the blocked process returns to the appropriate ready queue.

The simulator currently supports **FIFO** and **Random** page replacement.

## Resource Management and Deadlocks

The simulated system contains three resource types with the default total vector:

```text
R1 = 2
R2 = 1
R3 = 3
```

Each process is assigned a maximum-demand vector. During execution, valid resource requests may be generated based on its remaining need.

When a resource request cannot be satisfied immediately, the process enters a resource-wait state while retaining resources it already holds. This makes real hold-and-wait deadlock scenarios possible.

Deadlock detection uses `Work`, `Allocation`, `Request`, and `Finish` structures. When deadlock is detected, one victim is terminated, its resources and memory are released, and waiting requests are retried.

## Project Structure

```text
src/com/github/mniabedii/
├── Main.java
├── buffer/
│   └── ProcessBuffer.java
├── clock/
│   └── SimulationClock.java
├── config/
│   └── SimulationConfig.java
├── disk/
│   ├── DiskIO.java
│   ├── PageFaultQueue.java
│   └── PageFaultRequest.java
├── generator/
│   └── ProcessGenerator.java
├── memory/
│   ├── MMU.java
│   ├── MemoryManager.java
│   ├── PhysicalMemory.java
│   ├── PageTable.java
│   ├── TLB.java
│   └── ...
├── process/
│   ├── PCB.java
│   ├── ProcessState.java
│   ├── ProcessType.java
│   └── ...
├── resource/
│   ├── ResourceManager.java
│   ├── DeadlockDetector.java
│   └── ...
└── scheduler/
    ├── CPUScheduler.java
    └── ReadyQueue.java
```

## Default Configuration

The main simulation parameters are centralized in `SimulationConfig`.

| Parameter           |     Default |
| ------------------- | ----------: |
| Maximum processes   |          15 |
| Physical frames     |          12 |
| TLB capacity        |           3 |
| Round Robin quantum |     3 ticks |
| Context-switch cost |     2 ticks |
| Disk page-load cost |     5 ticks |
| Process pages       |        6–12 |
| Total resources     | `[2, 1, 3]` |
| Page replacement    |        FIFO |

Other values such as the Aging threshold, generation interval, burst range, and resource-request probability are configurable in the same class.

## Build and Run

No external dependencies or build framework are required.

### Compile

```bash
rm -rf out
mkdir out
javac -d out $(find src -name "*.java")
```

### Run

```bash
java -cp out com.github.mniabedii.Main
```

### Save the simulation log

```bash
java -cp out com.github.mniabedii.Main 2>&1 | tee simulation-output.log
```

## Simulation Output

During execution, the simulator reports the state of the system at each logical tick, including:

* CPU activity and running process
* ready queues
* physical-memory usage
* TLB hits, misses, and hit rate
* page faults and Disk I/O status
* resource availability and blocked requests
* deadlock detections and recovery victims
* context switches, preemptions, completed processes, and CPU utilization

After all threads terminate, a final report verifies that processes have completed and that memory, disk work, ready queues, and resources have been cleanly released.

## Documentation

For a deeper explanation of the implementation and the mapping between requirements and source code, see:

* [Implementation Details](docs/IMPLEMENTATION_DETAILS.md)

---

> This repository is an academic simulation of operating-system concepts and is intended for learning and experimentation rather than production use.
