# Kvaesitso - Architecture & Module Organization

## High-Level Architecture

Kvaesitso uses a **multi-module Gradle architecture** with 60+ modules organized into 6 layers:

```
┌─────────────────────────────────────────────┐
│                  app/app                     │  Application entry point
│                  app/ui                      │  Compose UI layer
├─────────────────────────────────────────────┤
│              services/*                      │  Business logic orchestration
├─────────────────────────────────────────────┤
│                data/*                        │  Data access & providers
├─────────────────────────────────────────────┤
│                core/*                        │  Foundation & shared abstractions
├─────────────────────────────────────────────┤
│                libs/*                        │  External library wrappers
├─────────────────────────────────────────────┤
│              plugins/sdk                     │  Plugin SDK (published to Maven)
└─────────────────────────────────────────────┘
```

## Module Inventory

### App Layer (2 modules)
| Module | Purpose |
|--------|---------|
| `app:app` | Main application, Koin DI setup, entry point |
| `app:ui` | All Compose UI: screens, themes, widgets, settings |

### Core Layer (10 modules)
| Module | Purpose |
|--------|---------|
| `core:base` | Search abstractions, plugin API, icons, settings |
| `core:shared` | Plugin contracts shared between launcher and SDK (Apache 2.0) |
| `core:ktx` | Kotlin/Android extension functions |
| `core:preferences` | DataStore-based settings with migrations |
| `core:i18n` | String resources (30+ languages) |
| `core:permissions` | Centralized reactive permission management |
| `core:crashreporter` | Crash logging and diagnostics |
| `core:compat` | Android API backward compatibility |
| `core:devicepose` | Device orientation/location sensor tracking |
| `core:profiles` | Multi-user/Work Profile/Private Space support |

### Data Layer (21 modules)
| Module | Purpose |
|--------|---------|
| `data:database` | Room database (v32), entities, DAOs, migrations |
| `data:applications` | Installed app management |
| `data:appshortcuts` | App shortcut management |
| `data:searchable` | Saved/favorited searchable persistence & ranking |
| `data:search-actions` | Web search & intent-based actions |
| `data:customattrs` | Per-item custom labels, tags, icons |
| `data:calendar` | Calendar events (native + Tasks.org + plugins) |
| `data:contacts` | Contacts (native + plugins) |
| `data:weather` | Weather forecasts (multi-provider) |
| `data:calculator` | Math expression evaluation |
| `data:unitconverter` | Unit conversion |
| `data:currencies` | Exchange rate management |
| `data:files` | Local + cloud file search |
| `data:websites` | URL detection & Open Graph metadata |
| `data:wikipedia` | Wikipedia article search |
| `data:locations` | Location/place search (OSM + plugins) |
| `data:widgets` | Widget state persistence |
| `data:themes` | Theme customization (colors, shapes, typography) |
| `data:notifications` | System notification tracking |
| `data:plugins` | Plugin registry & lifecycle |
| `data:i18n` | String normalization for search matching |

### Services Layer (12 modules)
| Module | Purpose |
|--------|---------|
| `services:search` | Central search orchestration across all sources |
| `services:badges` | Badge system (notifications, profiles, hidden) |
| `services:icons` | Icon resolution with LRU cache + icon packs |
| `services:favorites` | Favorites/pinning management |
| `services:tags` | Custom tagging system |
| `services:plugins` | Plugin discovery, loading, state management |
| `services:widgets` | Widget management (built-in + AppWidget) |
| `services:global-actions` | System actions (notifications, power, recents) |
| `services:feed` | Feed/news provider integration |
| `services:music` | Media session control & metadata |
| `services:backup` | Full state backup/restore (ZIP format) |
| `services:accounts` | Cloud account management (Nextcloud, OwnCloud) |

### Libraries (5 modules)
| Module | Purpose |
|--------|---------|
| `libs:material-color-utilities` | Google Material You dynamic color generation |
| `libs:nextcloud` | Nextcloud OAuth + file operations |
| `libs:owncloud` | OwnCloud OAuth + file operations |
| `libs:webdav` | WebDAV protocol (used by Nextcloud/OwnCloud) |
| `libs:address-formatter` | International address formatting (YAML + Mustache) |

### Plugins (1 module)
| Module | Purpose |
|--------|---------|
| `plugins:sdk` | Plugin SDK published to Maven Central |

## Dependency Graph

