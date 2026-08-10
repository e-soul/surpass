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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

/** Creates a resolution-independent application icon for JavaFX and the system tray. */
final class SurpassIcon {

    private SurpassIcon() {
    }

    static Image createFx(int size) {
        BufferedImage source = createAwt(size);
        WritableImage image = new WritableImage(size, size);
        image.getPixelWriter().setPixels(0, 0, size, size,
                javafx.scene.image.PixelFormat.getIntArgbInstance(),
                source.getRGB(0, 0, size, size, null, 0, size), 0, size);
        return image;
    }

    static BufferedImage createAwt(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            double unit = size / 64.0;
            var body = new RoundRectangle2D.Double(5 * unit, 25 * unit, 54 * unit, 34 * unit,
                    11 * unit, 11 * unit);
            graphics.setPaint(new GradientPaint(0, (float) (22 * unit), new Color(0x65, 0x7B, 0xFF),
                    size, size, new Color(0x35, 0x42, 0x9A)));
            graphics.fill(body);

            graphics.setColor(new Color(0x38, 0x4A, 0xA8));
            graphics.setStroke(new BasicStroke((float) (6 * unit), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            graphics.drawArc((int) (18 * unit), (int) (5 * unit), (int) (28 * unit),
                    (int) (39 * unit), 0, 180);

            graphics.setColor(Color.WHITE);
            graphics.setStroke(new BasicStroke((float) (2.8 * unit), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            for (int center : new int[] {17, 32, 47}) {
                drawAsterisk(graphics, center * unit, 42 * unit, 6 * unit);
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void drawAsterisk(Graphics2D graphics, double x, double y, double radius) {
        graphics.drawLine((int) (x - radius), (int) y, (int) (x + radius), (int) y);
        graphics.drawLine((int) x, (int) (y - radius), (int) x, (int) (y + radius));
        double diagonal = radius * .72;
        graphics.drawLine((int) (x - diagonal), (int) (y - diagonal),
                (int) (x + diagonal), (int) (y + diagonal));
        graphics.drawLine((int) (x - diagonal), (int) (y + diagonal),
                (int) (x + diagonal), (int) (y - diagonal));
    }
}
