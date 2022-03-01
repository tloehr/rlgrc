/*
 * Created by JFormDesigner on Sun Feb 13 18:06:24 CET 2022
 */

package de.flashheart.rlgrc.ui;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import de.flashheart.rlgrc.misc.NotEmptyVerifier;
import de.flashheart.rlgrc.misc.NumberVerifier;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * @author Torsten Löhr
 */
public class ConquestParams extends GameParams {
    public ConquestParams() {
        super();
        initComponents();
        initPanel();
    }

    void initPanel() {
        load_defaults();
        txtComment.setInputVerifier(new NotEmptyVerifier());
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

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        pnlConquest = new JPanel();
        txtComment = new JTextField();
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
        txtRedSpawn = new JTextField();
        txtBlueSpawn = new JTextField();

        //======== this ========
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        //======== pnlConquest ========
        {
            pnlConquest.setLayout(new FormLayout(
                "pref, $rgap, default:grow, $ugap, pref, $rgap, default:grow",
                "4*(default, $lgap), fill:default:grow"));

            //---- txtComment ----
            txtComment.setFont(new Font(".SF NS Text", Font.PLAIN, 16));
            pnlConquest.add(txtComment, CC.xywh(1, 1, 7, 1));

            //---- label3 ----
            label3.setText("Respawn Tickets");
            pnlConquest.add(label3, CC.xy(1, 3));
            pnlConquest.add(txtCnqTickets, CC.xy(3, 3));

            //---- label4 ----
            label4.setText("Bleeding starts @");
            pnlConquest.add(label4, CC.xy(5, 3));
            pnlConquest.add(txtCnqBleedStarts, CC.xy(7, 3));

            //---- label5 ----
            label5.setText("Start Bleed interval");
            pnlConquest.add(label5, CC.xy(1, 5));
            pnlConquest.add(txtCnqSBleedInt, CC.xy(3, 5));

            //---- lbl12345 ----
            lbl12345.setText("Final Bleed interval");
            pnlConquest.add(lbl12345, CC.xy(5, 5));
            pnlConquest.add(txtCnqEBleedInt, CC.xy(7, 5));

            //---- label9 ----
            label9.setText("Ticket Price");
            pnlConquest.add(label9, CC.xy(1, 7));
            pnlConquest.add(txtCnqTPrice, CC.xy(3, 7));

            //======== pnl1234 ========
            {
                pnl1234.setLayout(new FormLayout(
                    "70dlu:grow, $lcgap, 20dlu, $ugap, 70dlu:grow, $lcgap, 20dlu",
                    "15dlu, $rgap, 2*(default), default:grow, $lgap, default"));
                ((FormLayout)pnl1234.getLayout()).setColumnGroups(new int[][] {{1, 5}});

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

                //---- txtRedSpawn ----
                txtRedSpawn.setText("test");
                txtRedSpawn.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 22));
                txtRedSpawn.setAlignmentX(0.5F);
                txtRedSpawn.setBackground(new Color(255, 0, 51));
                txtRedSpawn.setOpaque(true);
                txtRedSpawn.setForeground(new Color(255, 255, 51));
                txtRedSpawn.setHorizontalAlignment(SwingConstants.CENTER);
                pnl1234.add(txtRedSpawn, CC.xywh(1, 7, 3, 1, CC.DEFAULT, CC.FILL));

                //---- txtBlueSpawn ----
                txtBlueSpawn.setText("test");
                txtBlueSpawn.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 22));
                txtBlueSpawn.setAlignmentX(0.5F);
                txtBlueSpawn.setBackground(new Color(51, 51, 255));
                txtBlueSpawn.setOpaque(true);
                txtBlueSpawn.setForeground(new Color(255, 255, 51));
                txtBlueSpawn.setHorizontalAlignment(SwingConstants.CENTER);
                pnl1234.add(txtBlueSpawn, CC.xywh(5, 7, 3, 1, CC.FILL, CC.FILL));
            }
            pnlConquest.add(pnl1234, CC.xywh(1, 9, 7, 1, CC.FILL, CC.DEFAULT));
        }
        add(pnlConquest);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    @Override
    public String getMode() {
        return "conquest";
    }

    @Override
    void set_parameters() {
        txtComment.setText(params.getString("comment"));
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
    JSONObject read_parameters() {
        params.clear();
        params.put("respawn_tickets", Integer.parseInt(txtCnqTickets.getText()));
        params.put("ticket_price_for_respawn", Integer.parseInt(txtCnqTPrice.getText()));
        params.put("not_bleeding_before_cps", Integer.parseInt(txtCnqBleedStarts.getText()));
        params.put("start_bleed_interval", Double.parseDouble(txtCnqSBleedInt.getText()));
        params.put("end_bleed_interval", Double.parseDouble(txtCnqEBleedInt.getText()));
        params.put("comment", txtComment.getText());

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

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel pnlConquest;
    private JTextField txtComment;
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
    private JTextField txtRedSpawn;
    private JTextField txtBlueSpawn;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
