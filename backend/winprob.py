"""Estimated matchup win chance from projections and NFL game time remaining.

Model (``winProbSource: "fantasytv-normal-v1"``):

* Each starter's expected final = actual points + remaining projection.
  Remaining projection is the pregame projection scaled by the share of their
  NFL game still to play (all of it before kickoff, none after the final).
* Uncertainty grows with the points still to be scored: each team's variance is
  ``VARIANCE_PER_POINT`` × its remaining projection (about 22 points of
  standard deviation for a typical 115-point pregame team).
* Win chance = P(my final − their final > 0) under a normal approximation.

It is an estimate, not a provider number. It returns ``None`` when a starter
whose game is not final has no projection or no known game state, so the bar
stays honest instead of guessing.
"""

import math

VARIANCE_PER_POINT = 4.3
REGULATION_SECONDS = 3600.0
QUARTER_SECONDS = 900.0


def _share_left(player):
    state = player.get("nflGameState")
    if state == "final":
        return 0.0
    if state == "scheduled":
        return 1.0
    if state != "live":
        return None
    period, clock = player.get("nflPeriod") or 0, player.get("nflClock")
    if period >= 5:  # Overtime: only a sliver of the game is left.
        return 0.02
    if not 1 <= period <= 4:
        return None
    left = (4 - period) * QUARTER_SECONDS + (clock if isinstance(clock, (int, float)) else QUARTER_SECONDS / 2)
    return max(0.0, min(1.0, left / REGULATION_SECONDS))


def _team(side):
    expected = remaining = 0.0
    players = side.get("players") or []
    if not players:
        return None
    for player in players:
        points = player.get("pts") or 0.0
        share = _share_left(player)
        if share is None:
            return None
        if share == 0:
            expected += points
            continue
        projection = player.get("proj")
        if not isinstance(projection, (int, float)):
            return None
        rest = max(0.0, projection - points) if share >= 1 else max(0.0, projection * share)
        expected += points + rest
        remaining += rest
    return expected, remaining


def estimate(league):
    me, opp = _team(league.get("me") or {}), _team(league.get("opp") or {})
    if me is None or opp is None:
        return None
    lead = me[0] - opp[0]
    variance = VARIANCE_PER_POINT * (me[1] + opp[1])
    if variance < 1e-6:
        return 1.0 if lead > 0 else 0.0 if lead < 0 else 0.5
    probability = 0.5 * (1 + math.erf(lead / math.sqrt(2 * variance)))
    return round(min(1.0, max(0.0, probability)), 4)


def annotate(payload):
    """Fill missing winProb values in place; never overrides a provider value."""
    for league in payload.get("leagues", []):
        if league.get("winProb") is None:
            value = estimate(league)
            if value is not None:
                league["winProb"] = value
                league["winProbSource"] = "fantasytv-normal-v1"
    return payload
