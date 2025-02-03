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
package org.esoul.surpass.gui;

import java.awt.Component;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.InvalidPasswordException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.gui.dialog.MessageDialog;

class StoreDataOperation extends BaseDataOperationWorker {

    private Collection<String> selectedServicesIds;

    private Session session = null;
    private char[] password = null;

    StoreDataOperation(Session session, MainWindowComponents components, char[] password, Collection<String> selectedServicesIds) {
        super(components.frame, components.operationProgressBar);
        this.session = session;
        this.password = password;
        this.selectedServicesIds = new ArrayList<>(selectedServicesIds);
    }

    @Override
    protected Consumer<Component> operation() {
        try {
            session.storeData(password, selectedServicesIds);
        } catch (IOException e) {
            return parent -> MessageDialog.STORE_ERROR.show(parent, "Secrets cannot be stored! " + e.getMessage());
        } catch (GeneralSecurityException e) {
            return parent -> MessageDialog.ENCRYPT_ERROR.show(parent, "Secrets cannot be encrypted! " + e.getMessage());
        } catch (ExistingDataNotLoadedException e) {
            return parent -> MessageDialog.STORE_WARNING.show(parent,
                    "Data file exists but is not loaded. Load the data file before you can store new changes.");
        } catch (InvalidPasswordException e) {
            return parent -> MessageDialog.INVALID_PASS_ERROR.show(parent,
                    "This password cannot be used to decrypt your secrets, therefore it cannot be used to encrypt them as well!");
        }
        return parent -> {
        };
    }
}
