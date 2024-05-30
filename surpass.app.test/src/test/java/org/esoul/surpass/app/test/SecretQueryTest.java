package org.esoul.surpass.app.test;

import java.nio.file.Path;
import java.util.List;

import org.esoul.surpass.app.SecretQuery;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.app.SessionFactory;
import org.esoul.surpass.test.Fs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SecretQueryTest {

    @AfterEach
    public void tearDown() throws Exception {
        Fs.tearDownDataDir();
    }

    @Test
    public void testGetUniqueIdentifiers(@TempDir Path tmp) throws Exception {
        Fs.setupDataDir(tmp);
        Session session = SessionFactory.create();
        session.start();

        SecretQuery secretQuery = session.createQuery();
        List<String> uniqueIdentifiers = secretQuery.getUniqueIdentifiers();
        Assertions.assertEquals(0, uniqueIdentifiers.size());

        session.write("pass1".toCharArray(), "main".toCharArray(), "note1".toCharArray());
        uniqueIdentifiers = secretQuery.getUniqueIdentifiers();
        Assertions.assertEquals(1, uniqueIdentifiers.size());
        Assertions.assertEquals("main", uniqueIdentifiers.get(0));

        // Add a second secret with the main identifier.
        session.write("pass2".toCharArray(), "main".toCharArray(), "note2".toCharArray());
        uniqueIdentifiers = secretQuery.getUniqueIdentifiers();
        Assertions.assertEquals(1, uniqueIdentifiers.size());
        Assertions.assertEquals("main", uniqueIdentifiers.get(0));

        // Add a third secret with a secondary identifier.
        session.write("pass1".toCharArray(), "secondary".toCharArray(), "note1".toCharArray());
        uniqueIdentifiers = secretQuery.getUniqueIdentifiers();
        Assertions.assertEquals(2, uniqueIdentifiers.size());
        Assertions.assertEquals("main", uniqueIdentifiers.get(0));
        Assertions.assertEquals("secondary", uniqueIdentifiers.get(1));

        // Add two more secrets with the secondary identifier.
        session.write("pass2".toCharArray(), "secondary".toCharArray(), "note2".toCharArray());
        session.write("pass3".toCharArray(), "secondary".toCharArray(), "note3".toCharArray());
        uniqueIdentifiers = secretQuery.getUniqueIdentifiers();
        Assertions.assertEquals(2, uniqueIdentifiers.size());
        Assertions.assertEquals("secondary", uniqueIdentifiers.get(0));
        Assertions.assertEquals("main", uniqueIdentifiers.get(1));
    }
}
