package dabs.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Österreich / Frankreich: Aktivierungen von R-/D-Gebieten und Militärübungen aus NOTAMs.
 * Quelle: NOTAM-Dienst von XCSoar (notam.xcsoar.org, Cache der FAA-NOTAM-API).
 */
public final class Notams {
    public static final String API = "https://notam.xcsoar.org/notam.php";
    static final double MAX_CIRCLE_NM = 50; // größere Kreise (ganze FIR o.ä.) nur als Hinweis

    /** Abfragepunkte (lat, lon), Radius 100 NM, so gewählt, dass das Land abgedeckt ist. */
    public static final double[][] AUSTRIA = {{47.6, 10.8}, {47.6, 13.4}, {47.9, 16.0}};
    public static final double[][] FRANCE = {
            {42.8, -0.4}, {42.8, 2.8}, {42.8, 6.0}, {42.2, 9.0},
            {45.2, -3.6}, {45.2, -0.4}, {45.2, 2.8}, {45.2, 6.0},
            {47.6, -3.6}, {47.6, -0.4}, {47.6, 2.8}, {47.6, 6.0},
            {50.0, -3.6}, {50.0, -0.4}, {50.0, 2.8}, {50.0, 6.0}};
    public static final String[] FIR_AT = {"LOVV"};
    public static final String[] FIR_FR = {"LFBB", "LFEE", "LFFF", "LFMM", "LFRR"};

    static final Pattern Q_CENTER = Pattern.compile("/(\\d{4}[NS])(\\d{5}[EW])(\\d{3})\\b");
    static final Pattern COORD = Pattern.compile("(\\d{4,6}(?:\\.\\d+)?[NS])\\s*(\\d{5,7}(?:\\.\\d+)?[EW])");
    static final Pattern AREA_CODE = Pattern.compile(
            "\\b(LO[RDPT] ?\\d+[A-Z]?|LF[- ]?[RDP] ?\\d+[A-Z]?\\d?|TRA ?\\d+[A-Z]?\\d?|TSA ?\\d+[A-Z]?\\d?|CBA ?\\d+[A-Z]?\\d?|AZBA|RTBA|AIP SUP \\d+/\\d+)(?![A-Z0-9])");

    public static Country.Result fetch(double[][] centers, String[] firs, LocalDate day) throws Exception {
        Http h = new Http();
        Set<String> seen = new HashSet<>();   // NOTAM-IDs und -Nummern (Duplikate aus mehreren Abfragen)
        List<Map<String, Object>> items = new ArrayList<>();
        int failed = 0;
        for (double[] c : centers) {
            try {
                String json = h.get(String.format(Locale.ROOT, "%s?lat=%.2f&lon=%.2f&radius=100", API, c[0], c[1]));
                for (Object o : Json.arr(Json.obj(Json.parse(json)).get("items"))) {
                    Map<String, Object> it = Json.obj(o);
                    Map<String, Object> n = notam(it);
                    if (n == null) continue;
                    String id = String.valueOf(n.get("id")), nr = String.valueOf(n.get("number"));
                    if (seen.add(id) && seen.add("#" + nr)) items.add(it);
                }
            } catch (Exception e) {
                failed++;
            }
        }
        if (failed == centers.length) throw new Exception("NOTAM-Dienst nicht erreichbar");
        Country.Result r = build(items, firs, day);
        if (failed > 0) r.report.add(0, "⚠ " + failed + " von " + centers.length + " NOTAM-Abfragen fehlgeschlagen");
        return r;
    }

    static Map<String, Object> notam(Map<String, Object> item) {
        try {
            return Json.obj(Json.obj(Json.obj(item.get("properties")).get("coreNOTAMData")).get("notam"));
        } catch (Exception e) {
            return null;
        }
    }

    static String str(Map<String, Object> m, String k) {
        Object o = m.get(k);
        return o == null ? "" : String.valueOf(o).trim();
    }

