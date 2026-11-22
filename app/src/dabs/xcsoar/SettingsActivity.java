package dabs.xcsoar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.text.Html;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Einstellungen: Programmhinweise, XCSoar-Versionen, Dateizugriff, Copyright & Datenquellen. */
public class SettingsActivity extends Activity {
    private Ui ui;
    private LinearLayout body;

    @Override
    protected void onCreate(Bundle b) {
        Ui.theme(this);
        super.onCreate(b);
        ui = new Ui(this);
        LinearLayout col = ui.page();
        ui.backHeader(col, "Einstellungen");
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        col.addView(body);
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        body.removeAllViews();

        // --- Programmhinweise
        body.addView(ui.section("So funktioniert's"));
        LinearLayout h = ui.card(true);
        h.addView(html(
                "Beim <b>Öffnen der App</b> werden die heute aktiven Militär- und Sperrgebiete der gewählten Länder "
                + "geladen und je Land als OpenAir-Datei in die XCSoar-Ordner geschrieben "
                + "(XCSoar 7.45+: Unterordner <i>airspace</i>). Die App läuft nicht im Hintergrund.<br><br>"
                + "<b>Einmalig in XCSoar:</b> Konfiguration → System → Standort-Dateien → Luftraum: "
                + "<i>dabs.txt</i> bzw. <i>mil-xx.txt</i> <b>zusätzlich</b> zur normalen Luftraumdatei auswählen. "
                + "XCSoar liest Luftraumdateien nur beim Start ein – also erst diese App öffnen, dann XCSoar.<br><br>"
                + "<b>Eigene Farbe:</b> Alle Gebiete haben die Klasse <b>TFR</b>. In XCSoar unter Luftraum → Farben "
                + "der Klasse TFR z. B. Violett geben. Die offizielle Kategorie steht im Namen: "
                + "[R] Sperrgebiet, [D] Gefahrengebiet, [P] Verbotszone, [TRA], [TSA], [MTA], [Warnung].<br><br>"
                + "<b>Grenzen:</b> Vorrang hat die offizielle Quelle. Nur wenn diese keine Grenzen liefert, "
                + "wird eine im aktiven XCSoar-Profil eingebundene Luftraumdatei genutzt – gekennzeichnet mit "
                + "„Grenzen aus &lt;Datei&gt;“. Sonst ein ungefährer Kreis aus dem NOTAM.<br><br>"
                + "Namen enthalten Datum und Zeiten (UTC). Steht ein altes Datum im Namen, war das Laden nicht erfolgreich."));
        body.addView(h);

        // --- XCSoar-Versionen
        body.addView(ui.section("XCSoar-Versionen"));
        LinearLayout v = ui.card(true);
        v.addView(ui.text("In welche Installationen sollen die Dateien geschrieben werden?", 13, ui.sub, false));
        for (Updater.Install i : Updater.installs()) {
            String ver = Updater.version(this, i);
            boolean exists = i.exists();
            CheckBox cb = new CheckBox(this);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(ui.accent));
            cb.setText(i.label + (ver != null ? " " + ver : ""));
            cb.setTextColor(exists ? ui.text : ui.sub);
            cb.setTextSize(15);
            cb.setChecked(exists && Updater.installEnabled(this, i));
            cb.setEnabled(exists);
            cb.setOnCheckedChangeListener((x, on) -> {
                Updater.setInstallEnabled(this, i, on);
                if (!on) Updater.removeAllFiles(i);
                MainActivity.dirty = true;
            });
            v.addView(cb);
            TextView p = ui.text(exists ? i.target().getPath() : "nicht gefunden", 12, ui.sub, false);
            p.setPadding(ui.dp(32), 0, 0, ui.dp(4));
            v.addView(p);
        }
        body.addView(v);

        // --- Dateizugriff
        body.addView(ui.section("Dateizugriff"));
        LinearLayout f = ui.card(true);
        boolean ok = Updater.hasStorageAccess(this);
        f.addView(ui.text(ok ? "✓ Zugriff auf alle Dateien erlaubt" : "✕ Zugriff auf alle Dateien fehlt",
                15, ok ? Ui.GREEN : Ui.RED, true));
        if (!ok) {
            Button g = ui.button("Dateizugriff erlauben", true);
            g.setOnClickListener(x -> requestStorage());
            f.addView(g);
        }
        body.addView(f);

        // --- Copyright & Datenquellen
        body.addView(ui.section("Copyright & Datenquellen"));
        LinearLayout c = ui.card(true);
        c.addView(html("<b>DABS für XCSoar</b> – Version " + versionName() + " (Beta)<br>"
                + "© 2026 Nadja · Lizenz: MIT · "
                + "<a href=\"https://github.com/fly-to-gecko/dabs-to-xcsoar\">github.com/fly-to-gecko/dabs-to-xcsoar</a><br><br>"
                + "<b>Datenquellen</b> (werden direkt von dort geladen, die App enthält keine Luftraumdaten):<br>"
                + "• Schweiz: DABS © Skyguide (<a href=\"https://www.skybriefing.com\">skybriefing.com</a>), "
                + "Gebietsgrenzen © SHV/FSVL (<a href=\"https://airspace.shv-fsvl.ch\">airspace.shv-fsvl.ch</a>)<br>"
                + "• Deutschland: WebAUP © DFS Deutsche Flugsicherung<br>"
                + "• Tschechien: AUP/UUP © ŘLP ČR / ANS CR (<a href=\"https://aup.ans.cz\">aup.ans.cz</a>), "
                + "Gebietsgrenzen: Luftraumdatei des Aeroklub ČR (J. Zahradka) über das XCSoar-Repository<br>"
                + "• Österreich, Frankreich: NOTAM über den NOTAM-Dienst von XCSoar (notam.xcsoar.org, Daten der FAA-NOTAM-API)<br>"
                + "• Ersatz-Grenzen: deine eigenen, in XCSoar eingebundenen Luftraumdateien (nur gelesen)<br><br>"
                + "<b>Haftungsausschluss:</b> Inoffizielles Hilfsmittel, ohne Gewähr. Nicht als primäres "
                + "Navigationsmittel verwenden. Maßgebend sind ausschließlich AIP, DABS/AUP und NOTAM der "
                + "zuständigen Stellen. XCSoar ist ein eigenständiges Projekt (GPL) und steht in keiner "
                + "Verbindung zu dieser App."));
        body.addView(c);
    }

    private TextView html(String s) {
        TextView t = ui.text(Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY), 14, ui.text, false);
        t.setLinkTextColor(ui.accent);
        t.setMovementMethod(LinkMovementMethod.getInstance());
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    private void requestStorage() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        MainActivity.dirty = true;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
