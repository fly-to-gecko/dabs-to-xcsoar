package dabs.xcsoar;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dabs.core.Converter;

/** Gemeinsame Logik: XCSoar-Ordner finden, aktualisieren, Status merken.
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

    /** Ziel je Installation: "airspace"-Unterordner (XCSoar 7.45+) oder der Datenordner selbst. */
    static List<File> targetFiles() {
        List<File> out = new ArrayList<>();
        for (File d : candidates()) {
            if (!d.isDirectory()) continue;
            File asp = new File(d, "airspace");
            out.add(new File(asp.isDirectory() ? asp : d, Converter.FILE_NAME));
        }
        return out;
    }

    static void copy(File from, File to) throws Exception {
        File tmp = new File(to.getParentFile(), to.getName() + ".tmp");
        try (FileInputStream in = new FileInputStream(from); FileOutputStream o = new FileOutputStream(tmp)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
        }
        if (!tmp.renameTo(to)) { to.delete(); if (!tmp.renameTo(to)) throw new Exception("Schreiben fehlgeschlagen: " + to); }
    }

    /** Lokale Kopie des Original-DABS, wird über PdfProvider angezeigt. */
    static File pdfFile(Context c) {
        return new File(c.getCacheDir(), "dabs.pdf");
    }

    static void savePdf(Context c, byte[] pdf) {
        try (FileOutputStream o = new FileOutputStream(pdfFile(c))) {
            o.write(pdf);
        } catch (Exception ignored) {
            // Anzeige des PDFs ist optional
        }
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("status", Context.MODE_PRIVATE);
    }

    /** Läuft im Hintergrund-Thread. Gibt Statustext zurück. */
    static String runUpdate(Context c) {
        String now = new SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY).format(new Date());
        SharedPreferences.Editor e = prefs(c).edit();
        try {
            List<File> targets = targetFiles();
            if (targets.isEmpty()) throw new Exception("XCSoar-Datenordner nicht gefunden. XCSoar einmal starten?");
            Converter.Summary s = Converter.run("today", targets.get(0));
            savePdf(c, s.pdf);
            StringBuilder b = new StringBuilder();
            b.append("OK ").append(now).append(": DABS ").append(s.date).append(" Version ").append(s.version)
                    .append(", ").append(s.count).append(" Aktivierungen\n");
            for (File t : targets) {
                try {
                    if (!t.equals(s.file)) copy(s.file, t);
                    b.append("→ ").append(t).append('\n');
                } catch (Exception ex) {
                    b.append("✗ ").append(t).append(": ").append(ex.getMessage()).append('\n');
                }
            }
            b.append('\n');
            for (String line : s.report) b.append(line).append('\n');
            String msg = b.toString();
            e.putString("last", msg).putString("lastOkDate", s.date).putBoolean("lastFailed", false).apply();
            return msg;
        } catch (Throwable t) {
            String msg = "FEHLER " + now + ": " + t.getMessage();
            e.putString("lastError", msg).putBoolean("lastFailed", true).apply();
            return msg;
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
