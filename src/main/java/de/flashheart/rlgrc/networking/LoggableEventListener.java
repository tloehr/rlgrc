package de.flashheart.rlgrc.networking;

import java.util.EventListener;

public interface LoggableEventListener extends EventListener {
    void on_event(LoggableEvent event);
}
