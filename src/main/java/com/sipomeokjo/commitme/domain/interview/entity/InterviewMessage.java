package com.sipomeokjo.commitme.domain.interview.entity;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Document(collection = "interview_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewMessage {

    @Builder
    private InterviewMessage(
            Long interviewId,
            Integer turnNo,
            Integer questionOrder,
            String questionId,
            String question,
            Instant askedAt) {
        this.interviewId = interviewId;
        this.turnNo = turnNo;
        this.questionOrder = questionOrder;
        this.questionId = questionId;
        this.question = question;
        this.askedAt = askedAt;
    }

    public static InterviewMessage createFromQuestion(
            Long interviewId, Integer turnNo, String question, Instant askedAt) {
        return InterviewMessage.builder()
                .interviewId(interviewId)
                .turnNo(turnNo)
                .questionOrder(null)
                .questionId(null)
                .question(question)
                .askedAt(askedAt)
                .build();
    }

    public static InterviewMessage createFromGeneratedQuestion(
            Long interviewId, Integer questionOrder, String questionId, String question) {
        return InterviewMessage.builder()
                .interviewId(interviewId)
                .turnNo(null)
                .questionOrder(questionOrder)
                .questionId(questionId)
                .question(question)
                .askedAt(null)
                .build();
    }

    public static InterviewMessage createFollowUpQuestion(
            Long interviewId, Integer turnNo, String questionId, String question, Instant askedAt) {
        return InterviewMessage.builder()
                .interviewId(interviewId)
                .turnNo(turnNo)
                .questionOrder(null)
                .questionId(questionId)
                .question(question)
                .askedAt(askedAt)
                .build();
    }

    @Id private String id;

    @Indexed
    @Field("interview_id")
    private Long interviewId;

    @Field("turn_no")
    private Integer turnNo;

    @Field("question_order")
    private Integer questionOrder;

    @Field("question_id")
    private String questionId;

    @Field("question")
    private String question;

    @Field("answer_input_type")
    private AnswerInputType answerInputType;

    @Field("answer")
    private String answer;

    @Field("audio_url")
    private String audioUrl;

    @Field("asked_at")
    private Instant askedAt;

    @Field("ai_responded_at")
    private Instant aiRespondedAt;

    @Field("answered_at")
    private Instant answeredAt;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    public void updateAnswer(String answer, AnswerInputType answerInputType, String audioUrl) {
        this.answer = answer;
        this.answerInputType = answerInputType;
        this.audioUrl = audioUrl;
        this.answeredAt = Instant.now();
    }

    public void markAsked(Integer turnNo, Instant askedAt) {
        this.turnNo = turnNo;
        this.askedAt = askedAt;
    }
}
