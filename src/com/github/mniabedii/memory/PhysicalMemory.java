package com.github.mniabedii.memory;

import com.github.mniabedii.process.PCB;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PhysicalMemory {

    public static final int NO_FREE_FRAME = -1;

    private final List<Frame> frames;

    public PhysicalMemory(int frameCount) {
        if (frameCount <= 0) {
            throw new IllegalArgumentException(
                    "Frame count must be positive");
        }

        this.frames = new ArrayList<>();

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

    public synchronized int loadPageIntoFreeFrame(
            PCB pcb,
            int pageNumber) {

        Objects.requireNonNull(pcb, "pcb");

        PageTable pageTable = pcb.getPageTable();
        PageTableEntry entry = pageTable.getEntry(pageNumber);

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

        freeFrame.load(
                pcb.getPid(),
                pageNumber);

        entry.mapToFrame(
                freeFrame.getFrameNumber());

        return freeFrame.getFrameNumber();
    }

    public synchronized void removePage(
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
                    result.append(System.lineSeparator());
                } else {
                    result.append(' ');
                }
            }

            result.append(frames.get(i));
        }

        return result.toString();
    }

    private Frame findFreeFrame() {
        for (Frame frame : frames) {
            if (frame.isFree()) {
                return frame;
            }
        }

        return null;
    }
}