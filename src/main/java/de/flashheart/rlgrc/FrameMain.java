/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import de.flashheart.rlgrc.jobs.ServerRefreshJob;
import de.flashheart.rlgrc.misc.Configs;
import de.flashheart.rlgrc.misc.NotEmptyVerifier;
import de.flashheart.rlgrc.misc.NumberVerifier;
import de.flashheart.rlgrc.ui.TM_Agents;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class FrameMain extends JFrame {
    private static final int TAB_OVERVIEW = 0;
    private static final int TAB_CONQUEST = 1;
    private static final int TAB_RUSH = 2;
    private final String GAMEID = "g1";
    private boolean GAME_PAUSED;
    private final Scheduler scheduler;
    private final Configs configs;
    private SimpleTrigger agentTrigger;
    private final JobKey agentJob;
    private Client client = ClientBuilder.newClient();
    private final int MAX_LOG_LINES = 200;
    private String current_mode = "overview";
    private final String[] MODES = new String[]{"overview", "conquest", "rush"};
    //private Optional<File> loaded_file;
    private HashMap<String, MutablePair<Optional<File>, JSONObject>> game_params;


    public FrameMain(Configs configs) throws SchedulerException, IOException {
        this.configs = configs;
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        this.scheduler.getContext().put("rlgrc", this);
        this.scheduler.start();
        game_params = new HashMap<>();
        game_params.put("conquest", new MutablePair<>(Optional.empty(), load_defaults("conquest")));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "conquest"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "rush"));
        agentJob = new JobKey(ServerRefreshJob.name, "group1");
        initComponents();
        initLogger();
        setVerifiers();
        initRefresh();
        initFrame();
        pack();
    }

    private void initFrame() {
        txtURI.setText(configs.get(Configs.REST_URI));
        params_to_dialog("conquest");
    }

    private void setVerifiers() {
        txtCnqComment.setInputVerifier(new NotEmptyVerifier());
        txtCnqTickets.setInputVerifier(new NumberVerifier());
        txtCnqTPrice.setInputVerifier(new NumberVerifier(BigDecimal.ONE, NumberVerifier.MAX, false));
        txtCnqBleedStarts.setInputVerifier(new NumberVerifier());
        txtCnqSBleedInt.setInputVerifier(new NumberVerifier(BigDecimal.ONE, NumberVerifier.MAX, false));
        txtCnqEBleedInt.setInputVerifier(new NumberVerifier(BigDecimal.ZERO, NumberVerifier.MAX, false));
    }

    private void btnFileNew(ActionEvent e) {
        if (pnlGames.getSelectedIndex() == TAB_OVERVIEW) return;
        game_params.put(current_mode, new MutablePair<>(Optional.empty(), load_defaults(current_mode)));
        params_to_dialog();
    }


    private JSONObject load_file(File file) {
        JSONObject params;
        try {
            params = new JSONObject(FileUtils.readFileToString(file));
            game_params.get(current_mode).setRight(params);
            game_params.get(current_mode).setLeft(Optional.of(file));
        } catch (IOException e) {
            log.error(e);
            addLog(e.getMessage());
            params = new JSONObject();
            game_params.get(current_mode).setLeft(Optional.empty());
        }
        game_params.get(current_mode).setRight(params);
        return params;
    }

    private void save_file(JSONObject params) {
        if (game_params.get(current_mode).getLeft().isEmpty()) {
            game_params.get(current_mode).setLeft(choose_file(true));
        }

        game_params.get(current_mode).getLeft().ifPresent(file -> {
            try {
                FileUtils.writeStringToFile(file, params.toString(4));
            } catch (IOException e) {
                log.error(e);
                addLog(e.getMessage());
            }
        });

    }

    private JSONObject load_defaults(String mode) {
        StringBuffer stringBuffer = new StringBuffer();
        InputStream in = getClass().getResourceAsStream("/defaults/" + mode + ".json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        reader.lines().forEach(s -> stringBuffer.append(s));
        return new JSONObject(stringBuffer.toString());
    }

//    private DefaultComboBoxModel<File> list_files() {
//        DefaultComboBoxModel dcbm = new DefaultComboBoxModel();
//        Collection<File> files = FileUtils.listFiles(new File(System.getProperty("workspace") + File.separator + current_mode + File.separator), new String[]{"json"}, false);
//        files.forEach(file -> dcbm.addElement(file));
//        return dcbm;
//    }

    private void btnSend(ActionEvent e) {
        if (current_mode.equals("overview")) return;
        addLog("--------------");
        addLog(post("game/load", GAMEID, params_from_dialog().toString()).toString());

    }

    private void initRefresh() throws SchedulerException {
        if (scheduler.checkExists(agentJob)) return;
        JobDetail job = newJob(ServerRefreshJob.class)
                .withIdentity(agentJob)
                .build();

        agentTrigger = newTrigger()
                .withIdentity(ServerRefreshJob.name + "-trigger", "group1")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(15)
                        .repeatForever())
                .build();
        scheduler.scheduleJob(job, agentTrigger);
    }

    private void set_pause_mode(boolean paused) {
        this.GAME_PAUSED = paused;
        btnPauseGame.setIcon(new ImageIcon(getClass().getResource(GAME_PAUSED ? "/artwork/player_fwd.png" : "/artwork/player_pause.png")));
    }


    private void refreshAgents() {
        JSONObject request = get("system/list_agents");
        SwingUtilities.invokeLater(() -> {
            ((TM_Agents) tblAgents.getModel()).refresh_agents(request);
        });
    }

    public void refreshServer() {
        if (cbRefreshAgents.isSelected()) refreshAgents();
        if (cbRefreshGameStatus.isSelected()) refreshStatus();
    }

    private void refreshStatus() {
        JSONObject request = get("game/status", GAMEID);
        try {
            request.getJSONObject("payload").getString("message");
            addLog("Game " + GAMEID + " not loaded on the server");
        } catch (Exception e) {
            addLog("--------------\n" + request.toString());
            // funny - when an exception means NO EXPCETION on the server side
        }
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

    private void btnStartGame(ActionEvent e) {
        JSONObject request = post("game/start", GAMEID);
        //set_pause_mode(false);
        addLog(request.toString());
    }


    private void btnPauseGame(ActionEvent e) {
        JSONObject request = post(GAME_PAUSED ? "game/resume" : "game/pause", GAMEID);
        set_pause_mode(request.has("pause_start_time") && !request.getString("pause_start_time").equals("null"));
        addLog(request.toString());
    }

    private void btnResetGame(ActionEvent e) {
        JSONObject request = post("game/reset", GAMEID);
        addLog(request.toString());
    }

    private void btnUnloadGame(ActionEvent e) {
        JSONObject request = post("game/unload", GAMEID);
        addLog(request.toString());
    }

    private JSONObject post(String uri, String id) {
        return post(uri, id, "{}");
    }

    private JSONObject post(String uri, String id, String body) {
        JSONObject result = new JSONObject();

        try {
            Response response = client
                    .target(txtURI.getText().trim() + "/api/" + uri)
                    .queryParam("id", id)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body));
            addLog("----------\n" + response.getStatus() + " " + response.getStatusInfo().toString() + "\n");
            String entity = response.readEntity(String.class);
            response.close();
            if (entity.isEmpty()) result = new JSONObject();
            else result = new JSONObject(entity);

        } catch (Exception connectException) {
            addLog(connectException.getMessage());
        }
        return result;
    }

    private JSONObject get(String uri, String id) {
        JSONObject json = new JSONObject();
        try {
            Response response = client
                    .target(txtURI.getText().trim() + "/api/" + uri)
                    .queryParam("id", id)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            json = new JSONObject(response.readEntity(String.class));
            response.close();
        } catch (Exception connectException) {
            addLog(connectException.getMessage());
        }
        return json;
    }

    private JSONObject get(String uri) {
        JSONObject json = new JSONObject();
        try {
            Response response = client
                    .target(txtURI.getText() + "/api/" + uri)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            json = new JSONObject(response.readEntity(String.class));
            response.close();
        } catch (Exception connectException) {
            addLog(connectException.getMessage());
        }
        return json;
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

    private Optional<File> choose_file(boolean save) {
        if (current_mode.equals("overview")) return null;
        JFileChooser fileChooser = new JFileChooser(new File(System.getProperty("workspace") + File.separator + current_mode));
        int result;// = JFileChooser.CANCEL_OPTION;
        if (save) result = fileChooser.showSaveDialog(this);
        else result = fileChooser.showOpenDialog(this);
        Optional<File> file = Optional.empty();
        if (result == JFileChooser.APPROVE_OPTION) {
            file = Optional.of(fileChooser.getSelectedFile());
            lblFile.setText(file.get().getAbsolutePath());
        } else {
            lblFile.setText(null);
        }
        return file;
    }

    private void btnSaveFile(ActionEvent e) {
        if (current_mode.equals("overview")) return;
        if (game_params.get(current_mode).getLeft().isEmpty()) game_params.get(current_mode).setLeft(choose_file(true));
        game_params.get(current_mode).getLeft().ifPresent(file -> save_file(params_from_dialog()));
    }

    private void btnLoadFile(ActionEvent e) {
        if (current_mode.equals("overview")) return;

        game_params.get(current_mode).setLeft(choose_file(false));
        game_params.get(current_mode).getLeft().ifPresent(file -> game_params.get(current_mode).setRight(load_file(file)));
        params_to_dialog();
    }

    private void params_to_dialog(String mode) {
        JSONObject params = game_params.get(mode).getRight();
        if (params.isEmpty()) return;
        if (!params.has("mode")) return;
        if (params.get("mode").equals("conquest")) {
            txtCnqComment.setText(params.get("comment").toString());
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

    private void params_to_dialog() {
        params_to_dialog(current_mode);
    }

    private JSONObject params_from_dialog() {
        JSONObject params = new JSONObject();
        if (current_mode.equals("conquest")) {
            params.put("respawn_tickets", Integer.parseInt(txtCnqTickets.getText()));
            params.put("ticket_price_for_respawn", Integer.parseInt(txtCnqTPrice.getText()));
            params.put("not_bleeding_before_cps", Integer.parseInt(txtCnqBleedStarts.getText()));
            params.put("start_bleed_interval", Double.parseDouble(txtCnqSBleedInt.getText()));
            params.put("end_bleed_interval", Double.parseDouble(txtCnqEBleedInt.getText()));
            params.put("class", "de.flashheart.rlg.commander.games.Conquest");
            params.put("comment", txtCnqComment.getText().trim());

            JSONObject agents = new JSONObject();
            agents.put("capture_points", list_to_jsonarray(listCP));
            agents.put("sirens", list_to_jsonarray(listSirens));
            agents.put("red_spawn", new JSONArray().put(lblRedSpawn.getText()));
            agents.put("blue_spawn", new JSONArray().put(lblBlueSpawn.getText()));
            agents.put("spawns", new JSONArray().put(lblRedSpawn.getText()).put(lblBlueSpawn.getText()));
            params.put("agents", agents);
        }
        params.put("mode", current_mode);
        log.debug(params.toString(4));
        game_params.get(current_mode).setRight(params);
        return params;
    }


    private void initLogger() {
        // keeps the log window under MAX_LOG_LINES lines to prevent out of memory exception
        txtLogger.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    Element root = e.getDocument().getDefaultRootElement();
                    while (root.getElementCount() > MAX_LOG_LINES) {
                        Element firstLine = root.getElement(0);
                        try {
                            e.getDocument().remove(0, firstLine.getEndOffset());
                        } catch (BadLocationException ble) {
                            log.error(ble);
                        }
                    }
                });
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    public void addLog(String text) {
        log.debug(text);
        SwingUtilities.invokeLater(() -> {
            txtLogger.append(text + "\n");
            scrollPane1.getVerticalScrollBar().setValue(scrollPane1.getVerticalScrollBar().getMaximum());
        });
    }

    private void btnSetRed(ActionEvent e) {
        if (tblAgents.getSelectedRow() < 0) return;
        lblRedSpawn.setText(tblAgents.getModel().getValueAt(tblAgents.getSelectedRow(), 0).toString());
    }

    private void btnSetBlue(ActionEvent e) {
        if (tblAgents.getSelectedRow() < 0) return;
        lblBlueSpawn.setText(tblAgents.getModel().getValueAt(tblAgents.getSelectedRow(), 0).toString());
    }

    private void btnAddCP(ActionEvent e) {
        add_to_agents_list(listCP);
    }

    private void add_to_agents_list(JList jList) {
        int[] selection = tblAgents.getSelectionModel().getSelectedIndices();
        DefaultListModel dlm = new DefaultListModel();
        for (int s : selection) {
            dlm.addElement(tblAgents.getModel().getValueAt(s, 0).toString());
        }
        SwingUtilities.invokeLater(() -> {
            jList.setModel(dlm);
            jList.repaint();
        });
    }

    private void btnAddSirens(ActionEvent e) {
        add_to_agents_list(listSirens);
    }

    private void tbKeyLockItemStateChanged(ItemEvent e) {
        btnPauseGame.setEnabled(e.getStateChange() != ItemEvent.SELECTED);
    }

    private void pnlGamesStateChanged(ChangeEvent e) {
        log.debug(e.toString());
        current_mode = MODES[pnlGames.getSelectedIndex()];


        //    private void show_mode(String mode) {
        //        current_mode = mode;
        //        params = new JSONObject(load_file());
        //        configs.put(Configs.LAST_GAME_MODE, mode);
        //        params_to_dialog();
        //        //cmbFiles.setModel(list_files());
        //        cardLayout.show(cardPanel, mode);
        //    }

        //    private void btnConquest(ActionEvent e) {
        //        show_mode("conquest");
        //    }
        //
        //    private void btnRush(ActionEvent e) {
        //        cardLayout.show(cardPanel, "rush");
        //    }
        //
        //    private void btnSetup(ActionEvent e) {
        //        cardLayout.show(cardPanel, "setup");
        //    }

    }

    private void txtURIFocusLost(FocusEvent e) {
        configs.put(Configs.REST_URI, txtURI.getText().trim());
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        createUIComponents();

        mainPanel = new JPanel();
        pnlMain = new JTabbedPane();
        pnlConnection = new JPanel();
        label7 = new JLabel();
        txtURI = new JTextField();
        lblRestServerStatus = new JLabel();
        btnCheckServer = new JButton();
        pnlParams = new JPanel();
        pnlGameP = new JTabbedPane();
        pnlConquest = new JPanel();
        txtCnqComment = new JTextField();
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
        listCP = new JList();
        scrollPane2 = new JScrollPane();
        listSirens = new JList();
        btnAddCP = new JButton();
        label13 = new JLabel();
        btnAddSirens = new JButton();
        lblRedSpawn = new JLabel();
        btnSetRed = new JButton();
        lblBlueSpawn = new JLabel();
        btnSetBlue = new JButton();
        pnlRush = new JPanel();
        label8 = new JLabel();
        scrollPane3 = new JScrollPane();
        button1 = new JButton();
        pnlFiles = new JPanel();
        lblFile = new JLabel();
        hSpacer1 = new JPanel(null);
        btnFileNew = new JButton();
        btnLoadFile = new JButton();
        btnSaveFile = new JButton();
        pnlStatus = new JPanel();
        separator3 = new JSeparator();
        panel3 = new JPanel();
        cbRefreshGameStatus = new JCheckBox();
        scrollPane4 = new JScrollPane();
        txtLogger = new JTextArea();
        separator2 = new JSeparator();
        panel2 = new JPanel();
        btnSendToServer = new JButton();
        btnStartGame = new JButton();
        btnPauseGame = new JButton();
        hSpacer2 = new JPanel(null);
        tbKeyLock = new JToggleButton();
        btnResetGame = new JButton();
        btnUnloadGame = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "$ugap, default:grow, $ugap",
            "$rgap, default:grow, $ugap, default, $rgap"));

        //======== mainPanel ========
        {
            mainPanel.setLayout(new FormLayout(
                "default:grow, $ugap, default:grow",
                "default, $rgap, default, $lgap, fill:default:grow, $lgap, default"));

            //======== pnlMain ========
            {
                pnlMain.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                pnlMain.addChangeListener(e -> pnlGamesStateChanged(e));

                //======== pnlConnection ========
                {
                    pnlConnection.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, $lcgap, default",
                        "default, $lgap, default"));

                    //---- label7 ----
                    label7.setText("URI");
                    pnlConnection.add(label7, CC.xy(1, 1));

                    //---- txtURI ----
                    txtURI.setText("http://localhost:8090");
                    txtURI.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusLost(FocusEvent e) {
                            txtURIFocusLost(e);
                        }
                    });
                    pnlConnection.add(txtURI, CC.xy(3, 1));

                    //---- lblRestServerStatus ----
                    lblRestServerStatus.setText(null);
                    lblRestServerStatus.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
                    pnlConnection.add(lblRestServerStatus, CC.xy(5, 1));

                    //---- btnCheckServer ----
                    btnCheckServer.setText("Check Rest Server");
                    pnlConnection.add(btnCheckServer, CC.xywh(1, 3, 5, 1));
                }
                pnlMain.addTab("Connection", pnlConnection);

                //======== pnlParams ========
                {
                    pnlParams.setLayout(new FormLayout(
                        "default:grow, default",
                        "default:grow, 2*($lgap, default)"));

                    //======== pnlGameP ========
                    {
                        pnlGameP.setFont(new Font(".SF NS Text", Font.PLAIN, 18));

                        //======== pnlConquest ========
                        {
                            pnlConquest.setLayout(new FormLayout(
                                "pref:grow, $rgap, default, $ugap, pref:grow, $rgap, default",
                                "$lgap, default, $ugap, 3*(default, $lgap), fill:default:grow"));

                            //---- txtCnqComment ----
                            txtCnqComment.setToolTipText("Comment");
                            pnlConquest.add(txtCnqComment, CC.xywh(1, 2, 7, 1));

                            //---- label3 ----
                            label3.setText("Respawn Tickets");
                            pnlConquest.add(label3, CC.xy(1, 4));
                            pnlConquest.add(txtCnqTickets, CC.xy(3, 4));

                            //---- label4 ----
                            label4.setText("Bleeding starts @");
                            pnlConquest.add(label4, CC.xy(5, 4));
                            pnlConquest.add(txtCnqBleedStarts, CC.xy(7, 4));

                            //---- label5 ----
                            label5.setText("Start Bleed interval");
                            pnlConquest.add(label5, CC.xy(1, 6));
                            pnlConquest.add(txtCnqSBleedInt, CC.xy(3, 6));

                            //---- lbl12345 ----
                            lbl12345.setText("Final Bleed interval");
                            pnlConquest.add(lbl12345, CC.xy(5, 6));
                            pnlConquest.add(txtCnqEBleedInt, CC.xy(7, 6));

                            //---- label9 ----
                            label9.setText("Ticket Price");
                            pnlConquest.add(label9, CC.xy(1, 8));
                            pnlConquest.add(txtCnqTPrice, CC.xy(3, 8));

                            //======== pnl1234 ========
                            {
                                pnl1234.setLayout(new FormLayout(
                                    "70dlu:grow, $lcgap, 20dlu, $ugap, 70dlu:grow, $lcgap, 20dlu",
                                    "default, $rgap, 2*(default), default:grow, $lgap, default"));
                                ((FormLayout)pnl1234.getLayout()).setColumnGroups(new int[][] {{1, 5}});

                                //---- label10 ----
                                label10.setText("Capture Points");
                                label10.setBackground(SystemColor.windowBorder);
                                label10.setForeground(Color.black);
                                label10.setOpaque(true);
                                label10.setHorizontalAlignment(SwingConstants.CENTER);
                                pnl1234.add(label10, CC.xy(1, 1, CC.DEFAULT, CC.FILL));

                                //======== scrollPane1 ========
                                {
                                    scrollPane1.setViewportView(listCP);
                                }
                                pnl1234.add(scrollPane1, CC.xywh(1, 3, 3, 3, CC.DEFAULT, CC.FILL));

                                //======== scrollPane2 ========
                                {
                                    scrollPane2.setViewportView(listSirens);
                                }
                                pnl1234.add(scrollPane2, CC.xywh(5, 3, 3, 3));

                                //---- btnAddCP ----
                                btnAddCP.setText(null);
                                btnAddCP.setIcon(new ImageIcon(getClass().getResource("/artwork/back.png")));
                                btnAddCP.addActionListener(e -> btnAddCP(e));
                                pnl1234.add(btnAddCP, CC.xywh(3, 1, 1, 2));

                                //---- label13 ----
                                label13.setText("Sirens");
                                label13.setBackground(SystemColor.windowBorder);
                                label13.setForeground(Color.black);
                                label13.setOpaque(true);
                                label13.setHorizontalAlignment(SwingConstants.CENTER);
                                pnl1234.add(label13, CC.xy(5, 1, CC.DEFAULT, CC.FILL));

                                //---- btnAddSirens ----
                                btnAddSirens.setText(null);
                                btnAddSirens.setIcon(new ImageIcon(getClass().getResource("/artwork/back.png")));
                                btnAddSirens.addActionListener(e -> btnAddSirens(e));
                                pnl1234.add(btnAddSirens, CC.xy(7, 1));

                                //---- lblRedSpawn ----
                                lblRedSpawn.setText("test");
                                lblRedSpawn.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 22));
                                lblRedSpawn.setAlignmentX(0.5F);
                                lblRedSpawn.setBackground(new Color(255, 0, 51));
                                lblRedSpawn.setOpaque(true);
                                lblRedSpawn.setForeground(new Color(255, 255, 51));
                                lblRedSpawn.setHorizontalAlignment(SwingConstants.CENTER);
                                pnl1234.add(lblRedSpawn, CC.xy(1, 7, CC.DEFAULT, CC.FILL));

                                //---- btnSetRed ----
                                btnSetRed.setText(null);
                                btnSetRed.setIcon(new ImageIcon(getClass().getResource("/artwork/previous.png")));
                                btnSetRed.setMinimumSize(new Dimension(38, 38));
                                btnSetRed.setPreferredSize(new Dimension(38, 38));
                                btnSetRed.addActionListener(e -> btnSetRed(e));
                                pnl1234.add(btnSetRed, CC.xy(3, 7));

                                //---- lblBlueSpawn ----
                                lblBlueSpawn.setText("test");
                                lblBlueSpawn.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 22));
                                lblBlueSpawn.setAlignmentX(0.5F);
                                lblBlueSpawn.setBackground(new Color(51, 51, 255));
                                lblBlueSpawn.setOpaque(true);
                                lblBlueSpawn.setForeground(new Color(255, 255, 51));
                                lblBlueSpawn.setHorizontalAlignment(SwingConstants.CENTER);
                                pnl1234.add(lblBlueSpawn, CC.xy(5, 7, CC.FILL, CC.FILL));

                                //---- btnSetBlue ----
                                btnSetBlue.setText(null);
                                btnSetBlue.setIcon(new ImageIcon(getClass().getResource("/artwork/back.png")));
                                btnSetBlue.setMinimumSize(new Dimension(38, 38));
                                btnSetBlue.setPreferredSize(new Dimension(38, 38));
                                btnSetBlue.addActionListener(e -> btnSetBlue(e));
                                pnl1234.add(btnSetBlue, CC.xy(7, 7));
                            }
                            pnlConquest.add(pnl1234, CC.xywh(1, 10, 7, 1, CC.FILL, CC.DEFAULT));
                        }
                        pnlGameP.addTab("pnlConquest", pnlConquest);

                        //======== pnlRush ========
                        {
                            pnlRush.setLayout(new FormLayout(
                                "default, $lcgap, default:grow",
                                "default:grow, $lgap, default"));

                            //---- label8 ----
                            label8.setText("no rush yet...");
                            pnlRush.add(label8, CC.xywh(1, 1, 3, 1));
                        }
                        pnlGameP.addTab("pnlRush", pnlRush);
                    }
                    pnlParams.add(pnlGameP, CC.xywh(1, 1, 1, 3));

                    //======== scrollPane3 ========
                    {
                        scrollPane3.setViewportView(tblAgents);
                    }
                    pnlParams.add(scrollPane3, CC.xy(2, 1, CC.DEFAULT, CC.FILL));

                    //---- button1 ----
                    button1.setText("text");
                    pnlParams.add(button1, CC.xy(2, 3));

                    //======== pnlFiles ========
                    {
                        pnlFiles.setLayout(new BoxLayout(pnlFiles, BoxLayout.X_AXIS));

                        //---- lblFile ----
                        lblFile.setText("no file");
                        lblFile.setFont(new Font(".SF NS Text", Font.PLAIN, 12));
                        pnlFiles.add(lblFile);
                        pnlFiles.add(hSpacer1);

                        //---- btnFileNew ----
                        btnFileNew.setText(null);
                        btnFileNew.setIcon(new ImageIcon(getClass().getResource("/artwork/filenew.png")));
                        btnFileNew.setMinimumSize(new Dimension(38, 38));
                        btnFileNew.setPreferredSize(new Dimension(38, 38));
                        btnFileNew.addActionListener(e -> btnFileNew(e));
                        pnlFiles.add(btnFileNew);

                        //---- btnLoadFile ----
                        btnLoadFile.setText(null);
                        btnLoadFile.setIcon(new ImageIcon(getClass().getResource("/artwork/fileopen.png")));
                        btnLoadFile.setMinimumSize(new Dimension(38, 38));
                        btnLoadFile.setPreferredSize(new Dimension(38, 38));
                        btnLoadFile.addActionListener(e -> btnLoadFile(e));
                        pnlFiles.add(btnLoadFile);

                        //---- btnSaveFile ----
                        btnSaveFile.setText(null);
                        btnSaveFile.setIcon(new ImageIcon(getClass().getResource("/artwork/filesave.png")));
                        btnSaveFile.setMinimumSize(new Dimension(38, 38));
                        btnSaveFile.setPreferredSize(new Dimension(38, 38));
                        btnSaveFile.addActionListener(e -> btnSaveFile(e));
                        pnlFiles.add(btnSaveFile);
                    }
                    pnlParams.add(pnlFiles, CC.xywh(1, 5, 2, 1));
                }
                pnlMain.addTab("Params", pnlParams);

                //======== pnlStatus ========
                {
                    pnlStatus.setLayout(new BoxLayout(pnlStatus, BoxLayout.X_AXIS));
                }
                pnlMain.addTab("Status", pnlStatus);
            }
            mainPanel.add(pnlMain, CC.xywh(1, 1, 1, 7));

            //---- separator3 ----
            separator3.setOrientation(SwingConstants.VERTICAL);
            mainPanel.add(separator3, CC.xywh(2, 1, 1, 7, CC.CENTER, CC.DEFAULT));

            //======== panel3 ========
            {
                panel3.setLayout(new BoxLayout(panel3, BoxLayout.PAGE_AXIS));

                //---- cbRefreshGameStatus ----
                cbRefreshGameStatus.setText("Autorefresh Game Status");
                cbRefreshGameStatus.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                panel3.add(cbRefreshGameStatus);

                //======== scrollPane4 ========
                {

                    //---- txtLogger ----
                    txtLogger.setForeground(new Color(51, 255, 51));
                    txtLogger.setLineWrap(true);
                    txtLogger.setWrapStyleWord(true);
                    scrollPane4.setViewportView(txtLogger);
                }
                panel3.add(scrollPane4);
            }
            mainPanel.add(panel3, CC.xywh(3, 1, 1, 7));
        }
        contentPane.add(mainPanel, CC.xy(2, 2, CC.DEFAULT, CC.FILL));
        contentPane.add(separator2, CC.xy(2, 3));

        //======== panel2 ========
        {
            panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));

            //---- btnSendToServer ----
            btnSendToServer.setText(null);
            btnSendToServer.setIcon(new ImageIcon(getClass().getResource("/artwork/irkickflash.png")));
            btnSendToServer.setToolTipText("Loag game on the server");
            btnSendToServer.addActionListener(e -> btnSend(e));
            panel2.add(btnSendToServer);

            //---- btnStartGame ----
            btnStartGame.setText(null);
            btnStartGame.setToolTipText("Start loaded game");
            btnStartGame.setIcon(new ImageIcon(getClass().getResource("/artwork/player_play.png")));
            btnStartGame.addActionListener(e -> btnStartGame(e));
            panel2.add(btnStartGame);

            //---- btnPauseGame ----
            btnPauseGame.setText(null);
            btnPauseGame.setIcon(new ImageIcon(getClass().getResource("/artwork/player_pause.png")));
            btnPauseGame.addActionListener(e -> btnPauseGame(e));
            panel2.add(btnPauseGame);
            panel2.add(hSpacer2);

            //---- tbKeyLock ----
            tbKeyLock.setText(null);
            tbKeyLock.setIcon(new ImageIcon(getClass().getResource("/artwork/decrypted.png")));
            tbKeyLock.setSelectedIcon(new ImageIcon(getClass().getResource("/artwork/encrypted.png")));
            tbKeyLock.setToolTipText("Keylock");
            tbKeyLock.addItemListener(e -> tbKeyLockItemStateChanged(e));
            panel2.add(tbKeyLock);

            //---- btnResetGame ----
            btnResetGame.setText(null);
            btnResetGame.setIcon(new ImageIcon(getClass().getResource("/artwork/player_rew.png")));
            btnResetGame.setToolTipText("Resume Game");
            btnResetGame.addActionListener(e -> btnResetGame(e));
            panel2.add(btnResetGame);

            //---- btnUnloadGame ----
            btnUnloadGame.setText(null);
            btnUnloadGame.setToolTipText("Unload Game");
            btnUnloadGame.setIcon(new ImageIcon(getClass().getResource("/artwork/player_eject.png")));
            btnUnloadGame.addActionListener(e -> btnUnloadGame(e));
            panel2.add(btnUnloadGame);
        }
        contentPane.add(panel2, CC.xy(2, 4));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel mainPanel;
    private JTabbedPane pnlMain;
    private JPanel pnlConnection;
    private JLabel label7;
    private JTextField txtURI;
    private JLabel lblRestServerStatus;
    private JButton btnCheckServer;
    private JPanel pnlParams;
    private JTabbedPane pnlGameP;
    private JPanel pnlConquest;
    private JTextField txtCnqComment;
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
    private JList listCP;
    private JScrollPane scrollPane2;
    private JList listSirens;
    private JButton btnAddCP;
    private JLabel label13;
    private JButton btnAddSirens;
    private JLabel lblRedSpawn;
    private JButton btnSetRed;
    private JLabel lblBlueSpawn;
    private JButton btnSetBlue;
    private JPanel pnlRush;
    private JLabel label8;
    private JScrollPane scrollPane3;
    private JTable tblAgents;
    private JButton button1;
    private JPanel pnlFiles;
    private JLabel lblFile;
    private JPanel hSpacer1;
    private JButton btnFileNew;
    private JButton btnLoadFile;
    private JButton btnSaveFile;
    private JPanel pnlStatus;
    private JSeparator separator3;
    private JPanel panel3;
    private JCheckBox cbRefreshGameStatus;
    private JScrollPane scrollPane4;
    private JTextArea txtLogger;
    private JSeparator separator2;
    private JPanel panel2;
    private JButton btnSendToServer;
    private JButton btnStartGame;
    private JButton btnPauseGame;
    private JPanel hSpacer2;
    private JToggleButton tbKeyLock;
    private JButton btnResetGame;
    private JButton btnUnloadGame;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
