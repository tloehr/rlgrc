package de.flashheart.rlgrc.misc;


import com.google.common.io.Resources;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Log4j2
public class JSONConfigs {
    protected final JSONObject configs;
    protected final String CONFIGFILE;
    protected final String WORKSPACE;
    protected Properties buildProperties;


    public JSONConfigs(String workspace) throws IOException {
        this.WORKSPACE = workspace;
        this.CONFIGFILE = WORKSPACE + File.separator + "rlgrc.json";
        buildProperties = new Properties(); // inhalte der build.properties (von Maven)
        loadBuildContext();
        File config_file = new File(CONFIGFILE);
        config_file.createNewFile();
        String string_user_configs = FileUtils.readFileToString(config_file, Charset.defaultCharset());
        // load defaults first, then overwrite it with
        // the user_settings - where necessary
        JSONObject defaults = new JSONObject(Resources.toString(Resources.getResource("defaults/rlgrc.json"), Charset.defaultCharset()));
        defaults.put("uuid", UUID.randomUUID().toString());
        JSONObject user_configs = new JSONObject(string_user_configs.isEmpty() ? "{}" : string_user_configs);
        Map combined = defaults.toMap();
        combined.putAll(user_configs.toMap());

        configs = new JSONObject(combined);
        saveConfigs();
    }

    private void loadBuildContext() throws IOException {
        InputStream in2 = this.getClass().getResourceAsStream("/build.properties");
        buildProperties.load(in2);
        in2.close();
        if (buildProperties.containsKey("timestamp")) {
            SimpleDateFormat sdfmt = new SimpleDateFormat("yyMMddHHmm");
            //System.out.println(sdfmt.format(new Date())); // Mittwoch, 21. März 2007 09:14
            Date buildDate = new Date(Long.parseLong(buildProperties.getProperty("timestamp")));
            buildProperties.put("buildDate", sdfmt.format(buildDate));
        }
    }

    public void saveConfigs() {
        try {
            configs.put("comment", "rlgrc v" + buildProperties.getProperty("my.version") + " b" + buildProperties.getProperty("buildNumber"));
            configs.put("timestamp", JavaTimeConverter.to_iso8601(LocalDateTime.now()));
            FileUtils.writeStringToFile(new File(CONFIGFILE), configs.toString(4), Charset.defaultCharset());
        } catch (Exception ex) {
            log.error(ex);
            System.exit(0);
        }
    }

    public Properties getBuildProperties() {
        return buildProperties;
    }

    public JSONObject getConfigs() {
        return configs;
    }
}
