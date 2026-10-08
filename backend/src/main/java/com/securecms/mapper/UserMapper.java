package com.securecms.mapper;

import com.securecms.dto.UserResponse;
import com.securecms.encryption.EncryptionService;
import com.securecms.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final EncryptionService encryptionService;

    public UserResponse toResponse(User user) {
        String phone = encryptionService.decrypt(user.getEncryptedPhone());
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), mask(phone),
                user.getRole().getName().name(), user.getCreatedAt());
    }

    private String mask(String phone) {
        if (phone == null || phone.length() <= 4) {
            return phone;
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
