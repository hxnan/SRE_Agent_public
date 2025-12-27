package com.flashsale.loadtest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Service
public class LoadRunnerService {
    private final String backendUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    public LoadRunnerService(@Value("${BACKEND_URL:http://backend:3001}") String backendUrl) {
        this.backendUrl = backendUrl;
    }

    public String start(Map<String, Object> body) {
        String id = UUID.randomUUID().toString();
        Task t = new Task(id, body, backendUrl, mapper);
        tasks.put(id, t);
        t.start();
        return id;
    }

    public Map<String, Object> status(String id) {
        Task t = tasks.get(id);
        return t == null ? null : t.status();
    }

    public boolean stop(String id) {
        Task t = tasks.get(id);
        if (t == null) return false;
        t.stop();
        return true;
    }

    public Map<String, Object> results(String id) {
        Task t = tasks.get(id);
        return t == null ? null : t.results();
    }

    static class Task {
        final String id;
        final Map<String, Object> params;
        final String backendUrl;
        final ObjectMapper mapper;
        final ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
        final Semaphore semaphore;
        volatile boolean running = true;
        final Stats queryStats = new Stats();
        final Stats seckillStats = new Stats();
        final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        final List<String> tokens = Collections.synchronizedList(new ArrayList<>());
        final boolean debug;

        Task(String id, Map<String, Object> params, String backendUrl, ObjectMapper mapper) {
            this.id = id;
            this.params = params;
            this.backendUrl = backendUrl;
            this.mapper = mapper;
            int concurrency = ((Number) params.getOrDefault("concurrency", 1)).intValue();
            this.semaphore = new Semaphore(Math.max(concurrency, 1));
            Object dv = params.get("debug");
            boolean d = false;
            if (dv instanceof Boolean b) d = b;
            else if (dv instanceof String s) d = "true".equalsIgnoreCase(s);
            this.debug = d;
        }

        void start() {
            int users = ((Number) params.getOrDefault("user_count", 1)).intValue();
            int ramp = ((Number) params.getOrDefault("ramp_up_seconds", 0)).intValue();
            Map<String,Object> mix = (Map<String,Object>) params.getOrDefault("operation_mix", Map.of("query_pct",50,"seckill_pct",50));
            Object qv = mix.get("query_pct");
            Object sv = mix.get("seckill_pct");
            int queryPct = qv == null ? 50 : ((Number) qv).intValue();
            int seckillPct = sv == null ? 50 : ((Number) sv).intValue();
            int thinkMin = ((Number) params.getOrDefault("think_time_ms_min", 0)).intValue();
            int thinkMax = ((Number) params.getOrDefault("think_time_ms_max", 0)).intValue();
            List<Long> goodsIds = new ArrayList<>();
            Object gi = params.get("goods_ids");
            if (gi instanceof List<?>) {
                for (Object o : (List<?>) gi) {
                    if (o instanceof Number n) goodsIds.add(n.longValue());
                }
            }
            loginUsers(users);
            Runnable loop = () -> {
                Random r = new Random();
                while (running) {
                    try {
                        semaphore.acquire();
                        exec.submit(() -> {
                            try {
                                int op = r.nextInt(100);
                                if (op < queryPct) {
                                    long start = System.nanoTime();
                                    doQueryActive();
                                    queryStats.observe(Duration.ofNanos(System.nanoTime()-start));
                                } else {
                                    long gid = selectGoods(goodsIds);
                                    long start = System.nanoTime();
                                    doSeckill(gid);
                                    seckillStats.observe(Duration.ofNanos(System.nanoTime()-start));
                                }
                            } finally {
                                semaphore.release();
                            }
                            if (thinkMax > 0) {
                                int ms = thinkMin + r.nextInt(Math.max(thinkMax - thinkMin, 1));
                                try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
                            }
                        });
                    } catch (Exception ignored) {}
                }
            };
            Thread t = Thread.ofVirtual().start(loop);
            if (ramp > 0) {
                // simple ramp-up: no-op here, concurrency already bounded by semaphore
            }
            Integer duration = (params.get("duration_seconds") instanceof Number) ? ((Number) params.get("duration_seconds")).intValue() : null;
            if (duration != null && duration > 0) {
                Thread.ofVirtual().start(() -> {
                    try { Thread.sleep(duration * 1000L); } catch (InterruptedException ignored) {}
                    running = false;
                    t.interrupt();
                });
            }
        }

