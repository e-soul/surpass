package org.esoul.surpass.hello.test;

import java.io.IOException;
import java.util.Arrays;

import org.esoul.surpass.hello.internal.BindingCodec;
import org.esoul.surpass.hello.internal.HelloBinding;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BindingCodecTest {

    @Test
    public void roundTripsKeyCredentialBindingVersionTwo() throws Exception {
        HelloBinding expected = binding("Surpass vault test-key", bytes(32, 0x31));

        HelloBinding actual = BindingCodec.decode(BindingCodec.encode(expected));

        Assertions.assertEquals(expected.persistenceServiceId(), actual.persistenceServiceId());
        Assertions.assertArrayEquals(expected.vaultId(), actual.vaultId());
        Assertions.assertEquals(expected.keyName(), actual.keyName());
        Assertions.assertArrayEquals(expected.challenge(), actual.challenge());
        Assertions.assertArrayEquals(expected.nonce(), actual.nonce());
        Assertions.assertArrayEquals(expected.wrappedBundle(), actual.wrappedBundle());
        Assertions.assertEquals(expected.createdAt(), actual.createdAt());
        Assertions.assertEquals(expected.updatedAt(), actual.updatedAt());
    }

    @Test
    public void rejectsUnknownAndTruncatedBindings() throws Exception {
        byte[] encoded = BindingCodec.encode(binding("Surpass vault test-key", bytes(32, 0x31)));
        byte[] oldVersion = encoded.clone();
        oldVersion[5] = 1;

        Assertions.assertThrows(IOException.class, () -> BindingCodec.decode(oldVersion));
        Assertions.assertThrows(IOException.class,
                () -> BindingCodec.decode(Arrays.copyOf(encoded, encoded.length - 1)));
    }

    @Test
    public void authenticatesKeyNameAndChallengeAsWrapperMetadata() throws Exception {
        HelloBinding original = binding("Surpass vault first-key", bytes(32, 0x31));
        HelloBinding changedKey = binding("Surpass vault second-key", bytes(32, 0x31));
        HelloBinding changedChallenge = binding("Surpass vault first-key", bytes(32, 0x32));

        Assertions.assertFalse(Arrays.equals(BindingCodec.aad(original),
                BindingCodec.aad(changedKey)));
        Assertions.assertFalse(Arrays.equals(BindingCodec.aad(original),
                BindingCodec.aad(changedChallenge)));
    }

    private static HelloBinding binding(String keyName, byte[] challenge) {
        return new HelloBinding("local", bytes(16, 0x11), keyName, challenge,
                bytes(12, 0x22), bytes(48, 0x44), 100L, 200L);
    }

    private static byte[] bytes(int length, int value) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }
}
