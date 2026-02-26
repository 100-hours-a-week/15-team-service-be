package com.sipomeokjo.commitme.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;

public class JdbcQueryMetricsListener implements QueryExecutionListener {

    private static final String JDBC_QUERY_EXECUTION_TIMER = "jdbc.query.execution";
    private static final String JDBC_QUERY_SLOW_COUNTER = "jdbc.query.slow";
    private static final String UNKNOWN = "unknown";

    private final MeterRegistry meterRegistry;
    private final long slowQueryThresholdMs;

    public JdbcQueryMetricsListener(MeterRegistry meterRegistry, long slowQueryThresholdMs) {
        this.meterRegistry = meterRegistry;
        this.slowQueryThresholdMs = Math.max(1L, slowQueryThresholdMs);
    }

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {}

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        long elapsedTimeMs = Math.max(0L, execInfo.getElapsedTime());
        String datasource = safeTagValue(execInfo.getDataSourceName());
        String statementType = resolveStatementType(execInfo, queryInfoList);
        String success = Boolean.toString(execInfo.isSuccess());
        String batch = Boolean.toString(execInfo.isBatch());

        Timer.builder(JDBC_QUERY_EXECUTION_TIMER)
                .tag("datasource", datasource)
                .tag("success", success)
                .tag("statement_type", statementType)
                .tag("batch", batch)
                .register(meterRegistry)
                .record(elapsedTimeMs, TimeUnit.MILLISECONDS);

        if (elapsedTimeMs < slowQueryThresholdMs) {
            return;
        }

        Counter.builder(JDBC_QUERY_SLOW_COUNTER)
                .tag("datasource", datasource)
                .tag("success", success)
                .tag("statement_type", statementType)
                .tag("batch", batch)
                .register(meterRegistry)
                .increment();
    }

    private String resolveStatementType(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        if (queryInfoList != null) {
            for (QueryInfo queryInfo : queryInfoList) {
                if (queryInfo == null) {
                    continue;
                }
                String sqlVerb = extractSqlVerb(queryInfo.getQuery());
                if (!UNKNOWN.equals(sqlVerb)) {
                    return sqlVerb;
                }
            }
        }

        if (execInfo.getStatementType() == null) {
            return UNKNOWN;
        }
        return execInfo.getStatementType().name().toLowerCase(Locale.ROOT);
    }

    private String extractSqlVerb(String query) {
        if (query == null || query.isBlank()) {
            return UNKNOWN;
        }

        String trimmedQuery = query.stripLeading();
        int endIndex = 0;
        while (endIndex < trimmedQuery.length()
                && Character.isLetter(trimmedQuery.charAt(endIndex))) {
            endIndex++;
        }

        if (endIndex == 0) {
            return UNKNOWN;
        }
        return trimmedQuery.substring(0, endIndex).toLowerCase(Locale.ROOT);
    }

    private String safeTagValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return value;
    }
}
