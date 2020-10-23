package org.esoul.surpass.core.test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.BadPaddingException;

import org.esoul.surpass.core.SimpleCipher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SimpleCipherTest {

    private static final byte[] DATA = "AAAA".getBytes(StandardCharsets.UTF_8);

    private static final char[] KEY = "123".toCharArray();

    private static final int EXPECTED_CIPHER_TEXT_LEN = SimpleCipher.VERSION_LEN + SimpleCipher.SALT_LEN + SimpleCipher.IV_LEN + 16;

    private SimpleCipher cipher = new SimpleCipher();

    @Test
    public void testEncrypt() throws Exception {
        byte[] cipherText = cipher.encrypt(KEY, DATA);
        Assertions.assertEquals(EXPECTED_CIPHER_TEXT_LEN, cipherText.length, "Unexpected cipher text length!");
    }

    @Test
    public void testDecrypt() throws Exception {
        byte[] data = cipher.decrypt(KEY, cipher.encrypt(KEY, DATA));
        Assertions.assertTrue(Arrays.equals(DATA, data), "Incorrect decryption!");
    }

    @Test
    public void testDecryptNegative() throws Exception {
        Assertions.assertThrows(BadPaddingException.class, () -> cipher.decrypt("103".toCharArray(), cipher.encrypt(KEY, DATA)));
    }
}
