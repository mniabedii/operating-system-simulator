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
                PageFaultQueue pageFaultQueue = new PageFaultQueue();
                ResourceWaitQueue resourceWaitQueue = new ResourceWaitQueue();

                PhysicalMemory physicalMemory = new PhysicalMemory(
                                SimulationConfig.FRAME_COUNT,
                                SimulationConfig.PAGE_REPLACEMENT_POLICY);

                TLB tlb = new TLB(
                                SimulationConfig.TLB_CAPACITY);

                MMU mmu = new MMU(tlb);

                ResourceManager resourceManager = new ResourceManager(
                                SimulationConfig.getTotalResources());

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

                printConfiguration();

                /*
                 * Start consumers before the producer and scheduler.
                 */
                diskThread.start();
                memoryManagerThread.start();
                generatorThread.start();
                schedulerThread.start();

                /*
                 * The scheduler detects the global termination condition
                 * and closes the page-fault queue.
                 */
                schedulerThread.join();
                generatorThread.join();
                memoryManagerThread.join();
                diskThread.join();

                printFinalReport(
                                clock,
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

        private static void printConfiguration() {
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
                                "Scheduling: SYSTEM=FCFS, "
                                                + "INTERACTIVE=RR(q="
                                                + SimulationConfig.ROUND_ROBIN_QUANTUM
                                                + "), BACKGROUND=SRTF");

                System.out.println(
                                "Context-switch cost: "
                                                + SimulationConfig.CONTEXT_SWITCH_TICKS
                                                + " ticks");

                System.out.println(
                                "Aging threshold: "
                                                + SimulationConfig.AGING_THRESHOLD_TICKS
                                                + " ticks");

                System.out.println(
                                "Frames: "
                                                + SimulationConfig.FRAME_COUNT);

                System.out.println(
                                "TLB capacity: "
                                                + SimulationConfig.TLB_CAPACITY);

                System.out.println(
                                "Page replacement: "
                                                + SimulationConfig.PAGE_REPLACEMENT_POLICY);

                System.out.println(
                                "Disk I/O cost: "
                                                + SimulationConfig.DISK_IO_TICKS
                                                + " ticks");

                System.out.println(
                                "Resources: "
                                                + Arrays.toString(
                                                                SimulationConfig
                                                                                .getTotalResources()));

                System.out.println(
                                "========================================");

                System.out.println();
        }

        private static void printFinalReport(
                        SimulationClock clock,
                        CPUScheduler scheduler,
                        DiskIO diskIO,
                        ReadyQueue readyQueues,
                        PageFaultQueue pageFaultQueue,
                        ResourceWaitQueue resourceWaitQueue,
                        PhysicalMemory physicalMemory,
                        TLB tlb,
                        MMU mmu,
                        ResourceManager resourceManager) {

                boolean allProcessesTerminated = scheduler
                                .getTerminatedProcessCount() == SimulationConfig.MAX_PROCESSES;

                boolean readyQueuesEmpty = readyQueues.isEmpty();

                boolean pageFaultWorkFinished = !pageFaultQueue.hasPendingWork();

                boolean resourceWaitQueueEmpty = resourceWaitQueue.isEmpty();

                boolean diskIdle = !diskIO.isBusy();

                boolean allFramesReleased = physicalMemory.getFreeFrameCount() == physicalMemory.getFrameCount();

                boolean allResourcesReleased = Arrays.equals(
                                resourceManager
                                                .getAvailableResources(),
                                SimulationConfig
                                                .getTotalResources());

                boolean completedSuccessfully = allProcessesTerminated
                                && readyQueuesEmpty
                                && pageFaultWorkFinished
                                && resourceWaitQueueEmpty
                                && diskIdle
                                && allFramesReleased
                                && allResourcesReleased;

                System.out.println();

                System.out.println(
                                "========================================");

                System.out.println(
                                " FINAL SIMULATION REPORT");

                System.out.println(
                                "========================================");

                System.out.println(
                                "Final logical tick: "
                                                + clock.getCurrentTick());

                System.out.printf(
                                "Terminated processes: %d/%d%n",
                                scheduler.getTerminatedProcessCount(),
                                SimulationConfig.MAX_PROCESSES);

                System.out.println(
                                "Context switches: "
                                                + scheduler.getContextSwitchCount());

                System.out.println(
                                "Preemptions: "
                                                + scheduler.getPreemptionCount());

                System.out.println();

                System.out.println(
                                "Page faults: "
                                                + mmu.getPageFaultCount());

                System.out.println(
                                "Disk operations: "
                                                + diskIO.getCompletedOperationCount());

                System.out.println(
                                "TLB hits: "
                                                + tlb.getHitCount());

                System.out.println(
                                "TLB misses: "
                                                + tlb.getMissCount());

                System.out.printf(
                                "TLB hit rate: %.2f%%%n",
                                tlb.getHitRate() * 100.0);

                System.out.println();

                System.out.println(
                                "Deadlocks detected: "
                                                + scheduler.getDeadlockCount());

                System.out.println(
                                "Deadlock victims: "
                                                + scheduler.getDeadlockVictimCount());

                System.out.println();

                System.out.println(
                                "Ready queues empty: "
                                                + readyQueuesEmpty);

                System.out.println(
                                "Page-fault work finished: "
                                                + pageFaultWorkFinished);

                System.out.println(
                                "Resource wait queue empty: "
                                                + resourceWaitQueueEmpty);

                System.out.println(
                                "Disk idle: "
                                                + diskIdle);

                System.out.println(
                                "All frames released: "
                                                + allFramesReleased);

                System.out.println(
                                "All resources released: "
                                                + allResourcesReleased);

                System.out.println();

                System.out.println(
                                "Simulation completed successfully: "
                                                + completedSuccessfully);

                System.out.println(
                                "========================================");
        }
}