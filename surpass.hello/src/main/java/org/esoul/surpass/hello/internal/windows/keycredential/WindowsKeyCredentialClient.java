package org.esoul.surpass.hello.internal.windows.keycredential;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

import org.esoul.surpass.hello.api.HelloCapability;
import org.esoul.surpass.hello.api.HelloPromptOwner;
import org.esoul.surpass.hello.api.UnlockException;
import org.esoul.surpass.hello.internal.HelloKeyClient;
import org.esoul.surpass.hello.internal.UnsupportedHelloKeyClient;

public final class WindowsKeyCredentialClient implements HelloKeyClient {

    private static final String KEY_PREFIX = "Surpass vault ";
    private static final int MAX_KEY_NAME_BYTES = 256;

    private final WinRtKeyCredentialLibrary library;
    private final SecureRandom random = new SecureRandom();

    private WindowsKeyCredentialClient(WinRtKeyCredentialLibrary library) {
        this.library = library;
    }

    public static HelloKeyClient create() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!os.contains("windows") || !(arch.equals("amd64") || arch.equals("x86_64"))) {
            return new UnsupportedHelloKeyClient(HelloCapability.UNSUPPORTED_OS);
        }
        if (!WindowsKeyCredentialClient.class.getModule().isNativeAccessEnabled()) {
            return new UnsupportedHelloKeyClient(HelloCapability.NATIVE_ACCESS_DISABLED);
        }
        try {
            WinRtKeyCredentialLibrary library = new WinRtKeyCredentialLibrary();
            if (!library.isSupported()) {
                return new UnsupportedHelloKeyClient(
                        HelloCapability.PLATFORM_AUTHENTICATOR_UNAVAILABLE);
            }
            return new WindowsKeyCredentialClient(library);
        } catch (IllegalCallerException e) {
            return new UnsupportedHelloKeyClient(HelloCapability.NATIVE_ACCESS_DISABLED);
        } catch (RuntimeException | ExceptionInInitializerError | UnsatisfiedLinkError e) {
            return new UnsupportedHelloKeyClient(HelloCapability.DLL_UNAVAILABLE);
        }
    }

    @Override
    public HelloCapability capability() {
        return HelloCapability.AVAILABLE;
    }

    @Override
    public HelloPromptOwner capturePromptOwner() {
        return HelloPromptOwner.none();
    }

    @Override
    public Key createKey(HelloPromptOwner owner, byte[] vaultId)
            throws UnlockException {
        if (vaultId == null || vaultId.length != 16) {
            throw new UnlockException(UnlockException.Reason.NATIVE_FAILURE,
                    "The vault identifier is invalid");
        }
        byte[] suffix = new byte[18];
        random.nextBytes(suffix);
        String name = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(suffix);
        Arrays.fill(suffix, (byte) 0);
        library.create(name);
        return new Key(name);
    }

    @Override
    public byte[] deriveKeyMaterial(HelloPromptOwner owner, String keyName, byte[] challenge)
            throws UnlockException {
        validateKeyName(keyName);
        if (challenge == null || challenge.length != 32) {
            throw new UnlockException(UnlockException.Reason.BINDING_MISMATCH,
                    "The Windows Hello challenge is invalid");
        }
        byte[] signature = library.sign(keyName, challenge);
        try {
            return MessageDigest.getInstance("SHA-256").digest(signature);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is required by the Java platform", e);
        } finally {
            Arrays.fill(signature, (byte) 0);
        }
    }

    @Override
    public void deleteKey(String keyName) throws UnlockException {
        validateKeyName(keyName);
        library.delete(keyName);
    }

    @Override
    public void cancel() {
        library.cancel();
    }

    private static void validateKeyName(String keyName) throws UnlockException {
        if (keyName == null || !keyName.startsWith(KEY_PREFIX)) {
            throw new UnlockException(UnlockException.Reason.BINDING_MISMATCH,
                    "The Windows Hello key identifier is invalid");
        }
        byte[] encoded = keyName.getBytes(StandardCharsets.UTF_8);
        try {
            if (encoded.length == 0 || encoded.length > MAX_KEY_NAME_BYTES) {
                throw new UnlockException(UnlockException.Reason.BINDING_MISMATCH,
                        "The Windows Hello key identifier is invalid");
            }
        } finally {
            Arrays.fill(encoded, (byte) 0);
        }
    }
}
