package de.flashheart.rlgrc.ui.params;

import de.flashheart.rlgrc.misc.*;
import de.flashheart.rlgrc.ui.params.zeus.CenterFlagsZeus;
import de.flashheart.rlgrc.ui.params.zeus.ZeusDialog;
import org.json.JSONObject;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Maggi1Params extends GameParams {

    private JTextField txtCapturePoints;
    private JTextField txtSirens;
    private JButton btn_switch;

    public Maggi1Params(JSONConfigs configs) {
        super(configs);
        initPanel();
    }


    private void initPanel() {
        load_defaults();
        txtCapturePoints = new JTextField();
        txtCapturePoints.setInputVerifier(new NotEmptyVerifier());
        txtCapturePoints.setFont(default_font);
        txtCapturePoints.setToolTipText("Comma separated");

        txtSirens = new JTextField();
        txtSirens.setInputVerifier(new NotEmptyVerifier());
        txtSirens.setFont(default_font);
        txtSirens.setToolTipText("Comma separated");

        setLayout(new RiverLayout(5, 5));
        add(default_components);
        add(new JLabel("Gametime in seconds"), "br left");
        add(create_textfield("game_time", new NumberVerifier(BigDecimal.ONE, BigDecimal.valueOf(7200), true)), "left");
        add(new JLabel("Capture Points"), "br left");
        add(txtCapturePoints, "hfill");
        add(new JLabel("Seconds to Lock"), "br left");
        add(create_textfield("lock_time", new NumberVerifier(BigDecimal.ONE, BigDecimal.valueOf(20L), true)), "left");
        add(new JLabel("Seconds to Unlock"), "left");
        add(create_textfield("unlock_time", new NumberVerifier(BigDecimal.ONE, BigDecimal.valueOf(20L), true)), "left");
        add(new JLabel("Sirens"), "br left");
        add(txtSirens, "hfill");
    }

    @Override
    public void from_params_to_ui() {
        super.from_params_to_ui();
        txtCapturePoints.setText(to_string_list(params.getJSONObject("agents").getJSONArray("capture_points")));
        txtSirens.setText(to_string_list(params.getJSONObject("agents").getJSONArray("sirens")));
    }

    @Override
    String get_in_game_event_description(JSONObject event) {
        String type = event.getString("type");
        if (type.equalsIgnoreCase("general_game_state_change")) {
            return event.getString("message");
        }

        if (type.equalsIgnoreCase("in_game_state_change")) {
            String zeus = (event.has("zeus") ? HTML.linebreak() + "(by the hand of ZEUS)" : "");
            if (event.getString("item").equals("capture_point")) {
                return event.getString("agent") + " => " + event.getString("state")
                        + zeus;
            }
            if (event.getString("item").equals("add_seconds")) {
                String text = event.getLong("amount") >= 0 ? " has been granted %d seconds" : " has lost %d seconds";
                return "Team " + event.getString("team") + String.format(text, Math.abs(event.getLong("amount")))
                        + zeus;
            }
        }
        return "";
    }

    @Override
    public void from_ui_to_params() {
        super.from_ui_to_params();

        JSONObject agents = new JSONObject();
        agents.put("capture_points", from_string_list(txtCapturePoints.getText()));
        agents.put("sirens", from_string_list(txtSirens.getText()));
        params.put("agents", agents);

        params.put("class", "de.flashheart.rlg.commander.games.Maggi1");
        params.put("mode", getMode());
    }

    @Override
    public String getMode() {
        return "maggi1";
    }

    @Override
    public String get_score_as_html(JSONObject game_state) {
        JSONObject firstEvent = game_state.getJSONArray("in_game_events").getJSONObject(0);
        LocalDateTime first_pit = JavaTimeConverter.from_iso8601(firstEvent.getString("pit"));


        // Preparing Score Table

        String html =
                HTML.document(CSS,
                        HTML.h1("%s @ " + first_pit.format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm"))) +
                                HTML.h2("Match length: %s, remaining time: %s") +
                                HTML.h2("Score") +
                                "Team Red: %s" + HTML.linebreak() +
                                "Team Blue: %s" +
                                HTML.h2("Events") +
                                generate_table_for_events(game_state.getJSONArray("in_game_events"))
                );
        return String.format(html,
                game_state.optString("comment"),
                JavaTimeConverter.format(Instant.ofEpochSecond(game_state.getInt("match_length"))),
                JavaTimeConverter.format(Instant.ofEpochSecond(game_state.getInt("remaining"))),
                game_state.getInt("red_points"),
                game_state.getInt("blue_points")
        );
    }

    @Override
    public Optional<ZeusDialog> get_zeus() {
        return Optional.empty();
    }
}
