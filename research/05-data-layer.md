# Kvaesitso - Data Layer Deep Dive

## Database (data:database)

Room ORM database at version 32 with extensive migration chain (v6→v32).

### Entities

| Entity | PK | Purpose |
|--------|-----|---------|
| SavedSearchableEntity | key | Pinned/favorited items with usage stats |
| SearchActionEntity | position | Web search & intent actions |
| ForecastEntity | timestamp | Weather forecast data |
| WidgetEntity | id (UUID) | Widget instances and config |
| PluginEntity | authority | Installed plugin metadata |
| CustomAttributeEntity | key+type | Custom labels, tags, icons per item |
| CurrencyEntity | code | Exchange rates |
| IconEntity | authority+type | Icon cache (blob) |
| IconPackEntity | - | Icon pack metadata |
| ColorsEntity | - | Theme color palettes |
| ShapesEntity | - | Theme shape definitions |
| TransparenciesEntity | - | Theme opacity values |
| TypographyEntity | - | Theme typography rules |

### DAOs
WeatherDao, IconDao, SearchableDao, WidgetDao, CurrencyDao, BackupRestoreDao, CustomAttrsDao, SearchActionDao, ThemeDao, PluginDao

### Default Initialization
Pre-configured with weather, music, and calendar widgets.

---

## Applications (data:applications)

### LauncherApp Model
- Wraps `LauncherActivityInfo`
- Properties: componentName, label, versionName, isSuspended, user, score
- Multi-user profile support (work profiles, restricted users)
- Methods: `launch()`, `uninstall()`, `openAppDetails()`, `shareApkFile()`, `loadIcon()`

### AppRepository
- Listens to `LauncherApps` package callbacks in real-time
- `onPackageChanged`, `onPackagesAvailable`, `onPackagesUnavailable`
- MutableStateFlow for reactive updates
- Thread-safe with Mutex locks

---

## Searchable (data:searchable)

Core persistence for favorited/saved search items.

### SavedSearchable
| Field | Purpose |
|-------|---------|
| key | Unique identifier |
| searchable | The actual searchable object |
| launchCount | Usage frequency |
| pinPosition | Pin level (NotPinned=0, Auto=1, Manual=2+) |
| visibility | Hidden=-1, Default=0, Visible=1, MoreVisible=2 |
| weight | Ranking weight |

### Complex Queries
- Filter by type(s), visibility level, pinned level
- Order by: pinPosition DESC → weight DESC → launchCount DESC
- Integrates with SearchableDeserializer for type-based reconstruction

---

## Search Actions (data:search-actions)

### SearchAction Interface
Extends `Searchable` with: label, icon (23 icon types), iconColor, customIcon, `start(context)`

### Built-in Action Builders (9)
CallAction, MessageAction, EmailAction, ScheduleEventAction, SetAlarmAction, TimerAction, WebsearchAction, ShareAction, CustomIntentAction, CustomWebsearchAction

### SearchActionIcon Types (23)
Search, Custom, Website, Alarm, Timer, Contact, Phone, Email, Message, Calendar, Translate, WebSearch, PersonSearch, StatsSearch, SearchPage, SearchList, ImageSearch, Location, Movie, Music, Game, Note, Share

---

## Custom Attributes (data:customattrs)

Per-searchable customization stored as `CustomAttribute` sealed interface:

| Type | Purpose |
|------|---------|
| CustomLabel | Override display label |
| CustomTag | User-defined tag |
| CustomIconPackIcon | Icon from third-party pack |
| CustomThemedIcon | Dynamically themed icon |
| AdaptifiedLegacyIcon | Legacy icon with FG scale/BG color |
| CustomTextIcon | Single character + color |
| UnmodifiedSystemDefaultIcon | Use system default |
| DefaultPlaceholderIcon | Fallback placeholder |
| ForceThemedIcon | Always apply theme colors |

---

## Calendar (data:calendar)

### CalendarEvent Model
id, title, calendarName, description, location, color, startTime, endTime, includeTime, attendees, uri, isCompleted

### Provider Architecture
| Provider | Source |
|----------|--------|
| AndroidCalendarProvider | Native Android calendar (ContentProvider) |
| TasksCalendarProvider | Tasks.org integration |
| PluginCalendarProvider | Third-party plugins |

- Respects Calendar and Tasks permissions
- Configurable excluded calendars
- Default 14-day query range

---

## Contacts (data:contacts)

### Contact Model
name, phoneNumbers[], emailAddresses[], postalAddresses[], customActions[], photoUri

