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
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.esoul.surpass.persist.api.PersistenceService;

/**
 * Warning, not thread-safe.
 * 
 * @author mgp
 */
public class GooglePersistenceService implements PersistenceService {

    private final DriveFacade drive;

    public GooglePersistenceService(DriveFacade drive) {
        this.drive = drive;
    }

    public GooglePersistenceService() {
        this(new GoogleDrive());
    }

    @Override
    public void authorize(ContextAwareCryptoService crypto) {
        drive.authorize(crypto);
    }

    @Override
    public void regenerateSupprtingData(ContextAwareCryptoService crypto) {
        drive.regenerateCredentials(crypto);
    }

    @Override
    public byte[] read(String name) throws IOException {
        try {
            String directoryId = drive.searchDirectory(PersistenceDefaults.getDriveDataDir());
            if (null == directoryId) {
                return new byte[0];
            }
            String fileId = drive.searchFile(directoryId, name);
            if (null == fileId) {
                return new byte[0];
            }
            return drive.readFile(fileId);
        } catch (GeneralSecurityException e) {
            throw new IOException("Cannot read from Google Drive!", e);
        }
    }

    @Override
    public void write(String name, byte[] data) throws IOException {
        try {
            String directoryId = drive.searchDirectory(PersistenceDefaults.getDriveDataDir());
            if (null == directoryId) {
                directoryId = drive.createDirectory(PersistenceDefaults.getDriveDataDir());
            }
            String fileId = drive.searchFile(directoryId, name);
            if (null == fileId) {
                drive.createFile(directoryId, name, data);
            } else {
                drive.updateFile(fileId, data);
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Cannot write to Google Drive!", e);
        }
    }

    @Override
    public boolean exists(String name) throws IOException {
        try {
            String directoryId = drive.searchDirectory(PersistenceDefaults.getDriveDataDir());
            return null != directoryId && null != drive.searchFile(directoryId, name);
        } catch (GeneralSecurityException e) {
            throw new IOException("Cannot search Google Drive!", e);
        }
    }

    @Override
    public String getId() {
        return getClass().getName();
    }

    @Override
    public String getDisplayName() {
        return "Google Drive";
    }
}
