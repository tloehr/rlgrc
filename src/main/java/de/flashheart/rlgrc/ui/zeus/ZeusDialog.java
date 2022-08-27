package de.flashheart.rlgrc.ui.zeus;

import org.jdesktop.swingx.HorizontalLayout;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class ZeusDialog extends JDialog {

    final List<PropertyChangeListener> propertyChangeListenerList;
    final JSONObject zeus_intervention;
    final JSONObject empty;
    final JPanel center;

    public ZeusDialog(JFrame owner) {
        super(owner, true);
        setLayout(new BorderLayout());
        center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.PAGE_AXIS));
        add(center, BorderLayout.CENTER);
        add(get_button_panel(), BorderLayout.SOUTH);
        propertyChangeListenerList = new ArrayList<>();
        zeus_intervention = new JSONObject();
        empty = new JSONObject();
    }

    protected JPanel get_button_panel() {
        JPanel button_panel = new JPanel(new HorizontalLayout(5));
        JButton apply = new JButton(new ImageIcon(getClass().getResource("/artwork/apply.png")));
        JButton cancel = new JButton(new ImageIcon(getClass().getResource("/artwork/cancel.png")));
        button_panel.add(apply);
        button_panel.add(cancel);
        apply.addActionListener(e -> {
            propertyChangeListenerList.forEach(propertyChangeListener -> propertyChangeListener.propertyChange(new PropertyChangeEvent(e.getSource(), "zeus", empty, zeus_intervention)));
            dispose();
        });
        cancel.addActionListener(e -> dispose());
        return button_panel;
    }

    public void add_property_change_listener(PropertyChangeListener propertyChangeListener) {
        propertyChangeListenerList.add(propertyChangeListener);
    }

    @Override
    public void setVisible(boolean b) {
        setLocationRelativeTo(getParent());
        super.setVisible(b);
    }

}
