package com.sipomeokjo.commitme.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.response.ApiResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
	
	private final ObjectMapper objectMapper;
	
	@Override
	public void handle(HttpServletRequest request,
					   HttpServletResponse response,
					   AccessDeniedException accessDeniedException) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), ApiResponse.onFailure(ErrorCode.FORBIDDEN));
	}
}
