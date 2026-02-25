package com.sipomeokjo.commitme.domain.stt.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSttTranscribeRequest(@JsonProperty("s3Key") String s3Key, String language) {}
