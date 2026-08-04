package com.github.mniabedii.disk;

import com.github.mniabedii.process.PCB;

import java.util.Objects;

public class PageFaultRequest {

    private final PCB pcb;
    private final int pageNumber;
    private final int requestTick;

    public PageFaultRequest(
            PCB pcb,
            int pageNumber,
            int requestTick) {

        this.pcb = Objects.requireNonNull(pcb, "pcb");

        if (pageNumber < 0
                || pageNumber >= pcb.getPageTable().getPageCount()) {

            throw new IllegalArgumentException(
                    "Invalid page " + pageNumber
                            + " for P" + pcb.getPid());
        }

        if (requestTick < 0) {
            throw new IllegalArgumentException(
                    "Request tick cannot be negative");
        }

        this.pageNumber = pageNumber;
        this.requestTick = requestTick;
    }

    public PCB getPCB() {
        return pcb;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getRequestTick() {
        return requestTick;
    }

    @Override
    public String toString() {
        return "PageFaultRequest{P"
                + pcb.getPid()
                + ", page=" + pageNumber
                + ", tick=" + requestTick
                + '}';
    }
}