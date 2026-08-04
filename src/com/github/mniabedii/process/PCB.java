package com.github.mniabedii.process;

import com.github.mniabedii.memory.PageTable;

import java.util.Arrays;
import java.util.List;

public class PCB {

    // final
    private final int pid;
    private final ProcessType type;
    private final int arrivalTime;
    private final int totalBurstTime;
    private final int priority;
    private final int requiredPages;

    private PageTable pageTable;

    private int remainingBurstTime;
    private ProcessState state;

    private final List<Integer> pageReferenceString;
    private final int[] maxResourceDemand;

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
        this.arrivalTime = arrivalTime;
        this.totalBurstTime = burstTime;
        this.remainingBurstTime = burstTime;
        this.priority = priority;
        this.requiredPages = requiredPages;
        this.pageTable = null;
        this.pageReferenceString = List.copyOf(pageReferenceString);
        this.maxResourceDemand = Arrays.copyOf(maxResourceDemand, maxResourceDemand.length);
        this.state = ProcessState.NEW;
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

    public int getRemainingBurstTime() {
        return remainingBurstTime;
    }

    public int getPriority() {
        return priority;
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
        this.state = state;
    }

    // other process functions
    public void executeOneTick() {
        if (remainingBurstTime > 0) {
            remainingBurstTime--;
        }
    }

    public boolean isFinished() {
        return remainingBurstTime == 0;
    }

    @Override
    public String toString() {
        return "P" + pid
                + "{type=" + type
                + ", state=" + state
                + ", remaining=" + remainingBurstTime
                + '}';
    }
}