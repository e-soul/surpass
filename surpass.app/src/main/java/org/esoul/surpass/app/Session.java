/*
   Copyright 2017-2026 e-soul.org
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
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.esoul.surpass.hello.api.HelloCapability;
import org.esoul.surpass.hello.api.HelloPromptOwner;
import org.esoul.surpass.hello.api.UnlockedVault;
import org.esoul.surpass.hello.api.UnlockException;
import org.esoul.surpass.hello.api.VaultUnlockService;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.esoul.surpass.persist.api.PersistenceService;
import org.esoul.surpass.persist.api.PrimaryPersistenceService;
import org.esoul.surpass.secgen.api.CharClass;
import org.esoul.surpass.secgen.api.RandomSecretService;
import org.esoul.surpass.table.api.EmptySequenceException;
import org.esoul.surpass.table.api.MaxSizeExceededException;
import org.esoul.surpass.table.api.SecretTable;

/**
 * Facilitates the interactions between various services to provide a high-level API for building user interfaces. Logging in response to errors is also done by
 * this class, all exceptions are re-thrown. A typical usage pattern would look like this: Obtain an instance of this class. When the application is loaded,
 * call {@link #start()}. When the application is ready to process user input, call {@link #loadData(char[])}. React to user input via
 * {@link #write(char[], char[], char[])}, {@link #setEditMode(int)}, {@link #remove(int)}, etc. When the user wants to persist their changes, call
 * {@link #storeData(char[], Collection)}. Note, this class is thread-safe if the {@link SecretTable} implementation is.
 * 
 * @author mgp
 */
public class Session {

    private static final Logger logger = System.getLogger(Session.class.getSimpleName());

    private CollaboratorFactory collaboratorFactory = null;

    private PrimaryPersistenceService primaryPersistenceService = null;

    private Map<String, PersistenceService> persistenceServiceMap = null;

    private SecretTable secretTable = null;

    private VaultUnlockService vaultUnlockService = null;

