# DABS für XCSoar

Tagesaktuelle Militär-/Sperrgebiete für XCSoar: Schweiz, Deutschland, Tschechien, Österreich, Frankreich.

> **Beta-Version** – funktioniert, wird aber noch getestet. Rückmeldungen gern als Issue.

Android-App, die die **tagesaktuellen Militär-Lufträume der Schweiz** (Skyguide *DABS – Daily Airspace
Bulletin Switzerland*) als OpenAir-Datei `dabs.txt` in den Datenordner von **XCSoar** schreibt.
So zeigt XCSoar nur die Gebiete an, die heute wirklich aktiv sind – mit ihrer echten Form, Höhe und
Aktivierungszeit.

<img src="docs/screenshot.png" width="300" alt="Screenshot der App">

## So funktioniert's

Beim **Öffnen der App** (oder mit „Jetzt aktualisieren“) lädt sie für jedes angehakte Land die heute
aktiven Gebiete und schreibt je Land eine OpenAir-Datei in **alle** gefundenen XCSoar-Installationen
(XCSoar 7.45+: `Android/media/<paket>/airspace/`, ältere Versionen und OpenSoar: Datenordner).

| Land | Datei | Aktivierungen aus | Umrisse aus |
|---|---|---|---|
| 🇨🇭 Schweiz | `dabs.txt` | Skyguide DABS ([PDF](https://www.skybriefing.com/o/dabs?today)) | SHV/FSVL ([API](https://airspace.shv-fsvl.ch)) bzw. Kreis aus dem DABS |
| 🇩🇪 Deutschland | `mil-de.txt` | DFS [WebAUP](https://secais.dfs.de/pilotservice/briefing/aup/aup_edit.jsp) | DFS (kommen mit) |
| 🇨🇿 Tschechien | `mil-cz.txt` | ANS CR [AUP/UUP](https://aup.ans.cz/) (UUP-Stornos werden berücksichtigt) | Luftraumdatei des Aeroklub ČR (XCSoar-Repository, frei nutzbar) |
| 🇦🇹 Österreich | `mil-at.txt` | NOTAM über [notam.xcsoar.org](https://github.com/yorickreum/xcsoar-notam-proxy) | Koordinaten aus dem NOTAM, sonst Kreis aus der Q-Zeile |
| 🇫🇷 Frankreich | `mil-fr.txt` | NOTAM über notam.xcsoar.org | wie Österreich |

Die Gebietsnamen enthalten Datum und Zeiten, z. B. `LSR7 Hongrin 17.11. 0630-2100UTC` –
so sieht man in XCSoar sofort, wenn die Daten nicht von heute sind. Mit dem Knopf
**„Original-DABS anzeigen (PDF)“** öffnet die App das zugrunde liegende Skyguide-PDF.

Hinweise:
- **Schweiz:** Einträge, die Skyguide nicht in die DABS-Karte zeichnet („Activities not shown on the
  DABS Chart Side“, z. B. MIL-Nachtflüge über die ganze Schweiz), erscheinen nur als Hinweis in der App.
- **Deutschland:** Die DFS WebAUP zeigt laut DFS nur Aktivierungen bis FL100 (Alpen FL130) und ist nicht vollständig.
- **Österreich/Frankreich:** Nur NOTAMs, die eine Aktivität bedeuten (Q-Code …CA/CS/LW/LP).
  Sehr große Kreise (> 50 NM) nur als Hinweis.
- **Herkunft der Grenzen (alle Länder):** Vorrang hat immer die offizielle Quelle (SHV-Daten zum DABS,
  DFS, Aeroklub ČR, Koordinaten im NOTAM). **Nur wenn diese keine Grenzen liefert**, sucht die App das
  Gebiet in den Luftraumdateien, die im **aktiven XCSoar-Profil eingebunden** sind (laut `xcsoar.log`;
  andere Dateien im Ordner werden nicht benutzt, da sie veraltet sein können). Nennt ein NOTAM z. B.
  „LF-R368 A B C1“ oder „LF-D596 A/B/C“, werden die Teilgebiete einzeln gesucht. Solche Gebiete sind im
  Namen gekennzeichnet: `… [R] Grenzen aus FR-ASP-National-OpenAIP.txt`. Erst wenn auch dort nichts
  steht, wird der **ungefähre** Kreis gezeichnet; fehlt nur ein Teil, bekommt dieser zusätzlich den Kreis.
- Gibt es heute keine Aktivierung, enthält die Datei einen winzigen Platzhalter bei 0°N 0°E,
  weil XCSoar leere OpenAir-Dateien ablehnt.
- Abgewählte Länder: die App löscht deren Datei, damit keine veralteten Gebiete angezeigt werden.

Die App läuft **nicht im Hintergrund**. Geladen wird automatisch nur beim **Start der App** und an
einem **neuen Tag** – nicht beim Entsperren oder Zurückwechseln (dafür gibt es den großen Knopf).
Das schont die Server der Flugsicherungen. Selten geänderte Daten (SHV-Gebietsgrenzen, tschechische
Geometriedatei) werden höchstens einmal täglich geladen und zwischengespeichert; die tagesaktuellen
Daten (DABS, AUP, NOTAM) werden jedes Mal frisch geholt.

**Sprachen:** Deutsch, Englisch, Französisch – standardmäßig die Systemsprache, umstellbar unter
⚙ → Sprache. Auch der Hinweis „Grenzen aus …“ in den Gebietsnamen folgt der Sprache.

## Bedienung

- **Startseite:** je Land ein Häkchen (an/aus), eine **Ampel** und ein Knopf zur **Originalquelle**
  (Schweiz: genau das ausgewertete DABS-PDF). Ampel: drehend = lädt, grün ✓ = fertig (Anzahl Gebiete,
  Uhrzeit), rot ✕ = Fehler, orange ! = abgebrochen. Die Länder laden gleichzeitig.
- **Antippen eines Landes:** Details – Stand, Gebietsliste mit Zeiten und Höhen, Fehlermeldung,
  Herkunft der Grenzen, „Neu laden“.
- **Großer Knopf unten** „Alle neu laden“ (auch im Flug gut zu treffen); während des Ladens zeigt er
  den Fortschritt („3 von 5 fertig“).
- **⚙ Einstellungen** mit Unterseiten: So funktioniert's · XCSoar-Versionen (welche Installationen
  beschrieben werden, mit erkannter Version) · Dateizugriff · Sprache · Copyright & Quellen.

## Installation

1. Die neueste `dabs-xcsoar-<version>.apk` unter [Releases](../../releases) aufs Handy laden.
2. Öffnen und die Installation aus unbekannten Quellen erlauben.
3. App starten → **„Dateizugriff erlauben“** → *Zugriff auf alle Dateien* einschalten.
   (Nötig, weil die App in den Ordner einer anderen App – XCSoar – schreibt.)
4. Zurück in der App lädt sie sofort die Daten.

Voraussetzung: Android 8 oder neuer, XCSoar mindestens einmal gestartet.

## Einrichtung in XCSoar (einmalig)

**Konfiguration → System → Standort-Dateien → Luftraum:** `dabs.txt` bzw. `mil-xx.txt` **zusätzlich** zu deiner
normalen Luftraumdatei auswählen (XCSoar 7.45 kann mehrere Dateien gleichzeitig laden;
ältere Versionen haben dafür das Feld „Weitere Luftraumdatei“).

Wichtig: Die normale Luftraumdatei sollte **keine** DABS-Gebiete enthalten (z. B.
`CH-ASP-DABS-SegelflugCH.txt` oder eine Variante „ohne DABS“), sonst erscheinen alle
R-/D-Gebiete immer – egal ob aktiv.

### Eigene Farbe für die App-Gebiete (empfohlen)

Alle Gebiete aus der App haben die OpenAir-Klasse **TFR** (temporäre Flugbeschränkung). Diese Klasse
kommt in den üblichen Luftraumdateien nicht vor. Gib ihr in XCSoar einmalig eine eigene Farbe:

**Konfiguration → System → Luftraum → Farben → TFR** → z. B. Violett für Rand und Füllung.

Dann gilt: rot = Gebiet aus der Standard-Luftraumdatei, violett = heute aktiv laut DABS/AUP/NOTAM.
Die **offizielle Kategorie** steht im Namen: `[R]` Sperrgebiet, `[D]` Gefahrengebiet, `[P]` Verbotszone,
`[TRA]`, `[TSA]`, `[MTA]`, `[Warnung]`. Luftraumwarnungen sind für TFR in XCSoar standardmäßig an.
Zusätzlich enthalten die Dateien `SP`/`SB` (violett) für SeeYou & Co.; XCSoar wertet diese nicht aus.

(Von Hand im Profil, XCSoar vorher beenden: `AirspaceBorderColor42="#8000FF"` und `AirspaceFillColor42="#8000FF"`.)

**Vor dem Flug:** erst die DABS-App öffnen, bis „OK“ erscheint, dann XCSoar starten.
XCSoar liest Luftraumdateien nur beim Start (oder nach Änderungen in der Konfiguration) ein.

Das DABS für den Folgetag erscheint ab ca. 16:00 Uhr; die App lädt immer das DABS des aktuellen Tages.

## ⚠️ Haftungsausschluss

Inoffizielles Hilfsmittel, **ohne Gewähr**. Nicht als primäres Navigationsmittel verwenden.
Maßgebend sind ausschließlich das offizielle DABS und die NOTAM von Skyguide.
Das PDF-Format des DABS kann sich jederzeit ändern – dann findet die App eventuell keine oder
falsche Einträge. Prüfe die Anzeige in der App gegen das DABS.

Datenquellen: Skyguide (DABS), SHV/FSVL (Luftraumgeometrien). Die App lädt die Daten direkt von
deren Servern; dieses Repository enthält keine Luftraumdaten.

## Selbst bauen

Ohne Android Studio / Gradle, nur mit JDK 17 und dem Android-SDK (Platform 34, Build-Tools 34.0.0):

```bash
export ANDROID_HOME=~/Android      # SDK-Ordner
export JAVA_HOME=~/Android/jdk     # JDK 17
app/build.sh                       # -> app/build/dabs-xcsoar-<version>.apk
```

Ohne eigenen Release-Schlüssel signiert `build.sh` automatisch mit einem lokalen Debug-Schlüssel.
Für einen Release-Schlüssel eine Datei `~/.android/dabs-release.properties` anlegen:

```
KEYSTORE=/pfad/zum/release.keystore
KEY_ALIAS=dabs
KEYSTORE_PASS=...
```

### Am PC testen

Die Umwandlung (`app/src/dabs/core`) ist reines Java und läuft auch am PC:

```bash
mkdir -p /tmp/dabs && javac -d /tmp/dabs $(find app/src/dabs/core app/cli -name '*.java')
java -cp /tmp/dabs dabs.cli.Main ALL .                  # alle Länder in den aktuellen Ordner
java -cp /tmp/dabs dabs.cli.Main DE .                   # nur ein Land (CH, DE, CZ, AT, FR)
java -cp /tmp/dabs dabs.cli.Main --pdf dabs.pdf         # nur PDF-Parser testen
```

`tools/dabs2openair.py` ist der ursprüngliche Python-Prototyp (benötigt `pdftotext`).

## Projektstruktur

```
app/
  AndroidManifest.xml
  build.sh                 Build ohne Gradle (javac → d8 → aapt2 → apksigner)
  res/                     Icon
  src/dabs/core/           Länder-Quellen (DabsPdf, Germany, Czechia, Notams), OpenAir (plattformunabhängig)
  src/dabs/xcsoar/         Android-Oberfläche, XCSoar-Ordner finden
  cli/                     Kommandozeile zum Testen am PC
tools/dabs2openair.py      Python-Prototyp
```

## Lizenz

[MIT](LICENSE)
