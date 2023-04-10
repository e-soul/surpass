package org.esoul.surpass.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.esoul.surpass.persist.api.PersistenceDefaults;

public class Fs {

    /**
     * Resolves {@code datadir} and sets {@link PersistenceDefaults#SYS_PROP_DATADIR}.
     * 
     * @param testDir
     * @return
     */
    public static Path setupDataDir(Path testDir) {
        Path dataDir = testDir.resolve(PersistenceDefaults.DEFAULT_DATADIR);
        System.setProperty(PersistenceDefaults.SYS_PROP_DATADIR, dataDir.toString());
        return dataDir;
    }

    /**
     * Clears {@link PersistenceDefaults#SYS_PROP_DATADIR}.
     * 
     * @throws IOException
     */
    public static void tearDownDataDir() throws IOException {
        String dataDirStr = System.getProperty(PersistenceDefaults.SYS_PROP_DATADIR);
        if (null != dataDirStr) {
            Files.walkFileTree(Paths.get(dataDirStr), new RecursiveDeleteFileVisitor<>());
        }
        System.clearProperty(PersistenceDefaults.SYS_PROP_DATADIR);
    }
}
