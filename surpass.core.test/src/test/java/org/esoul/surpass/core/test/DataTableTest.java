package org.esoul.surpass.core.test;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.esoul.surpass.core.SquareMatrix;
import org.esoul.surpass.table.api.MaxSizeExceededException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataTableTest {

    private byte[][] bytes;
    private SquareMatrix dt;

    @BeforeEach
    public void setUp() {
        dt = new SquareMatrix();
        bytes = dt.getBytes();
    }

    @Test
    public void testCreateMultipleRows() throws Exception {
        checkServiceRow(0);
        char[] secret0 = "AAA".toCharArray();
        char[] identifier0 = "BBB".toCharArray();
        char[] note0 = "CCC".toCharArray();
        dt.createRow(secret0.clone(), identifier0.clone(), note0.clone());

        checkServiceRow(1);
        char[] secret1 = "BB".toCharArray();
        char[] identifier1 = "Bob".toCharArray();
        char[] note1 = "gg yo".toCharArray();
        dt.createRow(secret1.clone(), identifier1.clone(), note1.clone());

        checkServiceRow(2);
        char[] secret2 = "CCCC".toCharArray();
        char[] identifier2 = "Pesho".toCharArray();
        char[] note2 = "work work".toCharArray();
        dt.createRow(secret2.clone(), identifier2.clone(), note2.clone());

        checkRow(0, secret0, identifier0, note0);
        checkRow(1, secret1, identifier1, note1);
        checkRow(2, secret2, identifier2, note2);

        checkServiceRow(3);
    }

    private void checkRow(int row, char[] secret0, char[] identifier0, char[] note0) throws Exception {
        checkCellData(secret0, row, SquareMatrix.INDEX_SECRET_LEN, SquareMatrix.INDEX_SECRET);
        checkCellData(identifier0, row, SquareMatrix.INDEX_IDENTIFIER_LEN, SquareMatrix.INDEX_IDENTIFIER);
        checkCellData(note0, row, SquareMatrix.INDEX_NOTE_LEN, SquareMatrix.INDEX_NOTE);
    }

    private void checkCellData(char[] input, int indexRow, int indexDataLen, int indexData) {
        byte[] row = bytes[indexRow];
        checkData(row, input, indexDataLen, indexData);
    }

    @Test
    public void testRowOverflow() throws Exception {
        for (int i = 0; i < SquareMatrix.MAX_ROW; i++) {
            dt.createRow("GG".toCharArray(), "PP".toCharArray(), "AA".toCharArray());
        }
        Assertions.assertThrows(MaxSizeExceededException.class, () -> dt.createRow("GG".toCharArray(), "PP".toCharArray(), "AA".toCharArray()));
    }

    @Test
    public void testSecretOverflow() throws Exception {
        char[] secret = generateData(SquareMatrix.MAX_SECRET_LEN + 1);
        Assertions.assertThrows(MaxSizeExceededException.class, () -> dt.createRow(secret, "PP".toCharArray(), "FF".toCharArray()));
    }

    @Test
    public void testIdentifierOverflow() throws Exception {
        char[] identifier = generateData(SquareMatrix.MAX_IDENTIFIER_LEN + 1);
        Assertions.assertThrows(MaxSizeExceededException.class, () -> dt.createRow("GG".toCharArray(), identifier, "AA".toCharArray()));
    }

    @Test
    public void testNoteOverflow() throws Exception {
        char[] note = generateData(SquareMatrix.MAX_NOTE_LEN + 1);
        Assertions.assertThrows(MaxSizeExceededException.class, () -> dt.createRow("YY".toCharArray(), "QQ".toCharArray(), note));
    }

    private char[] generateData(int length) {
        StringBuilder secret = new StringBuilder();
        for (int i = 0; i < length; i++) {
            secret.append('A');
        }
        return secret.toString().toCharArray();
    }

    @Test
    public void testToOneDimension() throws Exception {
        char[] secret0 = "AAA".toCharArray();
        char[] identifier0 = "BBB".toCharArray();
        char[] note0 = "CCC".toCharArray();
        dt.createRow(secret0.clone(), identifier0.clone(), note0.clone());

        char[] secret1 = "BB".toCharArray();
        char[] identifier1 = "Bob".toCharArray();
        char[] note1 = "gg yo".toCharArray();
        dt.createRow(secret1.clone(), identifier1.clone(), note1.clone());

        char[] secret2 = "CCCC".toCharArray();
        char[] identifier2 = "Pesho".toCharArray();
        char[] note2 = "work work".toCharArray();
        dt.createRow(secret2.clone(), identifier2.clone(), note2.clone());

        byte[] sequence = dt.toOneDimension();

        checkRowInSequence(sequence, 0, secret0, identifier0, note0);
        checkRowInSequence(sequence, 1, secret1, identifier1, note1);
        checkRowInSequence(sequence, 2, secret2, identifier2, note2);
    }

    private void checkRowInSequence(byte[] sequence, int row, char[] secret, char[] identifier, char[] note) {
        int rowOffset = row * (SquareMatrix.MAX_COL + 1);
        checkData(sequence, secret, rowOffset + SquareMatrix.INDEX_SECRET_LEN, rowOffset + SquareMatrix.INDEX_SECRET);
        checkData(sequence, identifier, rowOffset + SquareMatrix.INDEX_IDENTIFIER_LEN, rowOffset + SquareMatrix.INDEX_IDENTIFIER);
        checkData(sequence, note, rowOffset + SquareMatrix.INDEX_NOTE_LEN, rowOffset + SquareMatrix.INDEX_NOTE);
    }

    private void checkData(byte[] sequence, char[] input, int indexDataLen, int indexData) {
        Assertions.assertEquals((byte) input.length, sequence[indexDataLen]);
        for (int i = 0; i < input.length; i++) {
            Assertions.assertEquals((byte) input[i], sequence[indexData + i]);
        }
    }

    @Test
    public void testLoad() throws Exception {
        testCreateMultipleRows();
        byte[] sequence = dt.toOneDimension();
        checkServiceRow(3);
        dt.load(sequence);

        char[] secret0 = "AAA".toCharArray();
        char[] identifier0 = "BBB".toCharArray();
        char[] note0 = "CCC".toCharArray();

        char[] secret1 = "BB".toCharArray();
        char[] identifier1 = "Bob".toCharArray();
        char[] note1 = "gg yo".toCharArray();

        char[] secret2 = "CCCC".toCharArray();
        char[] identifier2 = "Pesho".toCharArray();
        char[] note2 = "work work".toCharArray();

        checkRow(0, secret0, identifier0, note0);
        checkRow(1, secret1, identifier1, note1);
        checkRow(2, secret2, identifier2, note2);

        checkServiceRow(3);
    }

    private void checkServiceRow(int expectedFirstFreeRow) {
        Assertions.assertEquals((byte) 0, bytes[SquareMatrix.SERVICE_ROW][0]);
        Assertions.assertEquals((byte) expectedFirstFreeRow, bytes[SquareMatrix.SERVICE_ROW][1]);
        for (int i = 2; i < bytes[SquareMatrix.SERVICE_ROW].length; i++) {
            Assertions.assertEquals((byte) 0, bytes[SquareMatrix.SERVICE_ROW][i]);
        }
    }

    @Test
    public void testGetRowNumber() throws Exception {
        char[] secret0 = "AAA".toCharArray();
        char[] identifier0 = "BBB".toCharArray();
        char[] note0 = "CCC".toCharArray();
        dt.createRow(secret0.clone(), identifier0.clone(), note0.clone());

        char[] secret1 = "BB".toCharArray();
        char[] identifier1 = "Bob".toCharArray();
        char[] note1 = "gg yo".toCharArray();
        dt.createRow(secret1.clone(), identifier1.clone(), note1.clone());

        char[] secret2 = "CCCC".toCharArray();
        char[] identifier2 = "Pesho".toCharArray();
        char[] note2 = "work work".toCharArray();
        dt.createRow(secret2.clone(), identifier2.clone(), note2.clone());

        Assertions.assertEquals(3, dt.getRowNumber());
    }

    @Test
    public void testReadRows() throws Exception {
        char[] secret0 = "AAA".toCharArray();
        char[] identifier0 = "BBB".toCharArray();
        char[] note0 = "CCC".toCharArray();
        dt.createRow(secret0.clone(), identifier0.clone(), note0.clone());

        char[] secret1 = "BB".toCharArray();
        char[] identifier1 = "Bob".toCharArray();
        char[] note1 = "gg yo".toCharArray();
        dt.createRow(secret1.clone(), identifier1.clone(), note1.clone());

        char[] secret2 = "CCCC".toCharArray();
        char[] identifier2 = "Pesho".toCharArray();
        char[] note2 = "work work".toCharArray();
        dt.createRow(secret2.clone(), identifier2.clone(), note2.clone());

        Assertions.assertTrue(Arrays.equals(encode(secret0), dt.readSecret(0)));
        Assertions.assertTrue(Arrays.equals(encode(identifier0), dt.readIdentifier(0)));
        Assertions.assertTrue(Arrays.equals(encode(note0), dt.readNote(0)));

        Assertions.assertTrue(Arrays.equals(encode(secret1), dt.readSecret(1)));
        Assertions.assertTrue(Arrays.equals(encode(identifier1), dt.readIdentifier(1)));
        Assertions.assertTrue(Arrays.equals(encode(note1), dt.readNote(1)));

        Assertions.assertTrue(Arrays.equals(encode(secret2), dt.readSecret(2)));
        Assertions.assertTrue(Arrays.equals(encode(identifier2), dt.readIdentifier(2)));
        Assertions.assertTrue(Arrays.equals(encode(note2), dt.readNote(2)));
    }

    private byte[] encode(char[] data) {
        return StandardCharsets.UTF_8.encode(CharBuffer.wrap(data)).array();
    }

    @Test
    public void testRemoveRows() throws Exception {
        char[] secret0 = "AAA".toCharArray();
        char[] identifier0 = "BBB".toCharArray();
        char[] note0 = "CCC".toCharArray();
        dt.createRow(secret0.clone(), identifier0.clone(), note0.clone());

        char[] secret1 = "BB".toCharArray();
        char[] identifier1 = "Bob".toCharArray();
        char[] note1 = "gg yo".toCharArray();
        dt.createRow(secret1.clone(), identifier1.clone(), note1.clone());

        char[] secret2 = "CCCC".toCharArray();
        char[] identifier2 = "Pesho".toCharArray();
        char[] note2 = "work work".toCharArray();
        dt.createRow(secret2.clone(), identifier2.clone(), note2.clone());

        dt.removeRow(0);

        Assertions.assertEquals(2, dt.getRowNumber());

        Assertions.assertTrue(Arrays.equals(encode(secret1), dt.readSecret(0)));
        Assertions.assertTrue(Arrays.equals(encode(identifier1), dt.readIdentifier(0)));
        Assertions.assertTrue(Arrays.equals(encode(note1), dt.readNote(0)));

        Assertions.assertTrue(Arrays.equals(encode(secret2), dt.readSecret(1)));
        Assertions.assertTrue(Arrays.equals(encode(identifier2), dt.readIdentifier(1)));
        Assertions.assertTrue(Arrays.equals(encode(note2), dt.readNote(1)));

        dt.removeRow(1);

        Assertions.assertEquals(1, dt.getRowNumber());

        Assertions.assertTrue(Arrays.equals(encode(secret1), dt.readSecret(0)));
        Assertions.assertTrue(Arrays.equals(encode(identifier1), dt.readIdentifier(0)));
        Assertions.assertTrue(Arrays.equals(encode(note1), dt.readNote(0)));
    }

    @Test
    public void testUpdateRows() throws Exception {
        char[] secret0 = "AAA".toCharArray();
        char[] identifier0 = "BBB".toCharArray();
        char[] note0 = "CCC".toCharArray();
        dt.createRow(secret0.clone(), identifier0.clone(), note0.clone());

        char[] secret1 = "BB".toCharArray();
        char[] identifier1 = "Bob".toCharArray();
        char[] note1 = "gg yo".toCharArray();
        dt.createRow(secret1.clone(), identifier1.clone(), note1.clone());

        char[] secret2 = "CCCC".toCharArray();
        char[] identifier2 = "Pesho".toCharArray();
        char[] note2 = "work work".toCharArray();
        dt.createRow(secret2.clone(), identifier2.clone(), note2.clone());

        dt.updateRow(0, "AAA-updated".toCharArray(), "BBB-updated".toCharArray(), "CCC-updated".toCharArray());

        Assertions.assertTrue(Arrays.equals(encode("AAA-updated".toCharArray()), dt.readSecret(0)));
        Assertions.assertTrue(Arrays.equals(encode("BBB-updated".toCharArray()), dt.readIdentifier(0)));
        Assertions.assertTrue(Arrays.equals(encode("CCC-updated".toCharArray()), dt.readNote(0)));

        dt.updateRow(1, null, "BB-updated".toCharArray(), note1.clone());

        Assertions.assertTrue(Arrays.equals(encode(secret1), dt.readSecret(1)));
        Assertions.assertTrue(Arrays.equals(encode("BB-updated".toCharArray()), dt.readIdentifier(1)));
        Assertions.assertTrue(Arrays.equals(encode(note1), dt.readNote(1)));

        Assertions.assertTrue(Arrays.equals(encode(secret2), dt.readSecret(2)));
        Assertions.assertTrue(Arrays.equals(encode(identifier2), dt.readIdentifier(2)));
        Assertions.assertTrue(Arrays.equals(encode(note2), dt.readNote(2)));
    }
}
