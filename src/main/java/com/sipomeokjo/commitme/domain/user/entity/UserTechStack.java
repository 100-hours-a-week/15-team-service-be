package com.sipomeokjo.commitme.domain.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_tech_stacks")
@IdClass(UserTechStackId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTechStack {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tech_stack_id", nullable = false)
    private TechStack techStack;

    public static UserTechStack create(User user, TechStack techStack) {
        UserTechStack userTechStack = new UserTechStack();
        userTechStack.user = user;
        userTechStack.techStack = techStack;
        return userTechStack;
    }
}
