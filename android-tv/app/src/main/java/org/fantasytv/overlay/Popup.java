package org.fantasytv.overlay;

/** Why a popup is showing: a score change (with each side's change) or a captioned check-in. */
final class Popup {
    final double mineDelta, opponentDelta;
    final String caption;
    final long durationMs;
    private Popup(double mine, double opponent, String caption, long durationMs) {
        this.mineDelta = mine; this.opponentDelta = opponent; this.caption = caption; this.durationMs = durationMs;
    }
    static Popup score(double[] delta) { return new Popup(delta[0], delta[1], "", 5000); }
    static Popup check(String caption, long durationMs) { return new Popup(0, 0, caption, durationMs); }
    boolean isScore() { return Math.abs(mineDelta) > 1e-9 || Math.abs(opponentDelta) > 1e-9; }
    /** A score change beats a check-in; two score changes for the same league add up. */
    Popup merge(Popup next) {
        if (next == null) return this;
        if (isScore() && next.isScore()) return new Popup(mineDelta + next.mineDelta, opponentDelta + next.opponentDelta, "", 5000);
        if (isScore()) return this;
        return next;
    }
}
