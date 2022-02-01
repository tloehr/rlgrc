package de.flashheart.rlgrc.games;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ConquestParams extends GameParams {
    BigDecimal respawn_tickets;
    BigDecimal not_bleeding_before_cps;
    BigDecimal start_bleed_interval;
    BigDecimal end_bleed_interval;
    BigDecimal ticket_price_for_respawn;

    public ConquestParams(int respawn_tickets, int not_bleeding_before_cps, double start_bleed_interval, double end_bleed_interval, double ticket_price_for_respawn) {
        super("7CPs Conquest", "de.flashheart.rlg.commander.games.Conquest");
        this.respawn_tickets = BigDecimal.valueOf(respawn_tickets);
        this.not_bleeding_before_cps = BigDecimal.valueOf(not_bleeding_before_cps);
        this.start_bleed_interval = BigDecimal.valueOf(start_bleed_interval);
        this.end_bleed_interval = BigDecimal.valueOf(end_bleed_interval);
        this.ticket_price_for_respawn = BigDecimal.valueOf(ticket_price_for_respawn);
    }

    @Override
    public String toString() {
        return "ConquestParams{" +
                "respawn_tickets=" + respawn_tickets +
                ", not_bleeding_before_cps=" + not_bleeding_before_cps +
                ", start_bleed_interval=" + start_bleed_interval +
                ", end_bleed_interval=" + end_bleed_interval +
                ", ticket_price_for_respawn=" + ticket_price_for_respawn +
                '}' + super.toString();
    }
}
