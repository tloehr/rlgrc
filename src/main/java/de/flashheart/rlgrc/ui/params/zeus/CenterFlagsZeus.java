package de.flashheart.rlgrc.ui.params.zeus;

import com.google.common.collect.Lists;
import de.flashheart.rlgrc.misc.NumberVerifier;
import de.flashheart.rlgrc.ui.FrameMain;
import org.jdesktop.swingx.HorizontalLayout;
import org.json.JSONArray;

import javax.swing.*;
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
        center.add(add_change_respawns());
        center.add(new JSeparator(SwingConstants.HORIZONTAL));
        center.add(add_to_neutral());
        pack();
    }

    private JPanel add_to_neutral() {
        JPanel pnl = new JPanel(new HorizontalLayout(5));
        JLabel lbl = new JLabel("Set to neutral:");
        lbl.setFont(FrameMain.MY_FONT);
        final JComboBox cmb = new JComboBox<>(new Vector<>(cp_agents.toList().stream().sorted().collect(Collectors.toList())));
        JButton apply = new JButton(new ImageIcon(getClass().getResource("/artwork/apply.png")));
        cmb.setFont(FrameMain.MY_FONT);
        apply.addActionListener(e -> {
            zeus_intervention.clear();
            zeus_intervention.put("operation", "to_neutral").put("agent", cmb.getSelectedItem().toString());
            super.apply(e.getSource());
        });
        pnl.add(lbl);
        pnl.add(cmb);
        pnl.add(apply);
        return pnl;
    }

    private JPanel add_change_score() {
        JPanel pnl = new JPanel(new HorizontalLayout(5));
        JLabel lbl = new JLabel("+/- seconds:");
        lbl.setFont(FrameMain.MY_FONT);
        final JComboBox cmb = new JComboBox<>(new Vector<>(Lists.newArrayList("Blue", "Red")));
        cmb.setFont(FrameMain.MY_FONT);
        JButton apply = new JButton(new ImageIcon(getClass().getResource("/artwork/apply.png")));
        JTextField txtScore = new JTextField("0");
        txtScore.setFont(FrameMain.MY_FONT);
        txtScore.setInputVerifier(new NumberVerifier());
        apply.addActionListener(e -> {
            zeus_intervention.clear();
            zeus_intervention.put("operation", "add_seconds").put("team", cmb.getSelectedItem().toString()).put("amount", txtScore.getText());
            super.apply(e.getSource());
        });
        pnl.add(lbl);
        pnl.add(cmb);
        pnl.add(txtScore);
        pnl.add(apply);
        return pnl;
    }


    private JPanel add_change_respawns() {
        JPanel pnl = new JPanel(new HorizontalLayout(5));
        JLabel lbl = new JLabel("+/- respawns:");
        lbl.setFont(FrameMain.MY_FONT);
        final JComboBox cmb = new JComboBox<>(new Vector<>(Lists.newArrayList("Blue", "Red")));
        cmb.setFont(FrameMain.MY_FONT);
        JButton apply = new JButton(new ImageIcon(getClass().getResource("/artwork/apply.png")));
        JTextField txtScore = new JTextField("0");
        txtScore.setFont(FrameMain.MY_FONT);
        txtScore.setInputVerifier(new NumberVerifier());
        apply.addActionListener(e -> {
            zeus_intervention.clear();
            zeus_intervention.put("operation", "add_respawns").put("team", cmb.getSelectedItem().toString()).put("amount", txtScore.getText());
            super.apply(e.getSource());
        });
        pnl.add(lbl);
        pnl.add(cmb);
        pnl.add(txtScore);
        pnl.add(apply);
        return pnl;
    }

}
