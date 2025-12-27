package com.flashsale.loadtest.controller;

import com.flashsale.loadtest.service.LoadRunnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class LoadRunnerController {
    private final LoadRunnerService service;

    public LoadRunnerController(LoadRunnerService service) {
        this.service = service;
    }

    @PostMapping("/run")
    public ResponseEntity<?> run(@RequestBody Map<String, Object> body) {
        String id = service.start(body);
        return ResponseEntity.ok(Map.of("taskId", id));
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<?> status(@PathVariable("id") String id) {
        var s = service.status(id);
        if (s == null) return ResponseEntity.status(404).body(Map.of("error", "Task not found"));
        return ResponseEntity.ok(s);
    }

    @PostMapping("/stop/{id}")
    public ResponseEntity<?> stop(@PathVariable("id") String id) {
        boolean ok = service.stop(id);
        return ResponseEntity.ok(Map.of("stopped", ok));
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<?> results(@PathVariable("id") String id) {
        var r = service.results(id);
        if (r == null) return ResponseEntity.status(404).body(Map.of("error", "Task not found"));
        return ResponseEntity.ok(r);
    }
}

