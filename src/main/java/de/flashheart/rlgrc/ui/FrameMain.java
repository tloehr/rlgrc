/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc.ui;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import de.flashheart.rlgrc.jobs.ServerRefreshJob;
import de.flashheart.rlgrc.misc.Configs;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
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
import java.io.File;
import java.io.IOException;
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
        get("system/list_games");
    }

    private void btnSend(ActionEvent e) {

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
//        if (cbRefreshAgents.isSelected()) refreshAgents();
//        if (cbRefreshGameStatus.isSelected()) refreshStatus();
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
            set_response_status(response);
            response.close();
            if (entity.isEmpty()) result = new JSONObject();
            else result = new JSONObject(entity);
        } catch (Exception connectException) {
            addLog(connectException.getMessage());
            set_server_status(connectException);
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
            set_response_status(response);
            response.close();
        } catch (Exception connectException) {
            addLog(connectException.getMessage());
            set_server_status(connectException);
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
            set_response_status(response);
            response.close();
        } catch (Exception connectException) {
            addLog(connectException.getMessage());
            set_server_status(connectException);
        }
        return json;
    }

    void set_response_status(Response response) {
        String icon = "/artwork/ledyellow.png";
        if (response.getStatusInfo().getFamily().name().equalsIgnoreCase("CLIENT_ERROR") || response.getStatusInfo().getFamily().name().equalsIgnoreCase("SERVER_ERROR"))
            icon = "/artwork/ledred.png";
        if (response.getStatusInfo().getFamily().name().equalsIgnoreCase("SUCCESSFUL"))
            icon = "/artwork/ledgreen.png";
        btnServer.setIcon(new ImageIcon(getClass().getResource(icon)));
        btnServer.setText(Integer.toString(response.getStatusInfo().getStatusCode()));
        lblHTML.setText(response.getStatusInfo().getReasonPhrase());
    }

    void set_server_status(Exception exception) {
        btnServer.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
        btnServer.setText(null);
        btnServer.setToolTipText(exception.getMessage());
        lblHTML.setText("Connection Error");
    }

    private void btnSaveFile(ActionEvent e) {
        try {
            ((GameParams) pnlGames.getSelectedComponent()).save_file();
        } catch (IOException ex) {
            log.error(ex);
            addLog(ex.getMessage());
        }
    }

    private void btnLoadFile(ActionEvent e) {
        try {
            ((GameParams) pnlGames.getSelectedComponent()).load_file();
        } catch (IOException ex) {
            log.error(ex);
            addLog(ex.getMessage());
        }
    }


    private void btnFileNew(ActionEvent e) {
        ((GameParams) pnlGames.getSelectedComponent()).load_defaults();
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

    public void addLog(JSONObject jsonObject) {
        addLog(jsonObject.toString(4));
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


    private void tbKeyLockItemStateChanged(ItemEvent e) {
        btnPauseGame.setEnabled(e.getStateChange() != ItemEvent.SELECTED);
    }

    private void pnlGamesStateChanged(ChangeEvent e) {


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

    private void btnServer(ActionEvent e) {
        get("system/list_games");
    }

    /**
     * http://stackoverflow.com/questions/8741479/automatically-determine-optimal-fontcolor-by-backgroundcolor
     *
     * @param background
     * @return
     */
    public Color getForeground(Color background) {
        int red = 0;
        int green = 0;
        int blue = 0;

        if (background.getRed() + background.getGreen() + background.getBlue() < 383) {
            red = 255;
            green = 255;
            blue = 255;
        }
        return new Color(red, green, blue);
    }

    private void btnLoadGame(ActionEvent e) {
        addLog("--------------");
        addLog(post("game/load", GAMEID, ((GameParams) pnlGames.getSelectedComponent()).read_parameters().toString()));
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        createUIComponents();

        mainPanel = new JPanel();
        panel1 = new JPanel();
        label7 = new JLabel();
        txtURI = new JTextField();
        btnServer = new JButton();
        lblHTML = new JLabel();
        pnlMain = new JTabbedPane();
        pnlParams = new JPanel();
        pnlGames = new JTabbedPane();
        pnlFiles = new JPanel();
        lblFile = new JLabel();
        hSpacer1 = new JPanel(null);
        btnFileNew = new JButton();
        btnLoadFile = new JButton();
        btnSaveFile = new JButton();
        pnlServer = new JPanel();
        panel3 = new JPanel();
        cbRefreshGameStatus = new JCheckBox();
        scrollLog = new JScrollPane();
        txtLogger = new JTextArea();
        pnlAgents = new JPanel();
        scrollPane3 = new JScrollPane();
        btnRefreshAgents = new JButton();
        separator2 = new JSeparator();
        panel2 = new JPanel();
        btnLoadGame = new JButton();
        btnStartGame = new JButton();
        btnPauseGame = new JButton();
        btnResetGame = new JButton();
        btnUnloadGame = new JButton();
        hSpacer2 = new JPanel(null);
        tbKeyLock = new JToggleButton();

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
                label7.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                label7.setLabelFor(txtURI);
                panel1.add(label7);

                //---- txtURI ----
                txtURI.setText("http://localhost:8090");
                txtURI.setFont(new Font(".SF NS Text", Font.PLAIN, 16));
                txtURI.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        txtURIFocusLost(e);
                    }
                });
                panel1.add(txtURI);

                //---- btnServer ----
                btnServer.setText(null);
                btnServer.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
                btnServer.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                btnServer.addActionListener(e -> btnServer(e));
                panel1.add(btnServer);

                //---- lblHTML ----
                lblHTML.setText("text");
                lblHTML.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                panel1.add(lblHTML);
            }
            mainPanel.add(panel1, CC.xy(1, 1));

            //======== pnlMain ========
            {
                pnlMain.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                pnlMain.addChangeListener(e -> pnlGamesStateChanged(e));

                //======== pnlParams ========
                {
                    pnlParams.setLayout(new FormLayout(
                        "default:grow",
                        "default:grow, $lgap, default"));

                    //======== pnlGames ========
                    {
                        pnlGames.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                    }
                    pnlParams.add(pnlGames, CC.xy(1, 1, CC.DEFAULT, CC.FILL));

                    //======== pnlFiles ========
                    {
                        pnlFiles.setLayout(new BoxLayout(pnlFiles, BoxLayout.X_AXIS));

                        //---- lblFile ----
                        lblFile.setText("no file");
                        lblFile.setFont(new Font(".SF NS Text", Font.PLAIN, 16));
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
                    pnlParams.add(pnlFiles, CC.xy(1, 3));
                }
                pnlMain.addTab("Games", pnlParams);

                //======== pnlServer ========
                {
                    pnlServer.setLayout(new BoxLayout(pnlServer, BoxLayout.X_AXIS));

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
                    }
                    pnlServer.add(panel3);
                }
                pnlMain.addTab("Server", pnlServer);

                //======== pnlAgents ========
                {
                    pnlAgents.setLayout(new BoxLayout(pnlAgents, BoxLayout.X_AXIS));

                    //======== scrollPane3 ========
                    {
                        scrollPane3.setViewportView(tblAgents);
                    }
                    pnlAgents.add(scrollPane3);

                    //---- btnRefreshAgents ----
                    btnRefreshAgents.setText("Refresh Agents");
                    pnlAgents.add(btnRefreshAgents);
                }
                pnlMain.addTab("Agents", pnlAgents);
            }
            mainPanel.add(pnlMain, CC.xywh(1, 3, 1, 7));
        }
        contentPane.add(mainPanel, CC.xy(2, 2, CC.DEFAULT, CC.FILL));
        contentPane.add(separator2, CC.xy(2, 3));

        //======== panel2 ========
        {
            panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));

            //---- btnLoadGame ----
            btnLoadGame.setText("Load Game");
            btnLoadGame.setIcon(new ImageIcon(getClass().getResource("/artwork/irkickflash.png")));
            btnLoadGame.setToolTipText("Loag game on the server");
            btnLoadGame.setMinimumSize(new Dimension(38, 38));
            btnLoadGame.setPreferredSize(null);
            btnLoadGame.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            btnLoadGame.addActionListener(e -> btnLoadGame(e));
            panel2.add(btnLoadGame);

            //---- btnStartGame ----
            btnStartGame.setText("Run");
            btnStartGame.setToolTipText("Start loaded game");
            btnStartGame.setIcon(new ImageIcon(getClass().getResource("/artwork/player_play.png")));
            btnStartGame.setPreferredSize(null);
            btnStartGame.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            btnStartGame.addActionListener(e -> btnStartGame(e));
            panel2.add(btnStartGame);

            //---- btnPauseGame ----
            btnPauseGame.setText("Pause");
            btnPauseGame.setIcon(new ImageIcon(getClass().getResource("/artwork/player_pause.png")));
            btnPauseGame.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
            btnPauseGame.addActionListener(e -> btnPauseGame(e));
            panel2.add(btnPauseGame);

            //---- btnResetGame ----
            btnResetGame.setText("Reset");
            btnResetGame.setIcon(new ImageIcon(getClass().getResource("/artwork/player_rew.png")));
            btnResetGame.setToolTipText("Resume Game");
            btnResetGame.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
            btnResetGame.addActionListener(e -> btnResetGame(e));
            panel2.add(btnResetGame);

            //---- btnUnloadGame ----
            btnUnloadGame.setText("Unload");
            btnUnloadGame.setIcon(new ImageIcon(getClass().getResource("/artwork/player_eject.png")));
            btnUnloadGame.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
            btnUnloadGame.addActionListener(e -> btnUnloadGame(e));
            panel2.add(btnUnloadGame);
            panel2.add(hSpacer2);

            //---- tbKeyLock ----
            tbKeyLock.setText(null);
            tbKeyLock.setIcon(new ImageIcon(getClass().getResource("/artwork/decrypted.png")));
            tbKeyLock.setSelectedIcon(new ImageIcon(getClass().getResource("/artwork/encrypted.png")));
            tbKeyLock.setToolTipText("Keylock");
            tbKeyLock.addItemListener(e -> tbKeyLockItemStateChanged(e));
            panel2.add(tbKeyLock);
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
    private JButton btnServer;
    private JLabel lblHTML;
    private JTabbedPane pnlMain;
    private JPanel pnlParams;
    private JTabbedPane pnlGames;
    private JPanel pnlFiles;
    private JLabel lblFile;
    private JPanel hSpacer1;
    private JButton btnFileNew;
    private JButton btnLoadFile;
    private JButton btnSaveFile;
    private JPanel pnlServer;
    private JPanel panel3;
    private JCheckBox cbRefreshGameStatus;
    private JScrollPane scrollLog;
    private JTextArea txtLogger;
    private JPanel pnlAgents;
    private JScrollPane scrollPane3;
    private JTable tblAgents;
    private JButton btnRefreshAgents;
    private JSeparator separator2;
    private JPanel panel2;
    private JButton btnLoadGame;
    private JButton btnStartGame;
    private JButton btnPauseGame;
    private JButton btnResetGame;
    private JButton btnUnloadGame;
    private JPanel hSpacer2;
    private JToggleButton tbKeyLock;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
