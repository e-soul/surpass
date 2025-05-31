package org.esoul.surpass.google.drive.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.esoul.surpass.core.DefaultContextAwareCryptoServiceFactory;
import org.esoul.surpass.core.SimpleCipher;
import org.esoul.surpass.crypto.api.ContextAwareCryptoService;
import org.esoul.surpass.crypto.api.CryptoService;
import org.esoul.surpass.google.drive.GooglePersistenceService;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.esoul.surpass.test.Fs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

@TestMethodOrder(OrderAnnotation.class)
public class InteractiveGooglePersistenceServiceTest {

    private ContextAwareCryptoService crypto = null;

    private GooglePersistenceService persistenceService = null;

    @BeforeEach
    public void setUp() throws Exception {
        persistenceService = new GooglePersistenceService();
        CryptoService cryptoService = new SimpleCipher();
        char[] passwordHash = cryptoService.digest("123123".toCharArray());
        crypto = new DefaultContextAwareCryptoServiceFactory().create(cryptoService, passwordHash);
        persistenceService.authorize(crypto);
    }

    @AfterEach
    public void tearDown() throws Exception {
        Fs.tearDownDataDir();
    }

    @Test
    @Order(1)
    public void testWriteReadSearchInteractive(@TempDir Path tmp) throws Exception {
        assumeInteractiveEnv();
        Files.createDirectories(Fs.setupDataDir(tmp));
        persistenceService.write(PersistenceDefaults.DEFAULT_SECRETS, Fixtures.ABC);
        byte[] onlineData = persistenceService.read(PersistenceDefaults.DEFAULT_SECRETS);
        Assertions.assertArrayEquals(Fixtures.ABC, onlineData, "Reading returned different data than what was written!");
        Assertions.assertTrue(persistenceService.exists(PersistenceDefaults.DEFAULT_SECRETS), "Secrets not found!");
        Assertions.assertFalse(persistenceService.exists("non existent"));
    }

    @Test
    @Order(2)
    public void testHandleInvalidRefreshTokenInteractive(@TempDir Path tmp) throws Exception {
        assumeInteractiveEnv();
        Path dataDir = Fs.setupDataDir(tmp);
        setupInvalidRefreshToken(dataDir);
        byte[] secrets = loadSecrets();
        persistenceService.write(PersistenceDefaults.DEFAULT_SECRETS, secrets);
        byte[] onlineData = persistenceService.read(PersistenceDefaults.DEFAULT_SECRETS);
        Assertions.assertArrayEquals(secrets, onlineData, "Reading returned different data than what was written!");
    }

    private void assumeInteractiveEnv() {
        String interactiveTestEnv = System.getProperty("org.esoul.interactive.test.env", Boolean.FALSE.toString());
        Assumptions.assumeTrue(Boolean.parseBoolean(interactiveTestEnv), "Skipping interactive test.");
    }

    private void setupInvalidRefreshToken(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        try (InputStream is = getClass().getResourceAsStream("/StoredCredential_invalid_refresh_token")) {
            Assertions.assertNotNull(is);
            Files.copy(is, dataDir.resolve("StoredCredential"));
        }
    }

    private byte[] loadSecrets() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/secrets_123123")) {
            return is.readAllBytes();
        }
    }
}
