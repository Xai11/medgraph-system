package ru.medgraph.graph.chain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface FactLifecycleRepository extends JpaRepository<FactLifecycleEvent, Long> {

    List<FactLifecycleEvent> findAllByFactIdOrderByBatchIdAsc(UUID factId);
}
