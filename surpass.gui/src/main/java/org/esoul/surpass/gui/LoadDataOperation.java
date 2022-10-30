package org.esoul.surpass.gui;

import java.awt.Component;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.function.Consumer;

import org.esoul.surpass.app.InvalidPasswordException;
import org.esoul.surpass.app.ServiceUnavailableException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.gui.dialog.MessageDialog;

class LoadDataOperation extends BaseDataOperationWorker {

	private String serviceId;

	LoadDataOperation(Session session, Components components, char[] password, String serviceId) {
		super(session, components, password);
		this.serviceId = serviceId;
	}

	@Override
	protected Consumer<Component> operation() {
        try {
            session.loadData(password, serviceId);
        } catch (IOException | ServiceUnavailableException e) {
        	return parent -> MessageDialog.LOAD_ERROR.show(parent, "Secrets cannot be loaded! " + e.getMessage());
        } catch (GeneralSecurityException e) {
        	return parent -> MessageDialog.DECRYPT_ERROR.show(parent, "Secrets cannot be decrypted! " + e.getMessage());
        } catch (InvalidPasswordException e) {
        	return parent -> MessageDialog.EMPTY_PASS_ERROR.show(parent, "Password is empty! Provide password and try again.");
        }
		return msg -> {};
	}

	@Override
	protected void doneSuccess() {
		components.tableModel.fireTableDataChanged();
        components.addRowButton.setText(Labels.BTN_ADD);
	}
}
