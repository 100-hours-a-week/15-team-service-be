package com.sipomeokjo.commitme.domain.resume.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Pure calculation class for computing diffs between two resume snapshots represented as {@code
 * Map<String, Object>}.
 */
public class ResumeDiffEngine {

    public enum ChangeType {
        ADDED,
        REMOVED,
        MODIFIED,
        UNCHANGED
    }

    public record DiffEntry(String path, ChangeType changeType, Object before, Object after) {}

    public List<DiffEntry> diff(Map<String, Object> base, Map<String, Object> target) {
        List<DiffEntry> result = new ArrayList<>();
        diffMaps("", base, target, result);
        return result;
    }

    private void diffMaps(
            String prefix,
            Map<String, Object> base,
            Map<String, Object> target,
            List<DiffEntry> result) {
        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(base.keySet());
        allKeys.addAll(target.keySet());

        for (String key : allKeys) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            boolean inBase = base.containsKey(key);
            boolean inTarget = target.containsKey(key);
            Object baseVal = base.get(key);
            Object targetVal = target.get(key);

            if (!inBase) {
                result.add(new DiffEntry(path, ChangeType.ADDED, null, targetVal));
            } else if (!inTarget) {
                result.add(new DiffEntry(path, ChangeType.REMOVED, baseVal, null));
            } else {
                diffValues(path, baseVal, targetVal, result);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void diffValues(String path, Object base, Object target, List<DiffEntry> result) {
        if (base == null && target == null) return;
        if (base == null) {
            result.add(new DiffEntry(path, ChangeType.ADDED, null, target));
            return;
        }
        if (target == null) {
            result.add(new DiffEntry(path, ChangeType.REMOVED, base, null));
            return;
        }

        if (base instanceof Map<?, ?> && target instanceof Map<?, ?>) {
            diffMaps(path, (Map<String, Object>) base, (Map<String, Object>) target, result);
            return;
        }

        if (base instanceof List<?> && target instanceof List<?>) {
            diffLists(path, (List<Object>) base, (List<Object>) target, result);
            return;
        }

        if (!Objects.equals(base, target)) {
            if (base instanceof String s1
                    && target instanceof String s2
                    && (s1.contains("\n") || s2.contains("\n"))) {
                diffStrings(path, s1, s2, result);
            } else {
                result.add(new DiffEntry(path, ChangeType.MODIFIED, base, target));
            }
        }
    }

    private void diffStrings(String path, String base, String target, List<DiffEntry> result) {
        List<String> baseLines = Arrays.asList(base.split("\n", -1));
        List<String> targetLines = Arrays.asList(target.split("\n", -1));

        int m = baseLines.size(), n = targetLines.size();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                dp[i][j] =
                        baseLines.get(i).equals(targetLines.get(j))
                                ? 1 + dp[i + 1][j + 1]
                                : Math.max(dp[i + 1][j], dp[i][j + 1]);
            }
        }

        int i = 0, j = 0, idx = 0;
        while (i < m || j < n) {
            if (i < m && j < n && baseLines.get(i).equals(targetLines.get(j))) {
                result.add(
                        new DiffEntry(
                                path + "[" + idx + "]",
                                ChangeType.UNCHANGED,
                                baseLines.get(i),
                                baseLines.get(i)));
                i++;
                j++;
                idx++;
            } else if (i < m && (j >= n || dp[i + 1][j] >= dp[i][j + 1])) {
                result.add(
                        new DiffEntry(
                                path + "[" + idx + "]",
                                ChangeType.REMOVED,
                                baseLines.get(i),
                                null));
                i++;
                idx++;
            } else {
                result.add(
                        new DiffEntry(
                                path + "[" + idx + "]",
                                ChangeType.ADDED,
                                null,
                                targetLines.get(j)));
                j++;
                idx++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void diffLists(
            String path, List<Object> base, List<Object> target, List<DiffEntry> result) {
        String fieldName = extractFieldName(path);
        Function<Object, Object> keyExtractor = getKeyExtractor(fieldName);

        Map<Object, Object> baseByKey = indexList(base, keyExtractor);
        Map<Object, Object> targetByKey = indexList(target, keyExtractor);

        Set<Object> allKeys = new LinkedHashSet<>();
        allKeys.addAll(baseByKey.keySet());
        allKeys.addAll(targetByKey.keySet());

        for (Object key : allKeys) {
            boolean inBase = baseByKey.containsKey(key);
            boolean inTarget = targetByKey.containsKey(key);
            String itemPath = path + "[" + key + "]";

            if (!inBase) {
                result.add(new DiffEntry(itemPath, ChangeType.ADDED, null, targetByKey.get(key)));
            } else if (!inTarget) {
                result.add(new DiffEntry(itemPath, ChangeType.REMOVED, baseByKey.get(key), null));
            } else {
                Object baseItem = baseByKey.get(key);
                Object targetItem = targetByKey.get(key);
                if (baseItem instanceof Map<?, ?> && targetItem instanceof Map<?, ?>) {
                    diffMaps(
                            itemPath,
                            (Map<String, Object>) baseItem,
                            (Map<String, Object>) targetItem,
                            result);
                } else if (!Objects.equals(baseItem, targetItem)) {
                    result.add(new DiffEntry(itemPath, ChangeType.MODIFIED, baseItem, targetItem));
                }
            }
        }
    }

    private Map<Object, Object> indexList(
            List<Object> list, Function<Object, Object> keyExtractor) {
        Map<Object, Object> indexed = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Object key = keyExtractor.apply(item);
            // Fallback to index if key is null or duplicate
            Object finalKey = (key != null && !indexed.containsKey(key)) ? key : "__idx_" + i;
            indexed.put(finalKey, item);
        }
        return indexed;
    }

    private String extractFieldName(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : path;
    }

    @SuppressWarnings("unchecked")
    private Function<Object, Object> getKeyExtractor(String fieldName) {
        return switch (fieldName) {
            case "projects" ->
                    item -> {
                        if (item instanceof Map<?, ?> m) {
                            Object repoUrl = ((Map<String, Object>) m).get("repoUrl");
                            if (repoUrl != null) return repoUrl;
                            return ((Map<String, Object>) m).get("name");
                        }
                        return null;
                    };
            case "techStack", "techStacks" ->
                    item -> {
                        if (item instanceof Map<?, ?> m) {
                            return ((Map<String, Object>) m).get("name");
                        }
                        return null;
                    };
            case "experiences", "educations", "activities", "certificates" ->
                    item -> {
                        if (item instanceof Map<?, ?> m) {
                            return ((Map<String, Object>) m).get("id");
                            // natural key fallback (no-op, index fallback used)
                        }
                        return null;
                    };
            default ->
                    item -> {
                        if (item instanceof Map<?, ?> m) {
                            return ((Map<String, Object>) m).get("id");
                        }
                        return null;
                    };
        };
    }
}
