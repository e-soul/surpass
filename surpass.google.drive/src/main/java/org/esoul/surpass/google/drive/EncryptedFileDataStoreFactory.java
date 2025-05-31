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
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;

import org.esoul.surpass.crypto.api.ContextAwareCryptoService;

import com.google.api.client.util.IOUtils;
import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.AbstractMemoryDataStore;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;

public class EncryptedFileDataStoreFactory extends AbstractDataStoreFactory {

    /**
     * The purpose of this class is to reach inside the data store object hierarchy and capture references to {@link EncryptedFileDataStore}s. Which are then
     * used to trigger re-saving of the credentials.
     * 
     * @author mgp
     */
    public static class DataStoreCaptor {

        Collection<EncryptedFileDataStore<?>> dataStores = new ArrayList<>();
    }

    public static class EncryptedFileDataStore<T extends Serializable> extends AbstractMemoryDataStore<T> {

        private final Path file;
        private ContextAwareCryptoService crypto;

        protected EncryptedFileDataStore(DataStoreFactory dataStoreFactory, String id, Path directory, ContextAwareCryptoService crypto) throws IOException {
            super(dataStoreFactory, id);
            file = directory.resolve(id);
            this.crypto = crypto;
            if (Files.exists(file)) {
                byte[] cipherText = Files.readAllBytes(file);
                try {
                    keyValueMap = IOUtils.deserialize(crypto.decrypt(cipherText));
                } catch (GeneralSecurityException e) {
                    throw new IOException(e);
                }
            }
        }

        public void reinitializeAndResave(ContextAwareCryptoService crypto) throws IOException {
            this.crypto = crypto;
            save();
        }

        @Override
        public void save() throws IOException {
            byte[] clearText = IOUtils.serialize(keyValueMap);
            try {
                byte[] cipherText = crypto.encrypt(clearText);
                Files.write(file, cipherText);
            } catch (GeneralSecurityException e) {
                throw new IOException(e);
            }
        }
    }

    private final Path directory;
    private final ContextAwareCryptoService crypto;
    private final DataStoreCaptor dataStoreCaptor;

    public EncryptedFileDataStoreFactory(Path directory, ContextAwareCryptoService crypto, DataStoreCaptor dataStoreCaptor) {
        this.directory = directory;
        this.crypto = crypto;
        this.dataStoreCaptor = dataStoreCaptor;
    }

    @Override
    protected <V extends Serializable> DataStore<V> createDataStore(String id) throws IOException {
        EncryptedFileDataStore<V> dataStore = new EncryptedFileDataStore<V>(this, id, directory, crypto);
        dataStoreCaptor.dataStores.add(dataStore);
        return dataStore;
    }
}
