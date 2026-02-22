package com.sipomeokjo.commitme.security.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        DispatcherType dispatcherType = request.getDispatcherType();
        if (dispatcherType == DispatcherType.ASYNC || dispatcherType == DispatcherType.ERROR) {
            return;
        }
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        APIResponse<Void> body = APIResponse.body(ErrorCode.UNAUTHORIZED);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
