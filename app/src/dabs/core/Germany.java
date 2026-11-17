package dabs.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deutschland: DFS WebAUP (öffentlich, ohne Login). Liefert Zeiten, Höhen und Umrisse. */
public final class Germany {
    public static final String PAGE = "https://secais.dfs.de/pilotservice/briefing/aup/aup_edit.jsp";
    static final String AJAX = "https://secais.dfs.de/pilotservice/briefing/aup/ajax/aup_briefing.jsp";

    static final Pattern TABLE = Pattern.compile(
            "<table class=\"airspace\" data-part=\"([^\"]+)\" data-polygon=\"([^\"]*)\">(.*?)</table>", Pattern.DOTALL);
    static final Pattern VALID = Pattern.compile(
            "From <time datetime=\"([^\"]+)\">.*?until <time datetime=\"([^\"]+)\">.*?"
                    + "Lower Limit\\s+(\\S+)\\s+to Upper Limit\\s+([^<]+?)\\s*</td>", Pattern.DOTALL);
    static final Pattern COORD = Pattern.compile("(\\d{6}[NS])(\\d{7}[EW])");

    public static Country.Result fetch(LocalDate day) throws Exception {
        String d = day.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        Http h = new Http();
        h.get(PAGE); // Sitzungs-Cookie
        String html = h.post(AJAX, "DATE_BEGIN=" + d + "&TIME_BEGIN=00:00&DATE_END=" + d
                + "&TIME_END=23:59&LOWER=GND&UPPER=UNL&MAP=OSM");
        return parse(html, day);
    }

    static Country.Result parse(String html, LocalDate day) {
        Country.Result r = new Country.Result();
        Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plusSeconds(86400);
        StringBuilder b = new StringBuilder();
        java.util.Set<String> done = new java.util.HashSet<>(); // DFS listet manche Gebiete mehrfach
        Matcher t = TABLE.matcher(html);
        while (t.find()) {
            String part = t.group(1), poly = t.group(2), body = t.group(3);
            if (!done.add(part + "|" + poly + "|" + body)) continue;
            // Zeitfenster nach Höhenband gruppieren
            Map<String, List<String>> bands = new LinkedHashMap<>();
            Matcher v = VALID.matcher(body);
            while (v.find()) {
                Instant from = parseTime(v.group(1)), until = parseTime(v.group(2));
                if (from == null || until == null || !until.isAfter(dayStart) || !from.isBefore(dayEnd)) continue;
                if (from.isBefore(dayStart)) from = dayStart;
                if (until.isAfter(dayEnd)) until = dayEnd;
                String key = Area.alt(v.group(3)) + "|" + Area.alt(v.group(4).trim());
                List<String> l = bands.get(key);
                if (l == null) bands.put(key, l = new ArrayList<>());
                l.add(hhmm(from) + "-" + (until.equals(dayEnd) ? "2400" : hhmm(until)));
            }
            if (bands.isEmpty()) continue;
            List<String> geom = new ArrayList<>();
            Matcher c = COORD.matcher(poly);
            while (c.find()) geom.add(Area.dp(Area.dms(c.group(1)), Area.dms(c.group(2))));
            if (geom.size() < 3) {
                r.report.add(String.format(Locale.ROOT, "%-10s ohne Umriss – übersprungen", part));
                continue;
            }
            for (Map.Entry<String, List<String>> e : bands.entrySet()) {
                String[] lu = e.getKey().split("\\|");
                Area a = new Area();
                a.cls = part.contains("-D") ? "Q" : "R";
                String times = String.join(",", merge(e.getValue())) + "UTC";
                a.name = part + " " + Country.dayTag(day) + " " + times;
                a.lower = lu[0];
                a.upper = lu[1];
                a.geometry = geom;
                a.appendTo(b);
                r.count++;
                r.report.add(String.format(Locale.ROOT, "%-10s %-22s %s - %s", part, times, a.lower, a.upper));
            }
        }
        r.text = b.toString();
        return r;
    }

    /** Aneinanderstoßende Fenster zusammenfassen: 0000-0745,0745-1500 -> 0000-1500 */
    static List<String> merge(List<String> w) {
        List<String> out = new ArrayList<>();
        for (String s : w) {
            if (!out.isEmpty()) {
                String last = out.get(out.size() - 1);
                if (last.substring(5).equals(s.substring(0, 4))) {
                    out.set(out.size() - 1, last.substring(0, 5) + s.substring(5));
                    continue;
                }
            }
            out.add(s);
        }
        return out;
    }

    static Instant parseTime(String s) {
        try {
            // "2026-11-17T07:45Z"
            return Instant.parse(s.length() == 17 ? s.replace("Z", ":00Z") : s);
        } catch (Exception e) {
            return null;
        }
    }

    static String hhmm(Instant i) {
        return DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC).format(i);
    }
}
