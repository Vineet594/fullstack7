package com.securecms.encryption;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private final EncryptionService service = new EncryptionService("unit-test-secret-0123456789abcdef0123");

    @Test
    void encryptThenDecryptReturnsOriginalValue() {
        String encrypted = service.encrypt("+919876543210");
        assertNotEquals("+919876543210", encrypted);
        assertEquals("+919876543210", service.decrypt(encrypted));
    }

    @Test
    void sameInputProducesDifferentCiphertextBecauseOfRandomIv() {
        assertNotEquals(service.encrypt("1234567890"), service.encrypt("1234567890"));
    }

    @Test
    void tamperedCiphertextIsRejected() {
        String encrypted = service.encrypt("1234567890");
        char last = encrypted.charAt(encrypted.length() - 5);
        String tampered = encrypted.substring(0, encrypted.length() - 5) + (last == 'A' ? 'B' : 'A')
                + encrypted.substring(encrypted.length() - 4);
        assertThrows(IllegalStateException.class, () -> service.decrypt(tampered));
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalStateException.class, () -> new EncryptionService("too-short"));
    }
}
