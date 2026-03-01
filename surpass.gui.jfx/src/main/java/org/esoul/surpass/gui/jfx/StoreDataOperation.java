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
package org.esoul.surpass.gui.jfx;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;

import javafx.scene.control.ProgressBar;
import javafx.stage.Window;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.InvalidPasswordException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.gui.jfx.dialog.MessageDialog;

class StoreDataOperation extends BackgroundOperation {

    private final Session session;
    private final char[] password;
    private final Collection<String> selectedServicesIds;
    private final Runnable onSuccess;

    StoreDataOperation(Window owner, ProgressBar progressBar, Session session, char[] password,
            Collection<String> selectedServicesIds, Runnable onSuccess) {
        super(owner, progressBar);
        this.session = session;
        this.password = password;
        this.selectedServicesIds = new ArrayList<>(selectedServicesIds);
        this.onSuccess = onSuccess;
    }

    @Override
    protected Runnable operation() {
        try {
            session.storeData(password, selectedServicesIds);
        } catch (IOException e) {
            return () -> MessageDialog.STORE_ERROR.show(owner, "Secrets cannot be stored! " + e.getMessage());
        } catch (GeneralSecurityException e) {
            return () -> MessageDialog.ENCRYPT_ERROR.show(owner, "Secrets cannot be encrypted! " + e.getMessage());
        } catch (ExistingDataNotLoadedException e) {
            return () -> MessageDialog.STORE_WARNING.show(owner,
                    "Data file exists but is not loaded. Load the data file before you can store new changes.");
        } catch (InvalidPasswordException e) {
            return () -> MessageDialog.INVALID_PASS_ERROR.show(owner,
                    "This password cannot be used to decrypt your secrets, therefore it cannot be used to encrypt them as well!");
        }
        return () -> {};
    }

    @Override
    protected void doneSuccess() {
        onSuccess.run();
    }
}
