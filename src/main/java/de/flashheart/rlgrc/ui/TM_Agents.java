package de.flashheart.rlgrc.ui;

import de.flashheart.rlgrc.misc.Configs;
import de.flashheart.rlgrc.misc.JavaTimeConverter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.table.AbstractTableModel;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

@Log4j2
public class TM_Agents extends AbstractTableModel {
    private final MultiKeyMap<String, String> agents_states;
    private final HashMap<String, JSONObject> tooltips;
    private final ArrayList<String> agents;
    private final Configs configs;
    private String[] colnames = new String[]{"Agent", "Version", "last contact", "AP", "WIFI"};

    public TM_Agents(JSONObject agent_states, Configs configs) throws JSONException {
        this.configs = configs;
        agents_states = new MultiKeyMap<>();
        agents = new ArrayList<>();
        tooltips = new HashMap<>();
        refresh_agents(agent_states);
    }

    public void refresh_agents(JSONObject agent_states) {
        agents_states.clear();
        agents.clear();

        agent_states.keySet().forEach(agent -> {
                    agent_states.getJSONObject(agent).toMap().forEach((s, o) ->
                            agents_states.put(agent, s, o.toString()));
                    agents.add(agent);
                    tooltips.put(agent, agent_states.getJSONObject(agent));
                }
        );
        agents.sort(String::compareTo);
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int column) {
        return colnames[column];
    }

    @Override
    public int getRowCount() {
        return agents.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    public String getTooltipAt(int rowIndex) {
        return tooltips.get(agents.get(rowIndex)).toString(4).replaceAll("\\n", "<br/>");
    }

    public String getValueAt(int rowIndex) {
        return tooltips.get(agents.get(rowIndex)).toString(4);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        log.trace("rowindex {}, colindex{}, agents {}", rowIndex, columnIndex, agents.size());
        String agent = agents.get(rowIndex);
        Object value;


        switch (columnIndex) {
            case 0: {
                value = agent;
                break;
            }
            case 1: {
                value = agents_states.get(agent, "version");
                break;
            }
            case 2: {
                LocalDateTime ldt = JavaTimeConverter.from_iso8601(agents_states.get(agent, "timestamp"));
                Duration.between(ldt, LocalDateTime.now()).toSeconds();
                value = Duration.between(ldt, LocalDateTime.now()).toSeconds() + "s ago";
                break;
            }
            case 3: {
                Optional<String> optAP = Optional.ofNullable(agents_states.get(agent, "ap"));
                // check if there is a better name for this ap in config.txt. If not we simply show the MAC address.
                value = optAP.isPresent() ? configs.get("ap_" + optAP.get().toLowerCase(), optAP.get()) : "--";
                break;
            }
            case 4: {
                value = agents_states.get(agent, "wifi");
                break;
            }
            default: {
                value = "error";
            }
        }
        return value;
    }


}
