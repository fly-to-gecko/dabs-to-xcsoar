#!/usr/bin/env python3
"""DABS (Skyguide Daily Airspace Bulletin Switzerland) -> OpenAir für XCSoar.

Quellen:
  - DABS-PDF (tagesaktuelle Aktivierungen):  https://www.skybriefing.com/o/dabs?today
  - Geometrien der R-/D-Gebiete (SHV/FSVL):   https://airspace.shv-fsvl.ch/api/v2/native/airspaces

Für jede DABS-Zeile wird die echte Gebietsgeometrie aus den SHV-Daten genommen
(Zuordnung über den Code, z.B. LSR39B). Gibt es keine (z.B. temporäres
W-NOTAM-Gebiet), wird ein Kreis aus Mittelpunkt + Covering Radius gezeichnet.

Benutzung:
  python3 dabs2openair.py [--tomorrow] [-o datei.txt]
"""
import argparse
import json
import math
import re
import subprocess
import sys
import tempfile
import urllib.request
from datetime import date

DABS_URL = "https://www.skybriefing.com/o/dabs?{day}"
SHV_URL = "https://airspace.shv-fsvl.ch/api/v2/native/airspaces"
UA = "dabs2openair/0.1"


def fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read()


# ---------------------------------------------------------------- DABS parsen

ROW = re.compile(
    r"^\s*!?\s*(?P<id>[A-Z][A-Z0-9/]+)\s+"
    r"(?P<times>(?:\d{4}\s*-\s*\d{4}\s*)+?)\s+"
    r"(?P<lower>GND|FL\s*\d+|\d+m\s*/\s*\d+ft(?:\s*AGL)?)\s+"
    r"(?P<upper>UNL|FL\s*\d+|\d+m\s*/\s*\d+ft(?:\s*AGL)?)\s+"
    r"(?P<lat>\d{6}[NS])\s+(?P<lon>\d{7}[EW])\s+"
    r"(?P<rkm>[\d.]+)KM\s*/\s*(?P<rnm>[\d.]+)NM\s+"
    r"(?P<text>.*?)\s*!?\s*$"
)
NOTAM_LINE = re.compile(r"^\s*(W\d{4}/\d{2})\s*$")
DATE_RE = re.compile(r"DABS Date:\s*(\d{4} [A-Z]{3} \d{2})")
VERSION_RE = re.compile(r"Version\s+(\d+)\s*-\s*generated:\s*([\d.]+ [\d:]+ UTC)")


def pdf_to_text(pdf_bytes):
    with tempfile.NamedTemporaryFile(suffix=".pdf") as f:
        f.write(pdf_bytes)
        f.flush()
        return subprocess.run(["pdftotext", "-layout", f.name, "-"],
                              capture_output=True, text=True, check=True).stdout


def parse_dms(s):
    hemi = s[-1]
    s = s[:-1]
    deg_len = 2 if hemi in "NS" else 3
    d, m, sec = int(s[:deg_len]), int(s[deg_len:deg_len + 2]), int(s[deg_len + 2:])
    v = d + m / 60 + sec / 3600
    return -v if hemi in "SW" else v


def parse_dabs(text):
    if "not available yet" in text:
        raise SystemExit("DABS für dieses Datum ist noch nicht veröffentlicht (kommt ca. 16:00 LT am Vortag).")
    entries = []
    cur = None
    info_section = False
    for line in text.splitlines():
        if "Activities not shown" in line:
            info_section = True
        m = ROW.match(line)
        if m:
            cur = m.groupdict()
            cur["times"] = re.findall(r"(\d{4})\s*-\s*(\d{4})", cur["times"])
            cur["notam"] = None
            cur["lat"] = parse_dms(cur["lat"])
            cur["lon"] = parse_dms(cur["lon"])
            cur["rnm"] = float(cur["rnm"])
            cur["info"] = info_section
            entries.append(cur)
            continue
        if cur is None:
            continue
        n = NOTAM_LINE.match(line)
        if n and cur["notam"] is None:
            cur["notam"] = n.group(1)
            continue
        # Fortsetzungszeilen des NOTAM-Textes (weit eingerückt)
        stripped = line.strip().strip("!").strip()
        if stripped and line.startswith(" " * 60):
            cur["text"] += " " + stripped
        elif not stripped:
            pass
        else:
            cur = None  # neuer Abschnitt
    meta = {}
    if (m := DATE_RE.search(text)):
        meta["date"] = m.group(1)
    if (m := VERSION_RE.search(text)):
        meta["version"], meta["generated"] = m.groups()
    return meta, entries


