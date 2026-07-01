package com.example.demo.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RequestLogRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Long createRequestLog(Long userId, String method, String path, Integer statusCode) {
        String sql = """
                INSERT INTO dbo.request_logs (user_id, method, path, status_code)
                VALUES (:userId, :method, :path, :statusCode)
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("method", method)
                .addValue("path", path)
                .addValue("statusCode", statusCode);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[] {"id"});
        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }
}
