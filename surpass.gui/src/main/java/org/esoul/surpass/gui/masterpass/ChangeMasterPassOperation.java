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

import java.awt.Component;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.function.Consumer;

import javax.swing.JProgressBar;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.InvalidPasswordException;
import org.esoul.surpass.gui.BaseDataOperationWorker;
import org.esoul.surpass.gui.dialog.MessageDialog;

public class ChangeMasterPassOperation extends BaseDataOperationWorker {

    private ChangeMasterPassComponents components;
    private ChangeMasterPassPolicy policy;
    private Collection<String> selectedServices;

    public ChangeMasterPassOperation(Component parent, JProgressBar operationProgressBar, ChangeMasterPassComponents components, ChangeMasterPassPolicy policy,
            Collection<String> selectedServices) {
        super(parent, operationProgressBar);
        this.components = components;
        this.policy = policy;
        this.selectedServices = selectedServices;
    }

    @Override
    protected Consumer<Component> operation() {
        try {
            policy.executeChange(components, selectedServices);
        } catch (NewMasterPassInputMismatchException e) {
            return parent -> MessageDialog.INVALID_PASS_ERROR.show(parent, e.getMessage());
        } catch (InvalidPasswordException e) {
            return parent -> MessageDialog.INVALID_PASS_ERROR.show(parent, "Incorrect Current Master Password.");
        } catch (ExistingDataNotLoadedException e) {
            return parent -> MessageDialog.GENERIC_ERROR.show(parent, "Local secrets exist. Load them before changing the Master Password.");
        } catch (IOException | GeneralSecurityException e) {
            return parent -> MessageDialog.GENERIC_ERROR.show(parent, "Master Password cannot be changed! " + e.getMessage());
        }
        return parent -> {
        };
    }
}