# ------------------------------------------------------------ SHV-Geometrien

def norm_code(name):
    """'LS-R31 Emmen east' / 'LSR39B BRISEN' / 'LSD12 Sihltal' -> 'LSR31' ..."""
    m = re.match(r"\s*LS-?([RDP])\s*(\d+[A-Z]?)\b", name.upper())
    return f"LS{m.group(1)}{m.group(2)}" if m else None


def is_valid_today(asp, today):
    vals = asp.get("Validity")
    if not vals:
        return True
    for v in vals:
        f, t = v.get("ValidFrom"), v.get("ValidTo")
        if (f is None or f[:10] <= today) and (t is None or today <= t[:10]):
            return True
    return False


def load_shv_geometry(data, today):
    by_code = {}
    for asp in data["Airspaces"]:
        code = norm_code(asp["Name"])
        if code and is_valid_today(asp, today):
            by_code.setdefault(code, []).append(asp)
    return by_code


def dist_bearing(lat1, lon1, lat2, lon2):
    """Abstand (NM) und Peilung (rad) - lokal eben, reicht für kleine Bögen."""
    dy = (lat2 - lat1) * 60
    dx = (lon2 - lon1) * 60 * math.cos(math.radians((lat1 + lat2) / 2))
    return math.hypot(dx, dy), math.atan2(dx, dy)


def offset(lat, lon, dist_nm, brg):
    return (lat + dist_nm * math.cos(brg) / 60,
            lon + dist_nm * math.sin(brg) / 60 / math.cos(math.radians(lat)))


def shape_to_polygon(shape):
    """SHV-Shape -> Liste (lat, lon); Bögen werden in Punkte aufgelöst.
    Kreise werden als ('circle', lat, lon, r_nm) zurückgegeben."""
    if len(shape) == 1 and shape[0]["Type"] == "Circle":
        c = shape[0]["Coordinate"]
        return ("circle", c["Latitude"], c["Longitude"], shape[0]["Radius"])
    pts = []
    i = 0
    while i < len(shape):
        p = shape[i]
        c = p["Coordinate"]
        if p["Type"] == "Point":
            pts.append((c["Latitude"], c["Longitude"]))
        elif p["Type"] == "CoordArc":
            start = pts[-1] if pts else None
            nxt = shape[i + 1]["Coordinate"] if i + 1 < len(shape) else shape[0]["Coordinate"]
            end = (nxt["Latitude"], nxt["Longitude"])
            if start:
                cw = p.get("ArcDirection", "CW") == "CW"
                r, a0 = dist_bearing(c["Latitude"], c["Longitude"], *start)
                _, a1 = dist_bearing(c["Latitude"], c["Longitude"], *end)
                sweep = (a1 - a0) % (2 * math.pi) if cw else -((a0 - a1) % (2 * math.pi))
                steps = max(4, int(abs(sweep) / math.radians(5)))
                for k in range(1, steps):
                    pts.append(offset(c["Latitude"], c["Longitude"], r, a0 + sweep * k / steps))
        else:
            raise ValueError(f"unbekannter Shape-Typ {p['Type']}")
        i += 1
    return pts


# ----------------------------------------------------------------- OpenAir

def fmt_coord(lat, lon):
    def dms(v, w):
        v = abs(v)
        d = int(v)
        m = int((v - d) * 60)
        s = round(((v - d) * 60 - m) * 60)
        if s == 60:
            s, m = 0, m + 1
        if m == 60:
            m, d = 0, d + 1
        return f"{d:0{w}d}:{m:02d}:{s:02d}"
    return f"{dms(lat, 2)} {'N' if lat >= 0 else 'S'} {dms(lon, 3)} {'E' if lon >= 0 else 'W'}"


