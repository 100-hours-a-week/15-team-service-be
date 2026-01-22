package com.sipomeokjo.commitme.security;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record CustomUserDetails(
		Long userId,
		UserStatus userStatus,
		List<GrantedAuthority> authorities) implements UserDetails {
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}
	
	@Override
	public String getPassword() {
		return "";
	}
	
	@Override
	public String getUsername() {
		return "";
	}
}
