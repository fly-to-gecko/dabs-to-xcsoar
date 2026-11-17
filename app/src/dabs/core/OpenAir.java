package dabs.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Verknüpft DABS-Aktivierungen mit SHV-Geometrien und schreibt OpenAir. */
public final class OpenAir {

    static final Pattern CODE = Pattern.compile("^\\s*LS-?([RDP])\\s*(\\d+[A-Z]?)\\b");
    static final Pattern METRIC = Pattern.compile("^(\\d+)m/(\\d+)ft(AGL)?$");
    static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

    public static final class Output {
        public String text;
        public List<String> report = new ArrayList<>();
    }

    /** "LS-R31 Emmen east" / "LSD12 Sihltal" -> "LSR31" / "LSD12" */
    static String normCode(String name) {
        Matcher m = CODE.matcher(name.toUpperCase(Locale.ROOT));
        return m.find() ? "LS" + m.group(1) + m.group(2) : null;
    }

    static boolean validOn(Map<String, Object> asp, String isoDate) {
        List<Object> vals = Json.arr(asp.get("Validity"));
        if (vals.isEmpty()) return true;
        for (Object o : vals) {
            Map<String, Object> v = Json.obj(o);
            String f = (String) v.get("ValidFrom"), t = (String) v.get("ValidTo");
            boolean okF = f == null || f.substring(0, 10).compareTo(isoDate) <= 0;
            boolean okT = t == null || isoDate.compareTo(t.substring(0, 10)) <= 0;
            if (okF && okT) return true;
        }
        return false;
    }

    /** SHV-JSON -> Code -> Liste der Lufträume. */
    public static Map<String, List<Map<String, Object>>> indexShv(String json, String isoDate) {
        Map<String, List<Map<String, Object>>> idx = new HashMap<>();
        for (Object o : Json.arr(Json.obj(Json.parse(json)).get("Airspaces"))) {
            Map<String, Object> a = Json.obj(o);
            String code = normCode((String) a.get("Name"));
            if (code == null || !validOn(a, isoDate)) continue;
            List<Map<String, Object>> l = idx.get(code);
            if (l == null) idx.put(code, l = new ArrayList<>());
            l.add(a);
        }
        return idx;
    }

    /** "2026 NOV 17" -> "2026-11-17" */
    public static String isoDate(String dabsDate) {
        String[] p = dabsDate.split(" ");
        int mon = 1;
        for (int k = 0; k < 12; k++) if (MONTHS[k].equals(p[1])) mon = k + 1;
        return String.format(Locale.ROOT, "%s-%02d-%s", p[0], mon, p[2]);
    }

    static String fmtAlt(String s) {
        s = s.replace(" ", "");
        if (s.equals("GND") || s.equals("UNL")) return s;
        if (s.startsWith("FL")) return s;
        Matcher m = METRIC.matcher(s);
        if (m.matches()) return m.group(2) + "ft " + (m.group(3) != null ? "AGL" : "MSL");
        return s;
    }

    public static String fmtCoord(double lat, double lon) {
        return dms(lat, 2) + (lat >= 0 ? " N " : " S ") + dms(lon, 3) + (lon >= 0 ? " E" : " W");
    }

    private static String dms(double v, int w) {
        long total = Math.round(Math.abs(v) * 3600);
        long d = total / 3600, m = (total % 3600) / 60, s = total % 60;
        return String.format(Locale.ROOT, "%0" + w + "d:%02d:%02d", d, m, s);
    }

    // --- Geometrie ---------------------------------------------------------

    private static double[] distBearing(double lat1, double lon1, double lat2, double lon2) {
        double dy = (lat2 - lat1) * 60;
        double dx = (lon2 - lon1) * 60 * Math.cos(Math.toRadians((lat1 + lat2) / 2));
        return new double[]{Math.hypot(dx, dy), Math.atan2(dx, dy)};
    }

    private static double[] offset(double lat, double lon, double distNm, double brg) {
        return new double[]{lat + distNm * Math.cos(brg) / 60,
                lon + distNm * Math.sin(brg) / 60 / Math.cos(Math.toRadians(lat))};
    }

    private static double[] coord(Map<String, Object> p) {
        Map<String, Object> c = Json.obj(p.get("Coordinate"));
        return new double[]{(Double) c.get("Latitude"), (Double) c.get("Longitude")};
    }

