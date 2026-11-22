package dabs.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Die unterstützten Länder, je eine OpenAir-Datei. */
public enum Country {
    CH("Schweiz", "dabs.txt", "Skyguide DABS + SHV-Geometrie", "https://www.skybriefing.com/o/dabs?today"),
    DE("Deutschland", "mil-de.txt", "DFS WebAUP", Germany.PAGE),
    CZ("Tschechien", "mil-cz.txt", "ANS CR AUP/UUP + Aeroklub CR Geometrie", Czechia.INDEX),
    AT("Österreich", "mil-at.txt", "NOTAM (notam.xcsoar.org)", "https://www.homebriefing.com/fwf-acg/"),
    FR("Frankreich", "mil-fr.txt", "NOTAM (notam.xcsoar.org)", "https://www.sia.aviation-civile.gouv.fr/");

    public final String label, fileName, source, link;

    /** Länder, bei denen fehlende Grenzen aus eingebundenen XCSoar-Dateien ergänzt werden können. */
    public boolean usesLocalGeometry() {
        return this != DE; // DE liefert immer Umrisse
    }

    Country(String label, String fileName, String source, String link) {
        this.label = label;
        this.fileName = fileName;
        this.source = source;
        this.link = link;
    }

    public static final class Result {
        public String text = "";
        public int count;
        public List<String> report = new ArrayList<>();
        public String stand;
        public byte[] pdf; // nur CH: Original-DABS
    }

    public static LocalDate today() {
        return LocalDate.now(ZoneId.of("Europe/Zurich"));
    }

    static String dayTag(LocalDate d) {
        return d.format(DateTimeFormatter.ofPattern("dd.MM."));
    }

    /** Lädt die heutigen Aktivierungen; der Text ist komplette OpenAir-Datei inkl. Kopf. */
    public Result fetch(LocalDate day) throws Exception {
        return fetch(day, null);
    }

    /** @param local Umrisse aus den Luftraumdateien im XCSoar-Ordner (für NOTAM-Länder), darf null sein */
    public Result fetch(LocalDate day, LocalGeometry local) throws Exception {
        Result r;
        switch (this) {
            case CH: {
                Converter.Summary s = Converter.convert("today", local);
                r = new Result();
                r.text = s.text;
                r.count = s.count;
                r.report = s.report;
                r.pdf = s.pdf;
                r.stand = "DABS " + s.date + " Version " + s.version;
                return r;
            }
            case DE: r = Germany.fetch(day); break;
            case CZ: r = Czechia.fetch(day, local); break;
            case AT: r = Notams.fetch(Notams.AUSTRIA, Notams.FIR_AT, day, local); break;
            case FR: r = Notams.fetch(Notams.FRANCE, Notams.FIR_FR, day, local); break;
            default: throw new IllegalStateException();
        }
        String now = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT));
        if (r.stand == null) r.stand = "";
        r.stand = (r.stand + " geladen " + now + " UTC, gueltig " + dayTag(day)).trim();
        r.text = Area.header("Aktive Militaer-/Sperrgebiete " + label, source, r.stand) + r.text;
        return r;
    }

    /**
     * XCSoar erkennt eine OpenAir-Datei nur, wenn sie mindestens einen Luftraum enthält
     * ("Unbekannter Luftraumdateityp"). Ohne Aktivierungen daher ein winziger Platzhalter
     * bei 0°N 0°E (Golf von Guinea) – stört nirgends.
     */
    static String ensureNotEmpty(String text) {
        if (text.startsWith("AC ") || text.contains("\nAC ")) return text;
        return text + "* Keine Lufträume. Unsichtbarer Platzhalter (0N 0E), damit XCSoar die Datei akzeptiert:\n"
                + "AC G\nAN Platzhalter - keine Luftraeume\nAL GND\nAH 1ft AGL\n"
                + "V X=00:00:00 N 000:00:00 E\nDC 0.01\n";
    }

    /**
     * Leere Datei mit Rahmen für ein abgewähltes Land: keine Lufträume, aber gültiges OpenAir,
     * damit XCSoar beim Start keinen Fehler meldet, wenn die Datei im Profil eingetragen ist.
     */
    public String emptyFrame() {
        return "* Aktive Militaer-/Sperrgebiete " + label + " fuer XCSoar (DABS-App)\n"
                + "* Land in der DABS-App abgewaehlt - Datei absichtlich ohne Lufttraeume.\n\n";
    }

    /** Atomar schreiben (tmp + rename). */
    public static void write(File out, String text) throws Exception {
        text = ensureNotEmpty(text);
        File dir = out.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) throw new Exception("Ordner nicht anlegbar: " + dir);
        File tmp = new File(dir, out.getName() + ".tmp");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), Charset.forName("UTF-8"))) {
            w.write(text);
        }
        if (!tmp.renameTo(out)) {
            out.delete();
            if (!tmp.renameTo(out)) throw new Exception("Konnte " + out + " nicht schreiben");
        }
    }
}
