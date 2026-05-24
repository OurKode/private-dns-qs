# Private DNS Quick Setting

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Latest Release](https://img.shields.io/github/v/release/flashsphere/private-dns-qs?label=GitHub%20Release)](https://github.com/flashsphere/private-dns-qs/releases/latest)

Toggle and configure your Private DNS settings on Android 9+ (API 28+) directly from your Quick Settings panel.

This project is a feature-rich fork of [joshuawolfsohn/Private-DNS-Quick-Tile](https://github.com/joshuawolfsohn/Private-DNS-Quick-Tile) with major enhancements, including custom DNS provider profiles, Shizuku support, and advanced security options.

---

## Features

- **Quick Settings Tile:** Easily cycle through your configured Private DNS modes (Off, Auto, Private Hostname) from the Android Quick Settings panel.
- **Custom DNS Provider List:** 
  - Switch between multiple custom DNS providers with ease.
  - Built-in preconfigured popular profiles (Cloudflare, Google, AdGuard, Quad9).
  - Add, select, and manage your own custom DNS providers with dedicated names and hostnames.
- **Security - Lock Screen Protection:** Option to require the device to be unlocked before toggling the Private DNS tile, preventing unauthorized changes.
- **Shizuku Integration:** Automatically grants the required secure system permission, eliminating the need for ADB commands if you have Shizuku installed.
- **Launcher Shortcuts:** Quick access shortcuts on your home screen launcher:
  - **Toggle DNS:** Cycles through active states.
  - **Set DNS Off / Auto / On:** Sets the DNS mode directly.
- **Modern Jetpack Compose UI:** Beautifully crafted user interface utilizing Material Design 3 with dynamic theme adaptation.

---

## Setup & Permissions

Due to Android security restrictions, changing the Private DNS system setting requires the `WRITE_SECURE_SETTINGS` permission. You can grant this permission using one of the following methods:

### Method 1: Shizuku (Recommended & Automatic)
If you use [Shizuku](https://shizuku.rikka.app/), the app can request and grant this permission automatically inside the application.
1. Download and start Shizuku.
2. Open **Private DNS Quick Setting**.
3. Authorize the app when prompted by Shizuku.

### Method 2: ADB (Manual)
If you don't use Shizuku, you can grant the permission manually using ADB:
1. Enable **USB Debugging** in your Android Developer Options.
2. Connect your device to a computer and execute the following command:
   ```bash
   adb shell pm grant com.flashsphere.privatednsqs android.permission.WRITE_SECURE_SETTINGS
   ```
3. For step-by-step help, refer to the [Online Help Guide](https://private-dns-qs.web.app/help).

---

## Architecture & Tech Stack

This project is built using modern Android development practices:
* **UI Framework:** [Jetpack Compose](https://developer.android.com/compose) with [Material Design 3 (M3)](https://m3.material.io/).
* **Data Storage:** [Jetpack DataStore (Preferences)](https://developer.android.com/topic/libraries/architecture/datastore) for storing user configurations.
* **Asynchronous Flow:** Kotlin Coroutines & Flows for reactive state management.
* **System Integration:** 
  - `dev.rikka.shizuku` for secure automated permission execution.
  - `hiddenapibypass` for accessing system APIs.
* **Build Configuration:** Multi-flavor build system (`launcher` for full app vs `nolauncher` for tile/shortcut only).

---

## Building & Compiling

### Prerequisites
* Android Studio (Koala or newer recommended)
* JDK 21

### Compilation Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/flashsphere/private-dns-qs.git
   cd private-dns-qs
   ```
2. Compile the debug build using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

### Product Flavors
The project is configured with two product flavors under `dimension = "type"`:
- **`launcher` (Default):** Includes the launcher activity showing the main UI to configure settings and providers.
- **`nolauncher`:** Hides the launcher icon from the app drawer to keep your device clean. (Intended for setup through quick tiles and shortcuts directly).

To build a specific flavor:
```bash
./gradlew assembleLauncherDebug
./gradlew assembleNolauncherDebug
```

---

## Download

<div>
<a href="https://play.google.com/store/apps/details?id=com.flashsphere.privatednsqs" target="_blank">
    <img alt="Get it on Google Play" height="60" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" /></a>

<a href="https://apt.izzysoft.de/fdroid/index/apk/com.flashsphere.privatednsqs" target="_blank">
    <img alt="Get it on F-Droid" height="60" src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" /></a>

<a href="https://github.com/flashsphere/private-dns-qs/releases/latest" target="_blank">
    <img alt="Get it on GitHub" height="60" src="https://github.com/flashsphere/private-dns-qs/blob/main/static/get-it-on-github.png?raw=true" /></a>
</div>

---

## License & Credits

* Originally based on [Private-DNS-Quick-Tile](https://github.com/joshuawolfsohn/Private-DNS-Quick-Tile) by Joshua Wolfsohn.
* Released under the GPL-3.0 License. See the [LICENSE](file:///c:/Users/fikri/Documents/android/private-dns-qs/LICENSE) file for details.