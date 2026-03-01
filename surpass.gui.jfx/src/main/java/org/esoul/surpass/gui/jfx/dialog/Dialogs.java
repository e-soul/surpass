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

import java.util.Collection;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.stage.Window;

public final class Dialogs {

    private static final int COMBO_ITEM_MAX_LEN = 32;

    private Dialogs() {
        // no instances
    }

    public static char[] showPasswordInputDialog(Window owner, String title) {
        Dialog<char[]> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        PasswordField passwordField = new PasswordField();
        dialog.getDialogPane().setContent(passwordField);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Platform.runLater(passwordField::requestFocus);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return passwordField.getText().toCharArray();
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    public static String showComboSelectionDialog(Window owner, String title, Collection<String> items) {
        String[] filtered = items.stream()
                .map(s -> s.length() > COMBO_ITEM_MAX_LEN ? s.substring(0, COMBO_ITEM_MAX_LEN) + "..." : s)
                .toArray(String[]::new);

        ChoiceDialog<String> dialog = new ChoiceDialog<>(filtered.length > 0 ? filtered[0] : null, filtered);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        return dialog.showAndWait().orElse(null);
    }
}
