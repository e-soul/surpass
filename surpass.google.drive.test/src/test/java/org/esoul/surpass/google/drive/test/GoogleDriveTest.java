package org.esoul.surpass.google.drive.test;

import org.esoul.surpass.google.drive.DriveFacade;
import org.esoul.surpass.google.drive.GooglePersistenceService;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class GoogleDriveTest {

    private static final String TEST_DIR_ID = "surpass-dir-id";

    private static final String TEST_DIR_NAME = "Surpass";

    private static final String TEST_FILE_ID = "surpass-file-id";

    private static final String NON_EXISTENT_FILE = "somethingElse";

    private static final byte[] ABC = new byte[] { 65, 66, 67 };

    private DriveFacade drive = Mockito.mock(DriveFacade.class);

    @AfterEach
    public void tearDown() {
        Mockito.verifyNoMoreInteractions(drive);
    }

    @Test
    public void testWriteReadSearchInteractive() throws Exception {
        assumeInteractiveEnv();
        GooglePersistenceService persistenceService = new GooglePersistenceService();
        persistenceService.write(PersistenceDefaults.DEFAULT_SECRETS, ABC);
        byte[] onlineData = persistenceService.read(PersistenceDefaults.DEFAULT_SECRETS);
        Assertions.assertArrayEquals(ABC, onlineData, "Reading returned different data than what was written!");
        Assertions.assertTrue(persistenceService.exists(PersistenceDefaults.DEFAULT_SECRETS), "Secrets not found!");
        Assertions.assertFalse(persistenceService.exists(NON_EXISTENT_FILE));
    }

    private void assumeInteractiveEnv() {
        String interactiveTestEnv = System.getProperty("org.esoul.interactive.test.env", Boolean.FALSE.toString());
        Assumptions.assumeTrue(Boolean.parseBoolean(interactiveTestEnv), "Skipping interactive test.");
    }

    @Test
    public void testWriteNew() throws Exception {
        Mockito.when(drive.searchDirectory(TEST_DIR_NAME)).thenReturn(null);
        Mockito.when(drive.createDirectory(TEST_DIR_NAME)).thenReturn(TEST_DIR_ID);
        Mockito.when(drive.searchFile(null, PersistenceDefaults.DEFAULT_SECRETS)).thenReturn(null);

        new GooglePersistenceService(drive).write(PersistenceDefaults.DEFAULT_SECRETS, ABC);

        Mockito.verify(drive).searchDirectory(TEST_DIR_NAME);
        Mockito.verify(drive).createDirectory(TEST_DIR_NAME);
        Mockito.verify(drive).searchFile(TEST_DIR_ID, PersistenceDefaults.DEFAULT_SECRETS);
        Mockito.verify(drive).createFile(TEST_DIR_ID, PersistenceDefaults.DEFAULT_SECRETS, ABC);
    }

    @Test
    public void testWriteExistingDir() throws Exception {
        Mockito.when(drive.searchDirectory(TEST_DIR_NAME)).thenReturn(TEST_DIR_ID);
        Mockito.when(drive.searchFile(null, PersistenceDefaults.DEFAULT_SECRETS)).thenReturn(null);

        new GooglePersistenceService(drive).write(PersistenceDefaults.DEFAULT_SECRETS, ABC);

        Mockito.verify(drive).searchDirectory(TEST_DIR_NAME);
        Mockito.verify(drive).searchFile(TEST_DIR_ID, PersistenceDefaults.DEFAULT_SECRETS);
        Mockito.verify(drive).createFile(TEST_DIR_ID, PersistenceDefaults.DEFAULT_SECRETS, ABC);
    }

    @Test
    public void testWriteExistingFile() throws Exception {
        Mockito.when(drive.searchDirectory(TEST_DIR_NAME)).thenReturn(TEST_DIR_ID);
        Mockito.when(drive.searchFile(TEST_DIR_ID, PersistenceDefaults.DEFAULT_SECRETS)).thenReturn(TEST_FILE_ID);

        new GooglePersistenceService(drive).write(PersistenceDefaults.DEFAULT_SECRETS, ABC);

        Mockito.verify(drive).searchDirectory(TEST_DIR_NAME);
        Mockito.verify(drive).searchFile(TEST_DIR_ID, PersistenceDefaults.DEFAULT_SECRETS);
        Mockito.verify(drive).updateFile(TEST_FILE_ID, ABC);
    }

    @Test
    public void testRead() throws Exception {
        Mockito.when(drive.searchDirectory(TEST_DIR_NAME)).thenReturn(TEST_DIR_ID);
        Mockito.when(drive.searchFile(TEST_DIR_ID, PersistenceDefaults.DEFAULT_SECRETS)).thenReturn(TEST_FILE_ID);
        Mockito.when(drive.readFile(TEST_FILE_ID)).thenReturn(ABC);

        GooglePersistenceService persistenceService = new GooglePersistenceService(drive);

        byte[] data = persistenceService.read(PersistenceDefaults.DEFAULT_SECRETS);
        Assertions.assertArrayEquals(ABC, data, "Reading returned unexpected data!");

        data = persistenceService.read(NON_EXISTENT_FILE);
        Assertions.assertArrayEquals(new byte[0], data, "Reading returned unexpected data!");

        Mockito.verify(drive, Mockito.times(2)).searchDirectory(TEST_DIR_NAME);
        Mockito.verify(drive).searchFile(TEST_DIR_ID, PersistenceDefaults.DEFAULT_SECRETS);
        Mockito.verify(drive).searchFile(TEST_DIR_ID, NON_EXISTENT_FILE);
        Mockito.verify(drive).readFile(TEST_FILE_ID);
    }

    @Test
    public void testExists() throws Exception {
        Mockito.when(drive.searchDirectory(TEST_DIR_NAME)).thenReturn(TEST_DIR_ID);
        Mockito.when(drive.searchFile(TEST_DIR_ID, PersistenceDefaults.DEFAULT_SECRETS)).thenReturn(TEST_FILE_ID);

        GooglePersistenceService persistenceService = new GooglePersistenceService(drive);

        boolean fileExists = persistenceService.exists(PersistenceDefaults.DEFAULT_SECRETS);
        Assertions.assertTrue(fileExists, "File should exist but doesn't!");

        fileExists = persistenceService.exists(NON_EXISTENT_FILE);
        Assertions.assertFalse(fileExists, "File exists but shouldn't!");

        Mockito.verify(drive, Mockito.times(2)).searchDirectory(TEST_DIR_NAME);
        Mockito.verify(drive).searchFile(TEST_DIR_ID, PersistenceDefaults.DEFAULT_SECRETS);
        Mockito.verify(drive).searchFile(TEST_DIR_ID, NON_EXISTENT_FILE);
    }
}
