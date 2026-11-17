package dabs.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ein aktiver Luftraum, bereit für OpenAir. Plus Hilfsfunktionen für Höhen und Koordinaten. */
public final class Area {
    /**
     * Alle Gebiete der App bekommen die OpenAir-Klasse TFR ("temporary flight restriction").
     * Sie kommt in den üblichen Luftraumdateien praktisch nicht vor, daher kann man ihr in XCSoar
     * eine eigene Farbe geben (Klasse Nr. 42, z. B. violett) und sieht sofort, welche Gebiete
     * tagesaktuell aus der App stammen. Warnungen sind für TFR in XCSoar standardmäßig an.
     */
    public static final String OUT_CLASS = "TFR";

    /** Farbe (OpenAir SP/SB, violett). XCSoar ignoriert das, SeeYou u.a. werten es aus. */
    public static final String STYLE = "SP 0,2,128,0,255\nSB 128,0,255\n";

    public String cls = "R";        // ursprüngliche Art (R/Q/P) – nur informativ
    public String name;
    public String lower, upper;     // bereits im OpenAir-Format
    public List<String> geometry = new ArrayList<>(); // DP/V/DC/DB-Zeilen

    public void appendTo(StringBuilder b) {
        b.append("AC ").append(OUT_CLASS).append('\n');
        b.append("AN ").append(name).append('\n');
        b.append(STYLE);
        b.append("AL ").append(lower).append('\n');
        b.append("AH ").append(upper).append('\n');
        for (String g : geometry) b.append(g).append('\n');
        b.append('\n');
    }

    public static String header(String title, String source, String stand) {
        return "* " + title + " fuer XCSoar (DABS-App)\n"
                + "* Quelle: " + source + "\n"
                + "* Stand: " + stand + "\n"
                + "* Zeiten in UTC. Ohne Gewaehr - massgebend sind AIP, AUP/DABS und NOTAM.\n\n";
    }

    // --- Höhen -------------------------------------------------------------

    static final Pattern FL = Pattern.compile("^F(?:L)?\\s*0*(\\d+)$");
    static final Pattern A = Pattern.compile("^A0*(\\d{1,3})$");
    static final Pattern FT = Pattern.compile("^(\\d+)\\s*FT\\s*(AMSL|MSL|AGL|SFC|GND)?$");
    static final Pattern M = Pattern.compile("^(\\d+)\\s*M\\s*(AMSL|MSL|AGL|SFC|GND)?$");

    /** "F095" / "FL 95" / "A020" / "5500FT AMSL" / "GND" -> OpenAir. */
    public static String alt(String raw) {
        if (raw == null) return "UNL";
        String s = raw.trim().toUpperCase(Locale.ROOT).replace("/", " ").replaceAll("\\s+", " ");
        if (s.isEmpty() || s.equals("UNL") || s.equals("UNLTD") || s.equals("999")) return "UNL";
        if (s.equals("GND") || s.equals("SFC") || s.equals("000")) return "GND";
        if (s.equals("MSL") || s.equals("AMSL")) return "0ft MSL";
        Matcher m;
        if ((m = FL.matcher(s)).matches()) {
            int fl = Integer.parseInt(m.group(1));
            return fl >= 999 ? "UNL" : fl == 0 ? "GND" : "FL" + fl;
        }
        if ((m = A.matcher(s)).matches()) return (Integer.parseInt(m.group(1)) * 100) + "ft MSL";
        if ((m = FT.matcher(s)).matches()) return m.group(1) + "ft " + agl(m.group(2));
        if ((m = M.matcher(s)).matches())
            return Math.round(Integer.parseInt(m.group(1)) / 0.3048) + "ft " + agl(m.group(2));
        return s;
    }

    private static String agl(String ref) {
        return ref != null && (ref.equals("AGL") || ref.equals("SFC") || ref.equals("GND")) ? "AGL" : "MSL";
    }

    // --- Koordinaten -------------------------------------------------------

    /** "514530N" / "0084100E" / "4841N" / "01519E" -> Grad. */
    public static double dms(String s) {
        char h = s.charAt(s.length() - 1);
        String d = s.substring(0, s.length() - 1);
        int dl = (h == 'N' || h == 'S') ? 2 : 3;
        double v = Integer.parseInt(d.substring(0, dl));
        if (d.length() >= dl + 2) v += Integer.parseInt(d.substring(dl, dl + 2)) / 60.0;
        if (d.length() >= dl + 4) v += Integer.parseInt(d.substring(dl + 2, dl + 4)) / 3600.0;
        return (h == 'S' || h == 'W') ? -v : v;
    }

    public static String dp(double lat, double lon) {
        return "DP " + OpenAir.fmtCoord(lat, lon);
    }

    public static List<String> circle(double lat, double lon, double rNm) {
        List<String> g = new ArrayList<>();
        g.add("V X=" + OpenAir.fmtCoord(lat, lon));
        g.add(String.format(Locale.ROOT, "DC %.2f", rNm));
        return g;
    }
}
