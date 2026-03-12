package com.sipomeokjo.commitme.domain.position.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.position.dto.PositionCacheRefreshResponse;
import com.sipomeokjo.commitme.domain.position.service.PositionCacheRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/positions/cache")
@RequiredArgsConstructor
public class InternalPositionCacheController {
    private final PositionCacheRefreshService positionCacheRefreshService;

    @PostMapping("/refresh")
    public ResponseEntity<APIResponse<PositionCacheRefreshResponse>> refresh() {
        return APIResponse.onSuccess(
                SuccessCode.OK, positionCacheRefreshService.refreshAllInstances());
    }
}
