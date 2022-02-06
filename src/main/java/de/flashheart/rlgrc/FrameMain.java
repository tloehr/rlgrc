/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc;

import java.awt.*;
import java.awt.event.*;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import de.flashheart.rlgrc.jobs.AgentRefreshJob;
import de.flashheart.rlgrc.misc.Configs;
import de.flashheart.rlgrc.ui.TM_Agents;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.Arrays;
import java.util.Collection;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class FrameMain extends JFrame {
    private final Scheduler scheduler;
    private final Configs configs;
    private SimpleTrigger agentTrigger;
    private final JobKey agentJob;
    private final CardLayout cardLayout;
    private static final String REST_URI
            = "http://localhost:8090/api/";
    private Client client = ClientBuilder.newClient();
    private JSONObject params;
    private String current_mode;


    public FrameMain(Configs configs) throws SchedulerException, IOException {
        this.configs = configs;
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        this.scheduler.getContext().put("rlgrc", this);
        this.scheduler.start();
        agentJob = new JobKey(AgentRefreshJob.name, "group1");
        initComponents();
        initRefreshAgents();
        cardLayout = (CardLayout) cardPanel.getLayout();
        show_mode(configs.get(Configs.LAST_GAME_MODE));
    }

    private void show_mode(String mode) {
        current_mode = mode;
        try {
            params = new JSONObject(load_game_file(mode, configs.get(Configs.LAST_CONQUEST_FILE)));
        } catch (IOException e) {
            try {
                params = new JSONObject(load_game_file(mode, "<default>"));
            } catch (IOException ex) {
                ex.printStackTrace();
                params = null;
            }
        }
        configs.put(Configs.LAST_GAME_MODE, mode);
        params_to_dialog();
        cmbFiles.setModel(list_files());
        cardLayout.show(cardPanel, mode);

    }

    private void btnConquest(ActionEvent e) {
        show_mode("conquest");
    }

    private void btnRush(ActionEvent e) {
        cardLayout.show(cardPanel, "rush");
    }

    private void btnSetup(ActionEvent e) {
        cardLayout.show(cardPanel, "setup");
    }


    private String load_game_file(String mode, String filename) throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        if (filename.equals("<default>")) {
            return load_resource_file(mode);
        }
        return FileUtils.readFileToString(new File(System.getProperty("workspace") + File.pathSeparator + current_mode + File.separator + filename));
    }

    private void save_game_file(String filename) {
        log.debug(filename);
        dialog_to_params();
        try {
            FileUtils.writeStringToFile(new File(System.getProperty("workspace") + File.separator + current_mode + File.separator + filename + ".json"), params.toString(4));
        } catch (IOException e) {
            log.error(e);
        }
    }

    private String load_resource_file(String mode) {
        StringBuffer stringBuffer = new StringBuffer();
        InputStream in = getClass().getResourceAsStream("/defaults/" + mode + ".json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        reader.lines().forEach(s -> stringBuffer.append(s));
        return stringBuffer.toString();
    }

    private DefaultComboBoxModel<File> list_files() {
        DefaultComboBoxModel dcbm = new DefaultComboBoxModel();
        Collection<File> files = FileUtils.listFiles(new File(System.getProperty("workspace")+File.separator+current_mode+File.separator), new String[]{"json"}, false);
        files.forEach(file -> dcbm.addElement(file));
        return dcbm;
    }

    private void btnSend(ActionEvent e) {
        String id = "g1";

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

    private void initRefreshAgents() throws SchedulerException {
        if (scheduler.checkExists(agentJob)) return;
        JobDetail job = newJob(AgentRefreshJob.class)
                .withIdentity(agentJob)
                .build();

        agentTrigger = newTrigger()
                .withIdentity(AgentRefreshJob.name + "-trigger", "group1")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(15)
                        .repeatForever())
                .build();
        scheduler.scheduleJob(job, agentTrigger);
    }


    public void refreshAgents() {
        if (!cbRefreshAgents.isSelected()) return;
        Response response = client
                .target(REST_URI + "system/list_agents")
                .request(MediaType.APPLICATION_JSON)
                .get();
        String json = response.readEntity(String.class);

        log.debug(json);
        SwingUtilities.invokeLater(() -> {
            ((TM_Agents) tblAgents.getModel()).refresh_agents(new JSONObject(json));
        });
    }

    private void createUIComponents() {
        tblAgents = new JTable(new TM_Agents(new JSONObject())) {
            @Override
            public String getToolTipText(MouseEvent e) {
                //Implement table cell tool tips.
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColumnIndex = convertColumnIndexToModel(colIndex);
                TM_Agents model = (TM_Agents) getModel();
                log.debug(model.getValueAt(rowIndex));
                return "<html><p>" + model.getValueAt(rowIndex) + "</p></html>";
            }
        };
    }


    private void btnAddCP(ActionEvent e) {
        int[] selection = tblAgents.getSelectionModel().getSelectedIndices();
        DefaultListModel dlm = new DefaultListModel();
        for (int s : selection) {
            dlm.addElement(tblAgents.getModel().getValueAt(s, 0).toString());
        }
        SwingUtilities.invokeLater(() -> {
            listCP.setModel(dlm);
            listCP.repaint();
        });
    }

    private void fill_list(JList mylist, JSONArray myarray) {
        DefaultListModel dlm = new DefaultListModel();
        dlm.addAll(myarray.toList());
        SwingUtilities.invokeLater(() -> {
            mylist.setModel(dlm);
            mylist.repaint();
        });
    }

    private JSONArray list_to_jsonarray(JList mylist) {
        return new JSONArray(Arrays.asList(((DefaultListModel) mylist.getModel()).toArray()));
    }

    private void btnSave(ActionEvent e) {
        if (cmbFiles.getSelectedItem() == null) return;
        if (current_mode.equals("conquest")) {
            save_game_file(cmbFiles.getSelectedItem().toString());
        }
    }

    private void params_to_dialog() {
        if (current_mode.equals("conquest")) {
            txtCnqTickets.setText(params.get("respawn_tickets").toString());
            txtCnqTPrice.setText(params.get("ticket_price_for_respawn").toString());
            txtCnqBleedStarts.setText(params.get("not_bleeding_before_cps").toString());
            txtCnqSBleedInt.setText(params.get("start_bleed_interval").toString());
            txtCnqEBleedInt.setText(params.get("end_bleed_interval").toString());
            fill_list(listCP, params.getJSONObject("agents").getJSONArray("capture_points"));
            fill_list(listSirens, params.getJSONObject("agents").getJSONArray("sirens"));
            lblRedSpawn.setText(params.getJSONObject("agents").getJSONArray("red_spawn").getString(0));
            lblBlueSpawn.setText(params.getJSONObject("agents").getJSONArray("blue_spawn").getString(0));

        }
    }

    private void dialog_to_params() {
        params.clear();
        if (current_mode.equals("conquest")) {
            params.put("respawn_tickets", Integer.parseInt(txtCnqTickets.getText()));
            params.put("ticket_price_for_respawn", Integer.parseInt(txtCnqTPrice.getText()));
            params.put("not_bleeding_before_cps", Integer.parseInt(txtCnqBleedStarts.getText()));
            params.put("start_bleed_interval", Double.parseDouble(txtCnqSBleedInt.getText()));
            params.put("end_bleed_interval", Double.parseDouble(txtCnqEBleedInt.getText()));

            JSONObject agents = new JSONObject();
            agents.put("capture_points", list_to_jsonarray(listCP));
            agents.put("sirens", list_to_jsonarray(listSirens));
            agents.put("red_spawn", new JSONArray().put(lblRedSpawn.getText()));
            agents.put("blue_spawn", new JSONArray().put(lblBlueSpawn.getText()));
            params.put("agents", agents);
        }
        log.debug(params.toString(2));
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        createUIComponents();

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
        txtCnqTickets = new JTextField();
        label4 = new JLabel();
        txtCnqBleedStarts = new JTextField();
        label5 = new JLabel();
        txtCnqSBleedInt = new JTextField();
        lbl12345 = new JLabel();
        txtCnqEBleedInt = new JTextField();
        label9 = new JLabel();
        txtCnqTPrice = new JTextField();
        panel1 = new JPanel();
        label10 = new JLabel();
        label13 = new JLabel();
        panel5 = new JPanel();
        lblRedSpawn = new JLabel();
        btnSetRed = new JButton();
        lblBlueSpawn = new JLabel();
        btnSetBlue = new JButton();
        scrollPane1 = new JScrollPane();
        listCP = new JList();
        scrollPane2 = new JScrollPane();
        listSirens = new JList();
        panel3 = new JPanel();
        btnAddCP = new JButton();
        panel4 = new JPanel();
        btnAddSirens = new JButton();
        cmbFiles = new JComboBox();
        btnSave = new JButton();
        pnlSetup = new JPanel();
        label6 = new JLabel();
        label7 = new JLabel();
        textField1 = new JTextField();
        pnlRush = new JPanel();
        label2 = new JLabel();
        separator3 = new JSeparator();
        cbRefreshAgents = new JCheckBox();
        scrollPane3 = new JScrollPane();
        label11 = new JLabel();
        scrollPane4 = new JScrollPane();
        textPane1 = new JTextPane();
        separator2 = new JSeparator();
        panel2 = new JPanel();
        btnLoad2 = new JButton();
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
                "$ugap, default, $ugap, default:grow, $ugap, default, $ugap"));

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
        contentPane.add(buttonPanel, CC.xy(2, 2));
        contentPane.add(separator1, CC.xy(2, 3));

        //======== mainPanel ========
        {
            mainPanel.setLayout(new FormLayout(
                    "default:grow, 6dlu, default",
                    "default, $lgap, fill:96dlu, $rgap, default, $lgap, fill:default:grow, $lgap, default"));

            //======== cardPanel ========
            {
                cardPanel.setLayout(new CardLayout());

                //======== pnlConquest ========
                {
                    pnlConquest.setLayout(new FormLayout(
                            "default, $rgap, default:grow, $ugap, pref, $rgap, default:grow",
                            "5*(default, $lgap), fill:default:grow"));

                    //---- label1 ----
                    label1.setText("Conquest");
                    label1.setFont(new Font("sansserif", Font.BOLD, 20));
                    pnlConquest.add(label1, CC.xywh(1, 1, 3, 1));

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

                    //======== panel1 ========
                    {
                        panel1.setLayout(new FormLayout(
                                "2*(default:grow, $ugap), default",
                                "default, $rgap, 2*(default), default:grow, $lgap, default, $lgap, pref, $lgap, default"));

                        //---- label10 ----
                        label10.setText("Capture Points");
                        label10.setBackground(SystemColor.windowBorder);
                        label10.setForeground(Color.black);
                        label10.setOpaque(true);
                        panel1.add(label10, CC.xy(1, 1));

                        //---- label13 ----
                        label13.setText("Sirens");
                        label13.setBackground(SystemColor.windowBorder);
                        label13.setForeground(Color.black);
                        label13.setOpaque(true);
                        panel1.add(label13, CC.xy(3, 1));

                        //======== panel5 ========
                        {
                            panel5.setLayout(new FormLayout(
                                    "33dlu:grow, $lcgap, default",
                                    "default, $ugap, fill:default"));

                            //---- lblRedSpawn ----
                            lblRedSpawn.setText("test");
                            lblRedSpawn.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 22));
                            lblRedSpawn.setAlignmentX(0.5F);
                            lblRedSpawn.setBackground(new Color(255, 0, 51));
                            lblRedSpawn.setOpaque(true);
                            lblRedSpawn.setForeground(new Color(255, 255, 51));
                            panel5.add(lblRedSpawn, CC.xy(1, 1, CC.FILL, CC.DEFAULT));

                            //---- btnSetRed ----
                            btnSetRed.setText(null);
                            btnSetRed.setIcon(new ImageIcon(getClass().getResource("/artwork/edit_add.png")));
                            btnSetRed.setMinimumSize(new Dimension(38, 38));
                            btnSetRed.setPreferredSize(new Dimension(38, 38));
                            panel5.add(btnSetRed, CC.xy(3, 1, CC.RIGHT, CC.DEFAULT));

                            //---- lblBlueSpawn ----
                            lblBlueSpawn.setText("test");
                            lblBlueSpawn.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 22));
                            lblBlueSpawn.setAlignmentX(0.5F);
                            lblBlueSpawn.setBackground(new Color(51, 51, 255));
                            lblBlueSpawn.setOpaque(true);
                            lblBlueSpawn.setForeground(new Color(255, 255, 51));
                            panel5.add(lblBlueSpawn, CC.xy(1, 3, CC.FILL, CC.DEFAULT));

                            //---- btnSetBlue ----
                            btnSetBlue.setText(null);
                            btnSetBlue.setIcon(new ImageIcon(getClass().getResource("/artwork/edit_add.png")));
                            btnSetBlue.setMinimumSize(new Dimension(38, 38));
                            btnSetBlue.setPreferredSize(new Dimension(38, 38));
                            panel5.add(btnSetBlue, CC.xy(3, 3));
                        }
                        panel1.add(panel5, CC.xywh(5, 1, 1, 6));

                        //======== scrollPane1 ========
                        {
                            scrollPane1.setViewportView(listCP);
                        }
                        panel1.add(scrollPane1, CC.xywh(1, 3, 1, 3, CC.DEFAULT, CC.FILL));

                        //======== scrollPane2 ========
                        {
                            scrollPane2.setViewportView(listSirens);
                        }
                        panel1.add(scrollPane2, CC.xywh(3, 3, 1, 3));

                        //======== panel3 ========
                        {
                            panel3.setLayout(new BoxLayout(panel3, BoxLayout.X_AXIS));

                            //---- btnAddCP ----
                            btnAddCP.setText(null);
                            btnAddCP.setIcon(new ImageIcon(getClass().getResource("/artwork/edit_add.png")));
                            btnAddCP.addActionListener(e -> btnAddCP(e));
                            panel3.add(btnAddCP);
                        }
                        panel1.add(panel3, CC.xy(1, 7));

                        //======== panel4 ========
                        {
                            panel4.setLayout(new BoxLayout(panel4, BoxLayout.X_AXIS));

                            //---- btnAddSirens ----
                            btnAddSirens.setText(null);
                            btnAddSirens.setIcon(new ImageIcon(getClass().getResource("/artwork/edit_add.png")));
                            panel4.add(btnAddSirens);
                        }
                        panel1.add(panel4, CC.xy(3, 7));

                        //---- cmbFiles ----
                        cmbFiles.setEditable(true);
                        panel1.add(cmbFiles, CC.xywh(1, 9, 3, 1));

                        //---- btnSave ----
                        btnSave.setText(null);
                        btnSave.setIcon(new ImageIcon(getClass().getResource("/artwork/player_eject.png")));
                        btnSave.setMinimumSize(new Dimension(38, 38));
                        btnSave.setPreferredSize(new Dimension(38, 38));
                        btnSave.addActionListener(e -> btnSave(e));
                        panel1.add(btnSave, CC.xy(5, 9));
                    }
                    pnlConquest.add(panel1, CC.xywh(1, 11, 7, 1));
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
            mainPanel.add(cardPanel, CC.xywh(1, 1, 1, 9));

            //---- separator3 ----
            separator3.setOrientation(SwingConstants.VERTICAL);
            mainPanel.add(separator3, CC.xywh(2, 1, 1, 9, CC.CENTER, CC.DEFAULT));

            //---- cbRefreshAgents ----
            cbRefreshAgents.setText("Autorefresh Agent List");
            cbRefreshAgents.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
            cbRefreshAgents.setSelected(true);
            mainPanel.add(cbRefreshAgents, CC.xy(3, 1));

            //======== scrollPane3 ========
            {
                scrollPane3.setViewportView(tblAgents);
            }
            mainPanel.add(scrollPane3, CC.xy(3, 3));

            //---- label11 ----
            label11.setText("Server Response");
            mainPanel.add(label11, CC.xy(3, 5));

            //======== scrollPane4 ========
            {

                //---- textPane1 ----
                textPane1.setContentType("text/html");
                scrollPane4.setViewportView(textPane1);
            }
            mainPanel.add(scrollPane4, CC.xywh(3, 7, 1, 3));
        }
        contentPane.add(mainPanel, CC.xy(2, 4, CC.DEFAULT, CC.FILL));
        contentPane.add(separator2, CC.xy(2, 5));

        //======== panel2 ========
        {
            panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));

            //---- btnLoad2 ----
            btnLoad2.setText("Load Game");
            btnLoad2.addActionListener(e -> btnSend(e));
            panel2.add(btnLoad2);

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
        contentPane.add(panel2, CC.xy(2, 6));
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
    private JTextField txtCnqTickets;
    private JLabel label4;
    private JTextField txtCnqBleedStarts;
    private JLabel label5;
    private JTextField txtCnqSBleedInt;
    private JLabel lbl12345;
    private JTextField txtCnqEBleedInt;
    private JLabel label9;
    private JTextField txtCnqTPrice;
    private JPanel panel1;
    private JLabel label10;
    private JLabel label13;
    private JPanel panel5;
    private JLabel lblRedSpawn;
    private JButton btnSetRed;
    private JLabel lblBlueSpawn;
    private JButton btnSetBlue;
    private JScrollPane scrollPane1;
    private JList listCP;
    private JScrollPane scrollPane2;
    private JList listSirens;
    private JPanel panel3;
    private JButton btnAddCP;
    private JPanel panel4;
    private JButton btnAddSirens;
    private JComboBox cmbFiles;
    private JButton btnSave;
    private JPanel pnlSetup;
    private JLabel label6;
    private JLabel label7;
    private JTextField textField1;
    private JPanel pnlRush;
    private JLabel label2;
    private JSeparator separator3;
    private JCheckBox cbRefreshAgents;
    private JScrollPane scrollPane3;
    private JTable tblAgents;
    private JLabel label11;
    private JScrollPane scrollPane4;
    private JTextPane textPane1;
    private JSeparator separator2;
    private JPanel panel2;
    private JButton btnLoad2;
    private JButton btnLoad;
    private JButton btnStart;
    private JButton btnPause;
    private JButton btnResume;
    private JButton btnUnload;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
