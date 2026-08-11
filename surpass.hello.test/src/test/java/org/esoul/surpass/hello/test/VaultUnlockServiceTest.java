package org.esoul.surpass.hello.test;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.esoul.surpass.core.DefaultContextAwareCryptoServiceFactory;
import org.esoul.surpass.core.SimpleCipher;
import org.esoul.surpass.hello.api.DefaultVaultUnlockService;
import org.esoul.surpass.hello.api.HelloCapability;
import org.esoul.surpass.hello.api.HelloPromptOwner;
import org.esoul.surpass.hello.api.UnlockedVault;
import org.esoul.surpass.hello.api.UnlockException;
import org.esoul.surpass.hello.internal.HelloBinding;
import org.esoul.surpass.hello.internal.BindingStore;
import org.esoul.surpass.hello.internal.HelloKeyClient;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.esoul.surpass.persist.api.PersistenceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class VaultUnlockServiceTest {

    @BeforeAll
    public static void useMinimumTestKdf() {
        System.setProperty("org.esoul.surpass.hello.pbkdf2.iterations", "120000");
    }

    @Test
    public void createsStoresAndUnlocksVersionOneEnvelope() throws Exception {
        MemoryPersistence persistence = new MemoryPersistence("local");
        DefaultVaultUnlockService service = service();
        byte[] plaintext = "vault data".getBytes(UTF_8);

        try (UnlockedVault created = service.unlockWithPassword("correct".toCharArray(), persistence)) {
            service.store(created, plaintext, List.of(persistence));
        }

        try (UnlockedVault opened = service.unlockWithPassword("correct".toCharArray(), persistence)) {
            Assertions.assertArrayEquals(plaintext, opened.copyPlaintext());
        }
        UnlockException wrong = Assertions.assertThrows(UnlockException.class,
                () -> service.unlockWithPassword("wrong".toCharArray(), persistence));
        Assertions.assertEquals(UnlockException.Reason.INVALID_PASSWORD, wrong.reason());
    }

    @Test
    public void detectsContentTampering() throws Exception {
        MemoryPersistence persistence = new MemoryPersistence("local");
        DefaultVaultUnlockService service = service();
        try (UnlockedVault created = service.unlockWithPassword("correct".toCharArray(), persistence)) {
            service.store(created, "vault data".getBytes(UTF_8), List.of(persistence));
        }
        byte[] tampered = persistence.data.get("secrets");
        tampered[tampered.length - 1] ^= 1;
        UnlockException failure = Assertions.assertThrows(UnlockException.class,
                () -> service.unlockWithPassword("correct".toCharArray(), persistence));
        Assertions.assertEquals(UnlockException.Reason.CORRUPT_VAULT, failure.reason());
    }

    @Test
    public void migratesLegacyCiphertextAfterExplicitCommit() throws Exception {
        SimpleCipher legacy = new SimpleCipher();
        MemoryPersistence persistence = new MemoryPersistence("local");
        byte[] plaintext = "legacy data".getBytes(UTF_8);
        persistence.write("secrets", legacy.encrypt("correct".toCharArray(), plaintext));
        DefaultVaultUnlockService service = new DefaultVaultUnlockService(legacy,
                new DefaultContextAwareCryptoServiceFactory());

        try (UnlockedVault opened = service.unlockWithPassword("correct".toCharArray(), persistence)) {
            Assertions.assertTrue(opened.migrationPending());
            Assertions.assertArrayEquals(plaintext, opened.copyPlaintext());
            service.commitMigration(opened, persistence);
            Assertions.assertFalse(opened.migrationPending());
        }
        Assertions.assertTrue(persistence.data.containsKey("secrets.version0.backup"));
        Assertions.assertArrayEquals(new byte[] { 'S', 'P', 'V', '1' },
                java.util.Arrays.copyOf(persistence.data.get("secrets"), 4));
    }

    @Test
    public void rollsBackLegacyMigrationWhenReadBackVerificationFails() throws Exception {
        SimpleCipher legacy = new SimpleCipher();
        MemoryPersistence persistence = new MemoryPersistence("local");
        byte[] legacyCiphertext = legacy.encrypt("correct".toCharArray(),
                "legacy data".getBytes(UTF_8));
        persistence.write("secrets", legacyCiphertext);
        DefaultVaultUnlockService service = new DefaultVaultUnlockService(legacy,
                new DefaultContextAwareCryptoServiceFactory());

        try (UnlockedVault opened = service.unlockWithPassword("correct".toCharArray(), persistence)) {
            persistence.corruptNextSecretsWrite = true;
            Assertions.assertThrows(GeneralSecurityException.class,
                    () -> service.commitMigration(opened, persistence));
            Assertions.assertTrue(opened.migrationPending());
        }
        Assertions.assertArrayEquals(legacyCiphertext, persistence.read("secrets"));
    }

    @Test
    public void closeErasesAndInvalidatesPlaintextAccess() throws Exception {
        MemoryPersistence persistence = new MemoryPersistence("local");
        UnlockedVault vault = service().unlockWithPassword("correct".toCharArray(), persistence);
        vault.close();
        Assertions.assertThrows(IllegalStateException.class, vault::copyPlaintext);
        Assertions.assertFalse(vault.toString().contains("correct"));
    }

    @Test
    public void enrollsUnlocksAndStoresWithDeterministicHelloSignature(
            @TempDir Path temporaryDirectory)
            throws Exception {
        String oldDataDir = System.getProperty(PersistenceDefaults.SYS_PROP_DATADIR);
        System.setProperty(PersistenceDefaults.SYS_PROP_DATADIR, temporaryDirectory.toString());
        try {
            MemoryPersistence persistence = new MemoryPersistence("local");
            FakeHelloKeyClient helloKey = new FakeHelloKeyClient();
            DefaultVaultUnlockService service = new DefaultVaultUnlockService(new SimpleCipher(),
                    new DefaultContextAwareCryptoServiceFactory(), helloKey, new BindingStore(),
                    new SecureRandom());
            try (UnlockedVault passwordVault = service.unlockWithPassword("correct".toCharArray(),
                    persistence)) {
                service.store(passwordVault, "before".getBytes(UTF_8), List.of(persistence));
                service.enrollHello(HelloPromptOwner.none(), passwordVault);
            }
            Assertions.assertTrue(service.isHelloEnrolled("local"));

            try (UnlockedVault helloVault = service.unlockWithHello(HelloPromptOwner.none(),
                    persistence)) {
                Assertions.assertArrayEquals("before".getBytes(UTF_8), helloVault.copyPlaintext());
                service.store(helloVault, "after".getBytes(UTF_8), List.of(persistence));
            }
            try (UnlockedVault passwordVault = service.unlockWithPassword("correct".toCharArray(),
                    persistence)) {
                Assertions.assertArrayEquals("after".getBytes(UTF_8), passwordVault.copyPlaintext());
            }

            helloKey.cancelSigning = true;
            UnlockException canceled = Assertions.assertThrows(UnlockException.class,
                    () -> service.unlockWithHello(HelloPromptOwner.none(), persistence));
            Assertions.assertEquals(UnlockException.Reason.CANCELED, canceled.reason());
        } finally {
            if (oldDataDir == null) {
                System.clearProperty(PersistenceDefaults.SYS_PROP_DATADIR);
            } else {
                System.setProperty(PersistenceDefaults.SYS_PROP_DATADIR, oldDataDir);
            }
        }
    }

    @Test
    public void passwordChangeDisablesBindingWhenHelloRefreshIsCanceled(
            @TempDir Path temporaryDirectory) throws Exception {
        String oldDataDir = System.getProperty(PersistenceDefaults.SYS_PROP_DATADIR);
        System.setProperty(PersistenceDefaults.SYS_PROP_DATADIR, temporaryDirectory.toString());
        try {
            MemoryPersistence persistence = new MemoryPersistence("local");
            FakeHelloKeyClient helloKey = new FakeHelloKeyClient();
            DefaultVaultUnlockService service = new DefaultVaultUnlockService(new SimpleCipher(),
                    new DefaultContextAwareCryptoServiceFactory(), helloKey, new BindingStore(),
                    new SecureRandom());
            try (UnlockedVault vault = service.unlockWithPassword("old".toCharArray(), persistence)) {
                service.store(vault, "data".getBytes(UTF_8), List.of(persistence));
                service.enrollHello(HelloPromptOwner.none(), vault);
                helloKey.cancelSigning = true;
                service.changeMasterPassword(HelloPromptOwner.none(), vault, "old".toCharArray(),
                        "new".toCharArray(), List.of(persistence));
            }
            Assertions.assertFalse(service.isHelloEnrolled("local"));
            try (UnlockedVault opened = service.unlockWithPassword("new".toCharArray(), persistence)) {
                Assertions.assertArrayEquals("data".getBytes(UTF_8), opened.copyPlaintext());
            }
            UnlockException oldPassword = Assertions.assertThrows(UnlockException.class,
                    () -> service.unlockWithPassword("old".toCharArray(), persistence));
            Assertions.assertEquals(UnlockException.Reason.INVALID_PASSWORD, oldPassword.reason());
        } finally {
            if (oldDataDir == null) {
                System.clearProperty(PersistenceDefaults.SYS_PROP_DATADIR);
            } else {
                System.setProperty(PersistenceDefaults.SYS_PROP_DATADIR, oldDataDir);
            }
        }
    }

    @Test
    public void failedEnrollmentDeletesNewHelloKey(@TempDir Path temporaryDirectory)
            throws Exception {
        String oldDataDir = System.getProperty(PersistenceDefaults.SYS_PROP_DATADIR);
        System.setProperty(PersistenceDefaults.SYS_PROP_DATADIR, temporaryDirectory.toString());
        try {
            MemoryPersistence persistence = new MemoryPersistence("local");
            FakeHelloKeyClient helloKey = new FakeHelloKeyClient();
            helloKey.materialLength = 31;
            DefaultVaultUnlockService service = new DefaultVaultUnlockService(new SimpleCipher(),
                    new DefaultContextAwareCryptoServiceFactory(), helloKey, new BindingStore(),
                    new SecureRandom());
            try (UnlockedVault vault = service.unlockWithPassword("correct".toCharArray(),
                    persistence)) {
                service.store(vault, "data".getBytes(UTF_8), List.of(persistence));
                UnlockException failure = Assertions.assertThrows(UnlockException.class,
                        () -> service.enrollHello(HelloPromptOwner.none(), vault));
                Assertions.assertEquals(UnlockException.Reason.NATIVE_FAILURE, failure.reason());
            }
            Assertions.assertTrue(helloKey.deleted);
            Assertions.assertFalse(service.isHelloEnrolled("local"));
        } finally {
            restoreDataDirectory(oldDataDir);
        }
    }

    @Test
    public void changedHelloChallengeFailsBindingAuthentication(@TempDir Path temporaryDirectory)
            throws Exception {
        String oldDataDir = System.getProperty(PersistenceDefaults.SYS_PROP_DATADIR);
        System.setProperty(PersistenceDefaults.SYS_PROP_DATADIR, temporaryDirectory.toString());
        try {
            MemoryPersistence persistence = new MemoryPersistence("local");
            FakeHelloKeyClient helloKey = new FakeHelloKeyClient();
            BindingStore store = new BindingStore();
            DefaultVaultUnlockService service = new DefaultVaultUnlockService(new SimpleCipher(),
                    new DefaultContextAwareCryptoServiceFactory(), helloKey, store,
                    new SecureRandom());
            try (UnlockedVault vault = service.unlockWithPassword("correct".toCharArray(),
                    persistence)) {
                service.store(vault, "data".getBytes(UTF_8), List.of(persistence));
                service.enrollHello(HelloPromptOwner.none(), vault);
            }
            HelloBinding binding = store.read("local");
            byte[] changedChallenge = binding.challenge().clone();
            changedChallenge[0] ^= 1;
            store.write(new HelloBinding(binding.persistenceServiceId(), binding.vaultId(),
                    binding.keyName(), changedChallenge, binding.nonce(), binding.wrappedBundle(),
                    binding.createdAt(), binding.updatedAt()));

            UnlockException failure = Assertions.assertThrows(UnlockException.class,
                    () -> service.unlockWithHello(HelloPromptOwner.none(), persistence));
            Assertions.assertEquals(UnlockException.Reason.BINDING_MISMATCH, failure.reason());
        } finally {
            restoreDataDirectory(oldDataDir);
        }
    }

    private static void restoreDataDirectory(String oldDataDir) {
        if (oldDataDir == null) {
            System.clearProperty(PersistenceDefaults.SYS_PROP_DATADIR);
        } else {
            System.setProperty(PersistenceDefaults.SYS_PROP_DATADIR, oldDataDir);
        }
    }

    private static DefaultVaultUnlockService service() {
        return new DefaultVaultUnlockService(new SimpleCipher(),
                new DefaultContextAwareCryptoServiceFactory());
    }

    private static final class MemoryPersistence implements PersistenceService {
        private final String id;
        private final Map<String, byte[]> data = new HashMap<>();
        private boolean corruptNextSecretsWrite;

        private MemoryPersistence(String id) {
            this.id = id;
        }

        @Override
        public byte[] read(String name) throws IOException {
            byte[] value = data.get(name);
            if (value == null) {
                throw new java.nio.file.NoSuchFileException(name);
            }
            return value.clone();
        }

        @Override
        public void write(String name, byte[] value) throws IOException {
            byte[] stored = value.clone();
            if (corruptNextSecretsWrite && name.equals("secrets")) {
                corruptNextSecretsWrite = false;
                stored[stored.length - 1] ^= 1;
            }
            data.put(name, stored);
        }

        @Override
        public boolean exists(String name) throws IOException {
            return data.containsKey(name);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getDisplayName() {
            return id;
        }
    }

    private static final class FakeHelloKeyClient implements HelloKeyClient {
        private static final String KEY_NAME = "Surpass vault fake-key";
        private final byte[] keyMaterial = new byte[32];
        private boolean cancelSigning;
        private boolean deleted;
        private int materialLength = 32;

        private FakeHelloKeyClient() {
            java.util.Arrays.fill(keyMaterial, (byte) 0x5a);
        }

        @Override
        public HelloCapability capability() {
            return HelloCapability.AVAILABLE;
        }

        @Override
        public HelloPromptOwner capturePromptOwner() {
            return HelloPromptOwner.none();
        }

        @Override
        public Key createKey(HelloPromptOwner owner, byte[] vaultId) {
            return new Key(KEY_NAME);
        }

        @Override
        public byte[] deriveKeyMaterial(HelloPromptOwner owner, String requestedKeyName,
                byte[] challenge) throws UnlockException {
            if (cancelSigning) {
                throw new UnlockException(UnlockException.Reason.CANCELED, "Canceled");
            }
            Assertions.assertEquals(KEY_NAME, requestedKeyName);
            Assertions.assertEquals(32, challenge.length);
            return java.util.Arrays.copyOf(keyMaterial, materialLength);
        }

        @Override
        public void deleteKey(String requestedKeyName) {
            Assertions.assertEquals(KEY_NAME, requestedKeyName);
            deleted = true;
        }

        @Override
        public void cancel() {
            cancelSigning = true;
        }
    }
}
