package org.esoul.surpass.hello.api;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;

import org.esoul.surpass.persist.api.PersistenceService;

/** High-level password and optional Windows Hello vault-unlock service. */
public interface VaultUnlockService {

    HelloCapability helloCapability();

    boolean isHelloEnrolled(String persistenceServiceId);

    HelloPromptOwner capturePromptOwner();

    UnlockedVault unlockWithPassword(char[] password, PersistenceService persistenceService)
            throws IOException, GeneralSecurityException, UnlockException;

    UnlockedVault unlockWithHello(HelloPromptOwner promptOwner, PersistenceService persistenceService)
            throws IOException, GeneralSecurityException, UnlockException;

    void enrollHello(HelloPromptOwner promptOwner, UnlockedVault vault)
            throws IOException, GeneralSecurityException, UnlockException;

    void removeHello(UnlockedVault vault) throws IOException, UnlockException;

    void store(UnlockedVault vault, byte[] clearText, Collection<PersistenceService> persistenceServices)
            throws IOException, GeneralSecurityException;

    void changeMasterPassword(HelloPromptOwner promptOwner, UnlockedVault vault,
            char[] currentPassword, char[] newPassword,
            Collection<PersistenceService> persistenceServices)
            throws IOException, GeneralSecurityException, UnlockException;

    boolean verifyPassword(UnlockedVault vault, char[] password) throws GeneralSecurityException;

    void commitMigration(UnlockedVault vault, PersistenceService persistenceService)
            throws IOException, GeneralSecurityException;

    void cancelHello();
}
