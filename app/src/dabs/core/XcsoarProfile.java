package dabs.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Findet die Luftraumdateien, die in XCSoar tatsächlich eingebunden sind:
 * aktives Profil laut xcsoar.log ("Loaded profile from ..."), daraus AirspaceFileList (7.45+)
 * bzw. AirspaceFile/AdditionalAirspaceFile (ältere Versionen). Eigene Dateien der App zählen nicht.
 */
public final class XcsoarProfile {
    static final Pattern LOADED = Pattern.compile("Loaded profile from (\\S.*\\.prf)\\s*$");
    static final Pattern KEY = Pattern.compile("^(AirspaceFileList|AirspaceFile|AdditionalAirspaceFile)=\"(.*)\"\\s*$");

    /** Ergebnis: aktives Profil und die darin eingebundenen Dateien. */
    public static final class Config {
        public File profile;
        public List<File> files = new ArrayList<>();
    }

    /** @param dataDir Datenordner einer XCSoar-Installation (z.B. Android/media/org.xcsoar.foss) */
    public static Config read(File dataDir) {
        Config c = new Config();
        c.profile = activeProfile(dataDir);
        if (c.profile == null) return c;
        for (String line : lines(c.profile)) {
            Matcher m = KEY.matcher(line.trim());
            if (!m.matches()) continue;
            for (String entry : m.group(2).split("\\|")) {
                File f = resolve(dataDir, entry.trim());
                if (f == null) continue;
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.equals("dabs.txt") || n.startsWith("mil-")) continue; // eigene Dateien
                if (f.isFile() && !c.files.contains(f)) c.files.add(f);
            }
        }
        return c;
    }

    static File activeProfile(File dataDir) {
        String last = null;
        for (String log : new String[]{"xcsoar.log", "OpenSoar.log"})
            for (String line : lines(new File(dataDir, log))) {
                Matcher m = LOADED.matcher(line);
                if (m.find()) last = m.group(1);
            }
        if (last != null) {
            File f = new File(last);
            if (f.isFile()) return f;
            File g = new File(dataDir, last.substring(last.lastIndexOf('/') + 1));
            if (g.isFile()) return g;
        }
        for (File f : new File[]{new File(dataDir, "profiles/default.prf"), new File(dataDir, "default.prf")})
            if (f.isFile()) return f;
        return null;
    }

    /** "%LOCAL_PATH%\airspace/x.txt" -> <dataDir>/airspace/x.txt */
    static File resolve(File dataDir, String entry) {
        if (entry.isEmpty()) return null;
        String p = entry.replace("%LOCAL_PATH%", "").replace('\\', '/');
        while (p.startsWith("/") && !new File(p).isFile()) p = p.substring(1);
        File f = new File(p);
        return f.isAbsolute() ? f : new File(dataDir, p);
    }

    static List<String> lines(File f) {
        List<String> out = new ArrayList<>();
        if (!f.isFile()) return out;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), Charset.forName("UTF-8")))) {
            String l;
            while ((l = r.readLine()) != null) out.add(l);
        } catch (Exception ignored) {
            // nicht lesbar -> leer
        }
        return out;
    }
}
