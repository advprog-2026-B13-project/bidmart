package id.ac.ui.cs.advprog.bidmartcore.controller;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/health/status")
    public Map<String, Object> healthStatus() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Map.of(
                    "service", "bidmart-core",
                    "status", "UP",
                    "db", "CONNECTED");
        } catch (Exception e) {
            return Map.of(
                    "service", "bidmart-core",
                    "status", "UP",
                    "db", "DISCONNECTED",
                    "error", e.getMessage());
        }
    }
}
