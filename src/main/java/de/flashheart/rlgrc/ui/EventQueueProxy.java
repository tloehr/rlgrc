package de.flashheart.rlgrc.ui;

import lombok.extern.log4j.Log4j2;

import java.awt.*;

@Log4j2
public class EventQueueProxy extends EventQueue {

    protected void dispatchEvent(AWTEvent newEvent) {
        try {
            super.dispatchEvent(newEvent);
        } catch (Throwable t) {
            t.printStackTrace();
            String message = t.getMessage();

            if (message == null || message.length() == 0) {
                message = "Fatal: " + t.getClass();
            }

            log.error(message);
        }
    }
}