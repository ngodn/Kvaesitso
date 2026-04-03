# Kvaesitso - UI Architecture Deep Dive

## Technology Stack
- **Jetpack Compose** (Material3 + MaterialExpressive theme)
- **Navigation3** (androidx.navigation3) for composable navigation
- **Koin** for dependency injection
- **Coil** for image loading
- **Haze** for blur effects
- **Accompanist** for system UI control

## Activities

| Activity | Purpose | Launch Mode |
|----------|---------|-------------|
| LauncherActivity | Main home screen | singleTask |
| AssistantActivity | Google Assistant replacement | singleTop |
| SettingsActivity | Settings/preferences | singleTop |
| AddItemActivity | Shortcut pinning confirmation | standard |
| BindAndConfigureAppWidgetActivity | Widget config | standard |
| ImportThemeActivity | Theme file import | standard |

### LauncherActivity
- Extends `SharedLauncherActivity(LauncherActivityMode.Launcher)`
- Registered as HOME intent handler (default launcher)
- Handles gesture transitions and home screen resumption
- GestureNavContract for enter animations

### AssistantActivity
- Handles `android.intent.action.ASSIST` intent
- Separate task affinity (`de.mm20.launcher2.assistant`)
- Acts as Google Assistant replacement

### SettingsActivity
- Deep link support: `https://kvaesitso.mm20.de/in-app`
- Handles `APPLICATION_PREFERENCES` intent
- 50+ nested settings screens

---

## Compose Hierarchy

```
LauncherTheme
  └── ProvideCompositionLocals
      ├── ProvideCurrentTime
      ├── ProvideSettings
      └── ProvideAppWidgetHost
          └── OverlayHost
              ├── NavBarEffects (charging animation)
              └── LauncherScaffold
                  ├── Home Component
                  │   ├── ClockHomeComponent (simple)
                  │   └── ClockAndWidgetsHomeComponent (with widgets)
                  ├── SearchComponent
                  ├── Secondary Components (gesture-triggered)
                  └── SearchBar
```

---

## LauncherScaffold — Core Page Management

**File:** `app/ui/.../launcher/scaffold/LauncherScaffold.kt` (66.6 KB)

The most complex file in the codebase. Manages all launcher pages, gestures, and transitions.

### ScaffoldConfiguration
Defines the complete launcher layout:
- `homeComponent` — primary screen (clock + optional widgets)
- `searchComponent` — search results
- Gesture bindings for all gesture types
- Search bar position/style/behavior
- Navigation and status bar configuration
- Background color and wallpaper blur

### Gesture System

| Gesture | Default Behavior |
|---------|-----------------|
| SwipeUp | (configurable) |
| SwipeDown | (configurable) |
| SwipeLeft | (configurable) |
| SwipeRight | (configurable) |
| DoubleTap | (configurable) |
| LongPress | (configurable) |
| HomeButton | (configurable) |
| TapSearchBar | Open search |

### Scaffold Components (gesture-triggered)

| Component | Purpose |
|-----------|---------|
| SearchComponent | Search results with keyboard |
| WidgetsComponent | Widget library |
| NotificationsComponent | Active notifications |
| QuickSettingsComponent | Quick settings panel |
| RecentsComponent | Recent apps |
| PowerMenuComponent | Power menu |
| ScreenOffComponent | Lock screen |
| FeedComponent | News feed |
| LaunchComponent | Quick launch app |
| DismissComponent | Back/dismiss |
| SecretComponent | Easter egg |

### Animation Types
- `Rubberband` — elastic swipe with snap-back
- `Push` — slide transition
- `ZoomIn` — zoom-in pop effect

### LauncherScaffoldState
Manages: current offset, Z-offset, active gesture/component, search bar focus/offset, scroll positions, status/nav bar scrim visibility, touch slop, velocity thresholds

---

## Home Screen Components

### ClockHomeComponent (simple mode)
- Displays clock widget only
- No background (draws wallpaper)
- Full screen height

### ClockAndWidgetsHomeComponent (widget mode)
- Clock widget + editable widget list
- Vertical scrollable column
- Edit mode for add/remove/reorder
- IME support for widget input (notes)
- Tracks scroll position for header visibility

---

## Search System UI

### Search Bar
**File:** `app/ui/component/SearchBar.kt`

#### Levels
| Level | State |
|-------|-------|
| Resting | Hidden, scroll at top |
| Active | Search open, no content below |
| Raised | Content below, elevated |

#### Styles
| Style | Appearance |
|-------|-----------|
| Hidden | Initially invisible, appears on focus |
| Transparent | Translucent overlay |
| Solid | Opaque background |

Features: dynamic icon/content color animation, elevation, menu/action buttons, filter badge, hidden items button

### Search ViewModel (`SearchVM.kt`, 18.5 KB)
- Query input management
- Search results aggregation
- Category filtering
- Hidden items tracking
- Result scoring and ranking
- Profile-based filtering
- Device pose detection