def fmt_alt(s):
    s = s.replace(" ", "")
    if s == "GND":
        return "GND"
    if s == "UNL":
        return "UNL"
    if s.startswith("FL"):
        return "FL" + s[2:]
    m = re.match(r"(\d+)m/(\d+)ft(AGL)?", s)
    if m:
        return f"{m.group(2)}ft {'AGL' if m.group(3) else 'MSL'}"
    return s


def openair_class(code, info=False):
    if info or code.startswith("LSD"):
        return "Q"  # Danger
    return "R"      # Restricted (auch temporäre W-NOTAM-Gebiete)


def build_openair(meta, entries, geo):
    out = [
        "* Schweizer DABS-Aktivierungen (Skyguide) fuer XCSoar",
        f"* DABS-Datum: {meta.get('date', '?')}  Version {meta.get('version', '?')}"
        f"  erzeugt {meta.get('generated', '?')}",
        "* Geometrie: SHV/FSVL airspace.shv-fsvl.ch bzw. Kreis aus DABS-Mittelpunkt/Radius",
        "* Zeiten in UTC. Ohne Gewaehr - massgebend sind DABS und NOTAM.",
        "",
    ]
    report = []
    for e in entries:
        if e["info"]:  # "Activities not shown on the DABS Chart Side" -> nicht zeichnen
            report.append(f"  {e['id']:8s} NUR INFO: {e['text'][:100]}")
            continue
        code = e["id"]
        times = " ".join(f"{a}-{b}" for a, b in e["times"])
        label_base = f"{code} {times}UTC"
        if e["notam"]:
            label_base += f" {e['notam']}"
        parts = geo.get(code, [])
        lo, hi = fmt_alt(e["lower"]), fmt_alt(e["upper"])
        if parts:
            for asp in parts:
                for seg in asp["Segments"]:
                    name = f"{asp['Name']} {times}UTC"
                    out += [f"AC {openair_class(code)}", f"AN {name}",
                            f"AL {lo}", f"AH {hi}"]
                    poly = shape_to_polygon(seg["Shape"])
                    if poly[0] == "circle":
                        _, la, lo_, r = poly
                        out += [f"V X={fmt_coord(la, lo_)}", f"DC {r:.2f}"]
                    else:
                        out += [f"DP {fmt_coord(la, lo_)}" for la, lo_ in poly]
                    out.append("")
            report.append(f"  {code:8s} {times:12s} {lo:>11s} - {hi:<11s} Polygon ({len(parts)} Teil(e))")
        else:
            out += [f"AC {openair_class(code, e['info'])}", f"AN {label_base}",
                    f"AL {lo}", f"AH {hi}",
                    f"V X={fmt_coord(e['lat'], e['lon'])}", f"DC {e['rnm']:.2f}", ""]
            report.append(f"  {code:8s} {times:12s} {lo:>11s} - {hi:<11s} Kreis {e['rnm']} NM")
    return "\n".join(out) + "\n", report


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--tomorrow", action="store_true", help="DABS von morgen statt heute")
    ap.add_argument("--pdf", help="lokales DABS-PDF statt Download")
    ap.add_argument("-o", "--output", default="dabs.txt")
    a = ap.parse_args()

    pdf = open(a.pdf, "rb").read() if a.pdf else fetch(DABS_URL.format(day="tomorrow" if a.tomorrow else "today"))
    meta, entries = parse_dabs(pdf_to_text(pdf))
    geo = load_shv_geometry(json.loads(fetch(SHV_URL)), date.today().isoformat())
    text, report = build_openair(meta, entries, geo)
    with open(a.output, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"DABS {meta.get('date')} V{meta.get('version')}: {len(entries)} Aktivierungen -> {a.output}")
    print("\n".join(report))


if __name__ == "__main__":
    sys.exit(main())
