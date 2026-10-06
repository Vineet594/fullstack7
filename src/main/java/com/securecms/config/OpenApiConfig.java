package com.securecms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Arrays;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI secureCmsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Secure Content Management & User Management System API")
                        .version("1.0.0")
                        .description("""
                                REST API with JWT authentication (access + refresh tokens), role-based access control,
                                AES-GCM encryption, pagination, sorting, caching and optimised queries.

                                **How to test:** call POST /api/auth/login, copy the accessToken, click the
                                **Authorize** button and paste the token (without the word Bearer).""")
                        .contact(new Contact().name("Secure CMS Team")))
                .tags(List.of(
                        new Tag().name("Authentication").description("Register, login, refresh token, logout"),
                        new Tag().name("Users").description("User management (mostly ADMIN only)"),
                        new Tag().name("Posts").description("Content posts with pagination and sorting"),
                        new Tag().name("Categories").description("Cached post categories"),
                        new Tag().name("Roles").description("Available roles (ADMIN only)"),
                        new Tag().name("Performance").description("N+1 / cache benchmark (ADMIN only)")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the accessToken returned by POST /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /** Adds the standard error status codes to every documented operation. */
    @Bean
    public OperationCustomizer standardErrorResponses() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            addIfMissing(responses, "400", "Bad request / validation failed");
            addIfMissing(responses, "401", "Unauthorized - missing, invalid or expired credentials");
            addIfMissing(responses, "403", "Forbidden - authenticated but not allowed");
            boolean hasPathVariable = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(p -> p.hasParameterAnnotation(PathVariable.class));
            if (hasPathVariable) {
                addIfMissing(responses, "404", "Resource not found");
            }
            if (handlerMethod.hasMethodAnnotation(PostMapping.class)) {
                addIfMissing(responses, "409", "Conflict - duplicate or in-use resource");
            }
            addIfMissing(responses, "500", "Internal server error");
            return operation;
        };
    }

    private void addIfMissing(ApiResponses responses, String code, String description) {
        if (responses != null && !responses.containsKey(code)) {
            responses.addApiResponse(code, new ApiResponse().description(description));
        }
    }
}
