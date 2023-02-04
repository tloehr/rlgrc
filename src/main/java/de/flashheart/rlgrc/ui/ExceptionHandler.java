package de.flashheart.rlgrc.ui;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExceptionHandler implements Thread.UncaughtExceptionHandler{
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error(e.getMessage());
        e.printStackTrace();
    }
}
