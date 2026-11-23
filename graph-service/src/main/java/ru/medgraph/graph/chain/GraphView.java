package ru.medgraph.graph.chain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public record GraphView(String objectId, Long batchId, Instant changedAt, List<Node> nodes, List<Edge> edges) {

    public record Node(UUID id, Instant occurredAt, String eventType, int position) {
    }

    public record Edge(UUID from, UUID to) {
    }

    static GraphView from(String objectId, Long batchId, Instant changedAt, List<BatchChanges.Fact> facts) {
        List<Node> nodes = IntStream.range(0, facts.size())
                .mapToObj(index -> {
                    BatchChanges.Fact fact = facts.get(index);
                    return new Node(fact.id(), fact.occurredAt(), fact.eventType(), index + 1);
                })
                .toList();
        List<Edge> edges = IntStream.range(1, nodes.size())
                .mapToObj(index -> new Edge(nodes.get(index - 1).id(), nodes.get(index).id()))
                .toList();
        return new GraphView(objectId, batchId, changedAt, nodes, edges);
    }
}
