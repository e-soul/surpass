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
package org.esoul.surpass.app;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.NoSuchFileException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.esoul.surpass.crypto.api.CryptoService;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.esoul.surpass.persist.api.PersistenceService;
import org.esoul.surpass.persist.api.PrimaryPersistenceService;
import org.esoul.surpass.secgen.api.CharClass;
import org.esoul.surpass.secgen.api.RandomSecretService;
import org.esoul.surpass.table.api.EmptySequenceException;
import org.esoul.surpass.table.api.MaxSizeExceededException;
import org.esoul.surpass.table.api.SecretTable;

/**
 * Facilitates the interactions between various services to provide a high-level API for building user interfaces.
 * Logging in response to errors is also done by this class, all exceptions are re-thrown. A typical usage pattern would
 * look like this: Obtain an instance of this class. When the application is loaded, call {@link #start()}. When the
 * application is ready to process user input, call {@link #loadData(char[])}. React to user input via
 * {@link #write(char[], char[], char[])}, {@link #setEditMode(int)}, {@link #remove(int)}, etc. When the user wants to
 * persist their changes, call {@link #storeData(char[], Collection)}. Note, this class is thread-safe if the
 * {@link SecretTable} implementation is.
 * 
 * @author mgp
 */
public class Session {

    private static final Logger logger = System.getLogger(Session.class.getSimpleName());

    private CollaboratorFactory collaboratorFactory = null;

    private PrimaryPersistenceService primaryPersistenceService = null;

    private Map<String, PersistenceService> persistenceServiceMap = null;

    private SecretTable secretTable = null;

    private CryptoService cryptoService = null;

    private RandomSecretService randomSecretService = null;

    private DataState state = new DataState();

    public Session(CollaboratorFactory collaboratorFactory) {
        this.collaboratorFactory = collaboratorFactory;
    }

    /**
     * Starts the session. Will initialize state and allocate any resources needed for managing secrets.
     * 
     * @throws ServiceUnavailableException
     * @throws IOException
     */
    public void start() throws ServiceUnavailableException, IOException {
        createCollaborators();
        initState();
    }

    private void createCollaborators() throws ServiceUnavailableException {
        cryptoService = collaboratorFactory.obtainOne(CryptoService.class);
        primaryPersistenceService = collaboratorFactory.obtainOne(PrimaryPersistenceService.class);
        persistenceServiceMap = collaboratorFactory.obtainAll(PersistenceService.class).collect(Collectors.toMap(PersistenceService::getId, s -> s));
        secretTable = collaboratorFactory.obtainOne(SecretTable.class);
        randomSecretService = collaboratorFactory.obtainOne(RandomSecretService.class);
    }

    private void initState() throws IOException {
        try {
            state.dataFileExist = primaryPersistenceService.exists(PersistenceDefaults.DEFAULT_SECRETS);
        } catch (IOException e) {
            logger.log(Level.ERROR, () -> "Check secrets file exists error!", e);
            throw e;
        }
    }

    /**
     * Loads the data from the persistent state.
     * 
     * @param password The password needed to decrypt the data.
     * @param serviceId The ID of the service to use for loading.
     * @throws IOException
     * @throws InvalidPasswordException
     * @throws GeneralSecurityException
     * @throws ServiceUnavailableException
     */
    public void loadData(char[] password, String serviceId)
            throws IOException, InvalidPasswordException, GeneralSecurityException, ServiceUnavailableException {
        if ((null == password) || (0 == password.length)) {
            throw new InvalidPasswordException("Password is null or empty!");
        }
        try {
            byte[] clearText = readCipherTextAndDecrypt(password, serviceId);
            secretTable.load(clearText);
            state.dataFileLoaded = true;
        } catch (IOException e) {
            logger.log(Level.ERROR, () -> "Load secrets error!", e);
            throw e;
        } catch (GeneralSecurityException e) {
            logger.log(Level.ERROR, () -> "Decrypt secrets error!", e);
            throw e;
        }
    }

    public void changeMasterPassAndStoreData(char[] currentMasterPass, char[] newMasterPass, Collection<String> serviceIds)
            throws ExistingDataNotLoadedException, IOException, GeneralSecurityException, InvalidPasswordException {
        checkDataLoaded();
        if (null != newMasterPass) {
            try {
                byte[] clearText = secretTable.toOneDimension();
                byte[] cipherText = cryptoService.encrypt(newMasterPass, clearText);
                for (String serviceId : serviceIds) {
                    checkCanDecryptPassword(currentMasterPass, serviceId);
                    persistenceServiceMap.get(serviceId).write(PersistenceDefaults.DEFAULT_SECRETS, cipherText);
                }
                state.unsavedDataExist = false;
            } catch (IOException e) {
                logger.log(Level.ERROR, () -> "Store secrets error!", e);
                throw e;
            } catch (GeneralSecurityException e) {
                logger.log(Level.ERROR, () -> "Encrypt secrets error!", e);
                throw e;
            }
        }
    }

