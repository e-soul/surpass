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
package org.esoul.surpass.gui.jfx;

import java.awt.AWTException;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.SecretQuery;
import org.esoul.surpass.app.ServiceUnavailableException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.app.SessionFactory;
import org.esoul.surpass.gui.jfx.addupdatesec.AddUpdateSecretDialog;
import org.esoul.surpass.gui.jfx.dialog.Dialogs;
import org.esoul.surpass.gui.jfx.dialog.MessageDialog;
import org.esoul.surpass.gui.jfx.help.AboutDialog;
import org.esoul.surpass.gui.jfx.loadstore.LoadStoreDialog;
import org.esoul.surpass.gui.jfx.masterpass.ChangeMasterPassDialog;

/**
 * All GUI component creation, setup and policies are encapsulated here. This is the ultimate detail. Literals are
 * intentionally not externalized to help with readability.
 */
public final class MainWindow {

    private static final long DEFAULT_CLIPBOARD_EXPIRE_DELAY = 45L;

    private Session session;
    private Stage stage;

    private TableView<Integer> tableView;
    private ObservableList<Integer> rowData;
    private FilteredList<Integer> filteredData;

    private MenuItem editSecretMenuItem;
    private MenuItem removeSecretMenuItem;
    private Button showSecretButton;
    private Button editRowButton;
    private Button removeRowButton;

    private Label secretCountLabel;
    private ProgressBar operationProgressBar;

    private TrayIcon trayIcon;

    private MainWindow() {
        // no instances except via createAndShow()
    }

