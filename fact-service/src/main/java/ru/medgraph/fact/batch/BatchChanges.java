package ru.medgraph.fact.batch;

import java.time.Instant;
import java.util.List;

public record BatchChanges(long batchId, Instant changedAt, List<FactView> added, List<DeletedFact> deleted) {
}
