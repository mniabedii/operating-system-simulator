package com.github.mniabedii;

import com.github.mniabedii.buffer.ProcessBuffer;
import com.github.mniabedii.clock.SimulationClock;
import com.github.mniabedii.config.SimulationConfig;
import com.github.mniabedii.disk.DiskIO;
import com.github.mniabedii.disk.PageFaultQueue;
import com.github.mniabedii.generator.ProcessGenerator;
import com.github.mniabedii.memory.MMU;
import com.github.mniabedii.memory.MemoryManager;
import com.github.mniabedii.memory.PhysicalMemory;
import com.github.mniabedii.memory.TLB;
import com.github.mniabedii.resource.ResourceManager;
import com.github.mniabedii.resource.ResourceWaitQueue;
import com.github.mniabedii.scheduler.CPUScheduler;
import com.github.mniabedii.scheduler.ReadyQueue;

import java.util.Arrays;

public class Main {

        public static void main(String[] args)
                        throws InterruptedException {

                SimulationClock clock = new SimulationClock();

                ProcessBuffer processBuffer = new ProcessBuffer(
                                SimulationConfig.PROCESS_BUFFER_CAPACITY);

                ReadyQueue readyQueues = new ReadyQueue();

                /*
                 * Memory and paging.
                 */
                PhysicalMemory physicalMemory = new PhysicalMemory(
                                SimulationConfig.FRAME_COUNT,
                                SimulationConfig.PAGE_REPLACEMENT_POLICY);

                TLB tlb = new TLB(
                                SimulationConfig.TLB_CAPACITY);

                MMU mmu = new MMU(tlb);

                PageFaultQueue pageFaultQueue = new PageFaultQueue();

                /*
                 * Resource management.
                 */
                ResourceManager resourceManager = new ResourceManager(
                                SimulationConfig.getTotalResources());

                ResourceWaitQueue resourceWaitQueue = new ResourceWaitQueue();

                /*
                 * Runnable operating-system components.
                 */
                ProcessGenerator generator = new ProcessGenerator(
                                processBuffer,
                                clock,
                                SimulationConfig.MAX_PROCESSES);

                MemoryManager memoryManager = new MemoryManager(
                                processBuffer,
                                readyQueues,
                                clock,
                                resourceManager);

                DiskIO diskIO = new DiskIO(
                                pageFaultQueue,
                                physicalMemory,
                                readyQueues,
                                clock,
                                mmu);

                CPUScheduler scheduler = new CPUScheduler(
                                clock,
                                readyQueues,
                                mmu,
                                physicalMemory,
                                tlb,
                                pageFaultQueue,
                                generator,
                                memoryManager,
                                diskIO,
                                SimulationConfig.MAX_PROCESSES,
                                resourceManager,
                                resourceWaitQueue);

                /*
                 * Four real Java threads.
                 */
                Thread generatorThread = new Thread(
                                generator,
                                "process-generator");

                Thread memoryManagerThread = new Thread(
                                memoryManager,
                                "memory-manager");

                Thread diskThread = new Thread(
                                diskIO,
                                "disk-io");

                Thread schedulerThread = new Thread(
                                scheduler,
                                "cpu-scheduler");

                printStartupConfiguration();

                /*
                 * Start consumers before producers.
                 */
                diskThread.start();
                memoryManagerThread.start();
                generatorThread.start();
                schedulerThread.start();

                /*
                 * The Scheduler determines when the simulation ends.
                 */
                schedulerThread.join();

                generatorThread.join();
                memoryManagerThread.join();
                diskThread.join();

                printFinalReport(
                                clock,
                                generator,
                                memoryManager,
                                scheduler,
                                diskIO,
                                readyQueues,
                                pageFaultQueue,
                                resourceWaitQueue,
                                physicalMemory,
                                tlb,
                                mmu,
                                resourceManager);
        }

        private static void printStartupConfiguration() {
                System.out.println(
                                "========================================");

                System.out.println(
                                " SIMPLE OPERATING SYSTEM SIMULATION");

                System.out.println(
                                "========================================");

                System.out.println(
                                "Processes: "
                                                + SimulationConfig.MAX_PROCESSES);

                System.out.println(
                                "Physical frames: "
                                                + SimulationConfig.FRAME_COUNT);

                System.out.println(
                                "TLB capacity: "
                                                + SimulationConfig.TLB_CAPACITY);

                System.out.println(
                                "Page replacement: "
                                                + SimulationConfig.PAGE_REPLACEMENT_POLICY);

                System.out.println(
                                "Round Robin quantum: "
                                                + SimulationConfig.ROUND_ROBIN_QUANTUM);

                System.out.println(
                                "Context-switch cost: "
                                                + SimulationConfig.CONTEXT_SWITCH_TICKS);

                System.out.println(
                                "Disk I/O cost: "
                                                + SimulationConfig.DISK_IO_TICKS);

                System.out.println(
                                "Aging threshold: "
                                                + SimulationConfig.AGING_THRESHOLD_TICKS);

                System.out.println(
                                "Resources: "
                                                + Arrays.toString(
                                                                SimulationConfig.getTotalResources()));

                System.out.println(
                                "========================================");

                System.out.println();
        }

