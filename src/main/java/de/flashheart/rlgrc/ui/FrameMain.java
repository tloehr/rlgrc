/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc.ui;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import de.flashheart.rlgrc.misc.JSONConfigs;
import de.flashheart.rlgrc.networking.RestHandler;
import de.flashheart.rlgrc.networking.RestResponseEvent;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.HorizontalLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.SchedulerException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class FrameMain extends JFrame {
    public static final String _state_PROLOG = "PROLOG";
    public static final String _state_TEAMS_NOT_READY = "TEAMS_NOT_READY";
    public static final String _state_TEAMS_READY = "TEAMS_READY";
    public static final String _state_RUNNING = "RUNNING";
    public static final String _state_PAUSING = "PAUSING";
    public static final String _state_RESUMING = "RESUMING";
    public static final String _state_EPILOG = "EPILOG";
    public static final List<String> _states_ = Arrays.asList(_state_PROLOG, _state_TEAMS_NOT_READY, _state_TEAMS_READY, _state_RUNNING, _state_PAUSING, _state_RESUMING, _state_EPILOG);
    private static final int TAB_GAME_PARAMS = 0;
    private static final int TAB_ACTIVE_GAME = 1;
    private static final int TAB_SERVER = 2;
    private static final int TAB_AGENTS = 3;
    private static final int TAB_ABOUT = 4;
    private JSONObject current_state;
    private final JSONConfigs configs;
    private RestHandler restHandler;
    private final PnlServer pnlServer;
    private final PnlAgents pnlAgents;
    private final PnlGameParams pnlParams;
    private final PnlActiveGame pnlActiveGame;
    public static final Font MY_FONT = new Font(".SF NS Text", Font.PLAIN, 18);

    public FrameMain(JSONConfigs configs) throws SchedulerException, IOException {
        this.configs = configs;
        this.current_state = new JSONObject();


        this.restHandler = new RestHandler(() -> on_connect(), () -> on_disconnect());
        pnlParams = new PnlGameParams(restHandler, configs, this, "1");
        pnlActiveGame = new PnlActiveGame(restHandler, configs, this, "1");
        pnlServer = new PnlServer(restHandler, "1");
        pnlAgents = new PnlAgents(restHandler, configs);
        this.restHandler.addRestResponseListener(event -> on_response(event));
        this.restHandler.addLoggableEventListener(event -> pnlServer.addLog(event.getEvent()));


        setTitle("rlgrc v" + configs.getBuildProperties().getProperty("my.version") + " b" + configs.getBuildProperties().getProperty("buildNumber") + " " + configs.getBuildProperties().getProperty("buildDate"));

        initComponents();
        initTab();
        initFrame();
        pack();

        if (configs.getConfigs().optBoolean("maximize_on_start")) setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void initTab() {
        pnlMain.addTab("Params", pnlParams);
        pnlMain.addTab("Active", pnlActiveGame);
        pnlMain.addTab("Server", pnlServer);
        pnlMain.addTab("Agents", pnlAgents);
        pnlMain.addTab("About", new PnlAbout());
        pnlMain.setSelectedIndex(TAB_ABOUT);
        pnlMain.addChangeListener(e -> tab_selection_changed(e));
    }

    private void initFrame() throws IOException, SchedulerException {
        pnlParams.addPropertyChangeListener("game_unloaded", evt -> {
            current_state.clear();
            pnlActiveGame.disconnect_sse_client();
            update();
        });
        pnlParams.addPropertyChangeListener("game_loaded", evt -> {
            pnlMain.setSelectedIndex(TAB_ACTIVE_GAME);
            current_state = (JSONObject) evt.getNewValue();
            update();
        });
        pnlActiveGame.addPropertyChangeListener("game_state_changed", evt -> {
            current_state = (JSONObject) evt.getNewValue();
            update();
        });
        txtURI.setModel(new DefaultComboBoxModel(configs.getConfigs().getJSONArray("recent").toList().toArray()));
        txtURI.getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                add_to_recent_uris_list(txtURI.getSelectedItem().toString().trim());
            }
        });
        txtURI.addItemListener(e -> add_to_recent_uris_list(txtURI.getSelectedItem().toString().trim()));
        cmbGameSlots.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            String text = "--";
            if (value != null) {
                text = "#" + value;
            }
            return new DefaultListCellRenderer().getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        });

        pnlMain.setEnabled(false);
    }

    private void update() {
        pnlMain.setEnabled(restHandler.isConnected());
        cmbGameSlots.setEnabled(restHandler.isConnected());
        //btnConnect.setIcon(new ImageIcon(getClass().getResource(restHandler.isConnected() ? "/artwork/rlgrc-48-connected.png" : "/artwork/rlgrc-48-disconnected.png")));
        if (restHandler.isConnected()) {
            pnlMain.setEnabledAt(TAB_GAME_PARAMS, !current_state.has("game_state")
                    || current_state.getString("game_state").equals(_state_PROLOG));
            pnlMain.setEnabledAt(TAB_ACTIVE_GAME, current_state.has("game_state"));
        } else {
            pnlMain.setSelectedIndex(TAB_ABOUT);
        }
    }

    void on_response(RestResponseEvent event) {
        event.getException().ifPresent(exception -> {
            lblResponse.setIcon(new ImageIcon(getClass().getResource("/artwork/ledred.png")));
            lblResponse.setText(StringUtils.left(exception.getMessage(), 70));
            lblResponse.setToolTipText(exception.toString());
        });

        event.getResponse().ifPresent(response -> {
            String icon = "/artwork/ledyellow.png";
            if (response.getStatusInfo().getFamily().name().equalsIgnoreCase("CLIENT_ERROR") || response.getStatusInfo().getFamily().name().equalsIgnoreCase("SERVER_ERROR"))
                icon = "/artwork/ledred.png";
            if (response.getStatusInfo().getFamily().name().equalsIgnoreCase("SUCCESSFUL"))
                icon = "/artwork/ledgreen.png";
            lblResponse.setIcon(new ImageIcon(getClass().getResource(icon)));
            lblResponse.setText(response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase());
            lblResponse.setToolTipText(event.getDetails().orElse(null));
        });
    }

    void on_connect() {
        int max_number_of_games = restHandler.get("system/get_max_number_of_games").getInt("max_number_of_games");
        for (int i = 0; i < max_number_of_games; i++) cmbGameSlots.addItem(i + 1);
        current_state = restHandler.get("game/status", current_game_id()); // just in case a game is already running
        btnConnect.setIcon(new ImageIcon(getClass().getResource("/artwork/rlgrc-48-connected.png")));
        pnlActiveGame.setUri(restHandler.getUri());
        update();
    }

    void on_disconnect() {
        pnlMain.setEnabled(false);
        cmbGameSlots.setEnabled(false);
        btnConnect.setIcon(new ImageIcon(getClass().getResource("/artwork/rlgrc-48-disconnected.png")));
        pnlMain.setSelectedIndex(TAB_ABOUT);
//        pnlActiveGame.disconnect_sse_client();
        current_state.clear();
        cmbGameSlots.removeAllItems();
        lblResponse.setIcon(new ImageIcon(getClass().getResource("/artwork/leddarkred.png")));
        lblResponse.setText("not connected");
    }

    private void txtURIFocusLost(FocusEvent e) {
        //configs.put(Configs.REST_URI, txtURI.getSelectedItem().toString().trim());
        add_to_recent_uris_list(txtURI.getSelectedItem().toString().trim());
    }


    void add_to_recent_uris_list(String uri) {
        List<String> list = configs.getConfigs().getJSONArray("recent").toList().stream().map(Object::toString).collect(Collectors.toList());
        if (list.contains(uri)) list.remove(uri);
        // put last uri at the head of the list
        // nice trick btw: https://stackoverflow.com/a/28631202
        Collections.reverse(list);
        list.add(uri);
        Collections.reverse(list);
        configs.getConfigs().put("recent", new JSONArray(list));
        configs.saveConfigs();
    }


    private void tab_selection_changed(ChangeEvent e) {
        pnlParams.setSelected(pnlMain.getSelectedIndex() == TAB_GAME_PARAMS);
        pnlActiveGame.setSelected(pnlMain.getSelectedIndex() == TAB_ACTIVE_GAME);
        pnlServer.setSelected(pnlMain.getSelectedIndex() == TAB_SERVER);
        pnlAgents.setSelected(pnlMain.getSelectedIndex() == TAB_AGENTS);
    }


    private void btnConnect(ActionEvent e) {
        if (restHandler.isConnected()) {
            pnlActiveGame.disconnect_sse_client();
            restHandler.disconnect();
        } else restHandler.connect(txtURI.getSelectedItem());
    }

    private void cmbGameSlotsItemStateChanged(ItemEvent e) {
        if (e.getStateChange() != ItemEvent.SELECTED) return;
//        pnlServer.setCurrent_game_id(e.getItem().toString());
//        pnlParams.setCurrent_game_id(e.getItem().toString());
//        pnl
    }


    private String current_game_id() {
        return cmbGameSlots.getSelectedItem().toString();
    }

    private void thisWindowClosing(WindowEvent e) {
        restHandler.disconnect();
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        mainPanel = new JPanel();
        panel1 = new JPanel();
        txtURI = new JComboBox();
        btnConnect = new JButton();
        panel2 = new JPanel();
        cmbGameSlots = new JComboBox();
        lblResponse = new JLabel();
        pnlMain = new JTabbedPane();

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
                txtURI.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        txtURIFocusLost(e);
                    }
                });
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
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
