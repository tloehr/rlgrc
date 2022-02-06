package de.flashheart.rlgrc.ui;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;

@Log4j2
public class TM_Agents extends AbstractTableModel {
    private final MultiKeyMap<String, String> agents_states;
    private final HashMap<String, JSONObject> tooltips;
    private final ArrayList<String> agents;
    private String[] colnames = new String[]{"Agent","Timestamp","WIFI"};
    public TM_Agents(JSONObject agent_states) throws JSONException {
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
        return 3;
    }

    public String getValueAt(int rowIndex) {
        return tooltips.get(agents.get(rowIndex)).toString(4).replaceAll("\\n","<br/>");
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        log.debug("rowindex {}, colindex{}, agents {}", rowIndex, columnIndex, agents.size());
        String agent = agents.get(rowIndex);
        Object value;
        switch (columnIndex) {
            case 0: {
                value = agent;
                break;
            }
            case 1: {
                value = agents_states.get(agent, "timestamp");
                break;
            }
            case 2: {
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
