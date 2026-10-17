package io.github.stormrdoteth.notinfrontofthekids;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;

/** All user settings, stored on the device only. */
final class Prefs {
    static final String FILE = "shield";
    static final String KEY_CODE = "key_code";
    static final String LEARNING = "learning";
    static final String AUTO_OFF_MIN = "auto_off_min";
    static final String MUTE = "mute";
    static final String STYLE = "style";

    static final String STYLE_CLOCK = "clock";
    static final String STYLE_BLACK = "black";

    /** Auto-lift choices offered in setup; 0 means never. */
    static final int[] AUTO_OFF_CHOICES = {2, 3, 5, 10, 0};

    private Prefs() { }

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static int keyCode(Context c) { return get(c).getInt(KEY_CODE, -1); }
    static boolean learning(Context c) { return get(c).getBoolean(LEARNING, false); }
    static int autoOffMin(Context c) { return get(c).getInt(AUTO_OFF_MIN, 5); }
    static boolean mute(Context c) { return get(c).getBoolean(MUTE, true); }
    static String style(Context c) { return get(c).getString(STYLE, STYLE_CLOCK); }

    /** Buttons that can't be used as the shield button, so the remote stays usable. */
    static boolean isReserved(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_TV_POWER:
            case KeyEvent.KEYCODE_SLEEP:
            case KeyEvent.KEYCODE_WAKEUP:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;
            default:
                return false;
        }
    }

    /** "KEYCODE_PROG_RED" -> "Prog red (183)" */
    static String keyName(int code) {
        if (code < 0) return "not set";
        String s = KeyEvent.keyCodeToString(code);
        if (s.startsWith("KEYCODE_")) s = s.substring(8);
        s = s.replace('_', ' ').toLowerCase();
        if (!s.isEmpty()) s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        return s + " (" + code + ")";
    }
}
