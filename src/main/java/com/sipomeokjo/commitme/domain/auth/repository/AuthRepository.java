package com.sipomeokjo.commitme.domain.auth.repository;

import com.sipomeokjo.commitme.domain.auth.entity.Auth;
import com.sipomeokjo.commitme.domain.auth.entity.AuthProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRepository extends JpaRepository<Auth, Long> {
    Optional<Auth> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<Auth> findByUser_IdAndProvider(Long userId, AuthProvider provider);

    List<Auth> findAllByUser_Id(Long userId);

    List<Auth> findAllByProviderAndProviderUserIdStartingWith(
            AuthProvider provider, String providerUserIdPrefix);
}
