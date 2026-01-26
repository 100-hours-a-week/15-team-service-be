package com.sipomeokjo.commitme.domain.company.repository;

import com.sipomeokjo.commitme.domain.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByName(String name);
}