    /**
     * Stores the data to a persistent state.
     * 
     * @param password The password needed to encrypt the data.
     * @param serviceIds The IDs of the services to use to store the data. Can be obtained from
     *        {@link #getSupportedPersistenceServices()}.
     * @throws ExistingDataNotLoadedException
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws InvalidPasswordException
     * @throws ServiceUnavailableException
     */
    public void storeData(char[] password, Collection<String> serviceIds)
            throws ExistingDataNotLoadedException, IOException, GeneralSecurityException, InvalidPasswordException {
        checkDataLoaded();
        if (null != password) {
            try {
                for (var serviceId : serviceIds) {
                    checkCanDecryptPassword(password, serviceId);
                }
                byte[] clearText = secretTable.toOneDimension();
                byte[] cipherText = cryptoService.encrypt(password, clearText);
                for (String serviceId : serviceIds) {
                    persistenceServiceMap.get(serviceId).write(PersistenceDefaults.DEFAULT_SECRETS, cipherText);
                }
                state.unsavedDataExist = false;
            } catch (IOException e) {
                logger.log(Level.ERROR, () -> "Store secrets error!", e);
                throw e;
            } catch (GeneralSecurityException e) {
                logger.log(Level.ERROR, () -> "Encrypt secrets error!", e);
                throw e;
            } catch (InvalidPasswordException e) {
                logger.log(Level.ERROR, () -> "Invalid password error!", e);
                throw e;
            }
        }
    }

    private void checkCanDecryptPassword(char[] password, String serviceId) throws IOException, InvalidPasswordException {
        try {
            readCipherTextAndDecrypt(password, serviceId);
        } catch (GeneralSecurityException e) {
            throw new InvalidPasswordException(e);
        } catch (NoSuchFileException e) {
            logger.log(Level.TRACE, () -> "Checking password on a nonexistent data file.", e);
        }
    }

    private byte[] readCipherTextAndDecrypt(char[] password, String serviceId) throws IOException, GeneralSecurityException {
        byte[] cipherText = persistenceServiceMap.get(serviceId).read(PersistenceDefaults.DEFAULT_SECRETS);
        if (cipherText.length == 0) {
            return cipherText;
        }
        return cryptoService.decrypt(password, cipherText);
    }

    /**
     * Returns supported persistence services. This is intended to give the user a choice.
     * 
     * @return A {@link Map} service ID - service display name. The Service IDs can be used with
     *         {@link #storeData(char[], Collection)}.
     */
    public Map<String, String> getSupportedPersistenceServices() {
        return persistenceServiceMap.values().stream().collect(Collectors.toMap(PersistenceService::getId, PersistenceService::getDisplayName));
    }

    /**
     * Adds a new row or updates an existing row if in edit mode.
     * 
     * @param password
     * @param identifier
     * @param note
     * @throws ExistingDataNotLoadedException
     * @throws MaxSizeExceededException
     * @throws EmptySequenceException
     */
    public void write(char[] password, char[] identifier, char[] note) throws ExistingDataNotLoadedException, MaxSizeExceededException, EmptySequenceException {
        checkDataLoaded();
        if (0 <= state.currentlyEditedRow) {
            secretTable.updateRow(state.currentlyEditedRow, 0 != password.length ? password : null, identifier, note);
            state.currentlyEditedRow = -1;
        } else {
            secretTable.createRow(password, identifier, note);
        }
        state.unsavedDataExist = true;
    }

    public void checkDataLoaded() throws ExistingDataNotLoadedException {
        if (state.dataFileExist && !state.dataFileLoaded) {
            throw new ExistingDataNotLoadedException();
        }
    }

    /**
     * Removes a given row.
     * 
     * @param row The index of the row to remove.
     */
    public void remove(int row) {
        secretTable.removeRow(row);
        state.unsavedDataExist = true;
    }

    /**
     * Returns the underlying {@link SecretTable} instance.
     * 
     * @return
     */
    public SecretTable getSecretTable() {
        return secretTable;
    }

    /**
     * Sets edit mode for a given row.
     * 
     * @param row The index of the row to edit.
     */
    public void setEditMode(int row) {
        state.currentlyEditedRow = row;
    }

    /**
     * {@code true} if the data file exists and {@code false} otherwise.
     * 
     * @return
     */
    public boolean dataFileExist() {
        return state.dataFileExist;
    }

    /**
     * {@code true} if unsaved data exists and {@code false} otherwise.
     * 
     * @return
     */
    public boolean unsavedDataExists() {
        return state.unsavedDataExist;
    }

    /**
     * Generates a random secret based on the allowed character classes.
     * 
     * @param secret The input array to write the generated secret into.
     * @param allowedCharClasses The character classes allowed in the generated secret.
     */
    public void generateSecret(char[] secret, Collection<CharClass> allowedCharClasses) {
        randomSecretService.generateSecret(secret, allowedCharClasses);
    }

    public SecretQuery createQuery() {
        return new SecretQuery(secretTable);
    }
}
