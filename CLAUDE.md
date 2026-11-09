# GreywireAndroid - Claude Context

## What this project is

A native mobile app for **Greywire Studio**, Cherry Blossom Development's guitar-effects brand (the Linework pedals). It is one of two sibling apps:

| Project | Platform | Repo |
|---------|----------|------|
| **GreywireAndroid** | Android (Kotlin, Jetpack Compose, Retrofit) | https://github.com/Cherry-Blossom-Development/GreywireAndroid |
| **GreywireiPhone** | iOS (Swift, SwiftUI, XcodeGen) | https://github.com/Cherry-Blossom-Development/GreywireiPhone |

Both apps should offer the same features; build a feature in one, then match it in the other.

### How it relates to the other projects

- **GreywireStudio** (`../GreywireStudio`): the hardware side, meaning the pedal designs (KiCad, LTspice). The apps are the mobile companion to these products.
- **GreywireStudio-Website** (`../GreywireStudio-Website`): the Greywire web front end. **The app's look comes from this site** (see "Visual design" below).
- **Breakroom** (`../Breakroom`): the backend for these apps. They use the **same Express backend, MariaDB database and user accounts** as Prosaurus (`https://www.prosaurus.com/api`). **Any new backend endpoints or tables these apps need go into the Breakroom backend.** Don't create a separate backend. Greywire-specific routes live under `/api/greywire` (`backend/routes/greywire.js`).
- **Android** / **iPhone** (`../Android`, `../iPhone`): the Prosaurus mobile apps. **These are not the same apps.** The Greywire apps use the same tech stack and backend, and reuse patterns and code from them (auth, API client, environment switching, Socket.IO), but the interface and structure are very different. Some Prosaurus features will be carried over; many Greywire features won't exist in Prosaurus at all.

## Status

Foundation built 2026-11-09: login and a home screen.

- **Login**: works the same way as the Prosaurus app. `POST /api/auth/login`, the JWT goes into EncryptedSharedPreferences, and later calls send it as a Bearer header plus a `jwtToken` cookie. Refreshed tokens from the `X-New-Token` header are saved.
- **Home**: a header with the brand and Sign out, a hero section, the user's Greywire membership (`GET /api/greywire/membership`: member or not, plus claimed pedals), and cards for Flashover and Switchyard. The pedal text is static and copied from the website.
- If the saved token is rejected (401), the app signs out and goes back to Login.

**Future work is tracked in `TODO.md`**, including EULA acceptance, sign up, forgot password, pedal claiming, push notifications, release signing and E2E tests. Keep it up to date as items are done or found.

## Code layout (`app/src/main/java/com/cherryblossomdev/greywire/`)

Same structure as `../Android`:

| Path | What |
|------|------|
| `MainActivity.kt` | Edge-to-edge, `GreywireTheme`, `testTagsAsResourceId` root |
| `AppContainer.kt` | Manual DI: `TokenManager`, `AuthRepository`, `GreywireRepository` |
| `navigation/NavGraph.kt` | `Screen` routes (Login, Home) and `GreywireNavGraph` |
| `network/` | `RetrofitClient`, `ApiService` (auth), `GreywireApiService` (`/api/greywire`) |
| `data/` | `TokenManager`, `AuthRepository` (`AuthResult`), `GreywireRepository` |
| `ui/screens/` | `LoginScreen` + `LoginViewModel`, `HomeScreen` + `HomeViewModel` |
| `ui/components/GreywireComponents.kt` | Compose versions of the website's building blocks |
| `ui/theme/` | Palette, fonts, Material color scheme |

Identifiers: package and applicationId are `com.cherryblossomdev.greywire`; the app name is "Greywire".

## Visual design: match GreywireStudio-Website

The app should look like `../GreywireStudio-Website` (`css/site.css`). It's dark only, and Android 12 dynamic color is off.

- **Colors** (`ui/theme/Color.kt`) are the site's `:root` variables: bg `#141619`, surface `#1c1f23`, surface-2 `#24282d`, line `#33383e`, text `#e8e6e1`, muted `#9ba0a6`, copper `#d07a3e`, copper-soft `#e9a46f`, signal `#5fd38a`. If the site's palette changes, change these to match.
- **Fonts**: Space Grotesk for text and IBM Plex Mono for labels, both bundled in `res/font` under the OFL (license texts are in `licenses/`). There's no static SemiBold Space Grotesk, so 600 maps to Medium.
- **Components** (`GreywireComponents.kt`), with the site class each one mirrors: `GreywireHeader` (`.site-header`), `GreywireBrand` (`.brand`), `Eyebrow` (`.eyebrow`), `GreywireCard` (`.card`), `StatusPill` (`.status`), `GreywireButton` (`.button`), `GreywireGhostButton` (`.button.ghost`). Build new screens from these.
- **Logo**: `res/drawable/ic_greywire_logo.xml` is a vector port of the site's `img/logo.svg`. The launcher icon is adaptive (the wire and spark on surface-2), with the full logo as the fallback below API 26.

## Environments

| Env file | BASE_URL | Use |
|----------|----------|-----|
| `local` | `http://10.0.2.2:3001/` | Local backend on the dev machine (emulator loopback) |
| `dev-test` | `https://test.dev.prosaurus.com/` | EC2 dev test backend |
| `production` | `https://www.prosaurus.com/` | Production |

```powershell
.\switch-env.ps1 local    # writes environments/active.properties (gitignored)
```

Debug builds read `BASE_URL` from `active.properties` (default `http://10.0.2.2:3001/`). Release builds always use production. Plain HTTP is allowed only to `10.0.2.2`, and only in debug builds (`app/src/debug/res/xml/network_security_config.xml`).

To test locally, start the Breakroom test backend on port 3001 (from `../Breakroom`: load `.env.test`, then `docker compose -f docker-compose.test.yml up -d backend-test`). Its seeded users are `testuser` / `TestPass123` and `testadmin` / `TestPass123`.

Note: the backend only recognizes the `X-Client-Platform` values `web`, `android` and `ios`, so Greywire traffic is currently counted as `android` in analytics, together with Prosaurus. To tell them apart, add a value such as `greywire-android` to `backend/utilities/platform.js`.

## Build & install

The versions match `../Android`: AGP 9.3.0, Kotlin 2.4.10, Compose BOM 2026.06.01, compileSdk/targetSdk 37, minSdk 24.

```bash
cd "C:/Users/dalla/Repo/GreywireAndroid"
./gradlew.bat assembleDebug --rerun-tasks
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` (the SDK path) is gitignored, so copy it from `../Android` on a new machine. Release signing is used only when a `keystore.properties` exists; Greywire needs its own upload key before the first Play Store release (don't reuse Prosaurus's).

## Testing conventions

These are the same as `../Android`. Give interactive elements `Modifier.testTag("...")`; they show up to Appium as resource-ids with no package prefix. The current tags are `username-input`, `password-input`, `login-button`, `error-message`, `logout-button`, `home-title` and `membership-card`.
