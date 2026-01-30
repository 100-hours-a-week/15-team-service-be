package com.sipomeokjo.commitme.domain.company.dto;

import jakarta.validation.constraints.Size;

public record CompanyUpdateRequest(
        @Size(max = 200) String name, String preferred, String idealTalent) {}
