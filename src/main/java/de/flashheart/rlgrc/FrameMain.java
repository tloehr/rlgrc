/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import de.flashheart.rlgrc.games.Agents;
import de.flashheart.rlgrc.games.Conquest;
import lombok.extern.log4j.Log4j2;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class FrameMain extends JFrame {
    private final CardLayout cardLayout;

    private static final String REST_URI
            = "http://localhost:8090/api/game/load";

    private Client client = ClientBuilder.newClient();

    private String json = "{\n" +
            "  \"comment\": \"7 CPs, BF1 style\",\n" +
            "  \"rest_url\": \"http://localhost:8090/api/game/load\",\n" +
            "  \"class\": \"de.flashheart.rlg.commander.games.Conquest\",\n" +
            "  \"agents\": {\n" +
            "    \"capture_points\": [\n" +
            "      \"ag01\",\n" +
            "      \"ag02\",\n" +
            "      \"ag03\",\n" +
            "      \"ag04\",\n" +
            "      \"ag05\",\n" +
            "      \"ag09\",\n" +
            "      \"ag10\"\n" +
            "    ],\n" +
            "    \"red_spawn\": [\n" +
            "      \"ag06\"\n" +
            "    ],\n" +
            "    \"blue_spawn\": [\n" +
            "      \"ag07\"\n" +
            "    ],\n" +
            "    \"spawns\": [\n" +
            "      \"ag06\",\n" +
            "      \"ag07\"\n" +
            "    ],\n" +
            "    \"sirens\": [\n" +
            "      \"ag08\"\n" +
            "    ]\n" +
            "  },\n" +
            "  \"respawn_tickets\": 250,\n" +
            "  \"not_bleeding_before_cps\": 1,\n" +
            "  \"start_bleed_interval\": 5,\n" +
            "  \"end_bleed_interval\": 0.5,\n" +
            "  \"ticket_price_for_respawn\": 1\n" +
            "}";

    public FrameMain() {
        initComponents();
        cardLayout = (CardLayout) mainPanel.getLayout();
    }

    private void btnConquest(ActionEvent e) {
        cardLayout.show(mainPanel, "conquest");
    }

    private void btnRush(ActionEvent e) {
        cardLayout.show(mainPanel, "rush");
    }

    private void btnSetup(ActionEvent e) {
        cardLayout.show(mainPanel, "setup");
    }


    private void btnSend(ActionEvent e) {
        String id = "g1";
        Agents agents = new Agents(new String[]{"ag01"}, new String[]{"ag01"}, new String[]{"ag01"}, new String[]{"ag01"}, new String[]{"ag01"});
        Response response = client
                .target(REST_URI)
                .queryParam("id", id)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(new Conquest("", agents, 250,
                        1, 5d, 0.5d, 1d)));

        log.debug(response);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        mainPanel = new JPanel();
        pnlSetup = new JPanel();
        label6 = new JLabel();
        pnlConquest = new JPanel();
        label1 = new JLabel();
        label3 = new JLabel();
        label4 = new JLabel();
        label5 = new JLabel();
        pnlRush = new JPanel();
        label2 = new JLabel();
        buttonPanel = new JPanel();
        btnSetup = new JButton();
        btnConquest = new JButton();
        btnRush = new JButton();
        btnSend = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "2dlu, default:grow, $rgap",
            "$nlgap, default:grow, $lgap, default, $nlgap, default"));

        //======== mainPanel ========
        {
            mainPanel.setLayout(new CardLayout());

            //======== pnlSetup ========
            {
                pnlSetup.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "2*(default, $lgap), default"));

                //---- label6 ----
                label6.setText("Setup Connection");
                label6.setFont(new Font("sansserif", Font.BOLD, 20));
                pnlSetup.add(label6, CC.xywh(1, 1, 3, 1));
            }
            mainPanel.add(pnlSetup, "setup");

            //======== pnlConquest ========
            {
                pnlConquest.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "3*(default, $lgap), default"));

                //---- label1 ----
                label1.setText("Conquest");
                label1.setFont(new Font("sansserif", Font.BOLD, 20));
                pnlConquest.add(label1, CC.xywh(1, 1, 3, 1));

                //---- label3 ----
                label3.setText("text");
                pnlConquest.add(label3, CC.xy(1, 3));

                //---- label4 ----
                label4.setText("text");
                pnlConquest.add(label4, CC.xy(1, 5));

                //---- label5 ----
                label5.setText("text");
                pnlConquest.add(label5, CC.xy(1, 7));
            }
            mainPanel.add(pnlConquest, "conquest");

            //======== pnlRush ========
            {
                pnlRush.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "default, $lgap, default:grow, $lgap, default"));

                //---- label2 ----
                label2.setText("Rush");
                label2.setFont(new Font("sansserif", Font.BOLD, 20));
                pnlRush.add(label2, CC.xy(1, 1));
            }
            mainPanel.add(pnlRush, "rush");
        }
        contentPane.add(mainPanel, CC.xy(2, 2, CC.DEFAULT, CC.FILL));

        //======== buttonPanel ========
        {
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

            //---- btnSetup ----
            btnSetup.setText("Setup");
            btnSetup.addActionListener(e -> btnSetup(e));
            buttonPanel.add(btnSetup);

            //---- btnConquest ----
            btnConquest.setText("Conquest");
            btnConquest.addActionListener(e -> btnConquest(e));
            buttonPanel.add(btnConquest);

            //---- btnRush ----
            btnRush.setText("Rush");
            btnRush.addActionListener(e -> btnRush(e));
            buttonPanel.add(btnRush);
        }
        contentPane.add(buttonPanel, CC.xy(2, 4));

        //---- btnSend ----
        btnSend.setText("Send");
        btnSend.addActionListener(e -> btnSend(e));
        contentPane.add(btnSend, CC.xy(2, 6));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel mainPanel;
    private JPanel pnlSetup;
    private JLabel label6;
    private JPanel pnlConquest;
    private JLabel label1;
    private JLabel label3;
    private JLabel label4;
    private JLabel label5;
    private JPanel pnlRush;
    private JLabel label2;
    private JPanel buttonPanel;
    private JButton btnSetup;
    private JButton btnConquest;
    private JButton btnRush;
    private JButton btnSend;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
