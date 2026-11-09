package org.fantasytv.overlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure score-change policy: first data is quiet, projections never trigger alerts. */
public final class ScorePolicy {
    private Map<String, double[]> baseline = new HashMap<>();
    private Map<String, String> identities = new HashMap<>();
    private final Map<String, double[]> deltas = new HashMap<>();
    /** After accept(): how much {mine, opponent} moved for a changed league, else {0, 0}. */
    public double[] delta(String id) { double[] d = deltas.get(id); return d == null ? new double[]{0, 0} : d.clone(); }
    public List<String> accept(FeedModels.Feed feed) {
        List<String> changes = new ArrayList<>();
        deltas.clear();
        Map<String, double[]> next = new HashMap<>();
        Map<String, String> nextIdentities = new HashMap<>();
        for (FeedModels.League league : feed.leagues) {
            double[] totals = {league.mine.score, league.opponent.score};
            double[] old = baseline.get(league.id);
            String identity = league.season + "|" + league.week + "|" + league.mine.id + "|" + league.opponent.id;
            if (old != null && identity.equals(identities.get(league.id)) && (Double.compare(old[0], totals[0]) != 0 || Double.compare(old[1], totals[1]) != 0)) {
                changes.add(league.id);
                deltas.put(league.id, new double[]{Math.round((totals[0] - old[0]) * 100) / 100.0, Math.round((totals[1] - old[1]) * 100) / 100.0});
            }
            next.put(league.id, totals);
            nextIdentities.put(league.id, identity);
        }
        baseline = next; identities = nextIdentities;
        return changes;
    }
    public void reset() { baseline.clear(); identities.clear(); deltas.clear(); }
}
