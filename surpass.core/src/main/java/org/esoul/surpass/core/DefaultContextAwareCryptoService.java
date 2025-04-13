package org.esoul.surpass.core;

import java.nio.CharBuffer;
import java.security.GeneralSecurityException;
import java.util.Objects;

import org.esoul.surpass.crypto.api.ContextAwareCryptoService;
import org.esoul.surpass.crypto.api.CryptoService;

public class DefaultContextAwareCryptoService implements ContextAwareCryptoService {

    private final CryptoService cryptoService;
    private final CharBuffer key;

    public DefaultContextAwareCryptoService(CryptoService cryptoService, CharBuffer key) {
        this.cryptoService = cryptoService;
        this.key = key;
    }

    @Override
    public byte[] encrypt(byte[] data) throws GeneralSecurityException {
        return cryptoService.encrypt(key.array(), data);
    }

    @Override
    public byte[] decrypt(byte[] cipherInput) throws GeneralSecurityException {
        return cryptoService.decrypt(key.array(), cipherInput);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DefaultContextAwareCryptoService other = (DefaultContextAwareCryptoService) obj;
        return Objects.equals(key, other.key);
    }
}
