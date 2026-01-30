package com.sipomeokjo.commitme.domain.position.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.position.dto.PositionResponse;
import com.sipomeokjo.commitme.domain.position.service.PositionQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionQueryService positionQueryService;

    @GetMapping
    public ResponseEntity<APIResponse<List<PositionResponse>>> getPositions() {
        return APIResponse.onSuccess(
                SuccessCode.POSITION_LIST_FETCHED, positionQueryService.getPositions());
    }
}
