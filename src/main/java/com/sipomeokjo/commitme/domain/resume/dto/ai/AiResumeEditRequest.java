package com.sipomeokjo.commitme.domain.resume.dto.ai;

import com.fasterxml.jackson.databind.JsonNode;

public record AiResumeEditRequest(Long resumeId, JsonNode content, String requestMessage) {}
