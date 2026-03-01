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
package org.esoul.surpass.gui.jfx.masterpass;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;

import javafx.scene.control.ProgressBar;
import javafx.stage.Window;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.InvalidPasswordException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.gui.jfx.BackgroundOperation;
import org.esoul.surpass.gui.jfx.dialog.MessageDialog;

public class ChangeMasterPassOperation extends BackgroundOperation {

    private final Session session;
    private final char[] currentPass;
    private final char[] newPass;
    private final char[] repeatedNewPass;
    private final Collection<String> selectedServices;

    public ChangeMasterPassOperation(Window owner, ProgressBar progressBar, Session session,
            char[] currentPass, char[] newPass, char[] repeatedNewPass, Collection<String> selectedServices) {
        super(owner, progressBar);
        this.session = session;
        this.currentPass = currentPass;
        this.newPass = newPass;
        this.repeatedNewPass = repeatedNewPass;
        this.selectedServices = selectedServices;
    }

    @Override
    protected Runnable operation() {
        try {
            if (new String(currentPass).trim().isEmpty()) {
                throw new InvalidPasswordException("Current Master Password cannot be empty!");
            }
            if (!Arrays.equals(newPass, repeatedNewPass)) {
                throw new NewMasterPassInputMismatchException();
            }
            if (Arrays.equals(currentPass, newPass)) {
                return () -> {};
            }
            session.changeMasterPassAndStoreData(currentPass, newPass, selectedServices);
        } catch (NewMasterPassInputMismatchException e) {
            return () -> MessageDialog.INVALID_PASS_ERROR.show(owner, e.getMessage());
        } catch (InvalidPasswordException e) {
            return () -> MessageDialog.INVALID_PASS_ERROR.show(owner, "Incorrect Current Master Password.");
        } catch (ExistingDataNotLoadedException e) {
            return () -> MessageDialog.GENERIC_ERROR.show(owner, "Local secrets exist. Load them before changing the Master Password.");
        } catch (IOException | GeneralSecurityException e) {
            return () -> MessageDialog.GENERIC_ERROR.show(owner, "Master Password cannot be changed! " + e.getMessage());
        }
        return () -> {};
    }
}
