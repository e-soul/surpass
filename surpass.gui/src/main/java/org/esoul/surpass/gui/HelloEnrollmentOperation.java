package org.esoul.surpass.gui;

import java.awt.Component;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.gui.dialog.MessageDialog;
import org.esoul.surpass.hello.api.HelloPromptOwner;
import org.esoul.surpass.hello.api.UnlockException;

final class HelloEnrollmentOperation extends BaseDataOperationWorker {

    private final Session session;
    private final HelloPromptOwner promptOwner;
    private final boolean remove;

    HelloEnrollmentOperation(Session session, MainWindowComponents components,
            HelloPromptOwner promptOwner, boolean remove) {
        super(components.frame, components.operationProgressBar);
        this.session = session;
        this.promptOwner = promptOwner;
        this.remove = remove;
    }

    @Override
    protected Consumer<Component> operation() {
        try {
            if (remove) {
                session.removeHello();
            } else {
                session.enrollHello(promptOwner);
            }
            return parent -> JOptionPane.showMessageDialog(parent,
                    remove ? "Windows Hello was removed." : "Windows Hello was enabled.",
                    "Windows Hello", JOptionPane.INFORMATION_MESSAGE);
        } catch (UnlockException e) {
            if (e.reason() == UnlockException.Reason.CANCELED) {
                return _ -> {
                };
            }
            return parent -> MessageDialog.GENERIC_ERROR.show(parent,
                    "Windows Hello could not be " + (remove ? "removed: " : "enabled: ")
                            + e.getMessage());
        } catch (ExistingDataNotLoadedException e) {
            return parent -> MessageDialog.GENERIC_ERROR.show(parent,
                    "Unlock the vault with the master password first.");
        } catch (IOException | GeneralSecurityException e) {
            return parent -> MessageDialog.GENERIC_ERROR.show(parent, e.getMessage());
        }
    }
}
