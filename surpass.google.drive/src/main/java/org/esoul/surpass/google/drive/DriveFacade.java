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
package org.esoul.surpass.google.drive;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.esoul.surpass.crypto.api.ContextAwareCryptoService;

/**
 * A simplified representation of the Google Drive API.
 * 
 * @author mgp
 */
public interface DriveFacade {

    void authorize(ContextAwareCryptoService crypto);

    void regenerateCredentials(ContextAwareCryptoService crypto);

    /**
     * Searches for directory by name. If multiple directories with the same name exist, the first one is returned in the
     * order they were returned from Drive.
     * 
     * @param name The name of the directory
     * @return The directory ID if the directory exists or {@code null} otherwise.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    String searchDirectory(String name) throws IOException, GeneralSecurityException;

    /**
     * Creates a new directory in the root of Drive.
     * 
     * @param name The name of the new directory.
     * @return The directory ID if the newly created directory.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    String createDirectory(String name) throws IOException, GeneralSecurityException;

    /**
     * Searches for file by parent directory ID and name. If multiple files with the same name exist, the first one is
     * returned in the order they were returned from Drive.
     * 
     * @param parentDirectoryId Parent directory ID.
     * @param name The name of the file.
     * @return The file ID if the file exists or {@code null} otherwise.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    String searchFile(String parentDirectoryId, String name) throws IOException, GeneralSecurityException;

    /**
     * Creates a new file in a given directory.
     * 
     * @param parentDirectoryId The directory ID of the parent.
     * @param name The name of the file.
     * @param data The content of the file.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    void createFile(String parentDirectoryId, String name, byte[] data) throws IOException, GeneralSecurityException;

    /**
     * Updates an existing file.
     * 
     * @param fileId The ID of the file to update.
     * @param data The new content of the file.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    void updateFile(String fileId, byte[] data) throws IOException, GeneralSecurityException;

    /**
     * Returns the content of a file.
     * 
     * @param fileId The ID of the file.
     * @return The content of the file in bytes.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    byte[] readFile(String fileId) throws IOException, GeneralSecurityException;
}
