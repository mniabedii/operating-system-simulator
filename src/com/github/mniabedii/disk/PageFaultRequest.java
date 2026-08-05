package com.github.mniabedii.disk;

import com.github.mniabedii.process.PCB;

import java.util.Objects;

public class PageFaultRequest {

    private final PCB pcb;
    private final int pageNumber;

    public PageFaultRequest(PCB pcb, int pageNumber) {

        this.pcb = Objects.requireNonNull(pcb, "pcb");

        if (pageNumber < 0
                || pageNumber >= pcb.getPageTable().getPageCount()) {

            throw new IllegalArgumentException(
                    "Invalid page " + pageNumber
                            + " for P" + pcb.getPid());
        }

        this.pageNumber = pageNumber;
    }

    public PCB getPCB() {
        return pcb;
    }

    public int getPageNumber() {
        return pageNumber;
    }
}