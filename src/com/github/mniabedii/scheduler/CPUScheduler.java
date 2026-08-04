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
import com.github.mniabedii.process.SchedulingLevel;
import com.github.mniabedii.process.WaitReason;
import com.github.mniabedii.resource.ResourceManager;
import com.github.mniabedii.resource.ResourceRequestResult;
import com.github.mniabedii.resource.ResourceWaitQueue;
import com.github.mniabedii.resource.ResourceWaitRequest;

import java.util.List;
import java.util.Objects;

public class CPUScheduler implements Runnable {

        private final SimulationClock clock;
        private final ReadyQueue readyQueues;
        private int interactiveQuantumUsed;
        private int preemptionCount;
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

        private final ResourceManager resourceManager;
        private final ResourceWaitQueue resourceWaitQueue;

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
                        int expectedProcessCount,
                        ResourceManager resourceManager,
                        ResourceWaitQueue resourceWaitQueue) {

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
                this.preemptionCount = 0;

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

                this.resourceManager = Objects.requireNonNull(
                                resourceManager,
                                "resourceManager");

                this.resourceWaitQueue = Objects.requireNonNull(
                                resourceWaitQueue,
                                "resourceWaitQueue");
        }

        @Override
        public void run() {
                try {
                        while (!shouldTerminate()) {
                                String preemptionReason = determinePreemptionReason();

                                if (preemptionReason != null) {
                                        preemptCurrentProcess(preemptionReason);

                                        continue;
                                }

                                if (runningProcess == null) {
                                        PCB nextProcess = readyQueues.takeNextProcess();

                                        if (nextProcess == null) {
                                                advanceOneTick("IDLE");
                                                continue;
                                        }

                                        dispatch(nextProcess);
                                        continue;
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

        public int getPreemptionCount() {
                return preemptionCount;
        }

        private String determinePreemptionReason() {
                PCB current = runningProcess;

                if (current == null) {
                        return null;
                }

                switch (current.getSchedulingLevel()) {
                        case SYSTEM:
                                return null;

                        case INTERACTIVE:
                                if (readyQueues.hasSystemProcess()) {
                                        return "a higher-priority SYSTEM process is READY";
                                }

                                return null;

                        case BACKGROUND:
                                if (readyQueues.hasSystemProcess()) {
                                        return "a higher-priority SYSTEM process is READY";
                                }

                                if (readyQueues.hasInteractiveProcess()) {
                                        return "a higher-priority INTERACTIVE process is READY";
                                }

                                PCB shortestBackground = readyQueues.peekShortestBackgroundProcess();

                                if (shortestBackground != null
                                                && shortestBackground
                                                                .getRemainingBurstTime() < current
                                                                                .getRemainingBurstTime()) {

                                        return "shorter BACKGROUND P"
                                                        + shortestBackground.getPid()
                                                        + " is READY with "
                                                        + shortestBackground
                                                                        .getRemainingBurstTime()
                                                        + " ticks remaining";
                                }

                                return null;

                        default:
                                throw new IllegalStateException(
                                                "Unknown process type: "
                                                                + current.getType());
                }
        }

        private void preemptCurrentProcess(String reason) {

                PCB pcb = runningProcess;

                if (pcb == null) {
                        throw new IllegalStateException(
                                        "No running process exists");
                }

                pcb.setState(ProcessState.READY);
                readyQueues.addToReadyQueue(pcb);

                runningProcess = null;
                interactiveQuantumUsed = 0;
                preemptionCount++;

                System.out.printf(
                                "[Tick %d] P%d PREEMPTED: %s%n",
                                clock.getCurrentTick(),
                                pcb.getPid(),
                                reason);
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

                pcb.resetReadyWaitTicks();
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
                        blockForPageFault(pcb, pageNumber);

                        return;
                }

                pcb.executeOneTick();

                if (pcb.getSchedulingLevel() == SchedulingLevel.INTERACTIVE) {

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
                        return;
                }

                if (shouldEndInteractiveQuantum(pcb)) {
                        handleInteractiveQuantumEnd(pcb);
                }
        }

        private void blockForPageFault(
                        PCB pcb,
                        int pageNumber) {

                pcb.setWaitReason(WaitReason.PAGE_FAULT);
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
                resourceManager.releaseAllResources(pcb);

                terminatedProcessCount++;

                runningProcess = null;
                interactiveQuantumUsed = 0;

                retryResourceWaiters();

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

        private void advanceOneTick(String cpuStatus) throws InterruptedException {

                int tick = clock.advanceOneTick();

                List<PCB> promotedProcesses = readyQueues.applyAging(SimulationConfig.AGING_THRESHOLD_TICKS);

                printAgingPromotions(tick, promotedProcesses);

                printTickStatus(tick, cpuStatus);

                Thread.sleep(40);
        }

        private void printAgingPromotions(
                        int tick,
                        List<PCB> promotedProcesses) {

                for (PCB pcb : promotedProcesses) {
                        String previousLevel;

                        if (pcb.getSchedulingLevel() == SchedulingLevel.SYSTEM) {

                                previousLevel = "INTERACTIVE";
                        } else {
                                previousLevel = "BACKGROUND";
                        }

                        System.out.printf(
                                        "[Tick %d] AGING promoted P%d"
                                                        + " from %s to %s%n",
                                        tick,
                                        pcb.getPid(),
                                        previousLevel,
                                        pcb.getSchedulingLevel());
                }
        }

        private boolean shouldEndInteractiveQuantum(PCB pcb) {
                return pcb.getSchedulingLevel() == SchedulingLevel.INTERACTIVE
                                && interactiveQuantumUsed >= SimulationConfig.ROUND_ROBIN_QUANTUM;
        }

        private void handleInteractiveQuantumEnd(PCB pcb) {
                boolean anotherEligibleProcess = readyQueues.hasSystemProcess()
                                || readyQueues.hasInteractiveProcess();

                if (!anotherEligibleProcess) {
                        interactiveQuantumUsed = 0;

                        System.out.printf(
                                        "[Tick %d] P%d received another quantum"
                                                        + " because no SYSTEM or INTERACTIVE"
                                                        + " process is READY%n",
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

        private void blockForResources(PCB pcb, int[] request) {

                pcb.setWaitReason(WaitReason.RESOURCE);
                pcb.setState(ProcessState.WAITING);

                ResourceWaitRequest waitRequest = new ResourceWaitRequest(
                                pcb,
                                request,
                                clock.getCurrentTick());

                resourceWaitQueue.addRequest(waitRequest);

                runningProcess = null;
                interactiveQuantumUsed = 0;

                System.out.printf(
                                "[Tick %d] P%d entered WAITING"
                                                + " for resources %s%n",
                                clock.getCurrentTick(),
                                pcb.getPid(),
                                java.util.Arrays.toString(request));
        }

        private void retryResourceWaiters() {
                List<ResourceWaitRequest> waitingRequests = resourceWaitQueue.getSnapshot();

                for (ResourceWaitRequest waitRequest : waitingRequests) {

                        PCB pcb = waitRequest.getPCB();

                        ResourceRequestResult result = resourceManager.requestResources(
                                        pcb,
                                        waitRequest.getRequest());

                        if (result == ResourceRequestResult.GRANTED) {
                                resourceWaitQueue.removeRequest(waitRequest);

                                pcb.setWaitReason(WaitReason.NONE);
                                pcb.setState(ProcessState.READY);

                                readyQueues.addToReadyQueue(pcb);

                                System.out.printf(
                                                "[Tick %d] Resource request for P%d"
                                                                + " was granted; process returned"
                                                                + " to READY%n",
                                                clock.getCurrentTick(),
                                                pcb.getPid());
                        } else if (result == ResourceRequestResult.EXCEEDS_MAXIMUM) {

                                throw new IllegalStateException(
                                                "Stored resource request exceeds"
                                                                + " P" + pcb.getPid()
                                                                + " maximum demand");
                        }
                }
        }

        private void printTickStatus(
                        int tick,
                        String cpuStatus) {

                String quantumStatus = "-";

                if (runningProcess != null
                                && runningProcess.getSchedulingLevel() == SchedulingLevel.INTERACTIVE) {

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