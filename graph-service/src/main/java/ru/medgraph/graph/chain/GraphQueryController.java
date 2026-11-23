package ru.medgraph.graph.chain;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
class GraphQueryController {

    private final GraphSnapshotRepository snapshots;
    private final FactLifecycleRepository lifecycle;

    GraphQueryController(GraphSnapshotRepository snapshots, FactLifecycleRepository lifecycle) {
        this.snapshots = snapshots;
        this.lifecycle = lifecycle;
    }

    @GetMapping("/api/graph")
    @Operation(summary = "Получить текущие непустые цепочки всех объектов")
    List<GraphView> current() {
        return snapshots.findLatestSnapshots().stream()
                .filter(snapshot -> !snapshot.facts().isEmpty())
                .map(GraphSnapshot::view)
                .toList();
    }

    @GetMapping("/api/graph/objects/{objectId}")
    @Operation(summary = "Получить текущую цепочку объекта или её состояние на конец указанной даты UTC")
    GraphView chain(@PathVariable String objectId,
                    @Parameter(description = "Дата в формате YYYY-MM-DD, конец дня UTC")
                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var snapshot = date == null
                ? snapshots.findFirstByObjectIdOrderByBatchIdDesc(objectId)
                : snapshots.findFirstByObjectIdAndChangedAtLessThanOrderByBatchIdDesc(
                        objectId, date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        return snapshot.map(GraphSnapshot::view)
                .orElseGet(() -> GraphView.from(objectId, null, null, List.of()));
    }

    @GetMapping("/api/graph/objects/{objectId}/history")
    @Operation(summary = "Получить версии цепочки объекта по порядку применения порций")
    List<GraphView> history(@PathVariable String objectId) {
        return snapshots.findAllByObjectIdOrderByBatchIdAsc(objectId).stream()
                .map(GraphSnapshot::view)
                .toList();
    }

    @GetMapping("/api/graph/facts/{factId}/lifecycle")
    @Operation(summary = "Получить регистрацию, смены позиции и удаление факта")
    List<LifecycleChange> lifecycle(@PathVariable UUID factId) {
        List<FactLifecycleEvent> events = lifecycle.findAllByFactIdOrderByBatchIdAsc(factId);
        if (events.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fact not found in the graph history");
        }
        return events.stream().map(FactLifecycleEvent::view).toList();
    }
}
