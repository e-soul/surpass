/*
   Copyright 2017-2018 e-soul.org
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
package org.esoul.surpass.gui.table;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.esoul.surpass.core.DataTable;

public class ButtonTableCellEditor extends AbstractCellEditor implements TableCellEditor {

    private static final long serialVersionUID = 1L;

    private static final long DEFAULT_CLIPBOARD_EXPIRE_DELAY = 30L;

    private DataTable dataTable = null;

    private JButton button = new JButton();

    private int currentRow = -1;

    public ButtonTableCellEditor(DataTable dataTable) {
        this.dataTable = dataTable;
        button.addActionListener(this::showSecret);
        button.setToolTipText("Click to show secret.");
    }

    @Override
    public Object getCellEditorValue() {
        return button.getText();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        button.setText(value.toString());
        currentRow = row;
        return button;
    }

    private void showSecret(ActionEvent actionEvent) {
        // This String object will not be added to the string pool.
        String secretStr = new String(dataTable.readSecret(currentRow), StandardCharsets.UTF_8);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(secretStr), null);
        setupClipboardExpire(clipboard);
        JOptionPane.showMessageDialog(button, secretStr, "Secret copied to clipboard", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setupClipboardExpire(Clipboard clipboard) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> clipboard.setContents(new StringSelection(""), null), DEFAULT_CLIPBOARD_EXPIRE_DELAY, TimeUnit.SECONDS);
        executor.shutdown();
    }
}
