package de.flashheart.rlgrc.ui.params;

import de.flashheart.rlgrc.misc.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimedOnlyParams extends GameParams {

    private final JFrame owner;
    private JTextField txtRedRing, txtYellowRing, txtGreenRing, txtBlueRing;
    private JTextField txtSirens;

    public TimedOnlyParams(JSONConfigs configs, JFrame owner) {
        super(configs);
        this.owner = owner;
        initPanel();
    }


    private void initPanel() {
        load_defaults();
        txtRedRing = new JTextField();
        txtRedRing.setFont(default_font);
        txtRedRing.setToolTipText("Comma separated");

        txtYellowRing = new JTextField();
        txtYellowRing.setFont(default_font);
        txtYellowRing.setToolTipText("Comma separated");


        txtGreenRing = new JTextField();
        txtGreenRing.setFont(default_font);
        txtGreenRing.setToolTipText("Comma separated");


        txtBlueRing = new JTextField();
        txtBlueRing.setFont(default_font);
        txtBlueRing.setToolTipText("Comma separated");


        txtSirens = new JTextField();
        txtSirens.setInputVerifier(new NotEmptyVerifier());
        txtSirens.setFont(default_font);
        txtSirens.setToolTipText("Comma separated");

        setLayout(new RiverLayout(5, 5));
        add(default_components);
        add(new JLabel("Gametime in seconds"), "br left");
        add(create_textfield("game_time", new NumberVerifier(BigDecimal.ONE, BigDecimal.valueOf(7200), true)), "left");
        add(create_checkbox("allow_defuse", "Allow Defuse"), "left");
        add(new JLabel("Ring 1", new ImageIcon(getClass().getResource("/artwork/ledred.png")), SwingConstants.LEADING), "br left");
        add(txtRedRing, "hfill");
        add(new JLabel("Ring 2", new ImageIcon(getClass().getResource("/artwork/ledyellow.png")), SwingConstants.LEADING), "br left");
        add(txtYellowRing, "hfill");
        add(new JLabel("Ring 3", new ImageIcon(getClass().getResource("/artwork/ledgreen.png")), SwingConstants.LEADING), "br left");
        add(txtGreenRing, "hfill");
        add(new JLabel("Ring 4", new ImageIcon(getClass().getResource("/artwork/ledblue.png")), SwingConstants.LEADING), "br left");
        add(txtBlueRing, "hfill");
        add(new JLabel("Sirens"), "br left");
        add(txtSirens, "hfill");
    }

    @Override
    public void from_params_to_ui() {
        super.from_params_to_ui();
        txtSirens.setText(to_string_list(params.getJSONObject("agents").getJSONArray("sirens")));
        txtRedRing.setText(to_string_list(params.getJSONObject("agents").getJSONArray("red")));
        txtYellowRing.setText(to_string_list(params.getJSONObject("agents").getJSONArray("ylw")));
        txtGreenRing.setText(to_string_list(params.getJSONObject("agents").getJSONArray("grn")));
        txtBlueRing.setText(to_string_list(params.getJSONObject("agents").getJSONArray("blu")));
    }

    @Override
    String get_in_game_event_description(JSONObject event) {
        String type = event.getString("type");
        if (type.equalsIgnoreCase("general_game_state_change")) {
            return event.getString("message");
        }
        if (type.equalsIgnoreCase("general_game_state_change")) {
            return event.getString("message");
        }

        if (type.equalsIgnoreCase("in_game_state_change")) {
            String zeus = (event.has("zeus") ? HTML.linebreak() + "(by the hand of ZEUS)" : "");
            if (event.getString("item").equals("capture_point")) {
                return event.getString("agent") + " => " + event.getString("state")
                        + zeus;
            }
            if (event.getString("item").equals("ring")) {
                return event.getString("ring") + " => " + event.getString("state")
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

        JSONArray red = from_string_list(txtRedRing.getText());
        JSONArray yellow = from_string_list(txtYellowRing.getText());
        JSONArray green = from_string_list(txtGreenRing.getText());
        JSONArray blue = from_string_list(txtBlueRing.getText());

        agents.put("red", red);
        agents.put("ylw", yellow);
        agents.put("grn", green);
        agents.put("blu", blue);

        agents.put("capture_points", new JSONArray().putAll(red).putAll(yellow).putAll(green).putAll(blue));
        agents.put("sirens", from_string_list(txtSirens.getText()));
        params.put("agents", agents);

        params.put("class", "de.flashheart.rlg.commander.games.Stronghold");
        params.put("mode", getMode());
    }

    @Override
    public String getMode() {
        return "timed";
    }


    @Override
    public String get_score_as_html(JSONObject game_state) {
        JSONObject firstEvent = game_state.getJSONArray("in_game_events").getJSONObject(0);
        LocalDateTime first_pit = JavaTimeConverter.from_iso8601(firstEvent.getString("pit"));


        String html =
                HTML.document(CSS,
                        HTML.h1("%s @ " + first_pit.format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm"))) +
                                HTML.h2("Match length: %s, remaining time: %s") +
                                HTML.h2("Events") +
                                generate_table_for_events(game_state.getJSONArray("in_game_events"))
                );

        return String.format(html,
                game_state.optString("comment"),
                JavaTimeConverter.format(Instant.ofEpochSecond(game_state.getInt("match_length"))),
                JavaTimeConverter.format(Instant.ofEpochSecond(game_state.getInt("remaining")))
        );

    }


}
