package dabs.xcsoar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity {
    private TextView status;
    private Button grant, update;
    private boolean running;
    private boolean backFromPdf; // nach dem PDF-Betrachter nicht erneut laden

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("DABS → XCSoar");
        title.setTextSize(22);
        l.addView(title);

        TextView info = new TextView(this);
        info.setText("Lädt beim Öffnen der App die heute aktiven Militär-/Sperrgebiete der gewählten Länder "
                + "und schreibt je Land eine OpenAir-Datei in alle XCSoar-Ordner (bei XCSoar 7.45+ in „airspace“).\n"
                + "In XCSoar einmalig unter Konfiguration → Standort-Dateien → Luftraum die Dateien "
                + "zusätzlich auswählen.\n"
                + "Tipp: Die Gebiete haben die Luftraumklasse TFR – in XCSoar unter Luftraum → Farben "
                + "z. B. violett einstellen, dann heben sie sich von der Standarddatei ab.\n");
        l.addView(info);

        TextView lt = new TextView(this);
        lt.setText("Länder:");
        l.addView(lt);
        for (dabs.core.Country k : dabs.core.Country.values()) {
            CheckBox cb = new CheckBox(this);
            cb.setText(k.label + "  (" + k.fileName + ")");
            cb.setChecked(Updater.enabled(this, k));
            cb.setOnCheckedChangeListener((v, on) -> Updater.setEnabled(this, k, on));
            l.addView(cb);
        }

        grant = new Button(this);
        grant.setText("Dateizugriff erlauben");
        grant.setOnClickListener(v -> requestStorage());
        l.addView(grant);

        update = new Button(this);
        update.setText("Jetzt aktualisieren");
        update.setOnClickListener(v -> runNow());
        l.addView(update);

        Button pdf = new Button(this);
        pdf.setText("Original-DABS anzeigen (PDF)");
        pdf.setOnClickListener(v -> openPdf());
        l.addView(pdf);

        status = new TextView(this);
        status.setTypeface(Typeface.MONOSPACE);
        status.setTextSize(12);
        status.setTextIsSelectable(true);
        l.addView(status);

        ScrollView sv = new ScrollView(this);
        sv.addView(l);
        setContentView(sv);

    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean ok = Updater.hasStorageAccess(this);
        grant.setVisibility(ok ? View.GONE : View.VISIBLE);
        update.setEnabled(ok);
        showStatus(null);
        if (ok) {
            Updater.cancelOldJobs(this);
            if (backFromPdf) backFromPdf = false;
            else runNow(); // jedes Öffnen der App holt die aktuellen Daten
        }
    }

    private void showStatus(String fresh) {
        StringBuilder s = new StringBuilder();
        if (!Updater.hasStorageAccess(this)) s.append("⚠ Dateizugriff fehlt – bitte erlauben.\n\n");
        if (fresh != null) s.append(fresh);
        else {
            String last = Updater.prefs(this).getString("last", null);
            s.append(last != null ? last : "Noch kein Update gelaufen.");
        }
        status.setText(s);
    }

    private void runNow() {
        if (running) return;
        running = true;
        update.setEnabled(false);
        status.setText("Lade Daten …");
        new Thread(() -> {
            String r = Updater.runUpdate(getApplicationContext());
            runOnUiThread(() -> { running = false; update.setEnabled(true); showStatus(r); });
        }).start();
    }

    /** Zeigt das zuletzt geladene Original-PDF; ohne lokale Kopie die Skyguide-Seite. */
    private void openPdf() {
        File f = Updater.pdfFile(this);
        if (f.isFile() && f.length() > 0) {
            Intent i = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.parse("content://" + PdfProvider.AUTHORITY + "/dabs.pdf"), "application/pdf")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                backFromPdf = true;
                startActivity(i);
                return;
            } catch (Exception ignored) {
                backFromPdf = false; // kein PDF-Betrachter -> Browser
            }
        }
        backFromPdf = true;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(dabs.core.Converter.DABS_URL + "today")));
    }

    private void requestStorage() {
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
}
