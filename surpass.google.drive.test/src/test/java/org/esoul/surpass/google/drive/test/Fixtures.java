package org.esoul.surpass.google.drive.test;

import org.esoul.surpass.crypto.api.ContextAwareCrypto;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

class Fixtures {
    
    static final byte[] ABC = new byte[] { 65, 66, 67 };

    static ContextAwareCrypto setUpCryptoStub() {
        ContextAwareCrypto crypto = Mockito.mock(ContextAwareCrypto.class);
        Answer<byte[]> noOpAnswer = invocation -> {
            byte[] input = invocation.getArgument(0);
            return input;
        };
        try {
            Mockito.when(crypto.encrypt(Mockito.any())).thenAnswer(noOpAnswer);
            Mockito.when(crypto.decrypt(Mockito.any())).thenAnswer(noOpAnswer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return crypto;
    }
}
