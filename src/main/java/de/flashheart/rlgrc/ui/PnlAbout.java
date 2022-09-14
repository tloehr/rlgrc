package de.flashheart.rlgrc.ui;

import com.google.common.io.Resources;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXEditorPane;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.nio.charset.Charset;

@Log4j2
public class PnlAbout extends JPanel {
    private JScrollPane scrl;
    private JXEditorPane txtAbout;

    public PnlAbout() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        initPanel();
    }

    @SneakyThrows
    private void initPanel() {
        scrl = new JScrollPane();
        txtAbout = new JXEditorPane();
        txtAbout.setContentType("text/html");
        txtAbout.setEditable(false);
        scrl.setViewportView(txtAbout);
        txtAbout.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        txtAbout.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        if (Desktop.isDesktopSupported()) {
            txtAbout.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        log.warn(ex);
                    }
                }
            });
        }
        txtAbout.setText(Resources.toString(Resources.getResource("about.html"), Charset.defaultCharset()));
        add(scrl);
    }
}
