/*
   Copyright 2017-2025 e-soul.org
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
package org.esoul.surpass.gui.masterpass;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.InvalidPasswordException;
import org.esoul.surpass.app.Session;

public class ChangeMasterPassPolicy {

    private Session session = null;

    public ChangeMasterPassPolicy(Session session) {
        this.session = session;
    }

    public void executeChange(ChangeMasterPassComponents components, Collection<String> selectedServices)
            throws NewMasterPassInputMismatchException, ExistingDataNotLoadedException, IOException, GeneralSecurityException, InvalidPasswordException {
        char[] currentMasterPass = components.currentMasterPasswordField.getPassword();
        if (new String(components.currentMasterPasswordField.getPassword()).trim().isEmpty()) {
            throw new InvalidPasswordException("Current Master Password cannot be empty!");
        }
        if (!Arrays.equals(components.newMasterPasswordField.getPassword(), components.repeatedNewMasterPasswordField.getPassword())) {
            throw new NewMasterPassInputMismatchException();
        }
        if (Arrays.equals(components.currentMasterPasswordField.getPassword(), components.newMasterPasswordField.getPassword())) {
            return;
        }
        if (session.unsavedDataExists()) {
            
        }
        session.changeMasterPassAndStoreData(currentMasterPass, components.newMasterPasswordField.getPassword(), selectedServices);
    }

    public Map<String, String> getSupportedPersistenceServices() {
        return session.getSupportedPersistenceServices();
    }
}
