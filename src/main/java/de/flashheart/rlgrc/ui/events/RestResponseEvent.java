package de.flashheart.rlgrc.ui.events;

import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class RestResponseEvent {
    Response response;
}

