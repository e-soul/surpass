package org.esoul.surpass.hello.api;

/** A recoverable or terminal vault-unlock failure with a stable reason. */
public class UnlockException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum Reason {
        CANCELED,
        TIMED_OUT,
        NOT_ENROLLED,
        CREDENTIAL_MISSING,
        INVALID_PASSWORD,
        CORRUPT_VAULT,
        BINDING_MISMATCH,
        UNSUPPORTED,
        NATIVE_FAILURE
    }

    private final Reason reason;

    public UnlockException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public UnlockException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
