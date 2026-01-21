package com.sipomeokjo.commitme.dto.resume;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ResumeCreateRequest {
    private List<String> repos;
    private String position;
    private String company;
}
