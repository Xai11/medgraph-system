package ru.medgraph.graph.chain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
class GraphSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String objectId;
    private long batchId;
    private Instant changedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<BatchChanges.Fact> facts;

    protected GraphSnapshot() {
    }

    GraphSnapshot(String objectId, BatchChanges batch, List<BatchChanges.Fact> facts) {
        this.objectId = objectId;
        this.batchId = batch.batchId();
        this.changedAt = batch.changedAt();
        this.facts = facts;
    }

    List<BatchChanges.Fact> facts() {
        return facts;
    }

    GraphView view() {
        return GraphView.from(objectId, batchId, changedAt, facts);
    }
}
