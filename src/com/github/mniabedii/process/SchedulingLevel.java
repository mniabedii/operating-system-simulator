package com.github.mniabedii.process;

public enum SchedulingLevel {
    SYSTEM,
    INTERACTIVE,
    BACKGROUND;

    public static SchedulingLevel fromProcessType(
            ProcessType type) {

        switch (type) {
            case SYSTEM:
                return SYSTEM;

            case INTERACTIVE:
                return INTERACTIVE;

            case BACKGROUND:
                return BACKGROUND;

            default:
                throw new IllegalStateException(
                        "Unknown process type: " + type);
        }
    }
}