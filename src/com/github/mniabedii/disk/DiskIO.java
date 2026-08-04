package com.github.mniabedii.disk;

import com.github.mniabedii.clock.SimulationClock;
import com.github.mniabedii.config.SimulationConfig;
import com.github.mniabedii.memory.PhysicalMemory;
import com.github.mniabedii.process.PCB;
import com.github.mniabedii.process.ProcessState;
import com.github.mniabedii.scheduler.ReadyQueue;
import com.github.mniabedii.memory.MMU;
import com.github.mniabedii.memory.PageReplacementResult;

import java.util.Objects;

public class DiskIO implements Runnable {

    private final PageFaultQueue pageFaultQueue;
    private final PhysicalMemory physicalMemory;
    private final ReadyQueue readyQueues;
    private final SimulationClock clock;
    private final MMU mmu;

    private volatile boolean busy;
    private volatile boolean finished;

    public DiskIO(
            PageFaultQueue pageFaultQueue,
            PhysicalMemory physicalMemory,
            ReadyQueue readyQueues,
            SimulationClock clock,
            MMU mmu) {

        this.pageFaultQueue = Objects.requireNonNull(
                pageFaultQueue,
                "pageFaultQueue");

        this.physicalMemory = Objects.requireNonNull(
                physicalMemory,
                "physicalMemory");

        this.readyQueues = Objects.requireNonNull(
                readyQueues,
                "readyQueues");

        this.clock = Objects.requireNonNull(
                clock,
                "clock");

        this.mmu = mmu;

        this.busy = false;
        this.finished = false;
    }

    @Override
    public void run() {
        try {
            while (true) {
                PageFaultRequest request = pageFaultQueue.takeRequest();

                if (request == null) {
                    break;
                }

                processRequest(request);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            busy = false;
            finished = true;
        }
    }

    public boolean isBusy() {
        return busy;
    }

    public boolean isFinished() {
        return finished;
    }

    private void processRequest(
            PageFaultRequest request)
            throws InterruptedException {

        PCB pcb = request.getPCB();
        int pageNumber = request.getPageNumber();

        if (pcb.getState() != ProcessState.WAITING) {
            throw new IllegalStateException(
                    "P" + pcb.getPid()
                            + " must be WAITING during disk I/O");
        }

        busy = true;

        int startTick = clock.getCurrentTick();
        int completionTick = startTick + SimulationConfig.DISK_IO_TICKS;

        System.out.printf(
                "[Tick %d] Disk started loading"
                        + " page %d for P%d%n",
                startTick,
                pageNumber,
                pcb.getPid());

        clock.waitUntilTick(completionTick);

        loadRequestedPage(pcb, pageNumber);

        pcb.setState(ProcessState.READY);
        readyQueues.addToReadyQueue(pcb);

        System.out.printf(
                "[Tick %d] Disk loaded page %d"
                        + " for P%d and returned it to READY%n",
                clock.getCurrentTick(),
                pageNumber,
                pcb.getPid());

        busy = false;
    }

    private void loadRequestedPage(
            PCB pcb,
            int pageNumber) {

        if (pcb.getPageTable()
                .isPagePresent(pageNumber)) {

            return;
        }

        int frameNumber = physicalMemory.loadPageIntoFreeFrame(
                pcb,
                pageNumber);

        if (frameNumber != PhysicalMemory.NO_FREE_FRAME) {
            System.out.printf(
                    "[Tick %d] Disk placed P%d page %d"
                            + " into free frame F%d%n",
                    clock.getCurrentTick(),
                    pcb.getPid(),
                    pageNumber,
                    frameNumber);

            return;
        }

        PageReplacementResult replacement = physicalMemory.replacePage(
                pcb,
                pageNumber);

        mmu.invalidatePage(
                replacement.getVictimProcessId(),
                replacement.getVictimPageNumber());

        if (replacement.wasVictimDirty()) {
            System.out.printf(
                    "[Tick %d] Disk wrote dirty"
                            + " P%d page %d back to disk%n",
                    clock.getCurrentTick(),
                    replacement.getVictimProcessId(),
                    replacement.getVictimPageNumber());
        }

        System.out.printf(
                "[Tick %d] %s replacement used F%d:"
                        + " removed P%d page %d"
                        + " and loaded P%d page %d%n",
                clock.getCurrentTick(),
                physicalMemory.getReplacementPolicy(),
                replacement.getFrameNumber(),
                replacement.getVictimProcessId(),
                replacement.getVictimPageNumber(),
                pcb.getPid(),
                pageNumber);
    }
}