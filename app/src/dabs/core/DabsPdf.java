package dabs.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

/** Liest das Skyguide-DABS-PDF und liefert die Aktivierungen. */
public final class DabsPdf {

    public static final class Entry {
        public String id;          // LSR7, LSD12, W3015/26
        public String notam;       // W3005/26 oder null
        public List<String[]> times = new ArrayList<>(); // {"0630","2100"} UTC
        public String lower, upper; // Rohtext aus DABS
        public double lat, lon;     // Mittelpunkt
        public double radiusNm;
        public String text = "";
        public boolean infoOnly;    // "Activities not shown on the DABS Chart Side"
    }

    public static final class Result {
        public String date;      // "2026 NOV 17"
        public String version;   // "3"
        public String generated; // "17.11.2026 11:45 UTC"
        public List<Entry> entries = new ArrayList<>();
    }

    public static class NotAvailableException extends Exception {
        public NotAvailableException() {
            super("DABS für dieses Datum ist noch nicht veröffentlicht (kommt ca. 16:00 Uhr am Vortag).");
        }
    }

    static final Pattern TIME = Pattern.compile("^(\\d{4})\\s*-\\s*(\\d{4})$");
    static final Pattern NOTAM = Pattern.compile("^[A-Z]\\d{4}/\\d{2}$");
    /** Gebiets-/Schiess-Nr: Großbuchstabe am Anfang, enthält eine Ziffer, keine Leerzeichen. */
    static final Pattern AREA = Pattern.compile("^[A-Z][A-Z0-9-]*\\d[A-Z0-9-]*$");
    static final Pattern ALT = Pattern.compile("^(GND|UNL|FL\\s*\\d+|\\d+m\\s*/\\s*\\d+ft(\\s*AGL)?)$");
    static final Pattern CENTER = Pattern.compile("^(\\d{6}[NS])\\s+(\\d{7}[EW])$");
    static final Pattern RADIUS = Pattern.compile("^([\\d.]+)KM\\s*/\\s*([\\d.]+)NM$");
    static final Pattern DATE = Pattern.compile("DABS Date:\\s*(\\d{4} [A-Z]{3} \\d{2})");
    static final Pattern VERSION = Pattern.compile("Version\\s+(\\d+)\\s*-\\s*generated:\\s*([\\d.]+ [\\d:]+ UTC)");

    public static Result parse(byte[] pdf) throws Exception {
        return parseTokens(extractTokens(pdf));
    }

    /** Alle Textstücke (Tj/TJ) aus den Flate-Streams in Dokumentreihenfolge. */
    public static List<String> extractTokens(byte[] pdf) throws Exception {
        Charset latin1 = Charset.forName("ISO-8859-1");
        Charset cp1252 = Charset.forName("windows-1252");
        String all = new String(pdf, latin1);
        List<String> tokens = new ArrayList<>();
        Pattern streamStart = Pattern.compile("stream\\r?\\n");
        Matcher m = streamStart.matcher(all);
        int pos = 0;
        while (m.find(pos)) {
            int start = m.end();
            int end = all.indexOf("endstream", start);
            if (end < 0) break;
            pos = end + 9; // hinter "endstream", sonst matcht "stream\n" darin
            byte[] content = inflate(pdf, start, end - start);
            if (content == null) continue;
            String s = new String(content, latin1);
            if (s.indexOf("Tj") < 0 && s.indexOf("TJ") < 0) continue;
            collectStrings(s, tokens, cp1252);
        }
        return tokens;
    }

    private static byte[] inflate(byte[] data, int off, int len) {
        try {
            Inflater inf = new Inflater();
            inf.setInput(data, off, len);
            ByteArrayOutputStream out = new ByteArrayOutputStream(len * 4);
            byte[] buf = new byte[65536];
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0) {
                    if (inf.needsInput() || inf.needsDictionary()) break;
                }
                out.write(buf, 0, n);
            }
            inf.end();
            return out.size() > 0 ? out.toByteArray() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Sucht "[...] TJ" und "(...) Tj" und hängt den Text als Token an. */
    private static void collectStrings(String s, List<String> tokens, Charset cs) {
        int n = s.length();
        int i = 0;
        StringBuilder cur = null;     // Text innerhalb [ ... ]
        boolean inArray = false;
        StringBuilder single = null;  // letzter (..)-String außerhalb eines Arrays
        while (i < n) {
            char c = s.charAt(i);
            if (c == '(') {
                StringBuilder b = new StringBuilder();
                int depth = 1;
                i++;
                while (i < n && depth > 0) {
                    char d = s.charAt(i);
                    if (d == '\\' && i + 1 < n) {
                        char e = s.charAt(i + 1);
                        if (e >= '0' && e <= '7') {
                            int j = i + 1, v = 0, k = 0;
                            while (k < 3 && j < n && s.charAt(j) >= '0' && s.charAt(j) <= '7') {
                                v = v * 8 + (s.charAt(j) - '0'); j++; k++;
                            }
                            b.append((char) v);
                            i = j;
                            continue;
                        }
                        switch (e) {
                            case 'n': b.append('\n'); break;
                            case 'r': b.append('\r'); break;
                            case 't': b.append('\t'); break;
                            default: b.append(e);
                        }
                        i += 2;
                        continue;
                    }
                    if (d == '(') depth++;
                    if (d == ')') { depth--; if (depth == 0) { i++; break; } }
                    b.append(d);
                    i++;
                }
                if (inArray) cur.append(b); else single = b;
                continue;
            }
            if (c == '[') { inArray = true; cur = new StringBuilder(); i++; continue; }
            if (c == ']' && inArray) {
                inArray = false;
                int j = skipWs(s, i + 1);
                if (s.startsWith("TJ", j)) tokens.add(recode(cur.toString(), cs));
                i = j;
                continue;
            }
            if (c == 'T' && i + 1 < n && s.charAt(i + 1) == 'j' && single != null) {
                tokens.add(recode(single.toString(), cs));
                single = null;
                i += 2;
                continue;
            }
            i++;
        }
    }

