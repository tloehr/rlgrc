package de.flashheart.rlgrc.games;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Agents {
    String[] capture_points;
    String[] red_spawn;
    String[] blue_spawn;
    String[] spawns;
    String[] sirens;
}
