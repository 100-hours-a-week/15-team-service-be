package com.sipomeokjo.commitme.domain.resume.repository.mongo;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import java.util.List;

public final class ResumeEventAggregationResult {

    private ResumeEventAggregationResult() {}

    public record VersionListResult(
            List<ResumeEventDocument> committedPage, ResumeEventDocument latestSucceeded) {}
}
