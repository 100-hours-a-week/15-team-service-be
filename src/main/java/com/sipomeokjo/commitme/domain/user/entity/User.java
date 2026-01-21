package com.sipomeokjo.commitme.domain.user.entity;

import com.sipomeokjo.commitme.domain.position.entity.Position;
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
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "position_id", nullable = false)
	private Position position;

	@Column(nullable = false, length = 10)
	private String name;

	@Column(length = 20)
	private String phone;

	@Column(name = "profile_image_url", length = 1024)
	private String profileImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "deleted_at")
	private Instant deletedAt;
}
