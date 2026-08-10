/*
   Copyright 2017-2026 e-soul.org
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.animation.PauseTransition;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.InvalidPasswordException;
import org.esoul.surpass.app.ServiceUnavailableException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.table.api.SecretTable;

final class MainView {

    private static final long CLIPBOARD_EXPIRY_SECONDS = 45;

    private final Stage stage;
    private final Session session;
    private final HostServices hostServices;
    private final boolean extendedWindow;
    private final BorderPane root = new BorderPane();
    private final ObservableList<SecretRow> rows = FXCollections.observableArrayList();
    private final FilteredList<SecretRow> filteredRows = new FilteredList<>(rows);
    private final TableView<SecretRow> table = new TableView<>(filteredRows);
    private final TextField search = new TextField();
    private final Label count = new Label();
    private final Label status = new Label("Ready");
    private final ProgressIndicator progress = new ProgressIndicator();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final Button addButton = actionButton("Add", "accent-button");
    private final Button showButton = actionButton("Copy secret", "quiet-button");
    private final Button editButton = actionButton("Edit", "quiet-button");
    private final Button removeButton = actionButton("Remove", "danger-button");
    private Runnable trayCleanup = () -> { };
    private Runnable trayHiddenNotification = () -> { };
    private boolean closeToTray;
    private boolean closeApproved;

    MainView(Stage stage, Session session, HostServices hostServices, boolean extendedWindow) {
        this.stage = stage;
        this.session = session;
        this.hostServices = hostServices;
        this.extendedWindow = extendedWindow;
        buildView();
        configureActions();
        refreshRows();
    }

    BorderPane root() {
        return root;
    }

    void enableTray(Runnable cleanup, Runnable hiddenNotification) {
        trayCleanup = cleanup;
        trayHiddenNotification = hiddenNotification;
        closeToTray = true;
    }

    void showWindow() {
        stage.show();
        stage.setIconified(false);
        stage.toFront();
        stage.requestFocus();
    }

    void loadFromTray() {
        showWindow();
        loadData();
    }

    void storeFromTray() {
        showWindow();
        storeData();
    }

    void exitFromTray() {
        showWindow();
        requestExit();
    }

    void onCloseRequest(WindowEvent event) {
        if (closeApproved) {
            return;
        }
        event.consume();
        if (closeToTray) {
            stage.hide();
            status.setText("Running in the notification area");
            trayHiddenNotification.run();
        } else {
            requestExit();
        }
    }

    private void buildView() {
        root.getStyleClass().add("app-shell");
        MenuBar menuBar = createMenuBar();
        Node windowHeader = extendedWindow ? WindowChrome.createApplicationHeader(menuBar) : menuBar;
        root.setTop(new VBox(windowHeader, createHero()));
        root.setCenter(createVaultCard());
        root.setBottom(createStatusBar());
        BorderPane.setMargin(root.getCenter(), new Insets(0, 28, 20, 28));
    }

    private MenuBar createMenuBar() {
        MenuItem load = menuItem("Load…", KeyCode.L, this::loadData);
        MenuItem store = menuItem("Store…", KeyCode.S, this::storeData);
        MenuItem changePassword = new MenuItem("Change master password…");
        changePassword.setOnAction(_ -> changeMasterPassword());
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(_ -> requestExit());

        MenuItem add = menuItem("Add secret…", KeyCode.N, this::addSecret);
        MenuItem edit = menuItem("Edit selected…", KeyCode.E, this::editSelected);
        MenuItem copy = menuItem("Copy selected secret", KeyCode.C, this::copySelectedSecret);
        MenuItem remove = new MenuItem("Remove selected…");
        remove.setOnAction(_ -> removeSelected());

        var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        load.disableProperty().bind(busy);
        store.disableProperty().bind(busy);
        changePassword.disableProperty().bind(busy);
        add.disableProperty().bind(busy);
        edit.disableProperty().bind(busy.or(noSelection));
        copy.disableProperty().bind(busy.or(noSelection));
        remove.disableProperty().bind(busy.or(noSelection));

        Menu programme = new Menu("Programme", null, load, store, new SeparatorMenuItem(), changePassword,
                new SeparatorMenuItem(), exit);
        Menu secrets = new Menu("Secrets", null, add, edit, copy, new SeparatorMenuItem(), remove);
        MenuItem about = new MenuItem("About Surpass");
        about.setOnAction(_ -> UiDialogs.about(stage, () -> hostServices.showDocument("https://surpass.e-soul.org")));
        Menu help = new Menu("Help", null, about);
        MenuBar menuBar = new MenuBar(programme, secrets, help);
        menuBar.getStyleClass().add("main-menu");
        return menuBar;
    }

    private Node createHero() {
        Label eyebrow = new Label("PRIVATE PASSWORD VAULT");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Your secrets, quietly protected.");
        title.getStyleClass().add("hero-title");
        Label subtitle = new Label("Search, update, and securely copy credentials from one calm workspace.");
        subtitle.getStyleClass().add("hero-subtitle");
        VBox copy = new VBox(7, eyebrow, title, subtitle);

        Button load = actionButton("Load vault", "quiet-button");
        load.setOnAction(this::loadData);
        Button store = actionButton("Store changes", "accent-button");
        store.setOnAction(this::storeData);
        load.disableProperty().bind(busy);
        store.disableProperty().bind(busy);
        HBox commands = new HBox(10, load, store);
        commands.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox hero = new HBox(24, copy, spacer, commands);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(28, 32, 24, 32));
        hero.getStyleClass().add("hero");
        return hero;
    }

    private Node createVaultCard() {
        search.setPromptText("Filter by identifier or note");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((_, _, value) -> applyFilter(value));
        HBox.setHgrow(search, Priority.ALWAYS);

        Button clear = actionButton("Clear", "quiet-button");
        clear.disableProperty().bind(search.textProperty().isEmpty());
        clear.setOnAction(_ -> search.clear());
        HBox filterBar = new HBox(10, search, clear);
        filterBar.setAlignment(Pos.CENTER);

        configureTable();

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        count.getStyleClass().add("count-label");
        HBox actions = new HBox(9, count, actionSpacer, addButton, showButton, editButton, removeButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, filterBar, table, actions);
        VBox.setVgrow(table, Priority.ALWAYS);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("vault-card");
        return card;
    }

    private void configureTable() {
        TableColumn<SecretRow, String> identifier = new TableColumn<>("Identifier");
        identifier.setCellValueFactory(data -> Bindings.createStringBinding(data.getValue()::identifier));
        identifier.setMinWidth(220);
        identifier.setPrefWidth(330);

        TableColumn<SecretRow, String> note = new TableColumn<>("Note");
        note.setCellValueFactory(data -> Bindings.createStringBinding(data.getValue()::note));
        note.setMinWidth(320);
        note.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                setWrapText(true);
            }
        });

        table.getColumns().add(identifier);
        table.getColumns().add(note);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(58);
        table.getSelectionModel().setCellSelectionEnabled(false);
        table.setPlaceholder(emptyVaultPlaceholder());
        table.setRowFactory(_ -> {
            TableRow<SecretRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    editSelected();
                }
            });
            return row;
        });
    }

    private Node emptyVaultPlaceholder() {
        Label icon = new Label("◇");
        icon.getStyleClass().add("empty-icon");
        Label title = new Label("Your vault is ready");
        title.getStyleClass().add("empty-title");
        Label hint = new Label("Load an existing vault or add your first secret.");
        hint.getStyleClass().add("empty-hint");
        VBox placeholder = new VBox(8, icon, title, hint);
        placeholder.setAlignment(Pos.CENTER);
        return placeholder;
    }

    private Node createStatusBar() {
        progress.setMaxSize(18, 18);
        progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progress.visibleProperty().bind(busy);
        progress.managedProperty().bind(progress.visibleProperty());
        status.getStyleClass().add("status-message");
        HBox bar = new HBox(9, progress, status);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 32, 18, 32));
        return bar;
    }

    private void configureActions() {
        var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        addButton.disableProperty().bind(busy);
        showButton.disableProperty().bind(busy.or(noSelection));
        editButton.disableProperty().bind(busy.or(noSelection));
        removeButton.disableProperty().bind(busy.or(noSelection));
        addButton.setOnAction(this::addSecret);
        showButton.setOnAction(this::copySelectedSecret);
        editButton.setOnAction(this::editSelected);
        removeButton.setOnAction(_ -> removeSelected());
    }

    private void applyFilter(String value) {
        String needle = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        filteredRows.setPredicate(row -> needle.isEmpty()
                || row.identifier().toLowerCase(Locale.ROOT).contains(needle)
                || row.note().toLowerCase(Locale.ROOT).contains(needle));
    }

    private void refreshRows() {
        SecretTable secretTable = session.getSecretTable();
        rows.clear();
        for (int index = 0; index < secretTable.getRowNumber(); index++) {
            rows.add(new SecretRow(index,
                    new String(secretTable.readIdentifier(index), StandardCharsets.UTF_8),
                    new String(secretTable.readNote(index), StandardCharsets.UTF_8)));
        }
        String suffix = session.unsavedDataExists() ? "  •  unsaved changes" : "";
        count.setText(rows.size() + " / " + secretTable.getMaxRow() + " secrets" + suffix);
        table.getSelectionModel().clearSelection();
    }

    private void addSecret(ActionEvent event) {
        try {
            session.checkDataLoaded();
            boolean saved = SecretEditorDialog.showAdd(stage, session.createQuery().getUniqueIdentifiers(), session::generateSecret,
                    session::write);
            if (saved) {
                refreshRows();
                status.setText("Secret added");
            }
        } catch (ExistingDataNotLoadedException error) {
            UiDialogs.warning(stage, "Load the existing vault first", "Local secrets exist and must be loaded before adding a new entry.");
        }
    }

    private void editSelected(ActionEvent event) {
        editSelected();
    }

    private void editSelected() {
        SecretRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        session.setEditMode(selected.sourceIndex());
        boolean saved = SecretEditorDialog.showEdit(stage, selected.identifier(), selected.note(),
                session.createQuery().getUniqueIdentifiers(), session::generateSecret, session::write);
        if (!saved) {
            session.setEditMode(-1);
            return;
        }
        refreshRows();
        status.setText("Secret updated");
    }

    private void removeSelected() {
        SecretRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !UiDialogs.confirm(stage, "Remove secret", "Remove “" + selected.identifier() + "”?",
                "Remove", true)) {
            return;
        }
        session.remove(selected.sourceIndex());
        refreshRows();
        status.setText("Secret removed");
    }

    private void copySelectedSecret(ActionEvent event) {
        SecretRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        byte[] secretBytes = session.getSecretTable().readSecret(selected.sourceIndex());
        String secret = new String(secretBytes, StandardCharsets.UTF_8);
        byte[] expectedHash = hash(secretBytes);
        Arrays.fill(secretBytes, (byte) 0);

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(secret);
        clipboard.setContent(content);

        PauseTransition expiry = new PauseTransition(Duration.seconds(CLIPBOARD_EXPIRY_SECONDS));
        expiry.setOnFinished(_ -> {
            if (clipboard.hasString() && MessageDigest.isEqual(expectedHash,
                    hash(clipboard.getString().getBytes(StandardCharsets.UTF_8)))) {
                clipboard.clear();
            }
        });
        expiry.play();
        UiDialogs.secretCopied(stage, secret, CLIPBOARD_EXPIRY_SECONDS);
        status.setText("Secret copied temporarily");
    }

    private void loadData(ActionEvent event) {
        loadData();
    }

    private void loadData() {
        if (busy.get()) {
            return;
        }
        Optional<String> service = UiDialogs.loadService(stage, session.getSupportedPersistenceServices());
        if (service.isEmpty()) {
            return;
        }
        UiDialogs.password(stage, "Load secrets", "Enter the master password for this vault").ifPresent(password ->
                runOperation("Loading encrypted vault…", "Vault loaded",
                        () -> session.loadData(password, service.get()), this::describeLoadError,
                        () -> Arrays.fill(password, '\0')));
    }

    private void storeData(ActionEvent event) {
        storeData();
    }

    private void storeData() {
        if (busy.get()) {
            return;
        }
        Optional<Collection<String>> services = UiDialogs.storeServices(stage, session.getSupportedPersistenceServices());
        if (services.isEmpty()) {
            return;
        }
        UiDialogs.password(stage, "Store secrets", "Enter the master password used to encrypt this vault").ifPresent(password ->
                runOperation("Encrypting and storing vault…", "Vault stored securely",
                        () -> session.storeData(password, services.get()), this::describeStoreError,
                        () -> Arrays.fill(password, '\0')));
    }

    private void changeMasterPassword() {
        if (session.unsavedDataExists()) {
            UiDialogs.warning(stage, "Store your changes first",
                    "The master password can only be changed after all pending vault changes have been stored.");
            return;
        }
        UiDialogs.masterPassword(stage, session.getSupportedPersistenceServices()).ifPresent(change -> {
            if (Arrays.equals(change.currentPassword(), change.newPassword())) {
                change.clear();
                status.setText("Master password is unchanged");
                return;
            }
            runOperation("Changing master password…", "Master password changed",
                    () -> session.changeMasterPassAndStoreData(change.currentPassword(), change.newPassword(), change.serviceIds()),
                    this::describePasswordChangeError, change::clear);
        });
    }

    private void runOperation(String runningMessage, String successMessage, ThrowingRunnable operation,
            Consumer<Throwable> failureHandler, Runnable cleanup) {
        busy.set(true);
        status.setText(runningMessage);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                operation.run();
                return null;
            }
        };
        task.setOnSucceeded(_ -> {
            cleanup.run();
            refreshRows();
            busy.set(false);
            status.setText(successMessage);
        });
        task.setOnFailed(_ -> {
            cleanup.run();
            busy.set(false);
            status.setText("Operation failed");
            failureHandler.accept(task.getException());
        });
        Thread.ofVirtual().name("surpass-operation").start(task);
    }

    private void describeLoadError(Throwable error) {
        if (error instanceof InvalidPasswordException) {
            UiDialogs.error(stage, "Cannot load vault", "The master password is empty or invalid.", error);
        } else if (error instanceof GeneralSecurityException) {
            UiDialogs.error(stage, "Cannot decrypt vault", "The master password may be incorrect.", error);
        } else if (error instanceof IOException || error instanceof ServiceUnavailableException) {
            UiDialogs.error(stage, "Cannot load vault", "The selected persistence service could not load the secrets.", error);
        } else {
            UiDialogs.error(stage, "Cannot load vault", "An unexpected error occurred while loading secrets.", error);
        }
    }

    private void describeStoreError(Throwable error) {
        if (error instanceof ExistingDataNotLoadedException) {
            UiDialogs.error(stage, "Cannot store vault", "Load the existing data before storing new changes.", error);
        } else if (error instanceof InvalidPasswordException) {
            UiDialogs.error(stage, "Cannot store vault", "This password cannot decrypt the current vault.", error);
        } else if (error instanceof GeneralSecurityException || error instanceof IOException) {
            UiDialogs.error(stage, "Cannot store vault", "The vault could not be encrypted or stored.", error);
        } else {
            UiDialogs.error(stage, "Cannot store vault", "An unexpected error occurred while storing secrets.", error);
        }
    }

    private void describePasswordChangeError(Throwable error) {
        if (error instanceof InvalidPasswordException) {
            UiDialogs.error(stage, "Cannot change password", "The current master password is incorrect.", error);
        } else if (error instanceof ExistingDataNotLoadedException) {
            UiDialogs.error(stage, "Cannot change password", "Load the existing vault first.", error);
        } else {
            UiDialogs.error(stage, "Cannot change password", "The vault could not be re-encrypted.", error);
        }
    }

    private void requestExit() {
        String header = session.unsavedDataExists()
                ? "You have unsaved changes"
                : "Close Surpass?";
        String action = session.unsavedDataExists() ? "Exit without storing" : "Exit";
        if (UiDialogs.confirm(stage, "Exit Surpass", header, action, session.unsavedDataExists())) {
            closeApproved = true;
            closeToTray = false;
            trayCleanup.run();
            Platform.setImplicitExit(true);
            stage.close();
            Platform.exit();
        }
    }

    private static byte[] hash(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException error) {
            return new byte[0];
        }
    }

    private static Button actionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private static MenuItem menuItem(String text, KeyCode key, Consumer<ActionEvent> action) {
        MenuItem item = new MenuItem(text);
        item.setAccelerator(new KeyCodeCombination(key, KeyCombination.SHORTCUT_DOWN));
        item.setOnAction(action::accept);
        return item;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
