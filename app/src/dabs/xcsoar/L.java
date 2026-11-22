package dabs.xcsoar;

import android.content.Context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Übersetzungen (Deutsch, Englisch, Französisch). Standard: Systemsprache, sonst Englisch;
 * in den Einstellungen wählbar. Bewusst ohne Android-Ressourcen, damit der Build einfach bleibt.
 */
final class L {
    static final String[] CODES = {"auto", "de", "en", "fr"};
    private static int lang = 0; // 0 = de, 1 = en, 2 = fr
    private static final Map<String, String[]> T = new HashMap<>();

    private static void t(String key, String de, String en, String fr) {
        T.put(key, new String[]{de, en, fr});
    }

    /** Text holen; Platzhalter %s/%d wie String.format. */
    static String s(String key, Object... args) {
        String[] v = T.get(key);
        String r = v == null ? key : v[lang];
        return args.length == 0 ? r : String.format(Locale.ROOT, r, args);
    }

    static String code() { return new String[]{"de", "en", "fr"}[lang]; }

    /** Gewählte Sprache anwenden (vor dem Aufbau jeder Seite aufrufen). */
    static void init(Context c) {
        String pref = Updater.prefs(c).getString("lang", "auto");
        String l = pref.equals("auto") ? Locale.getDefault().getLanguage() : pref;
        lang = l.equals("de") ? 0 : l.equals("fr") ? 2 : 1;
        // Kennzeichnung in den OpenAir-Namen (sichtbar in XCSoar)
        dabs.core.Area.NOTE_PREFIX = s("note_prefix");
        dabs.core.Area.WARNING_LABEL = s("cat_warning");
    }

    static String pref(Context c) { return Updater.prefs(c).getString("lang", "auto"); }

    static void setPref(Context c, String code) { Updater.prefs(c).edit().putString("lang", code).apply(); }

    static String country(dabs.core.Country k) { return s("c_" + k.name()); }

    /** Bekannte deutsche Wörter aus dem Kern (Gebietsliste, Stand, Fehler) übersetzen. */
    static String core(String text) {
        if (lang == 0 || text == null) return text;
        String[][] map = {
                {"Umriss", "outline", "contour"},
                {"Grenzen aus", "limits from", "limites de"},
                {"Kreis ~", "circle ~", "cercle ~"},
                {"Kreis ", "circle ", "cercle "},
                {"nicht in eingebundenen Dateien", "not in configured files", "absent des fichiers configurés"},
                {"NUR INFO (nicht in Karte)", "INFO ONLY (not on chart)", "INFO SEULEMENT (hors carte)"},
                {"NUR INFO", "INFO ONLY", "INFO SEULEMENT"},
                {"Storniert (UUP)", "Cancelled (UUP)", "Annulé (UUP)"},
                {"kein Umriss gefunden", "no outline found", "aucun contour trouvé"},
                {"ohne Umriss – übersprungen", "no outline – skipped", "sans contour – ignoré"},
                {"NOTAM-Abfragen fehlgeschlagen", "NOTAM requests failed", "requêtes NOTAM échouées"},
                {" von ", " of ", " sur "},
                {"geladen", "loaded", "chargé"},
                {"gueltig", "valid", "valable"},
                {"Version", "version", "version"},
                {"NOTAM-Dienst nicht erreichbar", "NOTAM service unreachable", "service NOTAM injoignable"},
                {"DABS für dieses Datum ist noch nicht veröffentlicht (kommt ca. 16:00 Uhr am Vortag).",
                        "DABS for this date not yet published (around 16:00 the day before).",
                        "DABS pour cette date pas encore publié (vers 16h00 la veille)."},
        };
        String r = text;
        for (String[] m : map) r = r.replace(m[0], m[lang]);
        return r;
    }

