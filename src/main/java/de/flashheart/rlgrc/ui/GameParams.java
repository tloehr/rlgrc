package de.flashheart.rlgrc.ui;

import com.google.common.io.Resources;
import com.mchange.v2.lang.StringUtils;
import de.flashheart.rlgrc.misc.*;
import de.flashheart.rlgrc.ui.zeus.ZeusDialog;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
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
import java.util.stream.Collectors;

@Log4j2
public abstract class GameParams extends JPanel {
    private final JSONConfigs configs;
    protected JSONObject params;
    protected Optional<File> file;
    protected JPanel default_components;
    protected JCheckBox cbWait4Teams2BReady;
    protected JComboBox<String> cmbIntroMusic;
    private DateTimeFormatter dtf;
    protected String CSS = "";
    protected Font default_font = new Font(".SF NS Text", Font.PLAIN, 14);
    protected Font large_font = FrameMain.MY_FONT;
    protected HashMap<String, JTextComponent> components;

    public GameParams(JSONConfigs configs) {
        super();
        components = new HashMap<>();
        this.configs = configs;
        load_default_css();
        dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
        file = Optional.empty();
        init_default_components();
    }

    private void init_default_components() {
        default_components = new JPanel(new RiverLayout(5, 5));

        cbWait4Teams2BReady = new JCheckBox("Wait for Teams");
//        cmbIntroMusic = new JComboBox<>(StringUtils.splitCommaSeparated(configs.get(Configs.INTRO_MP3_FILES), true));

        cmbIntroMusic = new JComboBox<>(configs.getConfigs()
                .getJSONObject("audio")
                .getJSONArray("intro")
                .toList().stream().sorted().collect(Collectors.toList()).toArray(new String[]{})
        );

        cbWait4Teams2BReady.setFont(default_font);
        default_components.add(create_textfield("comment", new NotEmptyVerifier()), "hfill");
        default_components.add(create_label("Starter Countdown"), "br left");
        default_components.add(create_textfield("starter_countdown", new NumberVerifier(BigDecimal.ZERO, NumberVerifier.MAX, true)), "left");
        default_components.add(create_label("Intro MP3"), "left");
        default_components.add(cmbIntroMusic, "left");
        default_components.add(create_label("Countdown to Resume"), "left");
        default_components.add(create_textfield("resume_countdown", new NumberVerifier(BigDecimal.ZERO, NumberVerifier.MAX, true)), "left");
        default_components.add(cbWait4Teams2BReady);
        default_components.add(new JSeparator(SwingConstants.HORIZONTAL), "br hfill");

        //components.get("starter_countdown").add
        cmbIntroMusic.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (e.getItem().toString().equals("<none>")) return;
                components.get("starter_countdown").setText("30");
            }
        });

        components.get("starter_countdown").addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!((JTextField) e.getSource()).getText().equals("30")) {
                    cmbIntroMusic.setSelectedItem("<none>");
                }
            }
        });

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

    protected String getFilename() {
        return file.isEmpty() ? "no file" : file.get().getPath();
    }

    abstract String getMode();

    protected void set_parameters() {
        components.forEach((key, jTextComponent) -> jTextComponent.setText(params.get(key).toString()));
        cbWait4Teams2BReady.setSelected(params.getBoolean("wait4teams2B_ready"));
        cmbIntroMusic.setSelectedItem(params.getString("intro_mp3_file"));
    }

    protected void set_parameters(JSONObject params) {
        this.params = params;
        set_parameters();
    }

    protected JSONObject read_parameters() {
        params.clear();
        components.forEach((key, jTextComponent) -> params.put(key, jTextComponent.getText()));
        params.put("wait4teams2B_ready", Boolean.toString(cbWait4Teams2BReady.isSelected()));
        params.put("intro_mp3_file", cmbIntroMusic.getSelectedItem().toString());
        return params;
    }

    protected void load_defaults() {
        File myFile = FileUtils.getFile(System.getProperty("workspace"), getMode(), "default.json");
        file = Optional.empty();
        try {
            if (myFile.exists()) {
                params = new JSONObject(FileUtils.readFileToString(myFile, StandardCharsets.UTF_8));
            } else {
                params = new JSONObject(Resources.toString(Resources.getResource("defaults/" + getMode() + ".json"), Charset.defaultCharset()));
            }
            set_parameters();
        } catch (Exception e) {
            log.error(e);
        }
    }

    protected Optional<File> load_file() {
        Optional<File> myFile = choose_file(false);
        if (myFile.isEmpty()) return file;
        try {
            params = new JSONObject(FileUtils.readFileToString(myFile.get(), StandardCharsets.UTF_8));
            set_parameters();
        } catch (IOException e) {
            myFile = Optional.empty();
        }
        return myFile;
    }

    void save_file() throws IOException {
        if (params.isEmpty()) return;
        if (file.isEmpty()) file = choose_file(true);
        if (file.isEmpty()) return;
        FileUtils.writeStringToFile(file.get(), read_parameters().toString(4), StandardCharsets.UTF_8);
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

    protected JSONArray to_jsonarray(String list) {
        return new JSONArray(Collections.list(new StringTokenizer(list, "\n,")).stream().map(token -> (String) token).collect(Collectors.toList()));
    }

    /**
     * this method is implemented by the children classes to provide a screen friendly representation of the current
     * score / situation.
     *
     * @param game_state the game status as provided by the server
     * @return the current score in HTML
     */
    abstract String get_score_as_html(JSONObject game_state);

    abstract String get_in_game_event_description(JSONObject event);

    Optional<ZeusDialog> get_zeus() {
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

}
