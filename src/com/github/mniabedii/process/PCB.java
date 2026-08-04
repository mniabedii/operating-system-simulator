package com.github.mniabedii.process;

import com.github.mniabedii.memory.PageTable;

import java.util.Arrays;
import java.util.List;

public class PCB {

    // final
    private final int pid;
    private final ProcessType type;
    private volatile SchedulingLevel schedulingLevel;
    private int readyWaitTicks;
    private final int arrivalTime;
    private final int totalBurstTime;
    private final int priority;
    private final int requiredPages;

    private volatile PageTable pageTable;

    private volatile int remainingBurstTime;
    private volatile ProcessState state;

    private final List<Integer> pageReferenceString;
    private final int[] maxResourceDemand;

    private volatile WaitReason waitReason;

    private int firstRunTick;
    private int completionTick;
    private int totalReadyWaitTicks;

    public PCB(
            int pid,
            ProcessType type,
            int arrivalTime,
            int burstTime,
            int priority,
            int requiredPages,
            List<Integer> pageReferenceString,
            int[] maxResourceDemand) {

        if (pid <= 0) {
            throw new IllegalArgumentException("PID must be positive");
        }

        if (burstTime <= 0) {
            throw new IllegalArgumentException("Burst time must be positive");
        }

        if (requiredPages <= 0) {
            throw new IllegalArgumentException("Required pages must be positive");
        }

        if (maxResourceDemand.length != 3) {
            throw new IllegalArgumentException(
                    "Resource vector must contain three values");
        }

        this.pid = pid;
        this.type = type;
        this.schedulingLevel = SchedulingLevel.fromProcessType(type);
        this.readyWaitTicks = 0;
        this.arrivalTime = arrivalTime;
        this.totalBurstTime = burstTime;
        this.remainingBurstTime = burstTime;
        this.priority = priority;
        this.requiredPages = requiredPages;
        this.pageTable = null;

        if (pageReferenceString == null
                || pageReferenceString.isEmpty()) {

            throw new IllegalArgumentException(
                    "Page reference string cannot be empty");
        }

        for (Integer pageNumber : pageReferenceString) {
            if (pageNumber == null
                    || pageNumber < 0
                    || pageNumber >= requiredPages) {

                throw new IllegalArgumentException(
                        "Invalid page reference: " + pageNumber);
            }
        }

        this.pageReferenceString = List.copyOf(pageReferenceString);
        this.maxResourceDemand = Arrays.copyOf(maxResourceDemand, maxResourceDemand.length);
        this.state = ProcessState.NEW;
        this.waitReason = waitReason.NONE;

        this.firstRunTick = -1;
        this.completionTick = -1;
        this.totalReadyWaitTicks = 0;
    }

    // setters & getters
    public int getPid() {
        return pid;
    }

