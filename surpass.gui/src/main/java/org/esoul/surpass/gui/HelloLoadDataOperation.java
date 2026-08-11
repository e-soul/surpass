package org.esoul.surpass.gui;

import java.awt.Component;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.function.Consumer;

import org.esoul.surpass.app.Session;
import org.esoul.surpass.gui.dialog.MessageDialog;
import org.esoul.surpass.hello.api.HelloPromptOwner;
import org.esoul.surpass.hello.api.UnlockException;

final class HelloLoadDataOperation extends BaseDataOperationWorker {

    private final Session session;
    private final MainWindowComponents components;
    private final HelloPromptOwner promptOwner;
    private final String serviceId;

    HelloLoadDataOperation(Session session, MainWindowComponents components,
            HelloPromptOwner promptOwner, String serviceId) {
        super(components.frame, components.operationProgressBar);
        this.session = session;
        this.components = components;
        this.promptOwner = promptOwner;
        this.serviceId = serviceId;
    }

    @Override
    protected Consumer<Component> operation() {
        try {
            session.loadDataWithHello(promptOwner, serviceId);
            return _ -> {
            };
        } catch (UnlockException e) {
            if (e.reason() == UnlockException.Reason.CANCELED) {
                return _ -> {
                };
            }
            String message = switch (e.reason()) {
                case NOT_ENROLLED -> "Windows Hello is not enrolled for this storage location.";
                case CREDENTIAL_MISSING -> "The Windows Hello key is missing. Use the master password.";
                case BINDING_MISMATCH -> "The local Windows Hello binding does not match this vault.";
                case CORRUPT_VAULT -> "The vault is damaged or has been modified.";
                case TIMED_OUT -> "Windows Hello timed out.";
                case UNSUPPORTED -> "Windows Hello is unavailable on this system.";
                default -> "Windows Hello could not unlock the vault: " + e.getMessage();
            };
            return parent -> MessageDialog.LOAD_ERROR.show(parent, message);
        } catch (IOException | GeneralSecurityException e) {
            return parent -> MessageDialog.LOAD_ERROR.show(parent,
                    "Secrets cannot be loaded: " + e.getMessage());
        }
    }

    @Override
    protected void doneSuccess() {
        components.tableModel.fireTableDataChanged();
        components.setVaultUnlocked(session.isUnlocked());
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            session.cancelHello();
        }
        super.done();
    }
}
