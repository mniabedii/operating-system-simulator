package com.github.mniabedii.memory;

import java.util.ArrayList;
import java.util.List;

public class PageTable {

    private final List<PageTableEntry> entries;

    public PageTable(int pageCount) {
        if (pageCount <= 0) {
            throw new IllegalArgumentException(
                    "Page count must be positive");
        }

        this.entries = new ArrayList<>();

        for (int pageNumber = 0; pageNumber < pageCount; pageNumber++) {

            entries.add(new PageTableEntry(pageNumber));
        }
    }

    public synchronized int getPageCount() {
        return entries.size();
    }

    public synchronized PageTableEntry getEntry(
            int pageNumber) {

        validatePageNumber(pageNumber);
        return entries.get(pageNumber);
    }

    public synchronized boolean isPagePresent(
            int pageNumber) {

        return getEntry(pageNumber).isPresent();
    }

    public synchronized void mapPageToFrame(
            int pageNumber,
            int frameNumber) {

        getEntry(pageNumber).mapToFrame(frameNumber);
    }

    public synchronized void unmapPage(int pageNumber) {
        getEntry(pageNumber).removeFromFrame();
    }

    public synchronized int getPresentPageCount() {
        int count = 0;

        for (PageTableEntry entry : entries) {
            if (entry.isPresent()) {
                count++;
            }
        }

        return count;
    }

    private void validatePageNumber(int pageNumber) {
        if (pageNumber < 0 || pageNumber >= entries.size()) {
            throw new IllegalArgumentException(
                    "Invalid page number: " + pageNumber);
        }
    }

    @Override
    public synchronized String toString() {
        StringBuilder result = new StringBuilder();

        for (PageTableEntry entry : entries) {
            if (result.length() > 0) {
                result.append(System.lineSeparator());
            }

            result.append(entry);
        }

        return result.toString();
    }
}