package org.esoul.surpass.hello.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class BindingCodec {

    private static final byte[] MAGIC = { 'S', 'P', 'H', 'B' };
    private static final int VERSION = 2;
    private static final int KEY_PROVIDER_VERSION = 1;
    private static final int MAX_SERVICE_ID_BYTES = 1024;
    private static final int MAX_KEY_NAME_BYTES = 256;
    private static final int MAX_WRAPPED_BUNDLE_BYTES = 8192;

    private BindingCodec() {
    }

    public static byte[] encode(HelloBinding binding) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            writeHeader(out, binding);
            writeBytes(out, binding.wrappedBundle());
        }
        return bytes.toByteArray();
    }

    public static byte[] aad(HelloBinding binding) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.write("surpass/hello/v2/wrapper".getBytes(StandardCharsets.UTF_8));
            writeHeader(out, binding);
        }
        return bytes.toByteArray();
    }

    private static void writeHeader(DataOutputStream out, HelloBinding binding) throws IOException {
        out.write(MAGIC);
        out.writeShort(VERSION);
        out.writeShort(KEY_PROVIDER_VERSION);
        writeString(out, binding.persistenceServiceId());
        writeFixed(out, binding.vaultId(), 16);
        writeKeyName(out, binding.keyName());
        writeFixed(out, binding.challenge(), 32);
        writeFixed(out, binding.nonce(), CryptoSupport.GCM_NONCE_BYTES);
        out.writeLong(binding.createdAt());
        out.writeLong(binding.updatedAt());
    }

    public static HelloBinding decode(byte[] input) throws IOException {
        if (input.length > MAX_WRAPPED_BUNDLE_BYTES + MAX_KEY_NAME_BYTES + 2048) {
            throw new IOException("Hello binding is too large");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(input))) {
            if (!Arrays.equals(MAGIC, readExact(in, MAGIC.length))) {
                throw new IOException("Invalid Hello binding magic");
            }
            if (in.readUnsignedShort() != VERSION
                    || in.readUnsignedShort() != KEY_PROVIDER_VERSION) {
                throw new IOException("Unsupported Hello binding version");
            }
            String serviceId = readString(in);
            byte[] vaultId = readExact(in, 16);
            String keyName = readKeyName(in);
            byte[] challenge = readExact(in, 32);
            byte[] nonce = readExact(in, CryptoSupport.GCM_NONCE_BYTES);
            long created = in.readLong();
            long updated = in.readLong();
            byte[] wrapped = readBounded(in, 16, MAX_WRAPPED_BUNDLE_BYTES);
            if (in.read() != -1) {
                throw new IOException("Trailing Hello binding data");
            }
            return new HelloBinding(serviceId, vaultId, keyName, challenge, nonce, wrapped,
                    created, updated);
        } catch (EOFException e) {
            throw new IOException("Truncated Hello binding", e);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        if (value == null) {
            throw new IOException("Invalid persistence service ID");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > MAX_SERVICE_ID_BYTES) {
            throw new IOException("Invalid persistence service ID");
        }
        writeBytes(out, bytes);
    }

    private static void writeKeyName(DataOutputStream out, String value) throws IOException {
        if (value == null) {
            throw new IOException("Invalid Windows Hello key name");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > MAX_KEY_NAME_BYTES) {
            throw new IOException("Invalid Windows Hello key name");
        }
        writeBytes(out, bytes);
    }

    private static String readKeyName(DataInputStream in) throws IOException {
        byte[] bytes = readBounded(in, 1, MAX_KEY_NAME_BYTES);
        String value = new String(bytes, StandardCharsets.UTF_8);
        if (!Arrays.equals(bytes, value.getBytes(StandardCharsets.UTF_8))) {
            throw new IOException("Invalid UTF-8 in Windows Hello key name");
        }
        return value;
    }

    private static String readString(DataInputStream in) throws IOException {
        byte[] bytes = readBounded(in, 1, MAX_SERVICE_ID_BYTES);
        String value = new String(bytes, StandardCharsets.UTF_8);
        if (!Arrays.equals(bytes, value.getBytes(StandardCharsets.UTF_8))) {
            throw new IOException("Invalid UTF-8 in persistence service ID");
        }
        return value;
    }

    private static void writeFixed(DataOutputStream out, byte[] value, int expected)
            throws IOException {
        if (value.length != expected) {
            throw new IOException("Invalid fixed field length");
        }
        out.write(value);
    }

    private static void writeBytes(DataOutputStream out, byte[] value) throws IOException {
        out.writeInt(value.length);
        out.write(value);
    }

    private static byte[] readBounded(DataInputStream in, int min, int max) throws IOException {
        int length = in.readInt();
        if (length < min || length > max) {
            throw new IOException("Invalid Hello binding field length");
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
