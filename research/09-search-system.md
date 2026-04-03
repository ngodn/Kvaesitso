# Kvaesitso - Search System Deep Dive

## Overview

Search is the defining feature of Kvaesitso. The system provides unified search across 10+ data types with parallel execution, scoring, ranking, filtering, and persistence.

## Architecture

```
User Query
    │
    ▼
SearchService.search(query, filters)
    │
    ├──► AppRepository.search()          ─── immediate
    ├──► AppShortcutRepository.search()  ─── immediate
    ├──► ContactRepository.search()      ─── immediate
    ├──► CalendarRepository.search()     ─── immediate
    ├──► FileRepository.search()         ─── immediate
    ├──► SearchActionRepository.get()    ─── immediate
    ├──► CalculatorRepository.search()   ─── immediate
    ├──► UnitConverterRepository.search()─── immediate
    ├──► WebsiteRepository.search()      ─── immediate
    ├──► LocationRepository.search()     ─── delayed +250ms
    └──► WikipediaRepository.search()    ─── delayed +750ms
    │
    ▼
SearchResults (Flow, progressively updated)
    │
    ▼
SearchVM (scoring, filtering, ranking)
    │
    ▼
SearchColumn (UI rendering)
```

## Search Flow

### 1. Query Input
- User types in SearchBar
- Query debounced and passed to SearchService
- Empty query returns favorites/recent items

### 2. Parallel Execution
All providers searched concurrently via coroutines:
- Fast local sources (apps, contacts, calendar) return immediately
- Network sources delayed to avoid unnecessary requests
- Wikipedia delayed +750ms, Locations +250ms
- Each provider runs in `supervisorScope` — failures isolated

### 3. Multi-Provider per Type
Each data type can have multiple providers:

| Type | Providers |
|------|-----------|
| Calendar | Android native, Tasks.org, Plugin |
| Contacts | Android native, Plugin |
| Files | Local, Nextcloud, OwnCloud, Plugin |
| Locations | OpenStreetMaps, Plugin |
| Weather | MET Norway, OpenWeatherMap, Plugin |

### 4. Scoring & Ranking

#### ResultScore
Based on Jaro-Winkler similarity algorithm:

```
score = (similarity + isPrefix * 0.2 + isSubstring * 0.8) * (isPrimary ? 1.0 : 0.8)
```

| Factor | Weight | Description |
|--------|--------|-------------|
| Similarity | 0-1.0 | Jaro-Winkler string similarity |
| isPrefix | +0.2 | Query is a prefix of the result |
| isSubstring | +0.8 | Query is a substring of the result |
| isPrimary | ×1.0/0.8 | Primary field (name) vs secondary (description) |

#### Ranking Order
Within each category, results sorted by:
1. ResultScore (descending)
2. Launch count (for apps/favorites)
3. Alphabetical (tiebreaker)

### 5. Filtering

#### Filter Types
| Filter | Targets |
|--------|---------|
| Apps | Applications + AppShortcuts |
| Contacts | Contact results |
| Calendar | Calendar events |
| Files | Local + cloud files |
| Websites | Web results |
| Wikipedia | Article results |
| Locations | Place results |
| Calculator | Math results |
| UnitConverter | Conversion results |

- Default filters configurable in settings
- Filter bar shown in search UI
- Hidden items accessible via separate section

### 6. Custom Attributes Integration
Search respects per-item customizations:
- Custom labels matched alongside original names
- Tags used for category filtering
- Custom icons displayed in results
- Visibility overrides (show/hide items)

## Searchable Type Hierarchy

```
Searchable (marker interface)
│
├── SavableSearchable (extends Comparable)
│   ├── Application
│   │   └── LauncherApp (componentName, label, user, profile)
│   ├── AppShortcut
│   ├── Contact
│   │   ├── AndroidContact (native)
│   │   └── PluginContact
│   ├── CalendarEvent
│   │   ├── AndroidCalendarEvent
│   │   ├── TasksCalendarEvent
│   │   └── PluginCalendarEvent
│   ├── File
│   │   ├── LocalFile
│   │   ├── NextcloudFile
│   │   ├── OwncloudFile
│   │   └── PluginFile
│   ├── Location
│   │   ├── OsmLocation
│   │   └── PluginLocation
│   ├── Website
│   └── Article (Wikipedia)
│
└── SearchAction
    ├── CallAction
    ├── MessageAction
    ├── EmailAction
    ├── ScheduleEventAction
    ├── SetAlarmAction
    ├── TimerAction
    ├── WebsearchAction
    ├── ShareAction
    ├── CustomIntentAction
    └── CustomWebsearchAction
```

## Persistence Layer

### SavedSearchableEntity (Database)
| Column | Type | Purpose |
|--------|------|---------|
| key | String (PK) | Unique searchable identifier |
| type | String | Searchable type discriminator |
| serializedSearchable | String | JSON-serialized searchable |
| launchCount | Int | Usage frequency |
| pinPosition | Int | 0=unpinned, 1=auto, 2+=manual |
| visibility | Int | -1=hidden, 0=default, 1=visible, 2=more |
| weight | Double | Ranking weight |

### Serialization
- `SearchableSerializer.serialize(searchable)` → JSON String
- `SearchableDeserializer.deserialize(type, json)` → Searchable object
- Type-aware factory pattern
- Legacy format migration support

## String Normalization (data:i18n)

For consistent cross-script search matching:

### IcuStringNormalizer
- ICU transliterators (API 29+) for script-to-Latin conversion
- Per-locale configuration (e.g., Chinese pinyin, Japanese romaji)
- Fallback: Apache Commons `StringUtils.stripAccents()`
- Special mappings: æ→ae, œ→oe, ß→ss
- Cached per locale for performance

This ensures searching "cafe" matches "café", "Munchen" matches "München", etc.

## Built-in Tools

### Calculator
- Powered by mXparser expression library
- Supports: arithmetic, functions, hex (0x), binary (0b), octal (0)
- Output in decimal, binary, hex, octal
- Real-time evaluation as user types
- Configurable enable/disable

### Unit Converter
- Parses natural language: `"100 kg to lb"`, `"50 miles in km"`
- Types: Mass, Length, Area, Temperature, Time, Velocity, Volume, Currency
- Currency conversion uses live exchange rates
- Result shown inline in search results

### Quick Actions
8 built-in actions triggered by search content:
1. **Call** — phone number detected
2. **Message** — phone number detected
3. **Email** — email address detected
4. **Add Contact** — phone/email detected
5. **Set Alarm** — time pattern detected
6. **Set Timer** — duration pattern detected
7. **Schedule Event** — event pattern detected
8. **Open URL** — URL detected

## Profile-Aware Search

Search results filtered and grouped by user profile:
- **Personal** — main user apps/data
- **Work** — managed work profile apps
- **Private** — Android Private Space apps

Apps grouped with profile tabs in search results. Profile badges indicate item origin.

## Performance Optimizations

1. **Staggered delays** — expensive sources delayed to avoid unnecessary work
2. **supervisorScope** — provider failures don't crash others
3. **Flow-based** — progressive result delivery (fast local → slow network)
4. **LRU icon cache** — 200-item cache for icon resolution
5. **String normalization caching** — per-locale ICU transliterator cache
6. **Database indexing** — Room indices on key, type, pinPosition columns
