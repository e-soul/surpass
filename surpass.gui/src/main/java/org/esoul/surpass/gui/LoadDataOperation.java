/*
   Copyright 2017-2024 e-soul.org
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
import java.util.function.Consumer;

import javax.swing.table.AbstractTableModel;

import org.esoul.surpass.app.InvalidPasswordException;
import org.esoul.surpass.app.ServiceUnavailableException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.gui.dialog.MessageDialog;

class LoadDataOperation extends BaseDataOperationWorker {

    private AbstractTableModel tableModel;
    private Session session;
    private char[] password;
    private String serviceId;

    LoadDataOperation(Session session, MainWindowComponents components, char[] password, String serviceId) {
        super(components.frame, components.operationProgressBar);
        this.tableModel = components.tableModel;
        this.session = session;
        this.password = password;
        this.serviceId = serviceId;
    }

    @Override
    protected Consumer<Component> operation() {
        try {
            session.loadData(password, serviceId);
        } catch (IOException | ServiceUnavailableException e) {
            return parent -> MessageDialog.LOAD_ERROR.show(parent, "Secrets cannot be loaded! " + e.getMessage());
        } catch (GeneralSecurityException e) {
            return parent -> MessageDialog.DECRYPT_ERROR.show(parent, "Secrets cannot be decrypted! " + e.getMessage());
        } catch (InvalidPasswordException e) {
            return parent -> MessageDialog.EMPTY_PASS_ERROR.show(parent, "Password is empty! Provide password and try again.");
        }
        return msg -> {
        };
    }

    @Override
    protected void doneSuccess() {
        tableModel.fireTableDataChanged();
    }
}
