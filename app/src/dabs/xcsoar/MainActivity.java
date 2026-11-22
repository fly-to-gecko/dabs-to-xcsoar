package dabs.xcsoar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import dabs.core.Country;

/** Startseite: Länder mit Häkchen, Ampel je Land und Link zur Originalquelle. */
public class MainActivity extends Activity implements Updater.Listener {
    /** true, wenn wir selbst eine andere Seite öffnen – dann beim Zurückkommen nicht neu laden. */
    static boolean skipReload;
    /** von den Einstellungen gesetzt, wenn neu geladen werden soll */
    static boolean dirty;

    private Ui ui;
    private LinearLayout list;
    private View storageCard;
    private Button reloadBtn;

    @Override
    protected void onCreate(Bundle b) {
        Ui.theme(this);
        super.onCreate(b);
        ui = new Ui(this);
        LinearLayout col = ui.pageWithBottom();

        // Kopf: Logo, Titel, Aktualisieren, Einstellungen
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, ui.dp(4), 0, ui.dp(6));
        head.addView(ui.logo(56));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(ui.dp(12), 0, ui.dp(8), 0);
        titles.addView(ui.text("DABS für XCSoar", 21, ui.text, true));
        titles.addView(ui.text("Aktive Militär-Lufträume · " + Updater.today(), 13, ui.sub, false));
        head.addView(titles, Ui.weight1());
        TextView gear = ui.iconButton("⚙");
        gear.setContentDescription("Einstellungen");
        gear.setOnClickListener(v -> {
            skipReload = true;
            startActivity(new Intent(this, SettingsActivity.class));
        });
        head.addView(gear);
        col.addView(head);

        // Hinweis, wenn der Dateizugriff fehlt
        LinearLayout sc = ui.card(true);
        sc.addView(ui.text("Dateizugriff fehlt", 16, Ui.RED, true));
        TextView st = ui.text("Die App braucht „Zugriff auf alle Dateien“, um die Lufträume in den XCSoar-Ordner zu schreiben.", 14, ui.sub, false);
        st.setPadding(0, ui.dp(4), 0, ui.dp(8));
        sc.addView(st);
        Button grant = ui.button("Dateizugriff erlauben", true);
        grant.setOnClickListener(v -> requestStorage());
        sc.addView(grant);
        storageCard = sc;
        col.addView(sc);

        col.addView(ui.section("Länder"));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        col.addView(list);

        // großer Knopf unten (fest, scrollt nicht weg)
        reloadBtn = ui.bigButton("⟳  Alle neu laden");
        reloadBtn.setOnClickListener(v -> reload());
        ui.bottom.addView(reloadBtn);

        TextView foot = ui.text("Version " + versionName() + " (Beta) · ohne Gewähr – maßgebend sind AIP, DABS/AUP und NOTAM",
                12, ui.sub, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(ui.dp(8), ui.dp(20), ui.dp(8), 0);
        col.addView(foot);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Updater.setListener(this);
        boolean ok = Updater.hasStorageAccess(this);
        storageCard.setVisibility(ok ? View.GONE : View.VISIBLE);
        if (ok) {
            Updater.cancelOldJobs(this);
            if (dirty || !skipReload) reload(); // jedes Öffnen der App holt die aktuellen Daten
        }
        skipReload = false;
        dirty = false;
        changed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Updater.setListener(null);
    }

    private void reload() {
        if (!Updater.hasStorageAccess(this)) {
            Toast.makeText(this, "Bitte zuerst den Dateizugriff erlauben", Toast.LENGTH_SHORT).show();
            return;
        }
        Updater.updateAll(this);
        changed();
    }

    /** Länderliste neu zeichnen (vom Updater bei jeder Statusänderung aufgerufen). */
    @Override
    public void changed() {
        list.removeAllViews();
        int on = 0, done = 0;
        for (Country k : Country.values()) {
            list.addView(row(k));
            if (Updater.enabled(this, k)) {
                on++;
                if (!Updater.running(k)) done++;
            }
        }
        // großer Knopf zeigt den Fortschritt
        if (Updater.anyRunning()) {
            reloadBtn.setText("Lädt …  " + done + " von " + on + " fertig");
            reloadBtn.setAlpha(0.7f);
        } else {
            reloadBtn.setText("⟳  Alle neu laden");
            reloadBtn.setAlpha(1f);
        }
    }

    private View row(Country k) {
        Updater.Status s = Updater.status(this, k);
        LinearLayout c = ui.card(false);

        CheckBox cb = new CheckBox(this);
        cb.setChecked(s.state != Updater.State.OFF);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        cb.setOnCheckedChangeListener((v, on) -> {
            Updater.setEnabled(this, k, on);
            if (on) { if (Updater.hasStorageAccess(this)) Updater.update(this, k); }
            else Updater.removeFiles(this, k);
            changed();
        });
        c.addView(cb);

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(ui.dp(4), 0, ui.dp(8), 0);
        mid.addView(ui.text(k.label, 17, s.state == Updater.State.OFF ? ui.sub : ui.text, true));
        TextView line = ui.text(subline(s), 13, s.state == Updater.State.ERROR ? Ui.RED : ui.sub, false);
        mid.addView(line);
        c.addView(mid, Ui.weight1());

        View icon = ui.statusIcon(s.state);
        c.addView(icon);

        Button src = ui.button(k == Country.CH ? "DABS-PDF" : "Quelle", false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ui.dp(48));
        lp.leftMargin = ui.dp(10);
        src.setLayoutParams(lp);
        src.setOnClickListener(v -> openSource(this, k));
        c.addView(src);

        c.setOnClickListener(v -> {
            skipReload = true;
            startActivity(new Intent(this, DetailActivity.class).putExtra("country", k.name()));
        });
        return c;
    }

    static String subline(Updater.Status s) {
        switch (s.state) {
            case OFF: return "aus";
            case RUNNING: return "lädt …";
            case OK: {
                String t = s.count + (s.count == 1 ? " Gebiet" : " Gebiete") + " · " + Updater.when(s.time);
                if (s.report.contains("Grenzen aus")) t += " · mit Ersatz-Grenzen";
                return t;
            }
            case ERROR: return "Fehler · antippen für Details";
            case INTERRUPTED: return "abgebrochen – unten neu laden";
            default: return "noch nicht geladen";
        }
    }

    /** Originalquelle öffnen; Schweiz: genau das ausgewertete DABS-PDF. */
    static void openSource(Activity a, Country k) {
        skipReload = true;
        if (k == Country.CH) {
            File f = Updater.pdfFile(a);
            if (f.isFile() && f.length() > 0) {
                Intent i = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(Uri.parse("content://" + PdfProvider.AUTHORITY + "/dabs.pdf"), "application/pdf")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    a.startActivity(i);
                    return;
                } catch (Exception ignored) {
                    // kein PDF-Betrachter -> Browser
                }
            }
        }
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(k.link)));
        } catch (Exception e) {
            skipReload = false;
            Toast.makeText(a, "Kein Browser gefunden", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestStorage() {
        skipReload = false; // nach dem Erlauben gleich laden
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
