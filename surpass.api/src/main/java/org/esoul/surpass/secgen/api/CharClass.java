/*
   Copyright 2017-2025 e-soul.org
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
package org.esoul.surpass.secgen.api;

import java.util.function.IntPredicate;

/**
 * Represents the supported character classes when working with secrets. E.g. when generating secrets.
 * 
 * @author mgp
 */
public enum CharClass {

    DIGIT(c -> Character.isDigit((char) c)),
    ALPHA_UPPER(c -> Character.isLetter((char) c) && Character.isUpperCase((char) c)),
    ALPHA_LOWER(c -> Character.isLetter((char) c) && Character.isLowerCase((char) c)),
    SPECIAL(c -> !Character.isLetter((char) c) && !Character.isDigit((char) c));

    private IntPredicate isInClassPredicate = null;

    private CharClass(IntPredicate inClassPredicate) {
        this.isInClassPredicate = inClassPredicate;
    }

    /**
     * Returns {@code true} if the given character is in this character class and {@code false} otherwise.
     * 
     * @param c The character to check.
     * @return {@code true} if the given character is in this character class and {@code false} otherwise.
     */
    public boolean isInClass(char c) {
        return isInClassPredicate.test(c);
    }

    /**
     * Returns the {@link CharClass} for the given character.
     * 
     * @param c The character to get the {@link CharClass} for.
     * @return The {@link CharClass} for the given character.
     */
    public static CharClass getCharClass(char c) {
        for (CharClass charClass : values()) {
            if (charClass.isInClass(c)) {
                return charClass;
            }
        }
        throw new IllegalArgumentException(String.valueOf(c));
    }
}
