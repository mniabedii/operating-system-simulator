package com.github.mniabedii.resource;

public enum ResourceRequestResult {
    GRANTED, // the requested resources were allocated.
    NOT_AVAILABLE, // the request is valid, but the system
                   // does not currently have enough free instances
    EXCEEDS_MAXIMUM // the process requested more than its declared remaining need
}