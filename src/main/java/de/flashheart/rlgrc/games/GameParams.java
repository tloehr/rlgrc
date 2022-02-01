package de.flashheart.rlgrc.games;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@ToString
@Getter
@Setter
public class GameParams {
    String comment;
    String classname;
    Multimap<String, String> agents; // agent -> role
    Multimap<String, String> roles; // role -> agent

    public GameParams(){
        this.classname="";
        this.comment="";
        this.agents = HashMultimap.create();
        this.roles = HashMultimap.create();
    }

    public GameParams(String comment, String classname) {
        this();
        this.comment = comment;
        this.classname = classname;
    }

    public void add_agents(String role, String... agts) {
        for (String agent : agts) {
            roles.put(role, agent);
            agents.put(agent, role);
        }
    }

    public void add_spawn(String color, String agt) {
        add_agents(color + "_spawn", agt);
        add_agents("spawns", agt);
    }

    public void add_sirens(String... agt) {
        add_agents("sirens", agt);
    }

    public void add_zone_sirens(String zone, String... agt) {
        add_agents(zone + "_siren", agt);
        add_sirens(agt);
    }


}
