package com.github.mniabedii.resource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Iterator;

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

    private synchronized boolean containsProcess(
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

    public synchronized ResourceWaitRequest removeProcess(int processId) {

        Iterator<ResourceWaitRequest> iterator = requests.iterator();

        while (iterator.hasNext()) {
            ResourceWaitRequest request = iterator.next();

            if (request.getPCB().getPid() == processId) {
                iterator.remove();
                return request;
            }
        }

        return null;
    }
}