package de.flashheart.rlgrc.ui.events;

import java.time.LocalDateTime;
import java.util.EventListener;

public interface SpawnListener extends EventListener {
    void handleRespawnEvent(SpawnEvent event);
    void createRunGameJob(LocalDateTime start_time);
}
