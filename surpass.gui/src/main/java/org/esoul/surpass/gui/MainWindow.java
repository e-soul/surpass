/*
   Copyright 2017-2020 e-soul.org
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

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.InvalidPasswordException;
import org.esoul.surpass.app.NoUnsavedDataExistsException;
import org.esoul.surpass.app.ServiceUnavailableException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.app.SessionFactory;
import org.esoul.surpass.gui.table.SimpleTableModel;
import org.esoul.surpass.gui.table.TextAreaTableCellEditor;
import org.esoul.surpass.gui.table.TextAreaTableCellRenderer;
import org.esoul.surpass.table.api.EmptySequenceException;
import org.esoul.surpass.table.api.MaxSizeExceededException;

/**
 * All GUI component creation, setup and policies are encapsulated here. This is the ultimate detail. Literals are intentionally not externalized to help with readability.
 * 
 * @author mgp
 *
 */
public final class MainWindow {

    private static final Logger logger = System.getLogger(MainWindow.class.getSimpleName());

    private static final long DEFAULT_CLIPBOARD_EXPIRE_DELAY = 30L;

    private static final String MENU_ITEM_LBL_LOAD = "Load secrets";

    private static final String MENU_ITEM_LBL_STORE = "Store secrets";

    private static final String MENU_ITEM_LBL_EXIT = "Exit";

    private static final String BTN_LBL_ADD = "Add";

    private Session session = null;

    private Components components = new Components();

    private MainWindow() {
        // no instances except via createAndShow()
    }

