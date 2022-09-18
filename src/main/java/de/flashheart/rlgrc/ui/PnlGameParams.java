/*
 * Created by JFormDesigner on Wed Sep 14 20:24:44 CEST 2022
 */

package de.flashheart.rlgrc.ui;

import de.flashheart.rlgrc.misc.JSONConfigs;
import de.flashheart.rlgrc.networking.RestHandler;
import de.flashheart.rlgrc.ui.panels.MyPanels;
import de.flashheart.rlgrc.ui.params.CenterFlagsParams;
import de.flashheart.rlgrc.ui.params.ConquestParams;
import de.flashheart.rlgrc.ui.params.FarcryParams;
import de.flashheart.rlgrc.ui.params.GameParams;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import javax.swing.*;

/**
 * @author Torsten Löhr
 */
@Log4j2
public class PnlGameParams extends JPanel {
    private final HashMap<String, GameParams> game_modes;
    private final RestHandler restHandler;
    private final JSONConfigs configs;
    private final JFrame owner;
    private boolean selected;
    private JSONObject current_state;
    private Optional<GameParams> current_game_params;
    private String current_game_id;

    public PnlGameParams(RestHandler restHandler, JSONConfigs configs, JFrame owner, String current_game_id) {
        this.restHandler = restHandler;
        this.configs = configs;
        this.owner = owner;
        this.current_game_id = current_game_id;
        current_state = new JSONObject();
        current_game_params = Optional.empty();
        selected = false;
        game_modes = new HashMap<>();

        initComponents();
        initPanel();
    }

    public void setCurrent_game_id(String current_game_id) {
        this.current_game_id = current_game_id;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (!selected) current_game_params = Optional.empty();
        else {
            current_state = restHandler.get("game/status", current_game_id);
            if (current_state.has("game_state")) {
                cmbGameModes.setSelectedItem(current_state.getString("mode"));
            } else {
                update(cmbGameModes.getSelectedItem().toString());
            }
        }
    }

    private void initPanel() {
//        game_modes.put("conquest", new ConquestParams(configs));
//        game_modes.put("farcry", new FarcryParams(configs));
//        game_modes.put("centerflags", new CenterFlagsParams(configs, owner));
//        game_modes.forEach((s, gameParams) -> cmbGameModes.addItem(gameParams));

        cmbGameModes.addItem("conquest");
        cmbGameModes.addItem("farcry");
        cmbGameModes.addItem("centerflags");
        cmbGameModes.setEnabled(true);

        //cmbGameModes.setRenderer((list, value, index, isSelected, cellHasFocus) -> new DefaultListCellRenderer().getListCellRendererComponent(list, ((GameParams) value).getMode(), index, isSelected, cellHasFocus));

    }

    private void btnTestJSON(ActionEvent e) {
        GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();
        current_game_mode.from_ui_to_params();
        log.debug(current_game_mode.getParams().toString(4));
    }


    private void update(String mode) {
        if (mode.equals("conquest")) current_game_params = Optional.of(new ConquestParams(configs));
        if (mode.equals("centerflags")) current_game_params = Optional.of(new CenterFlagsParams(configs, owner));
        if (mode.equals("farcry")) current_game_params = Optional.of(new FarcryParams(configs));
        update();
    }

    private void update() {
        cmbGameModes.setEnabled(!current_state.has("game_state"));
        current_game_params.ifPresent(gameParams -> {
            //JSONObject current_state = restHandler.get("game/status", current_game_id);
            gameParams.from_params_to_ui(current_state);
            SwingUtilities.invokeLater(() -> {
                pnlCenter.removeAll();
                pnlCenter.add(gameParams);
                revalidate();
                repaint();
            });
        });
    }

    private void btnSaveFile(ActionEvent e) {
        try {
            GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();
            current_game_mode.save_file();
            lblFile.setText(current_game_mode.getFilename());
        } catch (IOException ex) {
            log.error(ex);
            //pnlServer.addLog(ex.getMessage());
        }
    }

    private void btnLoadFile(ActionEvent e) {
        GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();
        current_game_mode.load_file();
        current_game_mode.from_params_to_ui();
        lblFile.setText(current_game_mode.getFilename());
    }

    private void btnFileNew(ActionEvent e) {
        GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();
        current_game_mode.load_defaults();
        current_game_mode.from_params_to_ui();
        lblFile.setText(current_game_mode.getFilename());
    }

    private void btnUnloadOnServer(ActionEvent e) {
        restHandler.post("game/unload", current_game_id);
        current_state.clear();
        firePropertyChange("game_unloaded", "", current_game_id);
    }

    private void btnSendGameToServer(ActionEvent e) {
        //GameParams current_game_mode = (GameParams) cmbGameModes.getSelectedItem();
        current_game_params.ifPresent(gameParams -> {
            gameParams.from_ui_to_params();
            current_state = restHandler.post("game/load", gameParams.getParams().toString(4), current_game_id);
            firePropertyChange("game_loaded", "", current_state);
        });
    }

