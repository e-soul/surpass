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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.esoul.surpass.gui.dialog.MessageDialog;

public abstract class BaseDataOperationWorker extends SwingWorker<Consumer<Component>, Void> {

    private static final Logger logger = System.getLogger(BaseDataOperationWorker.class.getSimpleName());

    protected Component parent;
    protected JProgressBar operationProgressBar;

    public BaseDataOperationWorker(Component parent, JProgressBar operationProgressBar) {
        this.parent = parent;
        this.operationProgressBar = operationProgressBar;
    }

    @Override
    protected Consumer<Component> doInBackground() throws Exception {
        operationProgressBar.setString("Working...");
        operationProgressBar.setIndeterminate(true);
        try {
            return operation();
        } catch (RuntimeException e) {
            logger.log(Level.ERROR, () -> "Unexpected error!", e);
            return parent -> MessageDialog.UNEXPECTED_ERROR.show(parent, e.getMessage());
        }
    }

    protected abstract Consumer<Component> operation();

    @Override
    protected void done() {
        operationProgressBar.setString("");
        operationProgressBar.setIndeterminate(false);
        try {
            get().accept(parent);
            doneSuccess();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.log(Level.ERROR, () -> "Background operation error!", e);
        }
    }

    protected void doneSuccess() {
        // do nothing
    }
}
