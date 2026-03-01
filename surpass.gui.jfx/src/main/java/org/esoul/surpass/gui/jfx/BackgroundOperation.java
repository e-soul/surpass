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
package org.esoul.surpass.gui.jfx;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import javafx.stage.Window;

import org.esoul.surpass.gui.jfx.dialog.MessageDialog;

/**
 * Base class for background operations that update a progress bar while executing. Subclasses implement
 * {@link #operation()} which runs on a background thread and returns a {@link Runnable} to execute on the
 * JavaFX Application Thread upon completion (typically to display an error message, or a no-op on success).
 */
public abstract class BackgroundOperation {

    private static final Logger logger = System.getLogger(BackgroundOperation.class.getSimpleName());

    protected final Window owner;
    private final ProgressBar progressBar;

    protected BackgroundOperation(Window owner, ProgressBar progressBar) {
        this.owner = owner;
        this.progressBar = progressBar;
    }

    public void execute() {
        Task<Runnable> task = new Task<>() {
            @Override
            protected Runnable call() {
                try {
                    return operation();
                } catch (RuntimeException e) {
                    logger.log(Level.ERROR, () -> "Unexpected error!", e);
                    return () -> MessageDialog.UNEXPECTED_ERROR.show(owner, e.getMessage());
                }
            }
        };

        progressBar.setProgress(-1);

        task.setOnSucceeded(_ -> {
            progressBar.setProgress(0);
            task.getValue().run();
            doneSuccess();
        });

        task.setOnFailed(_ -> {
            progressBar.setProgress(0);
            Throwable ex = task.getException();
            if (ex != null) {
                logger.log(Level.ERROR, () -> "Background operation error!", ex);
            }
        });

        Thread thread = new Thread(task, "surpass-background-operation");
        thread.setDaemon(true);
        thread.start();
    }

    protected abstract Runnable operation();

    protected void doneSuccess() {
        // hook for subclasses
    }
}
