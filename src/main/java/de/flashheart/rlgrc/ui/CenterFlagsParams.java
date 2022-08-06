package de.flashheart.rlgrc.ui;

import de.flashheart.rlgrc.misc.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.Color;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class CenterFlagsParams extends GameParams {

    private JTextField txtCapturePoints;
    private JTextField txtRed;
    private JTextField txtBlue;
    private JTextField txtSirens;
    private JButton btn_switch;

    public CenterFlagsParams(Configs configs) {
        super(configs);
        initPanel();
    }


    private void initPanel() {

        txtCapturePoints = new JTextField();
        txtCapturePoints.setInputVerifier(new NotEmptyVerifier());
        txtCapturePoints.setFont(default_font);
        txtCapturePoints.setToolTipText("Comma separated");

        txtSirens = new JTextField();
        txtSirens.setInputVerifier(new NotEmptyVerifier());
        txtSirens.setFont(default_font);
        txtSirens.setToolTipText("Comma separated");

        txtRed = new JTextField();
        txtRed.setBackground(Color.RED);
        txtRed.setInputVerifier(new NotEmptyVerifier());
        txtRed.setFont(default_font);

        txtBlue = new JTextField();
        txtBlue.setBackground(Color.BLUE);
        txtBlue.setInputVerifier(new NotEmptyVerifier());
        txtBlue.setFont(default_font);

        btn_switch = new JButton(new ImageIcon(getClass().getResource("/artwork/lc_arrowshapes.png")));
        btn_switch.addActionListener(e -> {
            String a = txtBlue.getText();
            txtBlue.setText(txtRed.getText());
            txtRed.setText(a);
        });

        setLayout(new RiverLayout(5, 5));
        add(default_components);
        add(new JLabel("Gametime in seconds"), "br left");
        add(create_textfield("game_time", new NumberVerifier(BigDecimal.ONE, BigDecimal.valueOf(120), true)), "left");
        add(new JLabel("Capture Points"), "br left");
        add(txtCapturePoints, "hfill");
        add(new JLabel("Sirens"), "br left");
        add(txtSirens, "hfill");
        add(new JSeparator(SwingConstants.HORIZONTAL), "br hfill");
        add(txtRed, "br left");
        add(btn_switch, "left");
        add(txtBlue, "left");


    }

    @Override
    protected void set_parameters() {
        super.set_parameters();
        txtCapturePoints.setText(to_string_list(params.getJSONObject("agents").getJSONArray("capture_points")));
        txtRed.setText(params.getJSONObject("agents").getJSONArray("red_spawn").getString(0));
        txtBlue.setText(params.getJSONObject("agents").getJSONArray("blue_spawn").getString(0));
        txtSirens.setText(to_string_list(params.getJSONObject("agents").getJSONArray("sirens")));
    }

    @Override
    String get_in_game_event_description(JSONObject game_state) {
        String type = game_state.getString("type");
        if (type.equalsIgnoreCase("general_game_state_change")) {
            return game_state.getString("message");
        }
        if (type.equalsIgnoreCase("in_game_state_change")) {
            if (game_state.getString("item").equals("capture_point")) {
                return game_state.getString("agent") + " => " + game_state.getString("state");
            }
        }
        return "";
    }

    @Override
    protected JSONObject read_parameters() {
        super.read_parameters();

        JSONObject agents = new JSONObject();
        agents.put("capture_points", to_jsonarray(txtCapturePoints.getText()));
        agents.put("sirens", to_jsonarray(txtSirens.getText()));
        agents.put("red_spawn", new JSONArray().put(txtRed.getText()));
        agents.put("blue_spawn", new JSONArray().put(txtBlue.getText()));
        agents.put("spawns", new JSONArray().put(txtRed.getText()).put(txtBlue.getText()));
        params.put("agents", agents);

        params.put("class", "de.flashheart.rlg.commander.games.CenterFlags");
        params.put("mode", getMode());
        return params;
    }

    @Override
    String getMode() {
        return "centerflags";
    }

    @Override
    String get_score_as_html(JSONObject game_state) {
        JSONObject firstEvent = game_state.getJSONArray("in_game_events").getJSONObject(0);
        LocalDateTime first_pit = JavaTimeConverter.from_iso8601(firstEvent.getString("pit"));


        // Preparing Score Table
        JSONObject scores = game_state.getJSONObject("scores");
        List capture_points = game_state.getJSONObject("agents").getJSONArray("capture_points").toList().stream().sorted().collect(Collectors.toList());
        StringBuffer buffer = new StringBuffer();

        capture_points.forEach(o -> {
            String agent = o.toString();

            String color = "white";
            if (game_state.getJSONObject( "agent_states").getString(agent).equalsIgnoreCase("red")) color = "red";
            if (game_state.getJSONObject( "agent_states").getString(agent).equalsIgnoreCase("blue")) color = "blue";

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
                        HTML.h1("Center-Flags @" + first_pit.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))) +
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
}
