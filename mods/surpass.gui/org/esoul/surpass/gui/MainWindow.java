package org.esoul.surpass.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.security.GeneralSecurityException;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
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
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import org.esoul.surpass.core.DataTable;
import org.esoul.surpass.core.EmptySequenceException;
import org.esoul.surpass.core.MaxSizeExceededException;
import org.esoul.surpass.core.SimpleCipher;
import org.esoul.surpass.persist.api.PersistenceDefaults;
import org.esoul.surpass.persist.api.PersistenceService;

public final class MainWindow {

    private static final Logger logger = System.getLogger(MainWindow.class.getSimpleName());

    private DataTable dataTable = new DataTable();

    private SimpleCipher simpleCipher = new SimpleCipher();

    private Components components = new Components();

    private DataState state = new DataState();

    private AbstractTableModel tableModel = new SimpleTableModel(dataTable);

    private MainWindow() {
        // no instances except via createAndShow()
    }

    public static void createAndShow() {
        MainWindow mainWindow = new MainWindow();
        mainWindow.initState();
        mainWindow.createFrame();
        mainWindow.createMenuBar();
        mainWindow.createInputPanel();
        mainWindow.createTable();
        mainWindow.show();
    }

    private void initState() {
        consumePeristenceService(persistenceService -> {
            try {
                state.dataFileExist = persistenceService.exists(PersistenceDefaults.DEFAULT_SECRETS);
            } catch (IOException e) {
                logger.log(Level.ERROR, () -> "Check secrets file exists error!", e);
                JOptionPane.showMessageDialog(components.frame, "Cannot determine if secrets file exists! " + e.getMessage(), "Store error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void createFrame() {
        components.frame = new JFrame("Surpass");
        components.frame.setLayout(new BoxLayout(components.frame.getContentPane(), BoxLayout.PAGE_AXIS));
        components.frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        components.frame.addWindowListener(new WindowClosingHandler(state));
    }

    private void createMenuBar() {
        JMenuItem loadMenuItem = new JMenuItem("Load secrets", KeyEvent.VK_L);
        loadMenuItem.addActionListener(this::loadData);

        JMenuItem storeMenuItem = new JMenuItem("Store secrets", KeyEvent.VK_S);
        storeMenuItem.addActionListener(this::storeData);

        JMenuItem menuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        menuItem.addActionListener(new ExitProgrammeHandler(components.frame, state));

        JMenu menu = new JMenu("Programme");
        menu.setMnemonic(KeyEvent.VK_P);
        menu.add(loadMenuItem);
        menu.add(storeMenuItem);
        menu.add(menuItem);

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
        String addRowButtonLabel;
        if (state.dataFileExist) {
            addRowButtonLabel = "Load existing secrets then add";
        } else {
            addRowButtonLabel = "Add";
        }
        components.addRowButton = new JButton(addRowButtonLabel);
        components.addRowButton.addActionListener(this::addRow);
        inputPanel.add(components.addRowButton, constraints);

        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        components.frame.add(inputPanel);
    }

    private void addRow(ActionEvent event) {
        if (state.dataFileExist && !state.dataFileLoaded) {
            loadData(event);
        }
        try {
            if (state.dataFileExist && !state.dataFileLoaded) {
                return;
            }
            dataTable.createRow(components.secretPasswordField.getPassword(), components.identifierTextField.getText().toCharArray(),
                    components.noteTextArea.getText().toCharArray());

            components.identifierTextField.setText("");
            components.noteTextArea.setText("");
            tableModel.fireTableDataChanged();
            state.unsavedDataExist = true;
        } catch (MaxSizeExceededException | EmptySequenceException e) {
            logger.log(Level.ERROR, () -> "Create row error!", e);
            JOptionPane.showMessageDialog(components.frame, e.getMessage(), "Input error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Always clear the password. Clear the other fields only on success.
            components.secretPasswordField.setText("");
        }
    }

    private void createTable() {
        components.table = new JTable(tableModel);
        components.table.setPreferredScrollableViewportSize(new Dimension(500, 300));
        components.table.setFillsViewportHeight(true);

        TableColumn secretColumn = components.table.getColumn(SimpleTableModel.COLUMN_NAMES[SimpleTableModel.SECRET_COLUMN_INDEX]);
        secretColumn.setCellRenderer(new ButtonTableCellRenderer());
        secretColumn.setCellEditor(new ButtonTableCellEditor(dataTable));

        JScrollPane scrollPane = new JScrollPane(components.table);
        scrollPane.setBorder(new EmptyBorder(0, 10, 10, 10));
        components.frame.add(scrollPane);
    }

    private void show() {
        components.frame.pack();
        components.frame.setVisible(true);
    }

    private void loadData(ActionEvent actionEvent) {
        consumePeristenceService(persistenceService -> {
            try {
                byte[] cipherText = persistenceService.read(PersistenceDefaults.DEFAULT_SECRETS);
                char[] password = Dialogs.showPasswordInputDialog(components.frame, "Enter Master Password");
                if (null != password) {
                    byte[] clearText = simpleCipher.decrypt(password, cipherText);
                    dataTable.load(clearText);
                    state.dataFileLoaded = true;
                    tableModel.fireTableDataChanged();
                    components.addRowButton.setText("Add");
                }
            } catch (IOException e) {
                logger.log(Level.ERROR, () -> "Load secrets error!", e);
                JOptionPane.showMessageDialog(components.frame, "Secrets cannot be loaded! " + e.getMessage(), "Load error", JOptionPane.ERROR_MESSAGE);
            } catch (GeneralSecurityException e) {
                logger.log(Level.ERROR, () -> "Decrypt secrets error!", e);
                JOptionPane.showMessageDialog(components.frame, "Secrets cannot be decrypted! " + e.getMessage(), "Decrypt error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void storeData(ActionEvent actionEvent) {
        consumePeristenceService(persistenceService -> {
            try {
                char[] password = Dialogs.showPasswordInputDialog(components.frame, "Enter Master Password");
                if (null != password) {
                    byte[] clearText = dataTable.toOneDimension();
                    byte[] cipherText = simpleCipher.encrypt(password, clearText);
                    persistenceService.write(PersistenceDefaults.DEFAULT_SECRETS, cipherText);
                    state.unsavedDataExist = false;
                }
            } catch (IOException e) {
                logger.log(Level.ERROR, () -> "Store secrets error!", e);
                JOptionPane.showMessageDialog(components.frame, "Secrets cannot be stored! " + e.getMessage(), "Store error", JOptionPane.ERROR_MESSAGE);
            } catch (GeneralSecurityException e) {
                logger.log(Level.ERROR, () -> "Encrypt secrets error!", e);
                JOptionPane.showMessageDialog(components.frame, "Secrets cannot be encrypted! " + e.getMessage(), "Encrypt error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void consumePeristenceService(Consumer<PersistenceService> consumer) {
        ServiceLoader<PersistenceService> loader = ServiceLoader.load(PersistenceService.class);
        loader.findFirst().ifPresentOrElse(consumer,
                () -> JOptionPane.showMessageDialog(components.frame, "PersistenceService not available!", "Persistence error", JOptionPane.ERROR_MESSAGE));
    }
}