        long selectGoods(List<Long> goodsIds) {
            if (!goodsIds.isEmpty()) return goodsIds.get(Math.abs(new Random().nextInt()) % goodsIds.size());
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(backendUrl + "/api/products?status=active&expired=false&page=1&limit=50&sort_by=stock&sort_order=desc"))
                        .header("Authorization", "Bearer " + randomToken())
                        .header("Accept", "application/json")
                        .GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                String body = resp.body();
                if (resp.statusCode() >= 200 && resp.statusCode() < 300 && body != null && !body.isBlank()) {
                    Map<?,?> m = mapper.readValue(body, Map.class);
                    List<?> products = (List<?>) m.get("products");
                    if (products != null && !products.isEmpty()) {
                        for (Object o : products) {
                            if (o instanceof Map<?,?> pm) {
                                Object idObj = pm.get("id");
                                Object stockObj = pm.get("stock");
                                if (idObj instanceof Number && stockObj instanceof Number) {
                                    if (((Number) stockObj).longValue() > 0L) {
                                        return ((Number) idObj).longValue();
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            return 1L;
        }

        void loginUsers(int users) {
            for (int i = 0; i < users; i++) {
                String username = "loadtest_user_" + i;
                try {
                    Map<String,Object> payload = Map.of("username", username);
                    String json = mapper.writeValueAsString(payload);
                    HttpRequest req = HttpRequest.newBuilder(URI.create(backendUrl + "/api/auth/mock/login"))
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json)).build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    String body = resp.body();
                    if (body != null && !body.isBlank()) {
                        try {
                            Map<?,?> m = mapper.readValue(body, Map.class);
                            Object t = m.get("token");
                            if (t != null) tokens.add(String.valueOf(t));
                        } catch (Exception ignored) {}
                    }
                    if (debug) {
                        try {
                            java.util.Map<String,Object> ctx = new java.util.HashMap<>();
                            ctx.put("event", "login_response");
                            ctx.put("task_id", id);
                            ctx.put("username", username);
                            ctx.put("status", resp.statusCode());
                            ctx.put("body", body);
                            System.out.println(mapper.writeValueAsString(ctx));
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    if (debug) {
                        try {
                            java.util.Map<String,Object> ctx = new java.util.HashMap<>();
                            ctx.put("event", "login_error");
                            ctx.put("task_id", id);
                            ctx.put("username", username);
                            ctx.put("error", e.getMessage());
                            System.out.println(mapper.writeValueAsString(ctx));
                        } catch (Exception ignored) {}
                    }
                }
            }
            if (tokens.isEmpty()) {
                try {
                    String fallbackUser = String.valueOf(params.getOrDefault("fallback_username", "testuser"));
                    String fallbackPassword = String.valueOf(params.getOrDefault("fallback_password", "test123"));
                    String json = mapper.writeValueAsString(Map.of("username", fallbackUser, "password", fallbackPassword));
                    HttpRequest req = HttpRequest.newBuilder(URI.create(backendUrl + "/api/auth/login"))
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json)).build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    String body = resp.body();
                    if (body != null && !body.isBlank()) {
                        Map<?,?> m = mapper.readValue(body, Map.class);
                        Object t = m.get("token");
                        if (t != null) tokens.add(String.valueOf(t));
                    }
                    if (debug) {
                        try {
                            java.util.Map<String,Object> ctx = new java.util.HashMap<>();
                            ctx.put("event", "fallback_login_response");
                            ctx.put("task_id", id);
                            ctx.put("username", fallbackUser);
                            ctx.put("status", resp.statusCode());
                            ctx.put("body", body);
                            System.out.println(mapper.writeValueAsString(ctx));
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
            if (tokens.isEmpty()) {
                tokens.add("invalid-token");
            }
        }

        String randomToken() { return tokens.get(Math.abs(new Random().nextInt()) % tokens.size()); }

        void doQueryActive() {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(backendUrl + "/api/products?status=active&expired=false&page=1&limit=20"))
                        .header("Authorization", "Bearer " + randomToken())
                        .GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (debug) {
                    try {
                        java.util.Map<String,Object> ctx = new java.util.HashMap<>();
                        ctx.put("event", "query_response");
                        ctx.put("task_id", id);
                        ctx.put("status", resp.statusCode());
                        ctx.put("uri", req.uri().toString());
                        ctx.put("body", resp.body());
                        System.out.println(mapper.writeValueAsString(ctx));
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                queryStats.failures.incrementAndGet();
                if (debug) {
                    try {
                        java.util.Map<String,Object> ctx = new java.util.HashMap<>();
                        ctx.put("event", "query_error");
                        ctx.put("task_id", id);
                        ctx.put("error", e.getMessage());
                        System.out.println(mapper.writeValueAsString(ctx));
                    } catch (Exception ignored) {}
                }
            }
        }

        void doSeckill(long goodsId) {
            try {
                String json = mapper.writeValueAsString(Map.of("goods_id", goodsId));
                HttpRequest req = HttpRequest.newBuilder(URI.create(backendUrl + "/api/seckill"))
                        .header("Authorization", "Bearer " + randomToken())
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                String body = resp.body();
                boolean ok = false;
                if (resp.statusCode() >= 200 && resp.statusCode() < 300 && body != null && !body.isBlank()) {
                    try {
                        Map<?,?> m = mapper.readValue(body, Map.class);
                        Object success = m.get("success");
                        ok = Boolean.TRUE.equals(success);
                    } catch (Exception ignored) {
                        ok = false;
                    }
                }
                if (ok) seckillStats.success.incrementAndGet(); else seckillStats.failures.incrementAndGet();
                if (debug) {
                    try {
                        java.util.Map<String,Object> ctx = new java.util.HashMap<>();
                        ctx.put("event", "seckill_response");
                        ctx.put("task_id", id);
                        ctx.put("status", resp.statusCode());
                        ctx.put("uri", req.uri().toString());
                        ctx.put("goods_id", goodsId);
                        ctx.put("body", body);
                        System.out.println(mapper.writeValueAsString(ctx));
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                seckillStats.failures.incrementAndGet();
                if (debug) {
                    try {
                        java.util.Map<String,Object> ctx = new java.util.HashMap<>();
                        ctx.put("event", "seckill_error");
                        ctx.put("task_id", id);
                        ctx.put("goods_id", goodsId);
                        ctx.put("error", e.getMessage());
                        System.out.println(mapper.writeValueAsString(ctx));
                    } catch (Exception ignored) {}
                }
            }
        }

        Map<String, Object> status() {
            return Map.of(
                    "task_id", id,
                    "running", running,
                    "query", queryStats.snapshot(),
                    "seckill", seckillStats.snapshot()
            );
        }

        Map<String, Object> results() {
            running = false;
            return status();
        }

        void stop() { running = false; }
    }

    static class Stats {
        final java.util.concurrent.atomic.AtomicLong success = new java.util.concurrent.atomic.AtomicLong();
        final java.util.concurrent.atomic.AtomicLong failures = new java.util.concurrent.atomic.AtomicLong();
        final Deque<Long> latencies = new ArrayDeque<>();

        synchronized void observe(Duration d) {
            success.incrementAndGet();
            long ms = d.toMillis();
            if (latencies.size() > 10000) latencies.pollFirst();
            latencies.addLast(ms);
        }

        synchronized Map<String,Object> snapshot() {
            List<Long> list = new ArrayList<>(latencies);
            Collections.sort(list);
            long count = list.size();
            double avg = list.stream().mapToLong(Long::longValue).average().orElse(0);
            long p95 = count == 0 ? 0 : list.get((int)Math.min(count-1, Math.round(count*0.95)-1));
            return Map.of(
                    "success", success.get(),
                    "failures", failures.get(),
                    "avg_ms", (long) avg,
                    "p95_ms", p95,
                    "samples", count
            );
        }
    }
}
