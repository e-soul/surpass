package org.esoul.surpass.gui;

import java.awt.Component;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

import org.esoul.surpass.app.Session;
import org.esoul.surpass.gui.dialog.MessageDialog;

abstract class BaseDataOperationWorker extends SwingWorker<Consumer<Component>, Void> {
	
	private static final Logger logger = System.getLogger(BaseDataOperationWorker.class.getSimpleName());

	protected Session session;
	protected MainWindowComponents components;
	protected char[] password;
	
	BaseDataOperationWorker(Session session, MainWindowComponents components, char[] password) {
		this.session = session;
		this.components = components;
		this.password = password;
	}
	
	@Override
	protected Consumer<Component> doInBackground() throws Exception {
        try {
            return operation();
        } catch (RuntimeException e) {
            logger.log(Level.ERROR, () -> "Unexpected error!", e);
            return parent -> MessageDialog.UNEXPECTED_ERROR.show(parent, e.getMessage());
        }
	}

	abstract Consumer<Component> operation();

	@Override
	protected void done() {
		components.operationProgressBar.setString("");
        components.operationProgressBar.setIndeterminate(false);
		try {
			get().accept(components.frame);
			doneSuccess();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			logger.log(Level.ERROR, () -> "Background operation error!", e);
		}
	}

	protected void doneSuccess() {
		// do nothing
	}
}
