/*
 * Created by JFormDesigner on Fri Sep 16 14:37:15 CEST 2022
 */

package de.flashheart.rlgrc.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javax.swing.*;

import de.flashheart.rlgrc.jobs.FlashStateLedJob;
import de.flashheart.rlgrc.misc.JSONConfigs;
import de.flashheart.rlgrc.networking.RestHandler;
import de.flashheart.rlgrc.networking.SSEClient;
import de.flashheart.rlgrc.ui.params.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.jdesktop.swingx.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class PnlActiveGame extends JPanel {
    public final java.util.List<JButton> _message_buttons;
    public final List<JLabel> _state_labels;
    private final RestHandler restHandler;
    private final JSONConfigs configs;
    private final JFrame owner;
    private String current_game_id;
    private final Scheduler scheduler;
    private final JobKey state_flashing_job;
    private JSONObject current_state;
    private String uri = "";
    private Optional<GameParams> current_game_params;
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
    private Optional<SSEClient> opt_sseClient;
    private LocalDateTime last_sse_received;
    private boolean summary_written_on_epilog = false;
    private boolean selected;

    public PnlActiveGame(RestHandler restHandler, JSONConfigs configs, JFrame owner, String current_game_id) throws SchedulerException {
        this.restHandler = restHandler;
        this.configs = configs;
        this.owner = owner;
        this.current_state = new JSONObject();
        this.current_game_params = Optional.empty();
        this.current_game_id = current_game_id;
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        this.scheduler.getContext().put("rlgrc", this);
        this.state_flashing_job = new JobKey(FlashStateLedJob.name, "group1");
        this.selected = false;
        this.opt_sseClient = Optional.empty();
        initComponents();
        this._message_buttons = Arrays.asList(btnPrepare, btnReset, btnReady, btnRun, btnPause, btnResume, btnContinue, btnGameOver);
        this._state_labels = Arrays.asList(lblProlog, lblTeamsNotReady, lblTeamsReady, lblRunning, lblPausing, lblResuming, lblEpilog);
        initPanel();
    }

    public void setCurrent_game_id(String current_game_id) {
        this.current_game_id = current_game_id;
        current_state = restHandler.get("game/status", current_game_id);
        update();
    }

    private void initPanel() throws SchedulerException {
        for (JButton msg_button : _message_buttons) {
            msg_button.addActionListener(e -> {
                Properties props = new Properties();
                props.put("id", current_game_id);
                props.put("message", e.getActionCommand());
                restHandler.post("game/process", "{}", props);
            });
        }

        // makes the STATE leds flash
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
    }


    void update() {
        //btnLastSSE(null);
        if (!current_state.has("game_state")) return;

        current_game_params.ifPresent(gameParams -> {
            String state = current_state.getString("game_state");
            final String mode = current_state.getString("mode");
            //String mode = current_state.getString("mode");
            final String html = gameParams.get_score_as_html(current_state);
            // if we do not run this in a different thread, the scrollpane wont go up
            // don't know why. but did cost me some time to find out.
            SwingUtilities.invokeLater(() -> {
                try {
                    new Thread(() -> {
                        if (txtGameStatus != null && html != null) {
                            txtGameStatus.setText(html);
                            revalidate();
                            repaint();
                        }
                    }).start();
                } catch (Exception e) {
                    log.warn(e);
                }
            });

            int index = FrameMain._states_.indexOf(state.toUpperCase());
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
            if (state.equals(FrameMain._state_PROLOG)) summary_written_on_epilog = false;
            if (state.equals(FrameMain._state_EPILOG) && !summary_written_on_epilog) {
                try {
                    summary_written_on_epilog = true;
                    String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                    FileUtils.writeStringToFile(new File(System.getProperty("workspace") + File.separator + "results" + File.separator + mode + "(at)" + filename + ".html"), html, Charset.defaultCharset());
                } catch (IOException e) {
                    log.warn(e);
                }
            }
            firePropertyChange("game_state_changed", "", current_state);
        });
    }

    private void create_game_params_if_needed() {
        String mode = current_state.optString("mode", "none");
        if (current_game_params.isPresent() && current_game_params.get().getMode().equals(mode)) return;
        if (mode.equals("conquest")) current_game_params = Optional.of(new ConquestParams(configs, owner));
        if (mode.equals("centerflags")) current_game_params = Optional.of(new CenterFlagsParams(configs, owner));
        if (mode.equals("farcry")) current_game_params = Optional.of(new FarcryParams(configs));
        if (mode.equals("signal")) current_game_params = Optional.of(new SignalParams(configs));
        if (mode.equals("stronghold")) current_game_params = Optional.of(new StrongholdParams(configs, owner));
        if (mode.equals("timed")) current_game_params = Optional.of(new TimedOnlyParams(configs, owner));
        if (mode.equals("none")) current_game_params = Optional.empty();
        current_game_params.ifPresent(gameParams -> gameParams.from_params_to_ui(current_state));
    }

    public void setSelected(boolean selected) {
        if (this.selected == selected) return;
        this.selected = selected;
        try {
            if (selected) {
                log.debug("SELECTED");
                connect_sse_client();
                current_state = restHandler.get("game/status", current_game_id);
                this.scheduler.start();
                create_game_params_if_needed();
                update();
            } else {
                log.debug("DESELECTED");
                this.scheduler.standby();
                disconnect_sse_client();
                current_game_params = Optional.empty();
            }
        } catch (SchedulerException ex) {
            log.error(ex);
        }
    }

    /**
     * called from the quartz scheduler
     */
    public void flash_state_led() {
        final String state = current_state.getString("game_state");
        SwingUtilities.invokeLater(() -> {
            int index = FrameMain._states_.indexOf(state.toUpperCase());
            // toggle led
            _state_labels.get(index).setEnabled(!_state_labels.get(index).isEnabled());
            btnLastSSE.setText(last_sse_received == null ? "never" : Duration.between(last_sse_received, LocalDateTime.now()).toSeconds() + "s ago");
        });
    }

    public void disconnect_sse_client() {
        opt_sseClient.ifPresent(sseClient -> sseClient.shutdown());
        opt_sseClient = Optional.empty();
    }

    private void connect_sse_client() {
        if (opt_sseClient.isPresent()) return;
        opt_sseClient = Optional.of(
                SSEClient.builder()
                        .url(uri + "/api/game-sse?id=" + current_game_id)
                        .useKeepAliveMechanismIfReceived(true)
                        .eventHandler(eventText -> {
                            try {
                                // does not work, when there are newlines in the received messages
                                log.trace("sse_event_received - new state: {}", eventText);
                                last_sse_received = LocalDateTime.now();
                                current_state = restHandler.get("game/status", current_game_id);
                                update();
                            } catch (JSONException jsonException) {
                                log.error(jsonException);
                                restHandler.disconnect();
                            }
                        })
                        .build()
        );
        last_sse_received = null;
        opt_sseClient.ifPresent(sseClient -> {
            sseClient.start();
            last_sse_received = LocalDateTime.now();
        });
    }

    private void btnLastSSE(ActionEvent e) {
//        opt_sseClient.ifPresent(sseClient -> {
//            if (!sseClient.isSubscribedSuccessfully()) {
//                shutdown_sse_client();
//                connect_sse_client();
//            }
//        });
        current_state = restHandler.get("game/status", current_game_id);
        update();
    }

    private void btnLockItemStateChanged(ItemEvent e) {
        update();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panel5 = new JPanel();
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
        panel3 = new JPanel();
        scrlGameStatus = new JScrollPane();
        txtGameStatus = new JTextPane();
        panel1 = new JPanel();
        btnLock = new JToggleButton();
        btnZeus = new JButton();
        hSpacer1 = new JPanel(null);
        btnLastSSE = new JButton();

        //======== this ========
        setLayout(new BorderLayout());

        //======== panel5 ========
        {
            panel5.setLayout(new VerticalLayout());

            //======== pnlMessages ========
            {
                pnlMessages.setLayout(new HorizontalLayout(5));

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
            panel5.add(pnlMessages);

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
            panel5.add(pnlGameStates);
        }
        add(panel5, BorderLayout.NORTH);

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
        add(panel3, BorderLayout.CENTER);

        //======== panel1 ========
        {
            panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));

            //---- btnLock ----
            btnLock.setText(null);
            btnLock.setIcon(new ImageIcon(getClass().getResource("/artwork/decrypted.png")));
            btnLock.setSelectedIcon(new ImageIcon(getClass().getResource("/artwork/encrypted.png")));
            btnLock.setContentAreaFilled(false);
            btnLock.setBorderPainted(false);
            btnLock.setPreferredSize(new Dimension(38, 38));
            btnLock.setToolTipText("Lock");
            btnLock.addItemListener(e -> btnLockItemStateChanged(e));
            panel1.add(btnLock);

            //---- btnZeus ----
            btnZeus.setText(null);
            btnZeus.setIcon(new ImageIcon(getClass().getResource("/artwork/zeus-28.png")));
            btnZeus.setSelectedIcon(null);
            btnZeus.setContentAreaFilled(false);
            btnZeus.setBorderPainted(false);
            btnZeus.setPreferredSize(new Dimension(38, 38));
            btnZeus.setToolTipText("Zeus");
            btnZeus.addActionListener(e -> btnZeus(e));
            panel1.add(btnZeus);
            panel1.add(hSpacer1);

            //---- btnLastSSE ----
            btnLastSSE.setText(null);
            btnLastSSE.setIcon(new ImageIcon(getClass().getResource("/artwork/irkickflash.png")));
            btnLastSSE.setToolTipText("Last Message received");
            btnLastSSE.addActionListener(e -> btnLastSSE(e));
            panel1.add(btnLastSSE);
        }
        add(panel1, BorderLayout.SOUTH);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    private void btnZeus(ActionEvent e) {
        current_game_params
                .ifPresent(gameParams -> gameParams.get_zeus()
                        .ifPresent(zeusDialog -> {
                            zeusDialog.add_property_change_listener(evt -> restHandler.post("game/zeus", evt.getNewValue().toString(), current_game_id));
                            zeusDialog.setVisible(true);
                        })
                );
    }


    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panel5;
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
    private JPanel panel3;
    private JScrollPane scrlGameStatus;
    private JTextPane txtGameStatus;
    private JPanel panel1;
    private JToggleButton btnLock;
    private JButton btnZeus;
    private JPanel hSpacer1;
    private JButton btnLastSSE;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
