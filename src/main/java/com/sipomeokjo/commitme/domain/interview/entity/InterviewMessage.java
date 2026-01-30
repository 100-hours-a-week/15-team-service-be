package com.sipomeokjo.commitme.domain.interview.entity;

import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "interview_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(name = "turn_no", nullable = false)
    private Integer turnNo;

    @Column(name = "question", columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_input_type")
    private AnswerInputType answerInputType;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "asked_at", nullable = false)
    private Instant askedAt;

    @Column(name = "ai_responded_at")
    private Instant aiRespondedAt;

    @Column(name = "answered_at")
    private Instant answeredAt;
}
