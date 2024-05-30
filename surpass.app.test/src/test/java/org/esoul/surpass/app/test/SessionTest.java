package org.esoul.surpass.app.test;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.InvalidPasswordException;
import org.esoul.surpass.app.ServiceUnavailableException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.app.SessionFactory;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.esoul.surpass.table.api.EmptySequenceException;
import org.esoul.surpass.table.api.MaxSizeExceededException;
import org.esoul.surpass.table.api.SecretTable;
import org.esoul.surpass.test.Fs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SessionTest {

    @AfterEach
    public void tearDown() throws Exception {
        Fs.tearDownDataDir();
    }

    @Test
    public void testAddEdit(@TempDir Path tmp) throws Exception {
        Fs.setupDataDir(tmp);
        Session session = SessionFactory.create();
        session.start();
        Assertions.assertEquals(0, session.getSecretTable().getRowNumber());
        session.write("pass1".toCharArray(), "id1".toCharArray(), "note1".toCharArray());
        Assertions.assertEquals(1, session.getSecretTable().getRowNumber());
        session.setEditMode(0);
        session.write("pass2".toCharArray(), "id1".toCharArray(), "note1".toCharArray());
        Assertions.assertArrayEquals("pass2".getBytes(UTF_8), session.getSecretTable().readSecret(0));
    }

    @Test
    public void testLoad(@TempDir Path tmp) throws Exception {
        testStore(tmp);
        Session session = SessionFactory.create();
        session.start();
        checkSecret1(session, "123");
    }

    @Test
    public void testStore(@TempDir Path tmp) throws Exception {
        Fs.setupDataDir(tmp);
        Session session = createSessionWithSecret1();
        Map<String, String> supportedPersistenceServices = session.getSupportedPersistenceServices();
        Assertions.assertEquals(2, supportedPersistenceServices.size());
        Assertions.assertTrue(supportedPersistenceServices.containsKey("org.esoul.surpass.persist.LocalFileSystemPersistenceService"));
        Assertions.assertTrue(supportedPersistenceServices.containsKey("org.esoul.surpass.google.drive.GooglePersistenceService"));
        session.storeData("123".toCharArray(), Collections.singletonList("org.esoul.surpass.persist.LocalFileSystemPersistenceService"));
        Assertions.assertTrue(Files.exists(PersistenceDefaults.getSecrets()));
    }

    @Test
    public void testChangeMasterPassAndStore(@TempDir Path tmp) throws Exception {
        Fs.setupDataDir(tmp);
        Session session = createSessionWithSecret1();
        session.storeData("123".toCharArray(), Collections.singletonList("org.esoul.surpass.persist.LocalFileSystemPersistenceService"));
        session.changeMasterPassAndStoreData("123".toCharArray(), "abc".toCharArray(),
                Collections.singletonList("org.esoul.surpass.persist.LocalFileSystemPersistenceService"));
        checkSecret1(session, "abc");
    }

    @Test
    public void testChangeMasterPassWithWrongCurrent(@TempDir Path tmp) throws Exception {
        Fs.setupDataDir(tmp);
        Session session = createSessionWithSecret1();
        session.storeData("123".toCharArray(), Collections.singletonList("org.esoul.surpass.persist.LocalFileSystemPersistenceService"));
        Assertions.assertThrows(InvalidPasswordException.class, () -> session.changeMasterPassAndStoreData("WRONG".toCharArray(), "abc".toCharArray(),
                Collections.singletonList("org.esoul.surpass.persist.LocalFileSystemPersistenceService")));
    }

    private void checkSecret1(Session session, String masterPass)
            throws IOException, InvalidPasswordException, GeneralSecurityException, ServiceUnavailableException {
        session.loadData(masterPass.toCharArray(), "org.esoul.surpass.persist.LocalFileSystemPersistenceService");
        SecretTable secretTable = session.getSecretTable();
        Assertions.assertEquals(1, secretTable.getRowNumber());
        Assertions.assertArrayEquals("pass1".getBytes(UTF_8), secretTable.readSecret(0));
        Assertions.assertArrayEquals("id1".getBytes(UTF_8), secretTable.readIdentifier(0));
        Assertions.assertArrayEquals("note1".getBytes(UTF_8), secretTable.readNote(0));
    }

    private Session createSessionWithSecret1()
            throws ServiceUnavailableException, IOException, ExistingDataNotLoadedException, MaxSizeExceededException, EmptySequenceException {
        Session session = SessionFactory.create();
        session.start();
        session.write("pass1".toCharArray(), "id1".toCharArray(), "note1".toCharArray());
        Assertions.assertFalse(Files.exists(PersistenceDefaults.getSecrets()));
        return session;
    }
}
