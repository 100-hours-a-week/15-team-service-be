package com.sipomeokjo.commitme.domain.user.repository;

import com.sipomeokjo.commitme.domain.user.entity.TechStack;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechStackRepository extends JpaRepository<TechStack, Long> {
    List<TechStack> findAllByNameNormalizedIn(Collection<String> nameNormalized);
}
