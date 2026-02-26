package com.sipomeokjo.commitme.domain.stt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SttTranscribeRequest(@JsonProperty("s3Key") String s3Key, String language) {}
