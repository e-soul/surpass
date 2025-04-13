package org.esoul.surpass.crypto.api;

import java.nio.CharBuffer;

public interface ContextAwareCryptoAbstractFactory {

    ContextAwareCrypto create(CryptoService cryptoService, CharBuffer key);
}
