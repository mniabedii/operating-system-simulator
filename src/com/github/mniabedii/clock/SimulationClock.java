package com.github.mniabedii.clock;

public class SimulationClock {

    private int currentTick;

    public SimulationClock() {
        this.currentTick = 0;
    }

    public synchronized int getCurrentTick() {
        return currentTick;
    }

    public synchronized int advanceOneTick() {
        currentTick++;
        notifyAll();

        return currentTick;
    }

    public synchronized void waitUntilTick(int targetTick)
            throws InterruptedException {

        if (targetTick < 0) {
            throw new IllegalArgumentException(
                    "Target tick cannot be negative");
        }

        while (currentTick < targetTick) {
            wait();
        }
    }
}