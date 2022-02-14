/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc.ui;

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
    //private Optional<File> loaded_file;
    //private final HashMap<String, MutablePair<Optional<File>, JSONObject>> game_params;


    public FrameMain(Configs configs) throws SchedulerException, IOException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        this.configs = configs;
        this.agentJob = new JobKey(ServerRefreshJob.name, "group1");
        this.scheduler.getContext().put("rlgrc", this);
        this.scheduler.start();
        //this.game_params = new HashMap<>();
        initComponents();
        initFrame();
        pack();
    }

    private void initFrame() throws IOException {
        initLogger();
        //game_params.put("conquest", new MutablePair<>(Optional.empty(), load_defaults("conquest")));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "conquest"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "rush"));
        txtURI.setText(configs.get(Configs.REST_URI));
        pnlGames.add("Conquest", new ConquestParams());
        todod: params_to_dialog("conquest");


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
            scrollLog.getVerticalScrollBar().setValue(scrollLog.getVerticalScrollBar().getMaximum());
        });
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
        panel1 = new JPanel();
        label7 = new JLabel();
        txtURI = new JTextField();
        lblRestServerStatus = new JButton();
        pnlMain = new JTabbedPane();
        pnlParams = new JPanel();
        pnlGames = new JTabbedPane();
        pnlFiles = new JPanel();
        lblFile = new JLabel();
        hSpacer1 = new JPanel(null);
        btnFileNew = new JButton();
        btnLoadFile = new JButton();
        btnSaveFile = new JButton();
        pnlStatus = new JPanel();
        panel3 = new JPanel();
        cbRefreshGameStatus = new JCheckBox();
        scrollLog = new JScrollPane();
        txtLogger = new JTextArea();
        scrollPane3 = new JScrollPane();
        btnRefreshAgents = new JButton();
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
                "default:grow",
                "default, $lgap, default, $rgap, default, $lgap, fill:default:grow, $lgap, default"));

            //======== panel1 ========
            {
                panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));

                //---- label7 ----
                label7.setText("URI");
                panel1.add(label7);

                //---- txtURI ----
                txtURI.setText("http://localhost:8090");
                txtURI.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        txtURIFocusLost(e);
                    }
                });
                panel1.add(txtURI);

                //---- lblRestServerStatus ----
                lblRestServerStatus.setText(null);
                lblRestServerStatus.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
                panel1.add(lblRestServerStatus);
            }
            mainPanel.add(panel1, CC.xy(1, 1));

            //======== pnlMain ========
            {
                pnlMain.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                pnlMain.addChangeListener(e -> pnlGamesStateChanged(e));

                //======== pnlParams ========
                {
                    pnlParams.setLayout(new FormLayout(
                        "default:grow, default",
                        "default:grow, 2*($lgap, default)"));

                    //======== pnlGames ========
                    {
                        pnlGames.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                    }
                    pnlParams.add(pnlGames, CC.xywh(1, 1, 1, 3));

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

                    //======== panel3 ========
                    {
                        panel3.setLayout(new BoxLayout(panel3, BoxLayout.PAGE_AXIS));

                        //---- cbRefreshGameStatus ----
                        cbRefreshGameStatus.setText("Autorefresh Game Status");
                        cbRefreshGameStatus.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        panel3.add(cbRefreshGameStatus);

                        //======== scrollLog ========
                        {

                            //---- txtLogger ----
                            txtLogger.setForeground(new Color(51, 255, 51));
                            txtLogger.setLineWrap(true);
                            txtLogger.setWrapStyleWord(true);
                            scrollLog.setViewportView(txtLogger);
                        }
                        panel3.add(scrollLog);

                        //======== scrollPane3 ========
                        {
                            scrollPane3.setViewportView(tblAgents);
                        }
                        panel3.add(scrollPane3);

                        //---- btnRefreshAgents ----
                        btnRefreshAgents.setText("Refresh Agents");
                        panel3.add(btnRefreshAgents);
                    }
                    pnlStatus.add(panel3);
                }
                pnlMain.addTab("Status", pnlStatus);
            }
            mainPanel.add(pnlMain, CC.xywh(1, 3, 1, 7));
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
    private JPanel panel1;
    private JLabel label7;
    private JTextField txtURI;
    private JButton lblRestServerStatus;
    private JTabbedPane pnlMain;
    private JPanel pnlParams;
    private JTabbedPane pnlGames;
    private JPanel pnlFiles;
    private JLabel lblFile;
    private JPanel hSpacer1;
    private JButton btnFileNew;
    private JButton btnLoadFile;
    private JButton btnSaveFile;
    private JPanel pnlStatus;
    private JPanel panel3;
    private JCheckBox cbRefreshGameStatus;
    private JScrollPane scrollLog;
    private JTextArea txtLogger;
    private JScrollPane scrollPane3;
    private JTable tblAgents;
    private JButton btnRefreshAgents;
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
