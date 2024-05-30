/*
   Copyright 2017-2024 e-soul.org
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
package org.esoul.surpass.gui;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

public final class Layout {

    private static final int DEFAULT_HEIGHT = 26;

    private Layout() {
        // no instances
    }

    public static JDialog createDialogFrame(JFrame parentFrame, String title) {
        JDialog frame = new JDialog(parentFrame, title, true);
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));
        frame.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return frame;
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

    public static Component createVSpacer(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }
}
