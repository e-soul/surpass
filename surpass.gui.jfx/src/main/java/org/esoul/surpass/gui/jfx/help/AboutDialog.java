/*
   Copyright 2017-2025 e-soul.org
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
package org.esoul.surpass.gui.jfx.help;

import java.awt.Desktop;
import java.net.URI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Brings up an "about" dialog with basic information about this application such as release version and web page link.
 */
public class AboutDialog {

    private AboutDialog() {
        // no instances
    }

    public static void createAndShow(Window owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("About Surpass");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label nameLabel = new Label("Surpass");
        nameLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 20));

        Label versionLabel = new Label("version 1.5");

        Hyperlink homepage = new Hyperlink("https://surpass.e-soul.org");
        homepage.setFont(Font.font("Monospaced"));
        homepage.setOnAction(_ -> {
            try {
                Desktop.getDesktop().browse(new URI(homepage.getText()));
            } catch (Exception ex) {
                // Ignore.
            }
        });

        Label copyrightLabel = new Label("\u00A9 2017-2025 e-soul.org");

        content.getChildren().addAll(nameLabel, versionLabel, homepage, copyrightLabel);

        Scene scene = new Scene(content);
        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.showAndWait();
    }
}
