package de.flashheart.rlgrc.networking;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

@Log4j2
public class RestHandler {
    private final Runnable on_connect;
    private final Runnable on_disconnect;
    private boolean connected;
    private Client client;
    private String uri;
    private ArrayList<RestResponseListener> restResponseListeners;
    private ArrayList<LoggableEventListener> loggableEventListeners;

    public RestHandler(Runnable on_connect, Runnable on_disconnect) {
        this.on_connect = on_connect;
        this.on_disconnect = on_disconnect;
        restResponseListeners = new ArrayList<>();
        loggableEventListeners = new ArrayList<>();
        connected = false;
        client = ClientBuilder.newClient();
    }


    private void fireResponseReceived(RestResponseEvent event) {
        restResponseListeners.forEach(restResponseListener -> restResponseListener.on_response(event));
        log.trace(event);
    }

    public void addRestResponseListener(RestResponseListener toAdd) {
        restResponseListeners.add(toAdd);
    }

    public void addLoggableEventListener(LoggableEventListener toAdd) {
        loggableEventListeners.add(toAdd);
    }

    private void fireLoggableEvent(LoggableEvent event) {
        loggableEventListeners.forEach(loggableEventListener -> loggableEventListener.on_event(event));
        log.trace(event);
    }

    public void connect(Object server_object) {
        if (connected) return;
        try {
            uri = server_object.toString().trim();
            // just checking - will fail if nobody is answering
            int max_number_of_games = get("system/get_max_number_of_games").getInt("max_number_of_games");
            connected = true;
            on_connect.run();
        } catch (JSONException e) {
            log.error(e);
            disconnect();
        }
    }

    public void disconnect() {
        if (!connected) return;

        connected = false;
        uri = "";
        on_disconnect.run();

    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * conveniance method
     *
     * @param uri
     * @param body
     * @param id
     * @return
     */
    public JSONObject post(String uri, String body, String id) {
        Properties properties = new Properties();
        properties.put("id", id);
        return post(uri, body, properties);
    }

    /**
     * conveniance method
     *
     * @param uri
     * @param id
     * @return
     */
    public JSONObject post(String uri, String id) {
        return post(uri, "{}", id);
    }

    /**
     * posts a REST request.
     *
     * @param body
     * @param params
     * @return
     */
    public JSONObject post(String endpoint, String body, Properties params) throws IllegalStateException {
        JSONObject json = new JSONObject();

        try {
            WebTarget target = client.target(uri + "/api/" + endpoint);

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
                fireResponseReceived(new RestResponseEvent(response));
            } else {
                String err_message = json.optJSONObject("targetException").optString("message");
                err_message += json.optString("message");
                fireResponseReceived(new RestResponseEvent(response, err_message));
            }

            fireLoggableEvent(new LoggableEvent("\n\n" + response.getStatus() + " " + response.getStatusInfo().toString() + "\n" + json.toString(4)));
            response.close();
//            if (
//                    response.getStatusInfo().getFamily().equals(Response.Status.Family.CLIENT_ERROR)
//            ) throw new IllegalStateException(entity);

            //connect();
        } catch (Exception connectException) {
            fireLoggableEvent(new LoggableEvent(connectException.getMessage()));
            fireResponseReceived(new RestResponseEvent(connectException));
            disconnect();
        }
        return json;
    }


    public JSONObject get(String uri, String id) {
        JSONObject json;
        try {
            Response response = client
                    .target(this.uri + "/api/" + uri)
                    .queryParam("id", id)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            json = new JSONObject(response.readEntity(String.class));
            fireResponseReceived(new RestResponseEvent(response));
            fireLoggableEvent(new LoggableEvent("\n\n" + response.getStatus() + " " + response.getStatusInfo().toString() + "\n" + json.toString(4)));
            response.close();
            //connect();
        } catch (Exception connectException) {
            fireLoggableEvent(new LoggableEvent(connectException.getMessage()));
            fireResponseReceived(new RestResponseEvent(connectException));
            disconnect();
            json = new JSONObject();
        }
        return json;
    }

    public JSONObject get(String uri) {
        JSONObject json;
        try {
            Response response = client
                    .target(this.uri + "/api/" + uri)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            json = new JSONObject(response.readEntity(String.class));
            fireResponseReceived(new RestResponseEvent(response));
            response.close();
        } catch (Exception connectException) {
            fireLoggableEvent(new LoggableEvent(connectException.getMessage()));
            fireResponseReceived(new RestResponseEvent(connectException));
            disconnect();
            json = new JSONObject();
        }
        return json;
    }

    public String getUri() {
        return uri;
    }
}
