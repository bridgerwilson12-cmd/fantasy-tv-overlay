package org.fantasytv.overlay;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Optional helper, off until the viewer enables it in Accessibility settings.
 *
 * It does two things only:
 *  1. Watches which app comes to the front (package name only; it cannot read screen content)
 *     and shows the score a few seconds after a chosen app such as DirecTV or Prime Video opens.
 *  2. Consumes exactly one remote button the viewer picked and shows the score on press.
 *     Every other key passes straight through untouched.
 *
 * Both are no-ops unless score alerts are already running.
 */
public final class ScoreKeyService extends AccessibilityService {
    private static final long APP_OPEN_DELAY_MS = 4000, APP_OPEN_COOLDOWN_MS = 60_000;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Long> lastOpenPopup = new HashMap<>();
    private ConfigStore config;
    private String foreground = "";

    @Override protected void onServiceConnected() { config = new ConfigStore(this); }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (config == null || event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence name = event.getPackageName();
        if (name == null) return;
        String pkg = name.toString();
        // Our own popup and system overlays (volume, notifications) are not app switches.
        if (pkg.equals(getPackageName()) || pkg.equals(foreground) || !launchable(pkg)) return;
        foreground = pkg;
        config.setLastApp(pkg);
        if (!config.appOpenAlerts() || !watched(pkg)) return;
        Long last = lastOpenPopup.get(pkg);
        long now = SystemClock.elapsedRealtime();
        if (last != null && now - last < APP_OPEN_COOLDOWN_MS) return;
        lastOpenPopup.put(pkg, now);
        // Wait for the app's splash screen; skip if the viewer already moved on.
        main.postDelayed(() -> { if (pkg.equals(foreground)) OverlayService.showNow("SCORE CHECK", 6000); }, APP_OPEN_DELAY_MS);
    }

    @Override protected boolean onKeyEvent(KeyEvent event) {
        if (config == null) return false;
        int code = event.getKeyCode();
        if (config.capturingKey()) {
            if (reserved(code)) {
                if (event.getAction() == KeyEvent.ACTION_UP) toast("That button is needed for navigation. Press a different one.");
                return false;
            }
            if (event.getAction() == KeyEvent.ACTION_UP) {
                config.setScoreKey(code);
                toast("Score button set: " + describe(code));
            }
            return true;
        }
        int chosen = config.scoreKey();
        if (chosen == 0 || code != chosen || !OverlayService.isRunning()) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && event.getRepeatCount() == 0) OverlayService.showNow("SCORE CHECK", 8000);
        return true;
    }

    /** Keys the remote can't live without are never taken over. */
    static boolean reserved(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_HOME: case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP: case KeyEvent.KEYCODE_VOLUME_DOWN: case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_UNKNOWN:
                return true;
            default: return false;
        }
    }
    static String describe(int code) {
        String name = KeyEvent.keyCodeToString(code).replace("KEYCODE_", "").replace('_', ' ').toLowerCase(Locale.ROOT);
        return name.isEmpty() ? "button " + code : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    private boolean watched(String pkg) {
        for (String entry : config.watchedApps().split("[,\\s]+")) {
            String prefix = entry.trim();
            if (!prefix.isEmpty() && (pkg.equals(prefix) || pkg.startsWith(prefix + "."))) return true;
        }
        return false;
    }
    private boolean launchable(String pkg) {
        PackageManager pm = getPackageManager();
        try { return pm.getLeanbackLaunchIntentForPackage(pkg) != null || pm.getLaunchIntentForPackage(pkg) != null || isHome(pkg); }
        catch (RuntimeException e) { return false; }
    }
    private boolean isHome(String pkg) {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        android.content.pm.ResolveInfo info = getPackageManager().resolveActivity(home, 0);
        return info != null && info.activityInfo != null && pkg.equals(info.activityInfo.packageName);
    }
    private void toast(String text) { main.post(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show()); }
    @Override public void onInterrupt() { }
}