    public static void createAndShow(Stage primaryStage) {
        Session session = SessionFactory.create();
        try {
            session.start();
        } catch (ServiceUnavailableException | IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.setTitle("Critical error! Cannot start!");
            alert.showAndWait();
            return;
        }

        MainWindow mainWindow = new MainWindow();
        mainWindow.session = session;
        mainWindow.stage = primaryStage;

        Platform.setImplicitExit(false);

        primaryStage.setTitle("Surpass");
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            if (SystemTray.isSupported()) {
                primaryStage.hide();
            } else {
                mainWindow.confirmAndExit();
            }
        });

        VBox root = new VBox();

        MenuBar menuBar = mainWindow.createMenuBar();
        root.getChildren().add(menuBar);

        mainWindow.createTable();
        mainWindow.createFilterBar(root);

        VBox.setVgrow(mainWindow.tableView, Priority.ALWAYS);
        root.getChildren().add(mainWindow.tableView);

        HBox commandBox = mainWindow.createCommandPanel();
        root.getChildren().add(commandBox);

        Scene scene = new Scene(root, 700, 500);
        primaryStage.setScene(scene);

        mainWindow.setupWindowAndTrayIcon();
        primaryStage.show();
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(createProgramMenu(), createSecretsMenu(), createHelpMenu());
        return menuBar;
    }

    private Menu createProgramMenu() {
        MenuItem loadMenuItem = new MenuItem(Labels.MENU_ITEM_LOAD);
        loadMenuItem.setOnAction(_ -> loadData());

        MenuItem storeMenuItem = new MenuItem(Labels.MENU_ITEM_STORE);
        storeMenuItem.setOnAction(_ -> storeData());

        MenuItem changeMasterPassItem = new MenuItem(Labels.MENU_ITEM_CHANGE_MASTER_PASS);
        changeMasterPassItem.setOnAction(_ -> changeMasterPass());

        MenuItem exitMenuItem = new MenuItem(Labels.MENU_ITEM_EXIT);
        exitMenuItem.setOnAction(_ -> confirmAndExit());

        Menu programMenu = new Menu("Programme");
        programMenu.getItems().addAll(loadMenuItem, storeMenuItem, changeMasterPassItem, exitMenuItem);
        return programMenu;
    }

    private Menu createSecretsMenu() {
        MenuItem addSecretMenuItem = new MenuItem("Add");
        addSecretMenuItem.setOnAction(_ -> addSecret());

        editSecretMenuItem = new MenuItem("Edit");
        editSecretMenuItem.setDisable(true);
        editSecretMenuItem.setOnAction(_ -> loadRowInFormForEdit());

        removeSecretMenuItem = new MenuItem("Remove");
        removeSecretMenuItem.setDisable(true);
        removeSecretMenuItem.setStyle("-fx-text-fill: red;");
        removeSecretMenuItem.setOnAction(_ -> removeRow());

        Menu secretsMenu = new Menu("Secrets");
        secretsMenu.getItems().addAll(addSecretMenuItem, editSecretMenuItem, removeSecretMenuItem);
        return secretsMenu;
    }

    private Menu createHelpMenu() {
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(_ -> AboutDialog.createAndShow(stage));

        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().add(aboutItem);
        return helpMenu;
    }

    private void createFilterBar(VBox root) {
        Label filterLabel = new Label("Filter:");
        filterLabel.setPadding(new Insets(10, 10, 0, 10));

        TextField filterTextField = new TextField();
        filterTextField.setMaxHeight(26);
        filterTextField.textProperty().addListener((_, _, newValue) -> {
            filteredData.setPredicate(row -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                String identifier = new String(session.getSecretTable().readIdentifier(row));
                String note = new String(session.getSecretTable().readNote(row));
                return identifier.toLowerCase().contains(lowerCaseFilter)
                        || note.toLowerCase().contains(lowerCaseFilter);
            });
        });

        Button clearButton = createFixedButton("Clear", 85);
        clearButton.setOnAction(_ -> filterTextField.setText(""));

        HBox filterBox = new HBox(5, filterTextField, clearButton);
        filterBox.setPadding(new Insets(0, 10, 10, 10));
        HBox.setHgrow(filterTextField, Priority.ALWAYS);

        root.getChildren().addAll(filterLabel, filterBox);
    }

    @SuppressWarnings("unchecked")
    private void createTable() {
        rowData = FXCollections.observableArrayList();
        filteredData = new FilteredList<>(rowData, _ -> true);
        SortedList<Integer> sortedData = new SortedList<>(filteredData);

        tableView = new TableView<>(sortedData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableView.setPadding(new Insets(0, 10, 10, 10));

        TableColumn<Integer, String> identifierCol = new TableColumn<>("Identifier");
        identifierCol.setCellValueFactory(data ->
                new SimpleStringProperty(new String(session.getSecretTable().readIdentifier(data.getValue()))));
        identifierCol.setPrefWidth(250);

        TableColumn<Integer, String> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(data ->
                new SimpleStringProperty(new String(session.getSecretTable().readNote(data.getValue()))));
        noteCol.setPrefWidth(200);
        noteCol.setCellFactory(_ -> new TableCell<>() {
            private final Text text = new Text();

            {
                text.wrappingWidthProperty().bind(noteCol.widthProperty().subtract(10));
                setGraphic(text);
                setPrefHeight(USE_COMPUTED_SIZE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                text.setText(empty || item == null ? "" : item);
            }
        });

        tableView.getColumns().addAll(identifierCol, noteCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        tableView.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) ->
                setEnabledTableButtons(newValue != null));
    }

    private HBox createCommandPanel() {
        secretCountLabel = new Label();
        secretCountLabel.setPrefWidth(105);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        operationProgressBar = new ProgressBar(0);
        operationProgressBar.setPrefWidth(100);
        operationProgressBar.setMinWidth(80);
        operationProgressBar.setMaxWidth(160);

        Button addRowButton = createFixedButton("Add", 85);
        addRowButton.setOnAction(_ -> addSecret());

        showSecretButton = createFixedButton("Show", 85);
        showSecretButton.setDisable(true);
        showSecretButton.setOnAction(_ -> showSecret());

        editRowButton = createFixedButton("Edit", 85);
        editRowButton.setDisable(true);
        editRowButton.setOnAction(_ -> loadRowInFormForEdit());

        removeRowButton = createFixedButton("Remove", 85);
        removeRowButton.setStyle("-fx-text-fill: red;");
        removeRowButton.setDisable(true);
        removeRowButton.setOnAction(_ -> removeRow());

        HBox commandBox = new HBox(5, secretCountLabel, spacer, operationProgressBar,
                addRowButton, showSecretButton, editRowButton, removeRowButton);
        commandBox.setPadding(new Insets(0, 10, 10, 10));
        commandBox.setAlignment(Pos.CENTER_LEFT);
        return commandBox;
    }

    private void setEnabledTableButtons(boolean enabled) {
        editSecretMenuItem.setDisable(!enabled);
        removeSecretMenuItem.setDisable(!enabled);
        showSecretButton.setDisable(!enabled);
        editRowButton.setDisable(!enabled);
        removeRowButton.setDisable(!enabled);
    }

    private void addSecret() {
        try {
            session.checkDataLoaded();
            SecretQuery secretQuery = session.createQuery();
            AddUpdateSecretDialog.createAndShowAdd(stage, this::writeSecret, session::generateSecret,
                    secretQuery::getUniqueIdentifiers);
        } catch (ExistingDataNotLoadedException e) {
            MessageDialog.GENERIC_ERROR.show(stage, "Local secrets exist. Load them before adding new.");
        }
    }

    private void writeSecret(char[] secret, char[] identifier, char[] note) throws Exception {
        session.write(secret, identifier, note);
        refreshTable();
    }

    private void showSecret() {
        int row = getSelectedRow();
        if (row < 0) {
            return;
        }

        String secretStr = new String(session.getSecretTable().readSecret(row), StandardCharsets.UTF_8);
        byte[] secretHashValue = calculateHash(session.getSecretTable().readSecret(row));

        ClipboardContent content = new ClipboardContent();
        content.putString(secretStr);
        Clipboard.getSystemClipboard().setContent(content);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> Platform.runLater(() -> clearClipboard(secretHashValue)),
                DEFAULT_CLIPBOARD_EXPIRE_DELAY, TimeUnit.SECONDS);
        executor.shutdown();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("Secret copied to clipboard for " + DEFAULT_CLIPBOARD_EXPIRE_DELAY + "s.");
        alert.setHeaderText(null);
        alert.setContentText(secretStr);
        alert.showAndWait();
    }

    private void clearClipboard(byte[] secretHashValue) {
        String currentContent = Clipboard.getSystemClipboard().getString();
        if (currentContent != null && secretHashValue.length > 0) {
            byte[] contentsHashValue = calculateHash(currentContent.getBytes(StandardCharsets.UTF_8));
            if (!Arrays.equals(contentsHashValue, secretHashValue)) {
                return;
            }
        }
        ClipboardContent empty = new ClipboardContent();
        empty.putString("");
        Clipboard.getSystemClipboard().setContent(empty);
    }

    private byte[] calculateHash(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            return new byte[0];
        }
    }

    private void loadRowInFormForEdit() {
        int row = getSelectedRow();
        if (row < 0) {
            return;
        }
        session.setEditMode(row);
        SecretQuery secretQuery = session.createQuery();
        byte[] identifier = session.getSecretTable().readIdentifier(row);
        byte[] note = session.getSecretTable().readNote(row);
        AddUpdateSecretDialog.createAndShowUpdate(stage, this::writeSecret, session::generateSecret,
                secretQuery::getUniqueIdentifiers,
                new String(identifier, StandardCharsets.UTF_8),
                new String(note, StandardCharsets.UTF_8));
    }

    private void removeRow() {
        int row = getSelectedRow();
        if (row < 0) {
            return;
        }
        boolean confirmed = MessageDialog.showConfirmation(stage, "Remove?",
                "Are you sure you want to remove this entry?");
        if (confirmed) {
            session.remove(row);
            refreshTable();
            setEnabledTableButtons(false);
        }
    }

    private int getSelectedRow() {
        Integer selected = tableView.getSelectionModel().getSelectedItem();
        return selected != null ? selected : -1;
    }

    private void refreshTable() {
        rowData.clear();
        int rowCount = session.getSecretTable().getRowNumber();
        for (int i = 0; i < rowCount; i++) {
            rowData.add(i);
        }
        updateSecretCountLabel();
    }

    private void updateSecretCountLabel() {
        String text = session.getSecretTable().getRowNumber() + "/"
                + session.getSecretTable().getMaxRow() + " secrets";
        if (session.unsavedDataExists()) {
            text += " *";
        }
        secretCountLabel.setText(text);
    }

    private void setupWindowAndTrayIcon() {
        Image iconImage = new Image(getClass().getResourceAsStream("/icon.png"));
        stage.getIcons().add(iconImage);

        if (SystemTray.isSupported()) {
            try {
                java.awt.Image awtIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
                trayIcon = new TrayIcon(awtIcon);
                trayIcon.setImageAutoSize(true);

                java.awt.MenuItem loadMenuItem = new java.awt.MenuItem(Labels.MENU_ITEM_LOAD);
                loadMenuItem.addActionListener(_ -> Platform.runLater(this::loadData));

                java.awt.MenuItem storeMenuItem = new java.awt.MenuItem(Labels.MENU_ITEM_STORE);
                storeMenuItem.addActionListener(_ -> Platform.runLater(this::storeData));

                java.awt.MenuItem exitMenuItem = new java.awt.MenuItem(Labels.MENU_ITEM_EXIT);
                exitMenuItem.addActionListener(_ -> Platform.runLater(this::confirmAndExit));

                PopupMenu popupMenu = new PopupMenu("Surpass");
                popupMenu.add(loadMenuItem);
                popupMenu.add(storeMenuItem);
                popupMenu.add(exitMenuItem);

                trayIcon.setPopupMenu(popupMenu);
                trayIcon.addActionListener(_ -> Platform.runLater(this::showStage));
                trayIcon.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            Platform.runLater(MainWindow.this::showStage);
                        }
                    }
                });

                SystemTray.getSystemTray().add(trayIcon);
            } catch (AWTException e) {
                // do nothing
            }
        }
    }

    private void showStage() {
        stage.show();
        stage.setIconified(false);
        stage.toFront();
    }

    private void confirmAndExit() {
        if (!stage.isShowing()) {
            stage.show();
        }
        String message;
        String title;
        if (session.unsavedDataExists()) {
            message = "You have unsaved data.\nExiting will result in DATA LOSS! Are you sure you want to exit?";
            title = "Exit despite unsaved data?";
        } else {
            message = "Are you sure you want to exit?";
            title = "Exit";
        }
        boolean confirmed = MessageDialog.showConfirmation(stage, title, message);
        if (confirmed) {
            if (SystemTray.isSupported() && trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
            }
            Platform.exit();
        }
    }

    private void loadData() {
        String serviceId = LoadStoreDialog.showLoad(stage, session.getSupportedPersistenceServices());
        if (serviceId == null) {
            return;
        }
        char[] password = Dialogs.showPasswordInputDialog(stage, "Enter Master Password");
        if (password != null) {
            new LoadDataOperation(stage, operationProgressBar, session, password, serviceId, this::refreshTable)
                    .execute();
        }
    }

    private void storeData() {
        Collection<String> selectedServicesIds = LoadStoreDialog.showStore(stage,
                session.getSupportedPersistenceServices());
        if (selectedServicesIds == null || selectedServicesIds.isEmpty()) {
            return;
        }
        char[] password = Dialogs.showPasswordInputDialog(stage, "Enter Master Password");
        if (password != null) {
            new StoreDataOperation(stage, operationProgressBar, session, password, selectedServicesIds,
                    this::updateSecretCountLabel).execute();
        }
    }

    private void changeMasterPass() {
        if (session.unsavedDataExists()) {
            MessageDialog.SAVE_DATA_INFO.show(stage,
                    "You have unsaved data.\nSave your data before changing the Master Password.");
            return;
        }
        ChangeMasterPassDialog.createAndShow(stage, operationProgressBar, session);
    }

    private static Button createFixedButton(String text, double width) {
        Button button = new Button(text);
        button.setPrefWidth(width);
        button.setMinWidth(width);
        button.setMaxWidth(width);
        return button;
    }
}