    static Country.Result build(List<Map<String, Object>> items, String[] firs, LocalDate day) {
        Country.Result r = new Country.Result();
        Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plusSeconds(86400);
        StringBuilder b = new StringBuilder();
        for (Map<String, Object> it : items) {
            Map<String, Object> n = notam(it);
            String fir = str(n, "affectedFir");
            boolean inCountry = false;
            for (String f : firs) if (fir.startsWith(f)) inCountry = true;
            if (!inCountry) continue;
            String q = str(n, "selectionCode").toUpperCase(Locale.ROOT);
            // QR* = Luftraumbeschränkungen (R/D/P/TRA...), QWM/QWE = Militärübungen
            if (!isActivation(q)) continue;
            Instant from = time(str(n, "effectiveStart")), until = time(str(n, "effectiveEnd"));
            if (from != null && !from.isBefore(dayEnd)) continue;
            if (until != null && !until.isAfter(dayStart)) continue;

            String text = str(n, "text").replace("\r", " ").replace("\n", " ").replaceAll("\\s+", " ");
            String number = str(n, "number");
            String sched = str(n, "schedule");
            String when = sched.isEmpty() ? window(from, until, dayStart, dayEnd) : sched;
            String lo = Area.alt(str(n, "lowerLimit").isEmpty() ? str(n, "minimumFl") : str(n, "lowerLimit"));
            String hi = Area.alt(str(n, "upperLimit").isEmpty() ? "FL" + str(n, "maximumFl") : str(n, "upperLimit"));

            // Geometrie: Polygon aus dem Text, sonst Kreis aus der Q-Zeile
            List<String> geom = new ArrayList<>();
            Matcher c = COORD.matcher(text);
            List<double[]> pts = new ArrayList<>();
            while (c.find()) pts.add(new double[]{dmsAny(c.group(1)), dmsAny(c.group(2))});
            if (pts.size() >= 3) {
                for (double[] p : pts) geom.add(Area.dp(p[0], p[1]));
            } else {
                Matcher qm = Q_CENTER.matcher(formatted(it));
                if (!qm.find()) continue;
                double rad = Integer.parseInt(qm.group(3));
                double la = Area.dms(qm.group(1)), lo2 = Area.dms(qm.group(2));
                if (pts.size() == 1 && text.matches(".*RADIUS\\s*\\d.*")) { la = pts.get(0)[0]; lo2 = pts.get(0)[1]; }
                if (rad > MAX_CIRCLE_NM) {
                    r.report.add(String.format(Locale.ROOT, "%-9s NUR INFO (%d NM): %s", number, (int) rad, cut(text, 90)));
                    continue;
                }
                geom = Area.circle(la, lo2, Math.max(rad, 0.5));
            }
            Matcher am = AREA_CODE.matcher(text);
            String label = am.find() ? am.group(1).replace("AIP SUP ", "SUP").replace(" ", "") : cut(text, 30);
            if (label.matches("LF[RDP]\\d.*")) label = "LF-" + label.substring(2);
            String shape = pts.size() >= 3 ? "Umriss" : "Kreis ~" + geomRadius(geom);
            Area a = new Area();
            a.cls = classFromQ(q, label);
            a.name = label + " " + number + " " + Country.dayTag(day) + " " + when;
            a.lower = lo;
            a.upper = hi;
            a.geometry = geom;
            a.appendTo(b);
            r.count++;
            r.report.add(String.format(Locale.ROOT, "%-9s %-12s %s - %s %s  [%s]", number, label, lo, hi, when, shape));
        }
        r.text = b.toString();
        return r;
    }

    /**
     * Nur NOTAMs, die eine Aktivität bedeuten (ICAO-Q-Code, Buchstaben 4-5):
     * CA aktiviert, CS eingerichtet, LW findet statt, LP verboten; XX nur bei QRM/QRT (Übungsräume).
     * Nicht: CH geändert, TT Trigger (AIP-SUP/AIRAC), CD deaktiviert, LT eingeschränkt usw.
     */
    static boolean isActivation(String q) {
        if (q.length() != 5) return false;
        boolean area = q.startsWith("QR"), warn = q.startsWith("QWM") || q.startsWith("QWE");
        if (!area && !warn) return false;
        String cond = q.substring(3);
        if (cond.equals("CA") || cond.equals("CS") || cond.equals("LW") || cond.equals("LP")) return true;
        return cond.equals("XX") && (q.startsWith("QRM") || q.startsWith("QRT"));
    }

    /**
     * Kategorie aus dem ICAO-Q-Code (2./3. Buchstabe), bei TRA/TSA aus der Bezeichnung:
     * QRD Danger -> Q, QRP/QRO Prohibited -> P, QRM Military Operating Area -> MTA,
     * QRA Reserved -> TSA, QW.. Warnung (Übung) -> WARNING, sonst R.
     */
    static String classFromQ(String q, String label) {
        String byName = Area.classFor(label);
        if (byName.equals("TRA") || byName.equals("TSA")) return byName;
        if (q.startsWith("QRD")) return "Q";
        if (q.startsWith("QRP") || q.startsWith("QRO")) return "P";
        if (q.startsWith("QRM")) return "MTA";
        if (q.startsWith("QRA")) return "TSA";
        if (q.startsWith("QW")) return "WARNING";
        return "R";
    }

    static String formatted(Map<String, Object> item) {
        try {
            List<Object> tr = Json.arr(Json.obj(Json.obj(item.get("properties")).get("coreNOTAMData")).get("notamTranslation"));
            for (Object o : tr) {
                String f = str(Json.obj(o), "formattedText");
                if (f.contains("Q)")) return f;
            }
        } catch (Exception ignored) { }
        return "";
    }

    /** 4841N / 484530N / 484530.5N bzw. 01519E / 0151100E -> Grad */
    static double dmsAny(String s) {
        int dot = s.indexOf('.');
        if (dot > 0) s = s.substring(0, dot) + s.charAt(s.length() - 1);
        return Area.dms(s);
    }

    static Instant time(String s) {
        try { return s.isEmpty() ? null : Instant.parse(s); } catch (Exception e) { return null; }
    }

    static String window(Instant from, Instant until, Instant ds, Instant de) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC);
        String a = from == null || from.isBefore(ds) ? "0000" : f.format(from);
        String z = until == null || !until.isBefore(de) ? "2400" : f.format(until);
        return a + "-" + z + "UTC";
    }

    static String geomRadius(List<String> g) {
        for (String l : g) if (l.startsWith("DC ")) return l.substring(3).trim() + " NM";
        return "?";
    }

    static String cut(String s, int n) {
        return s.length() > n ? s.substring(0, n) + "…" : s;
    }
}