### Search Results Display (`SearchColumn.kt`, 17.3 KB)
Grid/list rendering of results by category:
- Apps (with profile tabs) + Shortcuts
- Contacts, Calendar events, Files
- Websites, Wikipedia articles
- Calculator results, Unit converter results
- Locations
- Favorites (with tag filtering)
- Filter bar, Hidden items section

---

## Theming System

### Multi-Level Theme Architecture

#### 1. Color Scheme
- Material3 color palette with system accent (API 31+)
- Wallpaper-based Material You dynamic colors (Material Color Utilities)
- HCT color space (Hue, Chroma, Tone)
- Supports 2021 and 2025 color specs
- Custom key colors: Primary, Secondary, Tertiary, Neutral, Neutral Variant, Error
- Dark/Light mode with high-contrast (Black and White) option

#### 2. Shapes
- Customizable corner radius
- Cut corner support
- Per-component shape definitions

#### 3. Typography
- Custom font families (device fonts, Google Sans Flex)
- Text styles with emphasis variants
- Size and weight customization

#### 4. Transparency
- Per-surface opacity values
- Configurable for surfaces, overlays, components

### Dynamic Color Pipeline
```
Wallpaper → Material Color Utilities → HCT conversion → TonalSpot variant
→ CorePalette → DynamicScheme → M3 ColorScheme → LauncherTheme
```

---

## Widget System

### Built-in Widgets
| Widget | Features |
|--------|----------|
| Clock | Multiple styles (analog, digital, binary), battery status, date, font/color |
| Calendar | 7-day events, calendar selection |
| Music | Now playing controls |
| Notes | Quick note taking |
| Weather | Current + forecast, compact mode, Breeze Weather integration |
| Favorites | Quick app/contact access |

### External AppWidgets
- Full Android AppWidget hosting
- Widget configuration UI
- Draggable repositioning
- Edit mode with add/remove

### Widget Column
- Drag-to-reorder in edit mode
- Per-widget background opacity
- Hierarchical (parent containers)

---

## Settings Navigation

Uses **androidx.navigation3** with custom backstack management.

### Main Categories (11)
| Category | Screen |
|----------|--------|
| Appearance | Theme, colors, shapes, typography |
| Home Screen | Clock, favorites, wallpaper blur |
| Icons | Icon packs, icon appearance |
| Search | Result filters, keyboard behavior |
| Gestures | Swipe/tap action binding |
| Integrations | Nextcloud, OwnCloud, OSM, Smartspacer |
| Plugins | Plugin management |
| Locale | Language, region |
| Backup | Backup/restore |
| Debug | Developer options, crash reporter, logs |
| About | App info, licenses, credits |

50+ nested settings screens total with type-safe route serialization.

---

## Bottom Sheets & Modals

| Sheet | Purpose |
|-------|---------|
| CustomizeSearchableSheet | Edit item visibility, icon, name, tags |
| EditFavoritesSheet | Manage pinned favorites, reorder |
| EditTagSheet | Create/edit/delete tags |
| WidgetPickerSheet | Browse and select system AppWidgets |
| ConfigureWidgetSheet | Android widget configuration |
| HiddenItemsSheet | Show/unhide filtered results |
| FailedGestureSheet | Explain gesture action failures |

State persistence via SavedStateRegistry.

---

## Composition Locals

| Local | Purpose |
|-------|---------|
| LocalCurrentTime | Real-time clock |
| LocalSettings | User preferences |
| LocalAppWidgetHost | Widget host service |
| LocalDarkTheme | Dark mode state |
| LocalWallpaperColors | Extracted wallpaper colors |
| LocalSnackbarHostState | Toast/snackbar display |
| LocalBottomSheetManager | Sheet state management |
| LocalBackStack | Navigation history |
| LocalGridSettings | Grid layout configuration |
| LocalWindowSize | Screen dimensions |
| LocalTransparencyScheme | Opacity settings |
| LocalEnterHomeTransitionManager | App launch animations |

---

## Common UI Components

### Core
- `SearchBar` — main search input (48dp height)
- `LauncherCard` — card surface with opacity control
- `ShapedLauncherIcon` (21.1 KB) — app icon rendering with custom shapes
- `Toolbar` — top app bar with navigation
- `BottomSheet` / `DismissableBottomSheet` — sheet components

### Specialized
- **Color Picker** — Material color selection
- **Emoji Picker** — emoji selection (androidx.emojipicker)
- **Drag & Drop** — drag gesture handling
- **Markdown** — markdown rendering (JetBrains Markdown)
- **Preferences** — preference UI (categories, switches, seekbars)
- **Weather** — weather display component

### Animation Utilities
- `Alignment.kt` — animated alignment transitions
- `TextStyle.kt` — animated text style interpolation
- `TextUnit.kt` — animated dimension changes
