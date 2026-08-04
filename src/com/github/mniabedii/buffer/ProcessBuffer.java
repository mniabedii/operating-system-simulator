package com.github.mniabedii.buffer;

import com.github.mniabedii.process.PCB;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public class ProcessBuffer {

    private final Queue<PCB> queue;
    private final int capacity;

    public ProcessBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Buffer capacity must be positive");
        }

        this.capacity = capacity;
        this.queue = new ArrayDeque<>();
    }

    public synchronized void putOnBuffer(PCB process)
            throws InterruptedException {

        Objects.requireNonNull(process, "process");

        while (queue.size() >= capacity) {
            wait();
        }

        queue.offer(process);
        notifyAll();
    }

    public synchronized PCB takeFromBuffer()
            throws InterruptedException {

        while (queue.isEmpty()) {
            wait();
        }

        PCB process = queue.poll();
        notifyAll();

        return process;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}