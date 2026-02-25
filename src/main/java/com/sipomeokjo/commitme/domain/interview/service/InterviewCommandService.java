package com.sipomeokjo.commitme.domain.interview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.company.repository.CompanyRepository;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewAnswerRequest;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewCreateRequest;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewStartResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewUpdateNameRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewChatRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewChatResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateRequest.ProjectPayload;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateResponse;
import com.sipomeokjo.commitme.domain.interview.entity.Interview;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewMessage;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewType;
import com.sipomeokjo.commitme.domain.interview.mapper.InterviewMapper;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewMessageRepository;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewRepository;
import com.sipomeokjo.commitme.domain.interview.sse.InterviewSseEmitterManager;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewCommandService {

    private final InterviewRepository interviewRepository;
    private final InterviewMessageRepository interviewMessageRepository;
    private final InterviewMapper interviewMapper;
    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final CompanyRepository companyRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final InterviewAiService interviewAiService;
    private final InterviewSseEmitterManager sseEmitterManager;
    private final ObjectMapper objectMapper;

    public InterviewResponse updateName(
            Long userId, Long interviewId, InterviewUpdateNameRequest request) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        interview.updateName(request.name());

        return interviewMapper.toResponse(interview);
    }

    public void delete(Long userId, Long interviewId) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        // MongoDB 삭제 먼저 (트랜잭션 분리)
        interviewMessageRepository.deleteByInterviewId(interviewId);
        // MySQL 삭제
        interviewRepository.delete(interview);
    }

    public InterviewStartResponse create(Long userId, InterviewCreateRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Position position =
                positionRepository
                        .findById(request.positionId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));

        Company company = null;
        if (request.companyId() != null) {
            company =
                    companyRepository
                            .findById(request.companyId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        }

        ResumeVersion resumeVersion = resolveResumeVersion(request);
        if (resumeVersion == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        AiInterviewGenerateRequest generateRequest =
                buildGenerateRequest(resumeVersion, request.interviewType(), position.getName());

        String interviewName = generateInterviewName(position.getName());

        Interview interview =
                Interview.create(user, position, company, interviewName, request.interviewType());

        interviewRepository.save(interview);

        AiInterviewGenerateResponse aiResponse =
                interviewAiService.generateInterview(generateRequest);
        if (aiResponse == null || !"success".equalsIgnoreCase(aiResponse.status())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (aiResponse.aiSessionId() == null || aiResponse.aiSessionId().isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (aiResponse.questions() == null || aiResponse.questions().isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        interview.updateAiSessionId(aiResponse.aiSessionId());

        List<InterviewMessage> questionMessages = new ArrayList<>();
        int order = 1;
        for (AiInterviewGenerateResponse.QuestionPayload question : aiResponse.questions()) {
            questionMessages.add(
                    InterviewMessage.createFromGeneratedQuestion(
                            interview.getId(), order, question.questionId(), question.text()));
            order++;
        }
        interviewMessageRepository.saveAll(questionMessages);

        return interviewMapper.toStartResponse(interview);
    }

    public void sendAnswer(Long userId, Long interviewId, InterviewAnswerRequest request) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        if (interview.isEnded()) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_ENDED);
        }

        InterviewMessage message =
                interviewMessageRepository
                        .findByInterviewIdAndTurnNo(interviewId, request.turnNo())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.INTERVIEW_SESSION_INVALID));

        message.updateAnswer(request.answer(), request.answerInputType(), request.audioUrl());
        interviewMessageRepository.save(message);

        if (message.getQuestionId() == null || message.getQuestionId().isBlank()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_INVALID);
        }

        AiInterviewChatResponse chatResponse =
                interviewAiService.chatInterview(
                        new AiInterviewChatRequest(
                                interview.getAiSessionId(),
                                message.getQuestionId(),
                                request.answer()));

        if (chatResponse == null || !"success".equalsIgnoreCase(chatResponse.status())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        String followUpQuestion = chatResponse.followUpQuestion();
        if (followUpQuestion != null && !followUpQuestion.isBlank()) {
            Integer nextTurnNo = nextTurnNo(interviewId);
            Instant askedAt = Instant.now();
            InterviewMessage followUpMessage =
                    InterviewMessage.createFollowUpQuestion(
                            interviewId,
                            nextTurnNo,
                            message.getQuestionId(),
                            followUpQuestion,
                            askedAt);
            interviewMessageRepository.save(followUpMessage);
            sseEmitterManager.sendQuestion(
                    interviewId,
                    Map.of(
                            "turnNo", nextTurnNo,
                            "question", followUpQuestion,
                            "askedAt", askedAt.toString()));
            return;
        }

        sendNextQuestionIfAvailable(interviewId);
    }

    public void end(Long userId, Long interviewId) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        if (interview.isEnded()) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_ENDED);
        }

        interview.end();

        List<InterviewMessage> messages =
                interviewMessageRepository.findByInterviewIdAndTurnNoIsNotNullOrderByTurnNoAsc(
                        interviewId);

        AiInterviewEndResponse endResponse = interviewAiService.endInterview(interview, messages);
        if (endResponse == null || !"success".equalsIgnoreCase(endResponse.status())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        interview.updateFeedback(writeFeedbackJson(endResponse));

        sseEmitterManager.sendFeedback(
                interviewId, Map.of("totalFeedback", interview.getTotalFeedback()));
        sseEmitterManager.sendEnd(interviewId);
    }

    private String generateInterviewName(String positionName) {
        return positionName + " 모의 면접";
    }

    private ResumeVersion resolveResumeVersion(InterviewCreateRequest request) {
        if (request.resumeVersionId() != null) {
            return resumeVersionRepository.findById(request.resumeVersionId()).orElse(null);
        }
        if (request.resumeId() != null && request.resumeVersionNo() != null) {
            return resumeVersionRepository
                    .findByResume_IdAndVersionNo(request.resumeId(), request.resumeVersionNo())
                    .orElse(null);
        }
        if (request.resumeId() != null) {
            return resumeVersionRepository
                    .findTopByResume_IdAndStatusOrderByVersionNoDesc(
                            request.resumeId(), ResumeVersionStatus.SUCCEEDED)
                    .orElse(null);
        }
        return null;
    }

    private AiInterviewGenerateRequest buildGenerateRequest(
            ResumeVersion resumeVersion, InterviewType type, String position) {
        JsonNode root = parseResumeContent(resumeVersion.getContent());
        JsonNode projectsNode = root == null ? null : root.get("projects");
        JsonNode techStackNode = root == null ? null : root.get("techStack");
        List<String> topTechStack = parseTechStack(techStackNode);

        List<ProjectPayload> projects = new ArrayList<>();
        if (projectsNode != null && projectsNode.isArray()) {
            for (JsonNode projectNode : projectsNode) {
                ProjectPayload payload = toProjectPayload(projectNode, topTechStack);
                if (payload != null) {
                    projects.add(payload);
                }
            }
        }

        if (projects.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        return new AiInterviewGenerateRequest(
                resumeVersion.getResume().getId().intValue(),
                new AiInterviewGenerateRequest.ResumeContent(projects),
                type.name().toLowerCase(),
                position);
    }

    private JsonNode parseResumeContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private List<String> parseTechStack(JsonNode node) {
        List<String> stacks = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    stacks.add(item.asText());
                }
            }
        }
        return stacks;
    }

    private ProjectPayload toProjectPayload(JsonNode node, List<String> fallbackTechStack) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String name = textValue(node.get("name"));
        if (name == null || name.isBlank()) {
            return null;
        }
        String repoUrl = textValue(node.get("repoUrl"));
        List<String> techStack = parseTechStack(node.get("techStack"));
        if (techStack.isEmpty()) {
            techStack = fallbackTechStack;
        }
        if (techStack == null || techStack.isEmpty()) {
            return null;
        }

        String description = parseDescription(node.get("description"));
        if (description == null || description.isBlank()) {
            description = "설명 없음";
        }

        return new ProjectPayload(name, repoUrl == null ? "" : repoUrl, techStack, description);
    }

    private String parseDescription(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            List<String> lines = new ArrayList<>();
            for (JsonNode item : node) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    lines.add(item.asText());
                }
            }
            return String.join("\n", lines);
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return node.toString();
    }

    private String textValue(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private Integer nextTurnNo(Long interviewId) {
        return interviewMessageRepository
                .findFirstByInterviewIdAndTurnNoIsNotNullOrderByTurnNoDesc(interviewId)
                .map(msg -> msg.getTurnNo() + 1)
                .orElse(1);
    }

    public void sendNextQuestionIfAvailable(Long interviewId) {
        InterviewMessage latestAsked =
                interviewMessageRepository
                        .findFirstByInterviewIdAndTurnNoIsNotNullOrderByTurnNoDesc(interviewId)
                        .orElse(null);
        if (latestAsked != null
                && (latestAsked.getAnswer() == null || latestAsked.getAnswer().isBlank())) {
            return;
        }

        InterviewMessage nextQuestion =
                interviewMessageRepository
                        .findFirstByInterviewIdAndQuestionOrderIsNotNullAndAskedAtIsNullOrderByQuestionOrderAsc(
                                interviewId)
                        .orElse(null);
        if (nextQuestion == null) {
            return;
        }

        Integer nextTurnNo = nextTurnNo(interviewId);
        Instant askedAt = Instant.now();
        nextQuestion.markAsked(nextTurnNo, askedAt);
        interviewMessageRepository.save(nextQuestion);

        sseEmitterManager.sendQuestion(
                interviewId,
                Map.of(
                        "turnNo", nextTurnNo,
                        "question", nextQuestion.getQuestion(),
                        "askedAt", askedAt.toString()));
    }

    private String writeFeedbackJson(AiInterviewEndResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"status\":\"failed\"}";
        }
    }
}
