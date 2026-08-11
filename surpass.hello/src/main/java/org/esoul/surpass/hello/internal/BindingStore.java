package org.esoul.surpass.hello.internal;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.esoul.surpass.persist.api.PersistenceDefaults;

public final class BindingStore {

    public boolean exists(String persistenceServiceId) {
        return Files.isRegularFile(pathFor(persistenceServiceId));
    }

    public HelloBinding read(String persistenceServiceId) throws IOException {
        HelloBinding binding = BindingCodec.decode(Files.readAllBytes(pathFor(persistenceServiceId)));
        if (!MessageDigest.isEqual(binding.persistenceServiceId().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                persistenceServiceId.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new IOException("Hello binding service ID mismatch");
        }
        return binding;
    }

    public void write(HelloBinding binding) throws IOException {
        Path target = pathFor(binding.persistenceServiceId());
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            byte[] encoded = BindingCodec.encode(binding);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(encoded);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    public void delete(String persistenceServiceId) throws IOException {
        Files.deleteIfExists(pathFor(persistenceServiceId));
    }

    public Path pathFor(String persistenceServiceId) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(persistenceServiceId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return PersistenceDefaults.getDataDir().resolve("hello")
                    .resolve(HexFormat.of().formatHex(hash) + ".binding");
        } catch (GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }
}
