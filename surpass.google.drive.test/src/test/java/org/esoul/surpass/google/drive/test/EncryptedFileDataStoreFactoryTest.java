package org.esoul.surpass.google.drive.test;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import org.esoul.surpass.crypto.api.ContextAwareCryptoService;
import org.esoul.surpass.google.drive.EncryptedFileDataStoreFactory;
import org.esoul.surpass.google.drive.EncryptedFileDataStoreFactory.DataStoreCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.google.api.client.util.IOUtils;
import com.google.api.client.util.Maps;
import com.google.api.client.util.store.DataStore;

class EncryptedFileDataStoreFactoryTest {

    @TempDir
    Path tempDir;

    private ContextAwareCryptoService crypto = Mockito.mock(ContextAwareCryptoService.class);
    private EncryptedFileDataStoreFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = new EncryptedFileDataStoreFactory(tempDir, crypto, new DataStoreCaptor());
        Answer<byte[]> fakeEncryptDecryptAnswer = invocation -> {
            byte[] text1 = invocation.getArgument(0);
            byte[] text2 = new byte[text1.length];
            fakeEncryptDecrypt(text1, text2);
            return text2;
        };
        Mockito.when(crypto.encrypt(Mockito.any())).thenAnswer(fakeEncryptDecryptAnswer);
        Mockito.when(crypto.decrypt(Mockito.any())).thenAnswer(fakeEncryptDecryptAnswer);
    }

    @Test
    void testEncrypt() throws Exception {
        DataStore<Serializable> store = factory.getDataStore("testStore");
        Assertions.assertNotNull(store);
        Assertions.assertEquals("testStore", store.getId());
        store.set("key1", "value1");

        Path dataStorePath = tempDir.resolve("testStore");
        Assertions.assertTrue(Files.exists(dataStorePath));

        byte[] expectedBytes = createTestDataStoreBytes();
        byte[] dataStoreBytes = Files.readAllBytes(dataStorePath);
        Assertions.assertArrayEquals(expectedBytes, dataStoreBytes);
    }

    @Test
    void testDecrypt() throws Exception {
        Path dataStorePath = tempDir.resolve("testStore");
        byte[] encryptedBytes = createTestDataStoreBytes();
        Files.write(dataStorePath, encryptedBytes);

        DataStore<Serializable> store = factory.getDataStore("testStore");

        Assertions.assertEquals("value1", store.get("key1"));
    }

    private byte[] createTestDataStoreBytes() throws IOException {
        var map = Maps.newHashMap();
        map.put("key1", IOUtils.serialize("value1"));
        byte[] bytes = IOUtils.serialize(map);
        fakeEncryptDecrypt(bytes, bytes);
        return bytes;
    }

    private void fakeEncryptDecrypt(byte[] text1, byte[] text2) {
        for (int i = 0; i < text1.length; i++) {
            text2[i] = (byte) ~text1[i];
        }
    }
}
