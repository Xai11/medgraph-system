package ru.medgraph.graph.chain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class FactLifecycleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID factId;
    private String objectId;
    private long batchId;
    private Instant changedAt;

    @Enumerated(EnumType.STRING)
    private LifecycleChange.Type eventType;

    private Integer previousPosition;
    private Integer position;

    protected FactLifecycleEvent() {
    }

    FactLifecycleEvent(UUID factId, String objectId, BatchChanges batch, LifecycleChange.Type type,
                       Integer previousPosition, Integer position) {
        this.factId = factId;
        this.objectId = objectId;
        this.batchId = batch.batchId();
        this.changedAt = batch.changedAt();
        this.eventType = type;
        this.previousPosition = previousPosition;
        this.position = position;
    }

    LifecycleChange view() {
        return new LifecycleChange(factId, objectId, batchId, changedAt, eventType, previousPosition, position);
    }
}
