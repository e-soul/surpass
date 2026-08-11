package org.esoul.surpass.hello.api;

/**
 * Opaque context for a Windows Hello prompt. Native handles never cross this
 * service boundary. The current KeyCredentialManager implementation lets Windows
 * own the prompt.
 */
public interface HelloPromptOwner {

    /** Returns an owner that lets Windows choose the prompt parent. */
    static HelloPromptOwner none() {
        return EmptyPromptOwner.INSTANCE;
    }

    final class EmptyPromptOwner implements HelloPromptOwner {
        private static final EmptyPromptOwner INSTANCE = new EmptyPromptOwner();

        private EmptyPromptOwner() {
        }
    }
}
