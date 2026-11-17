# GreywireAndroid - To Do

Loose ends to address later. **Features are planned in `ROADMAP.md`**, not here. When something here gets done, remove it, and match the change in GreywireiPhone.

## Accounts and auth

- **EULA status on error.** If `/api/eula/status` fails, the Legal screen shows the EULA as "Accepted" (copied from iOS, so a user isn't shown a status they can't act on). That's misleading; consider an "Unknown" state in both apps.

## Release and infrastructure

- **Release signing key.** Greywire needs its own upload keystore (don't reuse `breakroom-release.jks`). Release builds currently go unsigned unless `keystore.properties` exists.
- **Play Store listing**: text is drafted in `PLAY_STORE_LISTING.md` (from iOS's `APP_STORE_LISTING.md`). Still needed: the 512px icon, feature graphic, screenshots and the Data Safety form.
- **Version bumps**: `versionCode` / `versionName` in `app/build.gradle.kts` (currently 1 / 0.1.0).

## Polish

- **Splash icon size.** On Android 12+ the splash icon nearly fills its circle. The adaptive foreground (`ic_launcher_foreground.xml`, scale 1.2) could be a bit smaller.
- **Sign out button.** The outlined "ghost" button in the header has a faint fill; the website's ghost button is fully transparent. Check `GreywireGhostButton` colors.
- **Tablet and landscape.** The login form caps its width at 420dp, but Home doesn't cap its width yet.

## Dev environment notes

- The `Pixel_7_API_34` emulator is nearly full (about 240 MB free) and refuses installs. Use `Pixel_9_API_35`, or free space or wipe data on the Pixel 7.
