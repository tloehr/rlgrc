package de.flashheart.rlgrc.ui.params;

import de.flashheart.rlgrc.misc.*;
import org.json.JSONObject;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class FarcryParams extends GameParams {

    private JTextField txtCapturePoints;
    private JTextField txtCaptureSirens;
    private JTextField txtRespawnTimer;

    public FarcryParams(JSONConfigs configs) {
        super(configs);
        initPanel();
    }


    private void initPanel() {
        load_defaults();

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

        setLayout(new RiverLayout(5, 5));
        add(default_components);
        add(new JLabel("Gametime in Seconds"), "br left");
        add(create_textfield("game_time", new NumberVerifier(BigDecimal.TEN, BigDecimal.valueOf(60000), true)), "left");
        add(new JLabel("Bombtime in Seconds"), "tab");
        add(create_textfield("bomb_time", new NumberVerifier(BigDecimal.TEN, BigDecimal.valueOf(60000), true)), "left");
        add(new JLabel("Capture Points"), "br left");
        add(txtCapturePoints, "hfill");
        add(new JLabel("Capture Sirens"), "br left");
        add(txtCaptureSirens, "hfill");


    }

    @Override
    public void from_params_to_ui() {
        super.from_params_to_ui();
        txtCapturePoints.setText(to_string_list(params.getJSONObject("agents").getJSONArray("capture_points")));
        txtCaptureSirens.setText(to_string_list(params.getJSONObject("agents").getJSONArray("capture_sirens")));
        txtRespawnTimer.setText(Integer.toString(params.getJSONObject("spawns").getInt("respawn_time")));
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
    public JSONObject from_ui_to_params() {
        super.from_ui_to_params();

        JSONObject agents = new JSONObject();
        agents.put("capture_points", from_string_list(txtCapturePoints.getText()));
        agents.put("capture_sirens", from_string_list(txtCaptureSirens.getText()));
        agents.put("sirens", from_string_list(txtCaptureSirens.getText()));
        params.put("agents", agents);

        params.put("class", "de.flashheart.rlg.commander.games.Farcry");
        params.put("mode", getMode());
        return params;
    }

    @Override
    public String getMode() {
        return "farcry";
    }

    @Override
    public String get_score_as_html(JSONObject game_state) {

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
