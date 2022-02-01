/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import de.flashheart.rlgrc.games.Agents;
import de.flashheart.rlgrc.games.GameParams;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class FrameMain extends JFrame {
    private final CardLayout cardLayout;

    private static final String REST_URI
            = "http://localhost:8090/api/system/test";

    private Client client = ClientBuilder.newClient();


    public FrameMain() {
        initComponents();
        cardLayout = (CardLayout) cardPanel.getLayout();
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

        GameParams conquestParams = new GameParams("1", "3");
//                new ConquestParams(250,
//                1, 5d, 0.5d, 1d);
        conquestParams.add_agents("capture_points", "ag01", "ag02", "ag03", "ag04", "ag05", "ag09", "ag10");
        conquestParams.add_spawn("red", "ag06");
        conquestParams.add_spawn("blue", "ag07");
        conquestParams.add_sirens("ag08");

        log.debug(conquestParams.toString());

//        Response response = client
//                .target(REST_URI)
//                .queryParam("id", id)
//                .request(MediaType.APPLICATION_JSON)
//                .post(Entity.json("some string"));


//        ObjectMapper mapper = JsonMapper.builder() // or mapper for other formats
//                .addModule(new GuavaModule())
//                .build();
//
//        String jsonResult = null;
//        try {
//            jsonResult = mapper.writerWithDefaultPrettyPrinter()
//                    .writeValueAsString(conquestParams);
//            log.debug(jsonResult);
//        } catch (JsonProcessingException ex) {
//            ex.printStackTrace();
//        }


    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        buttonPanel = new JPanel();
        btnSetup = new JButton();
        btnConquest = new JButton();
        btnRush = new JButton();
        hSpacer1 = new JPanel(null);
        tbKeyLock = new JToggleButton();
        separator1 = new JSeparator();
        mainPanel = new JPanel();
        cardPanel = new JPanel();
        pnlConquest = new JPanel();
        label1 = new JLabel();
        label3 = new JLabel();
        textField2 = new JTextField();
        label4 = new JLabel();
        textField3 = new JTextField();
        label5 = new JLabel();
        textField4 = new JTextField();
        label8 = new JLabel();
        textField5 = new JTextField();
        label9 = new JLabel();
        textField6 = new JTextField();
        panel1 = new JPanel();
        label10 = new JLabel();
        label13 = new JLabel();
        label11 = new JLabel();
        scrollPane1 = new JScrollPane();
        list1 = new JList();
        scrollPane2 = new JScrollPane();
        list2 = new JList();
        comboBox1 = new JComboBox();
        label12 = new JLabel();
        comboBox2 = new JComboBox();
        panel3 = new JPanel();
        btnAddCP = new JButton();
        btnDelCP = new JButton();
        panel4 = new JPanel();
        btnAddCP2 = new JButton();
        btnDelCP2 = new JButton();
        pnlSetup = new JPanel();
        label6 = new JLabel();
        label7 = new JLabel();
        textField1 = new JTextField();
        pnlRush = new JPanel();
        label2 = new JLabel();
        label14 = new JLabel();
        scrollPane3 = new JScrollPane();
        list3 = new JList();
        btnRefreshAgents = new JButton();
        separator2 = new JSeparator();
        panel2 = new JPanel();
        btnLoad = new JButton();
        btnStart = new JButton();
        btnPause = new JButton();
        btnResume = new JButton();
        btnUnload = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "$ugap, default:grow, $ugap",
            "$lgap, default, $nlgap, default, $lgap, default, $nlgap, default:grow, $lgap, 2*(default), $ugap"));

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
            buttonPanel.add(hSpacer1);

            //---- tbKeyLock ----
            tbKeyLock.setText("Keylock");
            buttonPanel.add(tbKeyLock);
        }
        contentPane.add(buttonPanel, CC.xy(2, 4));
        contentPane.add(separator1, CC.xy(2, 6));

        //======== mainPanel ========
        {
            mainPanel.setLayout(new FormLayout(
                "default:grow, $lcgap, default",
                "default, fill:default:grow, $lgap, default"));

            //======== cardPanel ========
            {
                cardPanel.setLayout(new CardLayout());

                //======== pnlConquest ========
                {
                    pnlConquest.setLayout(new FormLayout(
                        "default, $lcgap, 193dlu:grow",
                        "6*(default, $lgap), fill:default:grow"));

                    //---- label1 ----
                    label1.setText("Conquest");
                    label1.setFont(new Font("sansserif", Font.BOLD, 20));
                    pnlConquest.add(label1, CC.xywh(1, 1, 3, 1));

                    //---- label3 ----
                    label3.setText("Respawn Tickets");
                    pnlConquest.add(label3, CC.xy(1, 3));
                    pnlConquest.add(textField2, CC.xy(3, 3));

                    //---- label4 ----
                    label4.setText("Bleeding starts @");
                    pnlConquest.add(label4, CC.xy(1, 5));
                    pnlConquest.add(textField3, CC.xy(3, 5));

                    //---- label5 ----
                    label5.setText("Start Bleed interval");
                    pnlConquest.add(label5, CC.xy(1, 7));
                    pnlConquest.add(textField4, CC.xy(3, 7));

                    //---- label8 ----
                    label8.setText("Final Bleed interval");
                    pnlConquest.add(label8, CC.xy(1, 9));
                    pnlConquest.add(textField5, CC.xy(3, 9));

                    //---- label9 ----
                    label9.setText("Ticket Price");
                    pnlConquest.add(label9, CC.xy(1, 11));
                    pnlConquest.add(textField6, CC.xy(3, 11));

                    //======== panel1 ========
                    {
                        panel1.setLayout(new FormLayout(
                            "2*(default:grow, $ugap), default",
                            "default, $rgap, 2*(default), default:grow, $lgap, default"));

                        //---- label10 ----
                        label10.setText("Capture Points");
                        panel1.add(label10, CC.xy(1, 1));

                        //---- label13 ----
                        label13.setText("Sirens");
                        panel1.add(label13, CC.xy(3, 1));

                        //---- label11 ----
                        label11.setText("Red Spawn");
                        panel1.add(label11, CC.xy(5, 1));

                        //======== scrollPane1 ========
                        {
                            scrollPane1.setViewportView(list1);
                        }
                        panel1.add(scrollPane1, CC.xywh(1, 3, 1, 3, CC.DEFAULT, CC.FILL));

                        //======== scrollPane2 ========
                        {
                            scrollPane2.setViewportView(list2);
                        }
                        panel1.add(scrollPane2, CC.xywh(3, 3, 1, 3));
                        panel1.add(comboBox1, CC.xy(5, 3));

                        //---- label12 ----
                        label12.setText("Blue Spawn");
                        panel1.add(label12, CC.xy(5, 4));
                        panel1.add(comboBox2, CC.xy(5, 5, CC.DEFAULT, CC.TOP));

                        //======== panel3 ========
                        {
                            panel3.setLayout(new BoxLayout(panel3, BoxLayout.X_AXIS));

                            //---- btnAddCP ----
                            btnAddCP.setText(null);
                            btnAddCP.setIcon(new ImageIcon(getClass().getResource("/artwork/edit_add.png")));
                            panel3.add(btnAddCP);

                            //---- btnDelCP ----
                            btnDelCP.setText(null);
                            btnDelCP.setIcon(new ImageIcon(getClass().getResource("/artwork/edit_remove.png")));
                            panel3.add(btnDelCP);
                        }
                        panel1.add(panel3, CC.xy(1, 7));

                        //======== panel4 ========
                        {
                            panel4.setLayout(new BoxLayout(panel4, BoxLayout.X_AXIS));

                            //---- btnAddCP2 ----
                            btnAddCP2.setText(null);
                            btnAddCP2.setIcon(new ImageIcon(getClass().getResource("/artwork/edit_add.png")));
                            panel4.add(btnAddCP2);

                            //---- btnDelCP2 ----
                            btnDelCP2.setText(null);
                            btnDelCP2.setIcon(new ImageIcon(getClass().getResource("/artwork/edit_remove.png")));
                            panel4.add(btnDelCP2);
                        }
                        panel1.add(panel4, CC.xy(3, 7));
                    }
                    pnlConquest.add(panel1, CC.xywh(1, 13, 3, 1));
                }
                cardPanel.add(pnlConquest, "conquest");

                //======== pnlSetup ========
                {
                    pnlSetup.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "2*(default, $lgap), default"));

                    //---- label6 ----
                    label6.setText("Setup Connection");
                    label6.setFont(new Font("sansserif", Font.BOLD, 20));
                    pnlSetup.add(label6, CC.xywh(1, 1, 3, 1));

                    //---- label7 ----
                    label7.setText("URI");
                    pnlSetup.add(label7, CC.xy(1, 3));
                    pnlSetup.add(textField1, CC.xy(3, 3));
                }
                cardPanel.add(pnlSetup, "setup");

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
                cardPanel.add(pnlRush, "rush");
            }
            mainPanel.add(cardPanel, CC.xywh(1, 1, 1, 4));

            //---- label14 ----
            label14.setText("All Agents");
            mainPanel.add(label14, CC.xy(3, 1));

            //======== scrollPane3 ========
            {
                scrollPane3.setViewportView(list3);
            }
            mainPanel.add(scrollPane3, CC.xy(3, 2));

            //---- btnRefreshAgents ----
            btnRefreshAgents.setText(null);
            btnRefreshAgents.setIcon(new ImageIcon(getClass().getResource("/artwork/agt_reload32.png")));
            mainPanel.add(btnRefreshAgents, CC.xy(3, 4));
        }
        contentPane.add(mainPanel, CC.xy(2, 8, CC.DEFAULT, CC.FILL));
        contentPane.add(separator2, CC.xy(2, 10));

        //======== panel2 ========
        {
            panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));

            //---- btnLoad ----
            btnLoad.setText("Load Game");
            btnLoad.addActionListener(e -> btnSend(e));
            panel2.add(btnLoad);

            //---- btnStart ----
            btnStart.setText("Start Game");
            btnStart.addActionListener(e -> btnSend(e));
            panel2.add(btnStart);

            //---- btnPause ----
            btnPause.setText("Pause Game");
            btnPause.addActionListener(e -> btnSend(e));
            panel2.add(btnPause);

            //---- btnResume ----
            btnResume.setText("Resume Game");
            btnResume.addActionListener(e -> btnSend(e));
            panel2.add(btnResume);

            //---- btnUnload ----
            btnUnload.setText("Unload Game");
            btnUnload.addActionListener(e -> btnSend(e));
            panel2.add(btnUnload);
        }
        contentPane.add(panel2, CC.xy(2, 11));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel buttonPanel;
    private JButton btnSetup;
    private JButton btnConquest;
    private JButton btnRush;
    private JPanel hSpacer1;
    private JToggleButton tbKeyLock;
    private JSeparator separator1;
    private JPanel mainPanel;
    private JPanel cardPanel;
    private JPanel pnlConquest;
    private JLabel label1;
    private JLabel label3;
    private JTextField textField2;
    private JLabel label4;
    private JTextField textField3;
    private JLabel label5;
    private JTextField textField4;
    private JLabel label8;
    private JTextField textField5;
    private JLabel label9;
    private JTextField textField6;
    private JPanel panel1;
    private JLabel label10;
    private JLabel label13;
    private JLabel label11;
    private JScrollPane scrollPane1;
    private JList list1;
    private JScrollPane scrollPane2;
    private JList list2;
    private JComboBox comboBox1;
    private JLabel label12;
    private JComboBox comboBox2;
    private JPanel panel3;
    private JButton btnAddCP;
    private JButton btnDelCP;
    private JPanel panel4;
    private JButton btnAddCP2;
    private JButton btnDelCP2;
    private JPanel pnlSetup;
    private JLabel label6;
    private JLabel label7;
    private JTextField textField1;
    private JPanel pnlRush;
    private JLabel label2;
    private JLabel label14;
    private JScrollPane scrollPane3;
    private JList list3;
    private JButton btnRefreshAgents;
    private JSeparator separator2;
    private JPanel panel2;
    private JButton btnLoad;
    private JButton btnStart;
    private JButton btnPause;
    private JButton btnResume;
    private JButton btnUnload;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
