package org.esoul.surpass.hello.api;

import java.util.Arrays;
import java.util.Objects;

/**
 * Opaque, closeable owner of an unlocked vault's key material. The class never
 * exposes its DEK or persistence-authorization value.
 */
public final class UnlockedVault implements AutoCloseable {

    private byte[] vaultId;
    private byte[] dek;
    private char[] persistenceAuthorization;
    private byte[] plaintext;
    private int passwordIterations;
    private byte[] passwordSalt;
    private byte[] passwordNonce;
    private byte[] wrappedDek;
    private final String persistenceServiceId;
    private byte[] legacyCiphertext;
    private boolean migrationPending;
    private boolean passwordVerified;
    private boolean closed;

    UnlockedVault(byte[] vaultId, byte[] dek, char[] persistenceAuthorization, byte[] plaintext,
            int passwordIterations, byte[] passwordSalt, byte[] passwordNonce, byte[] wrappedDek,
            String persistenceServiceId, byte[] legacyCiphertext, boolean migrationPending,
            boolean passwordVerified) {
        this.vaultId = vaultId.clone();
        this.dek = dek.clone();
        this.persistenceAuthorization = persistenceAuthorization.clone();
        this.plaintext = plaintext.clone();
        this.passwordIterations = passwordIterations;
        this.passwordSalt = passwordSalt.clone();
        this.passwordNonce = passwordNonce.clone();
        this.wrappedDek = wrappedDek.clone();
        this.persistenceServiceId = Objects.requireNonNull(persistenceServiceId);
        this.legacyCiphertext = legacyCiphertext == null ? null : legacyCiphertext.clone();
        this.migrationPending = migrationPending;
        this.passwordVerified = passwordVerified;
    }

    /** Returns a defensive copy of the decrypted vault contents. */
    public synchronized byte[] copyPlaintext() {
        checkOpen();
        return plaintext.clone();
    }

    /** Returns the persistence service from which this unlock context originated. */
    public String persistenceServiceId() {
        return persistenceServiceId;
    }

    public synchronized boolean migrationPending() {
        checkOpen();
        return migrationPending;
    }

    synchronized byte[] vaultId() {
        checkOpen();
        return vaultId;
    }

    synchronized byte[] dek() {
        checkOpen();
        return dek;
    }

    synchronized char[] persistenceAuthorization() {
        checkOpen();
        return persistenceAuthorization;
    }

    synchronized int passwordIterations() {
        checkOpen();
        return passwordIterations;
    }

    synchronized byte[] passwordSalt() {
        checkOpen();
        return passwordSalt;
    }

    synchronized byte[] passwordNonce() {
        checkOpen();
        return passwordNonce;
    }

    synchronized byte[] wrappedDek() {
        checkOpen();
        return wrappedDek;
    }

    synchronized byte[] legacyCiphertext() {
        checkOpen();
        return legacyCiphertext;
    }

    synchronized boolean passwordVerified() {
        checkOpen();
        return passwordVerified;
    }

    synchronized void replacePlaintext(byte[] value) {
        checkOpen();
        Arrays.fill(plaintext, (byte) 0);
        plaintext = value.clone();
    }

    synchronized void replacePasswordWrapper(int iterations, byte[] salt, byte[] nonce,
            byte[] wrapper, char[] authorization) {
        checkOpen();
        Arrays.fill(passwordSalt, (byte) 0);
        Arrays.fill(passwordNonce, (byte) 0);
        Arrays.fill(wrappedDek, (byte) 0);
        Arrays.fill(persistenceAuthorization, '\0');
        passwordIterations = iterations;
        passwordSalt = salt.clone();
        passwordNonce = nonce.clone();
        wrappedDek = wrapper.clone();
        persistenceAuthorization = authorization.clone();
        passwordVerified = true;
    }

    synchronized void migrationCommitted() {
        checkOpen();
        if (legacyCiphertext != null) {
            Arrays.fill(legacyCiphertext, (byte) 0);
            legacyCiphertext = null;
        }
        migrationPending = false;
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Vault is locked");
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        Arrays.fill(vaultId, (byte) 0);
        Arrays.fill(dek, (byte) 0);
        Arrays.fill(persistenceAuthorization, '\0');
        Arrays.fill(plaintext, (byte) 0);
        Arrays.fill(passwordSalt, (byte) 0);
        Arrays.fill(passwordNonce, (byte) 0);
        Arrays.fill(wrappedDek, (byte) 0);
        if (legacyCiphertext != null) {
            Arrays.fill(legacyCiphertext, (byte) 0);
        }
        closed = true;
    }

    @Override
    public String toString() {
        return "UnlockedVault[service=" + persistenceServiceId + ", closed=" + closed + "]";
    }
}