### AndroidContact
- id (Long), lookupKey (String)
- Launches native Android Contacts app
- Loads photos from ContentProvider

### Providers
AndroidContactProvider (native), PluginContactProvider (third-party)

---

## Weather (data:weather)

### Forecast Model
timestamp, temperature, minTemp, maxTemp, pressure, humidity, windSpeed, windDirection, precipitation, clouds, uvIndex, icon, condition, location, provider, providerUrl, night

### Features
- Multi-provider support (built-in + plugins)
- Database-cached forecasts
- Periodic background updates (WorkManager)
- Daily forecast grouping
- Location search for weather setup

---

## Calculator (data:calculator)

### Model
- term (input expression), solution (Double result)
- Supports: hex (0x), binary (0b), octal (0) notation
- Formats output in decimal, binary, hex, octal
- Powered by mXparser Expression library

---

## Unit Converter (data:unitconverter)

### Supported Converters
Mass, Length, Area, Temperature, Time, Velocity, Volume, Currency

### Query Pattern
Parses natural language: `"100 kg to lb"`, `"50 miles in km"`

---

## Currencies (data:currencies)

- Automatic device currency detection
- Symbol-to-code mapping (€→EUR, $→USD, etc.)
- Periodic exchange rate updates (WorkManager)
- Database storage for latest rates

---

## Files (data:files)

### File Model
id, uri, displayName, mimeType, size, path, isDirectory, thumbnailUri, owner, metadata (title, artist, album, duration, dimensions, location, appName)

### Providers
| Provider | Source |
|----------|--------|
| LocalFileProvider | Device file system |
| NextcloudFileProvider | Nextcloud API |
| OwncloudFileProvider | OwnCloud API |
| PluginFileProvider | Third-party plugins |

---

## Websites (data:websites)

### Model
label, url, description, imageUrl, faviconUrl, color

### Features
- URL validation and detection
- Open Graph metadata extraction (og:title, og:description, og:image)
- Favicon and theme color extraction
- Result caching
- Network-only (no results when offline)

---

## Locations (data:locations)

### Location Model
id, name, latitude, longitude, address, category, description, url, icon, distance, openingHours, uri (Maps intent)

### Providers
| Provider | Source |
|----------|--------|
| OsmLocationProvider | OpenStreetMaps via Overpass API |
| PluginLocationProvider | Third-party plugins |

- User location integration
- Configurable search radius
- Category filtering
- 30-second async timeout

---

## Wikipedia (data:wikipedia)

### Model
label (title), text (extract), imageUrl, sourceUrl, id (page ID), wikipediaUrl, sourceName

### Features
- Configurable Wikipedia instance (custom URL)
- 4-character minimum query length
- Network-only search

---

## Widgets (data:widgets)

### Widget Types
| Type | Config Class |
|------|-------------|
| WeatherWidget | WeatherWidgetConfig |
| MusicWidget | MusicWidgetConfig |
| CalendarWidget | CalendarWidgetConfig |
| AppsWidget (Favorites) | FavoritesWidgetConfig |
| AppWidget (external) | AppWidgetConfig |
| NotesWidget | NotesWidgetConfig |

- UUID-based identification
- Hierarchical (parent containers)
- JSON-serialized configs (kotlinx.serialization)

---

## Themes (data:themes)

### Sub-repositories
| Repository | Entity | Purpose |
|------------|--------|---------|
| ColorsRepository | ColorsEntity | Color palettes |
| ShapesRepository | ShapesEntity | Corner radius, shapes |
| TransparenciesRepository | TransparenciesEntity | Opacity settings |
| TypographyRepository | TypographyEntity | Font styles, sizes |

Full theme export/import as JSON.

---

## Notifications (data:notifications)

### Model
- key (unique identifier)
- Flow-based reactive updates
- `dismiss()` for cancellation

### Repository
- Maintains current notification list
- Responds to notification posted/removed events
- Integration with NotificationListenerService

---

## Plugins (data:plugins)

### Plugin Model
enabled, label, description, packageName, className, type, authority

### PluginType Enum
FileSearch, Weather, LocationSearch, Calendar, ContactSearch

### Repository
- CRUD operations
- Query by type, enabled state, package name
- Database-backed persistence

---

## I18N (data:i18n)

### IcuStringNormalizer
- ICU transliterators (API 29+) for script-to-Latin conversion
- Fallback: Apache Commons StringUtils.stripAccents
- Special mappings: æ→ae, œ→oe, ß→ss
- Per-locale configuration with ID-based caching
