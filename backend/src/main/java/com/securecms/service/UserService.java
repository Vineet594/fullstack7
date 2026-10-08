package com.securecms.service;

import com.securecms.dto.CreateUserRequest;
import com.securecms.dto.PageResponse;
import com.securecms.dto.UserResponse;
import com.securecms.encryption.EncryptionService;
import com.securecms.entity.Role;
import com.securecms.entity.RoleName;
import com.securecms.entity.User;
import com.securecms.exception.ConflictException;
import com.securecms.exception.ResourceNotFoundException;
import com.securecms.exception.UserAlreadyExistsException;
import com.securecms.mapper.UserMapper;
import com.securecms.repository.PostRepository;
import com.securecms.repository.RefreshTokenRepository;
import com.securecms.repository.RoleRepository;
import com.securecms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PostRepository postRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(String username, String email, String rawPassword, String phone, RoleName roleName) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("USERNAME_TAKEN", "Username is already taken");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("EMAIL_TAKEN", "Email is already registered");
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not initialised: " + roleName));
        User saved = userRepository.save(User.builder()
                .username(username)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(rawPassword))           // BCrypt hash
                .encryptedPhone(encryptionService.encrypt(phone))        // AES-GCM
                .role(role)
                .build());
        log.info("User created: id={} role={}", saved.getId(), roleName);
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        return createUser(request.username(), request.email(), request.password(), request.phone(), request.role());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public UserResponse updateRole(Long id, RoleName roleName) {
        User user = findOrThrow(id);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not initialised: " + roleName));
        user.setRole(role);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new ConflictException("CANNOT_DELETE_SELF", "You cannot delete your own account");
        }
        User user = findOrThrow(id);
        refreshTokenRepository.deleteAllForUser(id);
        postRepository.deleteByAuthorId(id);
        userRepository.delete(user);
        log.info("User deleted: id={}", id);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found with id " + id));
    }
}
