package de.flashheart.rlgrc.misc;

import javax.swing.*;
import javax.swing.text.JTextComponent;

public class NotEmptyVerifier extends InputVerifier {
    @Override
    public boolean verify(JComponent input) {
        String text = ((JTextComponent) input).getText();
        return !text.trim().isEmpty();
    }
}
