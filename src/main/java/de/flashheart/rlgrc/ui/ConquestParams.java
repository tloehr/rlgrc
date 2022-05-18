/*
 * Created by JFormDesigner on Sun Feb 13 18:06:24 CET 2022
 */

package de.flashheart.rlgrc.ui;

import java.awt.event.*;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import de.flashheart.rlgrc.misc.HTML;
import de.flashheart.rlgrc.misc.JavaTimeConverter;
import de.flashheart.rlgrc.misc.NotEmptyVerifier;
import de.flashheart.rlgrc.misc.NumberVerifier;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.units.qual.C;
import org.jdesktop.swingx.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class ConquestParams extends GameParams {

    public ConquestParams() {
        super();
        add(default_components);
        initComponents();
        initPanel();
    }

    private void initPanel() {
        load_defaults();
        txtCnqTickets.setInputVerifier(new NumberVerifier());
        txtCnqTPrice.setInputVerifier(new NumberVerifier(BigDecimal.ONE, NumberVerifier.MAX, false));
        txtCnqBleedStarts.setInputVerifier(new NumberVerifier());
        txtCnqSBleedInt.setInputVerifier(new NumberVerifier(BigDecimal.ONE, NumberVerifier.MAX, false));
        txtCnqEBleedInt.setInputVerifier(new NumberVerifier(BigDecimal.ZERO, NumberVerifier.MAX, false));
        txtCPs.setInputVerifier(new NotEmptyVerifier());
        txtSirens.setInputVerifier(new NotEmptyVerifier());
        txtRedSpawn.setInputVerifier(new NotEmptyVerifier());
        txtBlueSpawn.setInputVerifier(new NotEmptyVerifier());
    }

    private void btnSwitchSides(ActionEvent e) {
        String a = txtBlueSpawn.getText();
        txtBlueSpawn.setText(txtRedSpawn.getText());
        txtRedSpawn.setText(a);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        pnlConquest = new JPanel();
        label3 = new JLabel();
        txtCnqTickets = new JTextField();
        label4 = new JLabel();
        txtCnqBleedStarts = new JTextField();
        label5 = new JLabel();
        txtCnqSBleedInt = new JTextField();
        lbl12345 = new JLabel();
        txtCnqEBleedInt = new JTextField();
        label9 = new JLabel();
        txtCnqTPrice = new JTextField();
        pnl1234 = new JPanel();
        label10 = new JLabel();
        scrollPane1 = new JScrollPane();
        txtCPs = new JTextArea();
        scrollPane2 = new JScrollPane();
        txtSirens = new JTextArea();
        label13 = new JLabel();
        panel1 = new JPanel();
        txtRedSpawn = new JTextField();
        btnSwitchSides = new JButton();
        txtBlueSpawn = new JTextField();

        //======== this ========
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        //======== pnlConquest ========
        {
            pnlConquest.setLayout(new FormLayout(
                    "pref, $rgap, default:grow, $ugap, pref, $rgap, default:grow",
                    "3*(default, $lgap), fill:default:grow"));

            //---- label3 ----
            label3.setText("Respawn Tickets");
            pnlConquest.add(label3, CC.xy(1, 1));
            pnlConquest.add(txtCnqTickets, CC.xy(3, 1));

            //---- label4 ----
            label4.setText("Bleeding starts @");
            pnlConquest.add(label4, CC.xy(5, 1));
            pnlConquest.add(txtCnqBleedStarts, CC.xy(7, 1));

            //---- label5 ----
            label5.setText("Start Bleed interval");
            pnlConquest.add(label5, CC.xy(1, 3));
            pnlConquest.add(txtCnqSBleedInt, CC.xy(3, 3));

            //---- lbl12345 ----
            lbl12345.setText("Final Bleed interval");
            pnlConquest.add(lbl12345, CC.xy(5, 3));
            pnlConquest.add(txtCnqEBleedInt, CC.xy(7, 3));

            //---- label9 ----
            label9.setText("Ticket Price");
            pnlConquest.add(label9, CC.xy(1, 5));
            pnlConquest.add(txtCnqTPrice, CC.xy(3, 5));

            //======== pnl1234 ========
            {
                pnl1234.setLayout(new FormLayout(
                        "70dlu:grow, $lcgap, 20dlu, $ugap, 70dlu:grow, $lcgap, 20dlu",
                        "15dlu, $rgap, 2*(default), default:grow, $lgap, default"));
                ((FormLayout) pnl1234.getLayout()).setColumnGroups(new int[][]{{1, 5}});

                //---- label10 ----
                label10.setText("Capture Points");
                label10.setBackground(SystemColor.windowBorder);
                label10.setForeground(Color.black);
                label10.setOpaque(true);
                label10.setHorizontalAlignment(SwingConstants.CENTER);
                pnl1234.add(label10, CC.xywh(1, 1, 3, 1, CC.DEFAULT, CC.FILL));

                //======== scrollPane1 ========
                {

                    //---- txtCPs ----
                    txtCPs.setLineWrap(true);
                    txtCPs.setWrapStyleWord(true);
                    scrollPane1.setViewportView(txtCPs);
                }
                pnl1234.add(scrollPane1, CC.xywh(1, 3, 3, 3, CC.DEFAULT, CC.FILL));

                //======== scrollPane2 ========
                {

                    //---- txtSirens ----
                    txtSirens.setLineWrap(true);
                    txtSirens.setWrapStyleWord(true);
                    scrollPane2.setViewportView(txtSirens);
                }
                pnl1234.add(scrollPane2, CC.xywh(5, 3, 3, 3));

                //---- label13 ----
                label13.setText("Sirens");
                label13.setBackground(SystemColor.windowBorder);
                label13.setForeground(Color.black);
                label13.setOpaque(true);
                label13.setHorizontalAlignment(SwingConstants.CENTER);
                pnl1234.add(label13, CC.xywh(5, 1, 3, 1, CC.DEFAULT, CC.FILL));

                //======== panel1 ========
                {
                    panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));

                    //---- txtRedSpawn ----
                    txtRedSpawn.setText("test");
                    txtRedSpawn.setFont(new Font(".SF NS", Font.PLAIN, 22));
                    txtRedSpawn.setAlignmentX(0.5F);
                    txtRedSpawn.setBackground(new Color(255, 0, 51));
                    txtRedSpawn.setOpaque(true);
                    txtRedSpawn.setForeground(new Color(255, 255, 51));
                    txtRedSpawn.setHorizontalAlignment(SwingConstants.CENTER);
                    panel1.add(txtRedSpawn);

                    //---- btnSwitchSides ----
                    btnSwitchSides.setText(null);
                    btnSwitchSides.setIcon(new ImageIcon(getClass().getResource("/artwork/lc_arrowshapes.png")));
                    btnSwitchSides.setToolTipText("switch sides");
                    btnSwitchSides.addActionListener(e -> btnSwitchSides(e));
                    panel1.add(btnSwitchSides);

                    //---- txtBlueSpawn ----
                    txtBlueSpawn.setText("test");
                    txtBlueSpawn.setFont(new Font(".SF NS", Font.PLAIN, 22));
                    txtBlueSpawn.setAlignmentX(0.5F);
                    txtBlueSpawn.setBackground(new Color(51, 51, 255));
                    txtBlueSpawn.setOpaque(true);
                    txtBlueSpawn.setForeground(new Color(255, 255, 51));
                    txtBlueSpawn.setHorizontalAlignment(SwingConstants.CENTER);
                    panel1.add(txtBlueSpawn);
                }
                pnl1234.add(panel1, CC.xywh(1, 7, 7, 1));
            }
            pnlConquest.add(pnl1234, CC.xywh(1, 7, 7, 1, CC.FILL, CC.DEFAULT));
        }
        add(pnlConquest);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    @Override
    public String getMode() {
        return "conquest";
    }

    @Override
    protected void set_parameters() {
        super.set_parameters();
        txtCnqTickets.setText(params.get("respawn_tickets").toString());
        txtCnqTPrice.setText(params.get("ticket_price_for_respawn").toString());
        txtCnqBleedStarts.setText(params.get("not_bleeding_before_cps").toString());
        txtCnqSBleedInt.setText(params.get("start_bleed_interval").toString());
        txtCnqEBleedInt.setText(params.get("end_bleed_interval").toString());
        txtCPs.setText(to_string_list(params.getJSONObject("agents").getJSONArray("capture_points")));
        txtSirens.setText(to_string_list(params.getJSONObject("agents").getJSONArray("sirens")));
        txtRedSpawn.setText(params.getJSONObject("agents").getJSONArray("red_spawn").getString(0));
        txtBlueSpawn.setText(params.getJSONObject("agents").getJSONArray("blue_spawn").getString(0));
    }

    @Override
    protected JSONObject read_parameters() {
        super.read_parameters();
        params.put("respawn_tickets", Integer.parseInt(txtCnqTickets.getText()));
        params.put("ticket_price_for_respawn", Integer.parseInt(txtCnqTPrice.getText()));
        params.put("not_bleeding_before_cps", Integer.parseInt(txtCnqBleedStarts.getText()));
        params.put("start_bleed_interval", Double.parseDouble(txtCnqSBleedInt.getText()));
        params.put("end_bleed_interval", Double.parseDouble(txtCnqEBleedInt.getText()));

        JSONObject agents = new JSONObject();
        agents.put("capture_points", to_jsonarray(txtCPs.getText()));
        agents.put("sirens", to_jsonarray(txtSirens.getText()));
        agents.put("red_spawn", new JSONArray().put(txtRedSpawn.getText()));
        agents.put("blue_spawn", new JSONArray().put(txtBlueSpawn.getText()));
        agents.put("spawns", new JSONArray().put(txtRedSpawn.getText()).put(txtBlueSpawn.getText()));
        params.put("agents", agents);

        params.put("class", "de.flashheart.rlg.commander.games.Conquest");
        params.put("mode", getMode());
        return params;
    }

    @Override
    protected String get_score_as_html(JSONObject game_state) {

        String blue_flags = "";
        String red_flags = "";

        for (Object cp : game_state.getJSONArray("cps_held_by_red").toList()) {
            red_flags += HTML.li(cp.toString());
        }

        for (Object cp : game_state.getJSONArray("cps_held_by_blue").toList()) {
            blue_flags += HTML.li(cp.toString());
        }

        if (blue_flags.isEmpty()) blue_flags = HTML.li("none");
        if (red_flags.isEmpty()) red_flags = HTML.li("none");

        JSONObject firstEvent = game_state.getJSONArray("in_game_events").getJSONObject(0);
        LocalDateTime first_pit = JavaTimeConverter.from_iso8601(firstEvent.getString("pit"));
        String html =
                HTML.document(CSS,
                        HTML.h1("Conquest@"+first_pit.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))) +
                                HTML.h2("Team Red &#8594; %s  &#8660; %s &#8592; Team Blue") +
                                HTML.h2("Capture points") +
                                HTML.h3("Team Red") +
                                HTML.ul(red_flags) +
                                HTML.h3("Team Blue") +
                                HTML.ul(blue_flags) +
                                HTML.h4("Number of Respawns") +
                                "Team Red: %s" + HTML.linebreak() +
                                "Team Blue: %s" +
                                HTML.h2("Events") +
                                generate_table_for_events(game_state.getJSONArray("in_game_events"))
                );
        return String.format(html, game_state.getInt("remaining_red_tickets"), game_state.getInt("remaining_blue_tickets"), game_state.getInt("red_respawns"), game_state.getInt("blue_respawns"));
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel pnlConquest;
    private JLabel label3;
    private JTextField txtCnqTickets;
    private JLabel label4;
    private JTextField txtCnqBleedStarts;
    private JLabel label5;
    private JTextField txtCnqSBleedInt;
    private JLabel lbl12345;
    private JTextField txtCnqEBleedInt;
    private JLabel label9;
    private JTextField txtCnqTPrice;
    private JPanel pnl1234;
    private JLabel label10;
    private JScrollPane scrollPane1;
    private JTextArea txtCPs;
    private JScrollPane scrollPane2;
    private JTextArea txtSirens;
    private JLabel label13;
    private JPanel panel1;
    private JTextField txtRedSpawn;
    private JButton btnSwitchSides;
    private JTextField txtBlueSpawn;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
