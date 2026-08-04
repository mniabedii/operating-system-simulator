package com.github.mniabedii.disk;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public class PageFaultQueue {

    private final Queue<PageFaultRequest> requests;

    private boolean closed;
    private int activeRequestCount;

    public PageFaultQueue() {
        this.requests = new ArrayDeque<>();
        this.closed = false;
        this.activeRequestCount = 0;
    }

    public synchronized void putRequest(
            PageFaultRequest request) {

        Objects.requireNonNull(request, "request");

        if (closed) {
            throw new IllegalStateException(
                    "Cannot add to a closed page-fault queue");
        }

        requests.offer(request);
        notifyAll();
    }

    public synchronized PageFaultRequest takeRequest()
            throws InterruptedException {

        while (requests.isEmpty() && !closed) {
            wait();
        }

        if (requests.isEmpty()) {
            return null;
        }

        PageFaultRequest request = requests.poll();
        activeRequestCount++;

        return request;
    }

    public synchronized void completeRequest() {
        if (activeRequestCount <= 0) {
            throw new IllegalStateException(
                    "No active disk request exists");
        }

        activeRequestCount--;
        notifyAll();
    }

    public synchronized boolean hasPendingWork() {
        return !requests.isEmpty()
                || activeRequestCount > 0;
    }

    public synchronized int getActiveRequestCount() {
        return activeRequestCount;
    }

    public synchronized void closeQueue() {
        closed = true;
        notifyAll();
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public synchronized boolean isEmpty() {
        return requests.isEmpty();
    }

    public synchronized int size() {
        return requests.size();
    }
}