package io.github.stormrdoteth.commercialshield;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * Watches for the one remote button the user picked. Pressing it toggles a
 * full-screen cover over whatever is playing and mutes the sound. Back also
 * lowers the cover, and it lowers itself after the auto-lift time so the TV
 * is never left covered and muted by accident.
 *
 * Every other key passes straight through untouched.
 */
public class ShieldService extends AccessibilityService {
    /** Set while the service is connected, so the setup screen can preview the cover. */
    static ShieldService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private CoverView cover;
    private boolean weMuted;
    /** After learning a new button, its key-up must not immediately raise the shield. */
    private int swallowUpFor = -1;

    private final Runnable autoOff = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onServiceConnected() {
        instance = this;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        boolean up = event.getAction() == KeyEvent.ACTION_UP;

        if (code == swallowUpFor) {
            if (up) swallowUpFor = -1;
            return true;
        }

        if (Prefs.learning(this)) {
            if (down && !Prefs.isReserved(code)) {
                Prefs.get(this).edit()
                        .putInt(Prefs.KEY_CODE, code)
                        .putBoolean(Prefs.LEARNING, false)
                        .apply();
                swallowUpFor = code;
                return true;
            }
            return false;
        }

        if (code == Prefs.keyCode(this)) {
            if (up) toggle();
            return true;
        }
        if (code == KeyEvent.KEYCODE_BACK && cover != null) {
            if (up) hide();
            return true;
        }
        return false;
    }

    void toggle() {
        if (cover == null) show(); else hide();
    }

    boolean isShowing() {
        return cover != null;
    }

    void show() {
        if (cover != null) return;
        cover = new CoverView(this, Prefs.style(this));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.OPAQUE);
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).addView(cover, lp);

        if (Prefs.mute(this)) mute();
        int minutes = Prefs.autoOffMin(this);
        if (minutes > 0) handler.postDelayed(autoOff, minutes * 60_000L);
    }

    void hide() {
        handler.removeCallbacks(autoOff);
        if (cover == null) return;
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).removeView(cover);
        cover = null;
        unmute();
    }

    private void mute() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // Leave the sound alone afterwards if it was already muted before the break.
        if (am.isStreamMute(AudioManager.STREAM_MUSIC)) return;
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
        weMuted = true;
    }

    private void unmute() {
        if (!weMuted) return;
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
        weMuted = false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        hide();
        if (instance == this) instance = null;
        super.onDestroy();
    }
}
