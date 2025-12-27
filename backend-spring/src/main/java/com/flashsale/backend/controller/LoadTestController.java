package com.flashsale.backend.controller;

import com.flashsale.backend.logging.LoggerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping("/api/loadtest")
public class LoadTestController {
    private final WebClient client;
    private final LoggerService log;
    private final String serviceBase;

    public LoadTestController(LoggerService log, @Value("${loadtest.service-base:http://loadtest-service:8080}") String serviceBase) {
        this.client = WebClient.builder().baseUrl(serviceBase).build();
        this.log = log;
        this.serviceBase = serviceBase;
    }

    @PostMapping(path = "/tasks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> start(@RequestBody Map<String, Object> body) {
        log.info("loadtest_start_forward", Map.of("serviceBase", serviceBase));
        try {
            var resp = client.post().uri("/run").contentType(MediaType.APPLICATION_JSON).bodyValue(body).retrieve().toEntity(String.class).block();
            if (resp == null) return ResponseEntity.status(502).body(Map.of("error", "No response from loadtest service"));
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping(path = "/tasks/{id}")
    public ResponseEntity<?> status(@PathVariable("id") String id) {
        try {
            var resp = client.get().uri("/status/" + id).retrieve().toEntity(String.class).block();
            if (resp == null) return ResponseEntity.status(502).body(Map.of("error", "No response from loadtest service"));
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    @DeleteMapping(path = "/tasks/{id}")
    public ResponseEntity<?> stop(@PathVariable("id") String id) {
        try {
            var resp = client.post().uri("/stop/" + id).retrieve().toEntity(String.class).block();
            if (resp == null) return ResponseEntity.status(502).body(Map.of("error", "No response from loadtest service"));
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping(path = "/tasks/{id}/results")
    public ResponseEntity<?> results(@PathVariable("id") String id) {
        try {
            var resp = client.get().uri("/results/" + id).retrieve().toEntity(String.class).block();
            if (resp == null) return ResponseEntity.status(502).body(Map.of("error", "No response from loadtest service"));
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }
}
