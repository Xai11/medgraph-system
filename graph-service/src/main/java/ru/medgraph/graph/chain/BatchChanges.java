package ru.medgraph.graph.chain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BatchChanges(long batchId, Instant changedAt, List<Fact> added, List<DeletedFact> deleted) {

    public record Fact(UUID id, String objectId, Instant occurredAt, String eventType) {
    }

    public record DeletedFact(UUID id, String objectId) {
    }
}
