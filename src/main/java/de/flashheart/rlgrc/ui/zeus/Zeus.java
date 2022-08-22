package de.flashheart.rlgrc.ui.zeus;

import org.jdesktop.swingx.HorizontalLayout;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class Zeus extends JDialog {

    final List<PropertyChangeListener> propertyChangeListenerList;
    final JSONObject zeus_intervention;
    final JSONObject empty;

    public Zeus(JFrame owner) {
        super(owner, true);
        propertyChangeListenerList = new ArrayList<>();
        zeus_intervention = new JSONObject();
        empty = new JSONObject();
        add(get_button_panel());
    }

    private JPanel get_button_panel() {
        JPanel button_panel = new JPanel(new HorizontalLayout(5));
        add(button_panel, BorderLayout.SOUTH);
        JButton apply = new JButton(new ImageIcon(getClass().getResource("/artwork/apply.png")));
        JButton cancel = new JButton(new ImageIcon(getClass().getResource("/artwork/cancel.png")));
        button_panel.add(apply);
        button_panel.add(cancel);
        apply.addActionListener(e -> {
            propertyChangeListenerList.forEach(propertyChangeListener -> firePropertyChange("zeus", empty, zeus_intervention));
            dispose();
        });
        cancel.addActionListener(e -> dispose());
        return button_panel;
    }

}
