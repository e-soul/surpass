package org.esoul.surpass.crypto.api;

import java.security.GeneralSecurityException;

public interface ContextAwareCrypto {
    /**
     * Encrypts data.
     *
     * @param data The data for encryption.
     * @return The cipher text + salt, iv and format version.
     * @throws GeneralSecurityException
     */
    byte[] encrypt(byte[] data) throws GeneralSecurityException;

    /**
     * Decrypts data.
     *
     * @param cipherInput The cipher text + salt, iv and format version.
     * @return The decrypted data.
     * @throws GeneralSecurityException
     */
    byte[] decrypt(byte[] cipherInput) throws GeneralSecurityException;
}
