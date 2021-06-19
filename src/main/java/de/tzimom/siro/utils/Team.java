package de.tzimom.siro.utils;

import java.util.UUID;

public class Team {

    public static final byte MAX_NAME_LENGTH = 20;

    private String teamName;
    private CustomPlayer[] members;

    public Team(String teamName, CustomPlayer[] members) {
        this.teamName = teamName;
        this.members = members;
    }

    public String getTeamName() {
        return teamName;
    }

    public void rename(String teamName) {
        this.teamName = teamName;
    }

    public CustomPlayer[] getMembers() {
        return members;
    }

}
