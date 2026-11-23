package ru.medgraph.fact.batch;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface FactRepository extends JpaRepository<Fact, UUID> {

    @Query("""
            select new ru.medgraph.fact.batch.DeletedFact(f.id, f.objectId)
            from Fact f
            where (:objectId is null or f.objectId = :objectId)
            order by random()
            """)
    List<DeletedFact> randomFacts(String objectId, Pageable pageable);
}
