package com.securecms.config;

import com.securecms.encryption.EncryptionService;
import com.securecms.entity.*;
import com.securecms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Always makes sure the ADMIN and USER roles exist. When app.seed.enabled=true (SEED_SAMPLE_DATA) and the
 * database has no users yet, it also inserts DEMO data: 1 admin, 3 users, 6 categories and 24 posts.
 * DEMO CREDENTIALS ARE FOR DEVELOPMENT/DEMO ONLY.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String[] TITLES = {
            "Getting started with Spring Boot", "Understanding REST principles", "JWT explained simply",
            "Role-based access control in practice", "Why AES-GCM beats AES-CBC", "Fixing the N+1 query problem",
            "Pagination done right", "Caching with Caffeine", "Designing database indexes", "Global exception handling",
            "Correlation IDs for tracing", "Deploying Spring Boot on Render", "Hosting a frontend on Netlify",
            "Writing integration tests", "Refresh token rotation", "BCrypt vs encryption",
            "Swagger and OpenAPI tips", "Layered architecture explained", "DTOs versus entities",
            "Logging best practices", "Validating request bodies", "CORS without headaches",
            "Docker for Java developers", "Query optimisation checklist"};

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.seed.demo-password}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(String... args) {
        for (RoleName name : RoleName.values()) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(Role.builder().name(name).build());
            }
        }
        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        Role userRole = roleRepository.findByName(RoleName.USER).orElseThrow();

        User admin = newUser("admin", "admin@securecms.dev", "+911234567890", adminRole);
        User alice = newUser("alice", "alice@securecms.dev", "+919876543210", userRole);
        User bob = newUser("bob", "bob@securecms.dev", "+919123456780", userRole);
        User charlie = newUser("charlie", "charlie@securecms.dev", "+918765432109", userRole);
        List<User> authors = userRepository.saveAll(List.of(admin, alice, bob, charlie));

        List<Category> categories = categoryRepository.saveAll(List.of(
                category("Backend", "Server-side development topics"),
                category("Security", "Authentication, authorization and cryptography"),
                category("Database", "Schema design, indexing and queries"),
                category("DevOps", "Deployment, containers and CI/CD"),
                category("Testing", "Unit and integration testing"),
                category("Architecture", "Design patterns and project structure")));

        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < TITLES.length; i++) {
            posts.add(Post.builder()
                    .title(TITLES[i])
                    .content("Sample article #" + (i + 1) + ": " + TITLES[i]
                            + ". This demo content exists so pagination, sorting and caching can be demonstrated.")
                    .category(categories.get(i % categories.size()))
                    .author(authors.get(i % authors.size()))
                    .build());
        }
        postRepository.saveAll(posts);
        log.info("Demo data created: {} users, {} categories, {} posts (DEMO CREDENTIALS - not for production)",
                authors.size(), categories.size(), posts.size());
    }

    private User newUser(String username, String email, String phone, Role role) {
        return User.builder().username(username).email(email)
                .password(passwordEncoder.encode(demoPassword))
                .encryptedPhone(encryptionService.encrypt(phone))
                .role(role).build();
    }

    private Category category(String name, String description) {
        return Category.builder().name(name).description(description).build();
    }
}
