/*
   Copyright 2017-2024 e-soul.org
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
package org.esoul.surpass.app;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.esoul.surpass.table.api.SecretTable;

public class SecretQuery {

    private final SecretTable secretTable;

    public SecretQuery(SecretTable secretTable) {
        this.secretTable = secretTable;
    }

    /**
     * Returns all unique identifiers ordered from the most frequently used to the least.
     * 
     * @return A {@link List} of unique identifiers.
     */
    public List<String> getUniqueIdentifiers() {
        Map<String, Integer> freqMap = new HashMap<>();
        for (int row = 0; row < secretTable.getRowNumber(); row++) {
            byte[] identifierBytes = secretTable.readIdentifier(row);
            String identifier = new String(identifierBytes, StandardCharsets.UTF_8).trim();
            freqMap.merge(identifier, 1, (oldValue, value) -> oldValue + value);
        }
        return freqMap.entrySet().stream().sorted((e1, e2) -> {
            return e2.getValue().compareTo(e1.getValue());
        }).map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
