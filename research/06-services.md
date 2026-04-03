# Kvaesitso - Services Layer Deep Dive

## services:search — Central Search Orchestration

The heart of the launcher. Combines all data sources into unified search results.

### SearchService Interface
```kotlin
search(query: String, filters: SearchFilters): Flow<SearchResults>
getAllApps(): Flow<Map<ProfileType, List<Application>>>
```

### Search Sources (in execution order)
1. Applications + AppShortcuts
2. Contacts
3. Calendar events
4. Files (local + cloud)
5. Search Actions (built-in + custom)
6. Calculator + Unit Converter
7. Wikipedia (delayed +750ms)
8. Locations (delayed +250ms)
9. Websites

### Features
- Concurrent parallel searches via coroutines
- Staggered delays for expensive/network sources
- Custom attributes integration (labels, tags)
- Profile-aware filtering (Personal, Work, Private)
- Search action support for direct actions

---

## services:badges — Badge System

Visual indicators on search items.

### Badge Providers
| Provider | Badge Source |
|----------|-------------|
| NotificationBadgeProvider | Active notifications |
| CloudBadgeProvider | Cloud sync status |
| ProfileBadgeProvider | Work/Private profile indicator |
| AppShortcutBadgeProvider | Shortcut badge counts |
| PluginBadgeProvider | Plugin-reported badges |
| HiddenItemBadgeProvider | Hidden item indicator |
| SuspendedAppsBadgeProvider | Suspended/disabled apps |

- Dynamically enabled/disabled via BadgeSettings
- Multiple badge sources combined into single Badge (count + color)
- Reactive updates via Flow

---

## services:icons — Icon Management

### IconService
Main icon resolution with **LRU cache (200 items)**.

### Icon Provider Chain
| Provider | Purpose |
|----------|---------|
| SystemIconProvider | Default system icons |
| PlaceholderIconProvider | Fallback placeholders |
| ThemedPlaceholderIconProvider | Themed fallbacks |
| DynamicClockIconProvider | Live clock icons |
| CalendarIconProvider | Date-aware calendar icons |
| IconPackIconProvider | Third-party icon packs |
| CustomIconPackIconProvider | User-selected per-app icons |
| CustomTextIconProvider | Text character icons |
| CustomThemedIconProvider | User-themed icons |

### Icon Transformations
- `LegacyToAdaptiveTransformation` — converts legacy icons to adaptive
- `ForceThemedIconTransformation` — applies theme colors to any icon

### IconPackManager
- Auto-detects installed icon packs via broadcast
- Loads and manages icon pack metadata
- Per-app icon override support

---

## services:favorites — Favorites Management

### FavoritesService
| Function | Purpose |
|----------|---------|
| `getFavorites()` | Get favorites filtered by pin level |
| `pinItem()` / `unpinItem()` | Pin/unpin items |
| `setVisibility()` | Show/hide items |
| `reportLaunch()` | Track usage for ranking |
| `updateFavorites()` | Bulk update sorted items |
| `reset()` | Clear all pinning/visibility |

### Pin Levels
- ManuallySorted — user-arranged order
- AutomaticallySorted — smart ranking
- FrequentlyUsed — auto-promoted by launch count

---

## services:tags — Tagging System

### TagsService
| Function | Purpose |
|----------|---------|
| `getAllTags()` | List all user tags |
| `createTag()` / `updateTag()` / `deleteTag()` | CRUD |
| `cloneTag()` | Duplicate tag |
| `getTags(searchable)` | Tags for specific item |
| `getTaggedItems(tag)` | Items with specific tag |

Uses CustomAttributesRepository + SavableSearchableRepository internally.

---

## services:plugins — Plugin Management

### PluginService
| Function | Purpose |
|----------|---------|
| `getPluginsWithState()` | All plugins + enable/disable state |
| `enablePluginPackage()` | Enable plugin |
| `disablePluginPackage()` | Disable plugin |
| `getPluginIcon()` | Plugin icon |
| `uninstallPluginPackage()` | Remove plugin |

### PluginScanner
- Discovers plugins via PackageManager intent queries
- SHA256 signature verification
- Auto-refreshes on app install/uninstall/update events
- Supports all plugin types: FileSearch, Weather, LocationSearch, Calendar, ContactSearch

---

## services:widgets — Widget Management

### WidgetsService
| Function | Purpose |
|----------|---------|
| `getAppWidgetProviders()` | System AppWidget providers (across profiles) |
| `getAvailableBuiltInWidgets()` | Weather, Music, Calendar, Apps, Notes |
| `addWidget()` / `updateWidget()` / `removeWidget()` | CRUD |
| `getWidgets()` | All configured widgets |
| `isFavoritesWidgetFirst()` | Check AppsWidget position |
| `countWidgets(type)` | Count by type |

---

## services:global-actions — System Actions

### GlobalActionsService
Uses AccessibilityService + reflection for system-level actions.

| Action | Method |
|--------|--------|
| Notification Drawer | `openNotificationDrawer()` |
| Quick Settings | `openQuickSettings()` |
| Lock Screen | `lockScreen()` |
| Power Dialog | `openPowerDialog()` |
| Recent Apps | `openRecents()` |

### LauncherAccessibilityService
Background service that performs global actions when direct API access fails.
Uses reflection for status bar expansion as primary method, AccessibilityService as fallback.

---

## services:feed — Feed Integration

### FeedService
Google Discover / Lawnfeed integration.

| Function | Purpose |
|----------|---------|
| `createFeedInstance()` | Create AIDL connection to feed provider |
| `getAvailableFeedProviders()` | Discover providers via intent |

- Uses `com.android.launcher3.WINDOW_OVERLAY` intent filter
- AIDL-based inter-process communication
- Blocks known problematic providers

---

## services:music — Media Control

### MusicService
Media session controller wrapper.

### Reactive Flows
| Flow | Data |
|------|------|
| `playbackState` | Playing, paused, stopped |
| `title` | Current track title |
| `artist` | Current artist |
| `album` | Current album |
| `albumArt` | Cover artwork |
| `position` / `duration` | Playback progress |
| `supportedActions` | Available controls |

### Controls
`play()`, `pause()`, `togglePause()`, `next()`, `previous()`, `seekTo()`

### Features
- `openPlayer()` — launch active player app
- `openPlayerChooser()` — player selection dialog
- `getInstalledPlayerPackages()` — list available players
- Per-app player memory
- Album art loading via Coil
- Custom action support

---

## services:backup — Backup/Restore

### BackupManager
Full app state backup and restore.

| Function | Purpose |
|----------|---------|
| `backup(uri)` | Create ZIP archive |
| `restore(uri)` | Restore from ZIP |
| `readBackupMeta()` | Read metadata without extraction |

### BackupMetadata
- Version, timestamp, device info

### Format
ZIP archive with component-specific subdirectories. Each `Backupable` component contributes its own data section.

### Backupable Components
Preferences, favorites, custom icons, custom attributes, search actions, themes, widget configs

---

## services:accounts — Cloud Account Management

### AccountsRepository
| Function | Purpose |
|----------|---------|
| `signin()` | OAuth flow for cloud service |
| `signout()` | Remove account |
| `getCurrentlySignedInAccount()` | Active account info |
| `isSupported()` | Check if account type available in build |

### Account Types
| Type | Library |
|------|---------|
| Nextcloud | libs:nextcloud (OAuth + WebDAV) |
| OwnCloud | libs:owncloud (OAuth + WebDAV) |

Both use `libs:webdav` for file operations, `androidx.browser` for OAuth, and `androidx.security-crypto` for credential storage.
