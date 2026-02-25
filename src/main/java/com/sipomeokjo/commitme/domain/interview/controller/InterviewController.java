package com.sipomeokjo.commitme.domain.interview.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewAnswerRequest;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewCreateRequest;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewDetailResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewStartResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewTypeResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewUpdateNameRequest;
import com.sipomeokjo.commitme.domain.interview.service.InterviewCommandService;
import com.sipomeokjo.commitme.domain.interview.service.InterviewQueryService;
import com.sipomeokjo.commitme.security.resolver.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewQueryService interviewQueryService;
    private final InterviewCommandService interviewCommandService;

    @GetMapping
    public ResponseEntity<APIResponse<List<InterviewResponse>>> getInterviews(
            @CurrentUserId Long userId) {
        List<InterviewResponse> interviews = interviewQueryService.getInterviews(userId);
        return APIResponse.onSuccess(SuccessCode.INTERVIEW_LIST_FETCHED, interviews);
    }

    @GetMapping("/{interviewId}")
    public ResponseEntity<APIResponse<InterviewDetailResponse>> getInterview(
            @CurrentUserId Long userId, @PathVariable Long interviewId) {
        InterviewDetailResponse interview = interviewQueryService.getInterview(userId, interviewId);
        return APIResponse.onSuccess(SuccessCode.INTERVIEW_FETCHED, interview);
    }

    @GetMapping("/types")
    public ResponseEntity<APIResponse<List<InterviewTypeResponse>>> getInterviewTypes() {
        List<InterviewTypeResponse> types = interviewQueryService.getInterviewTypes();
        return APIResponse.onSuccess(SuccessCode.INTERVIEW_TYPES_FETCHED, types);
    }

    @PatchMapping("/{interviewId}/name")
    public ResponseEntity<APIResponse<InterviewResponse>> updateInterviewName(
            @CurrentUserId Long userId,
            @PathVariable Long interviewId,
            @RequestBody @Valid InterviewUpdateNameRequest request) {
        InterviewResponse interview =
                interviewCommandService.updateName(userId, interviewId, request);
        return APIResponse.onSuccess(SuccessCode.INTERVIEW_NAME_UPDATED, interview);
    }

    @DeleteMapping("/{interviewId}")
    public ResponseEntity<APIResponse<Void>> deleteInterview(
            @CurrentUserId Long userId, @PathVariable Long interviewId) {
        interviewCommandService.delete(userId, interviewId);
        return APIResponse.onSuccess(SuccessCode.INTERVIEW_DELETED);
    }

    @PostMapping
    public ResponseEntity<APIResponse<InterviewStartResponse>> createInterview(
            @CurrentUserId Long userId, @RequestBody @Valid InterviewCreateRequest request) {
        InterviewStartResponse response = interviewCommandService.create(userId, request);
        return APIResponse.onSuccess(SuccessCode.INTERVIEW_STARTED, response);
    }

    @PostMapping("/{interviewId}/messages")
    public ResponseEntity<APIResponse<Void>> sendAnswer(
            @CurrentUserId Long userId,
            @PathVariable Long interviewId,
            @RequestBody @Valid InterviewAnswerRequest request) {
        interviewCommandService.sendAnswer(userId, interviewId, request);
        return APIResponse.onSuccess(SuccessCode.INTERVIEW_ANSWER_SENT);
    }

    @PostMapping("/{interviewId}/end")
    public ResponseEntity<APIResponse<Void>> endInterview(
            @CurrentUserId Long userId, @PathVariable Long interviewId) {
        interviewCommandService.end(userId, interviewId);
        return APIResponse.onSuccess(SuccessCode.INTERVIEW_ENDED);
    }
}
