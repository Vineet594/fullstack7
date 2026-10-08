package com.securecms.controller;

import com.securecms.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Hidden
@SecurityRequirements
public class HomeController {

    @GetMapping("/")
    public ApiResponse<Map<String, String>> home() {
        return ApiResponse.ok("Secure CMS API is running", Map.of(
                "documentation", "/swagger-ui.html",
                "health", "/actuator/health",
                "version", "1.0.0"));
    }
}
