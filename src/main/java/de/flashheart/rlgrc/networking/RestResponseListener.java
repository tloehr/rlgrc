package de.flashheart.rlgrc.networking;

import java.util.EventListener;

public interface RestResponseListener extends EventListener {
    void on_response(RestResponseEvent event);
}
