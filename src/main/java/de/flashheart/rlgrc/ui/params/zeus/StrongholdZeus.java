package de.flashheart.rlgrc.ui.params.zeus;

import de.flashheart.rlgrc.ui.FrameMain;
import org.jdesktop.swingx.HorizontalLayout;
import org.json.JSONArray;

import javax.swing.*;
import java.util.Vector;
import java.util.stream.Collectors;

public class StrongholdZeus extends ZeusDialog {
    private final JSONArray cp_agents;

    public StrongholdZeus(JFrame owner, JSONArray cp_agents) {
        super(owner);
        this.cp_agents = cp_agents;
        center.add(add_to_neutral());
        pack();
    }

    private JPanel add_to_neutral() {
        JPanel pnl = new JPanel(new HorizontalLayout(5));
        JLabel lbl = new JLabel("Defuse:");
        lbl.setFont(FrameMain.MY_FONT);
        //todo: fixme
        final JComboBox cmb = new JComboBox<>(new Vector<>(cp_agents.toList().stream().sorted().collect(Collectors.toList())));
        JButton apply = new JButton(new ImageIcon(getClass().getResource("/artwork/apply.png")));
        cmb.setFont(FrameMain.MY_FONT);
        apply.addActionListener(e -> {
            zeus_intervention.clear();
            zeus_intervention.put("operation", "defuse").put("agent", cmb.getSelectedItem().toString());
            super.apply(e.getSource());
        });
        pnl.add(lbl);
        pnl.add(cmb);
        pnl.add(apply);
        return pnl;
    }



}
