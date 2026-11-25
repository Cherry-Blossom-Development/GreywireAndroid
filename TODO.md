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

- **Heading in the bio editor.** After pressing Enter at the end of a heading, the new line stays a heading, and the H button can't turn it off until something is typed (a `compose-rich-editor` quirk; the web's Tiptap starts a normal paragraph). Type, then tap H. A fix could watch for Enter after a heading and reset the new paragraph.
- **Unsaved changes.** Back on the profile edit screens (Edit profile, music bio, instrument, band) discards changes without asking. Consider a "Discard changes?" confirmation.

## Testing

- **Popups need `Modifier.popupTestTags()`.** Dialogs and dropdown menus are separate windows that don't inherit `testTagsAsResourceId` from `MainActivity`. Use the modifier on any new one (fixed 2026-11-25 for the existing ones).

## Dev environment notes

- The `Pixel_7_API_34` emulator is nearly full (about 240 MB free) and refuses installs. Use `Pixel_9_API_35`, or free space or wipe data on the Pixel 7.
