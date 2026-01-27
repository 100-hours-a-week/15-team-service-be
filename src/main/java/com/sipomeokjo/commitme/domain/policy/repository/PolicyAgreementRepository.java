package com.sipomeokjo.commitme.domain.policy.repository;

import com.sipomeokjo.commitme.domain.policy.entity.PolicyAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyAgreementRepository extends JpaRepository<PolicyAgreement, Long> {
}
