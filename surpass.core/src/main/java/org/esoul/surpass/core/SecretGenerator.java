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
package org.esoul.surpass.core;

import java.security.SecureRandom;
import java.util.Collection;

import org.esoul.surpass.secgen.api.CharClass;
import org.esoul.surpass.secgen.api.RandomSecretService;

/**
 * Generates a secret with the following properties. Allowed character classes will be represented by at least one character regardless of the secret length. If
 * {@link CharClass#SPECIAL} is allowed, approximately 10% of the characters will be of class {@link CharClass#SPECIAL}. The other character classes will have
 * approximately equal representation. The secret length cannot be smaller than the number of allowed character classes. Only a subset of the printable special
 * ASCII characters are used.
 *
 * @author mgp
 */
public class SecretGenerator implements RandomSecretService {

    private static final int DIGIT_COUNT = 10;

    private static final int LATIN_LETTER_COUNT = 26;

    private static final int ASCII_ZERO_OFFSET = 48;

    private static final int ASCII_CAPITAL_A_OFFSET = 65;

    private static final int ASCII_SMALL_A_OFFSET = 97;

    private static final float SPECIAL_CLASS_FRACTION = .1f;

    private final char[] specialChars = new char[] { '!', '#', '$', '%', '&', '*', '+', '-', '/', '=', '?', '@' };

    private SecureRandom secureRandom = new SecureRandom();

    @Override
    public void generateSecret(char[] secret, Collection<CharClass> allowedCharClasses) {
        if (secret.length < allowedCharClasses.size()) {
            throw new IllegalArgumentException("Input array cannot be smaller than the number of allowed character classes!");
        }
        int index = 0;
        for (CharClass charClass : allowedCharClasses) {
            int charCount = getCharCount(secret.length, charClass, allowedCharClasses);
            for (int i = 0; i < charCount && index < secret.length; i++) {
                secret[index++] = getRandomChar(charClass);
            }
        }
        for (CharClass[] allowedCharClassesArray = allowedCharClasses.toArray(new CharClass[0]); index < secret.length; index++) {
            secret[index] = getRandomChar(allowedCharClassesArray[secureRandom.nextInt(allowedCharClassesArray.length)]);
        }
        shuffle(secret);
    }

    private int getCharCount(int secretLength, CharClass charClass, Collection<CharClass> allowedCharClasses) {
        if (!allowedCharClasses.contains(CharClass.SPECIAL)) {
            return secretLength / allowedCharClasses.size();
        }
        int specialCharCount = Math.max(1, Math.round(secretLength * SPECIAL_CLASS_FRACTION));
        if (CharClass.SPECIAL == charClass) {
            return specialCharCount;
        }
        return (secretLength - specialCharCount) / (allowedCharClasses.size() - 1);
    }

    private char getRandomChar(CharClass charClass) {
        return switch (charClass) {
        case ALPHA_LOWER -> (char) (secureRandom.nextInt(LATIN_LETTER_COUNT) + ASCII_SMALL_A_OFFSET);
        case ALPHA_UPPER -> (char) (secureRandom.nextInt(LATIN_LETTER_COUNT) + ASCII_CAPITAL_A_OFFSET);
        case DIGIT -> (char) (secureRandom.nextInt(DIGIT_COUNT) + ASCII_ZERO_OFFSET);
        case SPECIAL -> specialChars[secureRandom.nextInt(specialChars.length)];
        };
    }

    private void shuffle(char[] secret) {
        for (int i = secret.length - 1; i >= 0; i--) {
            int j = secureRandom.nextInt(secret.length);
            char other = secret[j];
            secret[j] = secret[i];
            secret[i] = other;
        }
    }
}
