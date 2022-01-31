package de.flashheart.rlgrc.games;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Conquest {
    String comment;
    String classname = "de.flashheart.rlg.commander.games.Conquest";
    Agents agents;
    int respawn_tickets;
    int not_bleeding_before_cps;
    double start_bleed_interval;
    double end_bleed_interval;
    double ticket_price_for_respawn;

    public Conquest(String comment, Agents agents, int respawn_tickets, int not_bleeding_before_cps, double start_bleed_interval, double end_bleed_interval, double ticket_price_for_respawn) {
        this.comment = comment;
        this.agents = agents;
        this.respawn_tickets = respawn_tickets;
        this.not_bleeding_before_cps = not_bleeding_before_cps;
        this.start_bleed_interval = start_bleed_interval;
        this.end_bleed_interval = end_bleed_interval;
        this.ticket_price_for_respawn = ticket_price_for_respawn;
    }
}
