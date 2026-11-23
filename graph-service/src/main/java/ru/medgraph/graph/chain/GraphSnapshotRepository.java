package ru.medgraph.graph.chain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface GraphSnapshotRepository extends JpaRepository<GraphSnapshot, Long> {

    Optional<GraphSnapshot> findFirstByObjectIdOrderByBatchIdDesc(String objectId);

    Optional<GraphSnapshot> findFirstByObjectIdAndChangedAtLessThanOrderByBatchIdDesc(String objectId, Instant cutoff);

    List<GraphSnapshot> findAllByObjectIdOrderByBatchIdAsc(String objectId);

    @Query(value = """
            select distinct on (object_id) *
            from graph_snapshot
            order by object_id, batch_id desc
            """, nativeQuery = true)
    List<GraphSnapshot> findLatestSnapshots();
}
