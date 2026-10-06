package com.securecms.dto;

import com.securecms.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Used by ADMIN to create a user with an explicit role. */
public record CreateUserRequest(
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username may contain only letters, digits, '.', '_' and '-'")
        String username,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email must be a valid email address")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
        String password,

        @Pattern(regexp = "^[0-9+\\-() ]{7,20}$", message = "Phone number must be 7-20 characters (digits, +, -, spaces, brackets)")
        String phone,

        @NotNull(message = "Role must be provided (ADMIN or USER)")
        RoleName role) {
}
