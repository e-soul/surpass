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
package org.esoul.surpass.google.drive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Collections;

import org.esoul.surpass.persist.api.PersistenceDefaults;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

class GoogleDrive implements DriveFacade {

    @FunctionalInterface
    interface DriveOperation<T> {
        T execute() throws IOException, GeneralSecurityException;
    }

    private static final Logger logger = System.getLogger(GoogleDrive.class.getSimpleName());

    private Drive service = null;

    private Drive getService() throws IOException, GeneralSecurityException {
        if (null == service) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(),
                    new InputStreamReader(GooglePersistenceService.class.getResourceAsStream("/surpass-oauth2.json"), StandardCharsets.UTF_8));
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, GsonFactory.getDefaultInstance(), clientSecrets,
                    java.util.List.of(DriveScopes.DRIVE_FILE)).setAccessType("offline")
                    .setDataStoreFactory(new FileDataStoreFactory(PersistenceDefaults.getDataDir().toFile())).build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(41080).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            service = new Drive.Builder(httpTransport, GsonFactory.getDefaultInstance(), credential).setApplicationName("Surpass").build();
        }
        return service;
    }

    private <T> T executeOperation(DriveOperation<T> s) throws IOException, GeneralSecurityException {
        try {
            return s.execute();
        } catch (TokenResponseException e) {
            logger.log(Level.INFO, () -> "Refresh token invalid. Starting new authorization.");
            logger.log(Level.DEBUG, () -> "Drive operation failed.", e);
            // Setting the service field to null, deleting the tokens and re-executing the operation will trigger a new authz.
            service = null;
            Files.delete(PersistenceDefaults.getGoogleStoredCredential());
            return s.execute();
        }
    }

    @Override
    public String searchDirectory(String name) throws IOException, GeneralSecurityException {
        String query = String.format("mimeType='application/vnd.google-apps.folder' and name='%s' and trashed=false and 'root' in parents", name);
        return search(query, name);
    }

    @Override
    public String searchFile(String parentDirectoryId, String name) throws IOException, GeneralSecurityException {
        String query = String.format("mimeType!='application/vnd.google-apps.folder' and name='%s' and trashed=false and '%s' in parents", name,
                parentDirectoryId);
        return search(query, name);
    }

    private String search(String query, String name) throws IOException, GeneralSecurityException {
        FileList list = executeOperation(() -> getService().files().list().setQ(query).setFields("files(id)").execute());
        Collection<File> result = list.getFiles();
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() > 1) {
            throw new IOException(String.format("Multiple entities found with the name %s! Remove all duplicates from Google Drive and try again.", name));
        }
        return result.iterator().next().getId();
    }

    @Override
    public String createDirectory(String name) throws IOException, GeneralSecurityException {
        File directoryMetadata = new File();
        directoryMetadata.setName(name);
        directoryMetadata.setMimeType("application/vnd.google-apps.folder");
        File directory = executeOperation(() -> getService().files().create(directoryMetadata).setFields("id").execute());
        return directory.getId();
    }

    @Override
    public void createFile(String parentDirectoryId, String name, byte[] data) throws IOException, GeneralSecurityException {
        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setParents(Collections.singletonList(parentDirectoryId));
        ByteArrayContent bytes = new ByteArrayContent("application/octet-stream", data);
        executeOperation(() -> getService().files().create(fileMetadata, bytes).execute());
    }

    @Override
    public void updateFile(String fileId, byte[] data) throws IOException, GeneralSecurityException {
        ByteArrayContent bytes = new ByteArrayContent("application/octet-stream", data);
        executeOperation(() -> getService().files().update(fileId, new File(), bytes).execute());
    }

    @Override
    public byte[] readFile(String fileId) throws IOException, GeneralSecurityException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        executeOperation(() -> {
            getService().files().get(fileId).executeMediaAndDownloadTo(baos);
            return null;
        });
        return baos.toByteArray();
    }
}