        private static void printFinalReport(
                        SimulationClock clock,
                        ProcessGenerator generator,
                        MemoryManager memoryManager,
                        CPUScheduler scheduler,
                        DiskIO diskIO,
                        ReadyQueue readyQueues,
                        PageFaultQueue pageFaultQueue,
                        ResourceWaitQueue resourceWaitQueue,
                        PhysicalMemory physicalMemory,
                        TLB tlb,
                        MMU mmu,
                        ResourceManager resourceManager) {

                int terminated = scheduler.getTerminatedProcessCount();

                int deadlockVictims = scheduler.getDeadlockVictimCount();

                int normalTerminations = terminated - deadlockVictims;

                boolean allFramesReleased = physicalMemory.getFreeFrameCount() == physicalMemory.getFrameCount();

                boolean allResourcesReleased = Arrays.equals(
                                resourceManager.getAvailableResources(),
                                SimulationConfig.getTotalResources());

                System.out.println();
                System.out.println(
                                "========================================");

                System.out.println(
                                " FINAL SIMULATION REPORT");

                System.out.println(
                                "========================================");

                System.out.println();
                System.out.println("--- General ---");

                System.out.println(
                                "Final logical tick: "
                                                + clock.getCurrentTick());

                System.out.println(
                                "Terminated processes: "
                                                + terminated
                                                + "/"
                                                + SimulationConfig.MAX_PROCESSES);

                System.out.println(
                                "Normal terminations: "
                                                + normalTerminations);

                System.out.println(
                                "Deadlock victims: "
                                                + deadlockVictims);

                System.out.println();
                System.out.println("--- Scheduling ---");

                System.out.println(
                                "Context switches: "
                                                + scheduler.getContextSwitchCount());

                System.out.println(
                                "Preemptions: "
                                                + scheduler.getPreemptionCount());

                System.out.println();
                System.out.println("--- Memory and Disk ---");

                System.out.println(
                                "Page faults: "
                                                + mmu.getPageFaultCount());

                System.out.println(
                                "TLB hits: "
                                                + tlb.getHitCount());

                System.out.println(
                                "TLB misses: "
                                                + tlb.getMissCount());

                System.out.printf(
                                "TLB hit rate: %.2f%%%n",
                                tlb.getHitRate() * 100.0);

                System.out.printf(
                                "Free frames: %d/%d%n",
                                physicalMemory.getFreeFrameCount(),
                                physicalMemory.getFrameCount());

                System.out.println();
                System.out.println("--- Resources and Deadlocks ---");

                System.out.println(
                                "Resource requests: "
                                                + scheduler.getResourceRequestCount());

                System.out.println(
                                "Resource grants: "
                                                + scheduler.getResourceGrantCount());

                System.out.println(
                                "Resource blocks: "
                                                + scheduler.getResourceBlockCount());

                System.out.println(
                                "Deadlocks detected: "
                                                + scheduler.getDeadlockCount());

                System.out.println(
                                "Deadlock victims: "
                                                + scheduler.getDeadlockVictimCount());

                System.out.println(
                                "Final available resources: "
                                                + Arrays.toString(
                                                                resourceManager
                                                                                .getAvailableResources()));

                System.out.println();
                System.out.println("--- Final State Checks ---");

                System.out.println(
                                "Generator finished: "
                                                + generator.isFinished());

                System.out.println(
                                "Memory Manager finished: "
                                                + memoryManager.isFinished());

                System.out.println(
                                "Scheduler finished: "
                                                + scheduler.isFinished());

                System.out.println(
                                "Disk finished: "
                                                + diskIO.isFinished());

                System.out.println(
                                "Ready queues empty: "
                                                + readyQueues.isEmpty());

                System.out.println(
                                "Page-fault queue empty: "
                                                + pageFaultQueue.isEmpty());

                System.out.println(
                                "Resource wait queue empty: "
                                                + resourceWaitQueue.isEmpty());

                System.out.println(
                                "All frames released: "
                                                + allFramesReleased);

                System.out.println(
                                "All resources released: "
                                                + allResourcesReleased);

                System.out.println();
                System.out.println(
                                "========================================");

                System.out.println(
                                " SIMULATION FINISHED");

                System.out.println(
                                "========================================");
        }
}