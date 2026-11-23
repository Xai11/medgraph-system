package ru.medgraph.fact.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import ru.medgraph.fact.batch.FactBatchService;

import java.util.concurrent.ThreadLocalRandom;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "medgraph.generator.enabled", matchIfMissing = true)
class FactSchedulingConfiguration implements SchedulingConfigurer {

    private final FactBatchService batches;

    FactSchedulingConfiguration(FactBatchService batches) {
        this.batches = batches;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(batches::runAutomaticBatch, context ->
                context.getClock().instant().plusSeconds(ThreadLocalRandom.current().nextLong(15, 31)));
    }
}
