package com.flashsale.loadtest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class FaultInjectionService {
    private final DataSource dataSource;
    private final AtomicReference<Connection> lockConn = new AtomicReference<>();
    private final String defaultTable;

    public FaultInjectionService(DataSource dataSource,
                                 @Value("${LOCK_TABLE_NAME:seckill_goods}") String defaultTable) {
        this.dataSource = dataSource;
        this.defaultTable = defaultTable;
    }

    public Map<String, Object> status() {
        Connection c = lockConn.get();
        boolean locked = false;
        try {
            locked = c != null && !c.isClosed();
        } catch (SQLException ignored) {
            locked = c != null;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("locked", locked);
        m.put("table", defaultTable);
        return m;
    }

    public Map<String, Object> lock(String tableOverride) {
        String table = (tableOverride == null || tableOverride.isBlank()) ? defaultTable : tableOverride.trim();
        Connection existing = lockConn.get();
        try {
            if (existing != null && !existing.isClosed()) {
                return Map.of("locked", true, "table", table, "message", "already_locked");
            }
        } catch (SQLException ignored) { /* proceed to re-lock */ }
        try {
            Connection conn = dataSource.getConnection();
            try (Statement st = conn.createStatement()) {
                st.execute("LOCK TABLES " + table + " WRITE");
            }
            lockConn.set(conn);
            return Map.of("locked", true, "table", table);
        } catch (SQLException e) {
            return Map.of("locked", false, "table", table, "error", e.getMessage());
        }
    }

    public Map<String, Object> unlock() {
        Connection conn = lockConn.get();
        if (conn == null) {
            return Map.of("locked", false, "message", "no_lock");
        }
        try {
            try (Statement st = conn.createStatement()) {
                st.execute("UNLOCK TABLES");
            }
        } catch (SQLException e) {
            // even if UNLOCK fails, attempt to close to release resources
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
            lockConn.set(null);
        }
        return Map.of("locked", false);
    }
}
