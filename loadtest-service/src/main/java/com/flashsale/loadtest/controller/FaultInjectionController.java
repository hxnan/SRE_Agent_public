package com.flashsale.loadtest.controller;

import com.flashsale.loadtest.service.FaultInjectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class FaultInjectionController {
    private final FaultInjectionService service;

    public FaultInjectionController(FaultInjectionService service) {
        this.service = service;
    }

    @GetMapping("/faults/db/deadlock/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(service.status());
    }

    @PostMapping("/faults/db/deadlock/lock")
    public ResponseEntity<?> lock(@RequestBody(required = false) Map<String, Object> body) {
        String table = null;
        if (body != null) {
            Object t = body.get("table_name");
            if (t != null) table = String.valueOf(t);
        }
        Map<String, Object> result = service.lock(table);
        boolean locked = Boolean.TRUE.equals(result.get("locked"));
        return locked ? ResponseEntity.ok(result) : ResponseEntity.status(500).body(result);
    }

    @PostMapping("/faults/db/deadlock/unlock")
    public ResponseEntity<?> unlock() {
        return ResponseEntity.ok(service.unlock());
    }
}
