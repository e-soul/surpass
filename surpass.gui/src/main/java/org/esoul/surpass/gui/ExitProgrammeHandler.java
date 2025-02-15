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

import java.awt.SystemTray;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.BooleanSupplier;

import javax.swing.JOptionPane;

public class ExitProgrammeHandler implements ActionListener {

    private BooleanSupplier unsavedDataExistSupplier = null;

    private MainWindowComponents components = null;

    public ExitProgrammeHandler(BooleanSupplier unsavedDataExistSupplier, MainWindowComponents components) {
        this.unsavedDataExistSupplier = unsavedDataExistSupplier;
        this.components = components;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int selectedOption;
        if (unsavedDataExistSupplier.getAsBoolean()) {
            selectedOption = JOptionPane.showConfirmDialog(components.frame,
                    "You have unsaved data.\nExiting will result in DATA LOSS! Are you sure you want to exit?", "Exit despite unsaved data?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        } else {
            selectedOption = JOptionPane.showConfirmDialog(components.frame, "Are you sure you want to exit?", "Exit", JOptionPane.YES_NO_OPTION);
        }
        if (JOptionPane.YES_OPTION == selectedOption) {
            if (SystemTray.isSupported()) {
                SystemTray.getSystemTray().remove(components.trayIcon);
            }
            components.frame.dispose();
        }
    }
}
