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
        ui.backHeader(col, k.label);
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
        Button src = ui.button(k == Country.CH ? "Original-DABS (PDF)" : "Originalquelle öffnen", true);
        src.setOnClickListener(v -> MainActivity.openSource(this, k));
        btns.addView(src);
        if (s.state != Updater.State.OFF) {
            Button again = ui.button("Neu laden", false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = ui.dp(10);
            again.setLayoutParams(lp);
            again.setEnabled(s.state != Updater.State.RUNNING);
            again.setOnClickListener(v -> { Updater.update(this, k); changed(); });
            btns.addView(again);
        }
        body.addView(btns);

        // Fehler
        if (s.state == Updater.State.ERROR && !s.error.isEmpty()) {
            body.addView(ui.section("Fehler"));
            LinearLayout c = ui.card(true);
            c.addView(ui.text(s.error, 14, Ui.RED, false));
            c.addView(ui.text("Die Datei vom letzten erfolgreichen Laden bleibt bestehen – Datum im Gebietsnamen beachten.",
                    13, ui.sub, false));
            body.addView(c);
        }

        // Info
        body.addView(ui.section("Info"));
        LinearLayout info = ui.card(true);
        info.addView(kv("Quelle", k.source));
        info.addView(kv("Datei", k.fileName));
        if (!s.stand.isEmpty()) info.addView(kv("Stand", s.stand));
        if (k.usesLocalGeometry()) info.addView(kv("Ersatz-Grenzen aus",
                s.localFiles.isEmpty() ? "– (keine in XCSoar eingebundene Luftraumdatei)" : s.localFiles));
        body.addView(info);

        // Gebiete
        if (s.state == Updater.State.OK || (!s.report.isEmpty() && s.state != Updater.State.OFF)) {
            body.addView(ui.section("Gebiete heute (" + s.count + ")"));
            LinearLayout c = ui.card(true);
            HorizontalScrollView hs = new HorizontalScrollView(this);
            TextView rep = ui.text(s.report.isEmpty() ? "Heute keine Aktivierungen." : s.report, 12, ui.text, false);
            rep.setTypeface(Typeface.MONOSPACE);
            rep.setTextIsSelectable(true);
            hs.addView(rep);
            c.addView(hs);
            body.addView(c);
            TextView hint = ui.text("Zeiten in UTC. „Kreis ~x NM“ = ungefähre Lage aus dem NOTAM; "
                    + "„Grenzen aus …“ = Umriss aus einer in XCSoar eingebundenen Datei.", 12, ui.sub, false);
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
            case OK: return "Aktuell";
            case RUNNING: return "Wird geladen";
            case ERROR: return "Fehler beim Laden";
            case INTERRUPTED: return "Abgebrochen";
            case OFF: return "Ausgeschaltet";
            default: return "Noch nicht geladen";
        }
    }
}
