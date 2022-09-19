/*
 * Created by JFormDesigner on Wed Sep 14 20:12:35 CEST 2022
 */

package de.flashheart.rlgrc.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;

import de.flashheart.rlgrc.misc.JSONConfigs;
import de.flashheart.rlgrc.networking.RestHandler;
import de.flashheart.rlgrc.ui.panels.MyPanels;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.*;
import org.json.JSONObject;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class PnlAgents extends JPanel {
    private final RestHandler restHandler;
    private final JSONConfigs configs;
    boolean selected;
    private Optional<String> selected_agent;

    public PnlAgents(RestHandler restHandler, JSONConfigs configs) {
        this.restHandler = restHandler;
        this.configs = configs;
        this.selected_agent = Optional.empty();
        selected = false;
        initComponents();
        initPanel();
    }


    @Override
    public boolean isShowing() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (!selected) return;
        btnRefreshAgents(null);
    }

    private void initPanel() {
        tblAgents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblAgents.getSelectionModel().addListSelectionListener(e -> table_of_agents_changed_selection(e));

        for (JButton testButton : Arrays.asList(button1, button2, button2, button3, button4, button5, button6, button7, button8, button9, button10, button11, button12, btnStartSignal, btnStopSignal)) {
            testButton.addActionListener(e -> {
                if (selected_agent.isEmpty()) return;
                Properties properties = new Properties();
                properties.put("agentid", selected_agent.get());
                properties.put("deviceid", e.getActionCommand());
                String pattern = "medium";
                if (testButton.equals(btnStartSignal)) pattern = "game_starts";
                if (testButton.equals(btnStopSignal)) pattern = "game_ends";
                properties.put("pattern", pattern);
                restHandler.post("system/test_agent", "{}", properties);
            });
        }
    }

    private void createUIComponents() {
        tblAgents = new JTable(new TM_Agents(new JSONObject(), configs));
    }

    private void btnRefreshAgents(ActionEvent e) {
        JSONObject request = restHandler.get("system/list_agents");
        SwingUtilities.invokeLater(() -> {
            tblAgents.getSelectionModel().clearSelection();
            txtAgent.setText(null);
            ((TM_Agents) tblAgents.getModel()).refresh_agents(request);
            selected_agent = Optional.empty();
        });
    }


    private void btnPowerSaveOn(ActionEvent e) {
        restHandler.post("system/powersave_agents", "", new Properties());
    }

    private void btnPowerSaveOff(ActionEvent e) {
        restHandler.post("system/welcome_agents", "", new Properties());
    }


    private void table_of_agents_changed_selection(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        selected_agent = Optional.empty();
        final DefaultListSelectionModel target = (DefaultListSelectionModel) e.getSource();
        if (target.isSelectionEmpty()) return;
        int selection = target.getAnchorSelectionIndex();
        if (selection < 0) return;
        String state = ((TM_Agents) tblAgents.getModel()).getValueAt(selection);
        txtAgent.setText(tblAgents.getModel().getValueAt(selection, 0) + "\n\n" + state);
        selected_agent = Optional.of(tblAgents.getModel().getValueAt(selection, 0).toString());
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        createUIComponents();

        panel7 = new JSplitPane();
        scrollPane3 = new JScrollPane();
        scrollPane1 = new JScrollPane();
        txtAgent = new JTextArea();
        pnlTesting = new JPanel();
        btnRefreshAgents = new JButton();
        separator1 = new JSeparator();
        vSpacer1 = new JPanel(null);
        btnPowerSaveOn = new JButton();
        btnPowerSaveOff = new JButton();
        vSpacer2 = new JPanel(null);
        button1 = new JButton();
        button2 = new JButton();
        button3 = new JButton();
        button4 = new JButton();
        button5 = new JButton();
        button6 = new JButton();
        button7 = new JButton();
        button8 = new JButton();
        button9 = new JButton();
        button10 = new JButton();
        btnStartSignal = new JButton();
        btnStopSignal = new JButton();
        button11 = new JButton();
        button12 = new JButton();

        //======== this ========
        setLayout(new BorderLayout());

        //======== panel7 ========
        {

            //======== scrollPane3 ========
            {
                scrollPane3.setViewportView(tblAgents);
            }
            panel7.setLeftComponent(scrollPane3);

            //======== scrollPane1 ========
            {

                //---- txtAgent ----
                txtAgent.setWrapStyleWord(true);
                txtAgent.setLineWrap(true);
                txtAgent.setEditable(false);
                scrollPane1.setViewportView(txtAgent);
            }
            panel7.setRightComponent(scrollPane1);
        }
        add(panel7, BorderLayout.CENTER);

        //======== pnlTesting ========
        {
            pnlTesting.setLayout(new BoxLayout(pnlTesting, BoxLayout.PAGE_AXIS));

            //---- btnRefreshAgents ----
            btnRefreshAgents.setText("Update");
            btnRefreshAgents.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
            btnRefreshAgents.setIcon(new ImageIcon(getClass().getResource("/artwork/reload-on.png")));
            btnRefreshAgents.setMaximumSize(new Dimension(32767, 34));
            btnRefreshAgents.setHorizontalAlignment(SwingConstants.LEFT);
            btnRefreshAgents.addActionListener(e -> btnRefreshAgents(e));
            pnlTesting.add(btnRefreshAgents);

            //---- separator1 ----
            separator1.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(separator1);
            pnlTesting.add(vSpacer1);

            //---- btnPowerSaveOn ----
            btnPowerSaveOn.setText("Sleep");
            btnPowerSaveOn.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
            btnPowerSaveOn.setIcon(new ImageIcon(getClass().getResource("/artwork/bluemoon.png")));
            btnPowerSaveOn.setToolTipText("send idle agents to powersave mode");
            btnPowerSaveOn.setMaximumSize(new Dimension(32767, 34));
            btnPowerSaveOn.setHorizontalAlignment(SwingConstants.LEFT);
            btnPowerSaveOn.addActionListener(e -> btnPowerSaveOn(e));
            pnlTesting.add(btnPowerSaveOn);

            //---- btnPowerSaveOff ----
            btnPowerSaveOff.setText("Wake up");
            btnPowerSaveOff.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
            btnPowerSaveOff.setIcon(new ImageIcon(getClass().getResource("/artwork/sun-3-28.png")));
            btnPowerSaveOff.setToolTipText("welcome back to idle agents");
            btnPowerSaveOff.setMaximumSize(new Dimension(32767, 34));
            btnPowerSaveOff.setHorizontalAlignment(SwingConstants.LEFT);
            btnPowerSaveOff.addActionListener(e -> btnPowerSaveOff(e));
            pnlTesting.add(btnPowerSaveOff);
            pnlTesting.add(vSpacer2);

            //---- button1 ----
            button1.setText("White");
            button1.setActionCommand("wht");
            button1.setIcon(new ImageIcon(getClass().getResource("/artwork/led-white-on.png")));
            button1.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button1.setHorizontalAlignment(SwingConstants.LEFT);
            button1.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button1);

            //---- button2 ----
            button2.setText("Red");
            button2.setActionCommand("red");
            button2.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
            button2.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button2.setHorizontalAlignment(SwingConstants.LEFT);
            button2.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button2);

            //---- button3 ----
            button3.setText("Yellow");
            button3.setActionCommand("ylw");
            button3.setIcon(new ImageIcon(getClass().getResource("/artwork/ledyellow.png")));
            button3.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button3.setHorizontalAlignment(SwingConstants.LEFT);
            button3.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button3);

            //---- button4 ----
            button4.setText("Green");
            button4.setActionCommand("grn");
            button4.setIcon(new ImageIcon(getClass().getResource("/artwork/ledgreen.png")));
            button4.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button4.setHorizontalAlignment(SwingConstants.LEFT);
            button4.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button4);

            //---- button5 ----
            button5.setText("Blue");
            button5.setActionCommand("blu");
            button5.setIcon(new ImageIcon(getClass().getResource("/artwork/ledblue.png")));
            button5.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button5.setHorizontalAlignment(SwingConstants.LEFT);
            button5.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button5);

            //---- button6 ----
            button6.setText("Buzzer");
            button6.setActionCommand("buzzer");
            button6.setIcon(new ImageIcon(getClass().getResource("/artwork/buzzer.png")));
            button6.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button6.setHorizontalAlignment(SwingConstants.LEFT);
            button6.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button6);

            //---- button7 ----
            button7.setText("Siren 1");
            button7.setActionCommand("sir1");
            button7.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
            button7.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button7.setHorizontalAlignment(SwingConstants.LEFT);
            button7.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button7);

            //---- button8 ----
            button8.setText("Siren 2");
            button8.setActionCommand("sir2");
            button8.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
            button8.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button8.setHorizontalAlignment(SwingConstants.LEFT);
            button8.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button8);

            //---- button9 ----
            button9.setText("Siren 3");
            button9.setActionCommand("sir3");
            button9.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
            button9.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button9.setHorizontalAlignment(SwingConstants.LEFT);
            button9.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button9);

            //---- button10 ----
            button10.setText("Siren 4");
            button10.setActionCommand("sir4");
            button10.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
            button10.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button10.setHorizontalAlignment(SwingConstants.LEFT);
            button10.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button10);

            //---- btnStartSignal ----
            btnStartSignal.setText("Start Signal");
            btnStartSignal.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
            btnStartSignal.setIcon(new ImageIcon(getClass().getResource("/artwork/up.png")));
            btnStartSignal.setToolTipText(null);
            btnStartSignal.setMaximumSize(new Dimension(32767, 34));
            btnStartSignal.setHorizontalAlignment(SwingConstants.LEFT);
            btnStartSignal.setActionCommand("sir1");
            pnlTesting.add(btnStartSignal);

            //---- btnStopSignal ----
            btnStopSignal.setText("Stop Signal");
            btnStopSignal.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
            btnStopSignal.setIcon(new ImageIcon(getClass().getResource("/artwork/down.png")));
            btnStopSignal.setToolTipText(null);
            btnStopSignal.setMaximumSize(new Dimension(32767, 34));
            btnStopSignal.setHorizontalAlignment(SwingConstants.LEFT);
            btnStopSignal.setActionCommand("sir1");
            pnlTesting.add(btnStopSignal);

            //---- button11 ----
            button11.setText("Play");
            button11.setActionCommand("play");
            button11.setIcon(new ImageIcon(getClass().getResource("/artwork/player_play.png")));
            button11.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button11.setHorizontalAlignment(SwingConstants.LEFT);
            button11.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button11);

            //---- button12 ----
            button12.setText("Stop");
            button12.setActionCommand("stop");
            button12.setIcon(new ImageIcon(getClass().getResource("/artwork/player_stop.png")));
            button12.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            button12.setHorizontalAlignment(SwingConstants.LEFT);
            button12.setMaximumSize(new Dimension(32767, 34));
            pnlTesting.add(button12);
        }
        add(pnlTesting, BorderLayout.EAST);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JSplitPane panel7;
    private JScrollPane scrollPane3;
    private JTable tblAgents;
    private JScrollPane scrollPane1;
    private JTextArea txtAgent;
    private JPanel pnlTesting;
    private JButton btnRefreshAgents;
    private JSeparator separator1;
    private JPanel vSpacer1;
    private JButton btnPowerSaveOn;
    private JButton btnPowerSaveOff;
    private JPanel vSpacer2;
    private JButton button1;
    private JButton button2;
    private JButton button3;
    private JButton button4;
    private JButton button5;
    private JButton button6;
    private JButton button7;
    private JButton button8;
    private JButton button9;
    private JButton button10;
    private JButton btnStartSignal;
    private JButton btnStopSignal;
    private JButton button11;
    private JButton button12;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
