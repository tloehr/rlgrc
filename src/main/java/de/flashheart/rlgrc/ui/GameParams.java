package de.flashheart.rlgrc.ui;

import org.json.JSONObject;

public interface GameParams {

    String getMode();
    void set_parameters(JSONObject params);
    JSONObject get_parameters();

}
