package com.securecms.bootstrap;

import com.securecms.entity.Role;
import com.securecms.entity.RoleName;
import com.securecms.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Makes sure the ADMIN and USER roles always exist (runs in every environment). */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
                log.info("Created role {}", roleName);
            }
        }
    }
}
