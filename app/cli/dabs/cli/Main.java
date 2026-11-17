package dabs.cli;

import dabs.core.Converter;
import dabs.core.DabsPdf;
import dabs.core.OpenAir;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;

/** PC-Test:  java -cp build/cli dabs.cli.Main [today|tomorrow] [ausgabe.txt]
 *            java -cp build/cli dabs.cli.Main --pdf datei.pdf   (nur Parser testen) */
public class Main {
    public static void main(String[] a) throws Exception {
        if (a.length >= 2 && a[0].equals("--pdf")) {
            DabsPdf.Result r = DabsPdf.parse(Files.readAllBytes(new File(a[1]).toPath()));
            OpenAir.Output o = OpenAir.build(r, new HashMap<>());
            System.out.println("DABS " + r.date + " V" + r.version + ": " + r.entries.size() + " Eintraege");
            for (String s : o.report) System.out.println("  " + s);
            return;
        }
        String day = a.length > 0 ? a[0] : "today";
        File out = new File(a.length > 1 ? a[1] : Converter.FILE_NAME);
        Converter.Summary s = Converter.run(day, out);
        System.out.println("DABS " + s.date + " V" + s.version + " (" + s.generated + "): "
                + s.count + " Aktivierungen -> " + s.file);
        for (String line : s.report) System.out.println("  " + line);
    }
}
