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

        t("sec_sources", "Woher kommen die Daten?", "Where does the data come from?", "D'où viennent les données ?");
        t("sources_html",
                "<b>🇨🇭 Schweiz</b><br>"
                        + "<b>Aktivierungen:</b> das offizielle <i>DABS – Daily Airspace Bulletin Switzerland</i> von Skyguide (skybriefing.com), als PDF. Die erste Version erscheint gegen 16:00 Uhr für den Folgetag, im Lauf des Tages kann es weitere Versionen geben. Die App liest die Tabelle: Gebiet, Zeiten (UTC), Unter- und Obergrenze.<br>"
                        + "<b>Grenzen:</b> die Umrisse der LS-R/LS-D-Gebiete aus der Luftraum-Datenbank des Schweizerischen Hängegleiterverbands SHV/FSVL (airspace.shv-fsvl.ch). Temporäre Gebiete aus W-NOTAMs sind dort nicht enthalten – sie werden als Kreis aus Mittelpunkt und Radius des DABS gezeichnet. Einträge, die Skyguide selbst nicht in die DABS-Karte zeichnet (z. B. militärische Nachtflüge über der ganzen Schweiz), erscheinen nur als Hinweis in der App.<br>"
                        + "<b>Gegenprüfen:</b> „Quelle“ öffnet genau das ausgewertete PDF.<br><br>"
                        + "<b>🇩🇪 Deutschland</b><br>"
                        + "<b>Aktivierungen und Grenzen:</b> die WebAUP der DFS Deutsche Flugsicherung (Airspace Use Plan). Sie enthält pro Gebiet (ED-R, ED-D, TRA …) die Zeitfenster, Höhen und den Umriss.<br>"
                        + "<b>Einschränkungen:</b> Laut DFS zeigt die WebAUP nur Aktivierungen bis FL100 (in den Alpen FL130) und ist nicht vollständig – H24-Gebiete und Gebiete, die nur per NOTAM veröffentlicht werden, fehlen. Weil die DFS automatisierte Abrufe nur mit Zustimmung erlaubt, lädt die App Deutschland höchstens einmal am Tag (eine Anfrage bei der DFS läuft).<br>"
                        + "<b>Gegenprüfen:</b> „Quelle“ öffnet die WebAUP der DFS.<br><br>"
                        + "<b>🇨🇿 Tschechien</b><br>"
                        + "<b>Aktivierungen:</b> der tägliche Luftraum-Nutzungsplan (AUP) der tschechischen Flugsicherung ŘLP ČR / ANS CR (aup.ans.cz) und dessen Aktualisierungen (UUP). Stornierte Gebiete (CNL) werden entfernt.<br>"
                        + "<b>Grenzen:</b> aus der Luftraumdatei des Aeroklub ČR (über das XCSoar-Repository), die ausdrücklich frei verwendet werden darf.<br>"
                        + "<b>Gegenprüfen:</b> „Quelle“ öffnet aup.ans.cz.<br><br>"
                        + "<b>🇦🇹 Österreich und 🇫🇷 Frankreich</b><br>"
                        + "<b>Aktivierungen:</b> Für diese Länder gibt es keinen frei zugänglichen Tagesplan. Die App nutzt deshalb NOTAMs – über den NOTAM-Dienst von XCSoar (notam.xcsoar.org), einen laufend aktualisierten Zwischenspeicher der weltweiten NOTAM-Datenbank der FAA. Übernommen werden nur NOTAMs der jeweiligen FIR (Österreich: LOVV, Frankreich: LFBB, LFEE, LFFF, LFMM, LFRR), die heute gelten und eine Aktivität bedeuten (z. B. „LOR6 ACTIVATED“, Militär-Übungsräume). Reine Änderungsmeldungen werden aussortiert.<br>"
                        + "<b>Grenzen:</b> (1) Koordinaten im NOTAM-Text; (2) sonst eine in deinem XCSoar-Profil eingebundene Luftraumdatei – im Namen gekennzeichnet mit „Grenzen aus &lt;Datei&gt;“; (3) sonst ein ungefährer Kreis aus dem NOTAM. Sehr große Kreise (über 50 NM) erscheinen nur als Hinweis.<br>"
                        + "<b>Was fehlt:</b> Alles, was nur im Luftraum-Nutzungsplan steht und nicht per NOTAM veröffentlicht wird – in Österreich ist der AUP nur im Homebriefing der Austro Control (mit Anmeldung) einsehbar, in Frankreich die AZBA/RTBA-Aktivität nur auf der Karte des SIA. Für diese Länder ist die App deshalb eine <b>Ergänzung</b>; maßgebend bleibt das offizielle Briefing.<br>"
                        + "<b>Gegenprüfen:</b> „Quelle“ öffnet Homebriefing bzw. die Seite des SIA.<br><br>"
                        + "<b>Allgemein:</b> Alle Zeiten in UTC. Die App enthält selbst keine Luftraumdaten, sie lädt sie direkt von den genannten Stellen und überträgt sie ins OpenAir-Format. Zeiten und Höhen stammen aus der jeweiligen Quelle; ergänzt werden nur Datum und Zeiten im Gebietsnamen, die Kategorie in eckigen Klammern sowie – wie oben beschrieben und gekennzeichnet – Ersatz-Grenzen oder Kreise.",
                "<b>🇨🇭 Switzerland</b><br>"
                        + "<b>Activations:</b> the official <i>DABS – Daily Airspace Bulletin Switzerland</i> by Skyguide (skybriefing.com), as a PDF. The first version is published around 16:00 for the next day; further versions may follow during the day. The app reads the table: area, times (UTC), lower and upper limit.<br>"
                        + "<b>Limits:</b> the outlines of the LS-R/LS-D areas from the airspace database of the Swiss Hang Gliding Association SHV/FSVL (airspace.shv-fsvl.ch). Temporary areas from W-NOTAMs are not included there – they are drawn as a circle from the DABS centre and radius. Entries that Skyguide itself does not draw on the DABS chart (e.g. military night flights over all of Switzerland) only appear as a note in the app.<br>"
                        + "<b>Cross-check:</b> “Source” opens exactly the evaluated PDF.<br><br>"
                        + "<b>🇩🇪 Germany</b><br>"
                        + "<b>Activations and limits:</b> the WebAUP of DFS Deutsche Flugsicherung (Airspace Use Plan). For each area (ED-R, ED-D, TRA …) it contains the time windows, altitudes and the outline.<br>"
                        + "<b>Limitations:</b> According to DFS the WebAUP only shows activations up to FL100 (FL130 in the Alps) and is not complete – H24 areas and areas published only by NOTAM are missing. Because DFS only allows automated retrieval with its consent, the app loads Germany at most once a day (a request to DFS is pending).<br>"
                        + "<b>Cross-check:</b> “Source” opens the DFS WebAUP.<br><br>"
                        + "<b>🇨🇿 Czechia</b><br>"
                        + "<b>Activations:</b> the daily Airspace Use Plan (AUP) of the Czech air navigation service ŘLP ČR / ANS CR (aup.ans.cz) and its updates (UUP). Cancelled areas (CNL) are removed.<br>"
                        + "<b>Limits:</b> from the airspace file of Aeroklub ČR (via the XCSoar repository), which may explicitly be used freely.<br>"
                        + "<b>Cross-check:</b> “Source” opens aup.ans.cz.<br><br>"
                        + "<b>🇦🇹 Austria and 🇫🇷 France</b><br>"
                        + "<b>Activations:</b> There is no freely accessible daily plan for these countries. The app therefore uses NOTAMs – via XCSoar's NOTAM service (notam.xcsoar.org), a continuously updated cache of the FAA's worldwide NOTAM database. Only NOTAMs of the respective FIR (Austria: LOVV, France: LFBB, LFEE, LFFF, LFMM, LFRR) that are valid today and mean an activity (e.g. “LOR6 ACTIVATED”, military training areas) are used. Mere change notices are filtered out.<br>"
                        + "<b>Limits:</b> (1) coordinates in the NOTAM text; (2) otherwise an airspace file configured in your XCSoar profile – marked “limits from &lt;file&gt;” in the name; (3) otherwise an approximate circle from the NOTAM. Very large circles (over 50 NM) only appear as a note.<br>"
                        + "<b>What is missing:</b> Everything that is only in the airspace use plan and not published by NOTAM – in Austria the AUP is only available in Austro Control's Homebriefing (login required), in France the AZBA/RTBA activity only on the SIA map. For these countries the app is therefore a <b>supplement</b>; the official briefing remains authoritative.<br>"
                        + "<b>Cross-check:</b> “Source” opens Homebriefing or the SIA website.<br><br>"
                        + "<b>General:</b> All times in UTC. The app itself contains no airspace data; it loads it directly from the bodies named above and converts it to OpenAir format. Times and altitudes come from the respective source; the app only adds the date and times to the area name, the category in square brackets and – as described and marked above – substitute limits or circles.",
                "<b>🇨🇭 Suisse</b><br>"
                        + "<b>Activations :</b> le <i>DABS – Daily Airspace Bulletin Switzerland</i> officiel de Skyguide (skybriefing.com), en PDF. La première version paraît vers 16h00 pour le lendemain ; d'autres versions peuvent suivre dans la journée. L'application lit le tableau : zone, heures (UTC), limites inférieure et supérieure.<br>"
                        + "<b>Limites :</b> les contours des zones LS-R/LS-D issus de la base de données de la Fédération suisse de vol libre SHV/FSVL (airspace.shv-fsvl.ch). Les zones temporaires des NOTAM W n'y figurent pas – elles sont dessinées en cercle à partir du centre et du rayon du DABS. Les entrées que Skyguide ne dessine pas sur la carte du DABS (p. ex. vols militaires de nuit sur toute la Suisse) n'apparaissent que comme note dans l'application.<br>"
                        + "<b>Vérifier :</b> « Source » ouvre exactement le PDF évalué.<br><br>"
                        + "<b>🇩🇪 Allemagne</b><br>"
                        + "<b>Activations et limites :</b> le WebAUP de la DFS Deutsche Flugsicherung (plan d'utilisation de l'espace aérien). Pour chaque zone (ED-R, ED-D, TRA …) il contient les créneaux horaires, les altitudes et le contour.<br>"
                        + "<b>Restrictions :</b> selon la DFS, le WebAUP n'affiche que les activations jusqu'au FL100 (FL130 dans les Alpes) et n'est pas complet – les zones H24 et celles publiées uniquement par NOTAM manquent. La DFS n'autorisant la récupération automatisée qu'avec son accord, l'application ne charge l'Allemagne qu'une fois par jour (une demande à la DFS est en cours).<br>"
                        + "<b>Vérifier :</b> « Source » ouvre le WebAUP de la DFS.<br><br>"
                        + "<b>🇨🇿 Tchéquie</b><br>"
                        + "<b>Activations :</b> le plan quotidien d'utilisation de l'espace aérien (AUP) du prestataire tchèque ŘLP ČR / ANS CR (aup.ans.cz) et ses mises à jour (UUP). Les zones annulées (CNL) sont retirées.<br>"
                        + "<b>Limites :</b> issues du fichier d'espace aérien de l'Aeroklub ČR (via le dépôt XCSoar), dont l'utilisation libre est expressément autorisée.<br>"
                        + "<b>Vérifier :</b> « Source » ouvre aup.ans.cz.<br><br>"
                        + "<b>🇦🇹 Autriche et 🇫🇷 France</b><br>"
                        + "<b>Activations :</b> il n'existe pas de plan quotidien librement accessible pour ces pays. L'application utilise donc les NOTAM – via le service NOTAM de XCSoar (notam.xcsoar.org), un cache continuellement mis à jour de la base NOTAM mondiale de la FAA. Seuls sont retenus les NOTAM de la FIR concernée (Autriche : LOVV, France : LFBB, LFEE, LFFF, LFMM, LFRR) valables aujourd'hui et signifiant une activité (p. ex. « LOR6 ACTIVATED », zones d'entraînement militaire). Les simples avis de modification sont écartés.<br>"
                        + "<b>Limites :</b> (1) coordonnées dans le texte du NOTAM ; (2) sinon un fichier d'espace aérien configuré dans votre profil XCSoar – signalé « limites de &lt;fichier&gt; » dans le nom ; (3) sinon un cercle approximatif issu du NOTAM. Les très grands cercles (plus de 50 NM) n'apparaissent que comme note.<br>"
                        + "<b>Ce qui manque :</b> tout ce qui figure uniquement dans le plan d'utilisation de l'espace et n'est pas publié par NOTAM – en Autriche, l'AUP n'est consultable que dans le Homebriefing d'Austro Control (connexion requise), en France l'activité AZBA/RTBA uniquement sur la carte du SIA. Pour ces pays, l'application est donc un <b>complément</b> ; seul le briefing officiel fait foi.<br>"
                        + "<b>Vérifier :</b> « Source » ouvre Homebriefing ou le site du SIA.<br><br>"
                        + "<b>Général :</b> toutes les heures en UTC. L'application ne contient elle-même aucune donnée d'espace aérien ; elle les charge directement auprès des organismes cités et les convertit au format OpenAir. Heures et altitudes proviennent de la source concernée ; l'application ajoute seulement la date et les heures au nom de la zone, la catégorie entre crochets et – comme décrit et signalé ci-dessus – des limites de substitution ou des cercles.");
        t("about_html",
                "<b>DABS für XCSoar</b> – Version %s (Beta)<br>© 2026 Nadja Cu · Lizenz: MIT · <a href=\"https://github.com/fly-to-gecko/dabs-to-xcsoar\">github.com/fly-to-gecko/dabs-to-xcsoar</a><br><br>"
                        + "<b>Datenquellen</b> (werden direkt von dort geladen, die App enthält keine Luftraumdaten):<br>"
                        + "• Schweiz: DABS © Skyguide (<a href=\"https://www.skybriefing.com\">skybriefing.com</a>), Gebietsgrenzen © SHV/FSVL (<a href=\"https://airspace.shv-fsvl.ch\">airspace.shv-fsvl.ch</a>)<br>"
                        + "• Deutschland: WebAUP © DFS Deutsche Flugsicherung<br>"
                        + "• Tschechien: AUP/UUP © ŘLP ČR / ANS CR (<a href=\"https://aup.ans.cz\">aup.ans.cz</a>), Gebietsgrenzen: Luftraumdatei des Aeroklub ČR (J. Zahradka) über das XCSoar-Repository<br>"
                        + "• Österreich, Frankreich: NOTAM über den NOTAM-Dienst von XCSoar (notam.xcsoar.org, Daten der FAA-NOTAM-API)<br>"
                        + "• Ersatz-Grenzen: deine eigenen, in XCSoar eingebundenen Luftraumdateien (nur gelesen)<br><br>"
                        + "<b>Haftungsausschluss:</b> Inoffizielles Hilfsmittel, ohne Gewähr. Nicht als primäres Navigationsmittel verwenden. Maßgebend sind ausschließlich AIP, DABS/AUP und NOTAM der zuständigen Stellen. XCSoar ist ein eigenständiges Projekt (GPL) und steht in keiner Verbindung zu dieser App.",
                "<b>DABS for XCSoar</b> – version %s (beta)<br>© 2026 Nadja Cu · Licence: MIT · <a href=\"https://github.com/fly-to-gecko/dabs-to-xcsoar\">github.com/fly-to-gecko/dabs-to-xcsoar</a><br><br>"
                        + "<b>Data sources</b> (loaded directly from there; the app contains no airspace data):<br>"
                        + "• Switzerland: DABS © Skyguide (<a href=\"https://www.skybriefing.com\">skybriefing.com</a>), area limits © SHV/FSVL (<a href=\"https://airspace.shv-fsvl.ch\">airspace.shv-fsvl.ch</a>)<br>"
                        + "• Germany: WebAUP © DFS Deutsche Flugsicherung<br>"
                        + "• Czechia: AUP/UUP © ŘLP ČR / ANS CR (<a href=\"https://aup.ans.cz\">aup.ans.cz</a>), area limits: airspace file of Aeroklub ČR (J. Zahradka) via the XCSoar repository<br>"
                        + "• Austria, France: NOTAM via XCSoar's NOTAM service (notam.xcsoar.org, FAA NOTAM API data)<br>"
                        + "• Substitute limits: your own airspace files configured in XCSoar (read only)<br><br>"
                        + "<b>Disclaimer:</b> Unofficial tool, no warranty. Do not use as primary means of navigation. Only the AIP, DABS/AUP and NOTAM of the competent authorities are authoritative. XCSoar is an independent project (GPL) and not affiliated with this app.",
                "<b>DABS pour XCSoar</b> – version %s (bêta)<br>© 2026 Nadja Cu · Licence : MIT · <a href=\"https://github.com/fly-to-gecko/dabs-to-xcsoar\">github.com/fly-to-gecko/dabs-to-xcsoar</a><br><br>"
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
