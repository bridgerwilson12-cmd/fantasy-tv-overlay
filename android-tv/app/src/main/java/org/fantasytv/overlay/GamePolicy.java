package org.fantasytv.overlay;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Detects NFL game milestones (kickoff, each new quarter, overtime, final) for games that involve
 * either team's starters. Like ScorePolicy, the first sight of a game is quiet and a new week or
 * opponent resets everything. Games missing from one sample keep their last known state.
 */
public final class GamePolicy {
    private final Map<String, String> identities = new HashMap<>();
    private final Map<String, Map<String, String>> games = new HashMap<>();

    /** Returns leagueId → caption for leagues where a starter's game reached a new milestone. */
    public Map<String, String> accept(FeedModels.Feed feed) {
        Map<String, String> events = new LinkedHashMap<>();
        for (FeedModels.League league : feed.leagues) {
            String identity = league.season + "|" + league.week + "|" + league.mine.id + "|" + league.opponent.id;
            if (!identity.equals(identities.get(league.id))) { identities.put(league.id, identity); games.put(league.id, new HashMap<>()); }
            Map<String, String> known = games.get(league.id);
            String caption = null; int rank = 0;
            for (FeedModels.Team team : new FeedModels.Team[]{league.mine, league.opponent}) {
                for (FeedModels.Player player : team.starters) {
                    if (player.gameId.isEmpty() || player.gameState.isEmpty()) continue;
                    String now = player.gameState + ":" + player.period, before = known.put(player.gameId, now);
                    if (before == null || before.equals(now)) continue;
                    String next = caption(before, player.gameState, player.period);
                    int nextRank = next == null ? 0 : next.equals("GAME FINAL") ? 1 : 2;
                    if (nextRank > rank) { caption = next; rank = nextRank; }
                }
            }
            if (caption != null) events.put(league.id, caption);
        }
        return events;
    }
    static String caption(String before, String state, int period) {
        String[] old = before.split(":", 2);
        int oldPeriod = old.length == 2 ? Integer.parseInt(old[1]) : 0;
        if (state.equals("final")) return old[0].equals("final") ? null : "GAME FINAL";
        if (!state.equals("live") || period <= 0 || (old[0].equals("live") && period <= oldPeriod)) return null;
        if (period == 1) return "KICKOFF";
        if (period >= 5) return "OVERTIME";
        return "START OF Q" + period;
    }
    public void reset() { identities.clear(); games.clear(); }
}
