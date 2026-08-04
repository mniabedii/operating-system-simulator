package com.github.mniabedii.scheduler;

import com.github.mniabedii.process.PCB;
import com.github.mniabedii.process.ProcessState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Iterator;

public class ReadyQueue {

    private static final Comparator<PCB> BACKGROUND_ORDER = Comparator.comparingInt(PCB::getRemainingBurstTime)
            .thenComparingInt(PCB::getArrivalTime)
            .thenComparingInt(PCB::getPid);

    private final Queue<PCB> systemQueue;
    private final Queue<PCB> interactiveQueue;
    private final PriorityQueue<PCB> backgroundQueue;

    public ReadyQueue() {
        this.systemQueue = new ArrayDeque<>();
        this.interactiveQueue = new ArrayDeque<>();
        this.backgroundQueue = new PriorityQueue<>(BACKGROUND_ORDER);
    }

    public synchronized void addToReadyQueue(PCB pcb) {
        Objects.requireNonNull(pcb, "pcb");

        if (pcb.getState() != ProcessState.READY) {
            throw new IllegalArgumentException(
                    "Only READY processes can enter a ready queue");
        }

        pcb.resetReadyWaitTicks();

        switch (pcb.getSchedulingLevel()) {
            case SYSTEM:
                systemQueue.offer(pcb);
                break;

            case INTERACTIVE:
                interactiveQueue.offer(pcb);
                break;

            case BACKGROUND:
                backgroundQueue.offer(pcb);
                break;

            default:
                throw new IllegalStateException(
                        "Unknown scheduling level: "
                                + pcb.getSchedulingLevel());
        }
    }

    public synchronized PCB takeNextProcess() {
        // fixed priority
        if (!systemQueue.isEmpty()) {
            return systemQueue.poll();
        }

        if (!interactiveQueue.isEmpty()) {
            return interactiveQueue.poll();
        }

        if (!backgroundQueue.isEmpty()) {
            return backgroundQueue.poll();
        }

        return null;
    }

    public synchronized List<PCB> applyAging(int thresholdTicks) {

        if (thresholdTicks <= 0) {
            throw new IllegalArgumentException(
                    "Aging threshold must be positive");
        }

        List<PCB> promotedProcesses = new ArrayList<>();

        ageInteractiveQueue(
                thresholdTicks,
                promotedProcesses);

        ageBackgroundQueue(
                thresholdTicks,
                promotedProcesses);

        return promotedProcesses;
    }

    private void ageInteractiveQueue(
            int thresholdTicks,
            List<PCB> promotedProcesses) {

        Iterator<PCB> iterator = interactiveQueue.iterator();

        while (iterator.hasNext()) {
            PCB pcb = iterator.next();

            pcb.incrementReadyWaitTicks();

            if (pcb.getReadyWaitTicks() >= thresholdTicks) {

                iterator.remove();
                pcb.promoteOneLevel();
                systemQueue.offer(pcb);

                promotedProcesses.add(pcb);
            }
        }
    }

    private void ageBackgroundQueue(
            int thresholdTicks,
            List<PCB> promotedProcesses) {

        Iterator<PCB> iterator = backgroundQueue.iterator();

        while (iterator.hasNext()) {
            PCB pcb = iterator.next();

            pcb.incrementReadyWaitTicks();

            if (pcb.getReadyWaitTicks() >= thresholdTicks) {

                iterator.remove();
                pcb.promoteOneLevel();
                interactiveQueue.offer(pcb);

                promotedProcesses.add(pcb);
            }
        }
    }

    public synchronized boolean isEmpty() {
        return systemQueue.isEmpty()
                && interactiveQueue.isEmpty()
                && backgroundQueue.isEmpty();
    }

    public synchronized int getTotalSize() {
        return systemQueue.size()
                + interactiveQueue.size()
                + backgroundQueue.size();
    }

    public synchronized int getSystemQueueSize() {
        return systemQueue.size();
    }

    public synchronized int getInteractiveQueueSize() {
        return interactiveQueue.size();
    }

    public synchronized int getBackgroundQueueSize() {
        return backgroundQueue.size();
    }

    public synchronized boolean hasSystemProcess() {
        return !systemQueue.isEmpty();
    }

    public synchronized boolean hasInteractiveProcess() {
        return !interactiveQueue.isEmpty();
    }

    public synchronized PCB peekShortestBackgroundProcess() {
        return backgroundQueue.peek();
    }

    public synchronized String getQueueStatus() {
        List<PCB> sortedBackground = new ArrayList<>(backgroundQueue);

        sortedBackground.sort(BACKGROUND_ORDER);

        return "System Queue: "
                + formatQueue(systemQueue)
                + System.lineSeparator()
                + "Interactive Queue: "
                + formatQueue(interactiveQueue)
                + System.lineSeparator()
                + "Background Queue: "
                + formatQueue(sortedBackground);
    }

    private String formatQueue(Collection<PCB> queue) {
        if (queue.isEmpty()) {
            return "empty";
        }

        StringBuilder result = new StringBuilder();

        for (PCB pcb : queue) {
            if (result.length() > 0) {
                result.append(' ');
            }

            result.append('P').append(pcb.getPid());
        }

        return result.toString();
    }

    public synchronized String getCompactStatus() {
        List<PCB> sortedBackground = new ArrayList<>(backgroundQueue);

        sortedBackground.sort(BACKGROUND_ORDER);

        return "Ready{SYS=["
                + formatQueue(systemQueue)
                + "], INT=["
                + formatQueue(interactiveQueue)
                + "], BG=["
                + formatQueue(sortedBackground)
                + "]}";
    }
}