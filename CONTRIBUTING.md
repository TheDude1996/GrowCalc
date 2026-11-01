# Mitwirken

Danke für dein Interesse an GrowCalc!

## Fehler melden / Ideen

Bitte ein [Issue](../../issues) anlegen und dabei angeben:

- Android-Version und Gerät
- Welcher Rechner, welche Eingaben
- Erwartetes und tatsächliches Ergebnis (gern mit Screenshot)

## Code beitragen

1. Repository forken und einen Branch anlegen: `git checkout -b feature/mein-feature`
2. Änderungen machen. Dabei:
   - Rechenlogik gehört nach `calculation/` und bekommt einen Unit-Test.
   - Für die UI die vorhandenen Styles aus `res/values/themes.xml` verwenden (siehe [docs/ARCHITEKTUR.md](docs/ARCHITEKTUR.md)).
   - Texte in `res/values/strings.xml` ablegen, nicht im Layout hart codieren.
   - Hell- **und** Dunkelmodus prüfen.
3. Tests ausführen: `./gradlew testDebugUnitTest`
4. Commit mit aussagekräftiger Nachricht und Pull Request öffnen.

## Code-Stil

- Kotlin-Stil: `kotlin.code.style=official`
- Kleine, fokussierte Pull Requests sind einfacher zu prüfen.
