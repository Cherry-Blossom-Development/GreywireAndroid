# GreywireAndroid

Native Android app for Greywire Studio.

## What this project is

A native mobile app for **Greywire Studio**, Cherry Blossom Development's guitar-effects brand (the Linework pedals). It is one of two sibling apps:

| Project | Platform | Repo |
|---------|----------|------|
| **GreywireAndroid** | Android (Kotlin, Jetpack Compose, Retrofit) | https://github.com/Cherry-Blossom-Development/GreywireAndroid |
| **GreywireiPhone** | iOS (Swift, SwiftUI, XcodeGen) | https://github.com/Cherry-Blossom-Development/GreywireiPhone |

Both apps should offer the same features; build a feature in one, then match it in the other.

### How it relates to the other projects

- **GreywireStudio** (`../GreywireStudio`): the hardware side, meaning the pedal designs (KiCad, LTspice). The apps are the mobile companion to these products.
- **GreywireStudio-Website** (`../GreywireStudio-Website`): the Greywire web front end. Like these apps, it talks to the Breakroom backend (for example, the surveys post to `/api/surveys/<name>`).
- **Breakroom** (`../Breakroom`): the backend for these apps. They use the **same Express backend, MariaDB database and user accounts** as Prosaurus (`https://www.prosaurus.com/api`). **Any new backend endpoints or tables these apps need go into the Breakroom backend.** Don't create a separate backend.
- **Android** / **iPhone** (`../Android`, `../iPhone`): the Prosaurus mobile apps. **These are not the same apps.** The Greywire apps use the same tech stack and backend, and can reuse patterns and code from them (auth, API client, environment switching, Socket.IO), but the interface and structure are very different. Some Prosaurus features will be carried over; many Greywire features won't exist in Prosaurus at all.

## Status

Foundation in place (2026-11-09): sign-in using the same Prosaurus/Breakroom account, and a home screen showing the user's Greywire membership and the Linework pedals. Visually it matches the Greywire Studio website. Build and run notes are in `CLAUDE.md`.
