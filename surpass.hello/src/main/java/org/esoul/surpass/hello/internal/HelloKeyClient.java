package org.esoul.surpass.hello.internal;

import org.esoul.surpass.hello.api.HelloCapability;
import org.esoul.surpass.hello.api.HelloPromptOwner;
import org.esoul.surpass.hello.api.UnlockException;

public interface HelloKeyClient {

    record Key(String name) {
        public Key {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Windows Hello key name is empty");
            }
        }
    }

    HelloCapability capability();

    HelloPromptOwner capturePromptOwner();

    Key createKey(HelloPromptOwner owner, byte[] vaultId) throws UnlockException;

    byte[] deriveKeyMaterial(HelloPromptOwner owner, String keyName, byte[] challenge)
            throws UnlockException;

    void deleteKey(String keyName) throws UnlockException;

    void cancel();
}
