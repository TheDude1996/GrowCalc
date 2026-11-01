# Architektur

## Überblick

GrowCalc ist eine Single-Activity-App mit klassischen XML-Layouts (Material 3, View-System).

```
MainActivity
 ├── MaterialToolbar + DrawerLayout/NavigationView
 └── fragment_container
      ├── DashboardFragment      → Kacheln zu den Rechnern
      ├── VolumenstromFragment   → ClimateCalculator.volumeFlow
      ├── VpdFragment            → ClimateCalculator.vpd
      ├── DliFragment            → ClimateCalculator.dli
      └── NutrientFragment ⇄ NutrientViewModel → NutrientCalculator
```

## Schichten

| Paket | Aufgabe |
|---|---|
| `calculation` | Reine Kotlin-Objekte ohne Android-Abhängigkeiten. Enthalten alle Formeln und sind dadurch einfach per JUnit testbar. |
| `model` | Datenklassen (`Manufacturer` → `NutrientLine` → `FeedProgram` → `FeedWeek` → `NutrientComponent`) und `NutrientDataProvider` mit den Herstellerschemata. |
| `viewmodel` | `NutrientViewModel` hält die Auswahl und Eingaben des Dosierrechners in `LiveData` und überlebt so Konfigurationswechsel (z. B. Drehen). |
| `fragments` | UI: lesen die Eingaben, prüfen sie, rufen die Rechenlogik auf und zeigen die Ergebnisse. |
| `ui` | Gemeinsame UI-Hilfen, z. B. `TextView.showStatus()` für farbige Status-Labels (`StatusTone`: GOOD, INFO, WARN, BAD). |

## Navigation

`MainActivity` tauscht Fragmente per `replace()` aus und setzt den Toolbar-Titel. Die Zurück-Taste führt aus jedem Rechner zurück zum Dashboard (über `OnBackPressedDispatcher`, kompatibel mit Predictive Back).

## Design-System

Alle Design-Tokens liegen in `res/values`:

- `colors.xml` / `values-night/colors.xml` – Farbpalette für hell und dunkel (Marke, Oberflächen, Werkzeug-Akzente, Status-Farben)
- `themes.xml` – Theme `Theme.GrowCalc` und wiederverwendbare Styles:
  - `Widget.Grow.Card.Section | Tile | Result` – Karten
  - `Widget.Grow.TextInputLayout(.Dropdown)` – Eingabefelder (Standard-Style über `textInputStyle`)
  - `Widget.Grow.Button(.Tonal)` – Buttons
  - `Grow.IconBadge`, `Grow.StatusPill`, `Grow.StatTile`, `Grow.KeyRow` … – kleine Bausteine
- `drawable/ic_*.xml` – Vektor-Icons, `bg_*.xml` – Hintergründe (Hero-Verlauf, Pills, Kacheln)

Neue Rechner sollten diese Styles verwenden, damit das Erscheinungsbild einheitlich bleibt.

## Einen neuen Rechner hinzufügen

1. Formel als Funktion in `calculation/` anlegen und Unit-Test schreiben.
2. Layout `fragment_<name>.xml` nach dem Muster der anderen Rechner erstellen (Header → Eingabe-Karte → Buttons → Ergebnis-Karte).
3. Fragment in `fragments/` anlegen.
4. Menüeintrag in `res/menu/drawer_menu.xml` und Navigation in `MainActivity` ergänzen.
5. Kachel im Dashboard (`fragment_dashboard.xml` + `DashboardFragment`) hinzufügen.

## Technik

| | |
|---|---|
| Sprache | Kotlin |
| minSdk / targetSdk | 24 / 36 |
| Build | Gradle (Kotlin DSL), Version Catalog `gradle/libs.versions.toml` |
| Bibliotheken | AndroidX AppCompat, Fragment, Lifecycle (ViewModel/LiveData), ConstraintLayout, Material Components |
| Tests | JUnit 4 (Rechenlogik) |
