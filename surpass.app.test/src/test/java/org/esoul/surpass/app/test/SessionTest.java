package org.esoul.surpass.app.test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.esoul.surpass.app.Session;
import org.esoul.surpass.app.SessionFactory;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.esoul.surpass.table.api.SecretTable;
import org.esoul.surpass.test.Fs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SessionTest {

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    @AfterEach
    public void tearDown() {
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
        Assertions.assertArrayEquals("pass2".getBytes(UTF8), session.getSecretTable().readSecret(0));
    }

    @Test
    public void testLoad(@TempDir Path tmp) throws Exception {
        testStore(tmp);
        Fs.setupDataDir(tmp);
        Session session = SessionFactory.create();
        session.start();
        session.loadData("123".toCharArray());
        SecretTable secretTable = session.getSecretTable();
        Assertions.assertEquals(1, secretTable.getRowNumber());
        Assertions.assertArrayEquals("pass1".getBytes(UTF8), secretTable.readSecret(0));
        Assertions.assertArrayEquals("id1".getBytes(UTF8), secretTable.readIdentifier(0));
        Assertions.assertArrayEquals("note1".getBytes(UTF8), secretTable.readNote(0));
    }

    @Test
    public void testStore(@TempDir Path tmp) throws Exception {
        Fs.setupDataDir(tmp);
        Session session = SessionFactory.create();
        session.start();
        session.write("pass1".toCharArray(), "id1".toCharArray(), "note1".toCharArray());
        Assertions.assertFalse(Files.exists(PersistenceDefaults.getSecrets()));
        session.storeData("123".toCharArray());
        Assertions.assertTrue(Files.exists(PersistenceDefaults.getSecrets()));
    }
}
