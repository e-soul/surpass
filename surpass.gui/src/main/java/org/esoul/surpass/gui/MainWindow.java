/*
   Copyright 2017-2024 e-soul.org
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.SecretQuery;
import org.esoul.surpass.app.ServiceUnavailableException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.app.SessionFactory;
import org.esoul.surpass.gui.addupdatesec.AddUpdateSecretWindow;
import org.esoul.surpass.gui.dialog.Dialogs;
import org.esoul.surpass.gui.dialog.MessageDialog;
import org.esoul.surpass.gui.help.AboutWindow;
import org.esoul.surpass.gui.loadstore.LoadStoreWindow;
import org.esoul.surpass.gui.masterpass.ChangeMasterPassPolicy;
import org.esoul.surpass.gui.masterpass.ChangeMasterPassWindow;
import org.esoul.surpass.gui.table.SimpleTableModel;
import org.esoul.surpass.gui.table.TextAreaTableCellEditor;
import org.esoul.surpass.gui.table.TextAreaTableCellRenderer;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.util.SystemInfo;

/**
 * All GUI component creation, setup and policies are encapsulated here. This is the ultimate detail. Literals are
 * intentionally not externalized to help with readability.
 * 
 * @author mgp
 */
public final class MainWindow {

    private static final long DEFAULT_CLIPBOARD_EXPIRE_DELAY = 45L;

    private Session session = null;

    private MainWindowComponents components = new MainWindowComponents();

    private MainWindow() {
        // no instances except via createAndShow()
    }

    public static void createAndShow() {
        setupLookAndFeel();

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
        mainWindow.createTable();
        mainWindow.createCommandPanel();
        mainWindow.createWindowAndTrayIcon();
        mainWindow.show();
    }

