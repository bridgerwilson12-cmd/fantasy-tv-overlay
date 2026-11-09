import unittest
from backend import winprob


def player(pts, proj, state, period=0, clock=None):
    return {"pts": pts, "proj": proj, "nflGameState": state, "nflPeriod": period, "nflClock": clock}


def league(me, opp):
    return {"me": {"players": me}, "opp": {"players": opp}, "winProb": None}


class WinProbTests(unittest.TestCase):
    def test_even_pregame_is_fifty_fifty(self):
        self.assertEqual(winprob.estimate(league([player(0, 15, "scheduled")], [player(0, 15, "scheduled")])), 0.5)

    def test_bigger_projection_favored(self):
        p = winprob.estimate(league([player(0, 130, "scheduled")], [player(0, 110, "scheduled")]))
        self.assertTrue(0.6 < p < 0.85, p)

    def test_all_final_is_certain(self):
        self.assertEqual(winprob.estimate(league([player(101, 90, "final")], [player(99, 120, "final")])), 1.0)

    def test_late_lead_is_near_certain(self):
        late = winprob.estimate(league([player(30, 15, "live", 4, 60)], [player(10, 15, "live", 4, 60)]))
        early = winprob.estimate(league([player(30, 15, "scheduled")], [player(10, 15, "scheduled")]))
        self.assertGreater(late, 0.99)
        self.assertGreater(late, early)

    def test_missing_projection_stays_unavailable(self):
        self.assertIsNone(winprob.estimate(league([player(0, None, "scheduled")], [player(0, 10, "scheduled")])))

    def test_unknown_game_state_stays_unavailable(self):
        self.assertIsNone(winprob.estimate(league([player(0, 10, "unknown")], [player(0, 10, "scheduled")])))

    def test_annotate_keeps_provider_value(self):
        payload = {"leagues": [dict(league([player(5, 1, "final")], [player(1, 1, "final")]), winProb=0.3)]}
        self.assertEqual(winprob.annotate(payload)["leagues"][0]["winProb"], 0.3)


if __name__ == "__main__":
    unittest.main()
