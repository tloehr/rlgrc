/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.github.ankzz.dynamicfsm.fsm.FSM;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import de.flashheart.rlgrc.misc.Configs;
import de.flashheart.rlgrc.networking.SSEClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class FrameMain extends JFrame {

    // taken from Game.java of rlgcommander
    public static final String _state_PROLOG = "PROLOG";
    public static final String _state_TEAMS_NOT_READY = "TEAMS_NOT_READY";
    public static final String _state_TEAMS_READY = "TEAMS_READY";
    public static final String _state_RUNNING = "RUNNING";
    public static final String _state_PAUSING = "PAUSING";
    public static final String _state_RESUMING = "RESUMING";
    public static final String _state_EPILOG = "EPILOG";
    public static final String[] _states_ = new String[]{_state_PROLOG, _state_TEAMS_NOT_READY, _state_TEAMS_READY, _state_RUNNING, _state_PAUSING, _state_RESUMING, _state_EPILOG};
    public JButton[] _message_buttons;
    public JLabel[] _state_labels;

    // button enabled for every game state
    // 1 is enabled, 0 otherwise
    public static final int[][] state_buttons_enable = new int[][]{
            // prepare, reset, ready, run, pause, resume, continue, game_over
            {1, 1, 1, 1, 0, 0, 0, 0}, /* PROLOG */
            {0, 1, 1, 1, 0, 0, 0, 0}, /* TEAMS_NOT_READY */
            {0, 1, 0, 1, 0, 0, 0, 0}, /* TEAMS_READY */
            {0, 1, 0, 0, 1, 0, 0, 1}, /* RUNNING */
            {0, 1, 0, 0, 0, 1, 0, 0}, /* PAUSING */
            {0, 1, 0, 0, 0, 0, 1, 0}, /* RESUMING */
            {0, 1, 0, 0, 0, 0, 0, 0}  /* EPILOG */
    };

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
    private Optional<String> selected_agent;
    private final ButtonGroup buttonGroup1 = new ButtonGroup();
    private SSEClient sseClient;


    private String GAMEID = "";

    public FrameMain(Configs configs) throws SchedulerException, IOException, ParserConfigurationException, SAXException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        this.configs = configs;
        this.connected = false;
        this.selected_agent = Optional.empty();
        this.GAME_SELECT_BUTTONS = new ArrayList<>();

        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            FlatDarkLaf.setup();
        } catch (Exception ex) {
            log.fatal("Failed to initialize LaF");
            System.exit(0);
        }

        setTitle("rlgrc v" + configs.getBuildProperties("my.version") + " bld" + configs.getBuildProperties("buildNumber") + " " + configs.getBuildProperties("buildDate"));
        guiFSM = new FSM(this.getClass().getClassLoader().getResourceAsStream("fsm/gui.xml"), null);
        initComponents();

        _message_buttons = new JButton[]{btnPrepare, btnReset, btnReady, btnRun, btnPause, btnResume, btnContinue, btnGameOver};
        _state_labels = new JLabel[]{lblProlog, lblTeamsNotReady, lblTeamsReady, lblRunning, lblResuming, lblEpilog};

        initFrame();

    }

    private void initFrame() throws IOException {
        initLogger();
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "conquest"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "rush"));
        txtURI.setText(configs.get(Configs.REST_URI));

        tblAgents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblAgents.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selected_agent = Optional.empty();
            final DefaultListSelectionModel target = (DefaultListSelectionModel) e.getSource();
            if (target.isSelectionEmpty()) return;
            int selection = target.getAnchorSelectionIndex();
            if (selection < 0) return;
            String state = ((TM_Agents) tblAgents.getModel()).getValueAt(selection);
            txtAgent.setText(tblAgents.getModel().getValueAt(selection, 0) + "\n\n" + state);
            selected_agent = Optional.of(tblAgents.getModel().getValueAt(selection, 0).toString());
        });
        pnlGames.add("Conquest", new ConquestParams());

        button1.addActionListener(e -> testAction(e));
        button2.addActionListener(e -> testAction(e));
        button3.addActionListener(e -> testAction(e));
        button4.addActionListener(e -> testAction(e));
        button5.addActionListener(e -> testAction(e));
        button6.addActionListener(e -> testAction(e));
        button7.addActionListener(e -> testAction(e));
        button8.addActionListener(e -> testAction(e));
        button9.addActionListener(e -> testAction(e));
        button10.addActionListener(e -> testAction(e));

        btnPrepare.addActionListener(e -> send_message(e));
        btnReady.addActionListener(e -> send_message(e));
        btnReset.addActionListener(e -> send_message(e));
        btnRun.addActionListener(e -> send_message(e));
        btnPause.addActionListener(e -> send_message(e));
        btnResume.addActionListener(e -> send_message(e));
        btnContinue.addActionListener(e -> send_message(e));
        btnGameOver.addActionListener(e -> send_message(e));

    }

    private void testAction(ActionEvent e) {
        if (selected_agent.isEmpty()) return;
        Properties properties = new Properties();
        properties.put("agentid", selected_agent.get());
        properties.put("deviceid", e.getActionCommand());
        post("system/test_agent", "{}", properties);
    }

    private void set_gui(JSONObject game_state) {
        String current_state = game_state.isEmpty() ? "CREATE_GAME" : game_state.getString("game_state");
        log.debug("Game State: {}", current_state);
        switch (current_state) {
            case "CREATE_GAME":
            case _state_PROLOG: {
                gui_state_create_game();
                break;
            }
            case _state_PAUSING: {

                break;
            }
            default: {
            }
        }
    }

    private void gui_state_create_game() {
        pnlMain.setEnabledAt(TAB_GAMES, true);
        pnlMain.setSelectedIndex(TAB_GAMES);
        btnLoadGame.setEnabled(true);
        btnRun.setEnabled(false);
        btnPause.setEnabled(false);
        btnReset.setEnabled(false);
        btnUnload.setEnabled(false);
    }

    private void gui_state_teams_ready() {

    }

    private void gui_state_teams_not_ready() {

    }

    private void gui_state_pausing() {
        pnlMain.setEnabledAt(TAB_GAMES, false);
        pnlMain.setSelectedIndex(TAB_SERVER);
        btnLoadGame.setEnabled(false);
        btnRun.setEnabled(true);
        btnPause.setEnabled(false);
        btnReset.setEnabled(true);
        btnUnload.setEnabled(true);
    }

    private void gui_state_running() {
        pnlMain.setEnabledAt(TAB_GAMES, false);
        pnlMain.setSelectedIndex(TAB_SERVER);
        btnLoadGame.setEnabled(false);
        btnRun.setEnabled(false);
        btnPause.setEnabled(true);
        btnReset.setEnabled(false);
        btnUnload.setEnabled(false);
    }

    private void gui_state_epilog() {
        pnlMain.setEnabledAt(TAB_GAMES, true);
        btnLoadGame.setEnabled(false);
        btnRun.setEnabled(false);
        btnPause.setEnabled(false);
        btnReset.setEnabled(true);
        btnUnload.setEnabled(true);
    }

    private void set_gui_state_to(String state) {
        int index = Arrays.asList(_states_).indexOf(state.toUpperCase());
        // States
        for (int i = 0; i < 7; i++) {
            _state_labels[i].setEnabled(state.equalsIgnoreCase(_state_labels[i].getName()));
        }
        // Messages
        for (int i = 0; i < 8; i++) {
            boolean enabled = Boolean.valueOf(state_buttons_enable[index][i] == 1 ? true : false);
            _message_buttons[i].setEnabled(enabled);
        }
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
        sseClient.shutdown();
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

    private JSONObject process(String id, String message) {
        Properties properties = new Properties();
        properties.put("id", id);
        properties.put("message", message);
        return post("game/process", "{}", properties);
    }

    /**
     * conveniance method
     *
     * @param uri
     * @param body
     * @param id
     * @return
     */
    private JSONObject post(String uri, String body, String id) {
        Properties properties = new Properties();
        properties.put("id", id);
        return post(uri, body, properties);
    }

    /**
     * conveniance method
     *
     * @param uri
     * @param id
     * @return
     */
    private JSONObject post(String uri, String id) {
        return post(uri, "{}", id);
    }

    /**
     * posts a REST request.
     *
     * @param uri
     * @param body
     * @param params
     * @return
     */
    private JSONObject post(String uri, String body, Properties params) {
        JSONObject json = new JSONObject();

        try {
            WebTarget target = client
                    .target(txtURI.getText().trim() + "/api/" + uri);

            for (Map.Entry entry : params.entrySet()) {
                target = target.queryParam(entry.getKey().toString(), entry.getValue());
            }

            Response response = target
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
            ((GameParams) pnlGames.getSelectedComponent()).save_file();
            lblFile.setText(((GameParams) pnlGames.getSelectedComponent()).getFilename());
        } catch (IOException ex) {
            log.error(ex);
            addLog(ex.getMessage());
        }
    }

    private void btnLoadFile(ActionEvent e) {
        try {
            ((GameParams) pnlGames.getSelectedComponent()).load_file();
            lblFile.setText(((GameParams) pnlGames.getSelectedComponent()).getFilename());
        } catch (IOException ex) {
            log.error(ex);
            addLog(ex.getMessage());
        }
    }

    private void btnFileNew(ActionEvent e) {
        ((GameParams) pnlGames.getSelectedComponent()).load_defaults();
        lblFile.setText(((GameParams) pnlGames.getSelectedComponent()).getFilename());
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
        tb.addActionListener(e -> game_selected(e));
        GAME_SELECT_BUTTONS.add(tb);
    }

    private void game_selected(ActionEvent e) {
        GAMEID = e.getActionCommand();
        if (sseClient != null) sseClient.shutdown();
        sseClient = SSEClient.builder()
                .url(txtURI.getText().trim() + "/api/game-sse?id=" + GAMEID)
                .useKeepAliveMechanismIfReceived(false)
                .eventHandler(log::debug)
                .build();
        sseClient.start();
        set_gui(get("game/status", GAMEID));
    }

    private void del_game_select_button(JToggleButton tb) {
        pnlLoadedGames.remove(tb);
        buttonGroup1.remove(tb);
    }

    private void btnLoadGame(ActionEvent e) {
        String params = ((GameParams) pnlGames.getSelectedComponent()).read_parameters().toString(4);
        log.debug(params);
        post("game/load", params, GAMEID);
        guiFSM.ProcessFSM("load_game");
    }

    private void btnRun(ActionEvent e) {
        if (guiFSM.getCurrentState().equalsIgnoreCase("PAUSING")) {
            process(GAMEID, "resume");
        } else {
            process(GAMEID, "prepare");
        }
        guiFSM.ProcessFSM("run");
    }

    private void btnPause(ActionEvent e) {
        process(GAMEID, "pause");
        guiFSM.ProcessFSM("pause");
    }

    private void btnReset(ActionEvent e) {
        process(GAMEID, "reset");
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
            selected_agent = Optional.empty();
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

    private void send_message(ActionEvent e) {
        Properties props = new Properties();
        props.put("id", GAMEID);
        props.put("message", e.getActionCommand());
        post("game/process", "{}", props);
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        createUIComponents();

        mainPanel = new JPanel();
        panel1 = new JPanel();
        txtURI = new JTextField();
        btnConnect = new JButton();
        pnlLoadedGames = new JPanel();
        pnlGameStates = new JPanel();
        lblProlog = new JLabel();
        lblTeamsNotReady = new JLabel();
        lblTeamsReady = new JLabel();
        lblRunning = new JLabel();
        lblPausing = new JLabel();
        lblResuming = new JLabel();
        lblEpilog = new JLabel();
        pnlMain = new JTabbedPane();
        pnlParams = new JPanel();
        pnlGames = new JTabbedPane();
        pnlFiles = new JPanel();
        btnFileNew = new JButton();
        btnLoadFile = new JButton();
        btnSaveFile = new JButton();
        btnLoadGame = new JButton();
        btnUnload = new JButton();
        hSpacer1 = new JPanel(null);
        lblFile = new JLabel();
        hSpacer2 = new JPanel(null);
        pnlServer = new JPanel();
        scrollLog = new JScrollPane();
        txtLogger = new JTextArea();
        panel4 = new JPanel();
        btnRefreshServer = new JButton();
        pnlAgents = new JPanel();
        panel7 = new JSplitPane();
        scrollPane3 = new JScrollPane();
        scrollPane1 = new JScrollPane();
        txtAgent = new JTextArea();
        pnlTesting = new JPanel();
        label1 = new JLabel();
        button1 = new JButton();
        button2 = new JButton();
        button3 = new JButton();
        button4 = new JButton();
        button5 = new JButton();
        button6 = new JButton();
        button7 = new JButton();
        button8 = new JButton();
        button9 = new JButton();
        button10 = new JButton();
        btnRefreshAgents = new JButton();
        panel3 = new JPanel();
        btnPrepare = new JButton();
        btnReset = new JButton();
        btnReady = new JButton();
        btnRun = new JButton();
        btnPause = new JButton();
        btnResume = new JButton();
        btnContinue = new JButton();
        btnGameOver = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "$ugap, default:grow, $ugap",
            "$rgap, default:grow"));

        //======== mainPanel ========
        {
            mainPanel.setLayout(new FormLayout(
                "default:grow, $rgap, default",
                "default, $rgap, 2*(default, $lgap), default, $rgap, default, $lgap, fill:default:grow"));

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
            mainPanel.add(panel1, CC.xywh(1, 1, 3, 1));

            //======== pnlLoadedGames ========
            {
                pnlLoadedGames.setLayout(new BoxLayout(pnlLoadedGames, BoxLayout.X_AXIS));
            }
            mainPanel.add(pnlLoadedGames, CC.xywh(1, 3, 3, 1));

            //======== pnlGameStates ========
            {
                pnlGameStates.setLayout(new HorizontalLayout(10));

                //---- lblProlog ----
                lblProlog.setText("Prolog");
                lblProlog.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                lblProlog.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
                lblProlog.setDisabledIcon(new ImageIcon(getClass().getResource("/artwork/leddarkred.png")));
                lblProlog.setName("PROLOG");
                pnlGameStates.add(lblProlog);

                //---- lblTeamsNotReady ----
                lblTeamsNotReady.setText("Teams not Ready");
                lblTeamsNotReady.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                lblTeamsNotReady.setIcon(new ImageIcon(getClass().getResource("/artwork/ledorange.png")));
                lblTeamsNotReady.setDisabledIcon(new ImageIcon(getClass().getResource("/artwork/leddarkorange.png")));
                lblTeamsNotReady.setName("TEAMS_NOT_READY");
                pnlGameStates.add(lblTeamsNotReady);

                //---- lblTeamsReady ----
                lblTeamsReady.setText("Teams Ready");
                lblTeamsReady.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                lblTeamsReady.setIcon(new ImageIcon(getClass().getResource("/artwork/ledyellow.png")));
                lblTeamsReady.setDisabledIcon(new ImageIcon(getClass().getResource("/artwork/leddarkyellow.png")));
                lblTeamsReady.setName("TEAMS_READY");
                pnlGameStates.add(lblTeamsReady);

                //---- lblRunning ----
                lblRunning.setText("Running");
                lblRunning.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                lblRunning.setIcon(new ImageIcon(getClass().getResource("/artwork/ledgreen.png")));
                lblRunning.setDisabledIcon(new ImageIcon(getClass().getResource("/artwork/leddarkgreen.png")));
                lblRunning.setName("RUNNING");
                pnlGameStates.add(lblRunning);

                //---- lblPausing ----
                lblPausing.setText("Pausing");
                lblPausing.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                lblPausing.setIcon(new ImageIcon(getClass().getResource("/artwork/ledlightblue.png")));
                lblPausing.setDisabledIcon(new ImageIcon(getClass().getResource("/artwork/leddarkcyan.png")));
                lblPausing.setName("PAUSING");
                pnlGameStates.add(lblPausing);

                //---- lblResuming ----
                lblResuming.setText("Resuming");
                lblResuming.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                lblResuming.setIcon(new ImageIcon(getClass().getResource("/artwork/ledblue.png")));
                lblResuming.setDisabledIcon(new ImageIcon(getClass().getResource("/artwork/leddarkblue.png")));
                lblResuming.setName("RESUMING");
                pnlGameStates.add(lblResuming);

                //---- lblEpilog ----
                lblEpilog.setText("Epilog");
                lblEpilog.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                lblEpilog.setIcon(new ImageIcon(getClass().getResource("/artwork/ledpurple.png")));
                lblEpilog.setDisabledIcon(new ImageIcon(getClass().getResource("/artwork/leddarkpurple.png")));
                pnlGameStates.add(lblEpilog);
            }
            mainPanel.add(pnlGameStates, CC.xywh(1, 5, 3, 1, CC.LEFT, CC.DEFAULT));

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

                        //---- btnLoadGame ----
                        btnLoadGame.setText(null);
                        btnLoadGame.setIcon(new ImageIcon(getClass().getResource("/artwork/upload.png")));
                        btnLoadGame.setToolTipText("Load game on the server");
                        btnLoadGame.setMinimumSize(new Dimension(38, 38));
                        btnLoadGame.setPreferredSize(new Dimension(38, 38));
                        btnLoadGame.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        btnLoadGame.addActionListener(e -> btnLoadGame(e));
                        pnlFiles.add(btnLoadGame);

                        //---- btnUnload ----
                        btnUnload.setText(null);
                        btnUnload.setIcon(new ImageIcon(getClass().getResource("/artwork/unload.png")));
                        btnUnload.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnUnload.setToolTipText("Remove the loaded game from the commander's memory.");
                        btnUnload.setHorizontalAlignment(SwingConstants.LEFT);
                        btnUnload.setPreferredSize(new Dimension(38, 38));
                        btnUnload.addActionListener(e -> btnUnload(e));
                        pnlFiles.add(btnUnload);
                        pnlFiles.add(hSpacer1);

                        //---- lblFile ----
                        lblFile.setText("no file");
                        lblFile.setFont(new Font(".SF NS Text", Font.PLAIN, 16));
                        pnlFiles.add(lblFile);
                        pnlFiles.add(hSpacer2);
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
                        txtLogger.setEditable(false);
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
                        "default:grow, $ugap, default",
                        "fill:default:grow"));

                    //======== panel7 ========
                    {

                        //======== scrollPane3 ========
                        {
                            scrollPane3.setViewportView(tblAgents);
                        }
                        panel7.setLeftComponent(scrollPane3);

                        //======== scrollPane1 ========
                        {

                            //---- txtAgent ----
                            txtAgent.setWrapStyleWord(true);
                            txtAgent.setLineWrap(true);
                            txtAgent.setEditable(false);
                            scrollPane1.setViewportView(txtAgent);
                        }
                        panel7.setRightComponent(scrollPane1);
                    }
                    pnlAgents.add(panel7, CC.xy(1, 1));

                    //======== pnlTesting ========
                    {
                        pnlTesting.setLayout(new FormLayout(
                            "left:default:grow",
                            "10*(fill:default), default, fill:9dlu:grow, default"));

                        //---- label1 ----
                        label1.setText("Agent Testing");
                        label1.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        label1.setIcon(new ImageIcon(getClass().getResource("/artwork/infoblue.png")));
                        label1.setHorizontalTextPosition(SwingConstants.LEADING);
                        label1.setToolTipText("Sends a test signal to the selected agent. Signal length is 1 second.");
                        pnlTesting.add(label1, CC.xy(1, 1, CC.FILL, CC.DEFAULT));

                        //---- button1 ----
                        button1.setText("White");
                        button1.setActionCommand("led_wht");
                        button1.setIcon(new ImageIcon(getClass().getResource("/artwork/led-white-on.png")));
                        button1.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button1.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button1, CC.xy(1, 2, CC.FILL, CC.DEFAULT));

                        //---- button2 ----
                        button2.setText("Red");
                        button2.setActionCommand("led_red");
                        button2.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
                        button2.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button2.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button2, CC.xy(1, 3, CC.FILL, CC.DEFAULT));

                        //---- button3 ----
                        button3.setText("Yellow");
                        button3.setActionCommand("led_ylw");
                        button3.setIcon(new ImageIcon(getClass().getResource("/artwork/ledyellow.png")));
                        button3.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button3.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button3, CC.xy(1, 4, CC.FILL, CC.DEFAULT));

                        //---- button4 ----
                        button4.setText("Green");
                        button4.setActionCommand("led_grn");
                        button4.setIcon(new ImageIcon(getClass().getResource("/artwork/ledgreen.png")));
                        button4.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button4.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button4, CC.xy(1, 5, CC.FILL, CC.DEFAULT));

                        //---- button5 ----
                        button5.setText("Blue");
                        button5.setActionCommand("led_blu");
                        button5.setIcon(new ImageIcon(getClass().getResource("/artwork/ledblue.png")));
                        button5.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button5.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button5, CC.xy(1, 6, CC.FILL, CC.DEFAULT));

                        //---- button6 ----
                        button6.setText("Buzzer");
                        button6.setActionCommand("buzzer");
                        button6.setIcon(new ImageIcon(getClass().getResource("/artwork/buzzer.png")));
                        button6.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button6.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button6, CC.xy(1, 7, CC.FILL, CC.DEFAULT));

                        //---- button7 ----
                        button7.setText("Siren 1");
                        button7.setActionCommand("sir1");
                        button7.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button7.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button7.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button7, CC.xy(1, 8, CC.FILL, CC.DEFAULT));

                        //---- button8 ----
                        button8.setText("Siren 2");
                        button8.setActionCommand("sir2");
                        button8.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button8.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button8.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button8, CC.xy(1, 9, CC.FILL, CC.DEFAULT));

                        //---- button9 ----
                        button9.setText("Siren 3");
                        button9.setActionCommand("sir3");
                        button9.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button9.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button9.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button9, CC.xy(1, 10, CC.FILL, CC.DEFAULT));

                        //---- button10 ----
                        button10.setText("Siren 4");
                        button10.setActionCommand("sir4");
                        button10.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button10.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button10.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button10, CC.xy(1, 11, CC.FILL, CC.DEFAULT));

                        //---- btnRefreshAgents ----
                        btnRefreshAgents.setText("Update");
                        btnRefreshAgents.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnRefreshAgents.setIcon(new ImageIcon(getClass().getResource("/artwork/reload-on.png")));
                        btnRefreshAgents.addActionListener(e -> btnRefreshAgents(e));
                        pnlTesting.add(btnRefreshAgents, CC.xy(1, 13, CC.FILL, CC.DEFAULT));
                    }
                    pnlAgents.add(pnlTesting, CC.xy(3, 1));
                }
                pnlMain.addTab("Agents", pnlAgents);
            }
            mainPanel.add(pnlMain, CC.xywh(1, 7, 1, 5));

            //======== panel3 ========
            {
                panel3.setLayout(new VerticalLayout());

                //---- btnPrepare ----
                btnPrepare.setText("Prepare");
                btnPrepare.setToolTipText("Start loaded game");
                btnPrepare.setIcon(null);
                btnPrepare.setPreferredSize(null);
                btnPrepare.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                btnPrepare.setActionCommand("prepare");
                panel3.add(btnPrepare);

                //---- btnReset ----
                btnReset.setText("Reset");
                btnReset.setToolTipText("Start loaded game");
                btnReset.setIcon(null);
                btnReset.setPreferredSize(null);
                btnReset.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                btnReset.setActionCommand("reset");
                panel3.add(btnReset);

                //---- btnReady ----
                btnReady.setText("Ready");
                btnReady.setToolTipText("Start loaded game");
                btnReady.setIcon(null);
                btnReady.setPreferredSize(null);
                btnReady.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                btnReady.setActionCommand("ready");
                panel3.add(btnReady);

                //---- btnRun ----
                btnRun.setText("Run");
                btnRun.setIcon(null);
                btnRun.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                btnRun.setToolTipText("Pause the running game");
                btnRun.setActionCommand("run");
                panel3.add(btnRun);

                //---- btnPause ----
                btnPause.setText("Pause");
                btnPause.setIcon(null);
                btnPause.setToolTipText("Reset the game to the state as if it was just loaded.");
                btnPause.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                btnPause.setActionCommand("pause");
                panel3.add(btnPause);

                //---- btnResume ----
                btnResume.setText("Resume");
                btnResume.setIcon(null);
                btnResume.setToolTipText("Reset the game to the state as if it was just loaded.");
                btnResume.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                btnResume.setActionCommand("resume");
                panel3.add(btnResume);

                //---- btnContinue ----
                btnContinue.setText("Continue");
                btnContinue.setIcon(null);
                btnContinue.setToolTipText("Reset the game to the state as if it was just loaded.");
                btnContinue.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                btnContinue.setActionCommand("continue");
                panel3.add(btnContinue);

                //---- btnGameOver ----
                btnGameOver.setText("Game Over");
                btnGameOver.setIcon(null);
                btnGameOver.setToolTipText("Reset the game to the state as if it was just loaded.");
                btnGameOver.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                btnGameOver.setActionCommand("game_over");
                panel3.add(btnGameOver);
            }
            mainPanel.add(panel3, CC.xywh(3, 7, 1, 5, CC.FILL, CC.DEFAULT));
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
    private JPanel pnlGameStates;
    private JLabel lblProlog;
    private JLabel lblTeamsNotReady;
    private JLabel lblTeamsReady;
    private JLabel lblRunning;
    private JLabel lblPausing;
    private JLabel lblResuming;
    private JLabel lblEpilog;
    private JTabbedPane pnlMain;
    private JPanel pnlParams;
    private JTabbedPane pnlGames;
    private JPanel pnlFiles;
    private JButton btnFileNew;
    private JButton btnLoadFile;
    private JButton btnSaveFile;
    private JButton btnLoadGame;
    private JButton btnUnload;
    private JPanel hSpacer1;
    private JLabel lblFile;
    private JPanel hSpacer2;
    private JPanel pnlServer;
    private JScrollPane scrollLog;
    private JTextArea txtLogger;
    private JPanel panel4;
    private JButton btnRefreshServer;
    private JPanel pnlAgents;
    private JSplitPane panel7;
    private JScrollPane scrollPane3;
    private JTable tblAgents;
    private JScrollPane scrollPane1;
    private JTextArea txtAgent;
    private JPanel pnlTesting;
    private JLabel label1;
    private JButton button1;
    private JButton button2;
    private JButton button3;
    private JButton button4;
    private JButton button5;
    private JButton button6;
    private JButton button7;
    private JButton button8;
    private JButton button9;
    private JButton button10;
    private JButton btnRefreshAgents;
    private JPanel panel3;
    private JButton btnPrepare;
    private JButton btnReset;
    private JButton btnReady;
    private JButton btnRun;
    private JButton btnPause;
    private JButton btnResume;
    private JButton btnContinue;
    private JButton btnGameOver;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
