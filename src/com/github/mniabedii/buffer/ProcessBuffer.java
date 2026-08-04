package com.github.mniabedii.buffer;

import com.github.mniabedii.process.PCB;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public class ProcessBuffer {

    private final Queue<PCB> queue;
    private final int capacity;

    private boolean closed;

    public ProcessBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Buffer capacity must be positive");
        }

        this.capacity = capacity;
        this.queue = new ArrayDeque<>();
        this.closed = false;
    }

    public synchronized void putOnBuffer(PCB pcb)
            throws InterruptedException {

        Objects.requireNonNull(pcb, "pcb");

        while (queue.size() >= capacity && !closed) {
            wait();
        }

        if (closed) {
            throw new IllegalStateException(
                    "Cannot add to a closed buffer");
        }

        queue.offer(pcb);
        notifyAll();
    }

    public synchronized PCB takeFromBuffer()
            throws InterruptedException {

        while (queue.isEmpty() && !closed) {
            wait();
        }

        if (queue.isEmpty()) {
            return null;
        }

        PCB pcb = queue.poll();
        notifyAll();

        return pcb;
    }

    public synchronized void closeBuffer() {
        closed = true;
        notifyAll();
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}