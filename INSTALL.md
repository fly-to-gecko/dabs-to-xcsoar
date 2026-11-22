# Installation auf einem Android-Handy

*English version: [INSTALL.en.md](INSTALL.en.md)*

Die App gibt es nicht im Google Play Store, sondern als Installationsdatei (**APK**) hier auf GitHub.
Die Installation dauert etwa 5 Minuten.

## Voraussetzungen

- **Android 8 oder neuer**: Einstellungen → Telefoninfo → Softwareinformationen → „Android-Version“
- **XCSoar** (oder OpenSoar) ist installiert und wurde **mindestens einmal gestartet**, damit sein
  Datenordner existiert
- Internet beim Laden der Daten

## 1. APK herunterladen

1. Auf dem Handy im Browser (z. B. Chrome oder Samsung Internet) öffnen:
   **https://github.com/fly-to-gecko/dabs-to-xcsoar/releases**
2. Beim obersten (neuesten) Release unter **Assets** auf **`dabs-xcsoar-<version>.apk`** tippen.
3. Falls der Browser warnt „Diese Art von Datei kann schädlich sein“: **Trotzdem herunterladen**.

> Tipp: Die Datei landet im Ordner **Downloads**.

## 2. Installation erlauben

Android installiert Apps außerhalb des Play Stores erst, wenn du es der App erlaubst, mit der du die
APK öffnest (Browser oder „Eigene Dateien“). Das fragt Android beim ersten Mal von selbst:

1. Die heruntergeladene APK antippen (Benachrichtigung „Download abgeschlossen“ oder Downloads-Ordner).
2. Meldung **„Aus Sicherheitsgründen ist die Installation unbekannter Apps … nicht zulässig“** →
   **Einstellungen** → Schalter **„Von dieser Quelle zulassen“** (bzw. „Unbekannte Apps installieren“)
   einschalten → zurück.
3. **Installieren** tippen.

Wo die Einstellung von Hand zu finden ist, falls keine Nachfrage kommt:

| Hersteller | Pfad |
|---|---|
| Samsung | Einstellungen → Apps → ⋮ (oben rechts) → Spezieller Zugriff → Unbekannte Apps installieren → *Browser/Eigene Dateien* → erlauben |
| Google Pixel / Android pur | Einstellungen → Apps → Spezieller App-Zugriff → Unbekannte Apps installieren → *Browser* → erlauben |
| Xiaomi / Redmi / POCO | Einstellungen → Datenschutz → Spezielle Berechtigungen → Unbekannte Apps installieren → *Browser* → erlauben |
| Huawei / Honor | Einstellungen → Sicherheit → Weitere Einstellungen → Unbekannte Apps installieren → *Browser* → erlauben |

### Google Play Protect

Möglicherweise meldet **Play Protect** „Unbekannte App“ oder „Von Play Protect blockiert“.
Das liegt nur daran, dass die App nicht aus dem Play Store kommt. Tippe auf **Details** bzw. **Mehr** →
**Trotzdem installieren**. Wird angeboten, die App „zur Prüfung zu senden“, kannst du das ablehnen.

## 3. Erster Start

1. **DABS für XCSoar** öffnen. Das Icon zeigt ein rot schraffiertes Sperrgebiet mit Segelflugzeug.
2. Die App meldet **„Dateizugriff fehlt“** → **Dateizugriff erlauben** → Schalter
   **„Zugriff auf alle Dateien erlauben“** einschalten → zurück.
   *Warum?* Die App schreibt ihre Dateien in den Ordner einer anderen App (XCSoar). Ohne diese
   Berechtigung lässt Android das nicht zu. Gelesen werden nur die XCSoar-Ordner.
   *(Android 8–10: stattdessen die Frage „Zugriff auf Dateien erlauben?“ mit **Zulassen** beantworten.)*
3. Länder anhaken, die du brauchst (Schweiz, Deutschland, Tschechien, Österreich, Frankreich).
4. Den großen Knopf **„Alle neu laden“** unten tippen. Jedes Land bekommt eine Ampel:
   grün ✓ = geladen, rot ✕ = Fehler (antippen für Details).
