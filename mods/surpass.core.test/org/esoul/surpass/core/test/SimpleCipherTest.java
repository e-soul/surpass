package org.esoul.surpass.core.test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.BadPaddingException;

import org.esoul.surpass.core.SimpleCipher;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author mgp
 */
public class SimpleCipherTest {

    private static final byte[] DATA = "AAAA".getBytes(StandardCharsets.UTF_8);

    private static final char[] KEY = "123".toCharArray();

    private static final int EXPECTED_CIPHER_TEXT_LEN = SimpleCipher.VERSION_LEN + SimpleCipher.SALT_LEN + SimpleCipher.IV_LEN + 16;

    private SimpleCipher cipher = new SimpleCipher();

    @Test
    public void testEncrypt() throws Exception {
        byte[] cipherText = cipher.encrypt(KEY, DATA);
        Assert.assertEquals("Unexpected cipher text length!", EXPECTED_CIPHER_TEXT_LEN, cipherText.length);
    }

    @Test
    public void testDecrypt() throws Exception {
        byte[] data = cipher.decrypt(KEY, cipher.encrypt(KEY, DATA));
        Assert.assertTrue("Incorrect decryption!", Arrays.equals(DATA, data));
    }

    @Test(expected = BadPaddingException.class)
    public void testDecryptNegative() throws Exception {
        cipher.decrypt("103".toCharArray(), cipher.encrypt(KEY, DATA));
    }
}
