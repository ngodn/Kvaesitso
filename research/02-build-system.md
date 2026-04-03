# Kvaesitso - Build System & Dependencies

## Build Tools

| Tool | Version |
|------|---------|
| Gradle | 8.1.2 |
| Android Gradle Plugin | 9.1.0 |
| KSP (Kotlin Symbol Processing) | 2.3.4 |
| Dokka (API docs) | 2.2.0 |

## Gradle Configuration (`gradle.properties`)

- JVM max heap: 4096MB
- `android.useAndroidX=true`
- `org.gradle.daemon=true`
- `org.gradle.parallel=true`
- `org.gradle.caching=true`
- `org.gradle.configuration-cache=true`
- `android.nonTransitiveRClass=false`
- File encoding: UTF-8

## SDK Versions

| Setting | Value |
|---------|-------|
| minSdk | 26 (Android 8.0 Oreo) |
| compileSdk | 36 (Android 15) |
| targetSdk | 36 (Android 15) |
| Java Target | JVM 1.8 |

## Key Dependencies (from `gradle/libs.versions.toml`)

### Language & Compiler

| Library | Version |
|---------|---------|
| Kotlin | 2.3.20 |
| Kotlinx Coroutines | 1.10.2 |
| Kotlinx Serialization | 1.10.0 |
| Kotlinx Immutable Collections | 0.4.0 |

### Jetpack Compose & UI

| Library | Version |
|---------|---------|
| Compose Runtime | 1.11.0-beta02 |
| Compose Material3 | 1.5.0-alpha16 |
| Compose Material3 Adaptive | 1.3.0-alpha09 |
| ConstraintLayout Compose | 1.1.1 |
| Accompanist | 0.36.0 |
| Haze (blur effects) | 1.7.2 |
| EmojiPicker | 1.6.0 |

### AndroidX Core

| Library | Version |
|---------|---------|
| Core KTX | 1.18.0 |
| AppCompat | 1.7.1 |
| Activity | 1.13.0 |
| Lifecycle | 2.10.0 |
| Navigation3 | 1.0.1 |
| WorkManager | 2.11.2 |
| Browser | 1.10.0 |
| Palette | 1.0.0 |
| DataStore | 1.2.1 |
| Security Crypto | 1.1.0 |
| EXIF Interface | 1.4.2 |

### Data & Networking

| Library | Version | Purpose |
|---------|---------|---------|
| Room | 2.8.4 | SQLite ORM |
| Ktor | 3.4.2 | HTTP client (OkHttp backend) |
| Coil | 2.7.0 | Image loading + SVG |
| Jackson Core | 2.20.1 | JSON/YAML parsing |
| JSoup | 1.22.1 | HTML parsing |

### Dependency Injection

| Library | Version |
|---------|---------|
| Koin | 4.2.0 |

### Utilities

| Library | Version | Purpose |
|---------|---------|---------|
| mXparser | 4.4.2 | Calculator (GPL-compatible, not updated to 5.x) |
| Suncalc | 3.11 | Sun position calculations |
| Mustache | 0.9.14 | Template engine (address formatting) |
| JetBrains Markdown | 0.7.3 | Markdown rendering |
| Commons Text | 1.15.0 | String utilities |
| String Similarity | 0.1.0 | Search ranking |
| OSM Opening Hours | 0.4.0 | Location opening hours |
| Smartspacer SDK | 1.1.2 | Smart widget integration |

### Testing

| Library | Version |
|---------|---------|
| JUnit | 4.13.2 |
| JUnit Android | 1.2.1 |
| Espresso Core | 3.6.1 |

### Debug (commented out)

| Library | Version |
|---------|---------|
| LeakCanary | 2.10 |

## Dependency Bundles

- **kotlin** — stdlib + coroutines + collections + serialization
- **androidx-lifecycle** — all lifecycle components
- **ktor** — full client stack (core + content negotiation + serialization + OkHttp)
- **tests** — JUnit

## Build Configuration

### Signing
- Debug: default debug keystore
- Nightly: GitHub Actions signing (env vars: `RUNNER_TEMP`, `KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`)

### Minification
- **Disabled** for all build types (`isMinifyEnabled = false`)
- Resource shrinking disabled
- ProGuard rules exist but are essentially boilerplate

### Lint
- `abortOnError = false`

### Compose Compiler Experimental APIs Enabled (in app/ui)
- ExperimentalComposeUiApi, ExperimentalFoundationApi, ExperimentalTextApi
- ExperimentalUnitApi, ExperimentalLayoutApi
- ExperimentalMaterialApi, ExperimentalMaterial3Api, ExperimentalMaterial3ExpressiveApi
- ExperimentalAnimationGraphicsApi, ExperimentalAnimationApi
- ExperimentalPagerApi (Accompanist), ExperimentalSharedTransitionApi

### Locale Generation
- Auto-generates locale configuration from resource directories

## CI/CD (GitHub Actions)

| Workflow | Purpose |
|----------|---------|
| `build-nightly.yml` | Nightly builds |
| `deploy-docs.yml` | Deploy VitePress docs site |
| `trigger-fdroid-repo-rebuild.yml` | Trigger F-Droid repo rebuild |

## Documentation Site
- Built with **VitePress**
- Deployed via GitHub Actions
- Source in `docs/` directory