    public ProcessType getType() {
        return type;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getTotalBurstTime() {
        return totalBurstTime;
    }

    public synchronized int getRemainingBurstTime() {
        return remainingBurstTime;
    }

    public int getPriority() {
        return priority;
    }

    public SchedulingLevel getSchedulingLevel() {
        return schedulingLevel;
    }

    public synchronized int getReadyWaitTicks() {
        return readyWaitTicks;
    }

    public synchronized void incrementReadyWaitTicks() {
        if (state != ProcessState.READY) {
            throw new IllegalStateException(
                    "Only a READY process can age");
        }

        readyWaitTicks++;
        totalReadyWaitTicks++;
    }

    public synchronized void resetReadyWaitTicks() {
        readyWaitTicks = 0;
    }

    public synchronized boolean canBePromoted() {
        return schedulingLevel != SchedulingLevel.SYSTEM;
    }

    public synchronized void promoteOneLevel() {
        switch (schedulingLevel) {
            case BACKGROUND:
                schedulingLevel = SchedulingLevel.INTERACTIVE;
                break;

            case INTERACTIVE:
                schedulingLevel = SchedulingLevel.SYSTEM;
                break;

            case SYSTEM:
                throw new IllegalStateException(
                        "A SYSTEM-level process cannot be promoted");

            default:
                throw new IllegalStateException(
                        "Unknown scheduling level");
        }

        readyWaitTicks = 0;
    }

    public int getRequiredPages() {
        return requiredPages;
    }

    public boolean hasPageTable() {
        return pageTable != null;
    }

    public PageTable getPageTable() {
        if (pageTable == null) {
            throw new IllegalStateException(
                    "Page table has not been created for P" + pid);
        }

        return pageTable;
    }

    public void setPageTable(PageTable pageTable) {
        if (pageTable == null) {
            throw new IllegalArgumentException(
                    "Page table cannot be null");
        }

        if (this.pageTable != null) {
            throw new IllegalStateException(
                    "Page table already exists for P" + pid);
        }

        this.pageTable = pageTable;
    }

    public ProcessState getState() {
        return state;
    }

    public List<Integer> getPageReferenceString() {
        return pageReferenceString;
    }

    public int[] getMaxResourceDemand() {
        return Arrays.copyOf(
                maxResourceDemand,
                maxResourceDemand.length);
    }

    public void setState(ProcessState state) {
        if (state == null) {
            throw new IllegalArgumentException(
                    "Process state cannot be null");
        }

        this.state = state;
    }

    public WaitReason getWaitReason() {
        return waitReason;
    }

    public void setWaitReason(WaitReason waitReason) {
        if (waitReason == null) {
            throw new IllegalArgumentException(
                    "Wait reason cannot be null");
        }

        this.waitReason = waitReason;
    }

    // other process functions
    public synchronized void executeOneTick() {
        if (remainingBurstTime > 0) {
            remainingBurstTime--;
        }
    }

    public synchronized int getCurrentPageReference() {
        if (remainingBurstTime == 0) {
            throw new IllegalStateException(
                    "P" + pid + " has already finished");
        }

        int executedTicks = totalBurstTime - remainingBurstTime;

        int referenceIndex = executedTicks % pageReferenceString.size();

        return pageReferenceString.get(referenceIndex);
    }

    public synchronized boolean isFinished() {
        return remainingBurstTime == 0;
    }

    public synchronized void recordFirstRunTick(int tick) {
        if (tick < arrivalTime) {
            throw new IllegalArgumentException(
                    "First run tick cannot precede arrival");
        }

        if (firstRunTick == -1) {
            firstRunTick = tick;
        }
    }

    public synchronized void recordCompletionTick(int tick) {
        if (tick < arrivalTime) {
            throw new IllegalArgumentException(
                    "Completion tick cannot precede arrival");
        }

        if (completionTick != -1) {
            throw new IllegalStateException(
                    "Completion tick already recorded for P" + pid);
        }

        completionTick = tick;
    }

    public synchronized boolean hasStarted() {
        return firstRunTick >= 0;
    }

    public synchronized boolean hasCompleted() {
        return completionTick >= 0;
    }

    public synchronized int getFirstRunTick() {
        return firstRunTick;
    }

    public synchronized int getCompletionTick() {
        return completionTick;
    }

    public synchronized int getTotalReadyWaitTicks() {
        return totalReadyWaitTicks;
    }

    public synchronized int getResponseTime() {
        if (!hasStarted()) {
            return -1;
        }

        return firstRunTick - arrivalTime;
    }

    public synchronized int getTurnaroundTime() {
        if (!hasCompleted()) {
            return -1;
        }

        return completionTick - arrivalTime;
    }

    @Override
    public String toString() {
        return "P" + pid
                + "{type=" + type
                + ", level=" + schedulingLevel
                + ", state=" + state
                + ", waitReason=" + waitReason
                + ", remaining=" + remainingBurstTime
                + ", readyWait=" + readyWaitTicks
                + '}';
    }
}