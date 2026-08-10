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

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Configures JavaFX's extended client-area window chrome while retaining native window controls and behavior.
 */
@SuppressWarnings("deprecation")
final class WindowChrome {

    private static final Color SCENE_FILL = Color.web("#11131C");
    private static final double HEADER_HEIGHT = 46;

    private WindowChrome() {
    }

    static boolean configure(Stage stage) {
        if (!isExtendedWindowSupported()) {
            return false;
        }
        stage.initStyle(StageStyle.EXTENDED);
        HeaderBar.setPrefButtonHeight(stage, HEADER_HEIGHT);
        return true;
    }

    static void configure(Scene scene) {
        scene.setFill(SCENE_FILL);
    }

    static HeaderBar createApplicationHeader(MenuBar menuBar) {
        Label brandMark = new Label("S");
        brandMark.getStyleClass().add("brand-mark");
        HeaderBar.setDragType(brandMark, HeaderDragType.DRAGGABLE);

        menuBar.getStyleClass().add("header-menu");
        HeaderBar.setDragType(menuBar, HeaderDragType.NONE);

        HBox left = new HBox(8, brandMark, menuBar);
        left.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Surpass");
        title.getStyleClass().add("window-title");
        HeaderBar.setDragType(title, HeaderDragType.DRAGGABLE);

        HeaderBar header = new HeaderBar(left, title, null);
        header.getStyleClass().add("app-header");
        HeaderBar.setMargin(left, new Insets(0, 8, 0, 12));
        return header;
    }

    static void configure(Dialog<?> dialog, String title) {
        if (!isExtendedWindowSupported()) {
            return;
        }
        dialog.initStyle(StageStyle.EXTENDED);

        Label windowTitle = new Label(title);
        windowTitle.getStyleClass().add("dialog-window-title");
        HeaderBar.setDragType(windowTitle, HeaderDragType.DRAGGABLE);

        HeaderBar header = new HeaderBar(null, windowTitle, null);
        header.getStyleClass().add("dialog-header");
        dialog.getDialogPane().setHeaderBar(header);
        dialog.getDialogPane().sceneProperty().addListener((_, _, scene) -> {
            if (scene != null) {
                configure(scene);
            }
        });
    }

    private static boolean isExtendedWindowSupported() {
        return Platform.isSupported(ConditionalFeature.EXTENDED_WINDOW);
    }
}
