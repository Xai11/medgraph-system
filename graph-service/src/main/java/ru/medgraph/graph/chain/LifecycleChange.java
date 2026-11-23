package ru.medgraph.graph.chain;

import java.time.Instant;
import java.util.UUID;

public record LifecycleChange(UUID factId, String objectId, long batchId, Instant changedAt,
                              Type type, Integer previousPosition, Integer position) {

    public enum Type {
        REGISTERED,
        POSITION_CHANGED,
        DELETED
    }
}
