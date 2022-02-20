/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc.ui;

import com.github.ankzz.dynamicfsm.action.FSMAction;
import com.github.ankzz.dynamicfsm.fsm.FSM;
import com.github.ankzz.dynamicfsm.states.FSMStateAction;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class FrameMain extends JFrame {
    private static final int TAB_GAMES = 0;
    private static final int TAB_SERVER = 1;
    private static final int TAB_AGENTS = 2;
    private boolean GAME_PAUSED;
    private final Scheduler scheduler;
    private final Configs configs;
    private SimpleTrigger agentTrigger;
    private final JobKey agentJob;
    private Client client = ClientBuilder.newClient();
    private final int MAX_LOG_LINES = 200;
    private final FSM guiFSM;
    private ArrayList<JToggleButton> GAME_SELECT_BUTTONS;
    private boolean connected;
    private final ButtonGroup buttonGroup1 = new ButtonGroup();
    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GAMEID = e.getActionCommand();
            guiFSM.ProcessFSM("game_slot_changed");
        }
    };
    private String GAMEID = "";

    public FrameMain(Configs configs) throws SchedulerException, IOException, ParserConfigurationException, SAXException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        this.configs = configs;
        this.connected = false;
        this.GAME_SELECT_BUTTONS = new ArrayList<>();
        this.agentJob = new JobKey(ServerRefreshJob.name, "group1");
        this.scheduler.getContext().put("rlgrc", this);
        this.scheduler.start();
        //connectionFSM = new FSM(this.getClass().getClassLoader().getResourceAsStream("fsm/connection.xml"), null);
        guiFSM = new FSM(this.getClass().getClassLoader().getResourceAsStream("fsm/gui.xml"), null);
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
        config_fsm();
        guiFSM.getCurrentState();
        guiFSM.getAllStates();
    }

    private void config_fsm() {
        guiFSM.setStatesAfterTransition("GAME_SLOT_CHANGED", (state, obj) -> {
            log.debug("FSM State: {}", state);
            JSONObject game_status = get("game/status", GAMEID);
            if (game_status.isEmpty()) {
                guiFSM.ProcessFSM("no_game_loaded");
            } else {
                guiFSM.ProcessFSM(game_status.getString("state"));
            }
        });
        guiFSM.setStatesAfterTransition("EDIT_GAME", (state, obj) -> {
            log.debug("FSM State: {}", state);
            pnlMain.setEnabledAt(TAB_GAMES, true);
            btnLoadGame.setEnabled(true);
            btnRun.setEnabled(false);
            btnPause.setEnabled(false);
            btnReset.setEnabled(false);
            btnUnload.setEnabled(false);
        });
        guiFSM.setStatesAfterTransition("PROLOG", (state, obj) -> {
            log.debug("FSM State: {}", state);
            pnlMain.setEnabledAt(TAB_GAMES, true);
            btnLoadGame.setEnabled(true);
            btnRun.setEnabled(true);
            btnPause.setEnabled(false);
            btnReset.setEnabled(false);
            btnUnload.setEnabled(true);
        });
        guiFSM.setStatesAfterTransition("PAUSING", (state, obj) -> {
            log.debug("FSM State: {}", state);
            btnLoadGame.setEnabled(false);
            btnRun.setEnabled(true);
            btnPause.setEnabled(true);
            btnReset.setEnabled(true);
            btnUnload.setEnabled(true);
        });
        guiFSM.setStatesAfterTransition("RUNNING", (state, obj) -> {
            log.debug("FSM State: {}", state);
            pnlMain.setEnabledAt(TAB_GAMES, false);
            btnLoadGame.setEnabled(false);
            btnRun.setEnabled(false);
            btnPause.setEnabled(true);
            btnReset.setEnabled(false);
            btnUnload.setEnabled(false);
        });
        guiFSM.setStatesAfterTransition("EPILOG", (state, obj) -> {
            log.debug("FSM State: {}", state);
            btnLoadGame.setEnabled(false);
            btnRun.setEnabled(false);
            btnPause.setEnabled(false);
            btnReset.setEnabled(true);
            btnUnload.setEnabled(true);
        });
    }

    private void btnConnect(ActionEvent e) {
        connect();
    }

    private void connect() {
        if (connected) return;
        try {
            int max_number_of_games = get("system/get_max_number_of_games").getInt("max_number_of_games");
            connected = true;
            for (int i = 0; i < max_number_of_games; i++) add_game_select_button(i + 1);
            GAME_SELECT_BUTTONS.get(0).doClick(); // always select the first one
            pnlMain.setEnabledAt(TAB_GAMES, false);
            pnlMain.setEnabledAt(TAB_SERVER, true);
            pnlMain.setEnabledAt(TAB_AGENTS, true);
        } catch (JSONException e) {
            log.error(e);
            disconnect();
            btnConnect.setToolTipText(e.getMessage());
        }
    }

    private void disconnect() {
        if (!connected) return;
        GAME_SELECT_BUTTONS.forEach(tb -> del_game_select_button(tb));
        GAME_SELECT_BUTTONS.clear();
        connected = false;
        pnlMain.setEnabled(false);
        btnLoadGame.setEnabled(false);
        btnRun.setEnabled(false);
        btnPause.setEnabled(false);
        btnReset.setEnabled(false);
        btnUnload.setEnabled(false);
        btnConnect.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
        btnConnect.setText(null);
        btnConnect.setToolTipText(null);
        pnlMain.setEnabledAt(TAB_GAMES, false);
        pnlMain.setEnabledAt(TAB_SERVER, false);
        pnlMain.setEnabledAt(TAB_AGENTS, false);
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
        btnPause.setIcon(new ImageIcon(getClass().getResource(GAME_PAUSED ? "/artwork/player_fwd.png" : "/artwork/player_pause.png")));
    }


