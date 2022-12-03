package de.flashheart.rlgrc.ui.params;

import com.google.common.io.Resources;
import de.flashheart.rlgrc.misc.*;
import de.flashheart.rlgrc.ui.FrameMain;
import de.flashheart.rlgrc.ui.params.zeus.ZeusDialog;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public abstract class GameParams extends JPanel {
    private final JSONConfigs configs;
    protected JSONObject params;
    protected Optional<File> file;
    protected JPanel default_components;
    protected JCheckBox cbWait4Teams2BReady;
    protected JTextField txt_starter_countdown;
    protected JCheckBox cbSilentGame; // don't use sirens
    protected JComboBox<String> cmbIntroMusic;
    private JButton btnSwitchSides;
    private JTextField txtBlueSpawn;
    private JTextField txtRedSpawn;
    private DateTimeFormatter dtf;
    protected String CSS = "";
    protected Font default_font = new Font(".SF NS Text", Font.PLAIN, 14);
    protected Font large_font = FrameMain.MY_FONT;
    protected HashMap<String, JComponent> components;
    private final int num_of_segments;

    public GameParams(JSONConfigs configs) {
        this(configs, 1); // for later use.
    }

    public GameParams(JSONConfigs configs, int num_of_segments) {
        super();
        log.debug("loading {}", getMode());
        this.num_of_segments = num_of_segments;
        components = new HashMap<>();
        this.configs = configs;
        load_default_css();
        dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
        file = Optional.empty();
        init_default_components();
    }

    private void init_default_components() {
        default_components = new JPanel(new RiverLayout(5, 5));
        txt_starter_countdown = new JTextField();
        txt_starter_countdown.setFont(default_font);
        txt_starter_countdown.setInputVerifier(new NumberVerifier(BigDecimal.ZERO, NumberVerifier.MAX, true));
        cbWait4Teams2BReady = new JCheckBox("Wait for Teams");
        cbWait4Teams2BReady.setFont(default_font);
        cbSilentGame = new JCheckBox("Silent Game");
        cbSilentGame.setFont(default_font);
        cbSilentGame.setToolTipText("a silent game doesn't use any sirens. works as expected, otherwise.");
        //StringUtils.splitCommaSeparated(configs.get(Configs.INTRO_MP3_FILES), true)
//        cmbIntroMusic = new JComboBox<>(StringUtils.splitCommaSeparated(configs.get(Configs.INTRO_MP3_FILES), true));

        cmbIntroMusic = new JComboBox<>(configs.getConfigs()
                .getJSONObject("audio")
                .getJSONArray("intro")
                .toList().stream().sorted().collect(Collectors.toList()).toArray(new String[]{})
        );

        default_components.add(create_textfield("comment", new NotEmptyVerifier()), "hfill");
        default_components.add(create_label("Starter Countdown"), "br left");
        default_components.add(txt_starter_countdown, "left");
        default_components.add(create_label("Intro MP3"), "left");
        default_components.add(cmbIntroMusic, "left");
        default_components.add(create_label("Countdown to Resume"), "left");
        default_components.add(create_textfield("resume_countdown", new NumberVerifier(BigDecimal.ZERO, NumberVerifier.MAX, true)), "left");
        default_components.add(cbWait4Teams2BReady);
        default_components.add(cbSilentGame);


        //components.get("starter_countdown").add
        cmbIntroMusic.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (e.getItem().toString().equals("<none>")) return;
                txt_starter_countdown.setText("30");
            }
        });

        txt_starter_countdown.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!((JTextField) e.getSource()).getText().equals("30")) {
                    cmbIntroMusic.setSelectedItem("<none>");
                }
            }
        });

        JPanel panel1 = new JPanel();
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));

        //---- txtRedSpawn ----
        txtRedSpawn = new JTextField();
        txtRedSpawn.setText("test");
        txtRedSpawn.setFont(new Font(".SF NS", Font.PLAIN, 22));
        txtRedSpawn.setAlignmentX(0.5F);
        txtRedSpawn.setBackground(new Color(255, 0, 51));
        txtRedSpawn.setOpaque(true);
        txtRedSpawn.setForeground(new Color(255, 255, 51));
        txtRedSpawn.setHorizontalAlignment(SwingConstants.CENTER);
        panel1.add(txtRedSpawn);

        //---- btnSwitchSides ----
        btnSwitchSides = new JButton();
        btnSwitchSides.setText(null);
        btnSwitchSides.setIcon(new ImageIcon(getClass().getResource("/artwork/lc_arrowshapes.png")));
        btnSwitchSides.setToolTipText("switch sides");
        btnSwitchSides.addActionListener(e -> btnSwitchSides(e));
        panel1.add(btnSwitchSides);

        //---- txtBlueSpawn ----
        txtBlueSpawn = new JTextField();
        txtBlueSpawn.setText("test");
        txtBlueSpawn.setFont(new Font(".SF NS", Font.PLAIN, 22));
        txtBlueSpawn.setAlignmentX(0.5F);
        txtBlueSpawn.setBackground(new Color(51, 51, 255));
        txtBlueSpawn.setOpaque(true);
        txtBlueSpawn.setForeground(new Color(255, 255, 51));
        txtBlueSpawn.setHorizontalAlignment(SwingConstants.CENTER);
        panel1.add(txtBlueSpawn);

        default_components.add(panel1, "br hfill");
        default_components.add(new JSeparator(SwingConstants.HORIZONTAL), "br hfill");
    }

    protected JLabel create_label(String label) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(default_font);
        return lbl;
    }

    protected JTextField create_textfield(String key, InputVerifier inputVerifier) {
        JTextField txt = new JTextField();
        txt.setFont(default_font);
        txt.setInputVerifier(inputVerifier);
        components.put(key, txt);
        return txt;
    }

    protected JCheckBox create_checkbox(String key, String label) {
        JCheckBox cb = new JCheckBox(label);
        cb.setFont(default_font);
        components.put(key, cb);
        return cb;
    }

    public Optional<File> getFile() {
        return file;
    }

    public abstract String getMode();

    public void from_params_to_ui() {
        components.forEach((key, jComponent) -> {
            if (jComponent instanceof JTextComponent) ((JTextComponent) jComponent).setText(params.get(key).toString());
            if (jComponent instanceof JCheckBox) ((JCheckBox) jComponent).setSelected(params.optBoolean(key));
        });
        cbSilentGame.setSelected(params.getBoolean("silent_game"));

        cbWait4Teams2BReady.setSelected(params.getJSONObject("spawns").getBoolean("wait4teams2B_ready"));
        cmbIntroMusic.setSelectedItem(params.getJSONObject("spawns").getString("intro_mp3_file"));
        txt_starter_countdown.setText(Integer.toString(params.getJSONObject("spawns").getInt("starter_countdown")));

        //log.debug(params.getJSONObject("spawns").getJSONArray("teams").toString(4));

        params.getJSONObject("spawns").getJSONArray("teams").forEach(o -> {
            JSONObject team = (JSONObject) o;
            if (team.getString("role").equals("red_spawn"))
                txtRedSpawn.setText(to_string_segment_list(team.getJSONArray("agents")));
            if (team.getString("role").equals("blue_spawn"))
                txtBlueSpawn.setText(to_string_segment_list(team.getJSONArray("agents")));
        });
    }

    public void from_params_to_ui(JSONObject params) {
        if (params.has("game_state")) this.params = params;
        else load_defaults();
        from_params_to_ui();
    }

    public void from_ui_to_params() {
        params.clear();
        components.forEach((key, jComponent) -> {
            if (jComponent instanceof JTextComponent) params.put(key, ((JTextComponent) jComponent).getText());
            if (jComponent instanceof JCheckBox) params.put(key, ((JCheckBox) jComponent).isSelected());
        });

        params.put("silent_game", Boolean.toString(cbSilentGame.isSelected()));

        JSONObject spawns = new JSONObject();
        spawns.put("wait4teams2B_ready", Boolean.toString(cbWait4Teams2BReady.isSelected()));
        spawns.put("intro_mp3_file", cmbIntroMusic.getSelectedItem().toString());
        spawns.put("starter_countdown", txt_starter_countdown.getText());
        spawns.put("respawn_time", 0); // maybe overwritten by some children classes

        JSONArray teams = new JSONArray();

        JSONObject redfor = new JSONObject();
        redfor.put("role", "red_spawn");
        redfor.put("led", "red");
        redfor.put("name", "RedFor");
        redfor.put("agents", from_string_segment_list(txtRedSpawn.getText()));
        teams.put(redfor);

        JSONObject blufor = new JSONObject();
        blufor.put("role", "blue_spawn");
        blufor.put("led", "blu");
        blufor.put("name", "BluFor");
        blufor.put("agents", from_string_segment_list(txtBlueSpawn.getText()));
        teams.put(blufor);

        spawns.put("teams", teams);
        params.put("spawns", spawns);

        //params.put("intro_mp3_file", cmbIntroMusic.getSelectedItem().toString());
//        return params;
    }


    public void load_defaults() {
        File myFile = FileUtils.getFile(System.getProperty("workspace"), getMode(), "default.json");
        file = Optional.empty();
        try {
            if (myFile.exists()) {
                params = new JSONObject(FileUtils.readFileToString(myFile, StandardCharsets.UTF_8));
            } else {
                params = new JSONObject(Resources.toString(Resources.getResource("defaults/" + getMode() + ".json"), Charset.defaultCharset()));
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    public Optional<File> load_file() {
        Optional<File> myFile = choose_file(false);
        if (myFile.isEmpty()) return file;
        file = myFile;
        try {
            params = new JSONObject(FileUtils.readFileToString(myFile.get(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            myFile = Optional.empty();
        }
        return myFile;
    }

    public void save_file() throws IOException {
        if (params.isEmpty()) return;
        if (file.isEmpty()) file = choose_file(true);
        if (file.isEmpty()) return;
        from_ui_to_params();
        FileUtils.writeStringToFile(file.get(), params.toString(4), StandardCharsets.UTF_8);
    }

    protected Optional<File> choose_file(boolean save) {
        JFileChooser fileChooser = new JFileChooser(new File(System.getProperty("workspace") + File.separator + getMode()));
        int result;// = JFileChooser.CANCEL_OPTION;
        if (save) result = fileChooser.showSaveDialog(this);
        else result = fileChooser.showOpenDialog(this);
        Optional<File> myFile = Optional.empty();
        if (result == JFileChooser.APPROVE_OPTION) {
            File chosen = fileChooser.getSelectedFile();
            String filePath = chosen.getAbsolutePath();
            if (!filePath.endsWith(".json")) {
                chosen = new File(filePath + ".json");
            }
            myFile = Optional.of(chosen);
        }
        return myFile;
    }

    /**
     * converts a jsonarray to a comma delimited string
     *
     * @param jsonArray
     * @return
     */
    protected String to_string_list(JSONArray jsonArray) {
        return jsonArray.toList().stream().map(o -> o.toString().trim()).collect(Collectors.joining(","));
    }

    protected String to_string_segment_list(JSONArray jsonArray) {
        StringBuffer segment_list = new StringBuffer();
        jsonArray.forEach(o -> {
            JSONArray segment = (JSONArray) o;
            segment_list.append(to_string_list(segment));
            segment_list.append(";");
        });
        return StringUtils.stripEnd(segment_list.toString(), ";");
    }

    /**
     * create a cascaded JSONArray out of a String like ag01,ag02;ag03,ag04;ag05,ag06
     *
     * @param list
     * @return
     */
    protected JSONArray from_string_segment_list(String list) {
        JSONArray outer = new JSONArray();
        Collections.list(new StringTokenizer(list, "\n;")).stream().map(token -> ((String) token).trim()).forEach(s -> {
            JSONArray inner = new JSONArray();
            Collections.list(new StringTokenizer(s, ",")).stream().map(token -> ((String) token).trim()).forEach(s1 -> inner.put(s1));
            outer.put(inner);
        });
        return outer;
    }

    // todo: verifier


    protected JSONArray from_string_list(String list) {
        return new JSONArray(
                Collections.list(new StringTokenizer(list, "\n,")).stream()
                        .map(token -> ((String) token).trim())
                        .sorted()
                        .collect(Collectors.toList()));
    }

    /**
     * this method is implemented by the children classes to provide a screen friendly representation of the current
     * score / situation.
     *
     * @param game_state the game status as provided by the server
     * @return the current score in HTML
     */
    public abstract String get_score_as_html(JSONObject game_state);

    abstract String get_in_game_event_description(JSONObject event);

    public Optional<ZeusDialog> get_zeus() {
        return Optional.empty();
    }

    protected String generate_table_for_events(JSONArray events) {
        StringBuffer buffer = new StringBuffer();

        int max_events = events.length();
        for (int e_index = max_events - 1; e_index >= 0; e_index--) {
            JSONObject event = events.getJSONObject(e_index);
            buffer.append(HTML.table_tr(
                    HTML.table_td(dtf.format(JavaTimeConverter.from_iso8601(event.getString("pit")))) +
                            HTML.table_td(get_in_game_event_description(event.getJSONObject("event"))) +
                            HTML.table_td(event.getString("new_state"))
            ));
        }

        return HTML.table(
                HTML.table_tr(
                        HTML.table_th("Timestamp") +
                                HTML.table_th("Event") +
                                HTML.table_th("State")
                ),
                buffer.toString(), "1"); //"<table><thead><tr><th>Timestamp</th><th>Event</th><th>State</th></tr></thead><tbody>";
    }

    protected void load_default_css() {
        File myFile = FileUtils.getFile(System.getProperty("workspace"), "default.css");
        String c;
        try {
            if (myFile.exists()) {
                c = FileUtils.readFileToString(myFile, StandardCharsets.UTF_8);
            } else {
                c = Resources.toString(Resources.getResource("defaults/default.css"), Charset.defaultCharset());
            }
            CSS = "<style type=\"text/css\" media=\"all\">\n" + c + "</style>";
        } catch (Exception e) {
            log.error(e);
            CSS = "";
        }
    }

    private void btnSwitchSides(ActionEvent e) {
        String a = txtBlueSpawn.getText();
        txtBlueSpawn.setText(txtRedSpawn.getText());
        txtRedSpawn.setText(a);
    }

    public JSONObject getParams() {
        return params;
    }
}
