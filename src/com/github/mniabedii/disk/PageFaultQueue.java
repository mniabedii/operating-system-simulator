package com.github.mniabedii.disk;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public class PageFaultQueue {

    private final Queue<PageFaultRequest> requests;

    private boolean closed;

    public PageFaultQueue() {
        this.requests = new ArrayDeque<>();
        this.closed = false;
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

        return requests.poll();
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