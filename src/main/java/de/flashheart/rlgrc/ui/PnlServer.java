package de.flashheart.rlgrc.ui;

import de.flashheart.rlgrc.networking.RestHandler;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@Log4j2
public class PnlServer extends JPanel {
    private final RestHandler restHandler;
    private String current_game_id;
    private JButton btnRefreshServer;
    private JTextArea txtLogger;
    private JScrollPane scrollLog;
    private final int MAX_LOG_LINES = 400;
    private boolean selected;
    private JSONObject current_state;


    public PnlServer(RestHandler restHandler, String current_game_id) {
        super(new BorderLayout());
        selected = false;
        current_state = new JSONObject();
        this.restHandler = restHandler;
        this.current_game_id = current_game_id;
        initLogger();
        initButtons();
    }

    @Override
    public boolean isShowing() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (!selected) return;
        current_state = restHandler.get("game/status", current_game_id);
    }

    public void setCurrent_game_id(String current_game_id) {
        this.current_game_id = current_game_id;
        current_state = restHandler.get("game/status", current_game_id);

    }

    void initLogger() {
        scrollLog = new JScrollPane();
        txtLogger = new JTextArea();
        txtLogger.setForeground(new Color(51, 255, 51));
        txtLogger.setLineWrap(true);
        txtLogger.setWrapStyleWord(true);
        txtLogger.setEditable(false);
        scrollLog.setViewportView(txtLogger);
        add(scrollLog, BorderLayout.CENTER);

        txtLogger.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    Element root = e.getDocument().getDefaultRootElement();
                    while (root.getElementCount() > MAX_LOG_LINES) {
                        Element firstLine = root.getElement(0);
                        try {
                            e.getDocument().remove(0, firstLine.getEndOffset());
                        } catch (BadLocationException ble) {
                            log.error(ble);
                        }
                    }
                });
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    public void addLog(String text){
        log.trace(text);
        SwingUtilities.invokeLater(() -> {
            txtLogger.append(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)) + ": " + text + "\n");
            scrollLog.getVerticalScrollBar().setValue(scrollLog.getVerticalScrollBar().getMaximum());
        });
    }

    public void clear() {
        txtLogger.setText(null);
    }

    private void initButtons() {

        btnRefreshServer = new JButton("Refresh Server Status", new ImageIcon(getClass().getResource("/artwork/reload-on.png")));
        btnRefreshServer.setFont(FrameMain.MY_FONT);
        btnRefreshServer.addActionListener(e -> {
            restHandler.get("game/status", current_game_id);
        });
        add(btnRefreshServer, BorderLayout.SOUTH);
    }
}
