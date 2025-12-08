package com.hft.wallet.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Service for encrypting and decrypting sensitive data.
 * Uses AES-256 encryption.
 */
@Slf4j
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";

    @Value("${wallet.encryption.key:default-dev-key-do-not-use-in-prod}")
    private String secretKey;

    /**
     * Encrypts the given value using AES.
     */
    public String encrypt(String value) {
        try {
            SecretKeySpec key = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedByteValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedByteValue);
        } catch (Exception e) {
            log.error("Error encrypting data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts the given value using AES.
     */
    public String decrypt(String value) {
        try {
            SecretKeySpec key = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedByteValue = cipher.doFinal(Base64.getDecoder().decode(value));
            return new String(decryptedByteValue, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private SecretKeySpec generateKey(String key) throws Exception {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        keyBytes = sha.digest(keyBytes);
        keyBytes = Arrays.copyOf(keyBytes, 16); // Use 128 bit key for simplicity/compatibility
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}