    static {
        // --- allgemein
        t("app_name", "DABS für XCSoar", "DABS for XCSoar", "DABS pour XCSoar");
        t("subtitle", "Aktive Militär-Lufträume · %s", "Active military airspace · %s", "Espaces militaires actifs · %s");
        t("countries", "Länder", "Countries", "Pays");
        t("source", "Quelle", "Source", "Source");
        t("reload_all", "⟳  Alle neu laden", "⟳  Reload all", "⟳  Tout recharger");
        t("loading_progress", "Lädt …  %d von %d fertig", "Loading …  %d of %d done", "Chargement …  %d sur %d");
        t("footer", "Version %s (Beta) · ohne Gewähr – maßgebend sind AIP, DABS/AUP und NOTAM",
                "Version %s (beta) · no warranty – AIP, DABS/AUP and NOTAM are authoritative",
                "Version %s (bêta) · sans garantie – seuls l'AIP, le DABS/AUP et les NOTAM font foi");
        t("storage_missing", "Dateizugriff fehlt", "File access missing", "Accès aux fichiers manquant");
        t("storage_why_main", "Die App braucht „Zugriff auf alle Dateien“, um die Lufträume in den XCSoar-Ordner zu schreiben.",
                "The app needs “All files access” to write the airspace files into the XCSoar folder.",
                "L'application a besoin de « l'accès à tous les fichiers » pour écrire les espaces aériens dans le dossier XCSoar.");
        t("storage_grant", "Dateizugriff erlauben", "Allow file access", "Autoriser l'accès aux fichiers");
        t("toast_storage_first", "Bitte zuerst den Dateizugriff erlauben", "Please allow file access first", "Veuillez d'abord autoriser l'accès aux fichiers");
        t("toast_no_browser", "Kein Browser gefunden", "No browser found", "Aucun navigateur trouvé");
        t("settings", "Einstellungen", "Settings", "Réglages");

        t("c_CH", "Schweiz", "Switzerland", "Suisse");
        t("c_DE", "Deutschland", "Germany", "Allemagne");
        t("c_CZ", "Tschechien", "Czechia", "Tchéquie");
        t("c_AT", "Österreich", "Austria", "Autriche");
        t("c_FR", "Frankreich", "France", "France");

        // --- Status
        t("st_off", "aus", "off", "désactivé");
        t("st_loading", "lädt …", "loading …", "chargement …");
        t("st_areas1", "%d Gebiet", "%d area", "%d zone");
        t("st_areas", "%d Gebiete", "%d areas", "%d zones");
        t("st_today", "heute %s", "today %s", "aujourd'hui %s");
        t("st_with_local", "mit Ersatz-Grenzen", "with substitute limits", "avec limites de substitution");
        t("st_error", "Fehler · antippen für Details", "Error · tap for details", "Erreur · toucher pour détails");
        t("st_interrupted", "abgebrochen – unten neu laden", "interrupted – reload below", "interrompu – recharger en bas");
        t("st_never", "noch nicht geladen", "not loaded yet", "pas encore chargé");
        t("state_ok", "Aktuell", "Up to date", "À jour");
        t("state_running", "Wird geladen", "Loading", "Chargement");
        t("state_error", "Fehler beim Laden", "Loading failed", "Échec du chargement");
        t("state_interrupted", "Abgebrochen", "Interrupted", "Interrompu");
        t("state_off", "Ausgeschaltet", "Switched off", "Désactivé");
        t("state_never", "Noch nicht geladen", "Not loaded yet", "Pas encore chargé");
        t("err_no_install", "Keine XCSoar-Installation gefunden bzw. gewählt (Einstellungen). XCSoar einmal starten?",
                "No XCSoar installation found or selected (settings). Start XCSoar once?",
                "Aucune installation XCSoar trouvée ou choisie (réglages). Lancer XCSoar une fois ?");

        t("st_daily", "1× täglich", "once a day", "1× par jour");
        t("block_daily", "Deutschland wird höchstens einmal am Tag geladen (Rücksicht auf die DFS) – heute bereits erledigt.",
                "Germany is loaded at most once a day (out of respect for DFS) – already done today.",
                "L'Allemagne n'est chargée qu'une fois par jour (par égard pour la DFS) – déjà fait aujourd'hui.");
        t("block_until", "Gerade erst geladen – erneut möglich ab %s (schont die Server).",
                "Just loaded – next reload possible from %s (to spare the servers).",
                "Chargé à l'instant – nouveau chargement possible à partir de %s (pour ménager les serveurs).");
        t("toast_all_current", "Alle Daten sind aktuell", "All data is up to date", "Toutes les données sont à jour");
        t("last_update", "Letztes Daten-Update: heute %s (Lokalzeit)", "Last data update: today %s (local time)",
                "Dernière mise à jour : aujourd'hui %s (heure locale)");
        t("st_stale", "veraltet · Stand %s", "outdated · from %s", "périmé · état du %s");
        t("state_stale", "Veraltet – bitte neu laden", "Outdated – please reload", "Périmé – veuillez recharger");
        t("load_some", "⟳  Laden: %s", "⟳  Load: %s", "⟳  Charger : %s");
        t("reload_small", "Neu laden", "Reload", "Recharger");
        t("p_force", "Daten-Update erzwingen", "Force data update", "Forcer la mise à jour");
        t("m_force", "Alle gewählten Länder sofort neu laden – ohne Sperrzeiten", "Reload all selected countries now – ignoring time limits",
                "Recharger tout de suite tous les pays choisis – sans délais");
        t("force_note", "Umgeht auch die Begrenzung für Deutschland (1× täglich). Bitte sparsam verwenden.",
                "Also bypasses the limit for Germany (once a day). Please use sparingly.",
                "Contourne aussi la limite pour l'Allemagne (1× par jour). À utiliser avec modération.");
        t("force_started", "Update wird erzwungen …", "Forcing update …", "Mise à jour forcée …");
        t("k_next_dabs", "Nächstes DABS", "Next DABS", "Prochain DABS");
        t("next_dabs", "Das DABS für morgen erscheint ab ca. 16:00 Uhr (Schweizer Zeit); im Lauf des Tages kann es neue Versionen geben.",
                "Tomorrow's DABS is published from about 16:00 (Swiss time); new versions may appear during the day.",
                "Le DABS du lendemain paraît vers 16h00 (heure suisse) ; de nouvelles versions peuvent paraître dans la journée.");

        // --- Details
        t("open_dabs_pdf", "Original-DABS (PDF)", "Original DABS (PDF)", "DABS original (PDF)");
        t("open_source", "Originalquelle öffnen", "Open original source", "Ouvrir la source");
        t("reload", "Neu laden", "Reload", "Recharger");
        t("sec_error", "Fehler", "Error", "Erreur");
        t("keep_old", "Die Datei vom letzten erfolgreichen Laden bleibt bestehen – Datum im Gebietsnamen beachten.",
                "The file from the last successful load is kept – check the date in the area names.",
                "Le fichier du dernier chargement réussi est conservé – vérifier la date dans les noms.");
        t("sec_info", "Info", "Info", "Infos");
        t("k_source", "Quelle", "Source", "Source");
        t("k_file", "Datei", "File", "Fichier");
        t("k_stand", "Stand", "Status", "État");
        t("k_local", "Ersatz-Grenzen aus", "Substitute limits from", "Limites de substitution depuis");
        t("no_local", "– (keine in XCSoar eingebundene Luftraumdatei)", "– (no airspace file configured in XCSoar)",
                "– (aucun fichier d'espace aérien configuré dans XCSoar)");
        t("sec_areas", "Gebiete heute (%d)", "Areas today (%d)", "Zones aujourd'hui (%d)");
        t("no_areas", "Heute keine Aktivierungen.", "No activations today.", "Aucune activation aujourd'hui.");
        t("areas_hint", "Zeiten in UTC. „Kreis ~x NM“ = ungefähre Lage aus dem NOTAM; „Grenzen aus …“ = Umriss aus einer in XCSoar eingebundenen Datei.",
                "Times in UTC. “circle ~x NM” = approximate position from the NOTAM; “limits from …” = outline from a file configured in XCSoar.",
                "Heures UTC. « cercle ~x NM » = position approximative du NOTAM ; « limites de … » = contour d'un fichier configuré dans XCSoar.");

        // --- Einstellungen
        t("p_help", "So funktioniert's", "How it works", "Fonctionnement");
        t("p_installs", "XCSoar-Versionen", "XCSoar versions", "Versions XCSoar");
        t("p_storage", "Dateizugriff", "File access", "Accès aux fichiers");
        t("p_lang", "Sprache", "Language", "Langue");
        t("p_about", "Copyright & Quellen", "Copyright & sources", "Copyright & sources");
        t("m_help", "Programmhinweise, Einrichtung in XCSoar, Farben", "Notes, setup in XCSoar, colours", "Notes, réglage dans XCSoar, couleurs");
        t("m_installs1", "%d Installation ausgewählt", "%d installation selected", "%d installation choisie");
        t("m_installs", "%d Installationen ausgewählt", "%d installations selected", "%d installations choisies");
        t("m_storage_ok", "✓ erlaubt", "✓ allowed", "✓ autorisé");
        t("m_storage_missing", "✕ fehlt – bitte erlauben", "✕ missing – please allow", "✕ manquant – à autoriser");
        t("m_about", "Version %s · Lizenz · Datenquellen", "Version %s · licence · data sources", "Version %s · licence · sources");
        t("lang_auto", "Automatisch (Systemsprache)", "Automatic (system language)", "Automatique (langue du système)");
        t("installs_q", "In welche Installationen sollen die Dateien geschrieben werden?",
                "Which installations should the files be written to?", "Dans quelles installations écrire les fichiers ?");
        t("legacy_dir", "XCSoar (alter Ordner XCSoarData)", "XCSoar (old folder XCSoarData)", "XCSoar (ancien dossier XCSoarData)");
        t("not_found", "nicht gefunden", "not found", "introuvable");
        t("installs_note", "Als Ersatz-Grenzen werden nur Luftraumdateien aus den aktiven Profilen der hier gewählten Installationen genutzt. Abwählen entfernt die App-Dateien dort.",
                "Substitute limits only come from airspace files in the active profiles of the installations selected here. Deselecting removes the app's files there.",
                "Les limites de substitution ne proviennent que des fichiers des profils actifs des installations choisies ici. Décocher supprime les fichiers de l'application.");
        t("storage_ok", "✓ Zugriff auf alle Dateien erlaubt", "✓ All files access allowed", "✓ Accès à tous les fichiers autorisé");
        t("storage_no", "✕ Zugriff auf alle Dateien fehlt", "✕ All files access missing", "✕ Accès à tous les fichiers manquant");
        t("storage_why", "Nötig, weil die App in den Ordner einer anderen App (XCSoar) schreibt. Gelesen werden nur die XCSoar-Ordner.",
                "Needed because the app writes into the folder of another app (XCSoar). Only the XCSoar folders are read.",
                "Nécessaire car l'application écrit dans le dossier d'une autre application (XCSoar). Seuls les dossiers XCSoar sont lus.");

        t("help_html",
                "Auf <b>Knopfdruck</b> werden die heute aktiven Militär- und Sperrgebiete der gewählten Länder geladen und je Land als OpenAir-Datei in die XCSoar-Ordner geschrieben (XCSoar 7.45+: Unterordner <i>airspace</i>). Geladen wird nur auf Knopfdruck – und zwar nur Länder, die noch nicht aktuell sind (z. B. neu angehakt). Abgewählte Länder bekommen eine leere Datei, damit XCSoar keinen Fehler meldet. Die App läuft nicht im Hintergrund.<br><br>"
                        + "<b>Einmalig in XCSoar:</b> Konfiguration → System → Standort-Dateien → Luftraum: <i>dabs.txt</i> bzw. <i>mil-xx.txt</i> <b>zusätzlich</b> zur normalen Luftraumdatei auswählen. XCSoar liest Luftraumdateien nur beim Start ein – also erst diese App öffnen, dann XCSoar.<br><br>"
                        + "<b>Eigene Farbe:</b> Alle Gebiete haben die Klasse <b>TFR</b>. In XCSoar unter Luftraum → Farben der Klasse TFR z. B. Violett geben. Die offizielle Kategorie steht im Namen: [R] Sperrgebiet, [D] Gefahrengebiet, [P] Verbotszone, [TRA], [TSA], [MTA], [Warnung].<br><br>"
                        + "<b>Grenzen:</b> Vorrang hat die offizielle Quelle. Nur wenn diese keine Grenzen liefert, wird eine im aktiven XCSoar-Profil eingebundene Luftraumdatei genutzt – gekennzeichnet mit „Grenzen aus &lt;Datei&gt;“. Sonst ein ungefährer Kreis aus dem NOTAM.<br><br>"
                        + "Namen enthalten Datum und Zeiten (UTC). Steht ein altes Datum im Namen, war das Laden nicht erfolgreich.",
                "On <b>request</b> (button) the app loads today's active military and restricted areas of the selected countries and writes one OpenAir file per country into the XCSoar folders (XCSoar 7.45+: subfolder <i>airspace</i>). Loading only happens on request – and only for countries that are not yet up to date (e.g. newly ticked). Deselected countries get an empty file so XCSoar reports no error. The app does not run in the background.<br><br>"
                        + "<b>Once in XCSoar:</b> Config → System → Site files → Airspace: select <i>dabs.txt</i> or <i>mil-xx.txt</i> <b>in addition</b> to your normal airspace file. XCSoar only reads airspace files at startup – so open this app first, then XCSoar.<br><br>"
                        + "<b>Own colour:</b> All areas use the class <b>TFR</b>. In XCSoar under Airspace → Colours give class TFR e.g. violet. The official category is in the name: [R] restricted, [D] danger, [P] prohibited, [TRA], [TSA], [MTA], [Warning].<br><br>"
                        + "<b>Limits:</b> The official source has priority. Only if it provides no limits, an airspace file configured in the active XCSoar profile is used – marked “limits from &lt;file&gt;”. Otherwise an approximate circle from the NOTAM.<br><br>"
                        + "Names contain the date and times (UTC). An old date in the name means loading was not successful.",
                "Sur <b>demande</b> (bouton), les zones militaires et réglementées actives du jour des pays choisis sont chargées et écrites, un fichier OpenAir par pays, dans les dossiers XCSoar (XCSoar 7.45+ : sous-dossier <i>airspace</i>). Le chargement se fait uniquement sur demande – et seulement pour les pays pas encore à jour (p. ex. nouvellement cochés). Les pays décochés reçoivent un fichier vide pour qu'XCSoar ne signale pas d'erreur. L'application ne tourne pas en arrière-plan.<br><br>"
                        + "<b>Une fois dans XCSoar :</b> Configuration → Système → Fichiers du site → Espace aérien : choisir <i>dabs.txt</i> ou <i>mil-xx.txt</i> <b>en plus</b> du fichier habituel. XCSoar ne lit les fichiers qu'au démarrage – ouvrir d'abord cette application, puis XCSoar.<br><br>"
                        + "<b>Couleur propre :</b> Toutes les zones ont la classe <b>TFR</b>. Dans XCSoar sous Espace aérien → Couleurs, donner à la classe TFR p. ex. violet. La catégorie officielle figure dans le nom : [R] réglementée, [D] dangereuse, [P] interdite, [TRA], [TSA], [MTA], [Avertissement].<br><br>"
                        + "<b>Limites :</b> la source officielle est prioritaire. Seulement si elle ne fournit pas de limites, un fichier configuré dans le profil XCSoar actif est utilisé – marqué « limites de &lt;fichier&gt; ». Sinon un cercle approximatif issu du NOTAM.<br><br>"
                        + "Les noms contiennent la date et les heures (UTC). Une date ancienne signifie que le chargement a échoué.");

        t("about_html",
                "<b>DABS für XCSoar</b> – Version %s (Beta)<br>© 2026 Nadja · Lizenz: MIT · <a href=\"https://github.com/fly-to-gecko/dabs-to-xcsoar\">github.com/fly-to-gecko/dabs-to-xcsoar</a><br><br>"
                        + "<b>Datenquellen</b> (werden direkt von dort geladen, die App enthält keine Luftraumdaten):<br>"
                        + "• Schweiz: DABS © Skyguide (<a href=\"https://www.skybriefing.com\">skybriefing.com</a>), Gebietsgrenzen © SHV/FSVL (<a href=\"https://airspace.shv-fsvl.ch\">airspace.shv-fsvl.ch</a>)<br>"
                        + "• Deutschland: WebAUP © DFS Deutsche Flugsicherung<br>"
                        + "• Tschechien: AUP/UUP © ŘLP ČR / ANS CR (<a href=\"https://aup.ans.cz\">aup.ans.cz</a>), Gebietsgrenzen: Luftraumdatei des Aeroklub ČR (J. Zahradka) über das XCSoar-Repository<br>"
                        + "• Österreich, Frankreich: NOTAM über den NOTAM-Dienst von XCSoar (notam.xcsoar.org, Daten der FAA-NOTAM-API)<br>"
                        + "• Ersatz-Grenzen: deine eigenen, in XCSoar eingebundenen Luftraumdateien (nur gelesen)<br><br>"
                        + "<b>Haftungsausschluss:</b> Inoffizielles Hilfsmittel, ohne Gewähr. Nicht als primäres Navigationsmittel verwenden. Maßgebend sind ausschließlich AIP, DABS/AUP und NOTAM der zuständigen Stellen. XCSoar ist ein eigenständiges Projekt (GPL) und steht in keiner Verbindung zu dieser App.",
                "<b>DABS for XCSoar</b> – version %s (beta)<br>© 2026 Nadja · Licence: MIT · <a href=\"https://github.com/fly-to-gecko/dabs-to-xcsoar\">github.com/fly-to-gecko/dabs-to-xcsoar</a><br><br>"
                        + "<b>Data sources</b> (loaded directly from there; the app contains no airspace data):<br>"
                        + "• Switzerland: DABS © Skyguide (<a href=\"https://www.skybriefing.com\">skybriefing.com</a>), area limits © SHV/FSVL (<a href=\"https://airspace.shv-fsvl.ch\">airspace.shv-fsvl.ch</a>)<br>"
                        + "• Germany: WebAUP © DFS Deutsche Flugsicherung<br>"
                        + "• Czechia: AUP/UUP © ŘLP ČR / ANS CR (<a href=\"https://aup.ans.cz\">aup.ans.cz</a>), area limits: airspace file of Aeroklub ČR (J. Zahradka) via the XCSoar repository<br>"
                        + "• Austria, France: NOTAM via XCSoar's NOTAM service (notam.xcsoar.org, FAA NOTAM API data)<br>"
                        + "• Substitute limits: your own airspace files configured in XCSoar (read only)<br><br>"
                        + "<b>Disclaimer:</b> Unofficial tool, no warranty. Do not use as primary means of navigation. Only the AIP, DABS/AUP and NOTAM of the competent authorities are authoritative. XCSoar is an independent project (GPL) and not affiliated with this app.",
                "<b>DABS pour XCSoar</b> – version %s (bêta)<br>© 2026 Nadja · Licence : MIT · <a href=\"https://github.com/fly-to-gecko/dabs-to-xcsoar\">github.com/fly-to-gecko/dabs-to-xcsoar</a><br><br>"
                        + "<b>Sources des données</b> (chargées directement à la source ; l'application ne contient aucune donnée d'espace aérien) :<br>"
                        + "• Suisse : DABS © Skyguide (<a href=\"https://www.skybriefing.com\">skybriefing.com</a>), limites © SHV/FSVL (<a href=\"https://airspace.shv-fsvl.ch\">airspace.shv-fsvl.ch</a>)<br>"
                        + "• Allemagne : WebAUP © DFS Deutsche Flugsicherung<br>"
                        + "• Tchéquie : AUP/UUP © ŘLP ČR / ANS CR (<a href=\"https://aup.ans.cz\">aup.ans.cz</a>), limites : fichier de l'Aeroklub ČR (J. Zahradka) via le dépôt XCSoar<br>"
                        + "• Autriche, France : NOTAM via le service NOTAM de XCSoar (notam.xcsoar.org, données de l'API NOTAM de la FAA)<br>"
                        + "• Limites de substitution : vos propres fichiers configurés dans XCSoar (lecture seule)<br><br>"
                        + "<b>Avertissement :</b> outil non officiel, sans garantie. Ne pas utiliser comme moyen de navigation principal. Seuls l'AIP, le DABS/AUP et les NOTAM des autorités compétentes font foi. XCSoar est un projet indépendant (GPL) sans lien avec cette application.");

        // --- in XCSoar sichtbar (OpenAir-Namen)
        t("note_prefix", "Grenzen aus", "limits from", "limites de");
        t("cat_warning", "Warnung", "Warning", "Avertissement");
    }
}