    private void cmbGameModesItemStateChanged(ItemEvent e) {
        update(e.getItem().toString());
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        pnlFiles = new JPanel();
        cmbGameModes = new JComboBox();
        btnFileNew = new JButton();
        btnLoadFile = new JButton();
        btnSaveFile = new JButton();
        btnSendGameToServer = new JButton();
        btnUnloadOnServer = new JButton();
        hSpacer1 = new JPanel(null);
        lblFile = new JLabel();
        hSpacer2 = new JPanel(null);
        btnTestJSON = new JButton();
        pnlCenter = new JPanel();

        //======== this ========
        setLayout(new BorderLayout());

        //======== pnlFiles ========
        {
            pnlFiles.setLayout(new BoxLayout(pnlFiles, BoxLayout.X_AXIS));

            //---- cmbGameModes ----
            cmbGameModes.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            cmbGameModes.addItemListener(e -> cmbGameModesItemStateChanged(e));
            pnlFiles.add(cmbGameModes);

            //---- btnFileNew ----
            btnFileNew.setText(null);
            btnFileNew.setIcon(new ImageIcon(getClass().getResource("/artwork/filenew.png")));
            btnFileNew.setMinimumSize(new Dimension(38, 38));
            btnFileNew.setPreferredSize(new Dimension(38, 38));
            btnFileNew.addActionListener(e -> btnFileNew(e));
            pnlFiles.add(btnFileNew);

            //---- btnLoadFile ----
            btnLoadFile.setText(null);
            btnLoadFile.setIcon(new ImageIcon(getClass().getResource("/artwork/fileopen.png")));
            btnLoadFile.setMinimumSize(new Dimension(38, 38));
            btnLoadFile.setPreferredSize(new Dimension(38, 38));
            btnLoadFile.addActionListener(e -> btnLoadFile(e));
            pnlFiles.add(btnLoadFile);

            //---- btnSaveFile ----
            btnSaveFile.setText(null);
            btnSaveFile.setIcon(new ImageIcon(getClass().getResource("/artwork/filesave.png")));
            btnSaveFile.setMinimumSize(new Dimension(38, 38));
            btnSaveFile.setPreferredSize(new Dimension(38, 38));
            btnSaveFile.addActionListener(e -> btnSaveFile(e));
            pnlFiles.add(btnSaveFile);

            //---- btnSendGameToServer ----
            btnSendGameToServer.setText(null);
            btnSendGameToServer.setIcon(new ImageIcon(getClass().getResource("/artwork/upload.png")));
            btnSendGameToServer.setToolTipText("Load game on the server");
            btnSendGameToServer.setMinimumSize(new Dimension(38, 38));
            btnSendGameToServer.setPreferredSize(new Dimension(38, 38));
            btnSendGameToServer.setFont(new Font(".SF NS Text", Font.PLAIN, 18));
            btnSendGameToServer.addActionListener(e -> btnSendGameToServer(e));
            pnlFiles.add(btnSendGameToServer);

            //---- btnUnloadOnServer ----
            btnUnloadOnServer.setText(null);
            btnUnloadOnServer.setIcon(new ImageIcon(getClass().getResource("/artwork/unload.png")));
            btnUnloadOnServer.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
            btnUnloadOnServer.setToolTipText("Remove the loaded game from the commander's memory.");
            btnUnloadOnServer.setHorizontalAlignment(SwingConstants.LEFT);
            btnUnloadOnServer.setPreferredSize(new Dimension(38, 38));
            btnUnloadOnServer.addActionListener(e -> btnUnloadOnServer(e));
            pnlFiles.add(btnUnloadOnServer);
            pnlFiles.add(hSpacer1);

            //---- lblFile ----
            lblFile.setText("no file");
            lblFile.setFont(new Font(".SF NS Text", Font.PLAIN, 16));
            pnlFiles.add(lblFile);
            pnlFiles.add(hSpacer2);

            //---- btnTestJSON ----
            btnTestJSON.setText(null);
            btnTestJSON.setIcon(new ImageIcon(getClass().getResource("/artwork/debug28.png")));
            btnTestJSON.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 18));
            btnTestJSON.setToolTipText("Remove the loaded game from the commander's memory.");
            btnTestJSON.setHorizontalAlignment(SwingConstants.LEFT);
            btnTestJSON.setPreferredSize(new Dimension(38, 38));
            btnTestJSON.addActionListener(e -> btnTestJSON(e));
            pnlFiles.add(btnTestJSON);
        }
        add(pnlFiles, BorderLayout.SOUTH);

        //======== pnlCenter ========
        {
            pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.PAGE_AXIS));
        }
        add(pnlCenter, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel pnlFiles;
    private JComboBox cmbGameModes;
    private JButton btnFileNew;
    private JButton btnLoadFile;
    private JButton btnSaveFile;
    private JButton btnSendGameToServer;
    private JButton btnUnloadOnServer;
    private JPanel hSpacer1;
    private JLabel lblFile;
    private JPanel hSpacer2;
    private JButton btnTestJSON;
    private JPanel pnlCenter;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
