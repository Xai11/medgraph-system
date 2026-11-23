package ru.medgraph.fact.batch;

import java.util.UUID;

public record DeletedFact(UUID id, String objectId) {
}
