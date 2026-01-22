package com.sipomeokjo.commitme.domain.position.controller;

import com.sipomeokjo.commitme.api.response.ApiResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.position.dto.PositionResponse;
import com.sipomeokjo.commitme.domain.position.service.PositionQueryService;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/positions")
public class PositionController {

    private final PositionQueryService positionQueryService;

    public PositionController(PositionQueryService positionQueryService) {
        this.positionQueryService = positionQueryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PositionResponse>>> getPositions() {
        return ApiResponse.onSuccess(SuccessCode.POSITION_LIST_FETCHED, positionQueryService.getPositions());
    }
}
