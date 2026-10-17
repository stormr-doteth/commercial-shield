package io.github.stormrdoteth.commercialshield;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The app's only screen, driven entirely with the TV remote:
 * turn the service on, pick the shield button, and tweak behavior.
 */
public class SetupActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private TextView status;
    private Button serviceButton, keyButton, autoOffButton, muteButton, styleButton, testButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(48);
        col.setPadding(pad, dp(36), pad, dp(36));
        col.setBackgroundColor(0xFF0B132B);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        col.addView(title);

        status = new TextView(this);
        status.setTextColor(0xCCFFFFFF);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        status.setPadding(0, dp(8), 0, dp(20));
        col.addView(status);

        serviceButton = button(col, new View.OnClickListener() {
            @Override public void onClick(View v) { openAccessibilitySettings(); }
        });
        keyButton = button(col, new View.OnClickListener() {
            @Override public void onClick(View v) { startLearning(); }
        });
        autoOffButton = button(col, new View.OnClickListener() {
            @Override public void onClick(View v) { cycleAutoOff(); }
        });
        muteButton = button(col, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.get(SetupActivity.this).edit().putBoolean(Prefs.MUTE, !Prefs.mute(SetupActivity.this)).apply();
            }
        });
        styleButton = button(col, new View.OnClickListener() {
            @Override public void onClick(View v) {
                String next = Prefs.STYLE_CLOCK.equals(Prefs.style(SetupActivity.this)) ? Prefs.STYLE_BLACK : Prefs.STYLE_CLOCK;
                Prefs.get(SetupActivity.this).edit().putString(Prefs.STYLE, next).apply();
            }
        });
        testButton = button(col, new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (ShieldService.instance != null) ShieldService.instance.show();
            }
        });

        TextView privacy = new TextView(this);
        privacy.setText("Commercial Shield has no internet access and can't see what's on screen. "
                + "It only listens for the one button you pick.");
        privacy.setTextColor(0x99FFFFFF);
        privacy.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        privacy.setPadding(0, dp(20), 0, 0);
        col.addView(privacy);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0B132B);
        scroll.addView(col);
        setContentView(scroll);

        // Start on the step the user most likely needs next.
        refresh();
        (serviceEnabled() ? keyButton : serviceButton).requestFocus();
    }

    private Button button(LinearLayout parent, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setPadding(dp(20), dp(12), dp(20), dp(12));
        b.setBackground(buttonBackground());
        b.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        parent.addView(b, lp);
        return b;
    }

    private StateListDrawable buttonBackground() {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(0xFF1D3557);
        normal.setCornerRadius(dp(10));
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0xFF2E5A8A);
        focused.setCornerRadius(dp(10));
        focused.setStroke(dp(3), 0xFFF4B942);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] {android.R.attr.state_focused}, focused);
        s.addState(new int[] {}, normal);
        return s;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Prefs.get(this).registerOnSharedPreferenceChangeListener(this);
        refresh();
    }

    @Override
    protected void onPause() {
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(this);
        // Don't leave the service stuck waiting for a button if setup is left mid-way.
        if (Prefs.learning(this)) Prefs.get(this).edit().putBoolean(Prefs.LEARNING, false).apply();
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String key) {
        refresh();
    }

    private boolean serviceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName me = new ComponentName(this, ShieldService.class);
        for (String s : enabled.split(":")) {
            ComponentName c = ComponentName.unflattenFromString(s);
            if (me.equals(c)) return true;
        }
        return false;
    }

    private void refresh() {
        boolean on = serviceEnabled();
        int key = Prefs.keyCode(this);
        boolean learning = Prefs.learning(this);

        if (!on) {
            status.setText("Step 1: turn on Commercial Shield in Accessibility settings.");
        } else if (learning) {
            status.setText("Press the remote button you want to use as the shield button…");
        } else if (key < 0) {
            status.setText("Step 2: pick which remote button raises the shield.");
        } else {
            status.setText("Ready. Press " + Prefs.keyName(key) + " during a commercial. Press it again (or Back) to return.");
        }

        serviceButton.setText("Accessibility service:  " + (on ? "On ✓" : "Off — open settings"));
        keyButton.setText(learning ? "Shield button:  waiting for a button press…" : "Shield button:  " + Prefs.keyName(key) + "  — change");
        keyButton.setEnabled(on);
        int m = Prefs.autoOffMin(this);
        autoOffButton.setText("Auto-lift after:  " + (m == 0 ? "never" : m + " min"));
        muteButton.setText("Mute sound:  " + (Prefs.mute(this) ? "On" : "Off"));
        styleButton.setText("Cover:  " + (Prefs.STYLE_CLOCK.equals(Prefs.style(this)) ? "Clock + timer" : "Plain black"));
        testButton.setText("Preview the cover (press Back to close)");
        testButton.setEnabled(on);
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void startLearning() {
        Prefs.get(this).edit().putBoolean(Prefs.LEARNING, true).apply();
    }

    private void cycleAutoOff() {
        int cur = Prefs.autoOffMin(this);
        int[] c = Prefs.AUTO_OFF_CHOICES;
        int next = c[0];
        for (int i = 0; i < c.length; i++) {
            if (c[i] == cur) { next = c[(i + 1) % c.length]; break; }
        }
        Prefs.get(this).edit().putInt(Prefs.AUTO_OFF_MIN, next).apply();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
