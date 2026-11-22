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
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(40000);
        c.setRequestProperty("User-Agent", UA);
        if (!cookies.isEmpty()) c.setRequestProperty("Cookie", String.join("; ", cookies));
        if (form != null) {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            try (OutputStream o = c.getOutputStream()) { o.write(form.getBytes(UTF8)); }
        }
        int code = c.getResponseCode();
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
