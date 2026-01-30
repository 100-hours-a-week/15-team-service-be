package com.sipomeokjo.commitme.security.resolver;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.security.handler.CustomUserDetails;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        CurrentUserId annotation = parameter.getParameterAnnotation(CurrentUserId.class);
        boolean required = annotation == null || annotation.required();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            if (required) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails details) {
            return details.userId();
        }

        if (required) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return null;
    }
}
