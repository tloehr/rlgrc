package de.flashheart.rlgrc.networking;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.util.Map;
import java.util.Properties;

@Log4j2
public class RestHandler {
    private boolean connected;

    public RestHandler() {
        connected = false;
    }

    private void connect() {
        if (connected) return;
        try {
            int max_number_of_games = get("system/get_max_number_of_games").getInt("max_number_of_games");
            connected = true;
            for (int i = 0; i < max_number_of_games; i++) cmbGameSlots.addItem(i + 1);
            current_state = get("game/status", current_game_id()); // just in case a game is already running
            set_gui_to_situation();
        } catch (JSONException e) {
            log.error(e);
            disconnect();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private JSONObject post(String uri, String id) {
        return post(uri, "{}", id);
    }

    /**
     * posts a REST request.
     *
     * @param uri
     * @param body
     * @param params
     * @return
     */
    private JSONObject post(String uri, String body, Properties params) throws IllegalStateException {
        JSONObject json = new JSONObject();

        try {
            WebTarget target = client
                    .target(txtURI.getSelectedItem().toString().trim() + "/api/" + uri);

            for (Map.Entry entry : params.entrySet()) {
                target = target.queryParam(entry.getKey().toString(), entry.getValue());
            }

            Response response = target
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body));

            String entity = response.readEntity(String.class);
            if (entity.isEmpty()) json = new JSONObject();
            else json = new JSONObject(entity);

            if (response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                set_response_status(response, null);
            } else {
                String err_message = json.optJSONObject("targetException").optString("message");
                err_message += json.optString("message");
                set_response_status(response, err_message);
            }


            pnlServer.addLog("\n\n" + response.getStatus() + " " + response.getStatusInfo().toString() + "\n" + json.toString(4));
            response.close();
//            if (
//                    response.getStatusInfo().getFamily().equals(Response.Status.Family.CLIENT_ERROR)
//            ) throw new IllegalStateException(entity);

            //connect();
        } catch (Exception connectException) {
            pnlServer.addLog(connectException.getMessage());
            set_response_status(connectException);
            disconnect();
        }
        return json;
    }


    private JSONObject get(String uri, String id) {
        JSONObject json;
        try {
            Response response = client
                    .target(txtURI.getSelectedItem().toString().trim() + "/api/" + uri)
                    .queryParam("id", id)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            json = new JSONObject(response.readEntity(String.class));
            set_response_status(response, null);
            pnlServer.addLog("\n\n" + response.getStatus() + " " + response.getStatusInfo().toString() + "\n" + json.toString(4));
            response.close();
            //connect();
        } catch (Exception connectException) {
            pnlServer.addLog(connectException.getMessage());
            set_response_status(connectException);
            disconnect();
            json = new JSONObject();
        }
        return json;
    }

    private JSONObject get(String uri) {
        JSONObject json;
        try {
            Response response = client
                    .target(txtURI.getSelectedItem().toString().trim() + "/api/" + uri)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            json = new JSONObject(response.readEntity(String.class));
            set_response_status(response, null);
            response.close();
        } catch (Exception connectException) {
            pnlServer.addLog(connectException.getMessage());
            set_response_status(connectException);
            disconnect();
            json = new JSONObject();
        }
        return json;
    }

    void set_response_status(Response response, String details) {
        String icon = "/artwork/ledyellow.png";
        if (response.getStatusInfo().getFamily().name().equalsIgnoreCase("CLIENT_ERROR") || response.getStatusInfo().getFamily().name().equalsIgnoreCase("SERVER_ERROR"))
            icon = "/artwork/ledred.png";
        if (response.getStatusInfo().getFamily().name().equalsIgnoreCase("SUCCESSFUL"))
            icon = "/artwork/ledgreen.png";
        lblResponse.setIcon(new ImageIcon(getClass().getResource(icon)));
        lblResponse.setText(response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase());
        lblResponse.setToolTipText(details);
    }

    void set_response_status(Exception exception) {
        String icon = "/artwork/ledred.png";
        lblResponse.setIcon(new ImageIcon(getClass().getResource(icon)));
        lblResponse.setText(StringUtils.left(exception.getMessage(), 70));
        lblResponse.setToolTipText(exception.toString());
    }


}
