package de.flashheart.rlgrc.misc;


import com.google.common.io.Resources;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
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
        // load defaults
        JSONObject defaults = new JSONObject(Resources.toString(Resources.getResource("defaults/configs.json"), Charset.defaultCharset()));
        defaults.put("uuid", UUID.randomUUID().toString());
        JSONObject user_configs = new JSONObject(FileUtils.readFileToString(config_file, Charset.defaultCharset()));
        configs = deepMerge(user_configs, defaults);
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

    /**
     * Merge "source" into "target". If fields have equal name, merge them recursively.
     * see <a href="https://stackoverflow.com/a/15070484">origin</a>
     *
     * @return the merged object (target).
     */
    private JSONObject deepMerge(JSONObject source, JSONObject target) throws JSONException {
        for (String key : JSONObject.getNames(source)) {
            Object value = source.get(key);
            if (!target.has(key)) {
                // new value for "key":
                target.put(key, value);
            } else {
                // existing value for "key" - recursively deep merge:
                if (value instanceof JSONObject) {
                    JSONObject valueJson = (JSONObject) value;
                    deepMerge(valueJson, target.getJSONObject(key));
                } else {
                    target.put(key, value);
                }
            }
        }
        return target;
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
