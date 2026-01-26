package com.sipomeokjo.commitme.domain.company.dto;

public record CompanyResponse(
        Long id,
        String name,
        String preferred,
        String idealTalent,
        boolean isVerified
) {}
