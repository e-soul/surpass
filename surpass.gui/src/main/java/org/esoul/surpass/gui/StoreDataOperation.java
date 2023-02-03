package org.esoul.surpass.gui;

import java.awt.Component;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import org.esoul.surpass.app.ExistingDataNotLoadedException;
import org.esoul.surpass.app.InvalidPasswordException;
import org.esoul.surpass.app.ServiceUnavailableException;
import org.esoul.surpass.app.Session;
import org.esoul.surpass.gui.dialog.MessageDialog;

class StoreDataOperation extends BaseDataOperationWorker {

	private Collection<String> selectedServicesIds;
	
	StoreDataOperation(Session session, MainWindowComponents components, char[] password, Collection<String> selectedServicesIds) {
		super(session, components, password);
		this.selectedServicesIds = new ArrayList<>(selectedServicesIds);
	}

	@Override
	protected Consumer<Component> operation() {
        try {
            session.storeData(password, selectedServicesIds);
        } catch (IOException | ServiceUnavailableException e) {
        	return parent -> MessageDialog.STORE_ERROR.show(parent, "Secrets cannot be stored! " + e.getMessage());
        } catch (GeneralSecurityException e) {
        	return parent -> MessageDialog.ENCRYPT_ERROR.show(parent, "Secrets cannot be encrypted! " + e.getMessage());
        } catch (ExistingDataNotLoadedException e) {
        	return parent -> MessageDialog.STORE_WARNING.show(parent, "Data file exists but is not loaded. Load the data file before you can store new changes.");
        } catch (InvalidPasswordException e) {
        	return parent -> MessageDialog.INVALID_PASS_ERROR.show(parent, "This password cannot be used to decrypt your secrets, therefore it cannot be used to encrypt them as well!");
        }
		return msg -> {};
	}
}
