package ru.medgraph.graph.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
public class HttpClientConfiguration {

    @Bean
    RestTemplate factServiceRestTemplate(
            RestTemplateBuilder builder,
            @Value("${medgraph.fact.base-url}") String factServiceBaseUrl
    ) {
        return builder.rootUri(factServiceBaseUrl).build();
    }
}
