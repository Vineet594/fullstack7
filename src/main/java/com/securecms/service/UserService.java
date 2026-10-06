package com.securecms.service;

import com.securecms.cache.CacheNames;
import com.securecms.dto.UserResponse;
import com.securecms.encryption.EncryptionService;
import com.securecms.entity.Role;
import com.securecms.entity.RoleName;
import com.securecms.entity.User;
import com.securecms.exception.BadRequestException;
import com.securecms.exception.ResourceNotFoundException;
import com.securecms.exception.UnauthorizedException;
import com.securecms.exception.UserAlreadyExistsException;
import com.securecms.mapper.UserMapper;
import com.securecms.repository.RefreshTokenRepository;
import com.securecms.repository.RoleRepository;
import com.securecms.repository.UserRepository;
import com.securecms.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;
    private final UserMapper userMapper;

    /** Shared by public registration (role USER) and the ADMIN "create user" endpoint. */
    @Transactional
    public UserResponse createUser(String username, String email, String rawPassword,
                                   String phone, RoleName roleName) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));

        User user = new User();
        user.setUsername(username);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));            // BCrypt hash (one-way)
        if (phone != null && !phone.isBlank()) {
            user.setEncryptedPhone(encryptionService.encrypt(phone.trim())); // AES-GCM (reversible)
        }
        user.setRole(role);

        User saved = userRepository.save(user);
        log.info("User created: id={}, username={}, role={}", saved.getId(), saved.getUsername(), roleName);
        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(loadUser(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(AuthenticatedUser principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        return findById(principal.getId());
    }

    @Transactional
    public UserResponse changeRole(Long id, RoleName newRole) {
        User user = loadUser(id);
        Role role = roleRepository.findByName(newRole)
                .orElseThrow(() -> new ResourceNotFoundException("Role", newRole));
        user.setRole(role);
        // The role is embedded in the JWT, so force a fresh login for this user
        refreshTokenRepository.revokeAllByUserId(id);
        log.info("Role of userId={} changed to {}", id, newRole);
        return userMapper.toResponse(user);
    }

    // The user's posts are removed too (cascade), so cached posts must be dropped
    @Transactional
    @CacheEvict(cacheNames = CacheNames.POST, allEntries = true)
    public void delete(Long id, AuthenticatedUser actor) {
        if (actor.getId().equals(id)) {
            throw new BadRequestException("CANNOT_DELETE_SELF", "You cannot delete your own account");
        }
        User user = loadUser(id);
        refreshTokenRepository.deleteAllByUserId(id);
        userRepository.delete(user);
        log.info("User deleted: id={} by adminId={}", id, actor.getId());
    }

    private User loadUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
