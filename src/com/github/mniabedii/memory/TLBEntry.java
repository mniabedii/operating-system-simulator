package com.github.mniabedii.memory;

public class TLBEntry {

    private final int processId;
    private final int pageNumber;
    private final int frameNumber;

    public TLBEntry(
            int processId,
            int pageNumber,
            int frameNumber) {

        if (processId <= 0) {
            throw new IllegalArgumentException(
                    "PID must be positive");
        }

        if (pageNumber < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        if (frameNumber < 0) {
            throw new IllegalArgumentException(
                    "Frame number cannot be negative");
        }

        this.processId = processId;
        this.pageNumber = pageNumber;
        this.frameNumber = frameNumber;
    }

    public int getProcessId() {
        return processId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public boolean matches(
            int processId,
            int pageNumber) {

        return this.processId == processId
                && this.pageNumber == pageNumber;
    }

    @Override
    public String toString() {
        return "P" + processId
                + ":page" + pageNumber
                + "->F" + frameNumber;
    }
}