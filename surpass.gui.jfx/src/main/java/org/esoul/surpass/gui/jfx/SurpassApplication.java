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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.esoul.surpass.app.Session;
import org.esoul.surpass.app.SessionFactory;

public final class SurpassApplication extends Application {

    private TrayIntegration trayIntegration;

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((_, error) -> Platform.runLater(() ->
                UiDialogs.error(stage, "Unexpected error", "Surpass encountered an unexpected error.", error)));

        Session session = SessionFactory.create();
        try {
            session.start();
        } catch (Exception error) {
            UiDialogs.error(stage, "Cannot start Surpass", "The required services could not be initialized.", error);
            Platform.exit();
            return;
        }

        boolean extendedWindow = WindowChrome.configure(stage);
        MainView mainView = new MainView(stage, session, getHostServices(), extendedWindow);
        Scene scene = new Scene(mainView.root(), 1060, 720);
        scene.getStylesheets().add(UiDialogs.stylesheet());
        WindowChrome.configure(scene);

        stage.setTitle("Surpass");
        stage.getIcons().add(SurpassIcon.createFx(64));
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.setOnCloseRequest(mainView::onCloseRequest);

        TrayIntegration.install(mainView::showWindow, mainView::loadFromTray,
                mainView::storeFromTray, mainView::exitFromTray).ifPresent(tray -> {
                    trayIntegration = tray;
                    Platform.setImplicitExit(false);
                    mainView.enableTray(tray::close, tray::notifyHidden);
                });
        stage.show();
    }

    @Override
    public void stop() {
        if (trayIntegration != null) {
            trayIntegration.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
