package com.sipomeokjo.commitme.domain.resume.dto;

import java.time.Instant;

public record ResumeVersionSummaryDto(Integer versionNo, Instant committedAt) {}
