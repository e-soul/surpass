package org.esoul.surpass.google.drive.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.esoul.surpass.crypto.api.ContextAwareCryptoService;
import org.esoul.surpass.google.drive.GooglePersistenceService;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InteractiveGooglePersistenceServiceTest {

    private ContextAwareCryptoService crypto = Fixtures.setUpCryptoStub();

    private GooglePersistenceService persistenceService = new GooglePersistenceService();

    @BeforeEach
    public void setUp() throws Exception {
        persistenceService.authorize(crypto);
    }

    @Test
    public void testWriteReadSearchInteractive() throws Exception {
        assumeInteractiveEnv();
        persistenceService.write(PersistenceDefaults.DEFAULT_SECRETS, Fixtures.ABC);
        byte[] onlineData = persistenceService.read(PersistenceDefaults.DEFAULT_SECRETS);
        Assertions.assertArrayEquals(Fixtures.ABC, onlineData, "Reading returned different data than what was written!");
        Assertions.assertTrue(persistenceService.exists(PersistenceDefaults.DEFAULT_SECRETS), "Secrets not found!");
        Assertions.assertFalse(persistenceService.exists("non existent"));
    }

    @Test
    public void testHandleInvalidRefreshTokenInteractive() throws Exception {
        assumeInteractiveEnv();
        Path testDir = Paths.get("google_drive_test_invalid_refresh_token_" + System.currentTimeMillis());
        setupInvalidRefreshToken(testDir);
        persistenceService.write(PersistenceDefaults.DEFAULT_SECRETS, Fixtures.ABC);
        tearDownInvalidRefreshToken(testDir);
    }

    private void assumeInteractiveEnv() {
        String interactiveTestEnv = System.getProperty("org.esoul.interactive.test.env", Boolean.FALSE.toString());
        Assumptions.assumeTrue(Boolean.parseBoolean(interactiveTestEnv), "Skipping interactive test.");
    }

    private void setupInvalidRefreshToken(Path testDir) throws IOException {
        Files.createDirectories(testDir);
        try (InputStream is = getClass().getResourceAsStream("/StoredCredential_invalid_refresh_token")) {
            Assertions.assertNotNull(is);
            Files.copy(is, testDir.resolve("StoredCredential"));
        }
        System.setProperty(PersistenceDefaults.SYS_PROP_DATADIR, testDir.toAbsolutePath().toString());
    }

    private void tearDownInvalidRefreshToken(Path testDir) throws IOException {
        System.clearProperty(PersistenceDefaults.SYS_PROP_DATADIR);
        Files.walkFileTree(testDir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (null == e) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
                throw e;
            }
        });
    }
}
