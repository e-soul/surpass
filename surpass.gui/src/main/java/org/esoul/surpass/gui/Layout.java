package org.esoul.surpass.gui;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JButton;

public final class Layout {
	
	private static final int DEFAULT_HEIGHT = 26;

	private Layout() {
		// no instances
	}
	
	public static JButton createFixedSizeButton(String title, int width) {
		JButton button = new JButton(title);
        button.setMinimumSize(new Dimension(width, DEFAULT_HEIGHT));
        button.setPreferredSize(new Dimension(width, DEFAULT_HEIGHT));
        button.setMaximumSize(new Dimension(width, DEFAULT_HEIGHT));
        return button;
	}
	
	public static Component createHSpacer() {
		return Box.createRigidArea(new Dimension(5, 0));
	}
	
	public static Component createVSpacer() {
		return Box.createRigidArea(new Dimension(0, 5));
	}
}
