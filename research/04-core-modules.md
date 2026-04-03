# Kvaesitso - Core Modules Deep Dive

## core:base — Foundation Module

The central abstraction layer providing search, plugin, icon, and settings interfaces.

### Searchable System
- `Searchable` — marker interface for all search results
- `SavableSearchable` — extends Searchable + Comparable for persistence
- `SearchableRepository<T>` — `search(query, allowNetwork): Flow<List<T>>`
- `SearchableSerializer` / `SearchableDeserializer` — JSON (de)serialization with type awareness
- `ResultScore` — Jaro-Winkler based scoring with prefix/substring bonuses

### Plugin API
- `Plugin` — plugin metadata
- `PluginApi` — base plugin interface
- `PluginPackage` — installed plugin package
- `PluginState` — Ready or SetupRequired
- `QueryPluginApi` — search/get/refresh pattern for query plugins

### Icon System
- `LauncherIcon` — icon data model
- `LauncherIconLayer` — layer-based icon composition

### Other
- `FeatureFlags` — feature flag management
- `Badge` — visual indicator utilities
- `BaseSettings` — preference base types
- `Backupable` — interface for backup/restore support

**Dependencies:** core:ktx, core:i18n, core:shared, DataStore, Palette, Koin

---

## core:shared — Plugin SDK Contracts (Apache 2.0)

Pure Kotlin module shared between the launcher and plugin SDK. No Android dependencies.

### Plugin Contracts
- `CalendarPluginContract` — calendar event data exchange
- `ContactPluginContract` — contact data exchange
- `WeatherPluginContract` — weather forecast data
- `SearchPluginContract` — generic search results
- Bundle/Cursor data models
- `PluginType` enum

### Serializers
- Color, DateTime, Duration, URI, DayOfWeek serializers
- Used by both launcher internals and third-party plugin apps

---

## core:ktx — Kotlin Extensions

Utility extension functions for Android APIs and Kotlin stdlib:

- **Context** extensions — resource access, system services
- **View/Drawable** extensions — UI utilities
- **String/Int/Float** extensions — formatting, conversion
- **List/Iterable** extensions — collection operations
- **Location/Address** extensions — geolocation helpers
- **Notification** extensions — notification building
- **PackageManager** extensions — app info queries
- **SharedPreferences** extensions — preference access
- **PendingIntent** extensions — intent creation
- **Rect/geometry** extensions — layout math
- **JSON/UUID/UserHandle** extensions — data handling

---

## core:preferences — Settings Management

Centralized DataStore-based settings with migration support.

### Key Classes
- `LauncherDataStore` — main settings store
- `LauncherSettingsData` — root settings model (serializable)

### Settings Categories
| Category | Controls |
|----------|----------|
| UI | Layout, grid, appearance |
| Badge | Notification badges, profile badges |
| Icon | Icon packs, adaptive icons, themed icons |
| Search | Calculator, calendar, contacts, files, location, ranking, shortcuts, websites, Wikipedia, unit converter |
| Weather | Provider, auto-location, update interval |
| Media | Player preferences |
| Feed | Feed provider selection |
| Gesture | Per-gesture action bindings |
| Locale | Language, region, imperial/metric defaults |

### Migrations
- Migration2 through Migration6 for backward compatibility
- DataStore proto-based persistence

**Dependencies:** core:base, core:ktx, core:i18n, DataStore, Koin

---

## core:i18n — Internationalization

String resources for 30+ languages. No code — resources only.

### Supported Languages
Arabic, Azerbaijani, Belarusian, Bengali, Catalan, Chinese (Simplified/Traditional), Croatian, Czech, Dutch, English, Finnish, French, German, Greek, Hebrew, Hungarian, Indonesian, Italian, Japanese, Korean, Lithuanian, Norwegian, Persian, Polish, Portuguese (BR/PT), Romanian, Russian, Serbian, Slovak, Spanish, Swedish, Turkish, Ukrainian, Vietnamese

### Structure
- `res/values/strings.xml` — English (default)
- `res/values-[locale]/strings.xml` — translations
- Gender-specific variants supported (Android 14+)

---

## core:permissions — Permission Management

Reactive permission tracking system.

### Permission Groups
| Group | Android Permission |
|-------|-------------------|
| Calendar | READ_CALENDAR, WRITE_CALENDAR |
| Tasks | org.tasks.permission.READ_TASKS |
| Location | ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION |
| Contacts | READ_CONTACTS |
| ExternalStorage | READ_EXTERNAL_STORAGE / MANAGE_EXTERNAL_STORAGE |
| Notifications | NotificationListenerService |
| AppShortcuts | (LauncherApps API) |
| Accessibility | AccessibilityService |
| ManageProfiles | INTERACT_ACROSS_PROFILES |
| Call | CALL_PHONE |

### Key Features
- `PermissionsManager` interface with `PermissionsManagerImpl`
- StateFlow for each permission group status
- Android 11+ MANAGE_EXTERNAL_STORAGE handling
- Special permission handlers (Notification Listener, Accessibility)

**Dependencies:** core:ktx, core:crashreporter, AndroidX Core, Koin

---

## core:crashreporter — Crash Reporting

### Features
- `CrashReporter` singleton wrapping balsikandar CrashReporter library
- `logException()` — logs exceptions (filters CancellationException)
- `getCrashReports()` — retrieves last 7 days of crash logs
- `getDeviceInformation()` — collects device info for diagnostics
- `CrashReport` — parsed crash log with stack traces
- All crash data stored locally, never sent automatically

---

## core:compat — Android Compatibility

### PackageManagerCompat
- `getInstallSourceInfo()` across Android versions
- Pre-R: uses deprecated `getInstallerPackageName()`
- R+: uses `getInstallSourceInfo()` with full metadata
- Returns `InstallSourceInfoCompat` with originating/initiating/installing package names

---

## core:devicepose — Device Orientation

### DevicePoseProvider
Reactive sensor and location APIs for context-aware features.

| Flow | Data |
|------|------|
| `getLocation()` | GPS/network location with caching |
| `getAzimuthDegrees()` | Compass heading (ROTATION_VECTOR sensor) |
| `getHeadingToDegrees()` | Relative bearing (compass + gravity) |

- Callback flows with thread-safe location caching (ReentrantReadWriteLock)
- Used for location-aware search and potential gesture context

---

## core:profiles — Multi-User Support

### ProfileManager
Manages Personal, Work, and Private Space profiles.

### Profile Types
| Type | Description |
|------|-------------|
| Personal | Main user profile |
| Work | Managed work profile |
| Private | Android Private Space |

### Reactive State
- `profiles` flow — all available profiles
- `activeProfiles` flow — unlocked profiles only
- `ProfileWithState` — profile + locked/unlocked state
- Broadcast receivers for profile added/removed/available/unlocked events

**Dependencies:** core:permissions, core:ktx, LauncherApps, UserManager
