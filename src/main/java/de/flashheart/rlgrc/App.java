package de.flashheart.rlgrc;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import de.flashheart.rlgrc.misc.JSONConfigs;
import de.flashheart.rlgrc.ui.ExceptionHandler;
import de.flashheart.rlgrc.ui.FrameMain;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;

@Log4j2
public class App {
    public static final String PROJECT = "rlgrc";

    // https://github.com/nidi3/graphviz-java#user-content-how-it-works
    public static void main(String[] args) throws Exception {
        if (!System.getProperties().containsKey("workspace")) {
            log.fatal("workspace directory parameter needs to be set via -Dworkspace=/path/you/want");
            Runtime.getRuntime().halt(0);
        }

        for (String game : Arrays.asList("conquest", "farcry", "rush", "centerflags", "signal", "results", "stronghold", "timed")) {
            FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + game));
        }

        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            FlatDarkLaf.setup();
        } catch (Exception ex) {
            log.fatal("Failed to initialize LaF");
            System.exit(0);
        }

        // https://stackoverflow.com/a/27858065
        // Regular Exception
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        // EDT Exception
        SwingUtilities.invokeAndWait(() -> Thread.currentThread().setUncaughtExceptionHandler(new ExceptionHandler()));

        new FrameMain(new JSONConfigs(System.getProperties().getProperty("workspace"))).setVisible(true);
    }
}
