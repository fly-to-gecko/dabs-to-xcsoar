package dabs.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Umrisse aus den Luftraumdateien, die bereits im XCSoar-Ordner liegen (die eigenen Dateien
 * der Nutzerin). Damit werden NOTAM-Aktivierungen ohne Koordinaten ("LF-R201 ACTIVE") mit der
 * echten Gebietsform gezeichnet statt mit dem ungefähren Kreis aus der Q-Zeile.
 * Die Dateien werden nur gelesen, nichts davon wird weitergegeben.
 */
public final class LocalGeometry {
    /** Basis eines Gebietscodes: Landeskennung + R/D/P + Nummer, z.B. "LF-R 201", "LOD21", "ED-R112". */
    static final Pattern BASE = Pattern.compile(
            "\\b(LF|LO|ED|LK|LS|LI|LJ|LH|LZ|EP|EB|EH|EL)[- ]?([RDP])[- ]?(\\d{1,4})(?!\\d)");
    /** Teil-Suffix wie A, B1, C2 – nur als eigenständiges Kürzel (nicht der Anfang eines Wortes). */
    static final Pattern SUFFIX = Pattern.compile("^([A-Z]\\d?)(?![A-Za-z0-9])");
    static final Pattern SEP = Pattern.compile("^(\\s*(?:/|,|&|\\bAND\\b|\\bET\\b|\\bUND\\b)?\\s*)");

    private final Map<String, List<String>> byKey = new HashMap<>();
    private final Map<String, String> source = new HashMap<>();
    public int files;

    public boolean isEmpty() { return byKey.isEmpty(); }

    // --- Codes erkennen ----------------------------------------------------

    /** Ein Fund im Text: Basis (z.B. "LFR368") und die genannten Teile (z.B. A, B, C1). */
    static final class Ref {
        String base;
        List<String> parts = new ArrayList<>();
    }

    /** Alle Gebietscodes in einem Text, inkl. Teil-Listen ("LF-R368 A B C1 C2", "LF-D596 A/B/C"). */
    static List<Ref> refs(String text, boolean firstOnly) {
        List<Ref> out = new ArrayList<>();
        String t = text.toUpperCase(Locale.ROOT);
        Matcher m = BASE.matcher(t);
        while (m.find()) {
            Ref r = new Ref();
            r.base = m.group(1) + m.group(2) + Integer.parseInt(m.group(3));
            String rest = t.substring(m.end());
            // direkt angehängtes Suffix: "R40A", "R201A1"
            Matcher s = SUFFIX.matcher(rest);
            if (s.find()) {
                r.parts.add(s.group(1));
                rest = rest.substring(s.end());
            }
            // weitere Teile mit Trennzeichen: " B", "/C", " AND B", " ET B"
            while (true) {
                Matcher sep = SEP.matcher(rest);
                if (!sep.find() || sep.group(1).isEmpty()) break;
                String after = rest.substring(sep.end());
                Matcher s2 = SUFFIX.matcher(after);
                if (!s2.find()) break;
                r.parts.add(s2.group(1));
                rest = after.substring(s2.end());
                if (firstOnly) break;
            }
            if (firstOnly && r.parts.size() > 1) r.parts = r.parts.subList(0, 1);
            out.add(r);
            if (firstOnly) break;
        }
        return out;
    }

    // --- Index aufbauen ----------------------------------------------------

    /** Liest alle OpenAir-Dateien der Ordner (neueste zuerst); eigene App-Dateien werden ausgelassen. */
    public static LocalGeometry scan(List<File> dirs) {
        LocalGeometry g = new LocalGeometry();
        List<File> all = new ArrayList<>();
        for (File d : dirs) {
            File[] fs = d.listFiles();
            if (fs != null) all.addAll(Arrays.asList(fs));
        }
        Collections.sort(all, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (File f : all) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (!f.isFile() || f.length() > 30_000_000L) continue;
            if (!(n.endsWith(".txt") || n.endsWith(".air") || n.endsWith(".openair"))) continue;
            if (n.equals("dabs.txt") || n.startsWith("mil-")) continue;
            try {
                if (g.add(f)) g.files++;
            } catch (Exception ignored) {
                // unlesbare Datei überspringen
            }
        }
        return g;
    }

