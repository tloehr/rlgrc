package de.flashheart.rlgrc.ui.zeus;

import com.google.common.collect.Lists;
import de.flashheart.rlgrc.misc.NumberVerifier;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class CenterFlagsZeus extends ZeusDialog {

    // "operation": "change_score"
//      "team": "blue", or "red"
//      "score": +10 or -10

    public CenterFlagsZeus(JFrame owner) {
        super(owner);
        center.add(add_change_score());
        center.add(new JSeparator(SwingConstants.HORIZONTAL));
        center.add(add_to_neutral());
        pack();
    }

    private JPanel add_to_neutral() {
        JPanel pnl = new JPanel(new FlowLayout());
        //    {
        //      "agent": "ag01",
        //      "operation": "to_neutral"
        //    }
        return pnl;
    }

    private JPanel add_change_score() {
        JPanel change_score = new JPanel(new FlowLayout());
        JLabel lbl = new JLabel("Add/Subtract Score:");
        lbl.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
        change_score.add(lbl);
        final JComboBox cmb = new JComboBox<>(new Vector<>(Lists.newArrayList("Blue", "Red")));
        cmb.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
        change_score.add(cmb);
        JTextField txtScore = new JTextField("0");
        txtScore.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
        txtScore.setInputVerifier(new NumberVerifier());
        change_score.add(txtScore);
        cmb.addItemListener(e -> {
            zeus_intervention.clear();
            zeus_intervention.put("operation", "change_score").put("team", cmb.getSelectedItem().toString()).put("score", txtScore.getText());
        });
        txtScore.addCaretListener(evt -> {
            zeus_intervention.clear();
            zeus_intervention.put("operation", "change_score").put("team", cmb.getSelectedItem().toString()).put("score", txtScore.getText());
        });
        return change_score;
    }

    @Override
    public void setVisible(boolean b) {
        setLocationRelativeTo(getParent());
        super.setVisible(b);
    }
}
