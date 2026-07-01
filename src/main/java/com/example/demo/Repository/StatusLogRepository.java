package com.example.demo.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.dto.log.StatusLogEntry;

@Repository
public class StatusLogRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void createStatusLogs(Long requestLogId, List<StatusLogEntry> entries) {
        if (requestLogId == null || entries == null || entries.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO dbo.status_logs (
                    request_log_id,
                    target_type,
                    target_id,
                    status_field,
                    new_status
                )
                VALUES (
                    :requestLogId,
                    :targetType,
                    :targetId,
                    :statusField,
                    :newStatus
                )
                """;

        Map<String, Object>[] batch = entries.stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("requestLogId", requestLogId);
                    map.put("targetType", entry.getTargetType());
                    map.put("targetId", entry.getTargetId());
                    map.put("statusField", entry.getStatusField());
                    map.put("newStatus", entry.getNewStatus());
                    return map;
                })
                .toArray(Map[]::new);
        namedParameterJdbcTemplate.batchUpdate(sql, batch);
    }
}
