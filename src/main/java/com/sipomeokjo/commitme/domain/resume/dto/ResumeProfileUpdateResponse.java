package com.sipomeokjo.commitme.domain.resume.dto;

import java.time.Instant;

public record ResumeProfileUpdateResponse(Long resumeId, Instant updatedAt) {}
