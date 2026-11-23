package ru.medgraph.graph;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "MedGraph Graph Service", version = "0.1.0"))
public class GraphServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphServiceApplication.class, args);
    }
}
