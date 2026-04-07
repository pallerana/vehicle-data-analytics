package com.solera.interview.controller;

import com.solera.interview.dto.common.HealthResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public HealthResponseDto health() {
        return HealthResponseDto.builder()
                .status("UP")
                .message("Interview bootstrap service is running")
                .build();
    }
}
