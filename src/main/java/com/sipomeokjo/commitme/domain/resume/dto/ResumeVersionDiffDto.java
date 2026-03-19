package com.sipomeokjo.commitme.domain.resume.dto;

import java.util.List;

public record ResumeVersionDiffDto(
        Long resumeId, Integer baseVersionNo, Integer targetVersionNo, List<DiffItem> diffs) {

    public record DiffItem(String path, String changeType, Object before, Object after) {}
}
