package de.flashheart.rlgrc.ui.zeus;

import com.google.common.collect.Lists;
import de.flashheart.rlgrc.misc.NumberVerifier;
import de.flashheart.rlgrc.ui.FrameMain;
import org.json.JSONArray;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import java.util.stream.Collectors;

public class CenterFlagsZeus extends ZeusDialog {
    private final JSONArray cp_agents;

    // "operation": "change_score"
//      "team": "blue", or "red"
//      "score": +10 or -10

    public CenterFlagsZeus(JFrame owner, JSONArray cp_agents) {
        super(owner);
        this.cp_agents = cp_agents;
        center.add(add_change_score());
        center.add(new JSeparator(SwingConstants.HORIZONTAL));
        center.add(add_to_neutral());
        pack();
    }

    private JPanel add_to_neutral() {
        //    {
//      "agent": "ag01",
//      "operation": "to_neutral"
//    }
        JPanel pnl = new JPanel(new FlowLayout());
        JLabel lbl = new JLabel("Set to neutral:");
        lbl.setFont(FrameMain.MY_FONT);
        pnl.add(lbl);
        final JComboBox cmb = new JComboBox<>(new Vector<>(cp_agents.toList().stream().sorted().collect(Collectors.toList())));
        cmb.setFont(FrameMain.MY_FONT);
        pnl.add(cmb);
        cmb.addItemListener(e -> {
            zeus_intervention.clear();
            zeus_intervention.put("operation", "to_neutral").put("agent", cmb.getSelectedItem().toString());
        });
        return pnl;
    }

    private JPanel add_change_score() {
        JPanel change_score = new JPanel(new FlowLayout());
        JLabel lbl = new JLabel("Add/Subtract Score:");
        lbl.setFont(FrameMain.MY_FONT);
        change_score.add(lbl);
        final JComboBox cmb = new JComboBox<>(new Vector<>(Lists.newArrayList("Blue", "Red")));
        cmb.setFont(FrameMain.MY_FONT);
        change_score.add(cmb);
        JTextField txtScore = new JTextField("0");
        txtScore.setFont(FrameMain.MY_FONT);
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


}
