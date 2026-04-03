# Kvaesitso - Project Overview

## Identity

**Kvaesitso** is a search-focused, free and open source launcher for Android. Built from scratch — not based on the AOSP launcher — it follows its own design concepts with global search as the central feature.

- **Repository:** [MM2-0/Kvaesitso](https://github.com/MM2-0/Kvaesitso)
- **Website:** https://kvaesitso.mm20.de
- **Package ID:** `de.mm20.launcher2`
- **Current Version:** 1.39.3 (version code 2026012400)
- **Min SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 36)
- **License:** GNU General Public License v3.0 (GPLv3)
  - Exception: Plugin SDK (`plugins/sdk` and `core/shared`) uses Apache License 2.0
- **Copyright:** 2021-2026 MM2-0 and contributors
- **Creator:** MM2-0
- **Community:** Telegram (@Kvaesitso)
- **Translations:** 40+ languages via [Weblate](https://i18n.mm20.de/engage/kvaesitso/)
- **Sponsorship:** [GitHub Sponsors](https://github.com/sponsors/MM2-0)
- **App Icon By:** @EliotAku

## Distribution

| Channel | Details |
|---------|---------|
| F-Droid (official) | Official repo — some non-FOSS features removed |
| MM20 F-Droid repo | https://fdroid.mm20.de/repo/ — feature-complete builds |
| IzzyOnDroid | Third-party F-Droid repo — delayed updates |
| GitHub Releases | Manual APK download |
| Nightly builds | https://fdroid.mm20.de/app/de.mm20.launcher2.nightly |

### Build Variants

| Variant | Application ID | Notes |
|---------|---------------|-------|
| Debug | `de.mm20.launcher2.debug` | Debuggable |
| Release | `de.mm20.launcher2.release` | Production |
| Nightly | `de.mm20.launcher2.nightly` | Date-stamped version suffix |
| F-Droid | (release with fdroid flavor) | Version name suffix |

## Core Features

### Global Search
The defining feature. Searches across multiple on-device and online sources:
- Installed apps and app shortcuts
- Contacts (name, phone, email)
- Calendar events
- Local files
- Cloud files (Nextcloud, OwnCloud, Google Drive, OneDrive)
- Wikipedia articles
- Web search (customizable providers)
- Locations (OpenStreetMap, plugins)

### Built-in Tools
- **Calculator** — math expression evaluation, DEC/OCT/HEX/BIN conversions (powered by mXparser)
- **Unit Converter** — mass, length, area, temperature, time, velocity, volume, currency
- **Quick Actions** — call, message, email, add contact, set alarm/timer, schedule event, open URL

### Customization
- **Color Schemes** — Material Design 3 with HCT color model, dynamic wallpaper colors (Material You), custom palettes
- **Themed Icons** — third-party icon pack support
- **Per-item Customization** — custom labels, icons, tags per searchable item
- **Shapes** — configurable corner radius and shape styles
- **Typography** — custom font families and sizes
- **Transparency** — configurable surface opacity

### Widgets
- Weather (current + forecast, compact mode)
- Calendar (7-day events, calendar selection)
- Clock (multiple styles: analog, digital, binary)
- Music (media playback controls)
- Notes (quick note taking)
- Favorites (pinned items)
- External Android AppWidgets (full system widget support)

### Integrations
- **Weather Providers:** MET Norway, OpenWeatherMap, HERE, Bright Sky, Breezy Weather (plugin)
- **Cloud Storage:** Nextcloud, OwnCloud, Google Drive, OneDrive (plugin)
- **Media Control:** Active media session control (play, pause, skip, seek)
- **Feed:** Google Discover / Lawnfeed integration
- **Smartspacer:** Smart widget integration
- **Tasks.org:** Task management (via plugin)
- **Public Transport:** Multiple transit sources (via plugin)

### Gesture System
Configurable actions for 8 gesture types:
- Swipe Up, Down, Left, Right
- Double Tap, Long Press
- Home Button, Tap Search Bar

### Favorites
Three-level pinning system:
1. Manually sorted pinned items
2. Automatically sorted pinned items
3. Frequently used items (auto-ranked by launch count)

### Tags
User-defined categorization for organizing search results.

### Privacy
- All data stored locally in app-specific secure directory
- HTTPS for external service communication
- Location only used if permission granted and auto-location enabled
- Crash reports stored locally, shared only by user choice
- No analytics or tracking

## Signing Key Fingerprints
- SHA1: `AF:1D:5F:4A:72:FB:AF:9F:CE:32:81:42:D1:ED:4A:3E:A4:E7:75:74`
- SHA256: `BF:65:BB:D6:17:99:73:97:80:0D:02:E0:AC:2E:45:CE:E1:53:15:15:18:54:5C:23:EF:66:B3:CB:2C:FD:F7:BC`
