package org.esoul.surpass.hello.api;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

import javax.crypto.AEADBadTagException;

import org.esoul.surpass.crypto.api.ContextAwareCryptoService;
import org.esoul.surpass.crypto.api.ContextAwareCryptoServiceAbstractFactory;
import org.esoul.surpass.crypto.api.CryptoService;
import org.esoul.surpass.hello.internal.BindingCodec;
import org.esoul.surpass.hello.internal.BindingStore;
import org.esoul.surpass.hello.internal.CryptoSupport;
import org.esoul.surpass.hello.internal.HelloBinding;
import org.esoul.surpass.hello.internal.UnlockBundleCodec;
import org.esoul.surpass.hello.internal.VaultEnvelope;
import org.esoul.surpass.hello.internal.HelloKeyClient;
import org.esoul.surpass.hello.internal.windows.keycredential.WindowsKeyCredentialClient;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.esoul.surpass.persist.api.PersistenceService;

/** Default service provider for portable password and local Windows Hello unlock. */
public final class DefaultVaultUnlockService implements VaultUnlockService {

    private static final String PASSWORD_AAD = "surpass/vault/v1/password-wrapper";
    private static final String CONTENT_AAD = "surpass/vault/v1/content";
    private static final byte[] HELLO_KEK_INFO = "surpass/hello/v2/kek"
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static final String LEGACY_BACKUP = PersistenceDefaults.DEFAULT_SECRETS
            + ".version0.backup";

    private final CryptoService legacyCrypto;
    private final ContextAwareCryptoServiceAbstractFactory contextFactory;
    private final HelloKeyClient helloKey;
    private final BindingStore bindingStore;
    private final SecureRandom random;

    public DefaultVaultUnlockService() {
        this(loadOne(CryptoService.class), loadOne(ContextAwareCryptoServiceAbstractFactory.class),
                WindowsKeyCredentialClient.create(), new BindingStore(), new SecureRandom());
    }

    /** Constructor useful to embedders that supply the existing crypto collaborators explicitly. */
    public DefaultVaultUnlockService(CryptoService legacyCrypto,
            ContextAwareCryptoServiceAbstractFactory contextFactory) {
        this(legacyCrypto, contextFactory, WindowsKeyCredentialClient.create(), new BindingStore(),
                new SecureRandom());
    }

    public DefaultVaultUnlockService(CryptoService legacyCrypto,
            ContextAwareCryptoServiceAbstractFactory contextFactory, HelloKeyClient helloKey,
            BindingStore bindingStore, SecureRandom random) {
        this.legacyCrypto = Objects.requireNonNull(legacyCrypto);
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.helloKey = Objects.requireNonNull(helloKey);
        this.bindingStore = Objects.requireNonNull(bindingStore);
        this.random = Objects.requireNonNull(random);
    }

    private static <T> T loadOne(Class<T> type) {
        return ServiceLoader.load(type).findFirst()
                .orElseThrow(() -> new IllegalStateException("No service provider for " + type.getName()));
    }

    @Override
    public HelloCapability helloCapability() {
        return helloKey.capability();
    }

