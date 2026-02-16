package com.sipomeokjo.commitme.logging;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.logging.AbstractQueryLogEntryCreator;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import org.slf4j.MDC;

public class DevQueryLogEntryCreator extends AbstractQueryLogEntryCreator {

    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final Pattern KEYWORD_PATTERN =
            Pattern.compile(
                    "\\b(SELECT|INSERT|UPDATE|DELETE|FROM|WHERE|JOIN|LEFT|RIGHT|INNER|OUTER|ON|VALUES|SET|AND|OR|GROUP|BY|ORDER|LIMIT|OFFSET|HAVING|INTO)\\b",
                    Pattern.CASE_INSENSITIVE);

    private final boolean highlightEnabled;

    public DevQueryLogEntryCreator(boolean highlightEnabled) {
        this.highlightEnabled = highlightEnabled;
    }

    @Override
    public String getLogEntry(
            ExecutionInfo executionInfo,
            List<QueryInfo> queryInfoList,
            boolean writeDataSourceName,
            boolean writeConnectionId,
            boolean writeIsolation) {
        String api = MDC.get(MdcApiInterceptor.MDC_API_KEY);
        List<String> queries = new ArrayList<>();
        for (QueryInfo queryInfo : queryInfoList) {
            String query = queryInfo.getQuery();
            List<List<ParameterSetOperation>> parametersList = queryInfo.getParametersList();
            if (parametersList == null || parametersList.isEmpty()) {
                queries.add(formatQuery(query));
                continue;
            }
            for (List<ParameterSetOperation> parameters : parametersList) {
                queries.add(formatQuery(replacePlaceholders(query, parameters)));
            }
        }
        String joined = String.join(" | ", queries);
        return api == null || api.isBlank() ? joined : "api=" + api + " | " + joined;
    }

    private String replacePlaceholders(String query, List<ParameterSetOperation> parameters) {
        if (query == null) {
            return "";
        }
        if (parameters == null || parameters.isEmpty()) {
            return query;
        }

        Map<Integer, String> parameterMap = new TreeMap<>();
        for (ParameterSetOperation parameter : parameters) {
            Object[] args = parameter.getArgs();
            if (args == null || args.length == 0) {
                continue;
            }
            if (!(args[0] instanceof Integer)) {
                continue;
            }
            int index = (Integer) args[0];
            String value = getDisplayValue(parameter);
            parameterMap.put(index, value == null ? "null" : value);
        }

        if (parameterMap.isEmpty()) {
            return query;
        }

        Iterator<String> values = parameterMap.values().iterator();
        StringBuilder replaced = new StringBuilder();
        for (int i = 0; i < query.length(); i++) {
            char current = query.charAt(i);
            if (current == '?' && values.hasNext()) {
                replaced.append(values.next());
            } else {
                replaced.append(current);
            }
        }
        return replaced.toString();
    }

    private String formatQuery(String query) {
        if (query == null) {
            return "";
        }
        String singleLine = query.replaceAll("\\s+", " ").trim();
        return highlightEnabled ? highlightKeywords(singleLine) : singleLine;
    }

    private String highlightKeywords(String query) {
        Matcher matcher = KEYWORD_PATTERN.matcher(query);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String keyword = matcher.group(1);
            matcher.appendReplacement(
                    buffer, Matcher.quoteReplacement(ANSI_CYAN + keyword + ANSI_RESET));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
