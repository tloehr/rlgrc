package de.flashheart.rlgrc.misc;

import de.flashheart.rlgrc.App;

import java.io.IOException;

public class Configs extends AbstractConfigs {
    public static final String REST_URI = "rest_uri";
    public static final String LAST_GAME_ID = "last_game_id";

    public Configs() throws IOException {
        super(System.getProperties().getProperty("workspace"));
    }

    @Override
    public String getProjectName() {
        return App.PROJECT;
    }

    /**
     * Das System erwartet dass es zu jedem Schlüssel einen Wert gibt. Die Defaults verhindern dass NPEs bei neuen
     * Installationen auftreten.
     */
    @Override
    public void loadDefaults() {
        configs.setProperty(REST_URI, "http://localhost:8090");
        configs.setProperty(LAST_GAME_ID, "g1");

    }

}
