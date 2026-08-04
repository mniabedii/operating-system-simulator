package com.github.mniabedii.memory;

public class PageReplacementResult {

    private final int frameNumber;
    private final int victimProcessId;
    private final int victimPageNumber;
    private final boolean victimDirty;

    public PageReplacementResult(
            int frameNumber,
            int victimProcessId,
            int victimPageNumber,
            boolean victimDirty) {

        this.frameNumber = frameNumber;
        this.victimProcessId = victimProcessId;
        this.victimPageNumber = victimPageNumber;
        this.victimDirty = victimDirty;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public int getVictimProcessId() {
        return victimProcessId;
    }

    public int getVictimPageNumber() {
        return victimPageNumber;
    }

    public boolean wasVictimDirty() {
        return victimDirty;
    }

    @Override
    public String toString() {
        return "F" + frameNumber
                + " replaced P" + victimProcessId
                + " page " + victimPageNumber
                + (victimDirty ? " dirty" : " clean");
    }
}