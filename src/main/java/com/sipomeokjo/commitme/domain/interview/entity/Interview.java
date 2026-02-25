package com.sipomeokjo.commitme.domain.interview.entity;

import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.user.entity.User;
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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "interview")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interview extends BaseEntity {

    @Builder
    private Interview(
            User user,
            Position position,
            Company company,
            String name,
            InterviewType interviewType,
            String aiSessionId,
            Instant startedAt) {
        this.user = user;
        this.position = position;
        this.company = company;
        this.name = name;
        this.interviewType = interviewType;
        this.aiSessionId = aiSessionId;
        this.startedAt = startedAt;
    }

    public static Interview create(
            User user,
            Position position,
            Company company,
            String name,
            InterviewType interviewType) {
        return Interview.builder()
                .user(user)
                .position(position)
                .company(company)
                .name(name)
                .interviewType(interviewType)
                .aiSessionId(UUID.randomUUID().toString())
                .startedAt(Instant.now())
                .build();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false)
    private InterviewType interviewType;

    @Column(name = "total_feedback", columnDefinition = "TEXT")
    private String totalFeedback;

    @Column(name = "ai_session_id", length = 36)
    private String aiSessionId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public void updateName(String name) {
        this.name = name;
    }

    public void end() {
        this.endedAt = Instant.now();
    }

    public void updateFeedback(String totalFeedback) {
        this.totalFeedback = totalFeedback;
    }

    public void updateAiSessionId(String aiSessionId) {
        this.aiSessionId = aiSessionId;
    }

    public boolean isEnded() {
        return this.endedAt != null;
    }
}
