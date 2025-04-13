package org.esoul.surpass.core;

import java.nio.CharBuffer;

import org.esoul.surpass.crypto.api.ContextAwareCrypto;
import org.esoul.surpass.crypto.api.ContextAwareCryptoAbstractFactory;
import org.esoul.surpass.crypto.api.CryptoService;

public class DefaultContextAwareCryptoFactory implements ContextAwareCryptoAbstractFactory {

    @Override
    public ContextAwareCrypto create(CryptoService cryptoService, CharBuffer key) {
        return new DefaultContextAwareCrypto(cryptoService, key);
    }
}
