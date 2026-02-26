package com.sipomeokjo.commitme.domain.interview.dto.ai;

import java.time.Instant;

public record AiInterviewMessageCallback(
        String aiSessionId, Integer turnNo, String question, Instant askedAt) {}
