/*
   Copyright 2017-2023 e-soul.org
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
package org.esoul.surpass.gui.dialog;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

public final class Dialogs {

    private static final int COMBO_ITEM_MAX_LEN = 32;

    private Dialogs() {
        // no instances
    }

    public static char[] showPasswordInputDialog(Component parentComponent, String title) {
        JPasswordField passwordField = new JPasswordField();
        JOptionPane pane = new JOptionPane(passwordField, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog(parentComponent, title);
        dialog.addWindowFocusListener(new WindowAdapter() {

            @Override
            public void windowGainedFocus(WindowEvent e) {
                passwordField.requestFocusInWindow();
            }
        });
        dialog.setVisible(true);
        dialog.dispose();
        if (pane.getValue() instanceof Integer option && JOptionPane.OK_OPTION == option.intValue()) {
            return passwordField.getPassword();
        }
        return null;
    }

    public static String showComboSelectionDialog(Component parentComponent, String title, Collection<String> items) {
        String[] filtered = items.stream().map(s -> {
            if (s.length() > COMBO_ITEM_MAX_LEN) {
                return s.substring(0, COMBO_ITEM_MAX_LEN) + "...";
            }
            return s;
        }).toArray(String[]::new);
        JComboBox<String> combo = new JComboBox<String>(filtered);
        JOptionPane pane = new JOptionPane(combo, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog(parentComponent, title);
        dialog.setVisible(true);
        dialog.dispose();
        if (pane.getValue() instanceof Integer option && JOptionPane.OK_OPTION == option.intValue()) {
            return (String) combo.getSelectedItem();
        }
        return null;
    }

    public static void show(Window parentFrame, Window frame) {
        frame.pack();
        frame.setLocationRelativeTo(parentFrame);
        frame.setVisible(true);
        frame.dispose();
    }
}
