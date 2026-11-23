package ru.medgraph.fact.batch;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class Fact {

    @Id
    private UUID id;
    private String objectId;
    private Instant occurredAt;
    private String eventType;

    protected Fact() {
    }

    Fact(FactView view) {
        this.id = view.id();
        this.objectId = view.objectId();
        this.occurredAt = view.occurredAt();
        this.eventType = view.eventType();
    }
}
