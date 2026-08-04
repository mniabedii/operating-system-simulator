package com.github.mniabedii.scheduler;

import com.github.mniabedii.clock.SimulationClock;
import com.github.mniabedii.config.SimulationConfig;
import com.github.mniabedii.disk.DiskIO;
import com.github.mniabedii.disk.PageFaultQueue;
import com.github.mniabedii.disk.PageFaultRequest;
import com.github.mniabedii.generator.ProcessGenerator;
import com.github.mniabedii.memory.MMU;
import com.github.mniabedii.memory.MemoryAccessResult;
import com.github.mniabedii.memory.PhysicalMemory;
import com.github.mniabedii.memory.TLB;
import com.github.mniabedii.memory.MemoryManager;
import com.github.mniabedii.process.PCB;
import com.github.mniabedii.process.ProcessState;
import com.github.mniabedii.process.ProcessType;

import java.util.Objects;

public class CPUScheduler implements Runnable {

        private final SimulationClock clock;
        private final ReadyQueue readyQueues;
        private int interactiveQuantumUsed;
        private final MMU mmu;
        private final PhysicalMemory physicalMemory;
        private final TLB tlb;
        private final PageFaultQueue pageFaultQueue;
        private final ProcessGenerator generator;
        private final MemoryManager memoryManager;
        private final DiskIO diskIO;
        private final int expectedProcessCount;

        private volatile PCB runningProcess;
        private volatile boolean finished;

        private int terminatedProcessCount;
        private int contextSwitchCount;

        public CPUScheduler(
                        SimulationClock clock,
                        ReadyQueue readyQueues,
                        MMU mmu,
                        PhysicalMemory physicalMemory,
                        TLB tlb,
                        PageFaultQueue pageFaultQueue,
                        ProcessGenerator generator,
                        MemoryManager memoryManager,
                        DiskIO diskIO,
                        int expectedProcessCount) {

                if (expectedProcessCount <= 0) {
                        throw new IllegalArgumentException(
                                        "Expected process count must be positive");
                }

                this.clock = Objects.requireNonNull(
                                clock,
                                "clock");

                this.readyQueues = Objects.requireNonNull(
                                readyQueues,
                                "readyQueues");

                this.interactiveQuantumUsed = 0;

                this.mmu = Objects.requireNonNull(
                                mmu,
                                "mmu");

                this.physicalMemory = Objects.requireNonNull(
                                physicalMemory,
                                "physicalMemory");

                this.tlb = Objects.requireNonNull(
                                tlb,
                                "tlb");

                this.pageFaultQueue = Objects.requireNonNull(
                                pageFaultQueue,
                                "pageFaultQueue");

                this.generator = Objects.requireNonNull(
                                generator,
                                "generator");

                this.memoryManager = Objects.requireNonNull(
                                memoryManager,
                                "memoryManager");

                this.diskIO = Objects.requireNonNull(
                                diskIO,
                                "diskIO");

                this.expectedProcessCount = expectedProcessCount;

                this.runningProcess = null;
                this.finished = false;
                this.terminatedProcessCount = 0;
                this.contextSwitchCount = 0;
        }

