package com.solera.interview.dto.user;

import java.util.List;

public record UserPageResponseDto(
        List<UserResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sortBy,
        String sortDirection
) {
}
