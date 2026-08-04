package com.github.mniabedii.config;

public final class SimulationConfig {

    public static final int MAX_PROCESSES = 15;

    public static final int ROUND_ROBIN_QUANTUM = 3;
    public static final int CONTEXT_SWITCH_TICKS = 2;
    public static final int DISK_IO_TICKS = 5;

    public static final int FRAME_COUNT = 12;
    public static final int TLB_CAPACITY = 3;

    public static final int MIN_PROCESS_PAGES = 6;
    public static final int MAX_PROCESS_PAGES = 12;

    private SimulationConfig() {
        // empty to prevent accidental object instantiation
    }

    // function, because arrays are mutable
    public static int[] createResourceVector() {
        return new int[] { 2, 1, 3 };
    }
}