package dabs.xcsoar;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import dabs.core.Country;

/** Details eines Landes: Status, Gebietsliste, Fehler, Herkunft der Grenzen, Quelle. */
public class DetailActivity extends Activity implements Updater.Listener {
    private Ui ui;
    private Country k;
    private LinearLayout body;

    @Override
    protected void onCreate(Bundle b) {
        Ui.theme(this);
        super.onCreate(b);
        ui = new Ui(this);
        k = Country.valueOf(getIntent().getStringExtra("country"));
        LinearLayout col = ui.page();
        ui.backHeader(col, L.country(k));
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        col.addView(body);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Updater.setListener(this);
        changed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Updater.setListener(null);
    }

    @Override
    public void changed() {
        body.removeAllViews();
        Updater.Status s = Updater.status(this, k);

        // Status
        LinearLayout st = ui.card(false);
        st.addView(ui.statusIcon(s.state));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(ui.dp(12), 0, 0, 0);
        t.addView(ui.text(stateText(s.state), 17, ui.text, true));
        t.addView(ui.text(MainActivity.subline(s), 13, ui.sub, false));
        st.addView(t, Ui.weight1());
        body.addView(st);

        // Knöpfe
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(0, ui.dp(10), 0, 0);
        Button src = ui.button(k == Country.CH ? L.s("open_dabs_pdf") : L.s("open_source"), true);
        src.setOnClickListener(v -> MainActivity.openSource(this, k));
        btns.addView(src);
        if (s.state != Updater.State.OFF) {
            Button again = ui.button(L.s("reload"), false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = ui.dp(10);
            again.setLayoutParams(lp);
            String blocked = Updater.blockedReason(this, k);
            again.setEnabled(s.state != Updater.State.RUNNING && blocked == null);
            if (blocked != null) again.setAlpha(0.5f);
            again.setOnClickListener(v -> { Updater.update(this, k); changed(); });
            btns.addView(again);
        }
        body.addView(btns);
        String why = Updater.blockedReason(this, k);
        if (why != null && s.state == Updater.State.OK) {
            TextView w = ui.text(why, 13, ui.sub, false);
            w.setPadding(ui.dp(4), ui.dp(8), ui.dp(4), 0);
            body.addView(w);
        }

        // Fehler
        if (s.state == Updater.State.ERROR && !s.error.isEmpty()) {
            body.addView(ui.section(L.s("sec_error")));
            LinearLayout c = ui.card(true);
            c.addView(ui.text(L.core(s.error), 14, Ui.RED, false));
            c.addView(ui.text(L.s("keep_old"), 13, ui.sub, false));
            body.addView(c);
        }

        // Info
        body.addView(ui.section(L.s("sec_info")));
        LinearLayout info = ui.card(true);
        info.addView(kv(L.s("k_source"), k.source));
        info.addView(kv(L.s("k_file"), k.fileName));
        if (!s.stand.isEmpty()) info.addView(kv(L.s("k_stand"), L.core(s.stand)));
        if (k == Country.CH) info.addView(kv(L.s("k_next_dabs"), L.s("next_dabs")));
        if (k.usesLocalGeometry()) info.addView(kv(L.s("k_local"),
                s.localFiles.isEmpty() ? L.s("no_local") : s.localFiles));
        body.addView(info);

        // Gebiete
        if (s.state == Updater.State.OK || (!s.report.isEmpty() && s.state != Updater.State.OFF)) {
            body.addView(ui.section(L.s("sec_areas", s.count)));
            LinearLayout c = ui.card(true);
            HorizontalScrollView hs = new HorizontalScrollView(this);
            TextView rep = ui.text(s.report.isEmpty() ? L.s("no_areas") : L.core(s.report), 12, ui.text, false);
            rep.setTypeface(Typeface.MONOSPACE);
            rep.setTextIsSelectable(true);
            hs.addView(rep);
            c.addView(hs);
            body.addView(c);
            TextView hint = ui.text(L.s("areas_hint"), 12, ui.sub, false);
            hint.setPadding(ui.dp(4), ui.dp(8), ui.dp(4), 0);
            body.addView(hint);
        }
    }

    private LinearLayout kv(String key, String value) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, ui.dp(4), 0, ui.dp(4));
        l.addView(ui.text(key, 12, ui.sub, false));
        TextView v = ui.text(value, 14, ui.text, false);
        v.setTextIsSelectable(true);
        l.addView(v);
        return l;
    }

    static String stateText(Updater.State st) {
        switch (st) {
            case OK: return L.s("state_ok");
            case RUNNING: return L.s("state_running");
            case ERROR: return L.s("state_error");
            case INTERRUPTED: return L.s("state_interrupted");
            case OFF: return L.s("state_off");
            default: return L.s("state_never");
        }
    }
}