```
Core Foundation (no Android deps):
  core:shared ──► (pure Kotlin/serialization)
  core:ktx ──► (Kotlin extensions)
  core:compat ──► (API compat shims)
  core:i18n ──► (string resources only)

Core Services:
  core:base ──► core:ktx, core:i18n, core:shared
  core:preferences ──► core:base, core:ktx, core:i18n
  core:permissions ──► core:ktx, core:crashreporter
  core:profiles ──► core:permissions, core:ktx
  core:crashreporter ──► (3rd party lib)
  core:devicepose ──► core:ktx

Data Layer (all depend on core:base):
  data:database ──► Room ORM
  data:applications ──► core:base, core:profiles
  data:searchable ──► core:base, data:database
  data:search-actions ──► core:base, data:database
  data:calendar ──► core:base, core:permissions
  data:contacts ──► core:base, core:permissions
  data:weather ──► core:base, data:database, WorkManager
  data:files ──► core:base, libs:nextcloud, libs:owncloud
  data:plugins ──► core:base, data:database
  ... (each depends on core:base + relevant core modules)

Services Layer:
  services:search ──► core:*, data:* (all data modules)
  services:badges ──► core:*, data:applications, data:notifications
  services:icons ──► core:*, data:customattrs, data:database
  services:favorites ──► data:searchable
  services:tags ──► core:*, data:customattrs, data:searchable
  services:plugins ──► core:base, core:permissions
  services:widgets ──► core:base, data:widgets
  services:global-actions ──► core:ktx (AccessibilityService)
  services:music ──► core:*, data:notifications
  services:backup ──► core:base (ZIP archive)
  services:accounts ──► libs:nextcloud, libs:owncloud

App Layer:
  app:app ──► ALL modules (60+ dependencies, Koin setup)
  app:ui ──► ALL modules (Compose UI, settings, themes)
```

## Key Architectural Patterns

### 1. Reactive Architecture (Kotlin Flow)
All data flows through `Flow<T>` and `StateFlow<T>`. ViewModels expose flows, UI collects them via `collectAsState()`. No LiveData — pure coroutines.

### 2. Dependency Injection (Koin)
`LauncherApplication` initializes 40+ Koin modules. Each feature module defines its own `Module` block. Services are injected as interfaces with internal implementations.

### 3. Repository Pattern
Every data source has a repository interface with implementation:
- `SearchableRepository<T>` — base search interface
- `SavableSearchableRepository` — persistence + ranking
- Per-type repositories (AppRepository, ContactRepository, etc.)

### 4. Multi-Provider Pattern
Each searchable type supports multiple data sources:
- **Native:** Android APIs (Contacts, Calendar, Files)
- **Plugin:** Third-party apps via ContentProvider SDK
- **Cloud:** External services (Nextcloud, weather APIs)
- Providers composed via `supervisorScope` for parallel execution

### 5. Searchable Hierarchy
All searchable items implement `Searchable` or `SavableSearchable`:
```
Searchable (marker)
  ├── SavableSearchable (persistable + comparable)
  │   ├── Application, AppShortcut, Contact
  │   ├── CalendarEvent, File, Location
  │   ├── Website, Article (Wikipedia)
  │   └── custom searchables
  └── SearchAction
      ├── CallAction, MessageAction, EmailAction
      └── WebsearchAction, etc.
```

### 6. Serialization for Persistence
Searchables serialized to JSON for database storage. `SearchableSerializer` / `SearchableDeserializer` handle type-aware (de)serialization with legacy format migration support.

### 7. Score-Based Ranking
- Jaro-Winkler similarity matching (0-1)
- Prefix/substring bonuses
- Primary field weighting
- `ResultScore` composite: `(similarity + prefix*0.2 + substring*0.8) * (primary?1:0.8)`

### 8. Profile-Aware Throughout
Multi-user support (Personal, Work, Private) permeates the architecture:
- `ProfileManager` tracks profile states
- Apps filtered by profile
- Badges indicate profile origin
- Search results grouped by profile

### 9. Backup Interface
Components implement `Backupable` for inclusion in ZIP backup/restore:
- Preferences, favorites, custom icons, custom attributes, search actions, themes

### 10. Composition Locals (Compose DI)
UI-layer dependency provision via CompositionLocal:
- `LocalCurrentTime`, `LocalSettings`, `LocalAppWidgetHost`
- `LocalDarkTheme`, `LocalWallpaperColors`, `LocalSnackbarHostState`
- `LocalBottomSheetManager`, `LocalBackStack`, `LocalGridSettings`
- `LocalWindowSize`, `LocalTransparencyScheme`
