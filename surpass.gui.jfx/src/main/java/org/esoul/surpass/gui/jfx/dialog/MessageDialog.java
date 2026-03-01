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
package org.esoul.surpass.gui.jfx.dialog;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public enum MessageDialog {

    SAVE_DATA_INFO("Save data", Alert.AlertType.INFORMATION),
    UNEXPECTED_ERROR("Unexpected error!", Alert.AlertType.ERROR),
    GENERIC_ERROR("Error", Alert.AlertType.ERROR),
    ENCRYPT_ERROR("Encrypt error", Alert.AlertType.ERROR),
    DECRYPT_ERROR("Decrypt error", Alert.AlertType.ERROR),
    LOAD_ERROR("Load error", Alert.AlertType.ERROR),
    STORE_ERROR("Store error", Alert.AlertType.ERROR),
    STORE_WARNING("Store warning", Alert.AlertType.WARNING),
    EMPTY_PASS_ERROR("Empty password error", Alert.AlertType.ERROR),
    INVALID_PASS_ERROR("Invalid password", Alert.AlertType.ERROR);

    private final String title;
    private final Alert.AlertType alertType;

    MessageDialog(String title, Alert.AlertType alertType) {
        this.title = title;
        this.alertType = alertType;
    }

    public void show(Window owner, String message) {
        Alert alert = new Alert(alertType);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean showConfirmation(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
