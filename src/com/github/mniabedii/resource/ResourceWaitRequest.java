package com.github.mniabedii.resource;

import com.github.mniabedii.process.PCB;

import java.util.Arrays;
import java.util.Objects;

public class ResourceWaitRequest {

    private final PCB pcb;
    private final int[] request;
    private final int requestTick;

    public ResourceWaitRequest(
            PCB pcb,
            int[] request,
            int requestTick) {

        this.pcb = Objects.requireNonNull(pcb, "pcb");

        if (request == null || request.length != 3) {
            throw new IllegalArgumentException(
                    "Resource request must contain three values");
        }

        boolean requestsSomething = false;

        for (int value : request) {
            if (value < 0) {
                throw new IllegalArgumentException(
                        "Resource request cannot be negative");
            }

            if (value > 0) {
                requestsSomething = true;
            }
        }

        if (!requestsSomething) {
            throw new IllegalArgumentException(
                    "Resource request cannot be all zero");
        }

        if (requestTick < 0) {
            throw new IllegalArgumentException(
                    "Request tick cannot be negative");
        }

        this.request = Arrays.copyOf(
                request,
                request.length);

        this.requestTick = requestTick;
    }

    public PCB getPCB() {
        return pcb;
    }

    public int[] getRequest() {
        return Arrays.copyOf(
                request,
                request.length);
    }

    public int getRequestTick() {
        return requestTick;
    }

    @Override
    public String toString() {
        return "P" + pcb.getPid()
                + " requests "
                + Arrays.toString(request)
                + " since tick "
                + requestTick;
    }
}