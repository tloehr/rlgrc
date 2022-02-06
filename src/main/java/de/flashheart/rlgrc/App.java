package de.flashheart.rlgrc;

import com.bulenkov.darcula.DarculaLaf;
import de.flashheart.rlgrc.misc.Configs;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;

@Log4j2
public class App {
    public static final String PROJECT = "rlgrc";

    public static void main(String[] args) throws Exception {
        if (!System.getProperties().containsKey("workspace")) {
            log.fatal("workspace directory parameter needs to be set via -Dworkspace=/path/you/want");
            Runtime.getRuntime().halt(0);
        }

        BasicLookAndFeel darcula = new DarculaLaf();
        UIManager.setLookAndFeel(darcula);

        new FrameMain(new Configs()).setVisible(true);
    }
}