    public static void createAndShow() {
        Session session = SessionFactory.create();
        try {
            session.start();
        } catch (ServiceUnavailableException | IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage(), "Critical error! Cannot start!", JOptionPane.ERROR_MESSAGE);
            return;
        }
        MainWindow mainWindow = new MainWindow();
        mainWindow.session = session;
        mainWindow.createFrame();
        mainWindow.createMenuBar();
        mainWindow.createInputPanel();
        mainWindow.createTable();
        mainWindow.createCommandPanel();
        mainWindow.createWindowAndTrayIcon();
        mainWindow.show();
    }

    private void createFrame() {
        components.frame = new JFrame("Surpass");
        components.frame.setLayout(new BoxLayout(components.frame.getContentPane(), BoxLayout.PAGE_AXIS));
    }

    private void createMenuBar() {
        JMenuItem loadMenuItem = new JMenuItem(MENU_ITEM_LBL_LOAD, KeyEvent.VK_L);
        loadMenuItem.addActionListener(this::loadData);

        JMenuItem storeMenuItem = new JMenuItem(MENU_ITEM_LBL_STORE, KeyEvent.VK_S);
        storeMenuItem.addActionListener(this::storeData);

        JMenuItem exitMenuItem = new JMenuItem(MENU_ITEM_LBL_EXIT, KeyEvent.VK_X);
        exitMenuItem.addActionListener(new ExitProgrammeHandler(session::unsavedDataExists, components));

        JMenu menu = new JMenu("Programme");
        menu.setMnemonic(KeyEvent.VK_P);
        menu.add(loadMenuItem);
        menu.add(storeMenuItem);
        menu.add(exitMenuItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menu);

        components.frame.setJMenuBar(menuBar);
    }

    private void createInputPanel() {
        JPanel inputPanel = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        JLabel identifierLabel = new JLabel("Identifier: ");
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.LINE_END;
        constraints.weightx = 0.2;
        constraints.weighty = 0.25;
        constraints.gridx = 0;
        constraints.gridy = 0;
        inputPanel.add(identifierLabel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 0.8;
        constraints.gridx = 1;
        constraints.gridy = 0;
        components.identifierTextField = new JTextField(50);
        inputPanel.add(components.identifierTextField, constraints);

        JLabel secretLabel = new JLabel("Secret: ");
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.LINE_END;
        constraints.weightx = 0.2;
        constraints.gridx = 0;
        constraints.gridy = 1;
        inputPanel.add(secretLabel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 0.8;
        constraints.gridx = 1;
        constraints.gridy = 1;
        components.secretPasswordField = new JPasswordField(50);
        inputPanel.add(components.secretPasswordField, constraints);

        JLabel noteLabel = new JLabel("Note: ");
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.FIRST_LINE_END;
        constraints.weightx = 0.2;
        constraints.gridx = 0;
        constraints.gridy = 2;
        inputPanel.add(noteLabel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 0.8;
        constraints.gridx = 1;
        constraints.gridy = 2;
        components.noteTextArea = new JTextArea(3, 50);
        inputPanel.add(new JScrollPane(components.noteTextArea), constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.LINE_END;
        constraints.weightx = 0.;
        constraints.gridx = 1;
        constraints.gridy = 3;
        components.addRowButton = new JButton(session.dataFileExist() ? "Load existing secrets then add" : BTN_LBL_ADD);
        components.addRowButton.addActionListener(this::addRow);
        inputPanel.add(components.addRowButton, constraints);

        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        components.frame.add(inputPanel);
    }

    private void addRow(ActionEvent event) {
        char[] password = components.secretPasswordField.getPassword();
        char[] identifier = components.identifierTextField.getText().toCharArray();
        char[] note = components.noteTextArea.getText().toCharArray();
        try {
            session.write(password, identifier, note);
            components.identifierTextField.setText("");
            components.noteTextArea.setText("");
            components.tableModel.fireTableDataChanged();
            components.addRowButton.setText(BTN_LBL_ADD);
        } catch (ExistingDataNotLoadedException e) {
            loadData(event);
            addRow(event);
        } catch (MaxSizeExceededException | EmptySequenceException e) {
            JOptionPane.showMessageDialog(components.frame, e.getMessage(), "Input error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Always clear the password. Clear the other fields only on success.
            components.secretPasswordField.setText("");
        }
    }

    private void createTable() {
        components.tableModel = new SimpleTableModel(session.getSecretTable());
        components.table = new JTable(components.tableModel);
        components.table.setPreferredScrollableViewportSize(new Dimension(500, 300));
        components.table.setFillsViewportHeight(true);
        components.table.setRowHeight(40);
        components.table.setCellSelectionEnabled(true);
        components.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        components.table.getSelectionModel().addListSelectionListener(this::tableSelectionChanged);
        components.table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnAdded(TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnMarginChanged(ChangeEvent e) {
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {
                tableSelectionChanged(e);
            }
        });

        setupNoteColumn();

        JScrollPane scrollPane = new JScrollPane(components.table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        components.frame.add(scrollPane);
    }

    private void tableSelectionChanged(ListSelectionEvent listSelectionEvent) {
        if (components.table.getSelectedColumn() == SimpleTableModel.IDENTIFIER_COLUMN_INDEX) {
            components.setEnabledTableButtons(true);
        } else {
            components.setEnabledTableButtons(false);
        }
    }

    private void setupNoteColumn() {
        TableColumn noteColumn = components.table.getColumn(SimpleTableModel.COLUMN_NAMES[SimpleTableModel.NOTE_COLUMN_INDEX]);
        noteColumn.setCellRenderer(new TextAreaTableCellRenderer());
        noteColumn.setCellEditor(new TextAreaTableCellEditor());
        noteColumn.setPreferredWidth(200);
    }

    private void createCommandPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        panel.add(Box.createHorizontalGlue());
        components.showSecretButton = new JButton("Show secret");
        components.showSecretButton.setEnabled(false);
        components.showSecretButton.addActionListener(this::showSecret);
        panel.add(components.showSecretButton);
        panel.add(Box.createRigidArea(new Dimension(5, 0)));
        components.editRowButton = new JButton("Edit");
        components.editRowButton.setEnabled(false);
        components.editRowButton.addActionListener(this::loadRowInFormForEdit);
        panel.add(components.editRowButton);
        panel.add(Box.createRigidArea(new Dimension(5, 0)));
        components.removeRowButton = new JButton("Remove");
        components.removeRowButton.setForeground(Color.RED);
        components.removeRowButton.setEnabled(false);
        components.removeRowButton.addActionListener(this::removeRow);
        panel.add(components.removeRowButton);
        components.frame.add(panel);
    }

    private void showSecret(ActionEvent actionEvent) {
        // This String object will not be added to the string pool.
        String secretStr = new String(session.getSecretTable().readSecret(components.table.getSelectedRow()), StandardCharsets.UTF_8);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(secretStr), null);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> clipboard.setContents(new StringSelection(""), null), DEFAULT_CLIPBOARD_EXPIRE_DELAY, TimeUnit.SECONDS);
        executor.shutdown();
        JOptionPane.showMessageDialog(components.frame, secretStr, "Secret copied to clipboard for " + DEFAULT_CLIPBOARD_EXPIRE_DELAY + "s.", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadRowInFormForEdit(ActionEvent actionEvent) {
        int row = components.table.getSelectedRow();
        session.setEditMode(row);
        components.identifierTextField.setText(new String(session.getSecretTable().readIdentifier(row)));
        components.noteTextArea.setText(new String(session.getSecretTable().readNote(row)));
        components.addRowButton.setText("Update");
    }

    private void removeRow(ActionEvent event) {
        int selectedOption = JOptionPane.showConfirmDialog(components.frame, "Are you sure you want to remove this entry?", "Remove?", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (JOptionPane.YES_OPTION == selectedOption) {
            int row = components.table.getSelectedRow();
            session.remove(row);
            components.tableModel.fireTableRowsDeleted(row, row);
            components.setEnabledTableButtons(false);
        }
    }

    private void createWindowAndTrayIcon() {
        Image iconImage = new ImageIcon(getClass().getResource("/icon.png")).getImage();

        components.frame.setIconImage(iconImage);

        if (SystemTray.isSupported()) {
            try {
                components.trayIcon = new TrayIcon(iconImage);
                components.trayIcon.setImageAutoSize(true);

                MenuItem loadMenuItem = new MenuItem(MENU_ITEM_LBL_LOAD);
                loadMenuItem.addActionListener(this::loadData);

                MenuItem storeMenuItem = new MenuItem(MENU_ITEM_LBL_STORE);
                storeMenuItem.addActionListener(this::storeData);

                MenuItem exitMenuItem = new MenuItem(MENU_ITEM_LBL_EXIT);
                exitMenuItem.addActionListener(new ExitProgrammeHandler(session::unsavedDataExists, components));

                PopupMenu popupMenu = new PopupMenu("Surpass");
                popupMenu.add(loadMenuItem);
                popupMenu.add(storeMenuItem);
                popupMenu.add(exitMenuItem);

                components.trayIcon.setPopupMenu(popupMenu);
                components.trayIcon.addActionListener(actionEvent -> show());
                components.trayIcon.addMouseListener(new TrayMouseHandler(this::show));
                SystemTray.getSystemTray().add(components.trayIcon);
            } catch (AWTException e) {
                // do nothing
            }
        }
    }

    private void show() {
        components.frame.pack();
        components.frame.setVisible(true);
        components.frame.setState(JFrame.NORMAL);
    }

    private void loadData(ActionEvent event) {
        char[] password = Dialogs.showPasswordInputDialog(components.frame, "Enter Master Password");
        if (null != password) {
            try {
                session.loadData(password);
                components.tableModel.fireTableDataChanged();
                components.addRowButton.setText(BTN_LBL_ADD);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(components.frame, "Secrets cannot be loaded! " + e.getMessage(), "Load error", JOptionPane.ERROR_MESSAGE);
            } catch (GeneralSecurityException e) {
                JOptionPane.showMessageDialog(components.frame, "Secrets cannot be decrypted! " + e.getMessage(), "Decrypt error", JOptionPane.ERROR_MESSAGE);
            } catch (InvalidPasswordException e) {
                JOptionPane.showMessageDialog(components.frame, "Password is empty! Provide password and try again.", "Empty password error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void storeData(ActionEvent actionEvent) {
        if (components.isFormDirty()) {
            int selectedOption = JOptionPane.showConfirmDialog(components.frame,
                    "There's new data in the form, are you sure you want to store the current table before the new data is added?", "Store despite existing new data?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (JOptionPane.YES_OPTION != selectedOption) {
                return;
            }
        }
        char[] password = Dialogs.showPasswordInputDialog(components.frame, "Enter Master Password");
        if (null != password) {
            try {
                session.storeData(password);
            } catch (IOException e) {
                logger.log(Level.ERROR, () -> "Store secrets error!", e);
                JOptionPane.showMessageDialog(components.frame, "Secrets cannot be stored! " + e.getMessage(), "Store error", JOptionPane.ERROR_MESSAGE);
            } catch (GeneralSecurityException e) {
                logger.log(Level.ERROR, () -> "Encrypt secrets error!", e);
                JOptionPane.showMessageDialog(components.frame, "Secrets cannot be encrypted! " + e.getMessage(), "Encrypt error", JOptionPane.ERROR_MESSAGE);
            } catch (ExistingDataNotLoadedException e) {
                JOptionPane.showMessageDialog(components.frame, "Data file exists but is not loaded. Load the data file before you can store new changes.", "Store warning",
                        JOptionPane.WARNING_MESSAGE);
            } catch (NoUnsavedDataExistsException e) {
                JOptionPane.showMessageDialog(components.frame, "There is no unsaved data!", "Store notice", JOptionPane.INFORMATION_MESSAGE);
            } catch (InvalidPasswordException e) {
                JOptionPane.showMessageDialog(components.frame, "This password cannot be used to decrypt your secrets, therefore it cannot be used to encrypt them as well!",
                        "Invalid password", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}