/*
   Copyright 2017-2023 e-soul.org
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

package org.esoul.surpass.gui.help;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Brings up an "about" dialog with basic information about this application such as release version and web page link.
 * 
 * @author mgp
 */
public class AboutWindow {

    /**
     * Shows the "about" dialog.
     * 
     * @param parentFrame The owner of this dialog.
     */
    public static void createAndShow(JFrame parentFrame) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        JLabel appName = createLabel("Surpass");
        appName.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        panel.add(appName);
        panel.add(createLabel("version 1.3"));
        JLabel homepage = createLabel("https://surpass.e-soul.org");

        Map<TextAttribute, Object> hyperlinkFontAttributes = Map.of(TextAttribute.FAMILY, Font.MONOSPACED, TextAttribute.UNDERLINE,
                TextAttribute.UNDERLINE_LOW_GRAY);
        homepage.setFont(new Font(hyperlinkFontAttributes));
        homepage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        homepage.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent event) {
                try {
                    Desktop.getDesktop().browse(new URI(homepage.getText()));
                } catch (IOException | URISyntaxException ex) {
                    // Ignore.
                }
            }
        });
        panel.add(homepage);
        panel.add(createLabel("\u00A9 2017-2024 e-soul.org"));
        JDialog aboutDialog = new JDialog(parentFrame, "About Surpass", true);
        aboutDialog.add(panel);
        aboutDialog.pack();
        aboutDialog.setLocationRelativeTo(parentFrame);
        aboutDialog.setVisible(true);
        aboutDialog.dispose();
    }

    private static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }
}
