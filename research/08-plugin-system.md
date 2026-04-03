# Kvaesitso - Plugin System Deep Dive

## Overview

Kvaesitso's plugin system allows third-party apps to extend the launcher's search and data capabilities via Android ContentProvider APIs. The Plugin SDK abstracts the low-level IPC details.

- **SDK Package:** `de.mm20.launcher2:plugin-sdk` (Maven Central)
- **License:** Apache License 2.0
- **SDK Versions:** v2.0.0 through v2.3.0

## Plugin Types

```kotlin
enum class PluginType {
    FileSearch,
    Weather,
    LocationSearch,
    Calendar,
    ContactSearch,
}
```

## Architecture

### Registration (AndroidManifest.xml)
```xml
<provider android:name=".MyPlugin"
    android:authorities="your.package.name.authority"
    android:exported="true">
    <intent-filter>
        <action android:name="de.mm20.launcher2.action.PLUGIN" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</provider>
```

**Important:** The `android:authorities` value is permanent — changing it breaks the plugin.

### Base Plugin Provider

`BasePluginProvider` extends Android's `ContentProvider`:

| Method Call | Purpose |
|-------------|---------|
| `GetType` | Returns plugin type |
| `GetState` | Returns PluginState (Ready / SetupRequired) |
| `GetConfig` | Returns plugin configuration |

- `checkPermissionOrThrow()` validates the calling app
- `PluginPermissionManager` uses DataStore for permission tracking

### Plugin Lifecycle States

```kotlin
sealed class PluginState {
    data class Ready(val statusText: String?) : PluginState()
    data class SetupRequired(val intent: Intent, val message: String) : PluginState()
}
```

## Query Plugin Pattern

`QueryPluginProvider<TQuery, TResult>` — standard pattern for searchable plugins:

| Method | Purpose |
|--------|---------|
| `search(query, params)` | Search for items |
| `get(id, params)` | Fetch single item by ID |
| `refresh(item, params)` | Update cached item |

### SearchParams
- `allowNetwork: Boolean`
- `lang: String`

### Storage Strategies
| Strategy | Behavior |
|----------|----------|
| StoreCopy | Launcher stores full item, calls `refresh()` on demand |
| StoreReference | Launcher stores only ID, calls `get()` to fetch |

## Specialized Plugin Providers

### WeatherProvider
Extends `BasePluginProvider` directly (not QueryPlugin).

| URI Path | Data |
|----------|------|
| `/forecasts` | Weather forecast data (Cursor with ForecastColumns) |
| `/locations` | Location search results |

**Parameters:** lat, lon, id, name, language

**ForecastColumns:** Timestamp, Temperature, Pressure, Humidity, WindSpeed, WindDirection, Rain, Clouds, UVIndex, Night, Condition, Icon, Location, Provider, etc.

### ContactProvider
`QueryPluginProvider<String, Contact>`

**Contact Model:** id, uri, name, phoneNumbers[], emailAddresses[], postalAddresses[], customActions[], photoUri

### CalendarProvider
`QueryPluginProvider<CalendarQuery, CalendarEvent>`

**CalendarEvent Model:** id, title, startTime, endTime, location, color, attendees, isCompleted

**Query:** search text + time range (from, to)

### LocationProvider
`QueryPluginProvider<LocationQuery, Location>`

**LocationQuery:** query text, userLocation, allowNetwork, radius, hideUncategorized

**Location Model:** id, name, lat, lon, address, category, description, url, icon, distance, openingHours

### FileProvider
`QueryPluginProvider<String, File>`

**File Model:** id, uri, displayName, mimeType, size, path, isDirectory, thumbnailUri, owner, metadata (title, artist, album, dimensions, appName)

## Plugin Discovery & Management

### PluginScanner (services:plugins)
- Discovers plugins via PackageManager intent queries
- Filters for `de.mm20.launcher2.action.PLUGIN` action
- SHA256 signature verification
- Auto-refreshes on app install/uninstall/update

### PluginService
- `getPluginsWithState()` — all plugins with enable/disable state
- `enablePluginPackage()` / `disablePluginPackage()`
- `uninstallPluginPackage()`

### Database Storage (data:plugins)
- `PluginEntity`: authority (PK), label, description, packageName, className, type, enabled
- `PluginDao`: CRUD operations

## Official Plugins

| Plugin | Type(s) | Source |
|--------|---------|--------|
| OpenWeatherMap | Weather | Official |
| Breezy Weather | Weather | Official |
| HERE | Weather, Places, Departures | Official |
| Foursquare | Places | Official |
| OneDrive | File Search | Official |
| Tasks.org | Calendar (tasks) | Official |
| Google Apps | Calendar, Files, Tasks | Official |
| Public Transport | Departures | Official |
| Meteo.lt | Weather | Official |

All available on GitHub under the [Kvaesitso organization](https://github.com/Kvaesitso) and via F-Droid.

## Plugin Activation Flow

1. User installs plugin APK (F-Droid, GitHub, manual)
2. Kvaesitso detects new ContentProvider via intent filter
3. Plugin appears in Settings → Plugins
4. User enables plugin and grants permissions
5. If `SetupRequired` state: user follows setup intent (e.g., login)
6. Plugin becomes active, data flows through search system

## Security

- Plugins must declare exported ContentProvider
- Caller verification via `checkPermissionOrThrow()`
- DataStore-based permission grants
- SHA256 signature tracking
- User must explicitly enable each plugin
