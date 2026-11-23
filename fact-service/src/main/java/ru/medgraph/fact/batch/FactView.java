package ru.medgraph.fact.batch;

import java.time.Instant;
import java.util.UUID;

public record FactView(UUID id, String objectId, Instant occurredAt, String eventType) {
}
