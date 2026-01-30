package com.sipomeokjo.commitme.security;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.security.handler.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {
	
	private SecurityUtil() {}
	
	public static Long currentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		
		Object principal = auth.getPrincipal();
		
		// JwtFilter에서 principal로 CustomUserDetails 넣는 구조
		if (principal instanceof CustomUserDetails cud) {
			if (cud.userId() == null) {
				throw new BusinessException(ErrorCode.UNAUTHORIZED);
			}
			return cud.userId();
		}
		
		// 혹시 다른 Provider가 Long을 principal로 넣는 케이스 대비 (AccessTokenProvider.getAuthentication 등)
		if (principal instanceof Long userId) {
			return userId;
		}
		
		throw new BusinessException(ErrorCode.UNAUTHORIZED);
	}
}
