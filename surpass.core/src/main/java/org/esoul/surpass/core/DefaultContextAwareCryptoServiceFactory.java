package org.esoul.surpass.core;

import org.esoul.surpass.crypto.api.ContextAwareCryptoService;
import org.esoul.surpass.crypto.api.ContextAwareCryptoServiceAbstractFactory;
import org.esoul.surpass.crypto.api.CryptoService;

public class DefaultContextAwareCryptoServiceFactory implements ContextAwareCryptoServiceAbstractFactory {

    @Override
    public ContextAwareCryptoService create(CryptoService cryptoService, char[] key) {
        return new DefaultContextAwareCryptoService(cryptoService, key);
    }
}
