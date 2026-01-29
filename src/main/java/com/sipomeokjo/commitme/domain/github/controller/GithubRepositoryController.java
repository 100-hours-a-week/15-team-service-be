package com.sipomeokjo.commitme.domain.github.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.github.dto.RepoSummaryDto;
import com.sipomeokjo.commitme.domain.github.service.GithubRepositoryService;
import com.sipomeokjo.commitme.security.SecurityUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/repositories")
public class GithubRepositoryController {

    private final GithubRepositoryService githubRepositoryService;

    @GetMapping
    public ResponseEntity<APIResponse<List<RepoSummaryDto>>> list() {
        Long userId = SecurityUtil.currentUserId();
        List<RepoSummaryDto> data = githubRepositoryService.listMyRepos(userId);
        return APIResponse.onSuccess(SuccessCode.OK, data);
    }
}
