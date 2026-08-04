package com.github.mniabedii.memory;

import com.github.mniabedii.buffer.ProcessBuffer;
import com.github.mniabedii.clock.SimulationClock;
import com.github.mniabedii.process.PCB;
import com.github.mniabedii.process.ProcessState;
import com.github.mniabedii.scheduler.ReadyQueue;

import java.util.Objects;

public class MemoryManager implements Runnable {

    private final ProcessBuffer processBuffer;
    private final ReadyQueue readyQueues;
    private final SimulationClock clock;

    private volatile boolean finished;

    public MemoryManager(
            ProcessBuffer processBuffer,
            ReadyQueue readyQueues,
            SimulationClock clock) {

        this.processBuffer = Objects.requireNonNull(
                processBuffer,
                "processBuffer");

        this.readyQueues = Objects.requireNonNull(
                readyQueues,
                "readyQueues");

        this.clock = Objects.requireNonNull(
                clock,
                "clock");

        this.finished = false;
    }

    @Override
    public void run() {
        try {
            while (true) {
                PCB pcb = processBuffer.takeFromBuffer();

                if (pcb == null) {
                    break;
                }

                admitProcess(pcb);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            finished = true;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    private void admitProcess(PCB pcb) {
        PageTable pageTable = new PageTable(pcb.getRequiredPages());

        pcb.setPageTable(pageTable);
        pcb.setState(ProcessState.READY);

        readyQueues.addToReadyQueue(pcb);

        System.out.printf(
                "[Tick %d] Memory Manager admitted P%d"
                        + " to %s queue"
                        + " with %d page-table entries"
                        + " and %d loaded pages%n",
                clock.getCurrentTick(),
                pcb.getPid(),
                pcb.getType(),
                pageTable.getPageCount(),
                pageTable.getPresentPageCount());
    }
}