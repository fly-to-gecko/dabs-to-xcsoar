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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity {
    private TextView status;
    private Button grant, update;
    private boolean running;

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
        info.setText("Lädt beim Öffnen der App die tagesaktuellen Militär-Lufträume (Skyguide DABS) "
                + "und schreibt sie als dabs.txt in alle XCSoar-Ordner (bei XCSoar 7.45+ in „airspace“).\n"
                + "In XCSoar einmalig unter Konfiguration → Standort-Dateien → "
                + "Luftraum dabs.txt zusätzlich auswählen.\n");
        l.addView(info);

        grant = new Button(this);
        grant.setText("Dateizugriff erlauben");
        grant.setOnClickListener(v -> requestStorage());
        l.addView(grant);

        update = new Button(this);
        update.setText("Jetzt aktualisieren");
        update.setOnClickListener(v -> runNow());
        l.addView(update);

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
            runNow(); // jedes Öffnen der App holt die aktuellen Daten
        }
    }

    private void showStatus(String fresh) {
        StringBuilder s = new StringBuilder();
        if (!Updater.hasStorageAccess(this)) s.append("⚠ Dateizugriff fehlt – bitte erlauben.\n\n");
        java.util.List<File> ts = Updater.hasStorageAccess(this) ? Updater.targetFiles() : new java.util.ArrayList<>();
        s.append("Ziele:\n");
        if (ts.isEmpty()) s.append("(kein XCSoar-Ordner gefunden)\n");
        for (File f : ts) s.append("  ").append(f.getPath()).append('\n');
        s.append('\n');
        if (fresh != null) s.append(fresh);
        else {
            String last = Updater.prefs(this).getString("last", null);
            if (Updater.prefs(this).getBoolean("lastFailed", false))
                s.append(Updater.prefs(this).getString("lastError", "")).append("\n\nLetzter Erfolg:\n");
            s.append(last != null ? last : "Noch kein Update gelaufen.");
        }
        status.setText(s);
    }

    private void runNow() {
        if (running) return;
        running = true;
        update.setEnabled(false);
        status.setText("Lade DABS …");
        new Thread(() -> {
            String r = Updater.runUpdate(getApplicationContext());
            runOnUiThread(() -> { running = false; update.setEnabled(true); showStatus(r); });
        }).start();
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
