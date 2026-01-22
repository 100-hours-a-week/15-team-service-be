package com.sipomeokjo.commitme.domain.resume.dto;

import java.util.List;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResumeCreateRequest {
    private List<String> repos;
    private Long positionId;
    private Long companyId;
    private String name;
}
