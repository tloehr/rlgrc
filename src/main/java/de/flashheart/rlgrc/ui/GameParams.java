package de.flashheart.rlgrc.ui;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

@Log4j2
public abstract class GameParams extends JPanel {
    JSONObject params;
    Optional<File> file;

    public GameParams() {
        super();
        file = Optional.empty();
    }

    abstract String getMode();

    abstract void set_parameters();

    void set_parameters(JSONObject params){
        this.params = params;
        set_parameters();
    }

    abstract JSONObject read_parameters();

    void load_defaults() {
        StringBuffer stringBuffer = new StringBuffer();
        InputStream in = getClass().getResourceAsStream("/defaults/" + getMode() + ".json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        reader.lines().forEach(s -> stringBuffer.append(s));
        params = new JSONObject(stringBuffer.toString());
        set_parameters();
    }

    File load_file() throws IOException {
        file = choose_file(false);
        if (file.isEmpty()) return null;
        params = new JSONObject(FileUtils.readFileToString(file.get()));
        set_parameters();
        return file.get();
    }

    File save_file() throws IOException {
        if (params.isEmpty()) return null;
        if (file.isEmpty()) file = choose_file(true);
        if (file.isEmpty()) return null;
        FileUtils.writeStringToFile(file.get(), params.toString(4));
        return file.get();
    }

    Optional<File> choose_file(boolean save) {
        JFileChooser fileChooser = new JFileChooser(new File(System.getProperty("workspace") + File.separator + getMode()));
        int result;// = JFileChooser.CANCEL_OPTION;
        if (save) result = fileChooser.showSaveDialog(this);
        else result = fileChooser.showOpenDialog(this);
        Optional<File> file = Optional.empty();
        if (result == JFileChooser.APPROVE_OPTION) {
            File chosen = fileChooser.getSelectedFile();
            String filePath = chosen.getAbsolutePath();
            if(!filePath.endsWith(".json")) {
                chosen = new File(filePath + ".json");
            }
            file = Optional.of(chosen);
        }
        return file;
    }

    String to_string_list(JSONArray jsonArray) {
        return jsonArray.toList().toString().replaceAll("\\[|\\]| ", "").replaceAll(",", "\n");
    }

    JSONArray to_jsonarray(String list) {
        return new JSONArray(Collections.list(new StringTokenizer(list, "\n,")).stream().map(token -> (String) token).collect(Collectors.toList()));
    }

}
