package com.github.mniabedii.memory;

import java.util.Objects;

public class MemoryAccessResult {

    public static final int NO_FRAME = -1;

    private final MemoryAccessStatus status;
    private final int pageNumber;
    private final int frameNumber;
    private final int offset;

    public MemoryAccessResult(
            MemoryAccessStatus status,
            int pageNumber,
            int frameNumber,
            int offset) {

        this.status = Objects.requireNonNull(
                status,
                "status");

        if (pageNumber < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        if (offset < 0) {
            throw new IllegalArgumentException(
                    "Offset cannot be negative");
        }

        if (status == MemoryAccessStatus.PAGE_FAULT) {
            if (frameNumber != NO_FRAME) {
                throw new IllegalArgumentException(
                        "A page fault cannot have a frame");
            }
        } else if (frameNumber < 0) {
            throw new IllegalArgumentException(
                    "Successful access requires a frame");
        }

        this.pageNumber = pageNumber;
        this.frameNumber = frameNumber;
        this.offset = offset;
    }

    public MemoryAccessStatus getStatus() {
        return status;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isPageFault() {
        return status == MemoryAccessStatus.PAGE_FAULT;
    }

    public boolean isTlbMiss() {
        return status != MemoryAccessStatus.TLB_HIT;
    }
}