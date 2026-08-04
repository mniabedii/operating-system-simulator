package com.github.mniabedii.memory;

import com.github.mniabedii.process.PCB;

import java.util.Objects;

public class Frame {

    public static final int NO_PROCESS = -1;
    public static final int NO_PAGE = -1;

    private final int frameNumber;

    private PCB pcb;
    private int pageNumber;

    public Frame(int frameNumber) {
        if (frameNumber < 0) {
            throw new IllegalArgumentException(
                    "Frame number cannot be negative");
        }

        this.frameNumber = frameNumber;
        this.pcb = null;
        this.pageNumber = NO_PAGE;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public synchronized int getProcessId() {
        if (pcb == null) {
            return NO_PROCESS;
        }

        return pcb.getPid();
    }

    public synchronized PCB getPCB() {
        if (pcb == null) {
            throw new IllegalStateException(
                    "Frame " + frameNumber + " is free");
        }

        return pcb;
    }

    public synchronized int getPageNumber() {
        return pageNumber;
    }

    public synchronized boolean isFree() {
        return pcb == null;
    }

    public synchronized boolean contains(
            int processId,
            int pageNumber) {

        return pcb != null
                && pcb.getPid() == processId
                && this.pageNumber == pageNumber;
    }

    public synchronized void load(
            PCB pcb,
            int pageNumber) {

        Objects.requireNonNull(pcb, "pcb");

        if (pageNumber < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        if (!isFree()) {
            throw new IllegalStateException(
                    "Frame " + frameNumber
                            + " is already occupied");
        }

        this.pcb = pcb;
        this.pageNumber = pageNumber;
    }

    public synchronized void clear() {
        this.pcb = null;
        this.pageNumber = NO_PAGE;
    }

    @Override
    public synchronized String toString() {
        if (isFree()) {
            return "F" + frameNumber + "{free}";
        }

        return "F" + frameNumber
                + "{P" + pcb.getPid()
                + ", page=" + pageNumber
                + '}';
    }
}