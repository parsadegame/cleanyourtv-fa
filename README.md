# CleanYourTV FA — Persian Android TV launcher

A customized fork of **[CleanYourTV](https://github.com/Gathin23/cleanyourtv) by
[Gathin](https://github.com/Gathin23)** — an Apple TV–style home screen for Android TV /
Google TV — with a Persian info strip and a few Iran-specific touches.

All credit for the launcher, the debloat tooling and the original themes goes to
**Gathin**. This fork only adds and adjusts a small set of features on top of his work.

> Upstream: https://github.com/Gathin23/cleanyourtv
> Original author: **Gathin** · MIT License

---

## What this fork adds

- **Live info strip under the header**
  - Tehran weather from Open-Meteo (city name, temperature, condition, high/low)
  - **US dollar** price in Toman from TGJU
  - **Emami gold coin** price in Toman from TGJU
  - **Gold ounce** price in USD from TGJU
  - Persian labels and Persian digits, refreshed every 10 minutes while Home is visible
- **Persian typography** — bundled Vazirmatn fonts
- **Shamsi (Solar Hijri) date** above the Gregorian date
- **Customized dock** (edit `app/src/main/res/values/config.xml`)
- **ambient-glass theme** included under `themes/ambient-glass/`
  (8 rotating wallpapers, glass dock, 5 columns, icon-only header buttons)

Everything else — the app grid, focus effects, Inputs/Wi-Fi/Settings buttons, Live TV
support, key helpers, settings screen, and all the upstream configuration keys — is
unchanged from CleanYourTV.

## Requirements

- Android TV / Google TV 8.0+ (`minSdk 26`)
- ADB access over the network (`adb connect <tv-ip>:5555`)
- To build: JDK 17+ and the Android SDK (compileSdk 34)

## Build

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

If Maven Central is only reachable through a local proxy, add these lines to
`gradle.properties` (they are intentionally not committed):

```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=10808
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=10808
systemProp.http.nonProxyHosts=localhost|127.0.0.1
```

## Install

```bash
adb connect <tv-ip>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm set-home-activity com.gathin.tvhome/.MainActivity
```

The package id is `com.gathin.tvhome` (kept from upstream so the fork can replace an
existing CleanYourTV install).

## Configuration

- **Launcher / dock / layout** — `app/src/main/res/values/config.xml`
- **Info strip** — `app/src/main/res/values/info.xml`
  - `show_info_strip` — show or hide the whole row
  - `weather_city`, `weather_latitude`, `weather_longitude`
  - `info_refresh_minutes`

The launcher also reads a theme overlay from
`/sdcard/Android/data/com.gathin.tvhome/files/config.json`, which is how the bundled
theme is applied without rebuilding.

## ambient-glass theme

The theme used on the author's TV is included in `themes/ambient-glass/`:

- `config.json` — theme keys (no `dock_apps`, so your own dock apps are kept)
- `wallpapers/` — 8 landscape photographs with `captions.txt`
- `CREDITS.md` — photo sources and licences

Push it with ADB:

```bash
adb push themes/ambient-glass/config.json /sdcard/Android/data/com.gathin.tvhome/files/config.json
adb shell mkdir -p /sdcard/Android/data/com.gathin.tvhome/files/wallpapers
adb push themes/ambient-glass/wallpapers/. /sdcard/Android/data/com.gathin.tvhome/files/wallpapers/
adb shell am force-stop com.gathin.tvhome
adb shell am start -n com.gathin.tvhome/.MainActivity
```

Remove the overlay to go back to the built-in `config.xml`:

```bash
adb shell "rm -f /sdcard/Android/data/com.gathin.tvhome/files/config.json /sdcard/Android/data/com.gathin.tvhome/files/theme_revision; rm -rf /sdcard/Android/data/com.gathin.tvhome/files/wallpapers"
adb shell am force-stop com.gathin.tvhome
adb shell am start -n com.gathin.tvhome/.MainActivity
```

## Credits

- **Gathin** — CleanYourTV, the launcher and its themes:
  https://github.com/Gathin23/cleanyourtv
- **Vazirmatn** font by Saber Rastikerdar (SIL OFL 1.1)
- **Outfit** font (SIL OFL 1.1)
- Wallpaper photographs: see `themes/ambient-glass/CREDITS.md`

## License

MIT — see [LICENSE](LICENSE). The original MIT copyright notice for **Gathin** is
preserved in full.
