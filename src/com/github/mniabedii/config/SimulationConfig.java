package com.github.mniabedii.config;

import com.github.mniabedii.memory.PageReplacementPolicy;

import java.util.Arrays;

public final class SimulationConfig {

    public static final int MAX_PROCESSES = 15;
    public static final int PROCESS_BUFFER_CAPACITY = 7;

    public static final int MIN_GENERATION_INTERVAL = 1;
    public static final int MAX_GENERATION_INTERVAL = 3;

    public static final int MIN_BURST_TIME = 4;
    public static final int MAX_BURST_TIME = 12;

    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 5;

    public static final int MIN_PROCESS_PAGES = 6;
    public static final int MAX_PROCESS_PAGES = 12;

    public static final int ROUND_ROBIN_QUANTUM = 3;
    public static final int CONTEXT_SWITCH_TICKS = 2;
    public static final int DISK_IO_TICKS = 5;

    public static final int FRAME_COUNT = 12;
    public static final int TLB_CAPACITY = 3;

    public static final PageReplacementPolicy PAGE_REPLACEMENT_POLICY = PageReplacementPolicy.FIFO;

    private static final int[] TOTAL_RESOURCES = { 2, 1, 3 };

    private SimulationConfig() {
    }

    public static int[] getTotalResources() {
        return Arrays.copyOf(
                TOTAL_RESOURCES,
                TOTAL_RESOURCES.length);
    }
}