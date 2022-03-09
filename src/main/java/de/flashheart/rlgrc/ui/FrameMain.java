/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc.ui;

import com.github.ankzz.dynamicfsm.fsm.FSM;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
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
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class FrameMain extends JFrame {
    private static final int TAB_GAMES = 0;
    private static final int TAB_SERVER = 1;
    private static final int TAB_AGENTS = 2;
    private final Scheduler scheduler;
    private final Configs configs;

    private Client client = ClientBuilder.newClient();
    private final int MAX_LOG_LINES = 400;
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
        setTitle("rlgrc v" + configs.getBuildProperties("my.version") + " bld" + configs.getBuildProperties("buildNumber") + " " + configs.getBuildProperties("buildDate"));
//        this.agentJob = new JobKey(ServerRefreshJob.name, "group1");
//        this.scheduler.getContext().put("rlgrc", this);
//        this.scheduler.start();
        guiFSM = new FSM(this.getClass().getClassLoader().getResourceAsStream("fsm/gui.xml"), null);
        initComponents();
        initFrame();
//        initRefresh();
        pack();
    }

    private void initFrame() throws IOException {
        initLogger();
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "conquest"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "rush"));
        txtURI.setText(configs.get(Configs.REST_URI));

        tblAgents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblAgents.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            final DefaultListSelectionModel target = (DefaultListSelectionModel) e.getSource();
            int selection = target.getAnchorSelectionIndex();
            if (selection < 0) return;
            String state = ((TM_Agents) tblAgents.getModel()).getValueAt(selection);
            txtAgent.setText(tblAgents.getModel().getValueAt(selection, 0) + "\n\n" + state);
        });
        pnlGames.add("Conquest", new ConquestParams());

        config_fsm();
    }

    private void config_fsm() {
        guiFSM.setStatesAfterTransition("GAME_SLOT_CHANGED", (state, obj) -> {
            log.debug("FSM State: {}", state);
            JSONObject game_status = get("game/status", GAMEID);
            if (game_status.isEmpty()) {
                guiFSM.ProcessFSM("create_game");
            } else {
                guiFSM.ProcessFSM(game_status.getString("state"));
            }
        });
        guiFSM.setStatesAfterTransition("CREATE_GAME", (state, obj) -> {
            log.debug("FSM State: {}", state);
            pnlMain.setEnabledAt(TAB_GAMES, true);
            pnlMain.setSelectedIndex(TAB_GAMES);
            btnLoadGame.setEnabled(true);
            btnRun.setEnabled(false);
            btnPause.setEnabled(false);
            btnReset.setEnabled(false);
            btnUnload.setEnabled(false);
        });
        guiFSM.setStatesAfterTransition("PROLOG", (state, obj) -> {
            log.debug("FSM State: {}", state);
            pnlMain.setEnabledAt(TAB_GAMES, true);
            JSONObject params = get("game/parameters", GAMEID);
            if (params.isEmpty()) ((GameParams) pnlGames.getSelectedComponent()).load_defaults();
            else ((GameParams) pnlGames.getSelectedComponent()).set_parameters(params);
            btnLoadGame.setEnabled(true);
            btnRun.setEnabled(true);
            btnPause.setEnabled(false);
            btnReset.setEnabled(false);
            btnUnload.setEnabled(true);
        });
        guiFSM.setStatesAfterTransition("PAUSING", (state, obj) -> {
            log.debug("FSM State: {}", state);
            pnlMain.setEnabledAt(TAB_GAMES, false);
            pnlMain.setSelectedIndex(TAB_SERVER);
            btnLoadGame.setEnabled(false);
            btnRun.setEnabled(true);
            btnPause.setEnabled(false);
            btnReset.setEnabled(true);
            btnUnload.setEnabled(true);
        });
        guiFSM.setStatesAfterTransition("RUNNING", (state, obj) -> {
            log.debug("FSM State: {}", state);
            pnlMain.setEnabledAt(TAB_GAMES, false);
            pnlMain.setSelectedIndex(TAB_SERVER);
            btnLoadGame.setEnabled(false);
            btnRun.setEnabled(false);
            btnPause.setEnabled(true);
            btnReset.setEnabled(false);
            btnUnload.setEnabled(false);
        });
        // todo: how do we know that its over ?
        guiFSM.setStatesAfterTransition("EPILOG", (state, obj) -> {
            log.debug("FSM State: {}", state);
            pnlMain.setEnabledAt(TAB_GAMES, true);
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
            pnlMain.setEnabled(true);
//            btnRefreshServer.setEnabled(true);
//            btnRefreshAgents.setEnabled(true);
            GAME_SELECT_BUTTONS.get(0).doClick(); // always select the first one
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
        pnlMain.setEnabled(false);
    }

    public void refreshServer() {
        //todo: remove me
    }


    private void createUIComponents() {
        tblAgents = new JTable(new TM_Agents(new JSONObject(), configs));

        /**
         *  {
         *             @Override
         *             public String getToolTipText(MouseEvent e) {
         *                 //Implement table cell tool tips.
         *                 java.awt.Point p = e.getPoint();
         *                 int rowIndex = rowAtPoint(p);
         *                 TM_Agents model = (TM_Agents) getModel();
         *                 log.debug(model.getTooltipAt(rowIndex));
         *                 return "<html><p>" + model.getValueAt(rowIndex) + "</p></html>";
         *             }
         */

    }

    private JSONObject post(String uri, String id) {
        return post(uri, id, "{}");
    }

    private JSONObject post(String uri, String id, String body) {
        JSONObject json = new JSONObject();

        try {
            Response response = client
                    .target(txtURI.getText().trim() + "/api/" + uri)
                    .queryParam("id", id)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body));
            String entity = response.readEntity(String.class);
            if (entity.isEmpty()) json = new JSONObject();
            else json = new JSONObject(entity);
            set_response_status(response);
            addLog("\n\n" + response.getStatus() + " " + response.getStatusInfo().toString() + "\n" + json.toString(4));
            response.close();
            //connect();
        } catch (Exception connectException) {
            addLog(connectException.getMessage());
            set_response_status(connectException);
            disconnect();
        }
        return json;
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
            addLog("\n\n" + response.getStatus() + " " + response.getStatusInfo().toString() + "\n" + json.toString(4));
            response.close();
            //connect();
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
            File file = ((GameParams) pnlGames.getSelectedComponent()).save_file();
            if (file == null) lblFile.setText("no file");
            else lblFile.setText(file.getPath());
        } catch (IOException ex) {
            log.error(ex);
            addLog(ex.getMessage());
        }
    }

    private void btnLoadFile(ActionEvent e) {
        try {
            File file = ((GameParams) pnlGames.getSelectedComponent()).load_file();
            if (file == null) lblFile.setText("no file");
            else lblFile.setText(file.getPath());
        } catch (IOException ex) {
            log.error(ex);
            addLog(ex.getMessage());
        }
    }

    private void btnFileNew(ActionEvent e) {
        ((GameParams) pnlGames.getSelectedComponent()).load_defaults();
        lblFile.setText("no file");
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
            txtLogger.append(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)) + text + "\n");
            scrollLog.getVerticalScrollBar().setValue(scrollLog.getVerticalScrollBar().getMaximum());
        });
    }

    private void txtURIFocusLost(FocusEvent e) {
        configs.put(Configs.REST_URI, txtURI.getText().trim());
    }

    private void pnlGamesPropertyChange(PropertyChangeEvent e) {
        log.debug(e);
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

// source: https://stackoverflow.com/a/26046778
//    public MyJFrame() {
//        initComponents();
//        resizeColumns();
//        addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                resizeColumns();
//            }
//        });
//    }
//    //SUMS 1
//    float[] columnWidthPercentage = {0.2f, 0.55f, 0.1f, 0.05f, 0.05f, 0.05f};
//
//    private void resizeColumns() {
//        // Use TableColumnModel.getTotalColumnWidth() if your table is included in a JScrollPane
//        int tW = jTable1.getWidth();
//        TableColumn column;
//        TableColumnModel jTableColumnModel = jTable1.getColumnModel();
//        int cantCols = jTableColumnModel.getColumnCount();
//        for (int i = 0; i < cantCols; i++) {
//            column = jTableColumnModel.getColumn(i);
//            int pWidth = Math.round(columnWidthPercentage[i] * tW);
//            column.setPreferredWidth(pWidth);
//        }
//    }

    private void btnRefreshAgents(ActionEvent e) {
        JSONObject request = get("system/list_agents");
        SwingUtilities.invokeLater(() -> {
            tblAgents.getSelectionModel().clearSelection();
            txtAgent.setText(null);
            ((TM_Agents) tblAgents.getModel()).refresh_agents(request);
        });
    }

    private void btnRefreshServer(ActionEvent e) {
        get("game/status", GAMEID);
    }

    private void pnlMainStateChanged(ChangeEvent e) {
        if (pnlMain.getSelectedIndex() == TAB_AGENTS) {
            btnRefreshAgents(null);
        }
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
        scrollLog = new JScrollPane();
        txtLogger = new JTextArea();
        panel4 = new JPanel();
        btnRefreshServer = new JButton();
        pnlAgents = new JPanel();
        scrollPane3 = new JScrollPane();
        scrollPane1 = new JScrollPane();
        txtAgent = new JTextArea();
        panel3 = new JPanel();
        btnRefreshAgents = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "$ugap, default:grow, $ugap",
                "$rgap, default:grow"));

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
                pnlMain.setEnabled(false);
                pnlMain.addChangeListener(e -> pnlMainStateChanged(e));

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
                    pnlServer.setLayout(new BorderLayout());

                    //======== scrollLog ========
                    {

                        //---- txtLogger ----
                        txtLogger.setForeground(new Color(51, 255, 51));
                        txtLogger.setLineWrap(true);
                        txtLogger.setWrapStyleWord(true);
                        scrollLog.setViewportView(txtLogger);
                    }
                    pnlServer.add(scrollLog, BorderLayout.CENTER);

                    //======== panel4 ========
                    {
                        panel4.setLayout(new BoxLayout(panel4, BoxLayout.X_AXIS));

                        //---- btnRefreshServer ----
                        btnRefreshServer.setText("Refresh Server Status");
                        btnRefreshServer.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnRefreshServer.setIcon(new ImageIcon(getClass().getResource("/artwork/reload-on.png")));
                        btnRefreshServer.addActionListener(e -> btnRefreshServer(e));
                        panel4.add(btnRefreshServer);
                    }
                    pnlServer.add(panel4, BorderLayout.SOUTH);
                }
                pnlMain.addTab("Server", pnlServer);

                //======== pnlAgents ========
                {
                    pnlAgents.setLayout(new FormLayout(
                            "default:grow, $ugap, 32dlu:grow",
                            "fill:default:grow, fill:default"));

                    //======== scrollPane3 ========
                    {
                        scrollPane3.setViewportView(tblAgents);
                    }
                    pnlAgents.add(scrollPane3, CC.xy(1, 1));

                    //======== scrollPane1 ========
                    {

                        //---- txtAgent ----
                        txtAgent.setWrapStyleWord(true);
                        txtAgent.setLineWrap(true);
                        scrollPane1.setViewportView(txtAgent);
                    }
                    pnlAgents.add(scrollPane1, CC.xywh(3, 1, 1, 2));

                    //======== panel3 ========
                    {
                        panel3.setLayout(new BoxLayout(panel3, BoxLayout.X_AXIS));

                        //---- btnRefreshAgents ----
                        btnRefreshAgents.setText("Refresh Agents");
                        btnRefreshAgents.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnRefreshAgents.setIcon(new ImageIcon(getClass().getResource("/artwork/reload-on.png")));
                        btnRefreshAgents.addActionListener(e -> btnRefreshAgents(e));
                        panel3.add(btnRefreshAgents);
                    }
                    pnlAgents.add(panel3, CC.xywh(1, 2, 3, 1));
                }
                pnlMain.addTab("Agents", pnlAgents);
            }
            mainPanel.add(pnlMain, CC.xywh(1, 7, 1, 7));
        }
        contentPane.add(mainPanel, CC.xy(2, 2, CC.DEFAULT, CC.FILL));
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
    private JScrollPane scrollLog;
    private JTextArea txtLogger;
    private JPanel panel4;
    private JButton btnRefreshServer;
    private JPanel pnlAgents;
    private JScrollPane scrollPane3;
    private JTable tblAgents;
    private JScrollPane scrollPane1;
    private JTextArea txtAgent;
    private JPanel panel3;
    private JButton btnRefreshAgents;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
