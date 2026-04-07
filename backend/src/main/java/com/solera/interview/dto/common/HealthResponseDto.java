package com.solera.interview.dto.common;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HealthResponseDto {
    String status;
    String message;
}
