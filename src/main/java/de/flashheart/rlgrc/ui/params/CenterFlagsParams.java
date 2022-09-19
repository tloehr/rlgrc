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
import java.util.*;
import java.util.stream.Collectors;

public class CenterFlagsParams extends GameParams {

    private final JFrame owner;
    private JTextField txtCapturePoints;
    private JTextField txtSirens;
    private JButton btn_switch;

    public CenterFlagsParams(JSONConfigs configs, JFrame owner) {
        super(configs);
        this.owner = owner;
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
            String zeus = (event.has("zeus") ? HTML.linebreak()+ "(by the hand of ZEUS)" : "");
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

        params.put("class", "de.flashheart.rlg.commander.games.CenterFlags");
        params.put("mode", getMode());
    }

    @Override
    public String getMode() {
        return "centerflags";
    }

    @Override
    public String get_score_as_html(JSONObject game_state) {
        JSONObject firstEvent = game_state.getJSONArray("in_game_events").getJSONObject(0);
        LocalDateTime first_pit = JavaTimeConverter.from_iso8601(firstEvent.getString("pit"));


        // Preparing Score Table
        JSONObject scores = game_state.getJSONObject("scores");
        List capture_points = game_state.getJSONObject("agents").getJSONArray("capture_points").toList().stream().sorted().collect(Collectors.toList());
        StringBuffer buffer = new StringBuffer();

        capture_points.forEach(o -> {
            String agent = o.toString();

            String color = "white";
            if (game_state.getJSONObject("agent_states").getString(agent).toLowerCase().matches("red|game_over_red"))
                color = "red";
            if (game_state.getJSONObject("agent_states").getString(agent).toLowerCase().matches("blue|game_over_blue"))
                color = "blue";

            buffer.append(HTML.table_tr(
                    String.format("<td bgcolor=%s>" + agent + "</td>", color) +
                            HTML.table_td(JavaTimeConverter.format(Instant.ofEpochMilli(scores.getJSONObject("blue").getLong(agent)))) +
                            HTML.table_td(JavaTimeConverter.format(Instant.ofEpochMilli(scores.getJSONObject("red").getLong(agent))))
            ));
        });
        buffer.append(HTML.table_tr(
                HTML.table_td(HTML.bold("SUMs")) +
                        HTML.table_td(HTML.bold(JavaTimeConverter.format(Instant.ofEpochMilli(scores.getJSONObject("blue").getLong("all"))))) +
                        HTML.table_td(HTML.bold(JavaTimeConverter.format(Instant.ofEpochMilli(scores.getJSONObject("red").getLong("all")))))
        ));

        String html =
                HTML.document(CSS,
                        HTML.h1("Center-Flags @ " + first_pit.format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm"))) +
                                HTML.h2("Match length: %s, remaining time: %s") +
                                HTML.h2("Score") +
                                HTML.table(
                                        HTML.table_tr(
                                                HTML.table_th("Agent") +
                                                        HTML.table_th("Blue Score") +
                                                        HTML.table_th("Red Score")
                                        ),
                                        buffer.toString(), "1") +
                                HTML.h2("Events") +
                                generate_table_for_events(game_state.getJSONArray("in_game_events"))
                );
        return String.format(html,
                JavaTimeConverter.format(Instant.ofEpochSecond(game_state.getInt("match_length"))),
                JavaTimeConverter.format(Instant.ofEpochSecond(game_state.getInt("remaining"))),
                game_state.getString("game_state")
        );
    }

    @Override
    public Optional<ZeusDialog> get_zeus() {
        return Optional.of(new CenterFlagsZeus(owner, from_string_list(txtCapturePoints.getText())));
    }
}
