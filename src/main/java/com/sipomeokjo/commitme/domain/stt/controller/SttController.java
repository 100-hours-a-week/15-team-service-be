package com.sipomeokjo.commitme.domain.stt.controller;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.stt.dto.SttTranscribeRequest;
import com.sipomeokjo.commitme.domain.stt.dto.SttTranscribeResponse;
import com.sipomeokjo.commitme.domain.stt.dto.ai.AiSttTranscribeRequest;
import com.sipomeokjo.commitme.domain.stt.dto.ai.AiSttTranscribeResponse;
import com.sipomeokjo.commitme.domain.stt.service.SttAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stt")
@RequiredArgsConstructor
public class SttController {

    private final SttAiService sttAiService;

    @PostMapping("/transcribe")
    public ResponseEntity<APIResponse<SttTranscribeResponse>> transcribe(
            @RequestBody SttTranscribeRequest request) {
        if (request == null || request.s3Key() == null || request.s3Key().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        AiSttTranscribeResponse aiResponse =
                sttAiService.transcribe(
                        new AiSttTranscribeRequest(request.s3Key(), request.language()));

        if (aiResponse == null || !"success".equalsIgnoreCase(aiResponse.status())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return APIResponse.onSuccess(SuccessCode.OK, new SttTranscribeResponse(aiResponse.text()));
    }
}