        @Override
        public void run() {
                try {
                        while (!shouldTerminate()) {
                                if (runningProcess == null) {
                                        PCB nextProcess = readyQueues.takeNextProcess();

                                        if (nextProcess == null) {
                                                advanceOneTick("IDLE");
                                                continue;
                                        }

                                        dispatch(nextProcess);
                                }

                                executeCurrentProcessStep();
                        }
                } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                } finally {
                        pageFaultQueue.closeQueue();
                        finished = true;
                }
        }

        public boolean isFinished() {
                return finished;
        }

        public int getTerminatedProcessCount() {
                return terminatedProcessCount;
        }

        public int getContextSwitchCount() {
                return contextSwitchCount;
        }

        public PCB getRunningProcess() {
                return runningProcess;
        }

        private void dispatch(PCB pcb)
                        throws InterruptedException {

                if (pcb.getState() != ProcessState.READY) {
                        throw new IllegalStateException(
                                        "Only a READY process can be dispatched");
                }

                contextSwitchCount++;

                for (int tick = 1; tick <= SimulationConfig.CONTEXT_SWITCH_TICKS; tick++) {

                        advanceOneTick(
                                        "CONTEXT_SWITCH "
                                                        + tick
                                                        + "/"
                                                        + SimulationConfig.CONTEXT_SWITCH_TICKS
                                                        + " -> P"
                                                        + pcb.getPid());
                }

                interactiveQuantumUsed = 0;

                pcb.setState(ProcessState.RUNNING);
                runningProcess = pcb;

                System.out.printf(
                                "[Tick %d] P%d is now RUNNING%n",
                                clock.getCurrentTick(),
                                pcb.getPid());
        }

        private void executeCurrentProcessStep()
                        throws InterruptedException {

                PCB pcb = runningProcess;

                int pageNumber = pcb.getCurrentPageReference();

                MemoryAccessResult result = mmu.translate(
                                pcb,
                                pageNumber,
                                0);

                if (result.isTlbMiss()) {
                        advanceOneTick(
                                        "P" + pcb.getPid()
                                                        + " TLB_MISS"
                                                        + " page=" + pageNumber);
                }

                if (result.isPageFault()) {
                        blockForPageFault(
                                        pcb,
                                        pageNumber);

                        return;
                }

                pcb.executeOneTick();
                if (pcb.getType() == ProcessType.INTERACTIVE) {
                        interactiveQuantumUsed++;
                }

                advanceOneTick(
                                "RUNNING P"
                                                + pcb.getPid()
                                                + " page="
                                                + pageNumber
                                                + " remaining="
                                                + pcb.getRemainingBurstTime());

                if (pcb.isFinished()) {
                        terminateCurrentProcess();
                }
        }

        private void blockForPageFault(
                        PCB pcb,
                        int pageNumber) {

                pcb.setState(ProcessState.WAITING);

                PageFaultRequest request = new PageFaultRequest(
                                pcb,
                                pageNumber,
                                clock.getCurrentTick());

                pageFaultQueue.putRequest(request);

                System.out.printf(
                                "[Tick %d] P%d caused PAGE FAULT"
                                                + " on page %d and entered WAITING%n",
                                clock.getCurrentTick(),
                                pcb.getPid(),
                                pageNumber);

                runningProcess = null;
                interactiveQuantumUsed = 0;
        }

        private void terminateCurrentProcess() {
                PCB pcb = runningProcess;

                pcb.setState(ProcessState.TERMINATED);

                mmu.invalidateProcess(pcb.getPid());
                physicalMemory.releaseProcess(pcb);

                terminatedProcessCount++;

                runningProcess = null;
                interactiveQuantumUsed = 0;

                System.out.printf(
                                "[Tick %d] P%d TERMINATED"
                                                + " (%d/%d completed)%n",
                                clock.getCurrentTick(),
                                pcb.getPid(),
                                terminatedProcessCount,
                                expectedProcessCount);
        }

        private boolean shouldTerminate() {
                return terminatedProcessCount == expectedProcessCount
                                && generator.isFinished()
                                && memoryManager.isFinished()
                                && runningProcess == null
                                && readyQueues.isEmpty()
                                && !pageFaultQueue.hasPendingWork()
                                && !diskIO.isBusy();
        }

        private void advanceOneTick(String cpuStatus)
                        throws InterruptedException {

                int tick = clock.advanceOneTick();

                printTickStatus(
                                tick,
                                cpuStatus);

                Thread.sleep(40);
        }

        private boolean shouldEndInteractiveQuantum(PCB pcb) {
                return pcb.getType() == ProcessType.INTERACTIVE
                                && interactiveQuantumUsed >= SimulationConfig.ROUND_ROBIN_QUANTUM;
        }

        private void handleInteractiveQuantumEnd(PCB pcb) {
                if (readyQueues.isEmpty()) {
                        interactiveQuantumUsed = 0;

                        System.out.printf(
                                        "[Tick %d] P%d received another quantum"
                                                        + " because no other process is READY%n",
                                        clock.getCurrentTick(),
                                        pcb.getPid());

                        return;
                }

                pcb.setState(ProcessState.READY);
                readyQueues.addToReadyQueue(pcb);

                runningProcess = null;
                interactiveQuantumUsed = 0;

                System.out.printf(
                                "[Tick %d] P%d used its Round Robin quantum"
                                                + " and returned to READY%n",
                                clock.getCurrentTick(),
                                pcb.getPid());
        }

        private void printTickStatus(
                        int tick,
                        String cpuStatus) {

                String quantumStatus = "-";

                if (runningProcess != null
                                && runningProcess.getType() == ProcessType.INTERACTIVE) {

                        quantumStatus = interactiveQuantumUsed
                                        + "/"
                                        + SimulationConfig.ROUND_ROBIN_QUANTUM;
                }
                System.out.printf(
                                "[Tick %d] CPU=%s"
                                                + " | Quantum=%s"
                                                + " | %s"
                                                + " | Memory=%d/%d used"
                                                + " | TLB=%dH/%dM"
                                                + " | Faults=%d"
                                                + " | Disk=%s%n",
                                tick,
                                cpuStatus,
                                quantumStatus,
                                readyQueues.getCompactStatus(),
                                physicalMemory.getOccupiedFrameCount(),
                                physicalMemory.getFrameCount(),
                                tlb.getHitCount(),
                                tlb.getMissCount(),
                                mmu.getPageFaultCount(),
                                diskIO.isBusy() ? "BUSY" : "IDLE");
        }
}