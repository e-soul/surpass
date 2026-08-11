package org.esoul.surpass.hello.internal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class CryptoSupport {

    public static final int AES_KEY_BYTES = 32;
    public static final int GCM_NONCE_BYTES = 12;
    public static final int GCM_TAG_BITS = 128;
    public static final int PASSWORD_SALT_BYTES = 16;
    public static final int MIN_PBKDF2_ITERATIONS = 120_000;
    private static final int MAX_PBKDF2_ITERATIONS = 2_000_000;
    private static final String ITERATIONS_PROPERTY = "org.esoul.surpass.hello.pbkdf2.iterations";
    private static volatile int calibratedIterations;

    private CryptoSupport() {
    }

    public static byte[] randomBytes(SecureRandom random, int length) {
        byte[] value = new byte[length];
        random.nextBytes(value);
        return value;
    }

    public static byte[] derivePasswordKey(char[] password, byte[] salt, int iterations)
            throws GeneralSecurityException {
        if (iterations < MIN_PBKDF2_ITERATIONS || iterations > MAX_PBKDF2_ITERATIONS) {
            throw new GeneralSecurityException("Invalid PBKDF2 iteration count");
        }
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, AES_KEY_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    public static int calibratedIterations(char[] password, SecureRandom random)
            throws GeneralSecurityException {
        int configured = Integer.getInteger(ITERATIONS_PROPERTY, 0);
        if (configured != 0) {
            if (configured < MIN_PBKDF2_ITERATIONS || configured > MAX_PBKDF2_ITERATIONS) {
                throw new GeneralSecurityException("Configured PBKDF2 iteration count is outside the allowed range");
            }
            return configured;
        }
        int result = calibratedIterations;
        if (result != 0) {
            return result;
        }
        synchronized (CryptoSupport.class) {
            if (calibratedIterations == 0) {
                byte[] salt = randomBytes(random, PASSWORD_SALT_BYTES);
                long started = System.nanoTime();
                byte[] probe = derivePasswordKey(password, salt, MIN_PBKDF2_ITERATIONS);
                long elapsed = Math.max(1L, System.nanoTime() - started);
                Arrays.fill(probe, (byte) 0);
                Arrays.fill(salt, (byte) 0);
                long scaled = (MIN_PBKDF2_ITERATIONS * 250_000_000L) / elapsed;
                calibratedIterations = (int) Math.clamp(scaled, MIN_PBKDF2_ITERATIONS,
                        MAX_PBKDF2_ITERATIONS);
            }
            return calibratedIterations;
        }
    }

    public static byte[] encryptAesGcm(byte[] key, byte[] nonce, byte[] plaintext, byte[] aad)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(plaintext);
    }

    public static byte[] decryptAesGcm(byte[] key, byte[] nonce, byte[] ciphertext, byte[] aad)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(ciphertext);
    }

    public static byte[] hkdfSha256(byte[] inputKey, byte[] info, int outputLength)
            throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        byte[] zeroSalt = new byte[mac.getMacLength()];
        mac.init(new SecretKeySpec(zeroSalt, "HmacSHA256"));
        byte[] prk = mac.doFinal(inputKey);
        Arrays.fill(zeroSalt, (byte) 0);
        try {
            byte[] result = new byte[outputLength];
            byte[] previous = new byte[0];
            int offset = 0;
            int counter = 1;
            while (offset < outputLength) {
                mac.init(new SecretKeySpec(prk, "HmacSHA256"));
                mac.update(previous);
                mac.update(info);
                mac.update((byte) counter++);
                byte[] block = mac.doFinal();
                Arrays.fill(previous, (byte) 0);
                int copy = Math.min(block.length, outputLength - offset);
                System.arraycopy(block, 0, result, offset, copy);
                offset += copy;
                previous = block;
            }
            Arrays.fill(previous, (byte) 0);
            return result;
        } finally {
            Arrays.fill(prk, (byte) 0);
        }
    }

    public static byte[] aad(String label, int version, byte[] vaultId) {
        byte[] labelBytes = label.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(labelBytes.length + Integer.BYTES + vaultId.length);
        return buffer.put(labelBytes).putInt(version).put(vaultId).array();
    }
}
