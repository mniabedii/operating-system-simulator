package com.github.mniabedii.resource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

public class ResourceWaitQueue {

    private final Queue<ResourceWaitRequest> requests;

    public ResourceWaitQueue() {
        this.requests = new ArrayDeque<>();
    }

    public synchronized void addRequest(
            ResourceWaitRequest request) {

        Objects.requireNonNull(request, "request");

        int processId = request.getPCB().getPid();

        if (containsProcess(processId)) {
            throw new IllegalStateException(
                    "P" + processId
                            + " already has a pending resource request");
        }

        requests.offer(request);
    }

    public synchronized boolean removeRequest(
            ResourceWaitRequest request) {

        return requests.remove(request);
    }

    public synchronized boolean containsProcess(
            int processId) {

        for (ResourceWaitRequest request : requests) {
            if (request.getPCB().getPid() == processId) {
                return true;
            }
        }

        return false;
    }

    public synchronized List<ResourceWaitRequest> getSnapshot() {
        return new ArrayList<>(requests);
    }

    public synchronized boolean isEmpty() {
        return requests.isEmpty();
    }

    public synchronized int size() {
        return requests.size();
    }

    public synchronized String getStatus() {
        if (requests.isEmpty()) {
            return "Resource Wait Queue: empty";
        }

        StringBuilder result = new StringBuilder("Resource Wait Queue:");

        for (ResourceWaitRequest request : requests) {
            result.append(System.lineSeparator())
                    .append(request);
        }

        return result.toString();
    }
}