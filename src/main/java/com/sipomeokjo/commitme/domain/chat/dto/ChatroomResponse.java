package com.sipomeokjo.commitme.domain.chat.dto;

import java.time.Instant;

public record ChatroomResponse(
		Long id,
		String name,
		String lastMessage,
		Instant lastUpdatedAt
) { }
