package com.flashsale.backend.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LoggerService {
    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, Object> base() {
        Map<String, Object> m = new HashMap<>();
        m.put("timestamp", Instant.now().toString());
        m.put("service", "flashsale-backend");
        return m;
    }

    public void info(String message, Map<String, Object> ctx) {
        log("INFO", message, ctx);
    }
    public void warn(String message, Map<String, Object> ctx) {
        log("WARN", message, ctx);
    }
    public void error(String message, Map<String, Object> ctx) {
        log("ERROR", message, ctx);
    }
    private void log(String level, String message, Map<String, Object> ctx) {
        try {
            Map<String, Object> m = base();
            m.put("level", level);
            m.put("message", message);
            if (ctx != null) m.putAll(ctx);
            System.out.println(mapper.writeValueAsString(m));
        } catch (Exception ignored) {}
    }
}

