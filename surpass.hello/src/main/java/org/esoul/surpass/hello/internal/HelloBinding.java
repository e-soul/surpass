package org.esoul.surpass.hello.internal;

public record HelloBinding(String persistenceServiceId, byte[] vaultId, String keyName,
        byte[] challenge, byte[] nonce, byte[] wrappedBundle, long createdAt, long updatedAt) {

    public HelloBinding {
        if (persistenceServiceId == null || persistenceServiceId.isBlank()) {
            throw new IllegalArgumentException("Persistence service ID is empty");
        }
        if (vaultId == null || challenge == null || nonce == null || wrappedBundle == null) {
            throw new IllegalArgumentException("Windows Hello binding contains a null field");
        }
        vaultId = vaultId.clone();
        if (keyName == null || keyName.isBlank()) {
            throw new IllegalArgumentException("Windows Hello key name is empty");
        }
        challenge = challenge.clone();
        nonce = nonce.clone();
        wrappedBundle = wrappedBundle.clone();
    }
}
