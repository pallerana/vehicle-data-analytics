package com.solera.interview.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequestDto(
        @NotBlank(message = "firstName is required")
        @Size(max = 100, message = "firstName must be 100 characters or less")
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 100, message = "lastName must be 100 characters or less")
        String lastName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 255, message = "email must be 255 characters or less")
        String email
) {
}
