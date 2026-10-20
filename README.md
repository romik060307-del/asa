# RK Glass Launcher — Safe Core 1.1.0

This is a deliberately simplified native Kotlin launcher for Android 8.0/API 26 and later. It replaces the complex multi-screen implementation with a basic home screen, search field, and app list to isolate the startup/force-close problem on older devices.

## Build APK
1. Upload the **contents** of this project folder to the root of your GitHub repository, so `app/`, `build.gradle.kts`, `settings.gradle.kts`, and `.github/` are directly at repository root.
2. Open Actions and run **Build RK Glass Launcher APK**.
3. Download artifact `RK-Glass-Launcher-safe-debug-apk`.

The workflow expects the project files directly at repository root.

## Important
This is a reduced safe-core version, not the full Liquid Glass feature set. It is intended to verify that HOME selection, app discovery, search, and app launching work reliably on Android Oreo. The APK has not been built or device-tested in this authoring environment; do not treat the workflow as a confirmed successful build until GitHub Actions passes.


## Reinstall note
Each fresh GitHub Actions debug build may use a different debug signing key. If Android says **App not installed** when updating an older APK, select the Vivo launcher first, uninstall the old RK Glass Launcher, then install the new APK. Uninstalling removes the old launcher app data.


### Build fix (1.1.1-safe)

The Kotlin compile errors around `setTextColor` were caused by the class color property named `text` being shadowed by `TextView.text` inside `apply` blocks. The foreground color property is now named `foreground`, and the affected calls use `setTextColor(foreground)`.

Build using GitHub Actions → **Build RK Glass Launcher APK**. The APK has not been device-tested on a Vivo Y71 in this environment.