    private static void setupLookAndFeel() {
        String requestedLookAndFeel = System.getProperty("org.esoul.surpass.laf", "com.formdev.flatlaf.FlatDarkLaf");
        try {
            Class<?> lookAndFeelClass = Class.forName(requestedLookAndFeel);
            lookAndFeelClass.getMethod("setup").invoke(null);
            if (SystemInfo.isLinux) {
                JFrame.setDefaultLookAndFeelDecorated(true);
                JDialog.setDefaultLookAndFeelDecorated(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            FlatDarkLaf.setup();
            JOptionPane.showMessageDialog(null, requestedLookAndFeel + " cannot be used. Using default.", "Look and feel error!", JOptionPane.ERROR_MESSAGE);
        }
        UIManager.put("ProgressBar.background", "fade($ProgressBar.background, 100)");
    }

    private void createFrame() {
        components.frame = new JFrame("Surpass");
        components.frame.setLayout(new BoxLayout(components.frame.getContentPane(), BoxLayout.PAGE_AXIS));
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createProgramMenu());
        menuBar.add(createSecretsMenu());
        menuBar.add(createHelpMenu());

        components.frame.setJMenuBar(menuBar);
    }

    private JMenu createProgramMenu() {
        JMenuItem loadMenuItem = new JMenuItem(Labels.MENU_ITEM_LOAD, KeyEvent.VK_L);
        loadMenuItem.addActionListener(this::loadData);

        JMenuItem storeMenuItem = new JMenuItem(Labels.MENU_ITEM_STORE, KeyEvent.VK_S);
        storeMenuItem.addActionListener(this::storeData);

        JMenuItem changeMasterPassItem = new JMenuItem(Labels.MENU_ITEM_CHANGE_MASTER_PASS, KeyEvent.VK_C);
        changeMasterPassItem.addActionListener(this::changeMasterPass);

        JMenuItem exitMenuItem = new JMenuItem(Labels.MENU_ITEM_EXIT, KeyEvent.VK_X);
        exitMenuItem.addActionListener(new ExitProgrammeHandler(session::unsavedDataExists, components));

        JMenu programMenu = new JMenu("Programme");
        programMenu.setMnemonic(KeyEvent.VK_P);
        programMenu.add(loadMenuItem);
        programMenu.add(storeMenuItem);
        programMenu.add(changeMasterPassItem);
        programMenu.add(exitMenuItem);

        return programMenu;
    }

    private JMenu createSecretsMenu() {
        JMenuItem addSecretMenuItem = new JMenuItem("Add", KeyEvent.VK_A);
        addSecretMenuItem.addActionListener(this::addSecret);

        components.editSecretMenuItem = new JMenuItem("Edit", KeyEvent.VK_E);
        components.editSecretMenuItem.setEnabled(false);
        components.editSecretMenuItem.addActionListener(this::loadRowInFormForEdit);

        components.removeSecretMenuItem = new JMenuItem("Remove", KeyEvent.VK_R);
        components.removeSecretMenuItem.setEnabled(false);
        components.removeSecretMenuItem.setForeground(Color.RED);
        components.removeSecretMenuItem.addActionListener(this::removeRow);

        JMenu secretsMenu = new JMenu("Secrets");
        secretsMenu.setMnemonic(KeyEvent.VK_S);
        secretsMenu.add(addSecretMenuItem);
        secretsMenu.add(components.editSecretMenuItem);
        secretsMenu.add(components.removeSecretMenuItem);

        return secretsMenu;
    }

    private JMenu createHelpMenu() {
        JMenuItem aboutItem = new JMenuItem("About", KeyEvent.VK_S);
        aboutItem.addActionListener(l -> AboutWindow.createAndShow(components.frame));

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.add(aboutItem);
        return helpMenu;
    }

    private void addSecret(ActionEvent event) {
        try {
            session.checkDataLoaded();
            SecretQuery secretQuery = session.createQuery();
            AddUpdateSecretWindow.createAndShowAdd(components.frame, this::writeSecret, session::generateSecret, secretQuery::getUniqueIdentifiers);
        } catch (ExistingDataNotLoadedException e) {
            MessageDialog.GENERIC_ERROR.show(components.frame, "Local secrets exist. Load them before adding new.");
        }
    }

    private void writeSecret(char[] secret, char[] identifier, char[] note) throws Exception {
        session.write(secret, identifier, note);
        components.tableModel.fireTableDataChanged();
    }

    private void createTable() {
        components.tableModel = new SimpleTableModel(session.getSecretTable());
        components.tableModel.addTableModelListener(e -> {
            String secrets = components.tableModel.getRowCount() + "/" + session.getSecretTable().getMaxRow() + " secrets";
            components.secretCountLabel.setText(secrets);
        });
        TableRowSorter<AbstractTableModel> tableRowSorter = new TableRowSorter<>(components.tableModel);

        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
        components.frame.add(filterLabel);

        Box filterBox = new Box(BoxLayout.LINE_AXIS);
        filterBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterBox.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JTextField filterTextField = new JTextField();
        filterTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        filterTextField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                tableRowSorter.setRowFilter(RowFilter.regexFilter("(?iu:.*" + filterTextField.getText() + ".*)"));
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }
        });
        filterBox.add(filterTextField);

        filterBox.add(Layout.createHSpacer());

        JButton clearButton = Layout.createFixedSizeButton("Clear", 85);
        clearButton.addActionListener(l -> {
            filterTextField.setText("");
            tableRowSorter.setRowFilter(null);
        });
        filterBox.add(clearButton);
        components.frame.add(filterBox);

        components.table = new JTable(components.tableModel);
        components.table.setRowSorter(tableRowSorter);
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
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
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
        Box commandBox = new Box(BoxLayout.LINE_AXIS);
        commandBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        commandBox.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        components.secretCountLabel = new JLabel();
        components.secretCountLabel.setPreferredSize(new Dimension(105, 26));
        commandBox.add(components.secretCountLabel);

        commandBox.add(Box.createHorizontalGlue());

        components.operationProgressBar = new JProgressBar();
        components.operationProgressBar.setStringPainted(true);
        components.operationProgressBar.setString("");
        components.operationProgressBar.setIndeterminate(false);
        components.operationProgressBar.setBorderPainted(false);
        components.operationProgressBar.setMinimumSize(new Dimension(80, 26));
        components.operationProgressBar.setPreferredSize(new Dimension(100, 26));
        components.operationProgressBar.setMaximumSize(new Dimension(160, 26));
        commandBox.add(components.operationProgressBar);

        commandBox.add(Layout.createHSpacer());

        components.addRowButton = Layout.createFixedSizeButton("Add", 85);
        components.addRowButton.addActionListener(this::addSecret);
        commandBox.add(components.addRowButton);

        commandBox.add(Layout.createHSpacer());

        components.showSecretButton = Layout.createFixedSizeButton("Show", 85);
        components.showSecretButton.setEnabled(false);
        components.showSecretButton.addActionListener(this::showSecret);
        commandBox.add(components.showSecretButton);

        commandBox.add(Layout.createHSpacer());

        components.editRowButton = Layout.createFixedSizeButton("Edit", 85);
        components.editRowButton.setEnabled(false);
        components.editRowButton.addActionListener(this::loadRowInFormForEdit);
        commandBox.add(components.editRowButton);

        commandBox.add(Layout.createHSpacer());

        components.removeRowButton = Layout.createFixedSizeButton("Remove", 85);
        components.removeRowButton.setForeground(Color.RED);
        components.removeRowButton.setEnabled(false);
        components.removeRowButton.addActionListener(this::removeRow);
        commandBox.add(components.removeRowButton);
        components.frame.add(commandBox);
    }

    private void showSecret(ActionEvent actionEvent) {
        // This String object will not be added to the string pool.
        String secretStr = new String(session.getSecretTable().readSecret(getSelected()), StandardCharsets.UTF_8);
        byte[] secretHashValue = calculateHash(session.getSecretTable().readSecret(getSelected()));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(secretStr), null);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> clearClipboard(secretHashValue), DEFAULT_CLIPBOARD_EXPIRE_DELAY, TimeUnit.SECONDS);
        executor.shutdown();
        JOptionPane.showMessageDialog(components.frame, secretStr, "Secret copied to clipboard for " + DEFAULT_CLIPBOARD_EXPIRE_DELAY + "s.",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearClipboard(byte[] secretHashValue) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        try {
            Object value = contents.getTransferData(DataFlavor.stringFlavor);
            if (null != value && secretHashValue.length > 0) {
                byte[] contentsHashValue = calculateHash(value.toString().getBytes(StandardCharsets.UTF_8));
                if (!Arrays.equals(contentsHashValue, secretHashValue)) {
                    // Don't clear the clipboard if the content is different than the secret. The user may have already put something else
                    // in the clipboard already.
                    return;
                }
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            // Cannot verify the clipboard content, clear it just in case.
        }
        clipboard.setContents(new StringSelection(""), null);
    }

    private byte[] calculateHash(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            return new byte[0];
        }
    }

    private void loadRowInFormForEdit(ActionEvent actionEvent) {
        int row = getSelected();
        session.setEditMode(row);
        SecretQuery secretQuery = session.createQuery();
        byte[] identifier = session.getSecretTable().readIdentifier(row);
        byte[] note = session.getSecretTable().readNote(row);
        AddUpdateSecretWindow.createAndShowUpdate(components.frame, this::writeSecret, session::generateSecret, secretQuery::getUniqueIdentifiers,
                new String(identifier, StandardCharsets.UTF_8), new String(note, StandardCharsets.UTF_8));
    }

    private void removeRow(ActionEvent event) {
        int selectedOption = JOptionPane.showConfirmDialog(components.frame, "Are you sure you want to remove this entry?", "Remove?",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (JOptionPane.YES_OPTION == selectedOption) {
            int row = getSelected();
            session.remove(row);
            components.tableModel.fireTableRowsDeleted(row, row);
            components.setEnabledTableButtons(false);
        }
    }

    private int getSelected() {
        return components.table.convertRowIndexToModel(components.table.getSelectedRow());
    }

    private void createWindowAndTrayIcon() {
        Image iconImage = new ImageIcon(getClass().getResource("/icon.png")).getImage();

        components.frame.setIconImage(iconImage);

        if (SystemTray.isSupported()) {
            try {
                components.trayIcon = new TrayIcon(iconImage);
                components.trayIcon.setImageAutoSize(true);

                MenuItem loadMenuItem = new MenuItem(Labels.MENU_ITEM_LOAD);
                loadMenuItem.addActionListener(this::loadData);

                MenuItem storeMenuItem = new MenuItem(Labels.MENU_ITEM_STORE);
                storeMenuItem.addActionListener(this::storeData);

                MenuItem exitMenuItem = new MenuItem(Labels.MENU_ITEM_EXIT);
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
        String serviceId;
        serviceId = LoadStoreWindow.showLoad(components.frame, session.getSupportedPersistenceServices());
        if (null == serviceId) {
            return;
        }
        char[] password = Dialogs.showPasswordInputDialog(components.frame, "Enter Master Password");
        if (null != password) {
            new LoadDataOperation(session, components, password, serviceId).execute();
        }
    }

    private void storeData(ActionEvent actionEvent) {
        Collection<String> selectedServicesIds;
        selectedServicesIds = LoadStoreWindow.showStore(components.frame, session.getSupportedPersistenceServices());
        if (null == selectedServicesIds || selectedServicesIds.isEmpty()) {
            return;
        }
        char[] password = Dialogs.showPasswordInputDialog(components.frame, "Enter Master Password");
        if (null != password) {
            new StoreDataOperation(session, components, password, selectedServicesIds).execute();
        }
    }

    private void changeMasterPass(ActionEvent actionEvent) {
        if (session.unsavedDataExists()) {
            MessageDialog.SAVE_DATA_INFO.show(components.frame, "You have unsaved data.\nSave your data before changing the Master Password.");
            return;
        }
        ChangeMasterPassPolicy policy = new ChangeMasterPassPolicy(session);
        ChangeMasterPassWindow.createAndShow(components.frame, components.operationProgressBar, policy);
    }
}