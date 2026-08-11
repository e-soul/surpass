package org.esoul.surpass.hello.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

public final class UnlockBundleCodec {

    private static final byte[] MAGIC = { 'S', 'P', 'L', 'B' };
    private static final int VERSION = 1;
    private static final int MAX_SERVICE_ID_BYTES = 1024;
    private static final int MAX_AUTHORIZATION_BYTES = 4096;

    private UnlockBundleCodec() {
    }

    public record Bundle(byte[] vaultId, String persistenceServiceId, byte[] dek,
            char[] persistenceAuthorization) implements AutoCloseable {
        public Bundle {
            vaultId = vaultId.clone();
            dek = dek.clone();
            persistenceAuthorization = persistenceAuthorization.clone();
        }

        @Override
        public void close() {
            Arrays.fill(vaultId, (byte) 0);
            Arrays.fill(dek, (byte) 0);
            Arrays.fill(persistenceAuthorization, '\0');
        }
    }

    public static byte[] encode(byte[] vaultId, String serviceId, byte[] dek,
            char[] authorization) throws IOException {
        byte[] service = serviceId.getBytes(StandardCharsets.UTF_8);
        ByteBuffer encodedAuthorization = StandardCharsets.UTF_8.encode(CharBuffer.wrap(authorization));
        byte[] auth = new byte[encodedAuthorization.remaining()];
        encodedAuthorization.get(auth);
        try {
            if (vaultId.length != 16 || dek.length != CryptoSupport.AES_KEY_BYTES
                    || service.length == 0 || service.length > MAX_SERVICE_ID_BYTES
                    || auth.length == 0 || auth.length > MAX_AUTHORIZATION_BYTES) {
                throw new IOException("Invalid local unlock bundle field");
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.write(MAGIC);
                out.writeShort(VERSION);
                out.write(vaultId);
                writeBytes(out, service);
                out.write(dek);
                writeBytes(out, auth);
            }
            return bytes.toByteArray();
        } finally {
            Arrays.fill(auth, (byte) 0);
            if (encodedAuthorization.hasArray()) {
                Arrays.fill(encodedAuthorization.array(), (byte) 0);
            }
        }
    }

    public static Bundle decode(byte[] input) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(input))) {
            if (!Arrays.equals(MAGIC, readExact(in, MAGIC.length))) {
                throw new IOException("Invalid local unlock bundle magic");
            }
            if (in.readUnsignedShort() != VERSION) {
                throw new IOException("Unsupported local unlock bundle version");
            }
            byte[] vaultId = readExact(in, 16);
            byte[] serviceBytes = readBounded(in, 1, MAX_SERVICE_ID_BYTES);
            String serviceId = new String(serviceBytes, StandardCharsets.UTF_8);
            if (!Arrays.equals(serviceBytes, serviceId.getBytes(StandardCharsets.UTF_8))) {
                throw new IOException("Invalid local unlock bundle UTF-8");
            }
            byte[] dek = readExact(in, CryptoSupport.AES_KEY_BYTES);
            byte[] authBytes = readBounded(in, 1, MAX_AUTHORIZATION_BYTES);
            char[] authorization;
            try {
                CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(authBytes));
                authorization = new char[decoded.remaining()];
                decoded.get(authorization);
                if (decoded.hasArray()) {
                    Arrays.fill(decoded.array(), '\0');
                }
            } catch (CharacterCodingException e) {
                throw new IOException("Invalid local unlock authorization UTF-8", e);
            }
            if (in.read() != -1) {
                throw new IOException("Trailing local unlock bundle data");
            }
            Arrays.fill(authBytes, (byte) 0);
            return new Bundle(vaultId, serviceId, dek, authorization);
        } catch (EOFException e) {
            throw new IOException("Truncated local unlock bundle", e);
        }
    }

    private static void writeBytes(DataOutputStream out, byte[] value) throws IOException {
        out.writeInt(value.length);
        out.write(value);
    }

    private static byte[] readBounded(DataInputStream in, int min, int max) throws IOException {
        int length = in.readInt();
        if (length < min || length > max) {
            throw new IOException("Invalid local unlock bundle field length");
        }
        return readExact(in, length);
    }

    private static byte[] readExact(DataInputStream in, int length) throws IOException {
        byte[] value = in.readNBytes(length);
        if (value.length != length) {
            throw new EOFException();
        }
        return value;
    }
}
