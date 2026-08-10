/*
   Copyright 2017-2026 e-soul.org
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
package org.esoul.surpass.gui.jfx;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;

/**
 * Keeps the platform-specific AWT tray API at the edge of the JavaFX application.
 * Tray events arrive on AWT's event thread and are always handed to JavaFX.
 */
final class TrayIntegration implements AutoCloseable {

    private final SystemTray systemTray;
    private final TrayIcon trayIcon;
    private final AtomicBoolean hiddenNoticeShown = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    private TrayIntegration(SystemTray systemTray, TrayIcon trayIcon) {
        this.systemTray = systemTray;
        this.trayIcon = trayIcon;
    }

    static Optional<TrayIntegration> install(Runnable showWindow, Runnable load, Runnable store, Runnable exit) {
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
            return Optional.empty();
        }

        SystemTray systemTray = SystemTray.getSystemTray();
        int iconSize = Math.max(32, systemTray.getTrayIconSize().width);
        TrayIcon trayIcon = new TrayIcon(SurpassIcon.createAwt(iconSize), "Surpass password vault");
        trayIcon.setImageAutoSize(true);
        trayIcon.setPopupMenu(createMenu(showWindow, load, store, exit));
        trayIcon.addActionListener(_ -> runOnFxThread(showWindow));

        try {
            systemTray.add(trayIcon);
            return Optional.of(new TrayIntegration(systemTray, trayIcon));
        } catch (AWTException | SecurityException error) {
            return Optional.empty();
        }
    }

    void notifyHidden() {
        if (hiddenNoticeShown.compareAndSet(false, true)) {
            trayIcon.displayMessage("Surpass is still running",
                    "Use the tray icon to reopen your vault or exit Surpass.", TrayIcon.MessageType.INFO);
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            systemTray.remove(trayIcon);
        }
    }

    private static PopupMenu createMenu(Runnable showWindow, Runnable load, Runnable store, Runnable exit) {
        PopupMenu menu = new PopupMenu("Surpass");
        menu.add(item("Open Surpass", showWindow));
        menu.addSeparator();
        menu.add(item("Load secrets...", load));
        menu.add(item("Store secrets...", store));
        menu.addSeparator();
        menu.add(item("Exit", exit));
        return menu;
    }

    private static MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.addActionListener(_ -> runOnFxThread(action));
        return item;
    }

    private static void runOnFxThread(Runnable action) {
        Platform.runLater(action);
    }
}
