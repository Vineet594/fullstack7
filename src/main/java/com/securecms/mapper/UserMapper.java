package com.securecms.mapper;

import com.securecms.dto.UserResponse;
import com.securecms.encryption.EncryptionService;
import com.securecms.entity.User;
import com.securecms.exception.EncryptionException;
import com.securecms.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Entity -> DTO. Decrypts the phone number (AES) and masks it before it leaves the application. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final EncryptionService encryptionService;

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                readMaskedPhone(user),
                user.getRole().getName(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private String readMaskedPhone(User user) {
        if (user.getEncryptedPhone() == null) {
            return null;
        }
        try {
            return MaskUtil.maskPhone(encryptionService.decrypt(user.getEncryptedPhone()));
        } catch (EncryptionException ex) {
            // e.g. the AES key was changed. Never log the stored value itself.
            log.warn("Could not decrypt phone number for userId={}", user.getId());
            return null;
        }
    }
}
