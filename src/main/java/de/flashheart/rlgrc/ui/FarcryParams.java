package de.flashheart.rlgrc.ui;

import de.flashheart.rlgrc.misc.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class FarcryParams extends GameParams {

    private JTextField txtCapturePoints;
    private JTextField txtCaptureSirens;
    private JTextField txtAttacker;
    private JTextField txtDefender;
    private JTextField txtRespawnTimer;
    private JButton btn_switch;

    public FarcryParams(JSONConfigs configs) {
        super(configs);
        initPanel();
    }


    private void initPanel() {

        txtCapturePoints = new JTextField();
        txtCapturePoints.setInputVerifier(new NotEmptyVerifier());
        txtCapturePoints.setFont(default_font);
        txtCapturePoints.setToolTipText("Comma separated");

        txtCaptureSirens = new JTextField();
        txtCaptureSirens.setInputVerifier(new NotEmptyVerifier());
        txtCaptureSirens.setFont(default_font);
        txtCaptureSirens.setToolTipText("Comma separated");

        txtRespawnTimer = new JTextField();
        txtRespawnTimer.setInputVerifier(new NumberVerifier(BigDecimal.ZERO, BigDecimal.valueOf(1000l), true));
        txtRespawnTimer.setFont(default_font);
        txtRespawnTimer.setToolTipText("set 0 to disable mechanism");

        txtAttacker = new JTextField();
        txtAttacker.setBackground(Color.RED);
        txtAttacker.setInputVerifier(new NotEmptyVerifier());
        txtAttacker.setFont(default_font);

        txtDefender = new JTextField();
        txtDefender.setBackground(Color.BLUE);
        txtDefender.setInputVerifier(new NotEmptyVerifier());
        txtDefender.setFont(default_font);

        btn_switch = new JButton(new ImageIcon(getClass().getResource("/artwork/lc_arrowshapes.png")));
        btn_switch.addActionListener(e -> {
            String a = txtDefender.getText();
            txtDefender.setText(txtAttacker.getText());
            txtAttacker.setText(a);
        });

        setLayout(new RiverLayout(5, 5));
        add(default_components);
        add(new JLabel("Gametime in Seconds"), "br left");
        add(create_textfield("game_time", new NumberVerifier(BigDecimal.TEN, BigDecimal.valueOf(60000), true)), "left");
        add(new JLabel("Bombtime in Seconds"), "tab");
        add(create_textfield("bomb_time", new NumberVerifier(BigDecimal.TEN, BigDecimal.valueOf(60000), true)), "left");
        add(new JLabel("Respawn Timer"), "tab");
        add(create_textfield("respawn_time", new NumberVerifier(BigDecimal.ZERO, BigDecimal.valueOf(1000), true)), "left");
        add(new JLabel("Capture Points"), "br left");
        add(txtCapturePoints, "hfill");
        add(new JLabel("Capture Sirens"), "br left");
        add(txtCaptureSirens, "hfill");
        add(new JSeparator(SwingConstants.HORIZONTAL), "br hfill");
        add(txtAttacker, "br left");
        add(btn_switch, "left");
        add(txtDefender, "left");

    }

    @Override
    protected void set_parameters() {
        super.set_parameters();
        txtCapturePoints.setText(to_string_list(params.getJSONObject("agents").getJSONArray("capture_points")));
        txtCaptureSirens.setText(to_string_list(params.getJSONObject("agents").getJSONArray("capture_sirens")));
        txtAttacker.setText(params.getJSONObject("agents").getJSONArray("attacker_spawn").getString(0));
        txtDefender.setText(params.getJSONObject("agents").getJSONArray("defender_spawn").getString(0));
    }

    @Override
    String get_in_game_event_description(JSONObject event) {
        String type = event.getString("type");
        if (type.equalsIgnoreCase("general_game_state_change")) {
            return event.getString("message");
        }
        if (type.equalsIgnoreCase("in_game_state_change")) {
            if (event.getString("item").equals("capture_point")) {
                return event.getString("agent") + " => " + event.getString("state");
            }
            if (event.getString("item").equals("overtime")) {
                return "overtime";
            }
        }
        return "";
    }

    @Override
    protected JSONObject read_parameters() {
        super.read_parameters();

        JSONObject agents = new JSONObject();
        agents.put("capture_points", to_jsonarray(txtCapturePoints.getText()));
        agents.put("capture_sirens", to_jsonarray(txtCaptureSirens.getText()));
        agents.put("sirens", to_jsonarray(txtCaptureSirens.getText()));
        agents.put("attacker_spawn", new JSONArray().put(txtAttacker.getText()));
        agents.put("defender_spawn", new JSONArray().put(txtDefender.getText()));
        agents.put("spawns", new JSONArray().put(txtAttacker.getText()).put(txtDefender.getText()));
        params.put("agents", agents);

        params.put("class", "de.flashheart.rlg.commander.games.Farcry");
        params.put("mode", getMode());
        return params;
    }

    @Override
    String getMode() {
        return "farcry";
    }

    @Override
    String get_score_as_html(JSONObject game_state) {

        JSONObject firstEvent = game_state.getJSONArray("in_game_events").getJSONObject(0);

        LocalDateTime first_pit = JavaTimeConverter.from_iso8601(firstEvent.getString("pit"));
        int capture_points_taken = game_state.getInt("capture_points_taken");
        int max_capture_points = game_state.getInt("max_capture_points");


        String state = game_state.getString("game_state");
        if (state.equals("RUNNING")) {
            String next = "last one";
            if (max_capture_points > 1 && capture_points_taken < max_capture_points - 1) {
                next = "Next: " + game_state.getJSONObject("agents").getJSONArray("capture_points").getString(capture_points_taken + 1);
            }
            String active_agent = capture_points_taken < max_capture_points ? game_state.getJSONObject("agents").getJSONArray("capture_points").getString(capture_points_taken) : "";
            state = active_agent + ": " + game_state.getJSONObject("agent_states").getString(active_agent) + ", " + next;
        }
        LocalDateTime remainingTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(game_state.getInt("remaining")),
                TimeZone.getTimeZone("UTC").toZoneId());

        String html =
                HTML.document(CSS,
                        HTML.h1("FarCry Assault @ " + first_pit.format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm"))) +
                                HTML.h2("CPs taken: %d out of %d") +
                                HTML.h3("%s") +
                                HTML.h3("Remaining Time %s") +
                                HTML.h2("Events") +
                                generate_table_for_events(game_state.getJSONArray("in_game_events"))
                );
        return String.format(html, capture_points_taken, max_capture_points, state, remainingTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

    }
}