    private static int skipWs(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static String recode(String latin1, Charset cs) {
        return new String(latin1.getBytes(Charset.forName("ISO-8859-1")), cs);
    }

    /** Baut aus der Token-Folge die Einträge. Anker ist die Zeitspalte "HHMM - HHMM". */
    public static Result parseTokens(List<String> tok) throws NotAvailableException {
        Result r = new Result();
        for (String t : tok) {
            if (t.contains("not available yet")) throw new NotAvailableException();
            Matcher m;
            if (r.date == null && (m = DATE.matcher(t)).find()) r.date = m.group(1);
            if (r.version == null && (m = VERSION.matcher(t)).find()) {
                r.version = m.group(1);
                r.generated = m.group(2);
            }
        }
        boolean info = false;
        int lastEnd = 0;
        int i = 0;
        while (i < tok.size()) {
            String t = tok.get(i).trim();
            if (t.startsWith("Activities not shown")) info = true;
            if (!TIME.matcher(t).matches()) { i++; continue; }

            Entry e = new Entry();
            e.infoOnly = info;
            // IDs zwischen Ende des letzten Eintrags und der Zeitspalte
            String area = null, notam = null;
            for (int k = lastEnd; k < i; k++) {
                String x = tok.get(k).trim();
                if (NOTAM.matcher(x).matches()) notam = x;
                else if (AREA.matcher(x).matches()) { area = x; notam = null; }
            }
            int j = i;
            while (j < tok.size() && TIME.matcher(tok.get(j).trim()).matches()) {
                Matcher tm = TIME.matcher(tok.get(j).trim());
                tm.matches();
                e.times.add(new String[]{tm.group(1), tm.group(2)});
                j++;
            }
            if (j + 3 >= tok.size()) break;
            String lo = tok.get(j).trim(), hi = tok.get(j + 1).trim();
            Matcher cm = CENTER.matcher(tok.get(j + 2).trim());
            Matcher rm = RADIUS.matcher(tok.get(j + 3).trim());
            if ((area == null && notam == null) || !ALT.matcher(lo).matches() || !ALT.matcher(hi).matches()
                    || !cm.matches() || !rm.matches()) {
                i = j; // Kopfzeile o.ä., überspringen
                continue;
            }
            // Bei W-NOTAM ohne AIP-Gebiet ist das NOTAM selbst die ID
            e.id = area != null ? area : notam;
            e.notam = area != null ? notam : null;
            e.lower = lo;
            e.upper = hi;
            e.lat = dms(cm.group(1));
            e.lon = dms(cm.group(2));
            e.radiusNm = Double.parseDouble(rm.group(2));
            int k = j + 4;
            StringBuilder text = new StringBuilder();
            while (k < tok.size()) {
                String x = tok.get(k);
                String xt = x.trim();
                if (xt.isEmpty() || xt.equals("!") || xt.startsWith("Activities not shown")
                        || xt.startsWith("For detailed information")) break;
                text.append(x);
                k++;
            }
            e.text = text.toString().trim();
            r.entries.add(e);
            lastEnd = k;
            i = k;
        }
        return r;
    }

    static double dms(String s) {
        char h = s.charAt(s.length() - 1);
        String d = s.substring(0, s.length() - 1);
        int dl = (h == 'N' || h == 'S') ? 2 : 3;
        double v = Integer.parseInt(d.substring(0, dl))
                + Integer.parseInt(d.substring(dl, dl + 2)) / 60.0
                + Integer.parseInt(d.substring(dl + 2)) / 3600.0;
        return (h == 'S' || h == 'W') ? -v : v;
    }
}
