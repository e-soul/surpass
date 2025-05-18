package org.esoul.surpass.core.test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.esoul.surpass.core.SecretGenerator;
import org.esoul.surpass.secgen.api.CharClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("exports")
public class SecretGeneratorTest {

    private SecretGenerator secretGenerator = null;

    @BeforeEach
    public void setUp() {
        secretGenerator = new SecretGenerator();
    }

    public static Stream<Arguments> testGenerateSecretArguments() {
        return Stream.of(Arguments.of(6, List.of(CharClass.ALPHA_LOWER)), Arguments.of(4, List.of(CharClass.SPECIAL)),
                Arguments.of(8, List.of(CharClass.ALPHA_LOWER, CharClass.ALPHA_UPPER)), Arguments.of(12, List.of(CharClass.DIGIT, CharClass.SPECIAL)),
                Arguments.of(2, List.of(CharClass.ALPHA_UPPER, CharClass.SPECIAL)),
                Arguments.of(6, List.of(CharClass.ALPHA_LOWER, CharClass.ALPHA_UPPER, CharClass.DIGIT, CharClass.SPECIAL)),
                Arguments.of(20, List.of(CharClass.ALPHA_LOWER, CharClass.ALPHA_UPPER, CharClass.DIGIT, CharClass.SPECIAL)),
                Arguments.of(4, List.of(CharClass.ALPHA_LOWER, CharClass.ALPHA_UPPER, CharClass.DIGIT)));
    }

    @ParameterizedTest
    @MethodSource("testGenerateSecretArguments")
    public void testGenerateSecret(int secretLength, Collection<CharClass> allowedChars) {
        char[] secret = new char[secretLength];
        secretGenerator.generateSecret(secret, allowedChars);
        checkSecret(secret, allowedChars);
        System.out.println("secret: " + new String(secret));
    }

    private void checkSecret(char[] secret, Collection<CharClass> allowedChars) {
        Map<CharClass, Integer> charClassCounts = new HashMap<>();
        for (char c : secret) {
            CharClass charClass = CharClass.getCharClass(c);
            Integer counts = charClassCounts.getOrDefault(charClass, 0);
            charClassCounts.put(charClass, counts + 1);
        }
        for (CharClass charClass : allowedChars) {
            Integer counts = charClassCounts.remove(charClass);
            if (null == counts || 0 == counts) {
                Assertions.fail(charClass + " char class was not represented in " + new String(secret));
            }
        }
        if (!charClassCounts.isEmpty()) {
            String remainingCharClasses = charClassCounts.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
            Assertions.fail("Not allowed char class[es] exist " + remainingCharClasses + " in " + new String(secret));
        }
    }
}