    private UnlockedVault unlockedVault = null;

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
        vaultUnlockService = collaboratorFactory.obtainOne(VaultUnlockService.class);
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
        PersistenceService persistenceService = requirePersistenceService(serviceId);
        UnlockedVault candidate = null;
        try {
            candidate = vaultUnlockService.unlockWithPassword(password, persistenceService);
            vaultUnlockService.commitMigration(candidate, persistenceService);
            byte[] clearText = candidate.copyPlaintext();
            try {
                secretTable.load(clearText);
            } finally {
                Arrays.fill(clearText, (byte) 0);
            }
            replaceUnlockedVault(candidate);
            candidate = null;
            state.dataFileLoaded = true;
        } catch (IOException e) {
            logger.log(Level.ERROR, () -> "Load secrets error!", e);
            throw e;
        } catch (GeneralSecurityException e) {
            logger.log(Level.ERROR, () -> "Decrypt secrets error!", e);
            throw e;
        } catch (UnlockException e) {
            if (e.reason() == UnlockException.Reason.INVALID_PASSWORD) {
                throw new InvalidPasswordException(e);
            }
            throw new GeneralSecurityException(e.getMessage(), e);
        } finally {
            if (candidate != null) {
                candidate.close();
            }
        }
    }

    public void loadDataWithHello(HelloPromptOwner promptOwner, String serviceId)
            throws IOException, GeneralSecurityException, UnlockException {
        PersistenceService persistenceService = requirePersistenceService(serviceId);
        UnlockedVault candidate = vaultUnlockService.unlockWithHello(promptOwner, persistenceService);
        try {
            byte[] clearText = candidate.copyPlaintext();
            try {
                secretTable.load(clearText);
            } finally {
                Arrays.fill(clearText, (byte) 0);
            }
            replaceUnlockedVault(candidate);
            candidate = null;
            state.dataFileLoaded = true;
        } finally {
            if (candidate != null) {
                candidate.close();
            }
        }
    }

    public void changeMasterPassAndStoreData(char[] currentMasterPass, char[] newMasterPass, Collection<String> serviceIds)
            throws ExistingDataNotLoadedException, IOException, GeneralSecurityException, InvalidPasswordException {
        changeMasterPassAndStoreData(HelloPromptOwner.none(), currentMasterPass, newMasterPass, serviceIds);
    }

    public void changeMasterPassAndStoreData(HelloPromptOwner promptOwner, char[] currentMasterPass,
            char[] newMasterPass, Collection<String> serviceIds)
            throws ExistingDataNotLoadedException, IOException, GeneralSecurityException, InvalidPasswordException {
        checkDataLoaded();
        if (null != newMasterPass) {
            try {
                if (unlockedVault == null) {
                    throw new ExistingDataNotLoadedException();
                }
                vaultUnlockService.changeMasterPassword(promptOwner, unlockedVault,
                        currentMasterPass, newMasterPass, persistenceServices(serviceIds));
                state.unsavedDataExist = false;
            } catch (IOException e) {
                logger.log(Level.ERROR, () -> "Store secrets error!", e);
                throw e;
            } catch (GeneralSecurityException e) {
                logger.log(Level.ERROR, () -> "Encrypt secrets error!", e);
                throw e;
            } catch (UnlockException e) {
                if (e.reason() == UnlockException.Reason.INVALID_PASSWORD) {
                    throw new InvalidPasswordException(e);
                }
                throw new GeneralSecurityException(e.getMessage(), e);
            }
        }
    }

    /**
     * Stores the data to a persistent state.
     * 
     * @param password The password needed to encrypt the data.
     * @param serviceIds The IDs of the services to use to store the data. Can be obtained from {@link #getSupportedPersistenceServices()}.
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
                if (unlockedVault == null) {
                    String unlockServiceId = selectUnlockService(serviceIds);
                    unlockedVault = vaultUnlockService.unlockWithPassword(password,
                            requirePersistenceService(unlockServiceId));
                } else if (!vaultUnlockService.verifyPassword(unlockedVault, password)) {
                    throw new InvalidPasswordException("Incorrect master password");
                }
                storeWithActiveVault(serviceIds);
                state.unsavedDataExist = false;
                state.dataFileExist = true;
                state.dataFileLoaded = true;
            } catch (IOException e) {
                logger.log(Level.ERROR, () -> "Store secrets error!", e);
                throw e;
            } catch (GeneralSecurityException e) {
                logger.log(Level.ERROR, () -> "Encrypt secrets error!", e);
                throw e;
            } catch (InvalidPasswordException e) {
                logger.log(Level.ERROR, () -> "Invalid password error!", e);
                throw e;
            } catch (UnlockException e) {
                if (e.reason() == UnlockException.Reason.INVALID_PASSWORD) {
                    throw new InvalidPasswordException(e);
                }
                throw new GeneralSecurityException(e.getMessage(), e);
            }
        }
    }

    public void storeData(Collection<String> serviceIds)
            throws ExistingDataNotLoadedException, IOException, GeneralSecurityException {
        checkDataLoaded();
        if (unlockedVault == null) {
            throw new ExistingDataNotLoadedException();
        }
        storeWithActiveVault(serviceIds);
        state.unsavedDataExist = false;
        state.dataFileExist = true;
        state.dataFileLoaded = true;
    }

    private void storeWithActiveVault(Collection<String> serviceIds)
            throws IOException, GeneralSecurityException {
        byte[] clearText = secretTable.toOneDimension();
        try {
            vaultUnlockService.store(unlockedVault, clearText, persistenceServices(serviceIds));
        } finally {
            Arrays.fill(clearText, (byte) 0);
        }
    }

    private Collection<PersistenceService> persistenceServices(Collection<String> serviceIds) {
        Collection<PersistenceService> result = new ArrayList<>(serviceIds.size());
        for (String serviceId : serviceIds) {
            result.add(requirePersistenceService(serviceId));
        }
        return result;
    }

    private PersistenceService requirePersistenceService(String serviceId) {
        PersistenceService service = persistenceServiceMap.get(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Unknown persistence service: " + serviceId);
        }
        return service;
    }

    private String selectUnlockService(Collection<String> serviceIds) {
        if (serviceIds.contains(primaryPersistenceService.getId())) {
            return primaryPersistenceService.getId();
        }
        return serviceIds.stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No persistence service selected"));
    }

    private void replaceUnlockedVault(UnlockedVault replacement) {
        if (unlockedVault != null) {
            unlockedVault.close();
        }
        unlockedVault = replacement;
    }

    /**
     * Returns supported persistence services. This is intended to give the user a choice.
     * 
     * @return A {@link Map} service ID - service display name. The Service IDs can be used with {@link #storeData(char[], Collection)}.
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

    public boolean isUnlocked() {
        return unlockedVault != null;
    }

    public HelloCapability helloCapability() {
        return vaultUnlockService.helloCapability();
    }

    public boolean isHelloEnrolled(String persistenceServiceId) {
        return vaultUnlockService.isHelloEnrolled(persistenceServiceId);
    }

    public HelloPromptOwner captureHelloPromptOwner() {
        return vaultUnlockService.capturePromptOwner();
    }

    public void enrollHello(HelloPromptOwner promptOwner)
            throws ExistingDataNotLoadedException, IOException, GeneralSecurityException, UnlockException {
        if (unlockedVault == null) {
            throw new ExistingDataNotLoadedException();
        }
        vaultUnlockService.enrollHello(promptOwner, unlockedVault);
    }

    public void removeHello()
            throws ExistingDataNotLoadedException, IOException, UnlockException {
        if (unlockedVault == null) {
            throw new ExistingDataNotLoadedException();
        }
        vaultUnlockService.removeHello(unlockedVault);
    }

    public String activePersistenceServiceId() {
        return unlockedVault == null ? null : unlockedVault.persistenceServiceId();
    }

    public void cancelHello() {
        vaultUnlockService.cancelHello();
    }

    /** Erases the active unlock context and clears all decrypted table data. */
    public void lock() {
        if (unlockedVault != null) {
            unlockedVault.close();
            unlockedVault = null;
        }
        if (secretTable != null) {
            byte[] clear = secretTable.toOneDimension();
            try {
                Arrays.fill(clear, (byte) 0);
                secretTable.load(clear);
            } finally {
                Arrays.fill(clear, (byte) 0);
            }
        }
        state.dataFileLoaded = false;
        state.unsavedDataExist = false;
        state.currentlyEditedRow = -1;
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
