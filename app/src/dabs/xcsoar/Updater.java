package dabs.xcsoar;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dabs.core.Country;
import dabs.core.LocalGeometry;

/**
 * Lädt die Länder parallel, schreibt die Dateien in die gewählten XCSoar-Installationen und
 * merkt sich je Land den Status (lädt / ok / Fehler). Die Oberfläche wird per Listener benachrichtigt.
 * Aktualisiert wird nur, wenn die App geöffnet wird – keine Hintergrund-Jobs.
 */
public final class Updater {

    // --- XCSoar-Installationen --------------------------------------------

    /** Eine mögliche XCSoar-Installation (Paket + Datenordner). */
    static final class Install {
        final String pkg, label;
        final File dataDir;

        Install(String pkg, String label, File dataDir) {
            this.pkg = pkg;
            this.label = label;
            this.dataDir = dataDir;
        }

        boolean exists() { return dataDir.isDirectory(); }

        /** Zielordner: "airspace"-Unterordner (XCSoar 7.45+) oder der Datenordner selbst. */
        File target() {
            File asp = new File(dataDir, "airspace");
            return asp.isDirectory() ? asp : dataDir;
        }

        String key() { return "inst_" + (pkg != null ? pkg : "legacy"); }
    }

    static List<Install> installs() {
        File root = Environment.getExternalStorageDirectory();
        List<Install> l = new ArrayList<>();
        l.add(new Install("org.xcsoar.foss", "XCSoar (F-Droid/FOSS)", new File(root, "Android/media/org.xcsoar.foss")));
        l.add(new Install("org.xcsoar", "XCSoar", new File(root, "Android/media/org.xcsoar")));
        l.add(new Install("org.xcsoar.testing", "XCSoar Testing", new File(root, "Android/media/org.xcsoar.testing")));
        l.add(new Install("de.opensoar", "OpenSoar", new File(root, "Android/media/de.opensoar")));
        l.add(new Install(null, L.s("legacy_dir"), new File(root, "XCSoarData")));
        return l;
    }

    /** Versionsname der installierten App, z.B. "7.45.1", oder null. */
    static String version(Context c, Install i) {
        if (i.pkg == null) return null;
        try {
            PackageInfo p = c.getPackageManager().getPackageInfo(i.pkg, 0);
            return p.versionName;
        } catch (Exception e) {
            return null;
        }
    }

    static boolean installEnabled(Context c, Install i) {
        return prefs(c).getBoolean(i.key(), true);
    }

    static void setInstallEnabled(Context c, Install i, boolean on) {
        prefs(c).edit().putBoolean(i.key(), on).apply();
    }

    /** Gewählte, vorhandene Installationen. */
    static List<Install> activeInstalls(Context c) {
        List<Install> out = new ArrayList<>();
        for (Install i : installs()) if (i.exists() && installEnabled(c, i)) out.add(i);
        return out;
    }

    /** Luftraumdateien, die im aktiven Profil der gewählten Installationen eingebunden sind. */
    static List<File> configuredAirspaceFiles(Context c) {
        List<File> out = new ArrayList<>();
        for (Install i : activeInstalls(c))
            for (File f : dabs.core.XcsoarProfile.read(i.dataDir).files) if (!out.contains(f)) out.add(f);
        return out;
    }

