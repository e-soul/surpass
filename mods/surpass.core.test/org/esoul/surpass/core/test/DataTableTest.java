package org.esoul.surpass.core.test;

import org.junit.Test;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.esoul.surpass.core.DataTable;
import org.esoul.surpass.core.MaxSizeExceededException;
import org.junit.Assert;
import org.junit.Before;

/**
 *
 * @author mgp
 */
public class DataTableTest {

    private byte[][] bytes;
    private DataTable dt;

    @Before
    public void setUp() {
        dt = new DataTable();
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

    private void checkRow(int row, char[] secret0, char[] identifier0, char[] note0) throws MaxSizeExceededException {
        checkCellData(secret0, row, DataTable.INDEX_SECRET_LEN, DataTable.INDEX_SECRET);
        checkCellData(identifier0, row, DataTable.INDEX_IDENTIFIER_LEN, DataTable.INDEX_IDENTIFIER);
        checkCellData(note0, row, DataTable.INDEX_NOTE_LEN, DataTable.INDEX_NOTE);
    }

    private void checkCellData(char[] input, int indexRow, int indexDataLen, int indexData) {
        byte[] row = bytes[indexRow];
        checkData(row, input, indexDataLen, indexData);
    }

    @Test(expected = MaxSizeExceededException.class)
    public void testRowOverflow() throws Exception {
        for (int i = 0; i < 300; i++) {
            dt.createRow("GG".toCharArray(), "PP".toCharArray(), "AA".toCharArray());
        }
    }

    @Test(expected = MaxSizeExceededException.class)
    public void testSecretOverflow() throws Exception {
        char[] secret = generateData(DataTable.MAX_SECRET_LEN + 1);
        dt.createRow(secret, "PP".toCharArray(), "FF".toCharArray());
    }

    @Test(expected = MaxSizeExceededException.class)
    public void testIdentifierOverflow() throws Exception {
        char[] identifier = generateData(DataTable.MAX_IDENTIFIER_LEN + 1);
        dt.createRow("GG".toCharArray(), identifier, "AA".toCharArray());
    }

    @Test(expected = MaxSizeExceededException.class)
    public void testNoteOverflow() throws Exception {
        char[] note = generateData(DataTable.MAX_NOTE_LEN + 1);
        dt.createRow("YY".toCharArray(), "QQ".toCharArray(), note);
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
        int rowOffset = row * (DataTable.MAX_COL + 1);
        checkData(sequence, secret, rowOffset + DataTable.INDEX_SECRET_LEN, rowOffset + DataTable.INDEX_SECRET);
        checkData(sequence, identifier, rowOffset + DataTable.INDEX_IDENTIFIER_LEN, rowOffset + DataTable.INDEX_IDENTIFIER);
        checkData(sequence, note, rowOffset + DataTable.INDEX_NOTE_LEN, rowOffset + DataTable.INDEX_NOTE);
    }

    private void checkData(byte[] sequence, char[] input, int indexDataLen, int indexData) {
        Assert.assertEquals((byte) input.length, sequence[indexDataLen]);
        for (int i = 0; i < input.length; i++) {
            Assert.assertEquals((byte) input[i], sequence[indexData + i]);
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
        Assert.assertEquals((byte) 0, bytes[DataTable.SERVICE_ROW][0]);
        Assert.assertEquals((byte) expectedFirstFreeRow, bytes[DataTable.SERVICE_ROW][1]);
        for (int i = 2; i < bytes[DataTable.SERVICE_ROW].length; i++) {
            Assert.assertEquals((byte) 0, bytes[DataTable.SERVICE_ROW][i]);
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

        Assert.assertEquals(3, dt.getRowNumber());
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

        Assert.assertTrue(Arrays.equals(encode(secret0), dt.readSecret(0)));
        Assert.assertTrue(Arrays.equals(encode(identifier0), dt.readIdentifier(0)));
        Assert.assertTrue(Arrays.equals(encode(note0), dt.readNote(0)));

        Assert.assertTrue(Arrays.equals(encode(secret1), dt.readSecret(1)));
        Assert.assertTrue(Arrays.equals(encode(identifier1), dt.readIdentifier(1)));
        Assert.assertTrue(Arrays.equals(encode(note1), dt.readNote(1)));

        Assert.assertTrue(Arrays.equals(encode(secret2), dt.readSecret(2)));
        Assert.assertTrue(Arrays.equals(encode(identifier2), dt.readIdentifier(2)));
        Assert.assertTrue(Arrays.equals(encode(note2), dt.readNote(2)));
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

        Assert.assertEquals(2, dt.getRowNumber());

        Assert.assertTrue(Arrays.equals(encode(secret1), dt.readSecret(0)));
        Assert.assertTrue(Arrays.equals(encode(identifier1), dt.readIdentifier(0)));
        Assert.assertTrue(Arrays.equals(encode(note1), dt.readNote(0)));

        Assert.assertTrue(Arrays.equals(encode(secret2), dt.readSecret(1)));
        Assert.assertTrue(Arrays.equals(encode(identifier2), dt.readIdentifier(1)));
        Assert.assertTrue(Arrays.equals(encode(note2), dt.readNote(1)));

        dt.removeRow(1);

        Assert.assertEquals(1, dt.getRowNumber());

        Assert.assertTrue(Arrays.equals(encode(secret1), dt.readSecret(0)));
        Assert.assertTrue(Arrays.equals(encode(identifier1), dt.readIdentifier(0)));
        Assert.assertTrue(Arrays.equals(encode(note1), dt.readNote(0)));
    }
}
