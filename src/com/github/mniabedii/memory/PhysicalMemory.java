package com.github.mniabedii.memory;

import com.github.mniabedii.process.PCB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;

public class PhysicalMemory {

    public static final int NO_FREE_FRAME = -1;

    private final List<Frame> frames;
    private final Queue<Integer> fifoOrder;
    private final PageReplacementPolicy replacementPolicy;
    private final Random random;

    public PhysicalMemory(
            int frameCount,
            PageReplacementPolicy replacementPolicy) {

        if (frameCount <= 0) {
            throw new IllegalArgumentException(
                    "Frame count must be positive");
        }

        this.replacementPolicy = Objects.requireNonNull(
                replacementPolicy,
                "replacementPolicy");

        this.frames = new ArrayList<>();
        this.fifoOrder = new ArrayDeque<>();
        this.random = new Random();

        for (int frameNumber = 0; frameNumber < frameCount; frameNumber++) {

            frames.add(new Frame(frameNumber));
        }
    }

    public synchronized int getFrameCount() {
        return frames.size();
    }

    public synchronized int getFreeFrameCount() {
        int freeCount = 0;

        for (Frame frame : frames) {
            if (frame.isFree()) {
                freeCount++;
            }
        }

        return freeCount;
    }

    public synchronized int getOccupiedFrameCount() {
        return getFrameCount() - getFreeFrameCount();
    }

    public synchronized boolean hasFreeFrame() {
        return findFreeFrame() != null;
    }

    public PageReplacementPolicy getReplacementPolicy() {
        return replacementPolicy;
    }

    public synchronized int loadPageIntoFreeFrame(
            PCB pcb,
            int pageNumber) {

        Objects.requireNonNull(pcb, "pcb");

        PageTableEntry entry = pcb.getPageTable().getEntry(pageNumber);

        if (entry.isPresent()) {
            throw new IllegalStateException(
                    "Page " + pageNumber
                            + " of P" + pcb.getPid()
                            + " is already in memory");
        }

        Frame freeFrame = findFreeFrame();

        if (freeFrame == null) {
            return NO_FREE_FRAME;
        }

        loadIntoFrame(
                freeFrame,
                pcb,
                pageNumber);

        return freeFrame.getFrameNumber();
    }

    public synchronized PageReplacementResult replacePage(
            PCB incomingPCB,
            int incomingPageNumber) {

        Objects.requireNonNull(
                incomingPCB,
                "incomingPCB");

        PageTableEntry incomingEntry = incomingPCB.getPageTable()
                .getEntry(incomingPageNumber);

        if (incomingEntry.isPresent()) {
            throw new IllegalStateException(
                    "Requested page is already in memory");
        }

        if (hasFreeFrame()) {
            throw new IllegalStateException(
                    "Replacement is unnecessary while "
                            + "a free frame exists");
        }

        Frame victimFrame = selectVictimFrame();

        PCB victimPCB = victimFrame.getPCB();
        int victimPageNumber = victimFrame.getPageNumber();

        PageTableEntry victimEntry = victimPCB.getPageTable()
                .getEntry(victimPageNumber);

        boolean victimDirty = victimEntry.isDirty();

        int frameNumber = victimFrame.getFrameNumber();

        victimEntry.removeFromFrame();
        victimFrame.clear();
        fifoOrder.remove(frameNumber);

        loadIntoFrame(
                victimFrame,
                incomingPCB,
                incomingPageNumber);

        return new PageReplacementResult(
                frameNumber,
                victimPCB.getPid(),
                victimPageNumber,
                victimDirty);
    }

    private synchronized void removePage(
            PCB pcb,
            int pageNumber) {

        Objects.requireNonNull(pcb, "pcb");

        PageTableEntry entry = pcb.getPageTable().getEntry(pageNumber);

        if (!entry.isPresent()) {
            throw new IllegalStateException(
                    "Page " + pageNumber
                            + " of P" + pcb.getPid()
                            + " is not in memory");
        }

        int frameNumber = entry.getFrameNumber();

        Frame frame = frames.get(frameNumber);

        if (!frame.contains(
                pcb.getPid(),
                pageNumber)) {

            throw new IllegalStateException(
                    "Page table and frame are inconsistent");
        }

        entry.removeFromFrame();
        frame.clear();
        fifoOrder.remove(frameNumber);
    }

    public synchronized void releaseProcess(PCB pcb) {
        Objects.requireNonNull(pcb, "pcb");

        PageTable pageTable = pcb.getPageTable();

        for (int pageNumber = 0; pageNumber < pageTable.getPageCount(); pageNumber++) {

            if (pageTable.isPagePresent(pageNumber)) {
                removePage(pcb, pageNumber);
            }
        }
    }

    public synchronized String getMemoryStatus() {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < frames.size(); i++) {
            if (i > 0) {
                if (i % 4 == 0) {
                    result.append(
                            System.lineSeparator());
                } else {
                    result.append(' ');
                }
            }

            result.append(frames.get(i));
        }

        return result.toString();
    }

    private void loadIntoFrame(
            Frame frame,
            PCB pcb,
            int pageNumber) {

        frame.load(pcb, pageNumber);

        pcb.getPageTable()
                .getEntry(pageNumber)
                .mapToFrame(frame.getFrameNumber());

        fifoOrder.offer(frame.getFrameNumber());
    }

    private Frame findFreeFrame() {
        for (Frame frame : frames) {
            if (frame.isFree()) {
                return frame;
            }
        }

        return null;
    }

    private Frame selectVictimFrame() {
        switch (replacementPolicy) {
            case FIFO:
                return selectFIFOFrame();

            case RANDOM:
                return selectRandomFrame();

            default:
                throw new IllegalStateException(
                        "Unknown replacement policy: "
                                + replacementPolicy);
        }
    }

    private Frame selectFIFOFrame() {
        Integer frameNumber = fifoOrder.peek();

        if (frameNumber == null) {
            throw new IllegalStateException(
                    "FIFO order is empty");
        }

        return frames.get(frameNumber);
    }

    private Frame selectRandomFrame() {
        List<Frame> occupiedFrames = new ArrayList<>();

        for (Frame frame : frames) {
            if (!frame.isFree()) {
                occupiedFrames.add(frame);
            }
        }

        if (occupiedFrames.isEmpty()) {
            throw new IllegalStateException(
                    "No occupied frame exists");
        }

        int index = random.nextInt(occupiedFrames.size());

        return occupiedFrames.get(index);
    }
}