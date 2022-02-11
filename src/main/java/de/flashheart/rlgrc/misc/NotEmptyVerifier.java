package de.flashheart.rlgrc.misc;

import javax.swing.*;

public class NotEmptyVerifier extends InputVerifier {
    @Override
    public boolean verify(JComponent input) {
        String text = ((JTextField) input).getText();
        return !text.trim().isEmpty();
    }
}
