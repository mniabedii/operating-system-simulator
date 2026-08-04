package com.github.mniabedii.memory;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class TLB {

    public static final int NO_FRAME = -1;

    private final Queue<TLBEntry> entries;
    private final int capacity;

    private int hitCount;
    private int missCount;

    public TLB(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "TLB capacity must be positive");
        }

        this.capacity = capacity;
        this.entries = new ArrayDeque<>();
        this.hitCount = 0;
        this.missCount = 0;
    }

    public synchronized int lookup(
            int processId,
            int pageNumber) {

        for (TLBEntry entry : entries) {
            if (entry.matches(processId, pageNumber)) {
                hitCount++;
                return entry.getFrameNumber();
            }
        }

        missCount++;
        return NO_FRAME;
    }

    public synchronized void addEntry(
            int processId,
            int pageNumber,
            int frameNumber) {

        removeEntry(processId, pageNumber);

        if (entries.size() >= capacity) {
            entries.poll();
        }

        entries.offer(
                new TLBEntry(
                        processId,
                        pageNumber,
                        frameNumber));
    }

    public synchronized void removeEntry(
            int processId,
            int pageNumber) {

        Iterator<TLBEntry> iterator = entries.iterator();

        while (iterator.hasNext()) {
            TLBEntry entry = iterator.next();

            if (entry.matches(processId, pageNumber)) {
                iterator.remove();
                return;
            }
        }
    }

    public synchronized void removeProcessEntries(
            int processId) {

        Iterator<TLBEntry> iterator = entries.iterator();

        while (iterator.hasNext()) {
            TLBEntry entry = iterator.next();

            if (entry.getProcessId() == processId) {
                iterator.remove();
            }
        }
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized int getHitCount() {
        return hitCount;
    }

    public synchronized int getMissCount() {
        return missCount;
    }

    public synchronized int getLookupCount() {
        return hitCount + missCount;
    }

    public synchronized double getHitRate() {
        int totalLookups = getLookupCount();

        if (totalLookups == 0) {
            return 0.0;
        }

        return (double) hitCount / totalLookups;
    }

    public synchronized String getStatus() {
        if (entries.isEmpty()) {
            return "TLB: empty";
        }

        StringBuilder result = new StringBuilder("TLB: ");

        for (TLBEntry entry : entries) {
            if (result.length() > 5) {
                result.append(" | ");
            }

            result.append(entry);
        }

        return result.toString();
    }
}