5. Unter **⚙ → XCSoar-Versionen** prüfen, dass deine XCSoar-Installation angehakt ist.

Optional: Das App-Icon aus der App-Liste auf den Startbildschirm ziehen.

## 4. XCSoar einrichten (einmalig)

1. In der DABS-App zuerst einmal laden (siehe oben), damit die Dateien existieren.
2. In XCSoar: **Menü → Konfiguration → System → Standort-Dateien → Luftraum**.
3. Zusätzlich zu deiner normalen Luftraumdatei auswählen:
   - `dabs.txt` (Schweiz)
   - `mil-de.txt`, `mil-cz.txt`, `mil-at.txt`, `mil-fr.txt` (je nach Land)

   XCSoar 7.45 und neuer erlaubt mehrere Luftraumdateien gleichzeitig. Ältere Versionen haben nur ein
   zusätzliches Feld „Weitere Luftraumdatei“.
4. **Empfohlen:** in den **Luftraum-Einstellungen** von XCSoar unter **Farben** der Klasse **TFR** z. B.
   **violett** geben (Rand und Füllung). Alle Gebiete aus der App haben die Klasse TFR und heben sich so von der
   normalen Luftraumdatei ab. Die offizielle Kategorie steht im Namen: [R], [D], [P], [TRA] …
5. XCSoar neu starten.

> **Wichtig:** Die normale Luftraumdatei (z. B. `CH-ASP-DABS-SegelflugCH.txt`, `DE-ASP-National-DAEC.txt`)
> muss eingebunden bleiben, denn sie enthält CTR, TMA usw. Die App-Dateien enthalten **nur** die heute
> aktiven Militär- und Sperrgebiete.

## 5. Im Alltag

1. Vor dem Flug **DABS für XCSoar** öffnen. Die App lädt **nicht automatisch**.
2. **„Alle neu laden“** tippen. Sind alle Länder aktuell, steht unten
   „Letztes Daten-Update: heute HH:MM“.
3. **Danach XCSoar starten.** XCSoar liest Luftraumdateien nur beim Start ein.

Eine **orange Ampel** („veraltet“) bedeutet, dass der Stand nicht von heute ist. Dann neu laden.

## Update auf eine neue Version

Neue APK wie in Schritt 1 herunterladen und öffnen → **Installieren** bzw. **Aktualisieren**.
Einstellungen und Daten bleiben erhalten, eine Deinstallation ist nicht nötig.

## Deinstallieren

App-Icon gedrückt halten → **Deinstallieren**. Die Dateien `dabs.txt` und `mil-xx.txt` im XCSoar-Ordner
bleiben liegen. Entferne sie in XCSoar aus der Luftraum-Auswahl, sonst zeigt XCSoar die alten Gebiete
weiter an.

## Probleme

| Problem | Lösung |
|---|---|
| „App nicht installiert“ / „Paket steht in Konflikt“ | Eine anders signierte Version ist installiert. Alte Version deinstallieren und neu installieren. |
| Installation wird blockiert | Schritt 2 prüfen, Play Protect: „Trotzdem installieren“. |
| „Keine XCSoar-Installation gefunden“ | XCSoar einmal starten und schließen, dann erneut laden. Unter ⚙ → XCSoar-Versionen prüfen. |
| Rote Ampel | Land antippen, der Grund steht in den Details. Meist fehlt Internet. Der letzte Stand bleibt erhalten. |
| Gebiete erscheinen nicht in XCSoar | Dateien in XCSoar ausgewählt (Schritt 4)? XCSoar nach dem Laden neu gestartet? |
| XCSoar meldet „Unbekannter Luftraumdateityp“ | Die Datei wurde nie geladen. In der App das Land anhaken und laden. |

## Prüfsumme (optional)

Jede APK ist mit demselben Zertifikat signiert. Dessen SHA-256-Fingerabdruck steht in den
Release-Notizen:

```
02be778643b1a8f1926acfa279aff277c84b7fb386982e0e735dacdf00d228f3
```

---

⚠️ Beta-Version, inoffiziell, ohne Gewähr. Maßgebend sind ausschließlich AIP, DABS/AUP und NOTAM.
