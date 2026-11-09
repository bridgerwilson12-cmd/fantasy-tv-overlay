package org.fantasytv.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable normalized feed data. Never contains credentials or a URL to launch. */
public final class FeedModels {
    private FeedModels() {}
    public static final class Player {
        public final String slot, name, gameId, gameState;
        public final Double points, projection;
        public final boolean live, onTv;
        public final int period;
        public Player(String slot, String name, Double points, Double projection,
                      String gameId, boolean live, boolean onTv) {
            this(slot, name, points, projection, gameId, live, onTv, "", 0);
        }
        /** gameState is live/final/scheduled or empty; period is the NFL quarter (5+ = overtime). */
        public Player(String slot, String name, Double points, Double projection,
                      String gameId, boolean live, boolean onTv, String gameState, int period) {
            this.slot = slot; this.name = name; this.points = points; this.projection = projection;
            this.gameId = gameId; this.live = live; this.onTv = onTv;
            this.gameState = gameState == null ? "" : gameState; this.period = period;
        }
    }
    public static final class Team {
        public final String id, owner, teamName;
        public final double score;
        public final Double projection;
        public final List<Player> starters;
        public Team(String owner, double score, Double projection, List<Player> starters) {
            this("", owner, score, projection, starters);
        }
        public Team(String id, String owner, double score, Double projection, List<Player> starters) {
            this(id, owner, "", score, projection, starters);
        }
        public Team(String id, String owner, String teamName, double score, Double projection, List<Player> starters) {
            this.teamName = teamName == null ? "" : teamName;
            this.id = id; this.owner = owner; this.score = score; this.projection = projection;
            this.starters = Collections.unmodifiableList(new ArrayList<>(starters));
        }
        /** League team name when requested and known, otherwise the manager's name. */
        public String label(boolean preferTeamName) { return preferTeamName && !teamName.isEmpty() ? teamName : owner; }
    }
    public static final class League {
        public final String id, name;
        public final int week, season;
        public final Team mine, opponent;
        public final Double probability;
        public League(String id, String name, int week, Team mine, Team opponent, Double probability) {
            this(id,name,week,mine,opponent,probability,0);
        }
        public League(String id, String name, int week, Team mine, Team opponent, Double probability, int season) {
            this.season = season; this.id = id; this.name = name; this.week = week; this.mine = mine;
            this.opponent = opponent; this.probability = probability;
        }
    }
    public static final class Feed {
        public final double updated;
        public final String watching;
        public final List<League> leagues;
        public Feed(double updated, String watching, List<League> leagues) {
            this.updated = updated; this.watching = watching;
            this.leagues = Collections.unmodifiableList(new ArrayList<>(leagues));
        }
        public League find(String id) {
            for (League league : leagues) if (league.id.equals(id)) return league;
            return null;
        }
        public boolean fresh(double now) { return now - updated <= 120 && updated - now <= 60; }
    }
}
