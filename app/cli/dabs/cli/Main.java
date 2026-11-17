package dabs.cli;

import dabs.core.Country;
import dabs.core.DabsPdf;
import dabs.core.OpenAir;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;

/** PC-Test:
 *   java -cp build/cli dabs.cli.Main [CH|DE|CZ|AT|FR|ALL] [ausgabe-ordner] [xcsoar-datenordner]
 *   java -cp build/cli dabs.cli.Main --pdf datei.pdf   (nur DABS-Parser testen) */
public class Main {
    public static void main(String[] a) throws Exception {
        if (a.length >= 2 && a[0].equals("--pdf")) {
            DabsPdf.Result r = DabsPdf.parse(Files.readAllBytes(new File(a[1]).toPath()));
            OpenAir.Output o = OpenAir.build(r, new HashMap<>());
            System.out.println("DABS " + r.date + " V" + r.version + ": " + r.entries.size() + " Eintraege");
            for (String s : o.report) System.out.println("  " + s);
            return;
        }
        String which = a.length > 0 ? a[0].toUpperCase() : "CH";
        File dir = new File(a.length > 1 ? a[1] : ".");
        // optional: Ordner mit eigenen Luftraumdateien (wie im XCSoar-Ordner) für echte Umrisse
        dabs.core.LocalGeometry local = null;
        if (a.length > 2) {
            dabs.core.XcsoarProfile.Config cfg = dabs.core.XcsoarProfile.read(new File(a[2]));
            local = dabs.core.LocalGeometry.scanFiles(cfg.files);
            System.out.println("Aktives Profil: " + cfg.profile + ", eingebunden: " + cfg.files);
        }
        for (Country c : Country.values()) {
            if (!which.equals("ALL") && !which.equals(c.name())) continue;
            System.out.println("== " + c.label + " (" + c.source + ")");
            try {
                Country.Result r = c.fetch(Country.today(), local);
                File f = new File(dir, c.fileName);
                Country.write(f, r.text);
                System.out.println("   " + r.stand + ": " + r.count + " Gebiete -> " + f);
                for (String s : r.report) System.out.println("   " + s);
            } catch (Exception e) {
                System.out.println("   FEHLER: " + e);
            }
        }
    }
}
