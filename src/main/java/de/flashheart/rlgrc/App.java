package de.flashheart.rlgrc;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import de.flashheart.rlgrc.misc.JSONConfigs;
import de.flashheart.rlgrc.ui.FrameMain;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.File;

@Log4j2
public class App {
    public static final String PROJECT = "rlgrc";
// https://github.com/nidi3/graphviz-java#user-content-how-it-works
    public static void main(String[] args) throws Exception {
        if (!System.getProperties().containsKey("workspace")) {
            log.fatal("workspace directory parameter needs to be set via -Dworkspace=/path/you/want");
            Runtime.getRuntime().halt(0);
        }

        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "conquest"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "farcry"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "rush"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "centerflags"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "maggi1"));
        FileUtils.forceMkdir(new File(System.getProperty("workspace") + File.separator + "results"));

        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            FlatDarkLaf.setup();
        } catch (Exception ex) {
            log.fatal("Failed to initialize LaF");
            System.exit(0);
        }

//        BasicLookAndFeel darcula = new DarculaLaf();
//        UIManager.setLookAndFeel(darcula);

        new FrameMain(new JSONConfigs(System.getProperties().getProperty("workspace"))).setVisible(true);
    }
}