    // --- Einstellungen & Status -------------------------------------------

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("status", Context.MODE_PRIVATE);
    }

    static boolean enabled(Context c, Country k) {
        return prefs(c).getBoolean("on_" + k.name(), k == Country.CH);
    }

    static void setEnabled(Context c, Country k, boolean on) {
        prefs(c).edit().putBoolean("on_" + k.name(), on).apply();
    }

    enum State { OFF, RUNNING, OK, ERROR, INTERRUPTED, NEVER }

    /** Gespeicherter Status eines Landes. */
    static final class Status {
        State state = State.NEVER;
        int count;
        String time = "", stand = "", report = "", error = "", localFiles = "";
    }

    static Status status(Context c, Country k) {
        SharedPreferences p = prefs(c);
        Status s = new Status();
        if (!enabled(c, k)) { s.state = State.OFF; return s; }
        String st = p.getString("st_" + k.name(), "never");
        s.count = p.getInt("cnt_" + k.name(), 0);
        s.time = p.getString("time_" + k.name(), "");
        s.stand = p.getString("stand_" + k.name(), "");
        s.report = p.getString("rep_" + k.name(), "");
        s.error = p.getString("err_" + k.name(), "");
        s.localFiles = p.getString("local_" + k.name(), "");
        switch (st) {
            case "ok": s.state = State.OK; break;
            case "err": s.state = State.ERROR; break;
            case "run":
                // läuft nur, wenn es dieser Prozess gerade tut – sonst wurde die App beendet
                boolean mine = p.getInt("pid_" + k.name(), -1) == android.os.Process.myPid() && running(k);
                s.state = mine ? State.RUNNING : State.INTERRUPTED;
                break;
            default: s.state = State.NEVER;
        }
        return s;
    }

    // --- Laden ---------------------------------------------------------------

    interface Listener { void changed(); }

    private static final ExecutorService POOL = Executors.newFixedThreadPool(Country.values().length);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final java.util.Set<Country> RUNNING = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static Listener listener;
    private static LocalGeometry local;      // pro Durchlauf einmal gelesen
    private static List<File> localList = new ArrayList<>();

    static void setListener(Listener l) { listener = l; }

    static boolean running(Country k) { return RUNNING.contains(k); }

    static boolean anyRunning() { return !RUNNING.isEmpty(); }

    private static void notifyUi() {
        MAIN.post(() -> { if (listener != null) listener.changed(); });
    }

    /** Alle gewählten Länder neu laden (parallel); abgewählte: eigene Dateien entfernen. */
    /** Wurde in diesem App-Prozess schon automatisch geladen? (Prozess neu = App neu gestartet) */
    static boolean startedInThisProcess;

    static void updateAll(Context ctx) {
        startedInThisProcess = true;
        Context c = ctx.getApplicationContext();
        prefs(c).edit().putString("lastRunDay", today()).apply();
        dabs.core.Http.cacheDir = new File(c.getCacheDir(), "http");
        synchronized (Updater.class) { local = null; }
        for (Country k : Country.values()) {
            if (enabled(c, k)) update(c, k);
            else removeFiles(c, k);
        }
    }

    /** Ein Land neu laden. */
    static void update(Context ctx, Country k) {
        Context c = ctx.getApplicationContext();
        if (!RUNNING.add(k)) return; // läuft schon
        dabs.core.Http.cacheDir = new File(c.getCacheDir(), "http");
        prefs(c).edit().putString("st_" + k.name(), "run").putInt("pid_" + k.name(), android.os.Process.myPid()).apply();
        notifyUi();
        POOL.execute(() -> {
            String now = new SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY).format(new Date());
            SharedPreferences.Editor e = prefs(c).edit();
            try {
                List<Install> targets = activeInstalls(c);
                if (targets.isEmpty())
                    throw new Exception(L.s("err_no_install"));
                LocalGeometry lg = k.usesLocalGeometry() ? localGeometry(c) : null;
                Country.Result r = k.fetch(Country.today(), lg);
                for (Install i : targets) Country.write(new File(i.target(), k.fileName), r.text);
                if (r.pdf != null) savePdf(c, r.pdf);
                e.putString("st_" + k.name(), "ok")
                        .putInt("cnt_" + k.name(), r.count)
                        .putString("time_" + k.name(), now)
                        .putString("stand_" + k.name(), r.stand == null ? "" : r.stand)
                        .putString("rep_" + k.name(), String.join("\n", r.report))
                        .putString("err_" + k.name(), "")
                        .putString("local_" + k.name(), lg == null ? "" : names(localList));
            } catch (Throwable t) {
                e.putString("st_" + k.name(), "err")
                        .putString("time_" + k.name(), now)
                        .putString("err_" + k.name(), t.getMessage() != null ? t.getMessage() : t.toString());
            }
            e.apply();
            RUNNING.remove(k);
            notifyUi();
        });
    }

    /** Eingebundene Luftraumdateien nur einmal pro Durchlauf lesen (CH, CZ, AT, FR teilen sich das). */
    private static synchronized LocalGeometry localGeometry(Context c) {
        if (local == null) {
            localList = configuredAirspaceFiles(c);
            local = LocalGeometry.scanFiles(localList);
        }
        return local;
    }

    private static String names(List<File> fs) {
        List<String> n = new ArrayList<>();
        for (File f : fs) if (!n.contains(f.getName())) n.add(f.getName());
        return String.join(", ", n);
    }

    /** Abgewählt: Datei in allen Installationen entfernen, damit XCSoar keine veralteten Gebiete zeigt. */
    static void removeFiles(Context c, Country k) {
        for (Install i : installs()) if (i.exists()) new File(i.target(), k.fileName).delete();
        prefs(c).edit().putString("st_" + k.name(), "off").apply();
    }

    /** Installation abgewählt: eigene Dateien dort entfernen. */
    static void removeAllFiles(Install i) {
        if (!i.exists()) return;
        for (Country k : Country.values()) new File(i.target(), k.fileName).delete();
    }

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

    /** "22.11. 11:21" -> "heute 11:21" bzw. "21.11. 18:05" */
    static String when(String stored) {
        if (stored == null || stored.length() < 7) return stored == null ? "" : stored;
        String today = new SimpleDateFormat("dd.MM.", Locale.GERMANY).format(new Date());
        return stored.startsWith(today) ? L.s("st_today", stored.substring(7).trim()) : stored;
    }

    /** Wurde heute schon geladen? (sonst beim Öffnen automatisch laden) */
    static boolean loadedToday(Context c) {
        return today().equals(prefs(c).getString("lastRunDay", ""));
    }

    static String today() {
        LocalDate d = Country.today();
        return String.format(Locale.GERMANY, "%02d.%02d.%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }
}
