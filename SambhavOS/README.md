# SAMBHAV OS

A native Android launcher (Kotlin + Jetpack Compose), built from the approved **Concept A: Luxury Editorial** design.
Application id: `in.sambhav.os.launcher`. Android 9 (API 28) and newer. No root needed.

> Status: this project has **not been compiled yet**. The first GitHub Actions run is the real test.
> If it fails, open the failed run, copy the red error lines and send them back; they get fixed one by one.

## Build it from your phone (GitHub mobile website)

1. Create a new GitHub repository (github.com, Chrome on your phone, "Desktop site" off is fine).
2. In the repository tap **Add file > Create new file**. Name it exactly `.github/workflows/build.yml`
   (typing the slashes makes the folders). Paste the contents of `build.yml` from this package. Tap **Commit changes**.
3. Tap **Add file > Upload files**, choose `SambhavOS.zip` (do not unzip it), and commit.
   The workflow unpacks the zip by itself.
4. Open the **Actions** tab. The build starts automatically. Wait for the green tick (about 5 to 10 minutes the first time).
5. Open the finished run, scroll to **Artifacts**, download `SambhavOS-debug-apk`, unzip it, install `app-debug.apk`
   (allow "Install unknown apps" for your browser or file manager when asked).
6. Open SAMBHAV OS once, then **Settings > Set as default**, or press Home and choose SAMBHAV OS > Always.

If you later unzip the project into the repository yourself, the workflow builds it directly instead.

## What works

- Home screen: live clock and date, real battery strip, wallpaper shows through (use Wallpaper in edit mode), dim control.
- Press and hold an icon or empty space: edit mode with jiggle, drag to rearrange, remove badges, Wallpaper and Settings buttons.
- Dock (up to 5 apps), app-open scale animation from the tapped icon, spring drawer animation.
- Swipe up or tap Search: app drawer with Favorites, Recent, A to Z, live search, recent searches, keyboard search action.
- Long-press an app in the drawer: its shortcuts (when default launcher), add to home or dock, favorite, hide, app info, uninstall.
- Page 2: real Android widgets through the AppWidget framework (add, configure, resize, remove).
- Settings: theme, accent, wallpaper dim, clock, icon shape and size and style (themed monochrome on Android 13+), labels,
  grid columns and rows, drawer columns, hidden apps, animations, backup and restore to a file, default launcher.
- Installs, updates and removals are picked up automatically. No background services.

## Known limits of this first version

- No built-in weather: weather needs a network service. Add your phone's weather widget on page 2 instead.
- Cormorant Garamond is not bundled (the system serif is used). Drop font files into `res/font` later to match the mockup exactly.
- Icon packs, folders and multiple icon pages are not built yet.
- Dock and drawer icons cannot be dragged; use the long-press menu.

## Project layout

```
settings.gradle.kts, build.gradle.kts, gradle.properties
.github/workflows/build.yml
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/...                 theme, launcher icon, strings
app/src/main/java/sambhav/os/launcher/
  MainActivity.kt, LauncherViewModel.kt
  data/      Prefs.kt, AppInfo.kt, AppRepository.kt
  widgets/   WidgetController.kt
  ui/theme/  Theme.kt
  ui/components/ Common.kt
  ui/home/   HomeScreen.kt, HomeGrid.kt
  ui/drawer/ AppDrawer.kt
  ui/widgets/ WidgetsPage.kt
  ui/settings/ SettingsScreen.kt
```

The code package is `sambhav.os.launcher` because `in` is a reserved word in Kotlin and Java and would break the generated
`R` class. The installed app id is still `in.sambhav.os.launcher`.
