package de.flashheart.rlgrc.networking;

import de.flashheart.rlgrc.ui.events.SpawnEvent;

import java.time.LocalDateTime;
import java.util.EventListener;

public interface RestResponseListener extends EventListener {
    void on_response(RestResponseEvent event);
}
