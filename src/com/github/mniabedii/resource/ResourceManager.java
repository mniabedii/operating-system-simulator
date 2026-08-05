package com.github.mniabedii.resource;

import com.github.mniabedii.process.PCB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResourceManager {

    private final int[] totalResources;
    private final int[] availableResources;

    private final Map<Integer, PCB> processes;
    private final Map<Integer, int[]> allocations;

    public ResourceManager(int[] totalResources) {
        validateTotalResources(totalResources);

        this.totalResources = Arrays.copyOf(
                totalResources,
                totalResources.length);

        this.availableResources = Arrays.copyOf(
                totalResources,
                totalResources.length);

        this.processes = new LinkedHashMap<>();
        this.allocations = new LinkedHashMap<>();
    }

    public synchronized void registerProcess(PCB pcb) {
        Objects.requireNonNull(pcb, "pcb");

        if (processes.containsKey(pcb.getPid())) {
            throw new IllegalStateException(
                    "P" + pcb.getPid()
                            + " is already registered");
        }

        int[] maximumDemand = pcb.getMaxResourceDemand();

        validateMaximumDemand(
                pcb,
                maximumDemand);

        processes.put(pcb.getPid(), pcb);

        allocations.put(
                pcb.getPid(),
                new int[totalResources.length]);
    }

    public synchronized ResourceRequestResult requestResources(
            PCB pcb,
            int[] request) {

        validateRegisteredProcess(pcb);
        validateRequestVector(request);

        int[] allocation = allocations.get(pcb.getPid());

        int[] maximumDemand = pcb.getMaxResourceDemand();

        for (int i = 0; i < request.length; i++) {
            int remainingNeed = maximumDemand[i] - allocation[i];

            if (request[i] > remainingNeed) {
                return ResourceRequestResult.EXCEEDS_MAXIMUM;
            }
        }

        for (int i = 0; i < request.length; i++) {
            if (request[i] > availableResources[i]) {
                return ResourceRequestResult.NOT_AVAILABLE;
            }
        }

        for (int i = 0; i < request.length; i++) {
            availableResources[i] -= request[i];
            allocation[i] += request[i];
        }

        return ResourceRequestResult.GRANTED;
    }

    public synchronized void releaseAllResources(PCB pcb) {
        validateRegisteredProcess(pcb);

        int[] allocation = allocations.get(pcb.getPid());

        for (int i = 0; i < availableResources.length; i++) {

            availableResources[i] += allocation[i];
        }

        allocations.remove(pcb.getPid());
        processes.remove(pcb.getPid());
    }

    public synchronized boolean isRegistered(PCB pcb) {
        Objects.requireNonNull(pcb, "pcb");

        return processes.containsKey(pcb.getPid());
    }

    public synchronized int[] getAvailableResources() {
        return Arrays.copyOf(
                availableResources,
                availableResources.length);
    }

    public synchronized int[] getAllocation(PCB pcb) {
        validateRegisteredProcess(pcb);

        int[] allocation = allocations.get(pcb.getPid());

        return Arrays.copyOf(
                allocation,
                allocation.length);
    }

    public synchronized int[] getRemainingNeed(PCB pcb) {
        validateRegisteredProcess(pcb);

        int[] maximumDemand = pcb.getMaxResourceDemand();

        int[] allocation = allocations.get(pcb.getPid());

        int[] need = new int[totalResources.length];

        for (int i = 0; i < need.length; i++) {
            need[i] = maximumDemand[i] - allocation[i];
        }

        return need;
    }

    private void validateRegisteredProcess(PCB pcb) {
        Objects.requireNonNull(pcb, "pcb");

        if (!processes.containsKey(pcb.getPid())) {
            throw new IllegalStateException(
                    "P" + pcb.getPid()
                            + " is not registered");
        }
    }

    private void validateMaximumDemand(
            PCB pcb,
            int[] maximumDemand) {

        if (maximumDemand.length != totalResources.length) {

            throw new IllegalArgumentException(
                    "Invalid maximum-demand vector for P"
                            + pcb.getPid());
        }

        for (int i = 0; i < maximumDemand.length; i++) {

            if (maximumDemand[i] < 0) {
                throw new IllegalArgumentException(
                        "Maximum demand cannot be negative");
            }

            if (maximumDemand[i] > totalResources[i]) {
                throw new IllegalArgumentException(
                        "P" + pcb.getPid()
                                + " demands more R"
                                + (i + 1)
                                + " than the system owns");
            }
        }
    }

    private void validateRequestVector(int[] request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Resource request cannot be null");
        }

        if (request.length != totalResources.length) {
            throw new IllegalArgumentException(
                    "Resource request must contain "
                            + totalResources.length
                            + " values");
        }

        for (int value : request) {
            if (value < 0) {
                throw new IllegalArgumentException(
                        "Resource request cannot be negative");
            }
        }
    }

    private void validateTotalResources(
            int[] totalResources) {

        if (totalResources == null
                || totalResources.length == 0) {

            throw new IllegalArgumentException(
                    "Total resources cannot be empty");
        }

        for (int value : totalResources) {
            if (value < 0) {
                throw new IllegalArgumentException(
                        "Total resources cannot be negative");
            }
        }
    }

    public synchronized List<PCB> getRegisteredProcessesSnapshot() {

        return new ArrayList<>(processes.values());
    }
}