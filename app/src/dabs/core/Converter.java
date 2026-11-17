package dabs.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/** Download + Umwandlung + atomares Schreiben. Läuft auf PC und Android. */
public final class Converter {
    public static final String DABS_URL = "https://www.skybriefing.com/o/dabs?";
    public static final String SHV_URL = "https://airspace.shv-fsvl.ch/api/v2/native/airspaces";
    public static final String FILE_NAME = "dabs.txt";

    public static final class Summary {
        public String date, version, generated;
        public int count;
        public List<String> report;
        public File file;
        public byte[] pdf; // Original-DABS, wie heruntergeladen
        public String text; // OpenAir
    }

    /** Nur laden und umwandeln, ohne zu schreiben. */
    public static Summary convert(String day) throws Exception {
        return convert(day, null);
    }

    public static Summary convert(String day, LocalGeometry local) throws Exception {
        byte[] pdf = fetch(DABS_URL + day);
        DabsPdf.Result dabs = DabsPdf.parse(pdf);
        if (dabs.date == null) throw new Exception("DABS-PDF unlesbar (kein Datum gefunden)");
        String iso = OpenAir.isoDate(dabs.date);
        String json = new String(fetch(SHV_URL), Charset.forName("UTF-8"));
        OpenAir.Output o = OpenAir.build(dabs, OpenAir.indexShv(json, iso), local);
        Summary s = new Summary();
        s.date = iso;
        s.version = dabs.version;
        s.generated = dabs.generated;
        s.count = dabs.entries.size();
        s.report = o.report;
        s.pdf = pdf;
        s.text = o.text;
        return s;
    }

    public static byte[] fetch(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "dabs2openair/1.0");
        int code = c.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code + " bei " + url);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    /** day = "today" oder "tomorrow" */
    public static Summary run(String day, File outFile) throws Exception {
        byte[] pdf = fetch(DABS_URL + day);
        DabsPdf.Result dabs = DabsPdf.parse(pdf);
        if (dabs.date == null) throw new Exception("DABS-PDF unlesbar (kein Datum gefunden)");
        String iso = OpenAir.isoDate(dabs.date);
        String json = new String(fetch(SHV_URL), Charset.forName("UTF-8"));
        Map<String, List<Map<String, Object>>> geo = OpenAir.indexShv(json, iso);
        OpenAir.Output o = OpenAir.build(dabs, geo);

        File dir = outFile.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs())
            throw new Exception("Ordner nicht anlegbar: " + dir);
        File tmp = new File(dir, outFile.getName() + ".tmp");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), Charset.forName("UTF-8"))) {
            w.write(o.text);
        }
        if (!tmp.renameTo(outFile)) {
            outFile.delete();
            if (!tmp.renameTo(outFile)) throw new Exception("Konnte " + outFile + " nicht schreiben");
        }
        Summary s = new Summary();
        s.date = iso;
        s.version = dabs.version;
        s.generated = dabs.generated;
        s.count = dabs.entries.size();
        s.report = o.report;
        s.file = outFile;
        s.pdf = pdf;
        return s;
    }
}
