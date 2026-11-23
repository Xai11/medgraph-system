package ru.medgraph.fact.batch;

import org.springframework.data.jpa.repository.JpaRepository;

interface FactBatchRepository extends JpaRepository<FactBatch, Long> {
}
