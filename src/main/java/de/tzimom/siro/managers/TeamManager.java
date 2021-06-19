package de.tzimom.siro.managers;

import de.tzimom.siro.utils.Team;
import de.tzimom.siro.utils.CustomPlayer;

import java.util.*;

public class TeamManager extends FileManager {

    private Set<Team> teams = new HashSet<>();

    public TeamManager() {
        if (!getConfig().contains("teams"))
            return;

        if (!getConfig().isConfigurationSection("teams"))
            return;

        getConfig().getConfigurationSection("teams").getKeys(false).forEach(teamName -> {
            List<String> membersStrings = getConfig().getStringList("teams." + teamName);
            CustomPlayer[] members = new CustomPlayer[membersStrings.size()];
            Team team = new Team(teamName, members);

            for (int i = 0; i < membersStrings.size(); i++) {
                CustomPlayer customPlayer = CustomPlayer.fromKey(membersStrings.get(i));
                customPlayer.setTeam(team);

                members[i] = customPlayer;
            }

            teams.add(team);
        });
    }

    protected void saveConfig() {
        getConfig().set("teams", null);

        teams.forEach(team -> {
            List<String> stringList = new ArrayList<>();

            for (CustomPlayer member : team.getMembers()) {
                if (member != null) stringList.add(member.asKey());
            }

            getConfig().set("teams." + team.getTeamName(), stringList);
        });

        super.saveConfig();
    }

    public void registerTeam(Team team) {
        teams.add(team);

        for (CustomPlayer member : team.getMembers()) {
            if (member != null) member.setTeam(team);
        }
    }

    public void deleteTeam(Team team) {
        teams.remove(team);

        for (CustomPlayer member : team.getMembers())
            if (member != null) member.setTeam(null);
    }

    public Team getTeam(String teamName) {
        for (Team team : teams) {
            if (team.getTeamName().equalsIgnoreCase(teamName))
                return team;
        }

        return null;
    }

    public Set<Team> getTeams() {
        return teams;
    }

    public String getFileName() {
        return "teams";
    }

}
