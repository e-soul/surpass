package org.esoul.surpass.test;

import java.nio.file.Path;

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
     */
    public static void tearDownDataDir() {
        System.clearProperty(PersistenceDefaults.SYS_PROP_DATADIR);
    }
}
