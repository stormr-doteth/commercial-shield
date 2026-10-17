package io.github.stormrdoteth.notinfrontofthekids;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Random;

/**
 * The full-screen cover: a big clock, "Commercial break" and how long the
 * break has been running. The text drifts a little every minute so it
 * never burns into OLED or plasma screens.
 */
class CoverView extends FrameLayout {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final long start = System.currentTimeMillis();
    private final LinearLayout column;
    private TextView elapsed;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (elapsed != null) {
                long s = (System.currentTimeMillis() - start) / 1000;
                elapsed.setText(String.format("Back to the show soon  ·  %d:%02d", s / 60, s % 60));
            }
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable drift = new Runnable() {
        @Override
        public void run() {
            float range = getWidth() * 0.03f;
            column.animate()
                    .translationX((random.nextFloat() * 2 - 1) * range)
                    .translationY((random.nextFloat() * 2 - 1) * range)
                    .setDuration(4000)
                    .start();
            handler.postDelayed(this, 60_000);
        }
    };

    CoverView(Context context, String style) {
        super(context);
        column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);

        if (Prefs.STYLE_BLACK.equals(style)) {
            setBackgroundColor(Color.BLACK);
        } else {
            setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[] {0xFF1D3557, 0xFF0B132B, 0xFF000000}));

            TextClock clock = new TextClock(context);
            clock.setFormat12Hour("h:mm");
            clock.setFormat24Hour("H:mm");
            clock.setTextColor(Color.WHITE);
            clock.setGravity(Gravity.CENTER);
            clock.setIncludeFontPadding(false);
            clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh(context, 18));
            clock.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
            column.addView(clock);

            TextView title = text(context, "Commercial break", 6, 0xFFFFFFFF);
            column.addView(title);

            elapsed = text(context, "", 3, 0xB3FFFFFF);
            elapsed.setPadding(0, (int) vh(context, 1.5f), 0, 0);
            column.addView(elapsed);
        }

        addView(column, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    private static float vh(Context c, float percent) {
        return c.getResources().getDisplayMetrics().heightPixels * percent / 100f;
    }

    private static TextView text(Context c, String s, float sizeVh, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh(c, sizeVh));
        return t;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(tick);
        handler.postDelayed(drift, 60_000);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }
}
