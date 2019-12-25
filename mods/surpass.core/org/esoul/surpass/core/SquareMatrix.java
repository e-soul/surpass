/*
   Copyright 2017-2019 e-soul.org
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted
   provided that the following conditions are met:

   1. Redistributions of source code must retain the above copyright notice, this list of conditions
      and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
      and the following disclaimer in the documentation and/or other materials provided with the distribution.

   THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.esoul.surpass.core;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.esoul.surpass.table.api.EmptySequenceException;
import org.esoul.surpass.table.api.MaxSizeExceededException;
import org.esoul.surpass.table.api.SecretTable;

/**
 * This {@link SecretTable} implementation is backed by a square matrix. The idea is that the size of the data remains constant regardless of the number and length of stored
 * secrets. Of course this implies a hard limit on the number (and length) of secrets that can be stored. Storage scheme:
 * 
 * <pre>
 * [secret length - 1 byte][secret - up to 63 bytes][identifier length - 1 byte][identifier - up to 63 bytes][note length - 1 byte][note - up to 127 bytes]
 * </pre>
 * 
 * Unused positions are automatically filled with random bytes. The last row contains service data such as the number of used rows and the version of the format or storage scheme.
 * The focus is on simplicity at the expense of flexibility.
 * 
 * This class is thread-safe.
 *
 * @author mgp
 */
public class SquareMatrix implements SecretTable {

    private static class Column {

        private final int startIndex;
        private final int columnLength;

        private Column(int startIndex, int columnLength) {
            this.startIndex = startIndex;
            this.columnLength = columnLength;
        }
    }

    public static final int MAX_ROW = 255;

    public static final int MAX_COL = 255;

    public static final int SERVICE_ROW = MAX_ROW;

    /** For future format versions. */
    @SuppressWarnings("unused")
    private static final int SERVICE_COL_VERSION = 0;

    private static final int SERVICE_COL_NEXT_ROW = 1;

    public static final int MAX_SECRET_LEN = 63;
    public static final int MAX_IDENTIFIER_LEN = 63;
    public static final int MAX_NOTE_LEN = 127;

    public static final int INDEX_SECRET_LEN = 0;
    public static final int INDEX_IDENTIFIER_LEN = 64;
    public static final int INDEX_NOTE_LEN = 128;

    public static final int INDEX_SECRET = 1;
    public static final int INDEX_IDENTIFIER = 65;
    public static final int INDEX_NOTE = 129;

    private static final Column SECRET_LEN = new Column(INDEX_SECRET_LEN, 1);
    private static final Column SECRET = new Column(INDEX_SECRET, MAX_SECRET_LEN);
    private static final Column IDENTIFIER_LEN = new Column(INDEX_IDENTIFIER_LEN, 1);
    private static final Column IDENTIFIER = new Column(INDEX_IDENTIFIER, MAX_IDENTIFIER_LEN);
    private static final Column NOTE_LEN = new Column(INDEX_NOTE_LEN, 1);
    private static final Column NOTE = new Column(INDEX_NOTE, MAX_NOTE_LEN);

    private final byte[][] table = new byte[MAX_ROW + 1][MAX_COL + 1];

    private final SecureRandom secureRandom = new SecureRandom();

    private final Lock lock = new ReentrantLock();

    /**
     * Fills the table with random bytes except for the service row.
     */
    public SquareMatrix() {
        fillWithRandomBytesExceptForTheServiceRow();
    }

    private void fillWithRandomBytesExceptForTheServiceRow() {
        for (int i = 0; i < SERVICE_ROW - 1; i++) {
            secureRandom.nextBytes(table[i]);
        }
    }

    /**
     * For testing purposes. Not lock-protected.
     *
     * @return A direct reference to the current table.
     */
    public byte[][] getBytes() {
        return table;
    }

