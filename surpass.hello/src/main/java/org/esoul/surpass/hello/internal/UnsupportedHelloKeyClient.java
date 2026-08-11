package org.esoul.surpass.hello.internal;

import org.esoul.surpass.hello.api.HelloCapability;
import org.esoul.surpass.hello.api.HelloPromptOwner;
import org.esoul.surpass.hello.api.UnlockException;

public final class UnsupportedHelloKeyClient implements HelloKeyClient {

    private final HelloCapability capability;

    public UnsupportedHelloKeyClient(HelloCapability capability) {
        this.capability = capability;
    }

    @Override
    public HelloCapability capability() {
        return capability;
    }

    @Override
    public HelloPromptOwner capturePromptOwner() {
        return HelloPromptOwner.none();
    }

    @Override
    public Key createKey(HelloPromptOwner owner, byte[] vaultId) throws UnlockException {
        throw unavailable();
    }

    @Override
    public byte[] deriveKeyMaterial(HelloPromptOwner owner, String keyName, byte[] challenge)
            throws UnlockException {
        throw unavailable();
    }

    @Override
    public void deleteKey(String keyName) throws UnlockException {
        throw unavailable();
    }

    @Override
    public void cancel() {
    }

    private UnlockException unavailable() {
        return new UnlockException(UnlockException.Reason.UNSUPPORTED,
                "Windows Hello is unavailable: " + capability);
    }
}
