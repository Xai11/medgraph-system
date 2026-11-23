package ru.medgraph.fact.batch;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
class FactBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant changedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<FactView> added;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<DeletedFact> deleted;

    protected FactBatch() {
    }

    FactBatch(Instant changedAt, List<FactView> added, List<DeletedFact> deleted) {
        this.changedAt = changedAt;
        this.added = added;
        this.deleted = deleted;
    }

    BatchChanges changes() {
        return new BatchChanges(id, changedAt, added, deleted);
    }
}
