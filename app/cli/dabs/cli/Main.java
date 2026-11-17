package dabs.cli;

import dabs.core.Country;
import dabs.core.DabsPdf;
import dabs.core.OpenAir;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;

/** PC-Test:
 *   java -cp build/cli dabs.cli.Main [CH|DE|CZ|AT|FR|ALL] [ausgabe-ordner]
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
        for (Country c : Country.values()) {
            if (!which.equals("ALL") && !which.equals(c.name())) continue;
            System.out.println("== " + c.label + " (" + c.source + ")");
            try {
                Country.Result r = c.fetch(Country.today());
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
