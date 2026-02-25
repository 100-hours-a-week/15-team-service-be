package com.sipomeokjo.commitme.domain.stt.dto.ai;

public record AiSttTranscribeResponse(String status, String text, ErrorPayload error) {

    public record ErrorPayload(String code, String message) {}
}
