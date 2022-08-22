package de.flashheart.rlgrc.ui.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class SpawnEvent {
    String role;
    String agent;
    String message;
}

