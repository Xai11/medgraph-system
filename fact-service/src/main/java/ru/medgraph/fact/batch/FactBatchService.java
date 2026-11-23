package ru.medgraph.fact.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Service
public class FactBatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FactBatchService.class);
    private static final String TRANSFER_LOG = "event=batch_transfer batch_id={} method=POST from=fact-service "
            + "to=graph-service result={} http_status={} added={} deleted={}";
    private static final List<String> OBJECTS = List.of("DEVICE-001", "DEVICE-002", "DEVICE-003");
    private static final List<String> EVENT_TYPES = List.of("INSPECTED", "CALIBRATED", "MAINTAINED", "OPERATED");

    private final FactRepository facts;
    private final FactBatchRepository batches;
    private final TransactionTemplate transaction;
    private final RestTemplate graphServiceRestTemplate;

    FactBatchService(FactRepository facts, FactBatchRepository batches,
                     TransactionTemplate transaction, RestTemplate graphServiceRestTemplate) {
        this.facts = facts;
        this.batches = batches;
        this.transaction = transaction;
        this.graphServiceRestTemplate = graphServiceRestTemplate;
    }

    public BatchChanges get(long batchId) {
        return batches.findById(batchId)
                .map(FactBatch::changes)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found"));
    }

    public synchronized void runAutomaticBatch() {
        var random = ThreadLocalRandom.current();
        if (random.nextBoolean() || facts.count() == 0) {
            runBatch(BatchOperation.ADD, random.nextInt(2, 6), null);
        } else {
            runBatch(BatchOperation.DELETE, random.nextInt(1, 4), null);
        }
    }

    public synchronized BatchChanges runBatch(BatchOperation operation, int size, String objectId) {
        BatchChanges batch = transaction.execute(status -> saveBatch(operation, size, objectId));
        try {
            var response = graphServiceRestTemplate.postForEntity(
                    "/internal/graph/batches/{batchId}", null, Void.class, batch.batchId());
            LOGGER.info(TRANSFER_LOG, batch.batchId(), "OK", response.getStatusCode().value(),
                    batch.added().size(), batch.deleted().size());
        } catch (RestClientException exception) {
            Object status = exception instanceof RestClientResponseException response
                    ? response.getStatusCode().value() : "-";
            LOGGER.warn(TRANSFER_LOG, batch.batchId(), "ERROR", status, batch.added().size(), batch.deleted().size());
            throw exception;
        }
        return batch;
    }

    private BatchChanges saveBatch(BatchOperation operation, int size, String objectId) {
        Instant changedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        List<FactView> added = List.of();
        List<DeletedFact> deleted = List.of();
        if (operation == BatchOperation.ADD) {
            added = IntStream.range(0, size)
                    .mapToObj(index -> generate(changedAt, objectId))
                    .toList();
            facts.saveAll(added.stream().map(Fact::new).toList());
        } else {
            deleted = facts.randomFacts(objectId, PageRequest.ofSize(size));
            if (deleted.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No matching facts to delete");
            }
            facts.deleteAllByIdInBatch(deleted.stream().map(DeletedFact::id).toList());
        }
        return batches.save(new FactBatch(changedAt, added, deleted)).changes();
    }

    private FactView generate(Instant now, String requestedObjectId) {
        var random = ThreadLocalRandom.current();
        String objectId = requestedObjectId;
        if (objectId == null) {
            objectId = OBJECTS.get(random.nextInt(OBJECTS.size()));
        }
        Instant occurredAt = now.minusSeconds(random.nextLong(1, 30L * 24 * 60 * 60));
        String eventType = EVENT_TYPES.get(random.nextInt(EVENT_TYPES.size()));
        return new FactView(UUID.randomUUID(), objectId, occurredAt, eventType);
    }
}