    @Override
    public void createRow(char[] secret, char[] identifier, char[] note) throws MaxSizeExceededException, EmptySequenceException {
        byte[] secretBytes = encodeAndClear(secret);
        validateSecret(secretBytes);
        byte[] identifierBytes = encodeAndClear(identifier);
        validateIdentifier(identifierBytes);
        byte[] noteBytes = encodeAndClear(note);
        validateNote(noteBytes);
        lock.lock();
        try {
            int row = nextRow();
            writeSecretLength(row, secretBytes);
            writeSecret(row, secretBytes);
            writeIdentifierLength(row, identifierBytes);
            writeIdentifier(row, identifierBytes);
            writeNoteLength(row, noteBytes);
            writeNote(row, noteBytes);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateRow(int row, char[] secret, char[] identifier, char[] note) throws MaxSizeExceededException, EmptySequenceException {
        lock.lock();
        try {
            if (row >= getRowNumber()) {
                throw new IllegalArgumentException("Nonexistent row " + row);
            }

            byte[] secretBytes = null;
            if (null != secret) {
                secretBytes = encodeAndClear(secret);
                validateSecret(secretBytes);
            }
            byte[] identifierBytes = encodeAndClear(identifier);
            validateIdentifier(identifierBytes);
            byte[] noteBytes = encodeAndClear(note);
            validateNote(noteBytes);

            if (null != secretBytes) {
                writeSecretLength(row, secretBytes);
                writeSecret(row, secretBytes);
            }
            writeIdentifierLength(row, identifierBytes);
            writeIdentifier(row, identifierBytes);
            writeNoteLength(row, noteBytes);
            writeNote(row, noteBytes);
        } finally {
            lock.unlock();
        }
    }

    private byte[] encodeAndClear(char[] input) {
        byte[] data = StandardCharsets.UTF_8.encode(CharBuffer.wrap(input)).array();
        Arrays.fill(input, '\0');
        return data;
    }

    private void validateSecret(byte[] secretBytes) throws MaxSizeExceededException, EmptySequenceException {
        if (0 == secretBytes.length) {
            throw new EmptySequenceException("Secret cannot be empty!");
        }
        if (secretBytes.length > MAX_SECRET_LEN) {
            throw new MaxSizeExceededException("Secret cannot exceed " + MAX_SECRET_LEN + " bytes!");
        }
    }

    private void validateIdentifier(byte[] identifierBytes) throws MaxSizeExceededException, EmptySequenceException {
        if (0 == identifierBytes.length) {
            throw new EmptySequenceException("Identifier cannot be empty!");
        }
        if (identifierBytes.length > MAX_IDENTIFIER_LEN) {
            throw new MaxSizeExceededException("Identifier cannot exceed " + MAX_IDENTIFIER_LEN + " bytes!");
        }
    }

    private void validateNote(byte[] noteBytes) throws MaxSizeExceededException {
        if (noteBytes.length > MAX_NOTE_LEN) {
            throw new MaxSizeExceededException("Note cannot exceed " + MAX_NOTE_LEN + " bytes!");
        }
    }

    private int nextRow() throws MaxSizeExceededException {
        byte nextRow = table[SERVICE_ROW][SERVICE_COL_NEXT_ROW];
        if (nextRow == (byte) MAX_ROW) {
            throw new MaxSizeExceededException("Maximum number of secrets reached! " + Byte.toUnsignedInt(nextRow));
        }
        table[SERVICE_ROW][SERVICE_COL_NEXT_ROW] = (byte) (nextRow + (byte) 1);
        return Byte.toUnsignedInt(nextRow);
    }

    private void writeSecretLength(int row, byte[] secretBytes) {
        table[row][SECRET_LEN.startIndex] = (byte) secretBytes.length;
    }

    private void writeSecret(int row, byte[] secretBytes) {
        writeData(row, SECRET.startIndex, SECRET.columnLength, secretBytes);
    }

    private void writeIdentifierLength(int row, byte[] identifierBytes) {
        table[row][IDENTIFIER_LEN.startIndex] = (byte) identifierBytes.length;
    }

    private void writeIdentifier(int row, byte[] identifierBytes) {
        writeData(row, IDENTIFIER.startIndex, IDENTIFIER.columnLength, identifierBytes);
    }

    private void writeNoteLength(int row, byte[] noteBytes) {
        table[row][NOTE_LEN.startIndex] = (byte) noteBytes.length;
    }

    private void writeNote(int row, byte[] noteBytes) {
        writeData(row, NOTE.startIndex, NOTE.columnLength, noteBytes);
    }

    private void writeData(int row, int startIndex, int columnLength, byte[] bytes) {
        for (int columnWriteIndex = startIndex, secretReadIndex = 0; columnWriteIndex < (startIndex + columnLength); columnWriteIndex++, secretReadIndex++) {
            if (secretReadIndex < bytes.length) {
                table[row][columnWriteIndex] = bytes[secretReadIndex];
            } else {
                table[row][columnWriteIndex] = nextRandomByte();
            }
        }
    }

    private byte nextRandomByte() {
        return (byte) secureRandom.nextInt(256);
    }

    @Override
    public byte[] toOneDimension() {
        byte[] sequence = new byte[(MAX_ROW + 1) * (MAX_COL + 1)];
        int i = 0;
        lock.lock();
        try {
            for (byte[] row : table) {
                for (byte symbol : row) {
                    sequence[i++] = symbol;
                }
            }
        } finally {
            lock.unlock();
        }
        return sequence;
    }

    @Override
    public void load(byte[] sequence) {
        if (sequence.length != ((MAX_ROW + 1) * (MAX_COL + 1))) {
            throw new IllegalArgumentException("Invalid sequence size!");
        }
        lock.lock();
        try {
            clear();
            for (int i = 0, row = 0, col = 0; i < sequence.length; i++) {
                table[row][col] = sequence[i];
                if (col < MAX_COL) {
                    col++;
                } else {
                    row++;
                    col = 0;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void clear() {
        for (byte[] row : table) {
            Arrays.fill(row, (byte) 0);
        }
    }

    @Override
    public int getRowNumber() {
        lock.lock();
        try {
            return table[SERVICE_ROW][SERVICE_COL_NEXT_ROW];
        } finally {
            lock.unlock();
        }
    }

    @Override
    public byte[] readSecret(int row) {
        lock.lock();
        try {
            return Arrays.copyOfRange(table[row], SECRET.startIndex, SECRET.startIndex + table[row][SECRET_LEN.startIndex]);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public byte[] readIdentifier(int row) {
        lock.lock();
        try {
            return Arrays.copyOfRange(table[row], IDENTIFIER.startIndex, IDENTIFIER.startIndex + table[row][IDENTIFIER_LEN.startIndex]);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public byte[] readNote(int row) {
        lock.lock();
        try {
            return Arrays.copyOfRange(table[row], NOTE.startIndex, NOTE.startIndex + table[row][NOTE_LEN.startIndex]);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeRow(int row) {
        lock.lock();
        try {
            if (row >= getRowNumber()) {
                throw new IllegalArgumentException("Nonexistent row " + row);
            }

            int lastRowIndex = table[SERVICE_ROW][SERVICE_COL_NEXT_ROW] - (byte) 1;
            for (int i = row; i < lastRowIndex; i++) {
                swapRows(i, i + 1);
            }

            table[SERVICE_ROW][SERVICE_COL_NEXT_ROW] = (byte) lastRowIndex;
        } finally {
            lock.unlock();
        }
    }

    private void swapRows(int fromIndex, int toIndex) {
        byte[] localFromRow = table[fromIndex];
        table[fromIndex] = table[toIndex];
        table[toIndex] = localFromRow;
    }
}
