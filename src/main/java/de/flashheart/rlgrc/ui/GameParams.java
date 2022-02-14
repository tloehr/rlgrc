package de.flashheart.rlgrc.ui;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.util.Optional;

@Log4j2
public abstract class GameParams extends JPanel {
    JSONObject params;
    Optional<File> file;

    public GameParams() {
        super();
        load_defaults();
        file = Optional.empty();
    }

    abstract String getMode();

    abstract void set_parameters();

    abstract JSONObject read_parameters(String comment);

    void load_defaults() {
        StringBuffer stringBuffer = new StringBuffer();
        InputStream in = getClass().getResourceAsStream("/defaults/" + getMode() + ".json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        reader.lines().forEach(s -> stringBuffer.append(s));
        params = new JSONObject(stringBuffer.toString());
        set_parameters();
    }

    void load_file() throws IOException {
        file = choose_file(false);
        if (file.isEmpty()) return;
        params = new JSONObject(FileUtils.readFileToString(file.get()));
        set_parameters();
    }

    void save_file() throws IOException {
        if (params.isEmpty()) return;
        if (file.isEmpty()) file = choose_file(true);
        if (file.isEmpty()) return;
        FileUtils.writeStringToFile(file.get(), params.toString(4));
    }


    Optional<File> choose_file(boolean save) {
        JFileChooser fileChooser = new JFileChooser(new File(System.getProperty("workspace") + File.separator + getMode()));
        int result;// = JFileChooser.CANCEL_OPTION;
        if (save) result = fileChooser.showSaveDialog(this);
        else result = fileChooser.showOpenDialog(this);
        Optional<File> file = Optional.empty();
        if (result == JFileChooser.APPROVE_OPTION) {
            file = Optional.of(fileChooser.getSelectedFile());
        }
        return file;
    }

}
