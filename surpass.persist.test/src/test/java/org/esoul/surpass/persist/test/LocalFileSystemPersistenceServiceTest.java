package org.esoul.surpass.persist.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.esoul.surpass.persist.LocalFileSystemPersistenceService;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LocalFileSystemPersistenceServiceTest {

    private static Path testDir = null;

    private static Path dataDir = null;

    private static Path secrets = null;

    private static final byte[] TEST_DATA = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

    @BeforeAll
    public static void setUpClass() throws Exception {
        String javaTmpDir = System.getProperty("java.io.tmpdir");
        String testDirName = LocalFileSystemPersistenceServiceTest.class.getSimpleName() + System.currentTimeMillis();
        testDir = Paths.get(javaTmpDir, testDirName);
        Files.createDirectories(testDir);

        Assertions.assertTrue(Files.exists(testDir), testDir + " does not exist!");

        dataDir = testDir.resolve(PersistenceDefaults.DEFAULT_DATADIR);

        System.setProperty(PersistenceDefaults.SYS_PROP_DATADIR, dataDir.toString());

        secrets = dataDir.resolve(PersistenceDefaults.DEFAULT_SECRETS);
    }

    @AfterEach
    public void tearDown() throws Exception {
        Files.deleteIfExists(secrets);
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
        System.clearProperty(PersistenceDefaults.SYS_PROP_DATADIR);

        Files.walkFileTree(testDir, new RecursiveDeleteFileVisitor<>());
        Assertions.assertFalse(Files.exists(testDir), testDir + " not deleted!");
    }

    @Test
    public void testRead() throws Exception {
        Files.write(secrets, TEST_DATA);

        LocalFileSystemPersistenceService lfsps = new LocalFileSystemPersistenceService();
        byte[] data = lfsps.read(PersistenceDefaults.DEFAULT_SECRETS);

        Assertions.assertArrayEquals(TEST_DATA, data);
    }

    @Test
    public void testWrite() throws Exception {
        Assertions.assertFalse(Files.exists(secrets), secrets + " already exists!");

        LocalFileSystemPersistenceService lfsps = new LocalFileSystemPersistenceService();
        lfsps.write(PersistenceDefaults.DEFAULT_SECRETS, TEST_DATA);

        Assertions.assertTrue(Files.exists(secrets), secrets + " does not exist!");
    }

    @Test
    public void testExists() throws Exception {
        LocalFileSystemPersistenceService lfsps = new LocalFileSystemPersistenceService();

        Assertions.assertFalse(lfsps.exists(PersistenceDefaults.DEFAULT_SECRETS), PersistenceDefaults.DEFAULT_SECRETS + " reported as existing!");

        Files.write(secrets, TEST_DATA);

        Assertions.assertTrue(lfsps.exists(PersistenceDefaults.DEFAULT_SECRETS), PersistenceDefaults.DEFAULT_SECRETS + " reported as non-existing!");
    }
}