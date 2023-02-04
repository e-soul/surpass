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
package org.esoul.surpass.gui.loadstore;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class LoadStoreWindow {

    private static class ServiceRadioButton extends JRadioButton {

        private static final long serialVersionUID = 1L;

        final String serviceId;

        ServiceRadioButton(String serviceId, String displayName) {
            super(displayName);
            this.serviceId = serviceId;
        }
    }

    private static class ServiceCheckBox extends JCheckBox {

        private static final long serialVersionUID = 1L;

        final String serviceId;

        ServiceCheckBox(String serviceId, String displayName, boolean selected) {
            super(displayName, selected);
            this.serviceId = serviceId;
        }
    }

    public static String showLoad(JFrame parentFrame, Map<String, String> supportedPersistenceServices) {
        ButtonGroup buttonGroup = new ButtonGroup();
        Function<Map.Entry<String, String>, Component> toggleButtonFactory = e -> {
            ServiceRadioButton radioButton = new ServiceRadioButton(e.getKey(), e.getValue());
            buttonGroup.add(radioButton);
            radioButton.setSelected(true);
            return radioButton;
        };
        Function<Component[], String> resultFactory = components -> {
            for (var c : components) {
                if (((JRadioButton) c).isSelected()) {
                    return ((ServiceRadioButton) c).serviceId;
                }
            }
            return null;
        };
        return showDialog(parentFrame, supportedPersistenceServices, "Loading secrets", toggleButtonFactory, resultFactory);
    }

    public static Collection<String> showStore(JFrame parentFrame, Map<String, String> supportedPersistenceServices) {
        Function<Component[], Collection<String>> resultFactory = components -> Arrays.stream(components).filter(c -> ((JCheckBox) c).isSelected())
                .map(c -> ((ServiceCheckBox) c).serviceId).collect(Collectors.toSet());
        return showDialog(parentFrame, supportedPersistenceServices, "Storing secrets", e -> new ServiceCheckBox(e.getKey(), e.getValue(), true),
                resultFactory);
    }

    private static <T> T showDialog(JFrame parentFrame, Map<String, String> supportedPersistenceServices, String dialogTitle,
            Function<Map.Entry<String, String>, Component> toggleButtonFactory, Function<Component[], T> resultFactory) {
        JDialog persistenceDialog = new JDialog(parentFrame, dialogTitle, true);
        persistenceDialog.setLayout(new BoxLayout(persistenceDialog.getContentPane(), BoxLayout.PAGE_AXIS));
        Box servicesBox = new Box(BoxLayout.PAGE_AXIS);
        for (Map.Entry<String, String> e : supportedPersistenceServices.entrySet()) {
            Component c = toggleButtonFactory.apply(e);
            servicesBox.add(c);
        }
        JPanel servicesPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        servicesPanel.setBorder(BorderFactory.createTitledBorder("Supported persistence services"));
        servicesPanel.add(servicesBox);
        servicesPanel.setPreferredSize(new Dimension(200, 80));
        persistenceDialog.getContentPane().add(servicesPanel);
        JPanel commandPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(l -> persistenceDialog.setVisible(false));
        commandPanel.add(okButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(l -> {
            persistenceDialog.setVisible(false);
            servicesBox.removeAll();
        });
        commandPanel.add(cancelButton);
        persistenceDialog.getContentPane().add(commandPanel);

        persistenceDialog.pack();
        persistenceDialog.setLocationRelativeTo(parentFrame);
        persistenceDialog.setVisible(true);
        persistenceDialog.dispose();
        return resultFactory.apply(servicesBox.getComponents());
    }
}
