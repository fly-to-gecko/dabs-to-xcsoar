package dabs.core;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tschechien: AUP/UUP der ANS CR (aup.ans.cz) + Umrisse aus der Luftraumdatei des
 * Aeroklub CR (über das XCSoar-Repository, ausdrücklich frei nutzbar).
 */
public final class Czechia {
    public static final String INDEX = "https://aup.ans.cz/";
    static final String GEOMETRY = "https://download.xcsoar.org/content/airspace/country/CZ-ASP-National-XCSoar.txt";

    static final Pattern ROW = Pattern.compile("<TR>(.*?)</TR>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    static final Pattern CELL = Pattern.compile("<TD class=\"data\">(.*?)</TD>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    static final Pattern LINK = Pattern.compile("HREF=\"(data/(?:ANG|UNG)_[^\"]+\\.htm)\">\\s*Valid (AUP|UUP)",
            Pattern.CASE_INSENSITIVE);

    /** Eine Zeile aus Abschnitt C (AMA) oder D (NAM). */
    static final class Row {
        String area, lower, upper, from, until, info;
        String key() { return area + "|" + from + "|" + until; }
    }

    public static Country.Result fetch(LocalDate day) throws Exception {
        Http h = new Http();
        String index = h.get(INDEX);
        String aupUrl = null, uupUrl = null;
        String tag = day.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        Matcher m = LINK.matcher(index);
        while (m.find()) {
            if (!m.group(1).contains("_" + tag)) continue;
            if (m.group(2).equalsIgnoreCase("AUP")) aupUrl = INDEX + m.group(1);
            else uupUrl = INDEX + m.group(1);
        }
        if (aupUrl == null) aupUrl = INDEX + "data/ANG_" + tag + ".htm";
        Map<String, Row> rows = new LinkedHashMap<>();
        for (Row r : parseRows(h.get(aupUrl))) rows.put(r.key(), r);
        String stand = "AUP";
        List<String> cancelled = new ArrayList<>();
        if (uupUrl != null) { // UUP: Änderungen, CNL = storniert
            stand = "AUP + UUP";
            for (Row r : parseRows(h.get(uupUrl))) {
                if (r.info != null && r.info.toUpperCase(Locale.ROOT).contains("CNL")) {
                    if (rows.remove(r.key()) != null) cancelled.add(r.area);
                } else rows.put(r.key(), r);
            }
        }
        String geo = new String(h.request(GEOMETRY, null), Charset.forName("ISO-8859-1"));
        Country.Result res = build(new ArrayList<>(rows.values()), indexOpenAir(geo), day);
        res.stand = stand;
        if (!cancelled.isEmpty()) res.report.add("Storniert (UUP): " + String.join(", ", cancelled));
        return res;
    }

    static List<Row> parseRows(String html) {
        List<Row> out = new ArrayList<>();
        // nur Abschnitte C (AMA) und D (NAM)
        int c = html.indexOf("C/ AMC"), e = html.indexOf("E/ Unavailable");
        if (c < 0) return out;
        String s = html.substring(c, e > c ? e : html.length());
        Matcher tr = ROW.matcher(s);
        while (tr.find()) {
            List<String> cells = new ArrayList<>();
            Matcher td = CELL.matcher(tr.group(1));
            while (td.find()) cells.add(td.group(1).replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim());
            if (cells.size() < 6 || !cells.get(4).matches("\\d{2}:\\d{2}")) continue;
            Row r = new Row();
            r.area = cells.get(1);
            r.lower = cells.get(2);
            r.upper = cells.get(3);
            r.from = cells.get(4);
            r.until = cells.get(5);
            r.info = cells.size() > 7 ? cells.get(7) : null;
            out.add(r);
        }
        return out;
    }

    /** OpenAir-Datei -> Name (z.B. "LKTSA42") -> Geometriezeilen. */
    static Map<String, List<String>> indexOpenAir(String text) {
        Map<String, List<String>> idx = new HashMap<>();
        String name = null;
        List<String> geom = null;
        for (String line : text.split("\\r?\\n")) {
            String l = line.trim();
            if (l.startsWith("AC ")) { name = null; geom = new ArrayList<>(); continue; }
            if (l.startsWith("AN ") && geom != null) {
                String[] p = l.substring(3).trim().split("\\s+");
                name = p.length > 0 ? p[0].toUpperCase(Locale.ROOT) : null;
                if (name != null && !idx.containsKey(name)) idx.put(name, geom);
                continue;
            }
            if (geom != null && (l.startsWith("DP ") || l.startsWith("V ") || l.startsWith("DA ")
                    || l.startsWith("DB ") || l.startsWith("DC "))) {
                int star = l.indexOf('*');
                geom.add((star > 0 ? l.substring(0, star) : l).trim());
            }
        }
        return idx;
    }

    static Country.Result build(List<Row> rows, Map<String, List<String>> geo, LocalDate day) {
        Country.Result r = new Country.Result();
        StringBuilder b = new StringBuilder();
        for (Row row : rows) {
            String code = row.area.toUpperCase(Locale.ROOT).replace(" ", "");
            List<String> g = geo.get(code.startsWith("LK") ? code : "LK" + code);
            if (g == null) g = geo.get(code);
            String times = row.from.replace(":", "") + "-" + row.until.replace(":", "") + "UTC";
            String lo = Area.alt(row.lower), hi = Area.alt(row.upper);
            if (g == null || g.isEmpty()) {
                r.report.add(String.format(Locale.ROOT, "%-8s %-13s %s - %s  kein Umriss gefunden", row.area, times, lo, hi));
                continue;
            }
            Area a = new Area();
            a.cls = code.contains("D") && !code.contains("TSA") && !code.contains("TRA") ? "Q" : "R";
            a.name = (code.startsWith("LK") ? code : "LK" + code) + " " + Country.dayTag(day) + " " + times;
            a.lower = lo;
            a.upper = hi;
            a.geometry = g;
            a.appendTo(b);
            r.count++;
            r.report.add(String.format(Locale.ROOT, "%-8s %-13s %s - %s", row.area, times, lo, hi));
        }
        r.text = b.toString();
        return r;
    }
}
