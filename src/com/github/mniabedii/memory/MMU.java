package com.github.mniabedii.memory;

import com.github.mniabedii.process.PCB;

import java.util.Objects;

public class MMU {

    private final TLB tlb;

    private int pageFaultCount;

    public MMU(TLB tlb) {
        this.tlb = Objects.requireNonNull(tlb, "tlb");
        this.pageFaultCount = 0;
    }

    public synchronized MemoryAccessResult translate(
            PCB pcb,
            int pageNumber,
            int offset) {

        Objects.requireNonNull(pcb, "pcb");

        if (offset < 0) {
            throw new IllegalArgumentException(
                    "Offset cannot be negative");
        }

        PageTable pageTable = pcb.getPageTable();

        if (pageNumber < 0
                || pageNumber >= pageTable.getPageCount()) {

            throw new IllegalArgumentException(
                    "Invalid page " + pageNumber
                            + " for P" + pcb.getPid());
        }

        int frameNumber = tlb.lookup(
                pcb.getPid(),
                pageNumber);

        if (frameNumber != TLB.NO_FRAME) {
            return new MemoryAccessResult(
                    MemoryAccessStatus.TLB_HIT,
                    pageNumber,
                    frameNumber,
                    offset);
        }

        PageTableEntry entry = pageTable.getEntry(pageNumber);

        if (!entry.isPresent()) {
            pageFaultCount++;

            return new MemoryAccessResult(
                    MemoryAccessStatus.PAGE_FAULT,
                    pageNumber,
                    MemoryAccessResult.NO_FRAME,
                    offset);
        }

        frameNumber = entry.getFrameNumber();

        tlb.addEntry(
                pcb.getPid(),
                pageNumber,
                frameNumber);

        return new MemoryAccessResult(
                MemoryAccessStatus.PAGE_TABLE_HIT,
                pageNumber,
                frameNumber,
                offset);
    }

    public synchronized int getPageFaultCount() {
        return pageFaultCount;
    }

    public void invalidatePage(
            int processId,
            int pageNumber) {

        tlb.removeEntry(processId, pageNumber);
    }

    public void invalidateProcess(int processId) {
        tlb.removeProcessEntries(processId);
    }
}