# Änderungsprotokoll

Format angelehnt an [Keep a Changelog](https://keepachangelog.com/de/1.1.0/).

## [0.2.0-beta] – 2026-11-01

### Hinzugefügt
- PPFD-Rechner: Umrechnung Lux ↔ PPFD (µmol/m²/s) für Sonne/Tageslicht, LED Vollspektrum, HPS und Leuchtstofflampe
- Live-Messung mit dem Lichtsensor des Handys: geglätteter Messwert, „Wert halten“, Warnung bei übersteuertem Sensor
- Tabelle mit typischen Umrechnungen und Erklärung zu Lux, PPFD und Messgenauigkeit
- Einstellungen mit Kalibrierfaktor für den Lichtsensor

## [0.1.0-beta] – 2026-11-01

Erste öffentliche Beta.

### Hinzugefügt
- Volumenstromrechner (Luft)
- VPD-Rechner mit Bewertung
- DLI-Rechner mit Stunden-Schnellwahl
- Dosierrechner mit Schemata von Green House Feeding, Athena und Advanced Nutrients sowie Ca:Mg-Korrektur
- Neues Material-3-Design: eigenes Farbschema, Dark Mode, eigene Icons, Dashboard mit Kacheln, farbcodierte Ergebnisse
- Projektdokumentation (README, Formeln, Architektur)
- Signierte Release-APK als Download auf GitHub

### Geändert
- Paketname / Application-ID: `io.github.thedude1996.growcalc` (vorher `com.example.volumenstromrechner`)
- Theme heißt jetzt `Theme.GrowCalc`, Gradle-Projekt `GrowCalc`
