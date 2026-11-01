# Änderungsprotokoll

Format angelehnt an [Keep a Changelog](https://keepachangelog.com/de/1.1.0/).

## [Unveröffentlicht]

### Hinzugefügt
- Englische Oberfläche: Die App erscheint auf Englisch, wenn die Gerätesprache nicht Deutsch ist; auf deutschen Geräten bleibt alles auf Deutsch
- Sprache pro App wählbar (Android 13+: Einstellungen → Apps → GrowCalc → Sprache)

### Geändert
- Zahlen (Dezimal- und Tausendertrennzeichen) richten sich nach der Sprache, z. B. „1.47 L“ bzw. „1,47 L“
- Dünger-Wochen, Herstellerhinweise und „Bittersalz“ sind jetzt übersetzbar

## [0.4.0-beta] – 2026-11-01

### Hinzugefügt
- Bewässerungsrechner: Menge pro Topf aus Tropfer-Durchfluss (Farbcode-Presets 1,1–8,0 L/h oder eigener Wert), Tropfern pro Topf und Laufzeit
- Mehrere Bewässerungen pro Tag mit Uhrzeit und Laufzeit in Sekunden oder Minuten, Tagesmenge für alle Töpfe und Tank-Reichweite
- Umkehrmodus: Laufzeit für eine Zielmenge berechnen und als Bewässerung übernehmen
- Durchfluss auslitern mit dem Messbecher sowie gespeicherte Setups; die letzten Eingaben bleiben erhalten

## [0.3.0-beta] – 2026-11-01

### Geändert
- PPFD-Rechner neu gestaltet als Messgerät: großer Live-Wert, Lichtquelle per Wischen oder Antippen, runder Halten/Messen-Button, Erklärung im Info-Dialog
- Messung startet beim Öffnen automatisch; niedrige Werte mit einer Nachkommastelle

### Entfernt
- Manuelle Eingabe, Richtungstausch PPFD → Lux und Tabelle der typischen Umrechnungen

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
