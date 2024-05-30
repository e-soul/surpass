/*
   Copyright 2017-2024 e-soul.org
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted
   provided that the following conditions are met:

   1. Redistributions of source code must retain the above copyright notice, this list of conditions
      and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
      and the following disclaimer in the documentation and/or other materials provided with the distribution.

   THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.esoul.surpass.core;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.esoul.surpass.crypto.api.CryptoService;

/**
 * Uses a {@link Cipher} with configurable parameters for password-based encryption (PBE).
 *
 * @author mgp
 */
public class SimpleCipher implements CryptoService {

    private static final String PBE_ALGO = System.getProperty(ConfigurationProperties.PBE_ALGO, "PBEWithHmacSHA512AndAES_128");

    private static final int ITERATION_COUNT = Integer.getInteger(ConfigurationProperties.ITERATION_COUNT, 100);

    private static final int VERSION_INDEX = 0;

    public static final int VERSION_LEN = 1;

    public static final int SALT_LEN = Integer.getInteger(ConfigurationProperties.SALT_LEN, 16);

    public static final int IV_LEN = Integer.getInteger(ConfigurationProperties.IV_LEN, 16);

    private SecureRandom secureRandom = new SecureRandom();

    @Override
    public byte[] encrypt(char[] key, byte[] data) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LEN];
        secureRandom.nextBytes(salt);

        byte[] iv = new byte[IV_LEN];
        secureRandom.nextBytes(iv);

        Cipher pbeCipher = createCipher(Cipher.ENCRYPT_MODE, key, salt, iv);
        byte[] ciphertext = pbeCipher.doFinal(data);
        return addParameters(salt, iv, ciphertext);
    }

    @Override
    public byte[] decrypt(char[] key, byte[] cipherInput) throws GeneralSecurityException {
        byte[] salt = Arrays.copyOfRange(cipherInput, VERSION_LEN, VERSION_LEN + SALT_LEN);
        byte[] iv = Arrays.copyOfRange(cipherInput, VERSION_LEN + SALT_LEN, VERSION_LEN + SALT_LEN + IV_LEN);
        byte[] cipherText = Arrays.copyOfRange(cipherInput, VERSION_LEN + SALT_LEN + IV_LEN, cipherInput.length);

        Cipher pbeCipher = createCipher(Cipher.DECRYPT_MODE, key, salt, iv);
        return pbeCipher.doFinal(cipherText);
    }

    private Cipher createCipher(int mode, char[] key, byte[] salt, byte[] iv) throws GeneralSecurityException {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(key);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGO);
        SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);

        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, ITERATION_COUNT, new IvParameterSpec(iv));

        Cipher pbeCipher = Cipher.getInstance(PBE_ALGO);
        pbeCipher.init(mode, secretKey, pbeParamSpec);
        return pbeCipher;
    }

    private byte[] addParameters(byte[] salt, byte[] iv, byte[] cipherText) {
        byte[] result = new byte[VERSION_LEN + salt.length + iv.length + cipherText.length];
        result[VERSION_INDEX] = 0;
        for (int i = VERSION_LEN, j = 0; j < salt.length; i++, j++) {
            result[i] = salt[j];
        }
        for (int i = (VERSION_LEN + salt.length), j = 0; j < iv.length; i++, j++) {
            result[i] = iv[j];
        }
        for (int i = (VERSION_LEN + salt.length + iv.length), j = 0; j < cipherText.length; i++, j++) {
            result[i] = cipherText[j];
        }
        return result;
    }
}
