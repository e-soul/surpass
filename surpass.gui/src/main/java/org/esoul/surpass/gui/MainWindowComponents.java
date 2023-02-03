/*
   Copyright 2017-2022 e-soul.org
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

import java.awt.TrayIcon;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 * Holds references to JFC components and operations only on them.
 * 
 * @author mgp
 *
 */
final class MainWindowComponents {

    JFrame frame = null;

    JMenuItem editSecretMenuItem = null;

    JMenuItem removeSecretMenuItem = null;

    JTable table = null;

    AbstractTableModel tableModel = null;

    JButton addRowButton = null;

    JButton showSecretButton = null;

    JButton editRowButton = null;

    JButton removeRowButton = null;

    TrayIcon trayIcon = null;

    JLabel secretCountLabel = null;

    JProgressBar operationProgressBar = null;

    void setEnabledTableButtons(boolean enabled) {
    	editSecretMenuItem.setEnabled(enabled);
    	removeSecretMenuItem.setEnabled(enabled);
        showSecretButton.setEnabled(enabled);
        editRowButton.setEnabled(enabled);
        removeRowButton.setEnabled(enabled);
    }
}
