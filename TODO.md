# GreywireAndroid - To Do

Things to address later, gathered while building the foundation (login and home, 2026-11-09). When something here gets done, remove it, and match the change in GreywireiPhone.

## Accounts and auth

- **EULA acceptance.** The Prosaurus app sends new users to an EULA screen after login (`/api/eula/...`, and the backend creates a `eula_required` notification at login). Greywire skips this entirely. Decide whether Greywire users must accept the same EULA, or a Greywire-specific one, and add the screen.
- **Sign up.** There's no way to create an account in the app yet. Prosaurus hashes the password client-side for signup (`AuthRepository.signup` in `../Android`); reuse that.
- **Forgot password.** Not built. The Prosaurus app has `ForgotPasswordScreen` using `/api/auth/forgot-password`.
- **Guest accounts.** `/api/auth/me` returns `isGuest`, and guests can never be Greywire members. The app doesn't handle guests specially yet.

## Greywire features

- **Claim a pedal.** `POST /api/greywire/claim { serial }` already exists in the backend and makes the user a member. The home screen's "Not a member yet" card should lead to a claim flow (serial entry, and maybe a QR scan later). The backend rate-limits failed claims (10 per hour), so show its error messages.
- **Pedal catalog from the backend.** The Flashover and Switchyard cards on Home are hard-coded text copied from the website. Move them to the backend once there's a pedal catalog, so the app, site and backend don't drift apart.
- **Pedal illustrations.** The website has an SVG for each pedal (`../GreywireStudio-Website/img/*.svg`). Port them to vector drawables for the pedal cards.
- **The real app features**: setlists, sounds, and talking to Switchyard (the website describes the phone setting up setlists and sounds that Switchyard stores). These are the core of the app and still need design.
- **Community.** The website links to `community.greywirestudio.com`. Decide which community features (chat and so on) come into the app. Note that requests made to the Greywire site's host are checked by `backend/middleware/greywireAccess.js` and need membership; requests to `www.prosaurus.com` aren't.

## Backend

- **Analytics platform value.** The backend accepts only `web`, `android` and `ios` in `X-Client-Platform` (`backend/utilities/platform.js`), so Greywire traffic is counted as `android` together with Prosaurus. Add `greywire-android` / `greywire-ios` there, then send it from `RetrofitClient`.

## Release and infrastructure

- **Release signing key.** Greywire needs its own upload keystore (don't reuse `breakroom-release.jks`). Release builds currently go unsigned unless `keystore.properties` exists.
- **Push notifications.** Firebase isn't set up (no `google-services.json`, no FCM token registration). Prosaurus registers FCM tokens on login and removes them on logout; do the same when notifications are needed.
- **Play Store listing**: app name, icon assets, screenshots, privacy policy and Data Safety form.
- **Version bumps**: `versionCode` / `versionName` in `app/build.gradle.kts` (currently 1 / 0.1.0).

## Testing

- **E2E tests in BreakTest.** No Appium tests cover Greywire yet. The test tags are already in place (see `CLAUDE.md`). Remember `mobile: clearApp` with appId `com.cherryblossomdev.greywire` between tests.
- **A Greywire member in the test seed data.** `breakroom_test` has no user who is a Greywire member or holds a claimed pedal, so the home screen's "Member" state has only been checked in code, not on screen. Add a seeded member (a grant or a claimed pedal) to `BreakTest/database/setup.ts`.

## Polish

- **Splash icon size.** On Android 12+ the splash icon nearly fills its circle. The adaptive foreground (`ic_launcher_foreground.xml`, scale 1.2) could be a bit smaller.
- **Sign out button.** The outlined "ghost" button in the header has a faint fill; the website's ghost button is fully transparent. Check `GreywireGhostButton` colors.
- **Tablet and landscape.** The login form caps its width at 420dp, but Home doesn't cap its width yet.

## Dev environment notes

- The `Pixel_7_API_34` emulator is nearly full (about 240 MB free) and refuses installs. Use `Pixel_9_API_35`, or free space or wipe data on the Pixel 7.
