package com.example.testapp.core;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class CredentialHasher {

    private static final int ITERATIONS = 20_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hash(String rawSecret) {
        if (rawSecret == null || rawSecret.isEmpty()) {
            return "";
        }
        try {
            byte[] salt = new byte[SALT_BYTES];
            RANDOM.nextBytes(salt);

            byte[] hash = pbkdf2(rawSecret.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            return "pbkdf2:" + toHex(salt) + ":" + toHex(hash);
        } catch (Exception e) {
            // Fallback to SHA-256 with salt if PBKDF2 algorithm is unavailable
            return sha256Fallback(rawSecret);
        }
    }

    public static boolean verify(String rawSecret, String storedHashOrPlain) {
        if (rawSecret == null || storedHashOrPlain == null) {
            return false;
        }
        if (storedHashOrPlain.startsWith("pbkdf2:")) {
            String[] parts = storedHashOrPlain.split(":");
            if (parts.length != 3) return false;
            byte[] salt = fromHex(parts[1]);
            byte[] expectedHash = fromHex(parts[2]);

            try {
                byte[] actualHash = pbkdf2(rawSecret.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
                return MessageDigest.isEqual(expectedHash, actualHash);
            } catch (Exception e) {
                return false;
            }
        } else if (storedHashOrPlain.startsWith("sha256:")) {
            String[] parts = storedHashOrPlain.split(":");
            if (parts.length != 3) return false;
            byte[] salt = fromHex(parts[1]);
            byte[] expectedHash = fromHex(parts[2]);

            byte[] actualHash = sha256(rawSecret, salt);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } else {
            // Legacy plaintext comparison
            return MessageDigest.isEqual(rawSecret.getBytes(), storedHashOrPlain.getBytes());
        }
    }

    public static boolean isHashed(String stored) {
        return stored != null && (stored.startsWith("pbkdf2:") || stored.startsWith("sha256:"));
    }

    public static boolean isTrivialPin(String pin) {
        if (pin == null || pin.length() < 4) return true;
        String clean = pin.trim();
        String[] trivial = {"0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999", "1234", "4321", "1212", "6969"};
        for (String t : trivial) {
            if (clean.equals(t)) return true;
        }
        return false;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private static String sha256Fallback(String secret) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = sha256(secret, salt);
        return "sha256:" + toHex(salt) + ":" + toHex(hash);
    }

    private static byte[] sha256(String secret, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(secret.getBytes("UTF-8"));
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] fromHex(String hex) {
        if (hex == null || hex.length() % 2 != 0) return new byte[0];
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
