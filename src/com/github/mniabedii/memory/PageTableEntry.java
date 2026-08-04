package com.github.mniabedii.memory;

public class PageTableEntry {

    public static final int NO_FRAME = -1;

    private final int pageNumber;

    private int frameNumber;
    private boolean present;
    private boolean dirty;

    public PageTableEntry(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        this.pageNumber = pageNumber;
        this.frameNumber = NO_FRAME;
        this.present = false;
        this.dirty = false;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public synchronized int getFrameNumber() {
        return frameNumber;
    }

    public synchronized boolean isPresent() {
        return present;
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void mapToFrame(int frameNumber) {
        if (frameNumber < 0) {
            throw new IllegalArgumentException(
                    "Frame number cannot be negative");
        }

        this.frameNumber = frameNumber;
        this.present = true;
        this.dirty = false;
    }

    public synchronized void setDirty(boolean dirty) {
        if (dirty && !present) {
            throw new IllegalStateException(
                    "A page not in memory cannot be dirty");
        }

        this.dirty = dirty;
    }

    public synchronized void removeFromFrame() {
        this.frameNumber = NO_FRAME;
        this.present = false;
        this.dirty = false;
    }

    @Override
    public synchronized String toString() {
        if (!present) {
            return "Page " + pageNumber + " -> disk";
        }

        return "Page " + pageNumber
                + " -> Frame " + frameNumber
                + (dirty ? " dirty" : "");
    }
}