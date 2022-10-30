package org.esoul.surpass.gui.dialog;

import java.awt.Component;

import javax.swing.JOptionPane;

public enum MessageDialog {

	NULL("No message", JOptionPane.PLAIN_MESSAGE) {
		
		@Override
		public void show(Component parentComponent, String message) {
			// do nothing
		}
	},
	UNEXPECTED_ERROR("Unexpected error!", JOptionPane.ERROR_MESSAGE),
	ENCRYPT_ERROR("Encrypt error", JOptionPane.ERROR_MESSAGE),
	DECRYPT_ERROR("Decrypt error", JOptionPane.ERROR_MESSAGE),
	LOAD_ERROR("Load error", JOptionPane.ERROR_MESSAGE),
	STORE_ERROR("Store error", JOptionPane.ERROR_MESSAGE),
	STORE_WARNING("Store warning", JOptionPane.WARNING_MESSAGE),
	EMPTY_PASS_ERROR("Empty password error", JOptionPane.ERROR_MESSAGE),
	INVALID_PASS_ERROR("Invalid password", JOptionPane.ERROR_MESSAGE);
	
	private String title;
	private int messageType;
	
	private MessageDialog(String title, int messageType) {
		this.title = title;
		this.messageType = messageType;
	}
	
	public void show(Component parentComponent, String message) {
		JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
	}
}