//    private void refreshAgents() {
//        JSONObject request = get("system/list_agents");
//        SwingUtilities.invokeLater(() -> {
//            ((TM_Agents) tblAgents.getModel()).refresh_agents(request);
//        });
//    }

    public void refreshServer() {
//        if (cbRefreshAgents.isSelected()) refreshAgents();
//        if (cbRefreshGameStatus.isSelected()) refreshStatus();
    }

//    private void refreshStatus() {
//        JSONObject request = get("game/status", GAMEID);
//        try {
//            request.getJSONObject("payload").getString("message");
//            addLog("Game " + GAMEID + " not loaded on the server");
//        } catch (Exception e) {
//            addLog("--------------\n" + request.toString());
//            // funny - when an exception means NO EXPCETION on the server side
//        }
//    }

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
            connect();
        } catch (Exception connectException) {
            addLog(connectException.getMessage());
            set_response_status(connectException);
            disconnect();
        }
        return result;
    }

    private JSONObject get(String uri, String id) {
        JSONObject json;
        try {
            Response response = client
                    .target(txtURI.getText().trim() + "/api/" + uri)
                    .queryParam("id", id)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            json = new JSONObject(response.readEntity(String.class));
            set_response_status(response);
            response.close();
            connect();
        } catch (Exception connectException) {
            addLog(connectException.getMessage());
            set_response_status(connectException);
            disconnect();
            json = new JSONObject();
        }
        return json;
    }

    private JSONObject get(String uri) {
        JSONObject json;
        try {
            Response response = client
                    .target(txtURI.getText() + "/api/" + uri)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            json = new JSONObject(response.readEntity(String.class));
            set_response_status(response);
            response.close();
            disconnect();
        } catch (Exception connectException) {
            addLog(connectException.getMessage());
            set_response_status(connectException);
            disconnect();
            json = new JSONObject();
        }
        return json;
    }

    void set_response_status(Response response) {
        String icon = "/artwork/ledyellow.png";
        if (response.getStatusInfo().getFamily().name().equalsIgnoreCase("CLIENT_ERROR") || response.getStatusInfo().getFamily().name().equalsIgnoreCase("SERVER_ERROR"))
            icon = "/artwork/ledred.png";
        if (response.getStatusInfo().getFamily().name().equalsIgnoreCase("SUCCESSFUL"))
            icon = "/artwork/ledgreen.png";
        btnConnect.setIcon(new ImageIcon(getClass().getResource(icon)));
        btnConnect.setText(response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase());
    }

    void set_response_status(Exception exception) {
        String icon = "/artwork/ledred.png";
        btnConnect.setIcon(new ImageIcon(getClass().getResource(icon)));
        btnConnect.setText(exception.getMessage());
        btnConnect.setToolTipText(exception.toString());
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

    private void txtURIFocusLost(FocusEvent e) {
        configs.put(Configs.REST_URI, txtURI.getText().trim());
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


    private void pnlGamesPropertyChange(PropertyChangeEvent e) {
        log.debug(e);
        //btnLoadFile.setEnabled();
    }

    private void add_game_select_button(int gameid) {
        JToggleButton tb = new JToggleButton(Integer.toString(gameid));
        tb.setIcon(new ImageIcon(getClass().getResource("/artwork/led-white-off.png")));
        tb.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
        tb.setSelectedIcon(new ImageIcon(getClass().getResource("/artwork/led-white-on.png")));
        tb.setActionCommand(Integer.toString(gameid));
        pnlLoadedGames.add(tb);
        buttonGroup1.add(tb);
        tb.addActionListener(actionListener);
        GAME_SELECT_BUTTONS.add(tb);
    }

    private void del_game_select_button(JToggleButton tb) {
        pnlLoadedGames.remove(tb);
        buttonGroup1.remove(tb);
        tb.removeActionListener(actionListener);
    }

    private void btnLoadGame(ActionEvent e) {
        post("game/load", GAMEID, ((GameParams) pnlGames.getSelectedComponent()).read_parameters().toString());
        guiFSM.ProcessFSM("load_game");
    }

    private void btnRun(ActionEvent e) {
        if (guiFSM.getCurrentState().equalsIgnoreCase("PAUSING")) {
            post("game/resume", GAMEID);
        } else {
            post("game/start", GAMEID);
        }
        guiFSM.ProcessFSM("run");
    }

    private void btnPause(ActionEvent e) {
        post("game/pause", GAMEID);
        guiFSM.ProcessFSM("pause");
    }

    private void btnReset(ActionEvent e) {
        post("game/reset", GAMEID);
        guiFSM.ProcessFSM("reset");
    }

    private void btnUnload(ActionEvent e) {
        post("game/unload", GAMEID);
        guiFSM.ProcessFSM("unload");
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        createUIComponents();

        mainPanel = new JPanel();
        panel1 = new JPanel();
        txtURI = new JTextField();
        btnConnect = new JButton();
        pnlLoadedGames = new JPanel();
        panel2 = new JPanel();
        btnLoadGame = new JButton();
        btnRun = new JButton();
        btnPause = new JButton();
        btnReset = new JButton();
        btnUnload = new JButton();
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
                "default, $rgap, 2*(default, $lgap), default, $rgap, default, $lgap, fill:default:grow, $lgap, default"));

            //======== panel1 ========
            {
                panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));

                //---- txtURI ----
                txtURI.setText("http://localhost:8090");
                txtURI.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                txtURI.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        txtURIFocusLost(e);
                    }
                });
                panel1.add(txtURI);

                //---- btnConnect ----
                btnConnect.setText(null);
                btnConnect.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
                btnConnect.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                btnConnect.addActionListener(e -> btnConnect(e));
                panel1.add(btnConnect);
            }
            mainPanel.add(panel1, CC.xy(1, 1));

            //======== pnlLoadedGames ========
            {
                pnlLoadedGames.setLayout(new BoxLayout(pnlLoadedGames, BoxLayout.X_AXIS));
            }
            mainPanel.add(pnlLoadedGames, CC.xy(1, 3));

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
                btnLoadGame.setEnabled(false);
                btnLoadGame.addActionListener(e -> {
			btnLoadGame(e);
			btnLoadGame(e);
		});
                panel2.add(btnLoadGame);

                //---- btnRun ----
                btnRun.setText("Run");
                btnRun.setToolTipText("Start loaded game");
                btnRun.setIcon(new ImageIcon(getClass().getResource("/artwork/player_play.png")));
                btnRun.setPreferredSize(null);
                btnRun.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                btnRun.setEnabled(false);
                btnRun.addActionListener(e -> btnRun(e));
                panel2.add(btnRun);

                //---- btnPause ----
                btnPause.setText("Pause");
                btnPause.setIcon(new ImageIcon(getClass().getResource("/artwork/player_pause.png")));
                btnPause.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                btnPause.setEnabled(false);
                btnPause.addActionListener(e -> btnPause(e));
                panel2.add(btnPause);

                //---- btnReset ----
                btnReset.setText("Reset");
                btnReset.setIcon(new ImageIcon(getClass().getResource("/artwork/player-reset.png")));
                btnReset.setToolTipText("Resume Game");
                btnReset.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                btnReset.setEnabled(false);
                btnReset.addActionListener(e -> btnReset(e));
                panel2.add(btnReset);

                //---- btnUnload ----
                btnUnload.setText("Unload");
                btnUnload.setIcon(new ImageIcon(getClass().getResource("/artwork/player_eject.png")));
                btnUnload.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                btnUnload.setEnabled(false);
                btnUnload.addActionListener(e -> btnUnload(e));
                panel2.add(btnUnload);
            }
            mainPanel.add(panel2, CC.xy(1, 5));

            //======== pnlMain ========
            {
                pnlMain.setFont(new Font(".SF NS Text", Font.PLAIN, 18));

                //======== pnlParams ========
                {
                    pnlParams.setLayout(new FormLayout(
                        "default:grow",
                        "default:grow, $lgap, default"));

                    //======== pnlGames ========
                    {
                        pnlGames.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        pnlGames.addPropertyChangeListener("enabled", e -> pnlGamesPropertyChange(e));
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
            mainPanel.add(pnlMain, CC.xywh(1, 7, 1, 7));
        }
        contentPane.add(mainPanel, CC.xy(2, 2, CC.DEFAULT, CC.FILL));
        contentPane.add(separator2, CC.xy(2, 3));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel mainPanel;
    private JPanel panel1;
    private JTextField txtURI;
    private JButton btnConnect;
    private JPanel pnlLoadedGames;
    private JPanel panel2;
    private JButton btnLoadGame;
    private JButton btnRun;
    private JButton btnPause;
    private JButton btnReset;
    private JButton btnUnload;
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
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
