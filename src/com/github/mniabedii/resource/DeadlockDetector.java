package com.github.mniabedii.resource;

import com.github.mniabedii.process.PCB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DeadlockDetector {

    /*
     * 1. Terminate the deadlocked process holding
     * the largest number of resource instances.
     * 
     * 2. If tied, terminate the process with the
     * largest remaining burst.
     * 
     * 3. If still tied, terminate the process
     * with the highest PID.
     */
    public List<PCB> detectDeadlockedProcesses(
            ResourceManager resourceManager,
            ResourceWaitQueue waitQueue) {

        Objects.requireNonNull(
                resourceManager,
                "resourceManager");

        Objects.requireNonNull(
                waitQueue,
                "waitQueue");

        List<PCB> processes = resourceManager
                .getRegisteredProcessesSnapshot();

        int[] work = resourceManager.getAvailableResources();

        Map<Integer, int[]> pendingRequests = createPendingRequestMap(
                resourceManager,
                waitQueue);

        Map<Integer, int[]> allocations = new LinkedHashMap<>();

        Map<Integer, Boolean> finished = new LinkedHashMap<>();

        for (PCB pcb : processes) {
            int[] allocation = resourceManager.getAllocation(pcb);

            allocations.put(
                    pcb.getPid(),
                    allocation);

            /*
             * A process holding no resources cannot
             * participate in a circular hold-and-wait.
             */
            finished.put(
                    pcb.getPid(),
                    isZeroVector(allocation));
        }

        boolean progressMade;

        do {
            progressMade = false;

            for (PCB pcb : processes) {
                int processId = pcb.getPid();

                if (finished.get(processId)) {
                    continue;
                }

                int[] request = pendingRequests.get(processId);

                if (request == null) {
                    request = new int[work.length];
                }

                if (canBeSatisfied(request, work)) {
                    addToWork(
                            work,
                            allocations.get(processId));

                    finished.put(processId, true);
                    progressMade = true;
                }
            }
        } while (progressMade);

        List<PCB> deadlockedProcesses = new ArrayList<>();

        for (PCB pcb : processes) {
            if (!finished.get(pcb.getPid())) {
                deadlockedProcesses.add(pcb);
            }
        }

        return deadlockedProcesses;
    }

    public PCB selectVictim(
            List<PCB> deadlockedProcesses,
            ResourceManager resourceManager) {

        Objects.requireNonNull(
                deadlockedProcesses,
                "deadlockedProcesses");

        Objects.requireNonNull(
                resourceManager,
                "resourceManager");

        if (deadlockedProcesses.isEmpty()) {
            throw new IllegalArgumentException(
                    "Deadlocked process list cannot be empty");
        }

        PCB selectedVictim = null;
        int largestAllocation = -1;
        int largestRemainingBurst = -1;
        int largestPid = -1;

        for (PCB pcb : deadlockedProcesses) {
            int allocationSize = sum(resourceManager.getAllocation(pcb));

            int remainingBurst = pcb.getRemainingBurstTime();

            boolean betterVictim = allocationSize > largestAllocation
                    || allocationSize == largestAllocation
                            && remainingBurst > largestRemainingBurst
                    || allocationSize == largestAllocation
                            && remainingBurst == largestRemainingBurst
                            && pcb.getPid() > largestPid;

            if (betterVictim) {
                selectedVictim = pcb;
                largestAllocation = allocationSize;
                largestRemainingBurst = remainingBurst;
                largestPid = pcb.getPid();
            }
        }

        return selectedVictim;
    }

    private Map<Integer, int[]> createPendingRequestMap(
            ResourceManager resourceManager,
            ResourceWaitQueue waitQueue) {

        Map<Integer, int[]> pendingRequests = new LinkedHashMap<>();

        for (ResourceWaitRequest request : waitQueue.getSnapshot()) {

            PCB pcb = request.getPCB();

            if (!resourceManager.isRegistered(pcb)) {
                throw new IllegalStateException(
                        "Resource wait queue contains"
                                + " unregistered P"
                                + pcb.getPid());
            }

            pendingRequests.put(
                    pcb.getPid(),
                    request.getRequest());
        }

        return pendingRequests;
    }

    private boolean canBeSatisfied(
            int[] request,
            int[] work) {

        if (request.length != work.length) {
            throw new IllegalArgumentException(
                    "Resource vector lengths do not match");
        }

        for (int i = 0; i < request.length; i++) {
            if (request[i] > work[i]) {
                return false;
            }
        }

        return true;
    }

    private boolean isZeroVector(int[] vector) {
        for (int value : vector) {
            if (value != 0) {
                return false;
            }
        }

        return true;
    }

    private void addToWork(
            int[] work,
            int[] allocation) {

        for (int i = 0; i < work.length; i++) {
            work[i] += allocation[i];
        }
    }

    private int sum(int[] vector) {
        int result = 0;

        for (int value : vector) {
            result += value;
        }

        return result;
    }
}