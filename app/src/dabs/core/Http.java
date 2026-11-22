package dabs.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Kleine HTTP-Hilfe mit Cookie-Weitergabe (für die DFS-Sitzung). */
public final class Http {
    static final String UA = "dabs-to-xcsoar (github.com/fly-to-gecko/dabs-to-xcsoar)";
    static final Charset UTF8 = Charset.forName("UTF-8");

    private final List<String> cookies = new ArrayList<>();

    /** Zwischenspeicher für selten geänderte Daten (Gebietsgrenzen); null = kein Cache (PC). */
    public static java.io.File cacheDir;
    public static final long DAY_MS = 24L * 3600 * 1000;

    /**
     * Wie request(url, null), aber aus dem Cache, solange jünger als maxAgeMs.
     * Schlägt der Download fehl, wird ein älterer Cache-Stand verwendet.
     */
    public byte[] cached(String url, long maxAgeMs) throws Exception {
        if (cacheDir == null) return request(url, null);
        java.io.File f = new java.io.File(cacheDir, Integer.toHexString(url.hashCode()) + ".bin");
        if (f.isFile() && System.currentTimeMillis() - f.lastModified() < maxAgeMs) return readFile(f);
        try {
            byte[] data = request(url, null);
            cacheDir.mkdirs();
            java.io.File tmp = new java.io.File(cacheDir, f.getName() + ".tmp");
            try (java.io.FileOutputStream o = new java.io.FileOutputStream(tmp)) { o.write(data); }
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f); }
            return data;
        } catch (Exception e) {
            if (f.isFile()) return readFile(f); // offline: letzter Stand
            throw e;
        }
    }

    private static byte[] readFile(java.io.File f) throws Exception {
        return java.nio.file.Files.readAllBytes(f.toPath());
    }

    public String get(String url) throws Exception {
        return new String(request(url, null), UTF8);
    }

    public String post(String url, String form) throws Exception {
        return new String(request(url, form), UTF8);
    }

    public byte[] request(String url, String form) throws Exception {
        return request(url, form, null, null, null);
    }

    /** JSON per POST senden (NOTAM-Delta-Modus). */
    public String postJson(String url, String json) throws Exception {
        return new String(request(url, json, "application/json", null, null), UTF8);
    }

    /**
     * Bedingte Abfrage: Der Server antwortet mit "304 nicht geändert", wenn sich die Seite seit
     * dem letzten Abruf nicht geändert hat – dann wird der gespeicherte Stand verwendet (kaum Datenverkehr).
     */
    public byte[] conditional(String url) throws Exception {
        if (cacheDir == null) return request(url, null);
        String key = Integer.toHexString(url.hashCode());
        java.io.File body = new java.io.File(cacheDir, key + ".body"), meta = new java.io.File(cacheDir, key + ".meta");
        String[] val = new String[2]; // [ETag, Last-Modified]
        if (body.isFile() && meta.isFile()) {
            String[] m = new String(readFile(meta), UTF8).split("\n", -1);
            if (m.length >= 2) { val[0] = m[0]; val[1] = m[1]; }
        }
        String[] got = new String[2];
        byte[] data = request(url, null, null, body.isFile() ? val : null, got);
        if (data == null) { conditionalHits++; return readFile(body); } // 304
        cacheDir.mkdirs();
        try (java.io.FileOutputStream o = new java.io.FileOutputStream(body)) { o.write(data); }
        try (java.io.FileOutputStream o = new java.io.FileOutputStream(meta)) {
            o.write(((got[0] == null ? "" : got[0]) + "\n" + (got[1] == null ? "" : got[1])).getBytes(UTF8));
        }
        return data;
    }

    /** Zähler für Tests/Anzeige: wie oft der Server "nicht geändert" gemeldet hat. */
    public static int conditionalHits;

    /**
     * @param contentType  für POST (null = Formular)
     * @param ifNone       [ETag, Last-Modified] für bedingte Abfrage, sonst null
     * @param validators   bekommt [ETag, Last-Modified] der Antwort
     * @return Antwort oder null bei 304
     */
    byte[] request(String url, String form, String contentType, String[] ifNone, String[] validators) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(40000);
        c.setRequestProperty("User-Agent", UA);
        if (!cookies.isEmpty()) c.setRequestProperty("Cookie", String.join("; ", cookies));
        if (ifNone != null) {
            if (ifNone[0] != null && !ifNone[0].isEmpty()) c.setRequestProperty("If-None-Match", ifNone[0]);
            if (ifNone[1] != null && !ifNone[1].isEmpty()) c.setRequestProperty("If-Modified-Since", ifNone[1]);
        }
        if (form != null) {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", contentType != null ? contentType : "application/x-www-form-urlencoded; charset=UTF-8");
            try (OutputStream o = c.getOutputStream()) { o.write(form.getBytes(UTF8)); }
        }
        int code = c.getResponseCode();
        if (code == 304) { c.disconnect(); return null; }
        if (validators != null) {
            validators[0] = c.getHeaderField("ETag");
            validators[1] = c.getHeaderField("Last-Modified");
        }
        for (Map.Entry<String, List<String>> h : c.getHeaderFields().entrySet()) {
            if (h.getKey() != null && h.getKey().equalsIgnoreCase("Set-Cookie"))
                for (String v : h.getValue()) cookies.add(v.split(";", 2)[0]);
        }
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
}