    boolean add(File f) throws Exception {
        boolean any = false;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                Charset.forName("ISO-8859-1")), 65536)) {
            String line, key = null;
            List<String> geom = null;
            while ((line = r.readLine()) != null) {
                int star = line.indexOf('*');
                String l = (star >= 0 ? line.substring(0, star) : line).trim();
                if (l.length() < 3) continue;
                char c0 = Character.toUpperCase(l.charAt(0)), c1 = Character.toUpperCase(l.charAt(1));
                if (c0 == 'A' && c1 == 'C') {
                    key = null;
                    geom = new ArrayList<>();
                } else if (c0 == 'A' && c1 == 'N' && geom != null) {
                    List<Ref> refs = refs(l.substring(2).trim(), true);
                    String name = l.substring(2).trim().toUpperCase(Locale.ROOT);
                    // nur wenn der Name mit dem Code beginnt (nicht nur erwähnt)
                    if (!refs.isEmpty() && BASE.matcher(name).lookingAt()) {
                        Ref rf = refs.get(0);
                        key = rf.base + (rf.parts.isEmpty() ? "" : rf.parts.get(0));
                        if (!byKey.containsKey(key)) {
                            byKey.put(key, geom);
                            source.put(key, f.getName());
                            any = true;
                        }
                    }
                } else if (geom != null && (c0 == 'D' && (c1 == 'P' || c1 == 'A' || c1 == 'B' || c1 == 'C')
                        || c0 == 'V' && (c1 == ' ' || c1 == '\t'))) {
                    geom.add(l);
                }
            }
        }
        return any;
    }

    // --- Nachschlagen ------------------------------------------------------

    /** Ergebnis: je gefundenem Teilgebiet Code + Geometrie + Quelldatei. */
    public static final class Hit {
        public String code, file;
        public List<String> geometry;
    }

    /**
     * Sucht alle im Text genannten Gebiete. Genannte Teile (A, B, C1...) werden einzeln gesucht;
     * ohne Teilangabe zuerst das Gebiet selbst, sonst alle seine Teile (LF-R250 -> LF-R250A, B).
     */
    public List<Hit> lookup(String text) {
        return lookup(text, null);
    }

    /** Wie {@link #lookup(String)}; genannte, aber nicht gefundene Teile kommen in {@code missing}. */
    public List<Hit> lookup(String text, List<String> missing) {
        List<Hit> hits = new ArrayList<>();
        List<String> done = new ArrayList<>();
        for (Ref r : refs(text, false)) {
            List<String> keys = new ArrayList<>();
            List<String> notFound = new ArrayList<>();
            for (String p : r.parts) {
                if (byKey.containsKey(r.base + p)) keys.add(r.base + p);
                else notFound.add(r.base + p);
            }
            if (keys.isEmpty()) {
                notFound.clear();
                if (byKey.containsKey(r.base)) keys.add(r.base);
                else for (String k : byKey.keySet())
                    if (k.startsWith(r.base) && k.length() > r.base.length()
                            && Character.isLetter(k.charAt(r.base.length()))) keys.add(k);
                Collections.sort(keys);
                if (keys.isEmpty()) notFound.add(r.base);
            }
            if (missing != null) for (String k : notFound) missing.add(pretty(k));
            for (String k : keys) {
                if (done.contains(k) || byKey.get(k).isEmpty()) continue;
                done.add(k);
                Hit h = new Hit();
                h.code = pretty(k);
                h.file = source.get(k);
                h.geometry = byKey.get(k);
                hits.add(h);
            }
        }
        return hits;
    }

    /** "LFR201A1" -> "LF-R201A1", "LOD21" -> "LOD21" (österr./schweiz. Schreibweise ohne Bindestrich). */
    static String pretty(String k) {
        String p = k.substring(0, 2);
        if (p.equals("LF") || p.equals("ED") || p.equals("LK")) return p + "-" + k.substring(2);
        return k;
    }
}
