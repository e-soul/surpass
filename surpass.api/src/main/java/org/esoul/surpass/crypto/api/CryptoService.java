/*
   Copyright 2017-2025 e-soul.org
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
package org.esoul.surpass.crypto.api;

import java.security.GeneralSecurityException;

/**
 * Encrypt, decrypt or digest arbitrary data.
 * 
 * @author mgp
 */
public interface CryptoService {

    /**
     * Encrypts data.
     *
     * @param key The key used for encryption.
     * @param data The data for encryption.
     * @return The cipher text + salt, iv and format version.
     * @throws GeneralSecurityException
     */
    byte[] encrypt(char[] key, byte[] data) throws GeneralSecurityException;

    /**
     * Decrypts data.
     *
     * @param key The key used for decryption.
     * @param cipherInput The cipher text + salt, iv and format version.
     * @return The decrypted data.
     * @throws GeneralSecurityException
     */
    byte[] decrypt(char[] key, byte[] cipherInput) throws GeneralSecurityException;

    /**
     * Calculate the SHA-512 digest (a.k.a. the SHA-512 hash) of the given input.
     * 
     * @param input The input to digest.
     * @return The ASCII hex representation of the digest.
     * @throws GeneralSecurityException
     */
    char[] digest(char[] input) throws GeneralSecurityException;
}
