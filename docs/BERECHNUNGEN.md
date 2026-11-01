# Berechnungen & Formeln

Die gesamte Rechenlogik liegt in `app/src/main/java/com/example/volumenstromrechner/calculation/` und ist durch Unit-Tests abgedeckt (`app/src/test/...`).

## 1. Volumenstrom (Luft)

Volumenstrom durch ein rundes Rohr:

```
Q [m³/h] = π · r² · v · 3600
r [m]    = Durchmesser [mm] / 2000
```

| Eingabe | Einheit | Werte |
|---|---|---|
| Rohrdurchmesser | mm | 125, 140, 150, 160, 180, 200, 224, 250, 280, 300 |
| Strömungsgeschwindigkeit v | m/s | > 0 |

Beispiel: 150 mm bei 5 m/s ergibt ≈ 318,1 m³/h.

Quelle: `ClimateCalculator.volumeFlow()`

## 2. VPD (Vapor Pressure Deficit)

Sättigungsdampfdruck nach **Tetens** (kPa):

```
SVP(T) = 0,61078 · e^(17,27 · T / (T + 237,3))
```

```
AVP      = SVP(T_Luft) · rF / 100          (tatsächlicher Dampfdruck)
VPD_Luft = SVP(T_Luft) − AVP
T_Blatt  = T_Luft + Blatt-Offset
VPD_Blatt = SVP(T_Blatt) − AVP
```

Bewertung (anhand des Blatt-VPD):

| Blatt-VPD (kPa) | Status | Farbe |
|---|---|---|
| < 0,4 | Niedrig – Schimmelgefahr | blau |
| 0,4 – 1,2 | Ideal | grün |
| 1,2 – 1,6 | Hoch – Stress | orange |
| > 1,6 | Sehr hoch – Welkgefahr | rot |

Gültige Eingaben: Temperatur −50 bis 60 °C (auch Blatttemperatur), Feuchte 0–100 %. Der Blatt-Offset ist optional (Standard 0).

Quelle: `ClimateCalculator.vpd()`

## 3. DLI (Daily Light Integral)

```
DLI [mol/m²/Tag] = PPFD [µmol/m²/s] · Stunden · 3600 / 1 000 000
                 = PPFD · Stunden · 0,0036
```

| DLI | Einstufung |
|---|---|
| < 10 | Niedrig (Schattenpflanzen / Bewurzelung) |
| 10 – 20 | Mittel (Wachstumsphase) |
| 20 – 40 | Hoch (Blüte / Fruchtbildung) |
| > 40 | Sehr hoch (CO₂-Einsatz sinnvoll) |

Quelle: `ClimateCalculator.dli()`, `dliLevel()`

## 4. Dosierrechner (Dünger)

### Nährstoffgehalt einer Komponente

Herstellerangaben sind Gewichtsprozente. Flüssige Produkte (ml) werden über ihre Dichte in Gramm umgerechnet:

```
mg/L = Dosis [g oder ml pro L] · Dichte [g/ml] · 10 · Anteil [%]
```

(1 g/L bei 1 % = 10 mg/L. Für Pulver ist die Dichte 1,0.)

### Modi

- **Hersteller-Modus (Original):** Dosierung exakt laut Tabelle.
- **Ca:Mg-Verhältnis (2,0 / 2,5 / 3,0 / 3,5 : 1):** nur für Programme mit einem dosierbaren Calcium-Produkt.
  1. Mg aus Wasser + Dünger wird summiert. Liegt es unter **20 mg/L**, wird **Bittersalz** ergänzt (98 mg Mg pro Gramm).
  2. Ziel-Ca = Gesamt-Mg · Zielverhältnis
  3. Zusatz-Ca = max(0, Ziel-Ca − Ca aus Wasser − Ca aus übrigen Düngern)
  4. Die Calcium-Dosis wird so gewählt, dass genau dieser Zusatz erreicht wird.

### Ausgabe

- Gesamtmengen pro Komponente für das gewählte Mischvolumen
- Geschätzter Gesamt-EC = Ausgangs-EC des Wassers + Ziel-EC laut Schema
- Endlösung in mg/L: N, P₂O₅, K₂O, Ca, Mg sowie das resultierende Ca:Mg-Verhältnis
  - < 2,5 : 1 → magnesiumlastig
  - 2,5 – 4,0 : 1 → ausgewogen
  - > 4,0 : 1 → calciumlastig

Quelle: `NutrientCalculator.calculate()`, Schemata in `model/NutrientData.kt`

> **Hinweis:** Die Dichten der Flüssigdünger sind noch nicht hinterlegt (Standardwert 1,0). Siehe TODO in `NutrientData.kt`.
