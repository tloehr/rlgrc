package de.flashheart.rlgrc.misc;

import lombok.AllArgsConstructor;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

@AllArgsConstructor
public class StringListVerifier extends InputVerifier {

    @Override
    public boolean verify(JComponent input) {
        String text = ((JTextComponent) input).getText();

        try {
            text = text.replaceAll("\"|[|]", "");
            List<String> tokens = Collections.list(new StringTokenizer(text, " ,")).stream().map(token -> (String) token).collect(Collectors.toList());


            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isInteger(BigDecimal bd) {
        return bd.stripTrailingZeros().scale() <= 0;
    }


}
