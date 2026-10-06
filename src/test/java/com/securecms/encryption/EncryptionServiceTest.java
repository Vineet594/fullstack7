package com.securecms.encryption;

import com.securecms.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // test-only key: 32 zero bytes (never use such a key outside tests)
        encryptionService = new EncryptionService(Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void encryptThenDecrypt_returnsOriginalValue() {
        String encrypted = encryptionService.encrypt("9876543210");

        assertThat(encrypted).isNotEqualTo("9876543210");
        assertThat(encryptionService.decrypt(encrypted)).isEqualTo("9876543210");
    }

    @Test
    void encryptingSameValueTwice_givesDifferentCiphertexts() {
        String first = encryptionService.encrypt("9876543210");
        String second = encryptionService.encrypt("9876543210");

        assertThat(first).isNotEqualTo(second); // random IV every time
    }

    @Test
    void tamperedCiphertext_isRejected() {
        byte[] bytes = Base64.getDecoder().decode(encryptionService.encrypt("9876543210"));
        bytes[bytes.length - 1] ^= 0x01; // flip one bit
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> encryptionService.decrypt(tampered)).isInstanceOf(EncryptionException.class);
    }

    @Test
    void invalidKeyLength_failsFast() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[10]);

        assertThatThrownBy(() -> new EncryptionService(shortKey)).isInstanceOf(IllegalStateException.class);
    }
}
