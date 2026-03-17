package com.sipomeokjo.commitme.domain.interview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewAnswerRequest;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewCreateRequest;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewStartResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewUpdateNameRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewChatRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewChatResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndRequest.ProfilePayload.ActivityItem;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndRequest.ProfilePayload.CertificateItem;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndRequest.ProfilePayload.EducationItem;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndRequest.ProfilePayload.ExperienceItem;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndRequest.ProfilePayload.TechStackItem;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateRequest.ProjectPayload;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateResponse;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewMessage;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewType;
import com.sipomeokjo.commitme.domain.interview.mapper.InterviewMapper;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewMessageRepository;
import com.sipomeokjo.commitme.domain.interview.sse.InterviewSseEmitterManager;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileResponse;
import com.sipomeokjo.commitme.domain.resume.service.ResumeProfileService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterviewCommandService {

    private final InterviewMessageRepository interviewMessageRepository;
    private final InterviewCommandTransactionService interviewCommandTransactionService;
    private final InterviewAiService interviewAiService;
    private final InterviewMapper interviewMapper;
    private final InterviewSseEmitterManager sseEmitterManager;
    private final ObjectMapper objectMapper;
    private final ResumeProfileService resumeProfileService;

    public InterviewResponse updateName(
            Long userId, Long interviewId, InterviewUpdateNameRequest request) {
        return interviewCommandTransactionService.updateName(userId, interviewId, request.name());
    }

    public void delete(Long userId, Long interviewId) {
        interviewCommandTransactionService.validateOwnership(userId, interviewId);
        interviewMessageRepository.deleteByInterviewId(interviewId);
        interviewCommandTransactionService.deleteInterview(userId, interviewId);
    }

    public InterviewStartResponse create(Long userId, InterviewCreateRequest request) {
        InterviewCommandTransactionService.CreatePrepared prepared =
                interviewCommandTransactionService.prepareCreate(userId, request);
        AiInterviewGenerateRequest generateRequest =
                buildGenerateRequest(
                        prepared.resumeId(),
                        prepared.resumeContent(),
                        prepared.interviewType(),
                        prepared.positionName(),
                        prepared.companyName());

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

        var interview = interviewCommandTransactionService.createInterview(prepared, aiResponse);
        return interviewMapper.toStartResponse(interview);
    }

    public void sendAnswer(Long userId, Long interviewId, InterviewAnswerRequest request) {
        var prepared =
                interviewCommandTransactionService.prepareAnswer(userId, interviewId, request);

        AiInterviewChatResponse chatResponse =
                interviewAiService.chatInterview(
                        new AiInterviewChatRequest(
                                prepared.aiSessionId(), prepared.questionId(), prepared.answer()));

        if (chatResponse == null || !"success".equalsIgnoreCase(chatResponse.status())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        dispatchQuestion(
                interviewId,
                interviewCommandTransactionService.advanceAfterChat(interviewId, chatResponse));
    }

    public String end(Long userId, Long interviewId) {
        var prepared = interviewCommandTransactionService.prepareEnd(userId, interviewId);
        interviewCommandTransactionService.markEnded(userId, interviewId);

        List<InterviewMessage> messages =
                interviewMessageRepository.findByInterviewIdAndTurnNoIsNotNullOrderByTurnNoAsc(
                        interviewId);

        // 답변이 있는 메시지가 있는 경우에만 AI 피드백 요청
        boolean hasAnsweredMessages =
                messages.stream().anyMatch(m -> m.getAnswer() != null && !m.getAnswer().isBlank());

        if (hasAnsweredMessages) {
            AiInterviewEndRequest endRequest =
                    new AiInterviewEndRequest(
                            prepared.aiSessionId(),
                            prepared.interviewType(),
                            prepared.positionName(),
                            prepared.companyName(),
                            messages.stream()
                                    .filter(m -> m.getAnswer() != null && !m.getAnswer().isBlank())
                                    .map(
                                            m ->
                                                    new AiInterviewEndRequest.MessagePayload(
                                                            m.getTurnNo(),
                                                            m.getQuestion(),
                                                            m.getAnswer(),
                                                            m.getAnswerInputType() == null
                                                                    ? "text"
                                                                    : (m.getAnswerInputType()
                                                                                    .name()
                                                                                    .equals("AUDIO")
                                                                            ? "stt"
                                                                            : "text"),
                                                            m.getAskedAt() == null
                                                                    ? null
                                                                    : m.getAskedAt().toString(),
                                                            m.getAnsweredAt() == null
                                                                    ? null
                                                                    : m.getAnsweredAt().toString()))
                                    .toList(),
                            resolveProfilePayload(
                                    userId, prepared.resumeId(), prepared.profileSnapshot()));
            AiInterviewEndResponse endResponse = interviewAiService.endInterview(endRequest);
            if (endResponse != null && "success".equalsIgnoreCase(endResponse.status())) {
                String feedback =
                        interviewCommandTransactionService.applyFeedback(
                                interviewId, writeFeedbackJson(endResponse));
                sseEmitterManager.sendEnd(interviewId);
                return feedback;
            }
        }

        sseEmitterManager.sendEnd(interviewId);
        return null;
    }

    private AiInterviewGenerateRequest buildGenerateRequest(
            Long resumeId,
            String resumeContent,
            InterviewType type,
            String position,
            String companyName) {
        JsonNode root = parseResumeContent(resumeContent);
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
                resumeId.intValue(),
                new AiInterviewGenerateRequest.ResumeContent(projects),
                type.name().toLowerCase(),
                position,
                companyName);
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

    public void sendNextQuestionIfAvailable(Long interviewId) {
        dispatchQuestion(
                interviewId, interviewCommandTransactionService.prepareNextQuestion(interviewId));
    }

    private void dispatchQuestion(
            Long interviewId,
            InterviewCommandTransactionService.InterviewQuestionDispatch dispatch) {
        if (dispatch.completed()) {
            sseEmitterManager.sendAllQuestionsComplete(interviewId);
            return;
        }
        sseEmitterManager.sendQuestion(
                interviewId,
                Map.of(
                        "turnNo", dispatch.turnNo(),
                        "question", dispatch.question(),
                        "askedAt", dispatch.askedAt().toString()));
    }

    private AiInterviewEndRequest.ProfilePayload resolveProfilePayload(
            Long userId, Long resumeId, String profileSnapshot) {
        ResumeProfileResponse resp = null;

        if (profileSnapshot != null && !profileSnapshot.isBlank()) {
            try {
                resp = objectMapper.readValue(profileSnapshot, ResumeProfileResponse.class);
            } catch (Exception ignored) {
            }
        }

        if (resp == null && resumeId != null) {
            try {
                resp = resumeProfileService.getProfile(userId, resumeId);
            } catch (Exception ignored) {
            }
        }

        if (resp == null) {
            return null;
        }

        final ResumeProfileResponse r = resp;
        return new AiInterviewEndRequest.ProfilePayload(
                r.name(),
                r.profileImageUrl(),
                r.phoneCountryCode(),
                r.phoneNumber(),
                r.introduction(),
                r.techStacks().stream().map(t -> new TechStackItem(t.id(), t.name())).toList(),
                r.experiences().stream()
                        .map(
                                e ->
                                        new ExperienceItem(
                                                e.id(),
                                                e.companyName(),
                                                e.position(),
                                                e.department(),
                                                e.startAt(),
                                                e.endAt(),
                                                e.isCurrentlyWorking(),
                                                e.employmentType(),
                                                e.responsibilities()))
                        .toList(),
                r.educations().stream()
                        .map(
                                e ->
                                        new EducationItem(
                                                e.id(),
                                                e.educationType(),
                                                e.institution(),
                                                e.major(),
                                                e.status(),
                                                e.startAt(),
                                                e.endAt()))
                        .toList(),
                r.activities().stream()
                        .map(
                                a ->
                                        new ActivityItem(
                                                a.id(),
                                                a.title(),
                                                a.organization(),
                                                a.year(),
                                                a.description()))
                        .toList(),
                r.certificates().stream()
                        .map(
                                c ->
                                        new CertificateItem(
                                                c.id(),
                                                c.name(),
                                                c.score(),
                                                c.issuer(),
                                                c.issuedAt()))
                        .toList());
    }

    private String writeFeedbackJson(AiInterviewEndResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"status\":\"failed\"}";
        }
    }
}
