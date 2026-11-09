package org.fantasytv.overlay;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import static org.junit.Assert.*;

public class GamePolicyTest {
    private static FeedModels.Player player(String game, String state, int period) {
        return new FeedModels.Player("WR", "Demo", 1.0, 2.0, game, state.equals("live"), false, state, period);
    }
    private static FeedModels.Feed feed(FeedModels.Player... players) {
        return new FeedModels.Feed(1000, "", Collections.singletonList(new FeedModels.League("a", "Demo", 9,
            new FeedModels.Team("1", "Alex", 0, null, Arrays.asList(players)),
            new FeedModels.Team("2", "Sam", 0, null, Collections.emptyList()), null, 2026)));
    }
    @Test public void kickoffQuartersOvertimeAndFinal() {
        GamePolicy p = new GamePolicy();
        assertTrue(p.accept(feed(player("g", "scheduled", 0))).isEmpty());
        assertEquals(Collections.singletonMap("a", "KICKOFF"), p.accept(feed(player("g", "live", 1))));
        assertTrue(p.accept(feed(player("g", "live", 1))).isEmpty());
        assertEquals(Collections.singletonMap("a", "START OF Q2"), p.accept(feed(player("g", "live", 2))));
        assertEquals(Collections.singletonMap("a", "OVERTIME"), p.accept(feed(player("g", "live", 5))));
        assertEquals(Collections.singletonMap("a", "GAME FINAL"), p.accept(feed(player("g", "final", 5))));
        assertTrue(p.accept(feed(player("g", "final", 5))).isEmpty());
    }
    @Test public void firstSightingMidGameIsQuietAndGapsKeepState() {
        GamePolicy p = new GamePolicy();
        assertTrue(p.accept(feed(player("g", "live", 3))).isEmpty());
        assertTrue(p.accept(feed()).isEmpty());
        assertTrue(p.accept(feed(player("g", "live", 3))).isEmpty());
        Map<String, String> next = p.accept(feed(player("g", "live", 4)));
        assertEquals("START OF Q4", next.get("a"));
    }
}
