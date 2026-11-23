package ru.medgraph.graph.chain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
class GraphProjection {

    private static final Comparator<BatchChanges.Fact> FACT_ORDER =
            Comparator.comparing(BatchChanges.Fact::occurredAt).thenComparing(BatchChanges.Fact::id);

    private final GraphSnapshotRepository snapshots;
    private final FactLifecycleRepository lifecycle;

    GraphProjection(GraphSnapshotRepository snapshots, FactLifecycleRepository lifecycle) {
        this.snapshots = snapshots;
        this.lifecycle = lifecycle;
    }

    @Transactional
    public void apply(BatchChanges batch) {
        var addedByObject = batch.added().stream().collect(Collectors.groupingBy(BatchChanges.Fact::objectId));
        var deletedByObject = batch.deleted().stream().collect(Collectors.groupingBy(BatchChanges.DeletedFact::objectId));
        var affectedObjects = new TreeSet<>(addedByObject.keySet());
        affectedObjects.addAll(deletedByObject.keySet());
        for (String objectId : affectedObjects) {
            updateChain(batch, objectId, addedByObject.getOrDefault(objectId, List.of()),
                    deletedByObject.getOrDefault(objectId, List.of()));
        }
    }

    private void updateChain(BatchChanges batch, String objectId, List<BatchChanges.Fact> added,
                             List<BatchChanges.DeletedFact> deleted) {
        List<BatchChanges.Fact> previous = snapshots.findFirstByObjectIdOrderByBatchIdDesc(objectId)
                .map(GraphSnapshot::facts)
                .orElseGet(List::of);
        Map<UUID, BatchChanges.Fact> currentById = new HashMap<>();
        Map<UUID, Integer> previousPositions = new HashMap<>();
        for (int index = 0; index < previous.size(); index++) {
            BatchChanges.Fact fact = previous.get(index);
            currentById.put(fact.id(), fact);
            previousPositions.put(fact.id(), index + 1);
        }

        for (BatchChanges.DeletedFact fact : deleted) {
            if (currentById.remove(fact.id()) != null) {
                lifecycle.save(new FactLifecycleEvent(fact.id(), objectId, batch, LifecycleChange.Type.DELETED,
                        previousPositions.get(fact.id()), null));
            }
        }
        for (BatchChanges.Fact fact : added) {
            currentById.put(fact.id(), fact);
        }

        List<BatchChanges.Fact> current = currentById.values().stream().sorted(FACT_ORDER).toList();
        for (int index = 0; index < current.size(); index++) {
            UUID factId = current.get(index).id();
            int position = index + 1;
            Integer previousPosition = previousPositions.get(factId);
            if (previousPosition == null) {
                lifecycle.save(new FactLifecycleEvent(factId, objectId, batch, LifecycleChange.Type.REGISTERED,
                        null, position));
            } else if (previousPosition != position) {
                lifecycle.save(new FactLifecycleEvent(factId, objectId, batch, LifecycleChange.Type.POSITION_CHANGED,
                        previousPosition, position));
            }
        }
        snapshots.save(new GraphSnapshot(objectId, batch, current));
    }
}
