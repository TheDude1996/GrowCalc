# GrowCalc 🌱

🌐 **Projektseite:** https://thedude1996.github.io/GrowCalc/ · [English](https://thedude1996.github.io/GrowCalc/en/)

**GrowCalc** ist eine Android-App mit Rechnern für den Indoor-Anbau: Lüftung, Klima, Licht, Düngung und Bewässerung – übersichtlich, offline und ohne Werbung.

<p align="center">
  <img src="docs/screenshots/dashboard.png" width="260" alt="Dashboard" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/vpd.png" width="260" alt="VPD-Rechner mit Ergebnis" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/ppfd.png" width="260" alt="PPFD-Lichtmesser" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/bewaesserung_ergebnis.png" width="260" alt="Bewässerungsrechner mit Ergebnis" />
</p>

## Download

👉 **[Neueste Version herunterladen](https://github.com/TheDude1996/GrowCalc/releases/latest)** (Datei `GrowCalc-<version>.apk`)

So installierst du die APK:

1. APK auf dem Android-Gerät herunterladen (Android 7.0 oder neuer).
2. Datei antippen. Beim ersten Mal fragt Android, ob der Browser/Dateimanager **„Apps aus unbekannten Quellen installieren“** darf – erlauben.
3. Installieren und GrowCalc öffnen.

> ⚠️ **Beta:** Die App ist noch in Entwicklung. Bitte Fehler über [Issues](https://github.com/TheDude1996/GrowCalc/issues) melden.

## Funktionen

| Werkzeug | Was es berechnet |
|---|---|
| **Volumenstromrechner** | Luftleistung in m³/h aus Rohrdurchmesser (125–300 mm) und Strömungsgeschwindigkeit |
| **VPD-Rechner** | Dampfdruckdefizit (Luft und Blatt) aus Lufttemperatur, rel. Feuchte und Blatt-Offset, mit farbiger Bewertung |
| **DLI-Rechner** | Daily Light Integral aus PPFD und Beleuchtungsdauer, mit Schnellwahl (12/16/18/20/24 h) |
| **Dosierrechner (Dünger)** | Nährstoffmengen nach Herstellerschema (Green House Feeding, Athena, Advanced Nutrients) inkl. Ca:Mg-Korrektur anhand der Wasseranalyse |
| **PPFD-Lichtmesser** | Live-Messung mit dem Lichtsensor des Handys, Umrechnung Lux → PPFD (µmol/m²/s) für Sonne, LED, HPS und Leuchtstoff, mit Kalibrierfaktor |
| **Bewässerungsrechner** | Wassermenge pro Topf aus Tropfer-Durchfluss, Tropfern und Laufzeit; mehrere Bewässerungen pro Tag, Tagesmenge, Tank-Reichweite und Laufzeit für eine Zielmenge |

Weitere Merkmale:

- Material-3-Design mit eigenem Farbschema und **Dark Mode**
- Eingaben mit Punkt *oder* Komma
- Farbcodierte Ergebnisse (ideal / niedrig / hoch / kritisch)
- Komplett offline, keine Berechtigungen nötig

Die verwendeten Formeln sind in [docs/BERECHNUNGEN.md](docs/BERECHNUNGEN.md) beschrieben.

## Voraussetzungen

- Android Studio (aktuelle Version) mit JDK 17+ (das mitgelieferte JBR reicht)
- Android SDK 36
- Gerät oder Emulator mit **Android 7.0 (API 24)** oder neuer

## Bauen & Starten

```bash
# Debug-APK bauen
./gradlew assembleDebug          # Windows: gradlew.bat assembleDebug

# Auf verbundenem Gerät/Emulator installieren
./gradlew installDebug

# Unit-Tests ausführen
./gradlew testDebugUnitTest
```

Die APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.

Alternativ: Projekt in Android Studio öffnen und auf **Run ▶** klicken.

## Projektstruktur

```
app/src/main/
├── java/io/github/thedude1996/growcalc/
│   ├── MainActivity.kt          # Navigation (Drawer + Fragmente)
│   ├── calculation/             # Reine Rechenlogik (ohne Android-Abhängigkeiten, unit-getestet)
│   ├── fragments/               # Ein Fragment pro Rechner + Dashboard
│   ├── model/                   # Datenmodelle & Düngeschemata der Hersteller
│   ├── viewmodel/               # NutrientViewModel (Zustand des Dosierrechners)
│   └── ui/                      # UI-Hilfen (farbige Status-Labels)
└── res/                         # Layouts, Farben (inkl. values-night), Icons, Strings
```

Mehr Details: [docs/ARCHITEKTUR.md](docs/ARCHITEKTUR.md)

## Dokumentation

- [Berechnungen & Formeln](docs/BERECHNUNGEN.md)
- [Architektur](docs/ARCHITEKTUR.md)
- [Mitwirken](CONTRIBUTING.md)
- [Änderungsprotokoll](CHANGELOG.md)

## Lizenz

GrowCalc ist freie Software unter der [GNU General Public License v3.0](LICENSE). Du darfst die App nutzen, verändern und weitergeben – abgeleitete Werke müssen ebenfalls unter der GPL-3.0 veröffentlicht werden.

## Haftungsausschluss

Die Ergebnisse sind Richtwerte. Düngeschemata stammen aus Herstellerangaben und können sich ändern – im Zweifel gilt immer die aktuelle Angabe des Herstellers. Die genannten Marken gehören ihren jeweiligen Inhabern; dieses Projekt steht in keiner Verbindung zu ihnen.
