package org.esoul.surpass.core.test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.BadPaddingException;

import org.esoul.surpass.core.SimpleCipher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SimpleCipherTest {

    private static final byte[] CLEAR_TEXT = "AAAA".getBytes(StandardCharsets.UTF_8);

    private static final char[] KEY = "123".toCharArray();

    private static final int EXPECTED_CIPHER_TEXT_LEN = SimpleCipher.VERSION_LEN + SimpleCipher.SALT_LEN + SimpleCipher.IV_LEN + 16;

    private SimpleCipher cipher = new SimpleCipher();

    @Test
    public void testEncrypt() throws Exception {
        byte[] cipherText = cipher.encrypt(KEY, CLEAR_TEXT);
        Assertions.assertEquals(EXPECTED_CIPHER_TEXT_LEN, cipherText.length, "Unexpected cipher text length!");
    }

    @Test
    public void testDecrypt() throws Exception {
        byte[] data = cipher.decrypt(KEY, cipher.encrypt(KEY, CLEAR_TEXT));
        Assertions.assertTrue(Arrays.equals(CLEAR_TEXT, data), "Incorrect decryption!");
    }

    @Test
    public void testDecryptNegative() throws Exception {
        Assertions.assertThrows(BadPaddingException.class, () -> cipher.decrypt("103".toCharArray(), cipher.encrypt(KEY, CLEAR_TEXT)));
    }

    @Test
    public void testDigest() throws Exception {
        char[] digest = cipher.digest("AAAA".toCharArray());
        Assertions.assertEquals(
                "53b74be8b295b733fdfafbd7d2a22b1686733740de7fdc592b26cf3e1874cfce158170ce9230e24696331a61829244e5d9f48abdacc9ffa8c4cb498724844cf8",
                new String(digest));
    }
}
