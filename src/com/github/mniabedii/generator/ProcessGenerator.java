package com.github.mniabedii.generator;

import com.github.mniabedii.buffer.ProcessBuffer;
import com.github.mniabedii.clock.SimulationClock;
import com.github.mniabedii.config.SimulationConfig;
import com.github.mniabedii.process.PCB;
import com.github.mniabedii.process.ProcessType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ProcessGenerator implements Runnable {

    private final ProcessBuffer processBuffer;
    private final SimulationClock clock;
    private final Random random;
    private final int maxProcesses;

    private volatile boolean finished;

    public ProcessGenerator(
            ProcessBuffer processBuffer,
            SimulationClock clock,
            int maxProcesses) {

        if (maxProcesses <= 0) {
            throw new IllegalArgumentException(
                    "Maximum process count must be positive");
        }

        this.processBuffer = processBuffer;
        this.clock = clock;
        this.maxProcesses = maxProcesses;
        this.random = new Random();
        this.finished = false;
    }

    @Override
    public void run() {
        try {
            for (int pid = 1; pid <= maxProcesses; pid++) {
                waitForNextGeneration();

                PCB pcb = createPCB(pid);
                processBuffer.putOnBuffer(pcb);

                printGeneratedProcess(pcb);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            finished = true;
            processBuffer.closeBuffer();
        }
    }

    public boolean isFinished() {
        return finished;
    }

    private void waitForNextGeneration()
            throws InterruptedException {

        int interval = randomBetween(
                SimulationConfig.MIN_GENERATION_INTERVAL,
                SimulationConfig.MAX_GENERATION_INTERVAL);

        int targetTick = clock.getCurrentTick() + interval;
        clock.waitUntilTick(targetTick);
    }

    private PCB createPCB(int pid) {
        ProcessType type = generateProcessType();

        int burstTime = randomBetween(
                SimulationConfig.MIN_BURST_TIME,
                SimulationConfig.MAX_BURST_TIME);

        int priority = randomBetween(
                SimulationConfig.MIN_PRIORITY,
                SimulationConfig.MAX_PRIORITY);

        int requiredPages = randomBetween(
                SimulationConfig.MIN_PROCESS_PAGES,
                SimulationConfig.MAX_PROCESS_PAGES);

        List<Integer> pageReferences = generatePageReferenceString(
                requiredPages,
                burstTime);

        int[] maxResourceDemand = generateResourceDemand();

        return new PCB(
                pid,
                type,
                clock.getCurrentTick(),
                burstTime,
                priority,
                requiredPages,
                pageReferences,
                maxResourceDemand);
    }

    private ProcessType generateProcessType() {
        int value = random.nextInt(100);

        if (value < 20) {
            return ProcessType.SYSTEM;
        }

        if (value < 60) {
            return ProcessType.INTERACTIVE;
        }

        return ProcessType.BACKGROUND;
    }

    private List<Integer> generatePageReferenceString(
            int requiredPages,
            int length) {

        List<Integer> references = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            references.add(random.nextInt(requiredPages));
        }

        return references;
    }

    private int[] generateResourceDemand() {
        int[] totalResources = SimulationConfig.getTotalResources();

        int[] demand = new int[totalResources.length];

        for (int i = 0; i < totalResources.length; i++) {
            demand[i] = random.nextInt(totalResources[i] + 1);
        }

        return demand;
    }

    private int randomBetween(int minimum, int maximum) {
        return random.nextInt(maximum - minimum + 1)
                + minimum;
    }

    private void printGeneratedProcess(PCB pcb) {
        System.out.printf(
                "[Tick %d] Generated P%d"
                        + " type=%s"
                        + " burst=%d"
                        + " priority=%d"
                        + " pages=%d"
                        + " resources=%s%n",
                clock.getCurrentTick(),
                pcb.getPid(),
                pcb.getType(),
                pcb.getTotalBurstTime(),
                pcb.getPriority(),
                pcb.getRequiredPages(),
                Arrays.toString(
                        pcb.getMaxResourceDemand()));
    }
}