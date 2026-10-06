package com.securecms.bootstrap;

import com.securecms.encryption.EncryptionService;
import com.securecms.entity.Category;
import com.securecms.entity.Post;
import com.securecms.entity.Role;
import com.securecms.entity.RoleName;
import com.securecms.entity.User;
import com.securecms.repository.CategoryRepository;
import com.securecms.repository.PostRepository;
import com.securecms.repository.RoleRepository;
import com.securecms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DEVELOPMENT-ONLY demo data (1 ADMIN, 3 USERs, 5 categories, 24 posts).
 * Runs only when app.seed.enabled=true and the users table is empty.
 * Set SEED_ENABLED=false in any real environment - the passwords below are public demo passwords!
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class SampleDataLoader implements CommandLineRunner {

    // DEV ONLY demo credentials
    private static final String DEMO_ADMIN_PASSWORD = "Admin@12345";
    private static final String DEMO_USER_PASSWORD = "User@12345";

    private static final String[] POST_TITLES = {
            "Getting Started with Spring Boot 3", "Understanding REST API Design Principles",
            "JWT Authentication Explained Simply", "Mastering Spring Data JPA Relationships",
            "Caching Strategies for Backend Developers", "Why Database Indexes Matter",
            "Effective Study Techniques for Engineering Students", "How to Prepare for Technical Viva Examinations",
            "Time Management for College Students", "Building a Learning Roadmap for Java Developers",
            "Five Minute Desk Exercises for Students", "Healthy Eating on a Student Budget",
            "Sleep and Productivity Basics", "Managing Exam Stress Mindfully",
            "Budget Backpacking Across India", "Weekend Trips Near Your City",
            "Packing Checklist for Short Trips", "Train Travel Tips for First-Time Travellers",
            "Writing a Standout Developer Resume", "Preparing for Campus Placements",
            "How to Contribute to Open Source", "Building a Portfolio with Real Projects",
            "Docker Basics for Beginners", "Introduction to Microservices Architecture"
    };
    // index into the categories list for each title above
    private static final int[] POST_CATEGORY = {
            0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 0, 0
    };

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Sample data skipped: users already exist");
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        Role userRole = roleRepository.findByName(RoleName.USER).orElseThrow();

        User admin = createUser("admin", "admin@securecms.local", DEMO_ADMIN_PASSWORD, "9000000001", adminRole);
        User vineet = createUser("vineet", "vineet@securecms.local", DEMO_USER_PASSWORD, "9000000002", userRole);
        User anita = createUser("anita", "anita@securecms.local", DEMO_USER_PASSWORD, "9000000003", userRole);
        User rohan = createUser("rohan", "rohan@securecms.local", DEMO_USER_PASSWORD, "9000000004", userRole);
        List<User> authors = List.of(vineet, anita, rohan, admin);

        List<Category> categories = new ArrayList<>();
        categories.add(createCategory("Technology", "Programming, frameworks and software engineering"));
        categories.add(createCategory("Education", "Study tips and exam preparation"));
        categories.add(createCategory("Health & Fitness", "Staying healthy as a student"));
        categories.add(createCategory("Travel", "Trips, tips and itineraries"));
        categories.add(createCategory("Career", "Resumes, placements and growth"));

        for (int i = 0; i < POST_TITLES.length; i++) {
            Post post = new Post();
            post.setTitle(POST_TITLES[i]);
            post.setContent("Sample article: " + POST_TITLES[i] + ". This demo content exists so that pagination, "
                    + "sorting, caching and query optimisation can be demonstrated with realistic data.");
            post.setCategory(categories.get(POST_CATEGORY[i]));
            post.setAuthor(authors.get(i % authors.size()));
            // spread creation dates so that "sort=createdAt,desc" shows a visible order
            post.setCreatedAt(Instant.now().minus(Duration.ofDays(POST_TITLES.length - i)));
            postRepository.save(post);
        }

        log.warn("DEV sample data loaded (admin / {} users / {} categories / {} posts). "
                        + "Set SEED_ENABLED=false outside development!",
                authors.size() - 1, categories.size(), POST_TITLES.length);
    }

    private User createUser(String username, String email, String rawPassword, String phone, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEncryptedPhone(encryptionService.encrypt(phone));
        user.setRole(role);
        return userRepository.save(user);
    }

    private Category createCategory(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }
}
