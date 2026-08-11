package org.esoul.surpass.hello.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

public record VaultEnvelope(byte[] vaultId, int iterations, byte[] passwordSalt,
        byte[] passwordNonce, byte[] wrappedDek, byte[] contentNonce, byte[] encryptedContent) {

    public static final byte[] MAGIC = { 'S', 'P', 'V', '1' };
    public static final int VERSION = 1;
    private static final int CIPHER_AES_256_GCM = 1;
    private static final int KDF_PBKDF2_HMAC_SHA_512 = 1;
    private static final int VAULT_ID_BYTES = 16;
    private static final int MAX_CONTENT_BYTES = 64 * 1024 * 1024;
    private static final int WRAPPED_DEK_BYTES = CryptoSupport.AES_KEY_BYTES + 16;

    public VaultEnvelope {
        vaultId = vaultId.clone();
        passwordSalt = passwordSalt.clone();
        passwordNonce = passwordNonce.clone();
        wrappedDek = wrappedDek.clone();
        contentNonce = contentNonce.clone();
        encryptedContent = encryptedContent.clone();
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.write(MAGIC);
            out.writeShort(VERSION);
            out.writeByte(CIPHER_AES_256_GCM);
            out.writeByte(KDF_PBKDF2_HMAC_SHA_512);
            out.write(vaultId);
            out.writeInt(iterations);
            writeBytes(out, passwordSalt);
            writeBytes(out, passwordNonce);
            writeBytes(out, wrappedDek);
            writeBytes(out, contentNonce);
            writeBytes(out, encryptedContent);
        }
        return bytes.toByteArray();
    }

    public static boolean hasMagic(byte[] input) {
        return input.length >= MAGIC.length
                && Arrays.equals(MAGIC, Arrays.copyOf(input, MAGIC.length));
    }

    public static VaultEnvelope decode(byte[] input) throws IOException {
        if (input.length > MAX_CONTENT_BYTES + 1024) {
            throw new IOException("Vault envelope is too large");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(input))) {
            byte[] magic = in.readNBytes(MAGIC.length);
            if (!Arrays.equals(MAGIC, magic)) {
                throw new IOException("Invalid vault magic");
            }
            if (in.readUnsignedShort() != VERSION) {
                throw new IOException("Unsupported vault version");
            }
            if (in.readUnsignedByte() != CIPHER_AES_256_GCM
                    || in.readUnsignedByte() != KDF_PBKDF2_HMAC_SHA_512) {
                throw new IOException("Unsupported vault cryptographic suite");
            }
            byte[] vaultId = readExact(in, VAULT_ID_BYTES);
            int iterations = in.readInt();
            if (iterations < CryptoSupport.MIN_PBKDF2_ITERATIONS || iterations > 2_000_000) {
                throw new IOException("Invalid PBKDF2 parameters");
            }
            byte[] salt = readBounded(in, CryptoSupport.PASSWORD_SALT_BYTES,
                    CryptoSupport.PASSWORD_SALT_BYTES);
            byte[] passwordNonce = readBounded(in, CryptoSupport.GCM_NONCE_BYTES,
                    CryptoSupport.GCM_NONCE_BYTES);
            byte[] wrappedDek = readBounded(in, WRAPPED_DEK_BYTES, WRAPPED_DEK_BYTES);
            byte[] contentNonce = readBounded(in, CryptoSupport.GCM_NONCE_BYTES,
                    CryptoSupport.GCM_NONCE_BYTES);
            byte[] content = readBounded(in, 16, MAX_CONTENT_BYTES);
            if (in.read() != -1) {
                throw new IOException("Trailing vault data");
            }
            return new VaultEnvelope(vaultId, iterations, salt, passwordNonce, wrappedDek,
                    contentNonce, content);
        } catch (EOFException e) {
            throw new IOException("Truncated vault envelope", e);
        }
    }

    private static void writeBytes(DataOutputStream out, byte[] value) throws IOException {
        out.writeInt(value.length);
        out.write(value);
    }

    private static byte[] readBounded(DataInputStream in, int min, int max) throws IOException {
        int length = in.readInt();
        if (length < min || length > max) {
            throw new IOException("Invalid vault field length");
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
