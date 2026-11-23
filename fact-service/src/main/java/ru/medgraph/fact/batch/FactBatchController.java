package ru.medgraph.fact.batch;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@RestController
class FactBatchController {

    private static final Pattern OBJECT_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final FactBatchService batches;

    FactBatchController(FactBatchService batches) {
        this.batches = batches;
    }

    @PostMapping("/api/facts/batches")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Сгенерировать или удалить порцию фактов")
    BatchChanges change(@RequestParam BatchOperation operation, @RequestParam(defaultValue = "3") int size,
                        @RequestParam(required = false) String objectId) {
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100");
        }
        if (objectId != null && !OBJECT_ID.matcher(objectId).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid object ID");
        }
        return batches.runBatch(operation, size, objectId);
    }

    @GetMapping("/internal/facts/batches/{batchId}")
    @Operation(summary = "Получить сохранённую порцию изменений")
    BatchChanges get(@PathVariable long batchId) {
        return batches.get(batchId);
    }
}
