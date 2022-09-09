/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc.ui;

import com.google.common.io.Resources;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import de.flashheart.rlgrc.jobs.FlashStateLedJob;
import de.flashheart.rlgrc.misc.JSONConfigs;
import de.flashheart.rlgrc.networking.SSEClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.*;
import org.jdesktop.swingx.HorizontalLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
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
import java.util.stream.Collectors;

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
    public static final Font MY_FONT = new Font(".SF NS Text", Font.PLAIN, 18);


    private JSONObject current_state;
    private final JSONConfigs configs;

    private Client client = ClientBuilder.newClient();
    private final int MAX_LOG_LINES = 400;
    private ArrayList<JToggleButton> GAME_SELECT_BUTTONS;
    private boolean connected;
    private Optional<String> selected_agent;
    private SSEClient sseClient;
    private LocalDateTime last_sse_received;
    private final HashMap<String, GameParams> game_modes;
    private boolean summary_written_on_epilog = false;

    public FrameMain(JSONConfigs configs) throws SchedulerException, IOException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        this.scheduler.getContext().put("rlgrc", this);
        this.configs = configs;
        this.connected = false;
        this.selected_agent = Optional.empty();
        this.GAME_SELECT_BUTTONS = new ArrayList<>();
        this.current_state = new JSONObject();
        this.state_flashing_job = new JobKey(FlashStateLedJob.name, "group1");
        game_modes = new HashMap<>();
        game_modes.put("conquest", new ConquestParams(configs));
        game_modes.put("farcry", new FarcryParams(configs));
        game_modes.put("centerflags", new CenterFlagsParams(configs, this));

        initComponents();
        txtAbout.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        txtAbout.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        if (Desktop.isDesktopSupported()) {
            txtAbout.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        log.warn(ex);
                    }
                }
            });
        }
        setTitle("rlgrc v" + configs.getBuildProperties().getProperty("my.version") + " b" + configs.getBuildProperties().getProperty("buildNumber") + " " + configs.getBuildProperties().getProperty("buildDate"));
        //guiFSM = new FSM(this.getClass().getClassLoader().getResourceAsStream("fsm/gui.xml"), null);

        pnlMain.setSelectedIndex(TAB_ABOUT);
        txtAbout.setText(Resources.toString(Resources.getResource("about.html"), Charset.defaultCharset()));

        this._states_ = Arrays.asList(_state_PROLOG, _state_TEAMS_NOT_READY, _state_TEAMS_READY, _state_RUNNING, _state_PAUSING, _state_RESUMING, _state_EPILOG);
        this._message_buttons = Arrays.asList(btnPrepare, btnReset, btnReady, btnRun, btnPause, btnResume, btnContinue, btnGameOver);
        this._state_labels = Arrays.asList(lblProlog, lblTeamsNotReady, lblTeamsReady, lblRunning, lblPausing, lblResuming, lblEpilog);

        initFrame();


    }

    void add_to_recent_uris_list(String uri) {
        //ArrayList<String> list = Lists.newArrayList(StringUtils.split(configs.get(Configs.REST_URIS), ","));
        List<String> list = configs.getConfigs().getJSONArray("recent").toList().stream().map(Object::toString).collect(Collectors.toList());
        if (list.contains(uri)) list.remove(uri);
        // put last uri at the head of the list
        // nice trick btw: https://stackoverflow.com/a/28631202
        Collections.reverse(list);
        list.add(uri);
        Collections.reverse(list);
        //configs.getConfigs().remove("recent");
        configs.getConfigs().put("recent", new JSONArray(list));
        configs.saveConfigs();

        //configs.put(Configs.REST_URIS, StringUtils.join(list, ","));
    }

    private void initFrame() throws IOException, SchedulerException {
        initLogger();

        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "conquest"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "farcry"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "rush"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "centerflags"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "results"));
        txtURI.setModel(new DefaultComboBoxModel(configs.getConfigs().getJSONArray("recent").toList().toArray()));
        txtURI.getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                add_to_recent_uris_list(txtURI.getSelectedItem().toString().trim());
            }
        });
        txtURI.addItemListener(e -> add_to_recent_uris_list(txtURI.getSelectedItem().toString().trim()));
        //((JTextField) txtURI.getEditor().getEditorComponent()).setText(configs.get(Configs.REST_URI));

        tblAgents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblAgents.getSelectionModel().addListSelectionListener(e -> table_of_agents_changed_selection(e));
        //pnlGames.add("Conquest", new ConquestParams());

        game_modes.forEach((s, gameParams) -> cmbGameModes.addItem(gameParams));
        cmbGameSlots.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            String text = "--";
            if (value != null) {
                text = "#" + value;
            }
            return new DefaultListCellRenderer().getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        });
        cmbGameModes.setRenderer((list, value, index, isSelected, cellHasFocus) -> new DefaultListCellRenderer().getListCellRendererComponent(list, ((GameParams) value).getMode(), index, isSelected, cellHasFocus));

        for (JButton testButton : Arrays.asList(button1, button2, button2, button3, button4, button5, button6, button7, button8, button9, button10, button11, button12)) {
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
        btnConnect.setIcon(new ImageIcon(getClass().getResource(connected ? "/artwork/rlgrc-48-connected.png" : "/artwork/rlgrc-48-disconnected.png")));
        if (!connected) {
            pnlMain.setSelectedIndex(TAB_ABOUT);
            return;
        }

        pnlMain.setEnabledAt(TAB_SETUP, !is_game_loaded_on_server() || current_state.getString("game_state").equals(_state_PROLOG));
        pnlMain.setEnabledAt(TAB_RUNNING_GAME, is_game_loaded_on_server());
        // todo: nach neustart, wenn ein spiel schon läuft, dann glaubt er alles ist conquest auch wenn farcry läuft.
        // hier muss der richtige game mode gesetzt werden. de.flashheart.rlgrc.ui.FrameMain org.json.JSONException: JSONObject["cps_held_by_red"] not found.
        // und zwar erstmal nur für game:1
        if (pnlMain.getSelectedIndex() == TAB_SETUP) {
            if (is_game_loaded_on_server()) {
                cmbGameModes.setSelectedItem(game_modes.get(current_state.getString("mode")));
            } else cmbGameModes.setSelectedItem(game_modes.get("conquest"));
        } else {
            update_running_game_tab();
        }

        cmbGameModes.setEnabled(!is_game_loaded_on_server());
    }

    private boolean is_game_loaded_on_server() {
        return current_state.keySet().contains("game_state");
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
                update_setup_game_tab(Optional.ofNullable(game_modes.get(current_state.optString("mode"))));
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
        current_game_mode.from_params_to_ui();
        lblFile.setText(current_game_mode.getFilename());
    }

    private void btnFileNew(ActionEvent e) {
        GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();
        current_game_mode.load_defaults();
        current_game_mode.from_params_to_ui();
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
        log.trace(text);
        SwingUtilities.invokeLater(() -> {
            txtLogger.append(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)) + ": " + text + "\n");
            scrollLog.getVerticalScrollBar().setValue(scrollLog.getVerticalScrollBar().getMaximum());
        });
    }

    private void txtURIFocusLost(FocusEvent e) {
        //configs.put(Configs.REST_URI, txtURI.getSelectedItem().toString().trim());
        add_to_recent_uris_list(txtURI.getSelectedItem().toString().trim());
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

    private void update_setup_game_tab(Optional<GameParams> current_game_setup) {
        current_game_setup.ifPresent(gameParams -> {
            if (!is_game_loaded_on_server()) {
                gameParams.load_defaults();
                gameParams.from_params_to_ui();
            } else gameParams.from_params_to_ui(current_state);
            SwingUtilities.invokeLater(() -> {
                pnlGameMode.removeAll();
                pnlGameMode.add(gameParams);
                this.invalidate();
                this.repaint();
            });
            btnZeus.setEnabled(gameParams.get_zeus().isPresent());
        });
    }

    private void update_running_game_tab() {
        if (!is_game_loaded_on_server()) return;
        String state = current_state.getString("game_state");
        String mode = current_state.getString("mode");
        final String html = game_modes.get(mode).get_score_as_html(current_state);
        // if we do not run this in a different thread, the scrollpane wont go up
        // don't know why. but did cost me some time to find out.
        new Thread(() -> txtGameStatus.setText(html)).start();

        int index = _states_.indexOf(state.toUpperCase());
        // States
        for (int i = 0; i < 7; i++) {
            _state_labels.get(i).setEnabled(state.equalsIgnoreCase(_state_labels.get(i).getName()));
        }
        // Messages
        for (int i = 0; i < 8; i++) {
            boolean enabled = !btnLock.isSelected() && Boolean.valueOf(state_buttons_enable_table[index][i] == 1 ? true : false);
            _message_buttons.get(i).setEnabled(enabled);
        }

        // at the end of a match, we save the results for later use
        if (state.equals(_state_PROLOG)) summary_written_on_epilog = false;
        if (state.equals(_state_EPILOG) && !summary_written_on_epilog) {
            try {
                summary_written_on_epilog = true;
                String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                FileUtils.writeStringToFile(new File(System.getProperty("workspace") + File.separator + "results" + File.separator + mode + "(at)" + filename + ".html"), html, Charset.defaultCharset());
            } catch (IOException e) {
                log.warn(e);
            }
        }


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
                    .target(txtURI.getSelectedItem().toString().trim() + "/api/" + uri);

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
                    .target(txtURI.getSelectedItem().toString().trim() + "/api/" + uri)
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
                    .target(txtURI.getSelectedItem().toString().trim() + "/api/" + uri)
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
        lblResponse.setText(StringUtils.left(exception.getMessage(), 70));
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
        String params = current_game_mode.from_ui_to_params().toString(4);
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
                .url(txtURI.getSelectedItem().toString().trim() + "/api/game-sse?id=" + current_game_id())
                .useKeepAliveMechanismIfReceived(true)
                .eventHandler(eventText -> {
                    try {
                        // does not work, when there are newlines in the received messages
                        log.trace("sse_event_received - new state: {}", eventText);
                        last_sse_received = LocalDateTime.now();
                        current_state = get("game/status", current_game_id());
                        set_gui_to_situation();
                    } catch (JSONException jsonException) {
                        log.error(jsonException);
                        //disconnect();
                    }
                })
                .build();
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
        } else {
            current_state = get("game/status", current_game_id());
            update_running_game_tab();
        }

    }

    private void btnLockItemStateChanged(ItemEvent e) {
        update_running_game_tab();
    }

    private void cmbGameModesItemStateChanged(ItemEvent e) {
        update_setup_game_tab(Optional.of((GameParams) e.getItem()));
    }

    private void btnPowerSaveOn(ActionEvent e) {
        post("system/powersave_agents", "", new Properties());
    }

    private void btnPowerSaveOff(ActionEvent e) {
        post("system/welcome_agents", "", new Properties());
    }

    private void btnZeus(ActionEvent e) {
        ((GameParams) cmbGameModes.getSelectedItem()).get_zeus().ifPresent(zeusDialog -> {
            zeusDialog.add_property_change_listener(evt -> {
                post("game/zeus", evt.getNewValue().toString(), current_game_id());
            });
            zeusDialog.setVisible(true);
        });
    }

    private void btnTestJSON(ActionEvent e) {
        GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();
        String params = current_game_mode.from_ui_to_params().toString(4);
        log.debug(params);
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        createUIComponents();

        mainPanel = new JPanel();
        panel1 = new JPanel();
        txtURI = new JComboBox();
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
        btnTestJSON = new JButton();
        pnlRunningGame = new JPanel();
        pnlMessages = new JPanel();
        btnLock = new JToggleButton();
        btnZeus = new JButton();
        btnPrepare = new JButton();
        btnReset = new JButton();
        btnReady = new JButton();
        btnRun = new JButton();
        btnPause = new JButton();
        btnResume = new JButton();
        btnContinue = new JButton();
        btnGameOver = new JButton();
        pnlGameStates = new JPanel();
        btnLastSSE = new JButton();
        lblProlog = new JLabel();
        lblTeamsNotReady = new JLabel();
        lblTeamsReady = new JLabel();
        lblRunning = new JLabel();
        lblPausing = new JLabel();
        lblResuming = new JLabel();
        lblEpilog = new JLabel();
        panel3 = new JPanel();
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
        separator1 = new JSeparator();
        vSpacer1 = new JPanel(null);
        btnPowerSaveOn = new JButton();
        btnPowerSaveOff = new JButton();
        vSpacer2 = new JPanel(null);
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
        button11 = new JButton();
        button12 = new JButton();
        pnlAbout = new JPanel();
        scrollPane2 = new JScrollPane();
        txtAbout = new JXEditorPane();

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
                    "pref, $rgap, default, $lgap, default, $rgap, default, $lgap, fill:default:grow"));

            //======== panel1 ========
            {
                panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));

                //---- txtURI ----
                txtURI.setFont(new Font(".SF NS Text", Font.PLAIN, 20));
                txtURI.setEditable(true);
                panel1.add(txtURI);

                //---- btnConnect ----
                btnConnect.setText(null);
                btnConnect.setIcon(new ImageIcon(getClass().getResource("/artwork/rlgrc-48-disconnected.png")));
                btnConnect.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                btnConnect.setPreferredSize(new Dimension(50, 50));
                btnConnect.setBorderPainted(false);
                btnConnect.setContentAreaFilled(false);
                btnConnect.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/rlgrc-48-gear.png")));
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
                        cmbGameModes.addItemListener(e -> cmbGameModesItemStateChanged(e));
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

                        //---- btnTestJSON ----
                        btnTestJSON.setText(null);
                        btnTestJSON.setIcon(new ImageIcon(getClass().getResource("/artwork/debug28.png")));
                        btnTestJSON.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnTestJSON.setToolTipText("Remove the loaded game from the commander's memory.");
                        btnTestJSON.setHorizontalAlignment(SwingConstants.LEFT);
                        btnTestJSON.setPreferredSize(new Dimension(38, 38));
                        btnTestJSON.addActionListener(e -> btnTestJSON(e));
                        pnlFiles.add(btnTestJSON);
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

                        //---- btnLock ----
                        btnLock.setText(null);
                        btnLock.setIcon(new ImageIcon(getClass().getResource("/artwork/decrypted.png")));
                        btnLock.setSelectedIcon(new ImageIcon(getClass().getResource("/artwork/encrypted.png")));
                        btnLock.setContentAreaFilled(false);
                        btnLock.setBorderPainted(false);
                        btnLock.setPreferredSize(new Dimension(38, 38));
                        btnLock.setToolTipText("Lock");
                        btnLock.addItemListener(e -> btnLockItemStateChanged(e));
                        pnlMessages.add(btnLock);

                        //---- btnZeus ----
                        btnZeus.setText(null);
                        btnZeus.setIcon(new ImageIcon(getClass().getResource("/artwork/zeus-28.png")));
                        btnZeus.setSelectedIcon(null);
                        btnZeus.setContentAreaFilled(false);
                        btnZeus.setBorderPainted(false);
                        btnZeus.setPreferredSize(new Dimension(38, 38));
                        btnZeus.setToolTipText("Zeus");
                        btnZeus.addActionListener(e -> btnZeus(e));
                        pnlMessages.add(btnZeus);

                        //---- btnPrepare ----
                        btnPrepare.setText("Prepare");
                        btnPrepare.setToolTipText("Prepare");
                        btnPrepare.setIcon(null);
                        btnPrepare.setPreferredSize(null);
                        btnPrepare.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        btnPrepare.setActionCommand("prepare");
                        pnlMessages.add(btnPrepare);

                        //---- btnReset ----
                        btnReset.setText("Reset");
                        btnReset.setToolTipText(null);
                        btnReset.setIcon(null);
                        btnReset.setPreferredSize(null);
                        btnReset.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        btnReset.setActionCommand("reset");
                        pnlMessages.add(btnReset);

                        //---- btnReady ----
                        btnReady.setText("Ready");
                        btnReady.setToolTipText(null);
                        btnReady.setIcon(null);
                        btnReady.setPreferredSize(null);
                        btnReady.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        btnReady.setActionCommand("ready");
                        pnlMessages.add(btnReady);

                        //---- btnRun ----
                        btnRun.setText("Run");
                        btnRun.setIcon(null);
                        btnRun.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnRun.setToolTipText(null);
                        btnRun.setActionCommand("run");
                        pnlMessages.add(btnRun);

                        //---- btnPause ----
                        btnPause.setText("Pause");
                        btnPause.setIcon(null);
                        btnPause.setToolTipText(null);
                        btnPause.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnPause.setActionCommand("pause");
                        pnlMessages.add(btnPause);

                        //---- btnResume ----
                        btnResume.setText("Resume");
                        btnResume.setIcon(null);
                        btnResume.setToolTipText(null);
                        btnResume.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnResume.setActionCommand("resume");
                        pnlMessages.add(btnResume);

                        //---- btnContinue ----
                        btnContinue.setText("Continue");
                        btnContinue.setIcon(null);
                        btnContinue.setToolTipText(null);
                        btnContinue.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnContinue.setActionCommand("continue");
                        pnlMessages.add(btnContinue);

                        //---- btnGameOver ----
                        btnGameOver.setText("Game Over");
                        btnGameOver.setIcon(null);
                        btnGameOver.setToolTipText(null);
                        btnGameOver.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnGameOver.setActionCommand("game_over");
                        pnlMessages.add(btnGameOver);
                    }
                    pnlRunningGame.add(pnlMessages, CC.xy(1, 1, CC.FILL, CC.DEFAULT));

                    //======== pnlGameStates ========
                    {
                        pnlGameStates.setLayout(new HorizontalLayout(10));

                        //---- btnLastSSE ----
                        btnLastSSE.setText(null);
                        btnLastSSE.setIcon(new ImageIcon(getClass().getResource("/artwork/irkickflash.png")));
                        btnLastSSE.setToolTipText("Last Message received");
                        btnLastSSE.addActionListener(e -> btnLastSSE(e));
                        pnlGameStates.add(btnLastSSE);

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
                    pnlRunningGame.add(pnlGameStates, CC.xy(1, 3, CC.LEFT, CC.DEFAULT));

                    //======== panel3 ========
                    {
                        panel3.setLayout(new BoxLayout(panel3, BoxLayout.PAGE_AXIS));

                        //======== scrlGameStatus ========
                        {

                            //---- txtGameStatus ----
                            txtGameStatus.setContentType("text/html");
                            txtGameStatus.setEditable(false);
                            scrlGameStatus.setViewportView(txtGameStatus);
                        }
                        panel3.add(scrlGameStatus);
                    }
                    pnlRunningGame.add(panel3, CC.xy(1, 5, CC.DEFAULT, CC.FILL));
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
                        pnlTesting.setLayout(new BoxLayout(pnlTesting, BoxLayout.PAGE_AXIS));

                        //---- btnRefreshAgents ----
                        btnRefreshAgents.setText("Update");
                        btnRefreshAgents.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnRefreshAgents.setIcon(new ImageIcon(getClass().getResource("/artwork/reload-on.png")));
                        btnRefreshAgents.setMaximumSize(new Dimension(32767, 34));
                        btnRefreshAgents.addActionListener(e -> btnRefreshAgents(e));
                        pnlTesting.add(btnRefreshAgents);

                        //---- separator1 ----
                        separator1.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(separator1);
                        pnlTesting.add(vSpacer1);

                        //---- btnPowerSaveOn ----
                        btnPowerSaveOn.setText("Sleep");
                        btnPowerSaveOn.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnPowerSaveOn.setIcon(new ImageIcon(getClass().getResource("/artwork/bluemoon.png")));
                        btnPowerSaveOn.setToolTipText("send idle agents to powersave mode");
                        btnPowerSaveOn.setMaximumSize(new Dimension(32767, 34));
                        btnPowerSaveOn.addActionListener(e -> btnPowerSaveOn(e));
                        pnlTesting.add(btnPowerSaveOn);

                        //---- btnPowerSaveOff ----
                        btnPowerSaveOff.setText("Wake up");
                        btnPowerSaveOff.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
                        btnPowerSaveOff.setIcon(new ImageIcon(getClass().getResource("/artwork/sun-3-28.png")));
                        btnPowerSaveOff.setToolTipText("welcome back to idle agents");
                        btnPowerSaveOff.setMaximumSize(new Dimension(32767, 34));
                        btnPowerSaveOff.addActionListener(e -> btnPowerSaveOff(e));
                        pnlTesting.add(btnPowerSaveOff);
                        pnlTesting.add(vSpacer2);

                        //---- button1 ----
                        button1.setText("White");
                        button1.setActionCommand("wht");
                        button1.setIcon(new ImageIcon(getClass().getResource("/artwork/led-white-on.png")));
                        button1.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button1.setHorizontalAlignment(SwingConstants.LEFT);
                        button1.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button1);

                        //---- button2 ----
                        button2.setText("Red");
                        button2.setActionCommand("red");
                        button2.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
                        button2.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button2.setHorizontalAlignment(SwingConstants.LEFT);
                        button2.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button2);

                        //---- button3 ----
                        button3.setText("Yellow");
                        button3.setActionCommand("ylw");
                        button3.setIcon(new ImageIcon(getClass().getResource("/artwork/ledyellow.png")));
                        button3.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button3.setHorizontalAlignment(SwingConstants.LEFT);
                        button3.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button3);

                        //---- button4 ----
                        button4.setText("Green");
                        button4.setActionCommand("grn");
                        button4.setIcon(new ImageIcon(getClass().getResource("/artwork/ledgreen.png")));
                        button4.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button4.setHorizontalAlignment(SwingConstants.LEFT);
                        button4.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button4);

                        //---- button5 ----
                        button5.setText("Blue");
                        button5.setActionCommand("blu");
                        button5.setIcon(new ImageIcon(getClass().getResource("/artwork/ledblue.png")));
                        button5.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button5.setHorizontalAlignment(SwingConstants.LEFT);
                        button5.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button5);

                        //---- button6 ----
                        button6.setText("Buzzer");
                        button6.setActionCommand("buzzer");
                        button6.setIcon(new ImageIcon(getClass().getResource("/artwork/buzzer.png")));
                        button6.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button6.setHorizontalAlignment(SwingConstants.LEFT);
                        button6.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button6);

                        //---- button7 ----
                        button7.setText("Siren 1");
                        button7.setActionCommand("sir1");
                        button7.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button7.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button7.setHorizontalAlignment(SwingConstants.LEFT);
                        button7.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button7);

                        //---- button8 ----
                        button8.setText("Siren 2");
                        button8.setActionCommand("sir2");
                        button8.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button8.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button8.setHorizontalAlignment(SwingConstants.LEFT);
                        button8.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button8);

                        //---- button9 ----
                        button9.setText("Siren 3");
                        button9.setActionCommand("sir3");
                        button9.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button9.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button9.setHorizontalAlignment(SwingConstants.LEFT);
                        button9.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button9);

                        //---- button10 ----
                        button10.setText("Siren 4");
                        button10.setActionCommand("sir4");
                        button10.setIcon(new ImageIcon(getClass().getResource("/artwork/siren.png")));
                        button10.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button10.setHorizontalAlignment(SwingConstants.LEFT);
                        button10.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button10);

                        //---- button11 ----
                        button11.setText("Play");
                        button11.setActionCommand("play");
                        button11.setIcon(new ImageIcon(getClass().getResource("/artwork/player_play.png")));
                        button11.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button11.setHorizontalAlignment(SwingConstants.LEFT);
                        button11.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button11);

                        //---- button12 ----
                        button12.setText("Stop");
                        button12.setActionCommand("stop");
                        button12.setIcon(new ImageIcon(getClass().getResource("/artwork/player_stop.png")));
                        button12.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
                        button12.setHorizontalAlignment(SwingConstants.LEFT);
                        button12.setMaximumSize(new Dimension(32767, 34));
                        pnlTesting.add(button12);
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
                        txtAbout.setEditable(false);
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
    private JComboBox txtURI;
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
    private JButton btnTestJSON;
    private JPanel pnlRunningGame;
    private JPanel pnlMessages;
    private JToggleButton btnLock;
    private JButton btnZeus;
    private JButton btnPrepare;
    private JButton btnReset;
    private JButton btnReady;
    private JButton btnRun;
    private JButton btnPause;
    private JButton btnResume;
    private JButton btnContinue;
    private JButton btnGameOver;
    private JPanel pnlGameStates;
    private JButton btnLastSSE;
    private JLabel lblProlog;
    private JLabel lblTeamsNotReady;
    private JLabel lblTeamsReady;
    private JLabel lblRunning;
    private JLabel lblPausing;
    private JLabel lblResuming;
    private JLabel lblEpilog;
    private JPanel panel3;
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
    private JSeparator separator1;
    private JPanel vSpacer1;
    private JButton btnPowerSaveOn;
    private JButton btnPowerSaveOff;
    private JPanel vSpacer2;
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
    private JButton button11;
    private JButton button12;
    private JPanel pnlAbout;
    private JScrollPane scrollPane2;
    private JXEditorPane txtAbout;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
