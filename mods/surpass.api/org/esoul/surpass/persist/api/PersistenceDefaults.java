/*
   Copyright 2017-2018 e-soul.org
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted
   provided that the following conditions are met:

   1. Redistributions of source code must retain the above copyright notice, this list of conditions
      and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
      and the following disclaimer in the documentation and/or other materials provided with the distribution.

   THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.esoul.surpass.persist.api;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Default names and paths used in the persistence service.
 * 
 * @author mgp
 *
 */
public final class PersistenceDefaults {

    /**
     * Full path to the "data" directory.
     */
    public static final String SYS_PROP_DATADIR = "org.esoul.surpass.persist.datadir";

    /**
     * The name of the "secrets" file.
     */
    public static final String SYS_PROP_SECRETS = "org.esoul.surpass.persist.secrets";

    /**
     * The default name of the "data" directory.
     */
    public static final String DEFAULT_DATADIR = ".surpass";

    /**
     * The default name of the "secrets" file.
     */
    public static final String DEFAULT_SECRETS = "secrets";

    private PersistenceDefaults() {
        // no instances
    }

    /**
     * Returns a {@link Path} to the "secrets" file.
     * The "secrets" file is where user data is stored, passwords, keys, etc.
     * 
     * @return A {@link Path} to the "secrets" file.
     */
    public static Path getSecrets() {
        Path dataDir = getDataDir();
        return dataDir.resolve(System.getProperty(SYS_PROP_SECRETS, DEFAULT_SECRETS));
    }

    /**
     * Returns a {@link Path} to the "data" directory.
     * The "data" directory is where application and user data is stored, e.g. the "secrets" file.
     * 
     * @return A {@link Path} to the "data" directory.
     */
    public static Path getDataDir() {
        String dataDir = System.getProperty(SYS_PROP_DATADIR);
        if (null == dataDir) {
            return Paths.get(getUserHome(), DEFAULT_DATADIR);
        }
        return Paths.get(dataDir);
    }

    private static String getUserHome() {
        String userHome = System.getProperty("user.home");
        if (null == userHome) {
            throw new IllegalStateException("user.home is not set!");
        }
        return userHome;
    }
}
