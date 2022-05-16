/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc.ui;

import com.google.common.io.Resources;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import de.flashheart.rlgrc.jobs.FlashStateLedJob;
import de.flashheart.rlgrc.misc.Configs;
import de.flashheart.rlgrc.misc.JavaTimeConverter;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DateFormatter;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.*;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

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
    public final List<String> _states_;
    public final List<JButton> _message_buttons;
    public final List<JLabel> _state_labels;

    // button enabled for every game state
    // 1 is enabled, 0 otherwise
    public static final int[][] state_buttons_enable_table = new int[][]{
            // prepare, reset, ready, run, pause, resume, continue, game_over
            {1, 1, 1, 1, 0, 0, 0, 0}, /* PROLOG */
            {0, 1, 1, 1, 0, 0, 0, 0}, /* TEAMS_NOT_READY */
            {0, 1, 0, 1, 0, 0, 0, 0}, /* TEAMS_READY */
            {0, 1, 0, 0, 1, 0, 0, 1}, /* RUNNING */
            {0, 1, 0, 0, 0, 1, 0, 0}, /* PAUSING */
            {0, 1, 0, 0, 0, 0, 1, 0}, /* RESUMING */
            {0, 1, 0, 0, 0, 0, 0, 0} /* EPILOG */
    };

    private static final int TAB_SETUP = 0;
    private static final int TAB_RUNNING_GAME = 1;
    private static final int TAB_SERVER = 2;
    private static final int TAB_AGENTS = 3;
    private static final int TAB_ABOUT = 4;
    private final Scheduler scheduler;
    private final JobKey state_flashing_job;


    private JSONObject current_state;
    private final Configs configs;

    private Client client = ClientBuilder.newClient();
    private final int MAX_LOG_LINES = 400;
    private ArrayList<JToggleButton> GAME_SELECT_BUTTONS;
    private boolean connected;
    private Optional<String> selected_agent;
    private SSEClient sseClient;
    private LocalDateTime last_sse_received;

    public FrameMain(Configs configs) throws SchedulerException, IOException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        this.scheduler.getContext().put("rlgrc", this);
        this.configs = configs;
        this.connected = false;
        this.selected_agent = Optional.empty();
        this.GAME_SELECT_BUTTONS = new ArrayList<>();
        this.current_state = new JSONObject();
        this.state_flashing_job = new JobKey(FlashStateLedJob.name, "group1");

        initComponents();
        setTitle("rlgrc v" + configs.getBuildProperties("my.version") + " bld" + configs.getBuildProperties("buildNumber") + " " + configs.getBuildProperties("buildDate"));
        //guiFSM = new FSM(this.getClass().getClassLoader().getResourceAsStream("fsm/gui.xml"), null);

        pnlMain.setSelectedIndex(TAB_ABOUT);
        txtAbout.setText(Resources.toString(Resources.getResource("about.html"), Charset.defaultCharset()));

        this._states_ = Arrays.asList(_state_PROLOG, _state_TEAMS_NOT_READY, _state_TEAMS_READY, _state_RUNNING, _state_PAUSING, _state_RESUMING, _state_EPILOG);
        this._message_buttons = Arrays.asList(btnPrepare, btnReset, btnReady, btnRun, btnPause, btnResume, btnContinue, btnGameOver);
        this._state_labels = Arrays.asList(lblProlog, lblTeamsNotReady, lblTeamsReady, lblRunning, lblPausing, lblResuming, lblEpilog);

        initFrame();
    }

    private void initFrame() throws IOException, SchedulerException {
        initLogger();
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "conquest"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "rush"));
        txtURI.setText(configs.get(Configs.REST_URI));

        tblAgents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblAgents.getSelectionModel().addListSelectionListener(e -> table_of_agents_changed_selection(e));
        //pnlGames.add("Conquest", new ConquestParams());

        for (GameParams gameParam : Arrays.asList(new ConquestParams())) {
            cmbGameModes.addItem(gameParam);
        }

        cmbGameSlots.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            String text = "--";
            if (value != null) {
                text = "#" + value;
            }
            return new DefaultListCellRenderer().getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        });
        cmbGameModes.setRenderer((list, value, index, isSelected, cellHasFocus) -> new DefaultListCellRenderer().getListCellRendererComponent(list, ((GameParams) value).getMode(), index, isSelected, cellHasFocus));

        for (JButton testButton : Arrays.asList(button1, button2, button2, button3, button4, button5, button6, button7, button8, button9, button10)) {
            testButton.addActionListener(e -> {
                if (selected_agent.isEmpty()) return;
                Properties properties = new Properties();
                properties.put("agentid", selected_agent.get());
                properties.put("deviceid", e.getActionCommand());
                post("system/test_agent", "{}", properties);
            });
        }

        for (JButton msg_button : _message_buttons) {
            msg_button.addActionListener(e -> {
                Properties props = new Properties();
                props.put("id", current_game_id());
                props.put("message", e.getActionCommand());
                post("game/process", "{}", props);
            });
        }

        JobDetail job = newJob(FlashStateLedJob.class)
                .withIdentity(state_flashing_job)
                .build();

        SimpleTrigger state_flashing_trigger = newTrigger()
                .withIdentity(FlashStateLedJob.name + "-trigger", "group1")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(1)
                        .repeatForever())
                .build();
        scheduler.scheduleJob(job, state_flashing_trigger);
        pnlMain.setEnabled(false);
    }

    /**
     * called every time the user selects a different agent in the table
     *
     * @param e
     */
    private void table_of_agents_changed_selection(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        selected_agent = Optional.empty();
        final DefaultListSelectionModel target = (DefaultListSelectionModel) e.getSource();
        if (target.isSelectionEmpty()) return;
        int selection = target.getAnchorSelectionIndex();
        if (selection < 0) return;
        String state = ((TM_Agents) tblAgents.getModel()).getValueAt(selection);
        txtAgent.setText(tblAgents.getModel().getValueAt(selection, 0) + "\n\n" + state);
        selected_agent = Optional.of(tblAgents.getModel().getValueAt(selection, 0).toString());
    }

    private void set_gui_to_situation() {
        pnlMain.setEnabled(connected);
        cmbGameSlots.setEnabled(connected);
        btnConnect.setIcon(new ImageIcon(getClass().getResource(connected ? "/artwork/connected.png" : "/artwork/disconnected.png")));
        if (!connected) {
            pnlMain.setSelectedIndex(TAB_ABOUT);
            return;
        }

        pnlMain.setEnabledAt(TAB_SETUP, current_state.isEmpty() || current_state.getString("game_state").equals(_state_PROLOG));
        pnlMain.setEnabledAt(TAB_RUNNING_GAME, !current_state.isEmpty());
        if (!current_state.isEmpty()) update_running_game_tab();
    }

    private void tab_selection_changed(ChangeEvent e) {
        try {
            if (pnlMain.getSelectedIndex() == TAB_RUNNING_GAME)
                this.scheduler.start();
            else if (!this.scheduler.isInStandbyMode()) this.scheduler.standby();

        } catch (SchedulerException ex) {
            ex.printStackTrace();
        }

        switch (pnlMain.getSelectedIndex()) {
            case TAB_SETUP: {
                update_setup_game_tab();
                break;
            }
            case TAB_RUNNING_GAME: {
                btnLastSSE(null);
                break;
            }
            case TAB_AGENTS: {
                btnRefreshAgents(null);
                break;
            }
        }
    }

    private void btnConnect(ActionEvent e) {
        if (connected) disconnect();
        else connect();
    }

    private void connect() {
        if (connected) return;
        try {
            txtLogger.setText(null);
            int max_number_of_games = get("system/get_max_number_of_games").getInt("max_number_of_games");
            connected = true;
            for (int i = 0; i < max_number_of_games; i++) cmbGameSlots.addItem(i + 1);
            current_state = get("game/status", current_game_id()); // just in case a game is already running
            set_gui_to_situation();
        } catch (JSONException e) {
            log.error(e);
            disconnect();
        }
    }

    private void disconnect() {
        if (!connected) return;
        pnlMain.setSelectedIndex(TAB_ABOUT);
        txtLogger.setText(null);
        addLog("Server not connected...");
        shutdown_sse_client();
        connected = false;
        this.current_state = new JSONObject();
        cmbGameSlots.removeAllItems();
        lblResponse.setIcon(new ImageIcon(getClass().getResource("/artwork/leddarkred.png")));
        lblResponse.setText("not connected");
        set_gui_to_situation();
    }

    /**
     * called from the quartz job
     */
    public void flash_state_led() {
        if (!connected) return;
        if (pnlMain.getSelectedIndex() != TAB_RUNNING_GAME) return;
        final String state = current_state.getString("game_state");
        SwingUtilities.invokeLater(() -> {
            int index = _states_.indexOf(state.toUpperCase());
            // toggle led
            _state_labels.get(index).setEnabled(!_state_labels.get(index).isEnabled());
            btnLastSSE.setText(last_sse_received == null ? "never" : Duration.between(last_sse_received, LocalDateTime.now()).toSeconds() + "s ago");
        });
    }


    private void createUIComponents() {
        tblAgents = new JTable(new TM_Agents(new JSONObject(), configs));
    }


    private void btnSaveFile(ActionEvent e) {
        try {
            GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();
            current_game_mode.save_file();
            lblFile.setText(current_game_mode.getFilename());
        } catch (IOException ex) {
            log.error(ex);
            addLog(ex.getMessage());
        }
    }

    private void btnLoadFile(ActionEvent e) {
        GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();
        current_game_mode.load_file();
        lblFile.setText(current_game_mode.getFilename());
    }

    private void btnFileNew(ActionEvent e) {
        GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();
        current_game_mode.load_defaults();
        lblFile.setText(current_game_mode.getFilename());
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
            txtLogger.append(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)) + ": " + text + "\n");
            scrollLog.getVerticalScrollBar().setValue(scrollLog.getVerticalScrollBar().getMaximum());
        });
    }

    private void txtURIFocusLost(FocusEvent e) {
        configs.put(Configs.REST_URI, txtURI.getText().trim());
    }


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
        current_state = get("game/status", current_game_id());
        set_gui_to_situation();
    }


    private void update_setup_game_tab() {
        GameParams current_game_setup = (GameParams) cmbGameModes.getSelectedItem();
        if (current_game_setup == null) return; // for init phase
        if (current_state.isEmpty()) current_game_setup.load_defaults();
        else current_game_setup.set_parameters(current_state);
        SwingUtilities.invokeLater(() -> {
            pnlGameMode.removeAll();
            pnlGameMode.add(current_game_setup);
//            pnlParams.invalidate();
//            pnlParams.repaint();
        });
    }

    private void update_running_game_tab() {
        String state = current_state.getString("game_state");
        GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();

        int index = _states_.indexOf(state.toUpperCase());
        // States
        for (int i = 0; i < 7; i++) {
            _state_labels.get(i).setEnabled(state.equalsIgnoreCase(_state_labels.get(i).getName()));
        }
        // Messages
        for (int i = 0; i < 8; i++) {
            boolean enabled = Boolean.valueOf(state_buttons_enable_table[index][i] == 1 ? true : false);
            _message_buttons.get(i).setEnabled(enabled);
        }

        log.debug(current_game_mode);
        log.debug(current_state);
        //txtGameStatus.setText(current_game_mode.get_score_as_html(current_state));
        log.debug(current_game_mode.get_score_as_html(current_state));
        scrlGameStatus.getVerticalScrollBar().setValue(0);
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
        lblResponse.setIcon(new ImageIcon(getClass().getResource(icon)));
        lblResponse.setText(response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase());
    }

    void set_response_status(Exception exception) {
        String icon = "/artwork/ledred.png";
        lblResponse.setIcon(new ImageIcon(getClass().getResource(icon)));
        lblResponse.setText(exception.getMessage());
        lblResponse.setToolTipText(exception.toString());
    }

    private void cmbGameSlotsItemStateChanged(ItemEvent e) {
        if (e.getStateChange() != ItemEvent.SELECTED) return;
        btnRefreshServer(null);
    }

    private void btnUnloadOnServer(ActionEvent e) {
        shutdown_sse_client();
        post("game/unload", current_game_id());
        current_state = new JSONObject();
        set_gui_to_situation();
    }

    private void btnSendGameToServer(ActionEvent e) {
        GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();
        String params = current_game_mode.read_parameters().toString(4);
        current_state = post("game/load", params, current_game_id());
        set_gui_to_situation();
        pnlMain.setSelectedIndex(TAB_RUNNING_GAME);
    }

    private void shutdown_sse_client() {
        if (sseClient == null) return;
        sseClient.shutdown();
        sseClient = null;
    }

    private void connect_sse_client() {
        if (sseClient != null) return;
        sseClient = SSEClient.builder()
                .url(txtURI.getText().trim() + "/api/game-sse?id=" + current_game_id())
                .useKeepAliveMechanismIfReceived(true)
                .eventHandler(eventText -> {
                    try {
                        // does not work, when there are newlines in the received messages
                        log.debug("sse_event_received - new state: {}", eventText);
                        last_sse_received = LocalDateTime.now();
                        current_state = get("game/status", current_game_id());
                        set_gui_to_situation();
                    } catch (JSONException jsonException) {
                        log.error(jsonException);
                        //disconnect();
                    }
                })
                .build();
        // todo: retry when failing
        // todo: es scheint als würde die subscription erst laufen, wenn ein event passiert
        sseClient.start();
        last_sse_received = sseClient.isSubscribedSuccessfully() ? LocalDateTime.now() : null;
    }

    private String current_game_id() {
        return cmbGameSlots.getSelectedItem().toString();
    }

    private void thisWindowClosing(WindowEvent e) {
        disconnect();
    }

    private void btnLastSSE(ActionEvent e) {
        if (sseClient == null || !sseClient.isSubscribedSuccessfully()) {
            shutdown_sse_client();
            connect_sse_client();
        }
        current_state = get("game/status", current_game_id());
        update_running_game_tab();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        createUIComponents();

        mainPanel = new JPanel();
        panel1 = new JPanel();
        txtURI = new JTextField();
        btnConnect = new JButton();
        panel2 = new JPanel();
        cmbGameSlots = new JComboBox();
        lblResponse = new JLabel();
        pnlMain = new JTabbedPane();
        pnlParams = new JPanel();
        pnlGameMode = new JPanel();
        pnlFiles = new JPanel();
        cmbGameModes = new JComboBox();
        btnFileNew = new JButton();
        btnLoadFile = new JButton();
        btnSaveFile = new JButton();
        btnSendGameToServer = new JButton();
        btnUnloadOnServer = new JButton();
        hSpacer1 = new JPanel(null);
        lblFile = new JLabel();
        hSpacer2 = new JPanel(null);
        pnlRunningGame = new JPanel();
        pnlMessages = new JPanel();
        btnPrepare = new JButton();
        btnReset = new JButton();
        btnReady = new JButton();
        btnRun = new JButton();
        btnPause = new JButton();
        btnResume = new JButton();
        btnContinue = new JButton();
        btnGameOver = new JButton();
        pnlGameStates = new JPanel();
        lblProlog = new JLabel();
        lblTeamsNotReady = new JLabel();
        lblTeamsReady = new JLabel();
        lblRunning = new JLabel();
        lblPausing = new JLabel();
        lblResuming = new JLabel();
        lblEpilog = new JLabel();
        btnLastSSE = new JButton();
        scrlGameStatus = new JScrollPane();
        txtGameStatus = new JTextPane();
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
        btnRefreshAgents = new JButton();
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
        pnlAbout = new JPanel();
        scrollPane2 = new JScrollPane();
        txtAbout = new JTextPane();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thisWindowClosing(e);
            }
        });
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "$ugap, default:grow, $ugap",
                "$rgap, default:grow, $ugap"));

        //======== mainPanel ========
        {
            mainPanel.setLayout(new FormLayout(
                    "default:grow",
                    "35dlu, $rgap, default, $lgap, default, $rgap, default, $lgap, fill:default:grow"));

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
                btnConnect.setIcon(new ImageIcon(getClass().getResource("/artwork/disconnected.png")));
                btnConnect.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                btnConnect.addActionListener(e -> btnConnect(e));
                panel1.add(btnConnect);
            }
            mainPanel.add(panel1, CC.xy(1, 1));

            //======== panel2 ========
            {
                panel2.setLayout(new HorizontalLayout());

                //---- cmbGameSlots ----
                cmbGameSlots.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                cmbGameSlots.addItemListener(e -> cmbGameSlotsItemStateChanged(e));
                panel2.add(cmbGameSlots);

                //---- lblResponse ----
                lblResponse.setText("not connected");
                lblResponse.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                lblResponse.setIcon(new ImageIcon(getClass().getResource("/artwork/leddarkred.png")));
                panel2.add(lblResponse);
            }
            mainPanel.add(panel2, CC.xy(1, 3));

            //======== pnlMain ========
            {
                pnlMain.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                pnlMain.addChangeListener(e -> tab_selection_changed(e));

                //======== pnlParams ========
                {
                    pnlParams.setLayout(new FormLayout(
                            "default:grow",
                            "fill:default:grow, $lgap, default"));

                    //======== pnlGameMode ========
                    {
                        pnlGameMode.setLayout(new BoxLayout(pnlGameMode, BoxLayout.PAGE_AXIS));
                    }
                    pnlParams.add(pnlGameMode, CC.xy(1, 1));

                    //======== pnlFiles ========
                    {
                        pnlFiles.setLayout(new BoxLayout(pnlFiles, BoxLayout.X_AXIS));

                        //---- cmbGameModes ----
                        cmbGameModes.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        pnlFiles.add(cmbGameModes);

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

                        //---- btnSendGameToServer ----
                        btnSendGameToServer.setText(null);
                        btnSendGameToServer.setIcon(new ImageIcon(getClass().getResource("/artwork/upload.png")));
                        btnSendGameToServer.setToolTipText("Load game on the server");
                        btnSendGameToServer.setMinimumSize(new Dimension(38, 38));
                        btnSendGameToServer.setPreferredSize(new Dimension(38, 38));
                        btnSendGameToServer.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        btnSendGameToServer.addActionListener(e -> btnSendGameToServer(e));
                        pnlFiles.add(btnSendGameToServer);

                        //---- btnUnloadOnServer ----
                        btnUnloadOnServer.setText(null);
                        btnUnloadOnServer.setIcon(new ImageIcon(getClass().getResource("/artwork/unload.png")));
                        btnUnloadOnServer.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnUnloadOnServer.setToolTipText("Remove the loaded game from the commander's memory.");
                        btnUnloadOnServer.setHorizontalAlignment(SwingConstants.LEFT);
                        btnUnloadOnServer.setPreferredSize(new Dimension(38, 38));
                        btnUnloadOnServer.addActionListener(e -> btnUnloadOnServer(e));
                        pnlFiles.add(btnUnloadOnServer);
                        pnlFiles.add(hSpacer1);

                        //---- lblFile ----
                        lblFile.setText("no file");
                        lblFile.setFont(new Font(".SF NS Text", Font.PLAIN, 16));
                        pnlFiles.add(lblFile);
                        pnlFiles.add(hSpacer2);
                    }
                    pnlParams.add(pnlFiles, CC.xy(1, 3));
                }
                pnlMain.addTab("Setup Game", pnlParams);

                //======== pnlRunningGame ========
                {
                    pnlRunningGame.setLayout(new FormLayout(
                            "default:grow",
                            "default, $lgap, default, $rgap, default:grow"));

                    //======== pnlMessages ========
                    {
                        pnlMessages.setLayout(new HorizontalLayout(5));

                        //---- btnPrepare ----
                        btnPrepare.setText("Prepare");
                        btnPrepare.setToolTipText("Start loaded game");
                        btnPrepare.setIcon(null);
                        btnPrepare.setPreferredSize(null);
                        btnPrepare.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        btnPrepare.setActionCommand("prepare");
                        pnlMessages.add(btnPrepare);

                        //---- btnReset ----
                        btnReset.setText("Reset");
                        btnReset.setToolTipText("Start loaded game");
                        btnReset.setIcon(null);
                        btnReset.setPreferredSize(null);
                        btnReset.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        btnReset.setActionCommand("reset");
                        pnlMessages.add(btnReset);

                        //---- btnReady ----
                        btnReady.setText("Ready");
                        btnReady.setToolTipText("Start loaded game");
                        btnReady.setIcon(null);
                        btnReady.setPreferredSize(null);
                        btnReady.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        btnReady.setActionCommand("ready");
                        pnlMessages.add(btnReady);

                        //---- btnRun ----
                        btnRun.setText("Run");
                        btnRun.setIcon(null);
                        btnRun.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnRun.setToolTipText("Pause the running game");
                        btnRun.setActionCommand("run");
                        pnlMessages.add(btnRun);

                        //---- btnPause ----
                        btnPause.setText("Pause");
                        btnPause.setIcon(null);
                        btnPause.setToolTipText("Reset the game to the state as if it was just loaded.");
                        btnPause.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnPause.setActionCommand("pause");
                        pnlMessages.add(btnPause);

                        //---- btnResume ----
                        btnResume.setText("Resume");
                        btnResume.setIcon(null);
                        btnResume.setToolTipText("Reset the game to the state as if it was just loaded.");
                        btnResume.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnResume.setActionCommand("resume");
                        pnlMessages.add(btnResume);

                        //---- btnContinue ----
                        btnContinue.setText("Continue");
                        btnContinue.setIcon(null);
                        btnContinue.setToolTipText("Reset the game to the state as if it was just loaded.");
                        btnContinue.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnContinue.setActionCommand("continue");
                        pnlMessages.add(btnContinue);

                        //---- btnGameOver ----
                        btnGameOver.setText("Game Over");
                        btnGameOver.setIcon(null);
                        btnGameOver.setToolTipText("Reset the game to the state as if it was just loaded.");
                        btnGameOver.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnGameOver.setActionCommand("game_over");
                        pnlMessages.add(btnGameOver);
                    }
                    pnlRunningGame.add(pnlMessages, CC.xy(1, 1, CC.FILL, CC.DEFAULT));

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

                        //---- btnLastSSE ----
                        btnLastSSE.setText(null);
                        btnLastSSE.setIcon(new ImageIcon(getClass().getResource("/artwork/irkickflash.png")));
                        btnLastSSE.setToolTipText("Last Message received");
                        btnLastSSE.addActionListener(e -> btnLastSSE(e));
                        pnlGameStates.add(btnLastSSE);
                    }
                    pnlRunningGame.add(pnlGameStates, CC.xy(1, 3, CC.LEFT, CC.DEFAULT));

                    //======== scrlGameStatus ========
                    {

                        //---- txtGameStatus ----
                        txtGameStatus.setContentType("text/html");
                        scrlGameStatus.setViewportView(txtGameStatus);
                    }
                    pnlRunningGame.add(scrlGameStatus, CC.xy(1, 5, CC.FILL, CC.FILL));
                }
                pnlMain.addTab("Running Game", pnlRunningGame);

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
                                "fill:default, fill:default:grow, 9*(fill:default), 2*(default)"));

                        //---- btnRefreshAgents ----
                        btnRefreshAgents.setText("Update");
                        btnRefreshAgents.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnRefreshAgents.setIcon(new ImageIcon(getClass().getResource("/artwork/reload-on.png")));
                        btnRefreshAgents.addActionListener(e -> btnRefreshAgents(e));
                        pnlTesting.add(btnRefreshAgents, CC.xy(1, 1, CC.FILL, CC.DEFAULT));

                        //---- button1 ----
                        button1.setText("White");
                        button1.setActionCommand("led_wht");
                        button1.setIcon(new ImageIcon(getClass().getResource("/artwork/led-white-on.png")));
                        button1.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button1.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button1, CC.xy(1, 3, CC.FILL, CC.DEFAULT));

                        //---- button2 ----
                        button2.setText("Red");
                        button2.setActionCommand("led_red");
                        button2.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
                        button2.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button2.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button2, CC.xy(1, 4, CC.FILL, CC.DEFAULT));

                        //---- button3 ----
                        button3.setText("Yellow");
                        button3.setActionCommand("led_ylw");
                        button3.setIcon(new ImageIcon(getClass().getResource("/artwork/ledyellow.png")));
                        button3.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button3.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button3, CC.xy(1, 5, CC.FILL, CC.DEFAULT));

                        //---- button4 ----
                        button4.setText("Green");
                        button4.setActionCommand("led_grn");
                        button4.setIcon(new ImageIcon(getClass().getResource("/artwork/ledgreen.png")));
                        button4.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button4.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button4, CC.xy(1, 6, CC.FILL, CC.DEFAULT));

                        //---- button5 ----
                        button5.setText("Blue");
                        button5.setActionCommand("led_blu");
                        button5.setIcon(new ImageIcon(getClass().getResource("/artwork/ledblue.png")));
                        button5.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button5.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button5, CC.xy(1, 7, CC.FILL, CC.DEFAULT));

                        //---- button6 ----
                        button6.setText("Buzzer");
                        button6.setActionCommand("buzzer");
                        button6.setIcon(new ImageIcon(getClass().getResource("/artwork/buzzer.png")));
                        button6.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button6.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button6, CC.xy(1, 8, CC.FILL, CC.DEFAULT));

                        //---- button7 ----
                        button7.setText("Siren 1");
                        button7.setActionCommand("sir1");
                        button7.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button7.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button7.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button7, CC.xy(1, 9, CC.FILL, CC.DEFAULT));

                        //---- button8 ----
                        button8.setText("Siren 2");
                        button8.setActionCommand("sir2");
                        button8.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button8.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button8.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button8, CC.xy(1, 10, CC.FILL, CC.DEFAULT));

                        //---- button9 ----
                        button9.setText("Siren 3");
                        button9.setActionCommand("sir3");
                        button9.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button9.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button9.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button9, CC.xy(1, 11, CC.FILL, CC.DEFAULT));

                        //---- button10 ----
                        button10.setText("Siren 4");
                        button10.setActionCommand("sir4");
                        button10.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button10.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button10.setHorizontalAlignment(SwingConstants.LEFT);
                        pnlTesting.add(button10, CC.xy(1, 12, CC.FILL, CC.DEFAULT));
                    }
                    pnlAgents.add(pnlTesting, CC.xy(3, 1));
                }
                pnlMain.addTab("Agents", pnlAgents);

                //======== pnlAbout ========
                {
                    pnlAbout.setLayout(new BoxLayout(pnlAbout, BoxLayout.PAGE_AXIS));

                    //======== scrollPane2 ========
                    {

                        //---- txtAbout ----
                        txtAbout.setContentType("text/html");
                        scrollPane2.setViewportView(txtAbout);
                    }
                    pnlAbout.add(scrollPane2);
                }
                pnlMain.addTab("About", pnlAbout);
            }
            mainPanel.add(pnlMain, CC.xywh(1, 5, 1, 5));
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
    private JPanel panel2;
    private JComboBox cmbGameSlots;
    private JLabel lblResponse;
    private JTabbedPane pnlMain;
    private JPanel pnlParams;
    private JPanel pnlGameMode;
    private JPanel pnlFiles;
    private JComboBox cmbGameModes;
    private JButton btnFileNew;
    private JButton btnLoadFile;
    private JButton btnSaveFile;
    private JButton btnSendGameToServer;
    private JButton btnUnloadOnServer;
    private JPanel hSpacer1;
    private JLabel lblFile;
    private JPanel hSpacer2;
    private JPanel pnlRunningGame;
    private JPanel pnlMessages;
    private JButton btnPrepare;
    private JButton btnReset;
    private JButton btnReady;
    private JButton btnRun;
    private JButton btnPause;
    private JButton btnResume;
    private JButton btnContinue;
    private JButton btnGameOver;
    private JPanel pnlGameStates;
    private JLabel lblProlog;
    private JLabel lblTeamsNotReady;
    private JLabel lblTeamsReady;
    private JLabel lblRunning;
    private JLabel lblPausing;
    private JLabel lblResuming;
    private JLabel lblEpilog;
    private JButton btnLastSSE;
    private JScrollPane scrlGameStatus;
    private JTextPane txtGameStatus;
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
    private JButton btnRefreshAgents;
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
    private JPanel pnlAbout;
    private JScrollPane scrollPane2;
    private JTextPane txtAbout;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
