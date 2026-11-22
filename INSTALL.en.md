# Installing on an Android phone

*Deutsche Version: [INSTALL.md](INSTALL.md)*

The app is not in the Google Play Store; it is provided as an installation file (**APK**) here on GitHub.
Installation takes about 5 minutes.

## Requirements

- **Android 8 or newer** (Settings → About phone → Software information → "Android version")
- **XCSoar** (or OpenSoar) installed and **started at least once**, so that its data folder exists
- Internet access when loading data

## 1. Download the APK

1. On the phone, open in the browser (e.g. Chrome or Samsung Internet):
   **https://github.com/fly-to-gecko/dabs-to-xcsoar/releases**
2. In the top (latest) release, under **Assets**, tap **`dabs-xcsoar-<version>.apk`**.
3. If the browser warns "This type of file can harm your device": **Download anyway**.

## 2. Allow the installation

Android only installs apps from outside the Play Store if you allow it for the app you open the APK with
(browser or file manager). Android asks you the first time:

1. Tap the downloaded APK (notification "Download complete" or Downloads folder).
2. Message **"For your security, your phone is not allowed to install unknown apps from this source"** →
   **Settings** → turn on **"Allow from this source"** ("Install unknown apps") → back.
3. Tap **Install**.

Where to find the setting manually:

| Manufacturer | Path |
|---|---|
| Samsung | Settings → Apps → ⋮ (top right) → Special access → Install unknown apps → *browser/My Files* → allow |
| Google Pixel / stock Android | Settings → Apps → Special app access → Install unknown apps → *browser* → allow |
| Xiaomi / Redmi / POCO | Settings → Privacy protection → Special permissions → Install unknown apps → *browser* → allow |
| Huawei / Honor | Settings → Security → More settings → Install unknown apps → *browser* → allow |

### Google Play Protect

Play Protect may show "Unknown app" or "Blocked by Play Protect". This only means the app does not come
from the Play Store. Tap **Details** / **More** → **Install anyway**. You may decline sending the app for
a scan.

## 3. First start

1. Open **DABS for XCSoar** (red hatched restricted area with a glider).
2. The app shows **"File access missing"** → **Allow file access** → turn on
   **"Allow access to manage all files"** → back.
   *Why?* The app writes its files into the folder of another app (XCSoar); Android only allows this with
   this permission. Only the XCSoar folders are read.
   *(Android 8–10: answer "Allow access to files?" with **Allow** instead.)*
3. Tick the countries you need.
4. Tap the big **"Reload all"** button. Each country gets a status light:
   green ✓ = loaded, red ✕ = error (tap for details).
5. Check under **⚙ → XCSoar versions** that your XCSoar installation is ticked.

Optional: drag the app icon from the app drawer onto the home screen.

## 4. Set up XCSoar (once)

1. Load once in the DABS app (see above) so the files exist.
2. In XCSoar: **Menu → Config → System → Site files → Airspace**.
3. Select **in addition** to your normal airspace file:
   - `dabs.txt` (Switzerland)
   - `mil-de.txt`, `mil-cz.txt`, `mil-at.txt`, `mil-fr.txt` (per country)

   XCSoar 7.45+ allows several airspace files. Older versions only have one "Additional airspace file".
4. **Recommended:** in XCSoar's **airspace settings** under **Colours** give class **TFR** e.g.
   **violet** (border and fill). All areas from the app use class TFR and stand out from your normal airspace file.
   The official category is in the name: [R], [D], [P], [TRA] …
5. Restart XCSoar.

> **Important:** Keep your normal airspace file (CTR, TMA etc.). The app files contain **only** today's
> active military and restricted areas.

## 5. Daily use

1. Before the flight open **DABS for XCSoar**. It does **not** load automatically.
2. Tap **"Reload all"**. When all countries are current, the bottom shows "Last data update: today HH:MM".
3. **Then start XCSoar.** XCSoar only reads airspace files at startup.

An **orange light** ("outdated") means the data is not from today. Reload.

## Updating

Download the new APK as in step 1 and open it → **Install/Update**. Settings and data are kept, no need
to uninstall.

## Uninstalling

Long-press the app icon → **Uninstall**. The files `dabs.txt`/`mil-xx.txt` stay in the XCSoar folder.
Remove them from XCSoar's airspace selection, otherwise XCSoar keeps showing the old areas.

## Troubleshooting

| Problem | Solution |
|---|---|
| "App not installed" / "package conflicts" | A differently signed version is installed. Uninstall it and install again. |
| Installation blocked | Check step 2; Play Protect: "Install anyway". |
| "No XCSoar installation found" | Start and close XCSoar once, then reload. Check ⚙ → XCSoar versions. |
| Red light | Tap the country, the reason is in the details. Usually no internet. The last data is kept. |
| Areas don't appear in XCSoar | Files selected in XCSoar (step 4)? XCSoar restarted after loading? |
| XCSoar: "Unknown airspace file type" | The file was never loaded. Tick the country in the app and load. |

## Checksum (optional)

All APKs are signed with the same certificate (SHA-256 fingerprint, also in the release notes):

```
02be778643b1a8f1926acfa279aff277c84b7fb386982e0e735dacdf00d228f3
```

---

⚠️ Beta, unofficial, no warranty. Only the AIP, DABS/AUP and NOTAM are authoritative.
