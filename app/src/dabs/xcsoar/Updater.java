package dabs.xcsoar;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dabs.core.Country;
import dabs.core.LocalGeometry;

/** XCSoar-Ordner finden, Länder laden, Dateien schreiben, Status merken.
 *  Aktualisiert wird nur, wenn die App geöffnet wird – keine Hintergrund-Jobs. */
public final class Updater {

    /** Datenordner aller bekannten XCSoar-Varianten (Play, F-Droid/FOSS, Testing, OpenSoar, alt). */
    static File[] candidates() {
        File root = Environment.getExternalStorageDirectory();
        return new File[]{
                new File(root, "Android/media/org.xcsoar.foss"),
                new File(root, "Android/media/org.xcsoar"),
                new File(root, "Android/media/org.xcsoar.testing"),
                new File(root, "Android/media/de.opensoar"),
                new File(root, "XCSoarData"),
        };
    }

    /** Zielordner je Installation: "airspace"-Unterordner (XCSoar 7.45+) oder der Datenordner selbst. */
    static List<File> targetDirs() {
        List<File> out = new ArrayList<>();
        for (File d : candidates()) {
            if (!d.isDirectory()) continue;
            File asp = new File(d, "airspace");
            out.add(asp.isDirectory() ? asp : d);
        }
        return out;
    }

    /** Alle Ordner mit Luftraumdateien (Datenordner + "airspace") – Quelle für echte Umrisse. */
    static List<File> localAirspaceDirs() {
        List<File> out = new ArrayList<>();
        for (File d : candidates()) {
            if (!d.isDirectory()) continue;
            out.add(d);
            File asp = new File(d, "airspace");
            if (asp.isDirectory()) out.add(asp);
        }
        return out;
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("status", Context.MODE_PRIVATE);
    }

    static boolean enabled(Context c, Country k) {
        return prefs(c).getBoolean("on_" + k.name(), k == Country.CH);
    }

    static void setEnabled(Context c, Country k, boolean on) {
        prefs(c).edit().putBoolean("on_" + k.name(), on).apply();
    }

    static File pdfFile(Context c) {
        return new File(c.getCacheDir(), "dabs.pdf");
    }

    /** Läuft im Hintergrund-Thread. Gibt den Statustext zurück. */
    static String runUpdate(Context c) {
        String now = new SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY).format(new Date());
        StringBuilder b = new StringBuilder();
        List<File> dirs = targetDirs();
        if (dirs.isEmpty()) {
            String msg = "FEHLER " + now + ": XCSoar-Datenordner nicht gefunden. XCSoar einmal starten?";
            prefs(c).edit().putString("last", msg).apply();
            return msg;
        }
        LocalDate day = Country.today();
        LocalGeometry local = null;
        for (Country k : Country.values()) {
            if (enabled(c, k) && k.usesLocalGeometry() && local == null) {
                local = LocalGeometry.scan(localAirspaceDirs());
                b.append("Lokale Luftraumdateien für Umrisse: ").append(local.files).append("\n\n");
            }
            if (!enabled(c, k)) {
                // abgewählt: eigene Datei entfernen, damit XCSoar keine veralteten Gebiete zeigt
                if (k != Country.CH) for (File d : dirs) new File(d, k.fileName).delete();
                continue;
            }
            b.append("■ ").append(k.label).append(" → ").append(k.fileName).append('\n');
            try {
                Country.Result r = k.fetch(day, local);
                for (File d : dirs) Country.write(new File(d, k.fileName), r.text);
                if (r.pdf != null) savePdf(c, r.pdf);
                b.append("OK ").append(now).append(": ").append(r.count).append(" Gebiete. ").append(r.stand).append('\n');
                for (String line : r.report) b.append("  ").append(line).append('\n');
            } catch (Throwable t) {
                b.append("FEHLER ").append(now).append(": ").append(t.getMessage()).append('\n');
                b.append("  (alte Datei bleibt – Datum im Gebietsnamen prüfen)\n");
            }
            b.append('\n');
        }
        b.append("Ordner:\n");
        for (File d : dirs) b.append("  ").append(d).append('\n');
        String msg = b.toString();
        prefs(c).edit().putString("last", msg).apply();
        return msg;
    }

    static void savePdf(Context c, byte[] pdf) {
        try (FileOutputStream o = new FileOutputStream(pdfFile(c))) {
            o.write(pdf);
        } catch (Exception ignored) {
            // Anzeige des PDFs ist optional
        }
    }

    /** Entfernt geplante Hintergrund-Jobs älterer App-Versionen (alle 2 h). */
    static void cancelOldJobs(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js != null) js.cancelAll();
    }

    static boolean hasStorageAccess(Context c) {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return c.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
}
