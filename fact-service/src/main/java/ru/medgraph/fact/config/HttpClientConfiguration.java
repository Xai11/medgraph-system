package ru.medgraph.fact.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
public class HttpClientConfiguration {

    @Bean
    RestTemplate graphServiceRestTemplate(
            RestTemplateBuilder builder,
            @Value("${medgraph.graph.base-url}") String graphServiceBaseUrl
    ) {
        return builder.rootUri(graphServiceBaseUrl).build();
    }
}
