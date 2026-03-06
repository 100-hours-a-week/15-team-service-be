package com.sipomeokjo.commitme.domain.user.repository;

import com.sipomeokjo.commitme.domain.user.entity.UserCertificate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCertificateRepository extends JpaRepository<UserCertificate, Long> {
    List<UserCertificate> findAllByUser_IdOrderByIdAsc(Long userId);
}
