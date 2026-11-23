package com.example.tactech_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** WebClient reutilizable para llamadas HTTP no bloqueantes. */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder, @Value("${server.port:8080}") int puerto) {
        return builder.baseUrl("http://localhost:" + puerto).build();
    }
}
