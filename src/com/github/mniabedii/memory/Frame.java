package com.github.mniabedii.memory;

public class Frame {

    public static final int NO_PROCESS = -1;
    public static final int NO_PAGE = -1;

    private final int frameNumber;

    private int processId;
    private int pageNumber;

    public Frame(int frameNumber) {
        if (frameNumber < 0) {
            throw new IllegalArgumentException(
                    "Frame number cannot be negative");
        }

        this.frameNumber = frameNumber;
        this.processId = NO_PROCESS;
        this.pageNumber = NO_PAGE;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public synchronized int getProcessId() {
        return processId;
    }

    public synchronized int getPageNumber() {
        return pageNumber;
    }

    public synchronized boolean isFree() {
        return processId == NO_PROCESS;
    }

    public synchronized void load(
            int processId,
            int pageNumber) {

        if (processId <= 0) {
            throw new IllegalArgumentException(
                    "PID must be positive");
        }

        if (pageNumber < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        if (!isFree()) {
            throw new IllegalStateException(
                    "Frame " + frameNumber
                            + " is already occupied");
        }

        this.processId = processId;
        this.pageNumber = pageNumber;
    }

    public synchronized void clear() {
        this.processId = NO_PROCESS;
        this.pageNumber = NO_PAGE;
    }

    @Override
    public synchronized String toString() {
        if (isFree()) {
            return "F" + frameNumber + "{free}";
        }

        return "F" + frameNumber
                + "{P" + processId
                + ", page=" + pageNumber
                + '}';
    }
}