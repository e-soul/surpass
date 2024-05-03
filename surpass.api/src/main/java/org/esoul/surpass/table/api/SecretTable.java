/*
   Copyright 2017-2023 e-soul.org
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
package org.esoul.surpass.table.api;

public interface SecretTable {

    /**
     * Creates a row in this table. All input is cleared immediately.
     *
     * @param secret The secret. Usually a password or some sort of cryptographic key.
     * @param identifier The identifier used with the secret. Usually a user-name, email address or some sort of
     *        cryptographic key.
     * @param note A short note. Optional.
     * @throws MaxSizeExceededException In case the maximum size of any of the input is exceeded.
     * @throws EmptySequenceException
     */
    void createRow(char[] secret, char[] identifier, char[] note) throws MaxSizeExceededException, EmptySequenceException;

    /**
     * Updates an existing entry in this table. All input is cleared immediately.
     * 
     * @param row The row to update.
     * @param secret The secret. If {@code null} the secret is not updated.
     * @param identifier The identifier used with the secret.
     * @param note A short note.
     * @throws MaxSizeExceededException In case the maximum size of any of the input is exceeded.
     * @throws EmptySequenceException
     */
    void updateRow(int row, char[] secret, char[] identifier, char[] note) throws MaxSizeExceededException, EmptySequenceException;

    /**
     * Removes an existing row from this table.
     * 
     * @param row The to to remove.
     */
    void removeRow(int row);

    /**
     * Returns the number of allocated/used rows.
     * 
     * @return The number of allocated/used rows.
     */
    int getRowNumber();

    /**
     * Returns the maximum number of secrets this table can hold.
     * 
     * @return The maximum number of secrets this table can hold.
     */
    int getMaxRow();

    byte[] readSecret(int row);

    byte[] readIdentifier(int row);

    byte[] readNote(int row);

    /**
     * Converts the table to a one dimensional array.
     *
     * @return A one dimensional array.
     */
    byte[] toOneDimension();

    /**
     * Clears the current table and loads a one dimensional array into the table.
     *
     * @param sequence A one dimensional array.
     */
    void load(byte[] sequence);
}
