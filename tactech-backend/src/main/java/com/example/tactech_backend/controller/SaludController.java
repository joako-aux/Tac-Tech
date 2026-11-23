package com.example.tactech_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/** Para comprobar desde el navegador o Postman que el servicio esta arriba. */
@RestController
@RequestMapping("/api/salud")
@RequiredArgsConstructor
public class SaludController {

    private final WebClient webClient;

    @GetMapping
    public Map<String, Object> salud() {
        return Map.<String, Object>of("servicio", "tactech-backend", "estado", "OK", "fecha", LocalDateTime.now().toString());
    }

    /**
     * Ejemplo de WebClient: el servicio se llama a si mismo de forma reactiva (no bloqueante)
     * y responde si obtuvo respuesta antes de 3 segundos.
     */
    @GetMapping("/verificar")
    public Mono<Map<String, Object>> verificar() {
        return webClient.get()
                .uri("/api/salud")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(3))
                .map(respuesta -> Map.<String, Object>of("alcanzable", true, "respuesta", respuesta))
                .onErrorResume(e -> Mono.just(Map.<String, Object>of("alcanzable", false, "error", e.getClass().getSimpleName())));
    }
}
