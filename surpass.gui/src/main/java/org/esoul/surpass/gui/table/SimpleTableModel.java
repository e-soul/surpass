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
package org.esoul.surpass.gui.table;

import javax.swing.table.AbstractTableModel;

import org.esoul.surpass.table.api.SecretTable;

public class SimpleTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static final int IDENTIFIER_COLUMN_INDEX = 0;

    public static final int NOTE_COLUMN_INDEX = 1;

    public static final String[] COLUMN_NAMES = new String[] { "Identifier", "Note" };

    private SecretTable secretTable = null;

    public SimpleTableModel(SecretTable secretTable) {
        this.secretTable = secretTable;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (IDENTIFIER_COLUMN_INDEX == col) {
            return new String(secretTable.readIdentifier(row));
        } else if (NOTE_COLUMN_INDEX == col) {
            return new String(secretTable.readNote(row));
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public int getRowCount() {
        return secretTable.getRowNumber();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return NOTE_COLUMN_INDEX == column;
    }
}
