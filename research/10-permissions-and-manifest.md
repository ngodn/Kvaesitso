# Kvaesitso - Permissions & Manifest Reference

## Android Manifest Configuration

### Application Attributes
| Attribute | Value |
|-----------|-------|
| Application Class | `de.mm20.launcher2.LauncherApplication` |
| Theme | `@style/LauncherTheme` |
| Backup | Enabled (full backup content) |
| Cross-profile | Enabled |
| Resizable | true |
| RTL Support | true |
| Cleartext Traffic | Allowed |
| Legacy External Storage | Requested |

## Permissions (31 total)

### System
| Permission | Purpose |
|------------|---------|
| SET_ALARM | Set alarms via quick actions |
| com.android.alarm.permission.SET_ALARM | Alarm compatibility |
| GET_ACCOUNTS | Cloud account discovery |
| VIBRATE | Haptic feedback |
| EXPAND_STATUS_BAR | Notification drawer gesture |
| INTERACT_ACROSS_PROFILES | Work profile support |
| REQUEST_INSTALL_PACKAGES | App updates |
| REQUEST_DELETE_PACKAGES | App uninstall |
| QUERY_ALL_PACKAGES | Full app list access |
| CALL_PHONE | Direct call from search |

### Location
| Permission | Purpose |
|------------|---------|
| ACCESS_COARSE_LOCATION | Approximate location for weather/places |
| ACCESS_FINE_LOCATION | Precise location for places search |

### Network
| Permission | Purpose |
|------------|---------|
| INTERNET | Web searches, cloud services, weather |
| ACCESS_NETWORK_STATE | Connectivity checks |

### Calendar
| Permission | Purpose |
|------------|---------|
| READ_CALENDAR | Calendar event search |
| WRITE_CALENDAR | Schedule events via quick actions |

### Contacts
| Permission | Purpose |
|------------|---------|
| READ_CONTACTS | Contact search |

### Storage
| Permission | Purpose |
|------------|---------|
| READ_EXTERNAL_STORAGE | File search |
| WRITE_EXTERNAL_STORAGE | Legacy file access |
| MANAGE_EXTERNAL_STORAGE | Android 11+ file access |
| ACCESS_MEDIA_LOCATION | Media file location metadata |

### Third-Party
| Permission | Purpose |
|------------|---------|
| org.tasks.permission.READ_TASKS | Tasks.org integration |
| com.kieronquinn.app.smartspacer.permission.ACCESS_SMARTSPACER | Smartspacer widget |

### Hardware Features
| Feature | Required |
|---------|----------|
| android.hardware.telephony | No (optional) |

## Content Providers

### GenericFileProvider
- Authority: `${applicationId}.fileprovider`
- Exported: false
- Grant URI Permissions: true
- File paths defined in `@xml/provider_paths`

## Intent Filters

### LauncherActivity
| Action | Category | Purpose |
|--------|----------|---------|
| MAIN | LAUNCHER | App icon in drawer |
| MAIN | HOME, DEFAULT | Default launcher |

### AssistantActivity
| Action | Purpose |
|--------|---------|
| android.intent.action.ASSIST | Google Assistant replacement |

### SettingsActivity
| Action | Purpose |
|--------|---------|
| APPLICATION_PREFERENCES | App settings shortcut |
| VIEW (kvaesitso.mm20.de/in-app) | Deep link (autoVerify) |

### AddItemActivity
| Action | Purpose |
|--------|---------|
| CONFIRM_PIN_SHORTCUT | Widget/shortcut pinning |

### ImportThemeActivity
| MIME Type | Purpose |
|-----------|---------|
| application/vnd.de.mm20.launcher2.theme | Theme file import |

## Services

### LauncherAccessibilityService
- Used for global actions (lock screen, power menu, recents)
- Fallback when reflection-based status bar expansion fails

### NotificationListenerService
- Tracks active notifications for badge system
- Permission required: BIND_NOTIFICATION_LISTENER_SERVICE

## Metadata
- WebView metrics opt-out enabled
- Shortcuts defined in `@xml/shortcuts`
- Smartspacer SDK override library configuration
