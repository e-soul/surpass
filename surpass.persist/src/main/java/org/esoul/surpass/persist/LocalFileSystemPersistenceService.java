/*
   Copyright 2017-2022 e-soul.org
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
package org.esoul.surpass.persist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.esoul.surpass.persist.api.PersistenceService;

public class LocalFileSystemPersistenceService implements PersistenceService {

    @Override
    public byte[] read(String name) throws IOException {
        Path path = getPath(name);
        return Files.readAllBytes(path);
    }

    @Override
    public void write(String name, byte[] data) throws IOException {
        Path path = getPath(name);
        Files.write(path, data);
    }

    @Override
    public boolean exists(String name) throws IOException {
        Path path = getPath(name);
        return Files.exists(path);
    }

    private Path getPath(String name) throws IOException {
        Path path;
        if (null != name) {
            path = PersistenceDefaults.getDataDir().resolve(name);
        } else {
            path = PersistenceDefaults.getSecrets();
        }
        Files.createDirectories(path.getParent());
        return path;
    }
}