    /** Schreibt die Geometrie eines SHV-Segments; Bögen werden in Punkte aufgelöst. */
    static void writeShape(List<Object> shape, StringBuilder out) {
        if (shape.size() == 1 && "Circle".equals(Json.obj(shape.get(0)).get("Type"))) {
            Map<String, Object> p = Json.obj(shape.get(0));
            double[] c = coord(p);
            out.append("V X=").append(fmtCoord(c[0], c[1])).append('\n');
            out.append(String.format(Locale.ROOT, "DC %.2f%n", (Double) p.get("Radius")));
            return;
        }
        double[] prev = null;
        for (int i = 0; i < shape.size(); i++) {
            Map<String, Object> p = Json.obj(shape.get(i));
            double[] c = coord(p);
            String type = (String) p.get("Type");
            if ("Point".equals(type)) {
                out.append("DP ").append(fmtCoord(c[0], c[1])).append('\n');
                prev = c;
            } else if ("CoordArc".equals(type) && prev != null) {
                double[] end = coord(Json.obj(shape.get(i + 1 < shape.size() ? i + 1 : 0)));
                boolean cw = !"CCW".equals(p.get("ArcDirection"));
                double[] r0 = distBearing(c[0], c[1], prev[0], prev[1]);
                double[] r1 = distBearing(c[0], c[1], end[0], end[1]);
                double twoPi = 2 * Math.PI;
                double sweep = cw ? mod(r1[1] - r0[1], twoPi) : -mod(r0[1] - r1[1], twoPi);
                int steps = Math.max(4, (int) (Math.abs(sweep) / Math.toRadians(5)));
                for (int k = 1; k < steps; k++) {
                    double[] q = offset(c[0], c[1], r0[0], r0[1] + sweep * k / steps);
                    out.append("DP ").append(fmtCoord(q[0], q[1])).append('\n');
                }
            } else {
                throw new IllegalArgumentException("Unbekannter Shape-Typ " + type);
            }
        }
    }

    private static double mod(double a, double m) {
        double r = a % m;
        return r < 0 ? r + m : r;
    }

    // --- Ausgabe -----------------------------------------------------------

    public static Output build(DabsPdf.Result dabs, Map<String, List<Map<String, Object>>> geo) {
        Output o = new Output();
        String iso = dabs.date != null ? isoDate(dabs.date) : "?";
        String dayTag = iso.length() == 10 ? iso.substring(8, 10) + "." + iso.substring(5, 7) + "." : "";
        StringBuilder b = new StringBuilder();
        b.append("* Schweizer DABS-Aktivierungen (Skyguide) fuer XCSoar\n");
        b.append("* DABS-Datum: ").append(iso).append("  Version ").append(dabs.version)
                .append("  erzeugt ").append(dabs.generated).append('\n');
        b.append("* Geometrie: SHV/FSVL airspace.shv-fsvl.ch bzw. Kreis aus DABS-Mittelpunkt/Radius\n");
        b.append("* Zeiten in UTC. Ohne Gewaehr - massgebend sind DABS und NOTAM.\n\n");

        for (DabsPdf.Entry e : dabs.entries) {
            StringBuilder t = new StringBuilder();
            for (String[] tw : e.times) {
                if (t.length() > 0) t.append(',');
                t.append(tw[0]).append('-').append(tw[1]);
            }
            String times = t + "UTC";
            String lo = fmtAlt(e.lower), hi = fmtAlt(e.upper);
            if (e.infoOnly) {
                // "Activities not shown on the DABS Chart Side" (z.B. MIL-Nachtflüge, 98 NM Radius):
                // Skyguide zeichnet diese nicht in die Karte -> nur als Hinweis, nicht in OpenAir.
                String txt = e.text.length() > 140 ? e.text.substring(0, 140) + "…" : e.text;
                o.report.add(String.format(Locale.ROOT, "%-9s %-11s %11s - %-11s NUR INFO (nicht in Karte)%n          %s",
                        e.id, times, lo, hi, txt));
                b.append("* Info (nicht in DABS-Karte): ").append(e.id).append(' ').append(times).append(' ')
                        .append(lo).append(" - ").append(hi).append(": ").append(e.text).append("\n\n");
                continue;
            }
            String cls = e.id.startsWith("LSD") ? "Q" : "R";
            List<Map<String, Object>> parts = geo.get(e.id);
            if (parts != null && !parts.isEmpty()) {
                for (Map<String, Object> asp : parts) {
                    for (Object so : Json.arr(asp.get("Segments"))) {
                        Map<String, Object> seg = Json.obj(so);
                        b.append("AC ").append(cls).append('\n');
                        b.append("AN ").append(asp.get("Name")).append(' ').append(dayTag).append(' ').append(times).append('\n');
                        b.append("AL ").append(lo).append('\n');
                        b.append("AH ").append(hi).append('\n');
                        writeShape(Json.arr(seg.get("Shape")), b);
                        b.append('\n');
                    }
                }
                o.report.add(String.format(Locale.ROOT, "%-9s %-11s %11s - %-11s Umriss", e.id, times, lo, hi));
            } else {
                b.append("AC ").append(cls).append('\n');
                b.append("AN ").append(e.id).append(' ').append(dayTag).append(' ').append(times).append('\n');
                b.append("AL ").append(lo).append('\n');
                b.append("AH ").append(hi).append('\n');
                b.append("V X=").append(fmtCoord(e.lat, e.lon)).append('\n');
                b.append(String.format(Locale.ROOT, "DC %.2f%n%n", e.radiusNm));
                o.report.add(String.format(Locale.ROOT, "%-9s %-11s %11s - %-11s Kreis %.1f NM", e.id, times, lo, hi, e.radiusNm));
            }
        }
        o.text = b.toString();
        return o;
    }
}
