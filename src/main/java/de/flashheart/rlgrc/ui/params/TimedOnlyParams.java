package de.flashheart.rlgrc.ui.params;

import de.flashheart.rlgrc.misc.*;
import org.jdesktop.swingx.JXTitledPanel;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimedOnlyParams extends GameParams {

    private final JFrame owner;
    private JTextField txtLine1, txtLine2, txtLine3;
    private JTextField txtSirens;

    public TimedOnlyParams(JSONConfigs configs, JFrame owner) {
        super(configs);
        this.owner = owner;
        initPanel();
    }


    private void initPanel() {
        load_defaults();
        txtLine1 = new JTextField(20);
        txtLine1.setFont(default_font);
        txtLine2 = new JTextField(20);
        txtLine2.setFont(default_font);
        txtLine3 = new JTextField(20);
        txtLine3.setFont(default_font);

        txtSirens = new JTextField(20);
        txtSirens.setInputVerifier(new NotEmptyVerifier());
        txtSirens.setFont(default_font);
        txtSirens.setToolTipText("Comma separated");

        setLayout(new RiverLayout(5, 5));
        add(default_components);
        add(new JLabel("Gametime in seconds"), "br left");
        add(create_textfield("game_time", new NumberVerifier(BigDecimal.ONE, BigDecimal.valueOf(7200), true)), "left");
        add(create_checkbox("count_respawns", "Count Respawns"), "left");

        add(new JLabel("Display Content", SwingConstants.LEADING), "br left");
        add(new JLabel("Line1", SwingConstants.LEADING), "br left");
        add(txtLine1, "left");
        add(new JLabel("Line2", SwingConstants.LEADING), "tab");
        add(txtLine2, "left");
        add(new JLabel("Line3", SwingConstants.LEADING), "tab");
        add(txtLine3, "left");
        add(new JLabel("Sirens"), "br left");
        add(txtSirens, "left");
    }

    @Override
    public void from_params_to_ui() {
        super.from_params_to_ui();
        txtSirens.setText(to_string_list(params.getJSONObject("agents").getJSONArray("sirens")));
        txtLine1.setText(params.getJSONObject("display").getString("line1"));
        txtLine2.setText(params.getJSONObject("display").getString("line2"));
        txtLine3.setText(params.getJSONObject("display").getString("line3"));
    }

    @Override
    String get_in_game_event_description(JSONObject event) {
        String type = event.getString("type");
        if (type.equalsIgnoreCase("general_game_state_change")) {
            return event.getString("message");
        }

        return "";
    }

    @Override
    public void from_ui_to_params() {
        super.from_ui_to_params();

        JSONObject agents = new JSONObject();
        agents.put("sirens", from_string_list(txtSirens.getText()));
        params.put("agents", agents);

        params.put("class", "de.flashheart.rlg.commander.games.TimedOnly");
        params.put("mode", getMode());

        params.put("display", new JSONObject()
                .put("line1", txtLine1.getText())
                .put("line2", txtLine2.getText())
                .put("line3", txtLine3.getText())
        );

    }

    @Override
    public String getMode() {
        return "timed_only";
    }


    @Override
    public String get_score_as_html(JSONObject game_state) {
        JSONObject firstEvent = game_state.getJSONArray("in_game_events").getJSONObject(0);
        LocalDateTime first_pit = JavaTimeConverter.from_iso8601(firstEvent.getString("pit"));

        boolean count_respawns = game_state.optBoolean("count_respawns");

        if (count_respawns) {
            String html =
                    HTML.document(CSS,
                            HTML.h1("%s @ " + first_pit.format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm"))) +
                                    HTML.h2("Match length: %s, remaining time: %s") +
                                    HTML.h3("Number of Respawns") +
                                    "Team Red: %s" + HTML.linebreak() +
                                    "Team Blue: %s" +
                                    HTML.h2("Events") +
                                    generate_table_for_events(game_state.getJSONArray("in_game_events"))
                    );

            return String.format(html,
                    game_state.optString("comment"),
                    JavaTimeConverter.format(Instant.ofEpochSecond(game_state.getInt("match_length"))),
                    JavaTimeConverter.format(Instant.ofEpochSecond(game_state.getInt("remaining"))),
                    game_state.getInt("red_respawns"),
                    game_state.getInt("blue_respawns")
            );
        } else {
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
                    JavaTimeConverter.format(Instant.ofEpochSecond(game_state.getInt("remaining"))));
        }

    }


}
