package de.flashheart.rlgrc.ui;

import com.google.common.io.Resources;
import de.flashheart.rlgrc.misc.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

@Log4j2
public abstract class GameParams extends JPanel {
    protected JSONObject params;
    protected Optional<File> file;
    protected JPanel default_components;
    protected JTextField txtComment, txtStarterCountdown, txtResumeCountdown;
    protected JCheckBox cbWait4Teams2BReady;
    private DateTimeFormatter dtf;
    protected String CSS = "";

    public GameParams() {
        super();
        load_default_css();
        dtf = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
        file = Optional.empty();
        init_default_components();
    }

    private void init_default_components() {
        default_components = new JPanel(new RiverLayout(5, 5));
        txtComment = new JTextField();
        txtStarterCountdown = new JTextField();
        txtResumeCountdown = new JTextField();
        cbWait4Teams2BReady = new JCheckBox("Wait for Teams to REPORT-IN as ready");
        txtComment.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
        txtStarterCountdown.setFont(new Font(".SF NS Text", Font.PLAIN, 14));
        txtResumeCountdown.setFont(new Font(".SF NS Text", Font.PLAIN, 14));
        cbWait4Teams2BReady.setFont(new Font(".SF NS Text", Font.PLAIN, 14));
        txtComment.setInputVerifier(new NotEmptyVerifier());
        txtStarterCountdown.setInputVerifier(new NumberVerifier(BigDecimal.ZERO, NumberVerifier.MAX, true));
        txtResumeCountdown.setInputVerifier(new NumberVerifier(BigDecimal.ZERO, NumberVerifier.MAX, true));
        default_components.add(txtComment, "hfill");
        default_components.add(new JLabel("Countdown to Start"), "br left");
        default_components.add(txtStarterCountdown);
        default_components.add(new JLabel("Countdown to Resume"));
        default_components.add(txtResumeCountdown);
        default_components.add(cbWait4Teams2BReady);
        default_components.add(new JSeparator(SwingConstants.HORIZONTAL), "br hfill");
    }

    protected String getFilename() {
        return file.isEmpty() ? "no file" : file.get().getPath();
    }

    abstract String getMode();

    protected void set_parameters() {
        txtComment.setText(params.getString("comment"));
        txtStarterCountdown.setText(params.get("starter_countdown").toString());
        txtResumeCountdown.setText(params.get("resume_countdown").toString());
        cbWait4Teams2BReady.setSelected(params.getBoolean("wait4teams2B_ready"));
    }

    protected void set_parameters(JSONObject params) {
        this.params = params;
        set_parameters();
    }

    protected JSONObject read_parameters() {
        params.clear();
        params.put("comment", txtComment.getText());
        params.put("starter_countdown", txtStarterCountdown.getText());
        params.put("resume_countdown", txtResumeCountdown.getText());
        params.put("wait4teams2B_ready", Boolean.toString(cbWait4Teams2BReady.isSelected()));
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

    protected String to_string_list(JSONArray jsonArray) {
        return jsonArray.toList().toString().replaceAll("\\[|\\]| ", "").replaceAll(",", "\n");
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

    protected String generate_table_for_events(JSONArray events) {
        StringBuffer buffer = new StringBuffer();

        int max_events = events.length();
        for (int e_index = max_events - 1; e_index >= 0; e_index--) {
            JSONObject event = events.getJSONObject(e_index);
            buffer.append(HTML.table_tr(
                    HTML.table_td(dtf.format(JavaTimeConverter.from_iso8601(event.getString("pit")))) +
                            HTML.table_td(event.getString("event")) +
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
