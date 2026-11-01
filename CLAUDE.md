# CLAUDE.md

GrowCalc – Android-App (Kotlin, Views/Fragments, MVVM) mit Rechnern für Volumenstrom, VPD, DLI, Nährstoffe, PPFD und Bewässerung.

- Paketname / applicationId: `io.github.thedude1996.growcalc`
- Launcher-Activity: `io.github.thedude1996.growcalc/.MainActivity`
- minSdk 24, targetSdk/compileSdk 36, Java 11
- Ein Modul: `:app`.

## Umgebung

Falls `JAVA_HOME` nicht gesetzt ist, vor Gradle-Aufrufen in PowerShell auf das von Android Studio mitgelieferte JDK zeigen:

```powershell
$env:JAVA_HOME = "<ANDROID_STUDIO_DIR>\jbr"
```

Android SDK: `<ANDROID_SDK_DIR>` (adb unter `platform-tools`, Emulator unter `emulator`).

## Bauen

```powershell
.\gradlew assembleDebug        # Debug-APK -> app\build\outputs\apk\debug\
.\gradlew assembleRelease      # signiert automatisch, wenn keystore.properties existiert
```

`keystore.properties` (Projektwurzel) und der Keystore sind nicht eingecheckt – niemals committen oder Inhalte ausgeben.

## Installieren & Starten

Emulator headless starten, falls kein Gerät verbunden ist (verfügbare AVDs: `emulator -list-avds`):

```powershell
emulator -avd <AVD_NAME> -no-window
```

Dann:

```powershell
.\gradlew installDebug
adb shell am start -n io.github.thedude1996.growcalc/.MainActivity
```

Deinstallieren: `adb uninstall io.github.thedude1996.growcalc`

## Testen

```powershell
.\gradlew testDebugUnitTest        # JVM-Unit-Tests (app/src/test), kein Gerät nötig
.\gradlew connectedDebugAndroidTest # Instrumentierte Tests (app/src/androidTest), Gerät/Emulator nötig
.\gradlew lint
```

Die Rechenlogik liegt in `calculation/` (`ClimateCalculator`, `NutrientCalculator`, `LuxPpfdConverter`, `IrrigationCalculator`) und ist per Unit-Test abgedeckt – Änderungen dort immer mit Tests begleiten.

## Hinweise

- In Git Bash brauchen `adb shell`-Pfade wie `/sdcard` `MSYS_NO_PATHCONV=1`.
- Screenshots über Bash holen (`adb exec-out screencap -p > shot.png`); PowerShell-Umleitung beschädigt PNGs.
- Release: versionCode/versionName in `app/build.gradle.kts` erhöhen, `CHANGELOG.md` pflegen.
