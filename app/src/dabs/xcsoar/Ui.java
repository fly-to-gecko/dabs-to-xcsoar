package dabs.xcsoar;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

/** Gemeinsames Aussehen (Material-artig, hell/dunkel) ohne Zusatzbibliotheken. */
final class Ui {
    final Activity a;
    final boolean dark;
    final int bg, card, text, sub, accent, onAccent, line;
    static final int GREEN = 0xFF2E9D5B, RED = 0xFFD93A3A, ORANGE = 0xFFE08A00, GRAY = 0xFF9AA3AE;

    Ui(Activity a) {
        this.a = a;
        dark = (a.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        bg = dark ? 0xFF0F1419 : 0xFFF2F4F8;
        card = dark ? 0xFF1B222B : 0xFFFFFFFF;
        text = dark ? 0xFFE8EDF2 : 0xFF1B1F24;
        sub = dark ? 0xFF9AA7B5 : 0xFF5F6B7A;
        accent = dark ? 0xFF8FB4E3 : 0xFF1E3A5F;
        onAccent = dark ? 0xFF0F1419 : 0xFFFFFFFF;
        line = dark ? 0xFF2A333E : 0xFFE3E7ED;
    }

    /** Vor super.onCreate aufrufen. */
    static void theme(Activity a) {
        boolean dark = (a.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        a.setTheme(dark ? android.R.style.Theme_Material_NoActionBar : android.R.style.Theme_Material_Light_NoActionBar);
    }

    int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, a.getResources().getDisplayMetrics());
    }

    /** Grundgerüst: scrollbare Spalte auf Hintergrundfarbe. Gibt die Spalte zurück. */
    LinearLayout page() {
        Window w = a.getWindow();
        w.setStatusBarColor(bg);
        w.setNavigationBarColor(bg);
        if (!dark && Build.VERSION.SDK_INT >= 26)
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView sv = new ScrollView(a);
        sv.setBackgroundColor(bg);
        sv.setFillViewport(true);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(12), dp(16), dp(24));
        sv.addView(col);
        a.setContentView(sv);
        return col;
    }

    Drawable rounded(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    /** Karte mit Ripple-Effekt, wenn klickbar. */
    LinearLayout card(boolean vertical) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        c.setGravity(vertical ? Gravity.NO_GRAVITY : Gravity.CENTER_VERTICAL);
        c.setPadding(dp(14), dp(12), dp(12), dp(12));
        c.setBackground(new RippleDrawable(ColorStateList.valueOf(line), rounded(card, 16), rounded(Color.BLACK, 16)));
        c.setElevation(dp(1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        c.setLayoutParams(lp);
        return c;
    }

    TextView text(CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    TextView section(String s) {
        TextView t = text(s.toUpperCase(), 12, sub, true);
        t.setLetterSpacing(0.08f);
        t.setPadding(dp(4), dp(18), 0, dp(2));
        return t;
    }

    /** Gefüllter Knopf (Akzentfarbe) oder Umriss-Knopf. */
    Button button(String label, boolean filled) {
        Button b = new Button(a);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setMinHeight(dp(36));
        b.setMinimumHeight(dp(36));
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setStateListAnimator(null);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(18));
        if (filled) {
            g.setColor(accent);
            b.setTextColor(onAccent);
        } else {
            g.setColor(Color.TRANSPARENT);
            g.setStroke(dp(1), accent);
            b.setTextColor(accent);
        }
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(line), g, null));
        return b;
    }

    /** Runder Symbol-Knopf (z. B. ⟳ oder ⚙). */
    TextView iconButton(String glyph) {
        TextView t = text(glyph, 22, accent, false);
        t.setGravity(Gravity.CENTER);
        int s = dp(44);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(card);
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(line), g, null));
        t.setClickable(true);
        return t;
    }

    /** Ampel: drehend beim Laden, sonst farbiger Kreis mit Zeichen. */
    View statusIcon(Updater.State st) {
        int s = dp(28);
        if (st == Updater.State.RUNNING) {
            ProgressBar p = new ProgressBar(a, null, android.R.attr.progressBarStyleSmall);
            p.setIndeterminateTintList(ColorStateList.valueOf(accent));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
            p.setLayoutParams(lp);
            return p;
        }
        String g;
        int color;
        switch (st) {
            case OK: g = "✓"; color = GREEN; break;
            case ERROR: g = "✕"; color = RED; break;
            case INTERRUPTED: g = "!"; color = ORANGE; break;
            case OFF: g = ""; color = Color.TRANSPARENT; break;
            default: g = "–"; color = GRAY;
        }
        TextView t = text(g, 15, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        if (st == Updater.State.OFF) d.setStroke(dp(2), line);
        t.setBackground(d);
        return t;
    }

    /** Kopfzeile mit Zurück-Pfeil und Titel (für Unterseiten). */
    LinearLayout backHeader(LinearLayout col, String title) {
        LinearLayout h = new LinearLayout(a);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = iconButton("←");
        back.setOnClickListener(v -> a.finish());
        h.addView(back);
        TextView t = text(title, 22, text, true);
        t.setPadding(dp(10), 0, 0, 0);
        h.addView(t);
        col.addView(h);
        return h;
    }

    /** App-Logo (das adaptive Icon). */
    ImageView logo(int sizeDp) {
        ImageView iv = new ImageView(a);
        iv.setImageDrawable(a.getApplicationInfo().loadIcon(a.getPackageManager()));
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return iv;
    }

    static LinearLayout.LayoutParams weight1() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    static Context app(Activity a) { return a.getApplicationContext(); }
}
