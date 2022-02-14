/*
 * Created by JFormDesigner on Sun Feb 13 18:06:24 CET 2022
 */

package de.flashheart.rlgrc.ui;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import javax.swing.*;

import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import de.flashheart.rlgrc.misc.NotEmptyVerifier;
import de.flashheart.rlgrc.misc.NumberVerifier;
import org.json.JSONObject;

/**
 * @author Torsten Löhr
 */
public class ConquestParams extends JPanel implements GameParams {
    public ConquestParams() {
        initComponents();
        initPanel();
    }

    void initPanel() {

    }

    private void setVerifiers() {
        txtComment.setInputVerifier(new NotEmptyVerifier());
        txtCnqTickets.setInputVerifier(new NumberVerifier());
        txtCnqTPrice.setInputVerifier(new NumberVerifier(BigDecimal.ONE, NumberVerifier.MAX, false));
        txtCnqBleedStarts.setInputVerifier(new NumberVerifier());
        txtCnqSBleedInt.setInputVerifier(new NumberVerifier(BigDecimal.ONE, NumberVerifier.MAX, false));
        txtCnqEBleedInt.setInputVerifier(new NumberVerifier(BigDecimal.ZERO, NumberVerifier.MAX, false));
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
        lblRedSpawn = new JTextField();
        lblBlueSpawn = new JTextField();

        //======== this ========
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        //======== pnlConquest ========
        {
            pnlConquest.setLayout(new FormLayout(
                "pref:grow, $rgap, default, $ugap, pref:grow, $rgap, default",
                "4*(default, $lgap), fill:default:grow"));
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
                    scrollPane1.setViewportView(txtCPs);
                }
                pnl1234.add(scrollPane1, CC.xywh(1, 3, 3, 3, CC.DEFAULT, CC.FILL));

                //======== scrollPane2 ========
                {

                    //---- txtSirens ----
                    txtSirens.setLineWrap(true);
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

                //---- lblRedSpawn ----
                lblRedSpawn.setText("test");
                lblRedSpawn.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 22));
                lblRedSpawn.setAlignmentX(0.5F);
                lblRedSpawn.setBackground(new Color(255, 0, 51));
                lblRedSpawn.setOpaque(true);
                lblRedSpawn.setForeground(new Color(255, 255, 51));
                lblRedSpawn.setHorizontalAlignment(SwingConstants.CENTER);
                pnl1234.add(lblRedSpawn, CC.xywh(1, 7, 3, 1, CC.DEFAULT, CC.FILL));

                //---- lblBlueSpawn ----
                lblBlueSpawn.setText("test");
                lblBlueSpawn.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 22));
                lblBlueSpawn.setAlignmentX(0.5F);
                lblBlueSpawn.setBackground(new Color(51, 51, 255));
                lblBlueSpawn.setOpaque(true);
                lblBlueSpawn.setForeground(new Color(255, 255, 51));
                lblBlueSpawn.setHorizontalAlignment(SwingConstants.CENTER);
                pnl1234.add(lblBlueSpawn, CC.xywh(5, 7, 3, 1, CC.FILL, CC.FILL));
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
    public void set_parameters(JSONObject params) {

    }

    @Override
    public JSONObject get_parameters() {
        return null;
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
    private JTextField lblRedSpawn;
    private JTextField lblBlueSpawn;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
