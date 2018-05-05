package org.esoul.surpass.persist.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.esoul.surpass.persist.LocalFileSystemPersistenceService;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LocalFileSystemPersistenceServiceTest {

    private static Path testDir = null;

    private static Path dataDir = null;

    private static Path secrets = null;

    private static final byte[] TEST_DATA = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

    @BeforeClass
    public static void setUpClass() throws Exception {
        String javaTmpDir = System.getProperty("java.io.tmpdir");
        String testDirName = LocalFileSystemPersistenceServiceTest.class.getSimpleName() + System.currentTimeMillis();
        testDir = Paths.get(javaTmpDir, testDirName);
        Files.createDirectories(testDir);

        Assert.assertTrue(testDir + " does not exist!", Files.exists(testDir));

        dataDir = testDir.resolve(PersistenceDefaults.DEFAULT_DATADIR);

        System.setProperty(PersistenceDefaults.SYS_PROP_DATADIR, dataDir.toString());

        secrets = dataDir.resolve(PersistenceDefaults.DEFAULT_SECRETS);
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(secrets);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        System.clearProperty(PersistenceDefaults.SYS_PROP_DATADIR);

        Files.walkFileTree(testDir, new RecursiveDeleteFileVisitor<>());
        Assert.assertFalse(testDir + " not deleted!", Files.exists(testDir));
    }

    @Test
    public void testRead() throws Exception {
        Files.write(secrets, TEST_DATA);

        LocalFileSystemPersistenceService lfsps = new LocalFileSystemPersistenceService();
        byte[] data = lfsps.read(PersistenceDefaults.DEFAULT_SECRETS);

        Assert.assertArrayEquals(TEST_DATA, data);
    }

    @Test
    public void testWrite() throws Exception {
        Assert.assertFalse(secrets + " already exists!", Files.exists(secrets));

        LocalFileSystemPersistenceService lfsps = new LocalFileSystemPersistenceService();
        lfsps.write(PersistenceDefaults.DEFAULT_SECRETS, TEST_DATA);

        Assert.assertTrue(secrets + " does not exist!", Files.exists(secrets));
    }

    @Test
    public void testExists() throws Exception {
        LocalFileSystemPersistenceService lfsps = new LocalFileSystemPersistenceService();

        Assert.assertFalse(PersistenceDefaults.DEFAULT_SECRETS + " reported as existing!", lfsps.exists(PersistenceDefaults.DEFAULT_SECRETS));

        Files.write(secrets, TEST_DATA);

        Assert.assertTrue(PersistenceDefaults.DEFAULT_SECRETS + " reported as non-existing!", lfsps.exists(PersistenceDefaults.DEFAULT_SECRETS));
    }
}