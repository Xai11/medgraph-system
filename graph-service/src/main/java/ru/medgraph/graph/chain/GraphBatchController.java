package ru.medgraph.graph.chain;

import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@RestController
class GraphBatchController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphBatchController.class);
    private static final String TRANSFER_LOG = "event=batch_transfer batch_id={} method=GET from=graph-service "
            + "to=fact-service result={} http_status={} added={} deleted={}";

    private final RestTemplate factServiceRestTemplate;
    private final GraphProjection projection;

    GraphBatchController(RestTemplate factServiceRestTemplate, GraphProjection projection) {
        this.factServiceRestTemplate = factServiceRestTemplate;
        this.projection = projection;
    }

    @PostMapping("/internal/graph/batches/{batchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Принять уведомление и синхронно применить порцию изменений")
    void apply(@PathVariable long batchId) {
        ResponseEntity<BatchChanges> response;
        try {
            response = factServiceRestTemplate.getForEntity("/internal/facts/batches/{batchId}",
                    BatchChanges.class, batchId);
        } catch (RestClientException exception) {
            Object status = exception instanceof RestClientResponseException errorResponse
                    ? errorResponse.getStatusCode().value() : "-";
            LOGGER.warn(TRANSFER_LOG, batchId, "ERROR", status, "-", "-");
            throw exception;
        }
        BatchChanges batch = Objects.requireNonNull(response.getBody(), "Fact Service returned an empty batch");
        LOGGER.info(TRANSFER_LOG, batchId, "OK", response.getStatusCode().value(),
                batch.added().size(), batch.deleted().size());
        projection.apply(batch);
    }
}
