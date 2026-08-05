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

    private void validatePageNumber(int pageNumber) {
        if (pageNumber < 0 || pageNumber >= entries.size()) {
            throw new IllegalArgumentException(
                    "Invalid page number: " + pageNumber);
        }
    }
}