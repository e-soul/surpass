package org.esoul.surpass.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TrayMouseHandler extends MouseAdapter {

    private Runnable showFrameRunnable = null;

    public TrayMouseHandler(Runnable showFrameRunnable) {
        this.showFrameRunnable = showFrameRunnable;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        if (e.getButton() == MouseEvent.BUTTON1) {
            showFrameRunnable.run();
        }
    }
}
