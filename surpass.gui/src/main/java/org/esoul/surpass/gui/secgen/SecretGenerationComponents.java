package org.esoul.surpass.gui.secgen;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.esoul.surpass.secgen.api.CharClass;

final class SecretGenerationComponents {

    JCheckBox alphaUpperCheckBox;

    JCheckBox alphaLowerCheckBox;

    JCheckBox digitsCheckBox;

    JCheckBox specialCharsCheckBox;

    JLabel lengthLabel;

    JSlider lengthSlider;

    JButton generateButton;

    JTextField secretField;

    void updateLengthLabel() {
        lengthLabel.setText(getLengthLabelValue());
    }

    String getLengthLabelValue() {
        return String.format("%2d", lengthSlider.getValue());
    }

    Collection<CharClass> getSelectedCharClasses() {
        return Stream.of(Map.entry(CharClass.ALPHA_UPPER, alphaUpperCheckBox), Map.entry(CharClass.ALPHA_LOWER, alphaLowerCheckBox), Map.entry(CharClass.DIGIT, digitsCheckBox),
                Map.entry(CharClass.SPECIAL, specialCharsCheckBox)).filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
