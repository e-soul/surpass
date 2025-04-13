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
package org.esoul.surpass.persist.api;

import java.io.IOException;

import org.esoul.surpass.crypto.api.ContextAwareCryptoService;

/**
 * Read/write data from/to some local or remote storage medium.
 * 
 * @author mgp
 */
public interface PersistenceService {

    /**
     * Some services (like Google Drive) maintain supporting data (not user data) that needs to be encrypted with a key derived from the master password
     * ({@link ContextAwareCryptoService}). This method authorizes the service to encrypt supporting data with the given {@link ContextAwareCryptoService}.
     * 
     * @param crypto
     */
    default void authorize(ContextAwareCryptoService crypto) {
    }

    /**
     * Regenerates supporting data with the given {@link ContextAwareCryptoService}. This is needed for services that require authorization (like Google Drive)
     * to support changing the master password. The credential data like OAuth2 tokens are stored on the file system in an encrypted state and the encryption
     * key is derived from the master password. When the master password is changed the stored credentials need to be re-encrypted with the new key derived from
     * the new master password.
     * 
     * @param crypto Has the new encryption key to regenerate the stored credential data.
     */
    default void regenerateSupprtingData(ContextAwareCryptoService crypto) {
    }

    /**
     * Reads data into a byte array.
     * 
     * @param name The name of the file to read from.
     * @return The data.
     * @throws IOException In case of any IO error.
     */
    byte[] read(String name) throws IOException;

    /**
     * Writes data from a byte array.
     * 
     * @param name The name of the file to write to.
     * @param data The data.
     * @throws IOException In case of any IO error.
     */
    void write(String name, byte[] data) throws IOException;

    /**
     * Checks if data file exists.
     * 
     * @param name The name of the data file.
     * @return {@code true} if data file exists and {@code false} otherwise.
     * @throws IOException In case of any IO error.
     */
    boolean exists(String name) throws IOException;

    /**
     * Returns the unique identifier of the implementation.
     * 
     * @return The unique identifier of the implementation.
     */
    String getId();

    /**
     * Returns the display name of the implementation.
     * 
     * @return The unique identifier of the implementation.
     */
    String getDisplayName();
}