    @Override
    public boolean isHelloEnrolled(String persistenceServiceId) {
        if (!bindingStore.exists(persistenceServiceId)) {
            return false;
        }
        try {
            return bindingStore.read(persistenceServiceId).persistenceServiceId()
                    .equals(persistenceServiceId);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public HelloPromptOwner capturePromptOwner() {
        return helloKey.capturePromptOwner();
    }

    @Override
    public UnlockedVault unlockWithPassword(char[] password, PersistenceService persistenceService)
            throws IOException, GeneralSecurityException, UnlockException {
        requirePassword(password);
        Objects.requireNonNull(persistenceService);
        char[] authorization = legacyCrypto.digest(password);
        ContextAwareCryptoService context = contextFactory.create(legacyCrypto, authorization);
        persistenceService.authorize(context);

        boolean exists = persistenceService.exists(PersistenceDefaults.DEFAULT_SECRETS);
        if (!exists) {
            try {
                return newVault(password, authorization, persistenceService.getId(), new byte[0]);
            } finally {
                Arrays.fill(authorization, '\0');
            }
        }

        byte[] input;
        try {
            input = persistenceService.read(PersistenceDefaults.DEFAULT_SECRETS);
        } catch (NoSuchFileException e) {
            try {
                return newVault(password, authorization, persistenceService.getId(), new byte[0]);
            } finally {
                Arrays.fill(authorization, '\0');
            }
        }
        try {
            if (VaultEnvelope.hasMagic(input)) {
                return unlockEnvelope(password, authorization, persistenceService.getId(), input);
            }
            return unlockLegacy(password, authorization, persistenceService.getId(), input);
        } finally {
            Arrays.fill(authorization, '\0');
        }
    }

    private UnlockedVault unlockEnvelope(char[] password, char[] authorization, String serviceId,
            byte[] input) throws GeneralSecurityException, UnlockException {
        VaultEnvelope envelope;
        try {
            envelope = VaultEnvelope.decode(input);
        } catch (IOException e) {
            throw new UnlockException(UnlockException.Reason.CORRUPT_VAULT,
                    "The vault envelope is invalid", e);
        }
        byte[] passwordKey = CryptoSupport.derivePasswordKey(password, envelope.passwordSalt(),
                envelope.iterations());
        byte[] dek;
        try {
            try {
                dek = CryptoSupport.decryptAesGcm(passwordKey, envelope.passwordNonce(),
                        envelope.wrappedDek(),
                        CryptoSupport.aad(PASSWORD_AAD, VaultEnvelope.VERSION, envelope.vaultId()));
            } catch (AEADBadTagException e) {
                throw new UnlockException(UnlockException.Reason.INVALID_PASSWORD,
                        "Incorrect master password", e);
            }
        } finally {
            Arrays.fill(passwordKey, (byte) 0);
        }
        if (dek.length != CryptoSupport.AES_KEY_BYTES) {
            Arrays.fill(dek, (byte) 0);
            throw new UnlockException(UnlockException.Reason.CORRUPT_VAULT,
                    "The password wrapper contains an invalid key");
        }
        byte[] plaintext;
        try {
            plaintext = CryptoSupport.decryptAesGcm(dek, envelope.contentNonce(),
                    envelope.encryptedContent(),
                    CryptoSupport.aad(CONTENT_AAD, VaultEnvelope.VERSION, envelope.vaultId()));
        } catch (GeneralSecurityException e) {
            Arrays.fill(dek, (byte) 0);
            throw new UnlockException(UnlockException.Reason.CORRUPT_VAULT,
                    "Vault content authentication failed", e);
        }
        try {
            return new UnlockedVault(envelope.vaultId(), dek, authorization, plaintext,
                    envelope.iterations(), envelope.passwordSalt(), envelope.passwordNonce(),
                    envelope.wrappedDek(), serviceId, null, false, true);
        } finally {
            Arrays.fill(dek, (byte) 0);
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    private UnlockedVault unlockLegacy(char[] password, char[] authorization, String serviceId,
            byte[] input) throws GeneralSecurityException, UnlockException {
        byte[] plaintext;
        try {
            plaintext = legacyCrypto.decrypt(password, input);
        } catch (GeneralSecurityException | RuntimeException e) {
            throw new UnlockException(UnlockException.Reason.INVALID_PASSWORD,
                    "Incorrect master password or unreadable legacy vault", e);
        }
        try {
            UnlockedVault vault = newVault(password, authorization, serviceId, plaintext);
            try {
                return new UnlockedVault(vault.vaultId(), vault.dek(), authorization, plaintext,
                        vault.passwordIterations(), vault.passwordSalt(), vault.passwordNonce(),
                        vault.wrappedDek(), serviceId, input, true, true);
            } finally {
                vault.close();
            }
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    private UnlockedVault newVault(char[] password, char[] authorization, String serviceId,
            byte[] plaintext) throws GeneralSecurityException {
        byte[] vaultId = CryptoSupport.randomBytes(random, 16);
        byte[] dek = CryptoSupport.randomBytes(random, CryptoSupport.AES_KEY_BYTES);
        PasswordWrapper wrapper = createPasswordWrapper(password, vaultId, dek);
        try {
            return new UnlockedVault(vaultId, dek, authorization, plaintext, wrapper.iterations,
                    wrapper.salt, wrapper.nonce, wrapper.ciphertext, serviceId, null, false, true);
        } finally {
            Arrays.fill(vaultId, (byte) 0);
            Arrays.fill(dek, (byte) 0);
            wrapper.close();
        }
    }

    @Override
    public UnlockedVault unlockWithHello(HelloPromptOwner promptOwner,
            PersistenceService persistenceService)
            throws IOException, GeneralSecurityException, UnlockException {
        HelloBinding binding;
        try {
            binding = bindingStore.read(persistenceService.getId());
        } catch (NoSuchFileException e) {
            throw new UnlockException(UnlockException.Reason.NOT_ENROLLED,
                    "Windows Hello is not enrolled for this persistence service", e);
        } catch (IOException e) {
            throw new UnlockException(UnlockException.Reason.BINDING_MISMATCH,
                    "The local Windows Hello binding is invalid", e);
        }
        requireHelloAvailable();
        byte[] keyMaterial = helloKey.deriveKeyMaterial(promptOwner, binding.keyName(),
                binding.challenge());
        byte[] kek = null;
        byte[] clearBundle = null;
        try {
            if (keyMaterial.length != 32) {
                throw new UnlockException(UnlockException.Reason.NATIVE_FAILURE,
                        "Windows returned invalid Hello key material");
            }
            kek = CryptoSupport.hkdfSha256(keyMaterial, HELLO_KEK_INFO,
                    CryptoSupport.AES_KEY_BYTES);
            try {
                clearBundle = CryptoSupport.decryptAesGcm(kek, binding.nonce(),
                        binding.wrappedBundle(), BindingCodec.aad(binding));
            } catch (AEADBadTagException e) {
                throw new UnlockException(UnlockException.Reason.BINDING_MISMATCH,
                        "Windows Hello binding authentication failed", e);
            }
            try (UnlockBundleCodec.Bundle bundle = UnlockBundleCodec.decode(clearBundle)) {
                if (!MessageDigest.isEqual(binding.vaultId(), bundle.vaultId())
                        || !binding.persistenceServiceId().equals(bundle.persistenceServiceId())) {
                    throw new UnlockException(UnlockException.Reason.BINDING_MISMATCH,
                            "Windows Hello binding belongs to a different vault or service");
                }
                ContextAwareCryptoService context = contextFactory.create(legacyCrypto,
                        bundle.persistenceAuthorization());
                persistenceService.authorize(context);
                byte[] encoded = persistenceService.read(PersistenceDefaults.DEFAULT_SECRETS);
                if (!VaultEnvelope.hasMagic(encoded)) {
                    throw new UnlockException(UnlockException.Reason.CORRUPT_VAULT,
                            "Windows Hello requires a version-1 vault envelope");
                }
                VaultEnvelope envelope;
                try {
                    envelope = VaultEnvelope.decode(encoded);
                } catch (IOException e) {
                    throw new UnlockException(UnlockException.Reason.CORRUPT_VAULT,
                            "The vault envelope is invalid", e);
                }
                if (!MessageDigest.isEqual(envelope.vaultId(), binding.vaultId())) {
                    throw new UnlockException(UnlockException.Reason.BINDING_MISMATCH,
                            "The local binding and portable vault do not match");
                }
                byte[] plaintext;
                try {
                    plaintext = CryptoSupport.decryptAesGcm(bundle.dek(), envelope.contentNonce(),
                            envelope.encryptedContent(), CryptoSupport.aad(CONTENT_AAD,
                                    VaultEnvelope.VERSION, envelope.vaultId()));
                } catch (GeneralSecurityException e) {
                    throw new UnlockException(UnlockException.Reason.CORRUPT_VAULT,
                            "Vault content authentication failed", e);
                }
                try {
                    return new UnlockedVault(envelope.vaultId(), bundle.dek(),
                            bundle.persistenceAuthorization(), plaintext, envelope.iterations(),
                            envelope.passwordSalt(), envelope.passwordNonce(), envelope.wrappedDek(),
                            persistenceService.getId(), null, false, false);
                } finally {
                    Arrays.fill(plaintext, (byte) 0);
                }
            } catch (IOException e) {
                throw new UnlockException(UnlockException.Reason.BINDING_MISMATCH,
                        "The local Windows Hello bundle is invalid", e);
            }
        } finally {
            Arrays.fill(keyMaterial, (byte) 0);
            if (kek != null) {
                Arrays.fill(kek, (byte) 0);
            }
            if (clearBundle != null) {
                Arrays.fill(clearBundle, (byte) 0);
            }
        }
    }

    @Override
    public void enrollHello(HelloPromptOwner promptOwner, UnlockedVault vault)
            throws IOException, GeneralSecurityException, UnlockException {
        if (!vault.passwordVerified()) {
            throw new UnlockException(UnlockException.Reason.INVALID_PASSWORD,
                    "Enrollment requires a master-password unlock");
        }
        if (vault.migrationPending()) {
            throw new UnlockException(UnlockException.Reason.CORRUPT_VAULT,
                    "Complete vault migration before enrolling Windows Hello");
        }
        requireHelloAvailable();
        HelloBinding previousBinding = null;
        if (bindingStore.exists(vault.persistenceServiceId())) {
            try {
                previousBinding = bindingStore.read(vault.persistenceServiceId());
            } catch (IOException ignored) {
                // A successful enrollment atomically replaces an unreadable local binding.
            }
        }
        HelloKeyClient.Key key = helloKey.createKey(promptOwner, vault.vaultId());
        boolean committed = false;
        try {
            byte[] challenge = CryptoSupport.randomBytes(random, 32);
            byte[] keyMaterial = helloKey.deriveKeyMaterial(promptOwner, key.name(), challenge);
            try {
                writeBinding(vault, key.name(), challenge, keyMaterial, 0L);
                committed = true;
                if (previousBinding != null
                        && !previousBinding.keyName().equals(key.name())) {
                    try {
                        helloKey.deleteKey(previousBinding.keyName());
                    } catch (UnlockException ignored) {
                        // The new binding is valid; an orphaned old key is harmless.
                    }
                }
            } finally {
                Arrays.fill(challenge, (byte) 0);
                Arrays.fill(keyMaterial, (byte) 0);
            }
        } finally {
            if (!committed) {
                try {
                    helloKey.deleteKey(key.name());
                } catch (UnlockException ignored) {
                    // Enrollment failure is primary; best-effort cleanup cannot replace it.
                }
            }
        }
    }

    private void writeBinding(UnlockedVault vault, String keyName, byte[] challenge,
            byte[] keyMaterial, long createdAt)
            throws GeneralSecurityException, IOException, UnlockException {
        if (keyMaterial.length != 32) {
            throw new UnlockException(UnlockException.Reason.NATIVE_FAILURE,
                    "Windows returned invalid Hello key material");
        }
        byte[] kek = CryptoSupport.hkdfSha256(keyMaterial, HELLO_KEK_INFO,
                CryptoSupport.AES_KEY_BYTES);
        byte[] nonce = CryptoSupport.randomBytes(random, CryptoSupport.GCM_NONCE_BYTES);
        byte[] bundle = UnlockBundleCodec.encode(vault.vaultId(), vault.persistenceServiceId(),
                vault.dek(), vault.persistenceAuthorization());
        long now = System.currentTimeMillis();
        long created = createdAt == 0L ? now : createdAt;
        HelloBinding header = new HelloBinding(vault.persistenceServiceId(), vault.vaultId(),
                keyName, challenge, nonce, new byte[0], created, now);
        byte[] wrapped = null;
        try {
            wrapped = CryptoSupport.encryptAesGcm(kek, nonce, bundle, BindingCodec.aad(header));
            bindingStore.write(new HelloBinding(vault.persistenceServiceId(), vault.vaultId(),
                    keyName, challenge, nonce, wrapped, created, now));
        } finally {
            Arrays.fill(kek, (byte) 0);
            Arrays.fill(bundle, (byte) 0);
            Arrays.fill(nonce, (byte) 0);
            if (wrapped != null) {
                Arrays.fill(wrapped, (byte) 0);
            }
        }
    }

    @Override
    public void removeHello(UnlockedVault vault) throws IOException, UnlockException {
        HelloBinding binding;
        try {
            binding = bindingStore.read(vault.persistenceServiceId());
        } catch (NoSuchFileException e) {
            throw new UnlockException(UnlockException.Reason.NOT_ENROLLED,
                    "Windows Hello is not enrolled", e);
        }
        try {
            helloKey.deleteKey(binding.keyName());
        } catch (UnlockException e) {
            if (e.reason() != UnlockException.Reason.CREDENTIAL_MISSING) {
                throw e;
            }
        }
        bindingStore.delete(vault.persistenceServiceId());
    }

    @Override
    public void store(UnlockedVault vault, byte[] clearText,
            Collection<PersistenceService> persistenceServices)
            throws IOException, GeneralSecurityException {
        byte[] encoded = encode(vault, clearText);
        try {
            for (PersistenceService persistence : persistenceServices) {
                persistence.authorize(contextFactory.create(legacyCrypto,
                        vault.persistenceAuthorization()));
                persistence.write(PersistenceDefaults.DEFAULT_SECRETS, encoded);
            }
            vault.replacePlaintext(clearText);
        } finally {
            Arrays.fill(encoded, (byte) 0);
        }
    }

    private byte[] encode(UnlockedVault vault, byte[] clearText)
            throws GeneralSecurityException, IOException {
        byte[] nonce = CryptoSupport.randomBytes(random, CryptoSupport.GCM_NONCE_BYTES);
        byte[] content = CryptoSupport.encryptAesGcm(vault.dek(), nonce, clearText,
                CryptoSupport.aad(CONTENT_AAD, VaultEnvelope.VERSION, vault.vaultId()));
        try {
            return new VaultEnvelope(vault.vaultId(), vault.passwordIterations(),
                    vault.passwordSalt(), vault.passwordNonce(), vault.wrappedDek(), nonce, content)
                    .encode();
        } finally {
            Arrays.fill(nonce, (byte) 0);
            Arrays.fill(content, (byte) 0);
        }
    }

    @Override
    public void changeMasterPassword(HelloPromptOwner promptOwner, UnlockedVault vault,
            char[] currentPassword, char[] newPassword,
            Collection<PersistenceService> persistenceServices)
            throws IOException, GeneralSecurityException, UnlockException {
        requirePassword(newPassword);
        if (!verifyPassword(vault, currentPassword)) {
            throw new UnlockException(UnlockException.Reason.INVALID_PASSWORD,
                    "Incorrect current master password");
        }
        char[] newAuthorization = legacyCrypto.digest(newPassword);
        PasswordWrapper wrapper = null;
        byte[] nonce = null;
        byte[] plaintext = null;
        byte[] content = null;
        byte[] encoded = null;
        try {
            wrapper = createPasswordWrapper(newPassword, vault.vaultId(), vault.dek());
            nonce = CryptoSupport.randomBytes(random, CryptoSupport.GCM_NONCE_BYTES);
            plaintext = vault.copyPlaintext();
            content = CryptoSupport.encryptAesGcm(vault.dek(), nonce, plaintext,
                    CryptoSupport.aad(CONTENT_AAD, VaultEnvelope.VERSION, vault.vaultId()));
            encoded = new VaultEnvelope(vault.vaultId(), wrapper.iterations, wrapper.salt,
                    wrapper.nonce, wrapper.ciphertext, nonce, content).encode();
            ContextAwareCryptoService currentContext = contextFactory.create(legacyCrypto,
                    vault.persistenceAuthorization());
            ContextAwareCryptoService newContext = contextFactory.create(legacyCrypto,
                    newAuthorization);
            for (PersistenceService persistence : persistenceServices) {
                persistence.authorize(currentContext);
                persistence.write(PersistenceDefaults.DEFAULT_SECRETS, encoded);
                persistence.regenerateSupprtingData(newContext);
            }
            vault.replacePasswordWrapper(wrapper.iterations, wrapper.salt, wrapper.nonce,
                    wrapper.ciphertext, newAuthorization);
        } finally {
            Arrays.fill(newAuthorization, '\0');
            if (wrapper != null) {
                wrapper.close();
            }
            if (nonce != null) {
                Arrays.fill(nonce, (byte) 0);
            }
            if (plaintext != null) {
                Arrays.fill(plaintext, (byte) 0);
            }
            if (content != null) {
                Arrays.fill(content, (byte) 0);
            }
            if (encoded != null) {
                Arrays.fill(encoded, (byte) 0);
            }
        }

        if (bindingStore.exists(vault.persistenceServiceId())) {
            HelloBinding binding = null;
            try {
                binding = bindingStore.read(vault.persistenceServiceId());
                byte[] keyMaterial = helloKey.deriveKeyMaterial(promptOwner, binding.keyName(),
                        binding.challenge());
                try {
                    writeBinding(vault, binding.keyName(), binding.challenge(), keyMaterial,
                            binding.createdAt());
                } finally {
                    Arrays.fill(keyMaterial, (byte) 0);
                }
            } catch (IOException | GeneralSecurityException | UnlockException e) {
                if (binding != null) {
                    try {
                        helloKey.deleteKey(binding.keyName());
                    } catch (UnlockException ignored) {
                        // The binding is being disabled; key cleanup is best effort.
                    }
                }
                bindingStore.delete(vault.persistenceServiceId());
            }
        }
    }

    @Override
    public boolean verifyPassword(UnlockedVault vault, char[] password)
            throws GeneralSecurityException {
        if (password == null || password.length == 0) {
            return false;
        }
        byte[] passwordKey = CryptoSupport.derivePasswordKey(password, vault.passwordSalt(),
                vault.passwordIterations());
        byte[] candidate = null;
        try {
            candidate = CryptoSupport.decryptAesGcm(passwordKey, vault.passwordNonce(),
                    vault.wrappedDek(),
                    CryptoSupport.aad(PASSWORD_AAD, VaultEnvelope.VERSION, vault.vaultId()));
            return MessageDigest.isEqual(candidate, vault.dek());
        } catch (AEADBadTagException e) {
            return false;
        } finally {
            Arrays.fill(passwordKey, (byte) 0);
            if (candidate != null) {
                Arrays.fill(candidate, (byte) 0);
            }
        }
    }

    @Override
    public void commitMigration(UnlockedVault vault, PersistenceService persistenceService)
            throws IOException, GeneralSecurityException {
        if (!vault.migrationPending()) {
            return;
        }
        byte[] legacy = vault.legacyCiphertext().clone();
        byte[] plaintext = vault.copyPlaintext();
        byte[] encoded = null;
        try {
            encoded = encode(vault, plaintext);
            try {
                persistenceService.write(LEGACY_BACKUP, legacy);
                persistenceService.write(PersistenceDefaults.DEFAULT_SECRETS, encoded);
                byte[] readBack = persistenceService.read(PersistenceDefaults.DEFAULT_SECRETS);
                VaultEnvelope decoded = VaultEnvelope.decode(readBack);
                if (!MessageDigest.isEqual(decoded.vaultId(), vault.vaultId())) {
                    throw new IOException("Migrated vault verification failed");
                }
                byte[] clear = CryptoSupport.decryptAesGcm(vault.dek(), decoded.contentNonce(),
                        decoded.encryptedContent(), CryptoSupport.aad(CONTENT_AAD,
                                VaultEnvelope.VERSION, decoded.vaultId()));
                byte[] expected = vault.copyPlaintext();
                try {
                    if (!MessageDigest.isEqual(clear, expected)) {
                        throw new IOException("Migrated vault content verification failed");
                    }
                } finally {
                    Arrays.fill(clear, (byte) 0);
                    Arrays.fill(expected, (byte) 0);
                }
                vault.migrationCommitted();
            } catch (IOException | GeneralSecurityException | RuntimeException migrationFailure) {
                try {
                    persistenceService.write(PersistenceDefaults.DEFAULT_SECRETS, legacy);
                } catch (IOException rollbackFailure) {
                    migrationFailure.addSuppressed(rollbackFailure);
                }
                throw migrationFailure;
            }
        } finally {
            Arrays.fill(legacy, (byte) 0);
            Arrays.fill(plaintext, (byte) 0);
            if (encoded != null) {
                Arrays.fill(encoded, (byte) 0);
            }
        }
    }

    @Override
    public void cancelHello() {
        helloKey.cancel();
    }

    private PasswordWrapper createPasswordWrapper(char[] password, byte[] vaultId, byte[] dek)
            throws GeneralSecurityException {
        int iterations = CryptoSupport.calibratedIterations(password, random);
        byte[] salt = CryptoSupport.randomBytes(random, CryptoSupport.PASSWORD_SALT_BYTES);
        byte[] nonce = CryptoSupport.randomBytes(random, CryptoSupport.GCM_NONCE_BYTES);
        byte[] passwordKey = CryptoSupport.derivePasswordKey(password, salt, iterations);
        try {
            byte[] ciphertext = CryptoSupport.encryptAesGcm(passwordKey, nonce, dek,
                    CryptoSupport.aad(PASSWORD_AAD, VaultEnvelope.VERSION, vaultId));
            return new PasswordWrapper(iterations, salt, nonce, ciphertext);
        } finally {
            Arrays.fill(passwordKey, (byte) 0);
        }
    }

    private void requireHelloAvailable() throws UnlockException {
        if (helloKey.capability() != HelloCapability.AVAILABLE) {
            throw new UnlockException(UnlockException.Reason.UNSUPPORTED,
                    "Windows Hello is unavailable: " + helloKey.capability());
        }
    }

    private static void requirePassword(char[] password) throws UnlockException {
        if (password == null || password.length == 0) {
            throw new UnlockException(UnlockException.Reason.INVALID_PASSWORD,
                    "Master password is empty");
        }
    }

    private static final class PasswordWrapper implements AutoCloseable {
        private final int iterations;
        private final byte[] salt;
        private final byte[] nonce;
        private final byte[] ciphertext;

        private PasswordWrapper(int iterations, byte[] salt, byte[] nonce, byte[] ciphertext) {
            this.iterations = iterations;
            this.salt = salt;
            this.nonce = nonce;
            this.ciphertext = ciphertext;
        }

        @Override
        public void close() {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(nonce, (byte) 0);
            Arrays.fill(ciphertext, (byte) 0);
        }
    }
}
