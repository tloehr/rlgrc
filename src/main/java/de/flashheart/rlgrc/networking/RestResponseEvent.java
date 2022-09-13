package de.flashheart.rlgrc.networking;

import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@ToString
@Getter
public class RestResponseEvent {
    Optional<String> details;
    Optional<Response> response;
    Optional<Exception> exception;

    public RestResponseEvent(Response response, String details) {
        this.details = Optional.of(details);
        this.response = Optional.of(response);
        this.exception = Optional.empty();
    }

    public RestResponseEvent(Response response) {
        this.details = Optional.empty();
        this.response = Optional.of(response);
        this.exception = Optional.empty();
    }

    public RestResponseEvent(Exception exception) {
        this.details = Optional.empty();
        this.response = Optional.empty();
        this.exception = Optional.of(exception);
    }
}

