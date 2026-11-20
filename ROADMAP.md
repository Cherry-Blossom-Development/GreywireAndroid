# Greywire Roadmap

The feature plan for the Greywire Studio apps, agreed 2026-11-17. The work spans four projects, so this file tracks all of them in one place:

| Project | What it is | Where the work happens |
|---------|-----------|------------------------|
| **Backend** | `Breakroom/backend` (Express, MariaDB) | Either computer |
| **Web** | `Breakroom/frontend`, Greywire route table (community.greywirestudio.com) | Either computer |
| **Android** | `GreywireAndroid` (this repo) | Windows computer |
| **iPhone** | `GreywireiPhone` | Mac |

The Greywire apps include **only the music features** of Prosaurus, plus features that exist only in Greywire. Build a step on Android, then match it on iPhone. If the web community lacks the feature, add it there too.

Small loose ends (polish, dev notes) stay in `TODO.md`. Update this file as each step is done, so it stays the source of truth on both computers.

**Status key:** `done` · `wip` · `todo` · `n/a` (nothing needed in that project) · `?` (not yet checked)

## Where we left off (updated 2026-11-20)

**Done on Android:** 0.1 `X-Site: greywire`, 1.1 Sign up, 1.3 Forgot password, 1.4 Claim a pedal. All pushed and tested on the emulator against the local test backend.

**In progress: 1.6 Open Greywire to non-members (Breakroom).** Five commits in Breakroom, **pushed but not deployed** (Breakroom has no CI, so pushing doesn't deploy):
- `d992f67` migration **097** (`users.greywire_joined_at`) and helpers `greywireUserSql` / `isGreywireUser` / `ensureGreywireUser` in `backend/utilities/greywire.js`
- `59766bc` `greywireAccess.js` and the socket check let in any non-guest account and record it as a Greywire user (first time: joins the Greywire default rooms)
- `bb99148` friends, search, profiles and band invites on Greywire show Greywire users (used Greywire, or a member)
- `990ce05` web: route guard, layout and services block only guests; sign up goes to chat; band invites don't need a pedal; App.vue watches only the username (so claiming doesn't start services twice)
- `df6d4dd` web wording: Greywire home, claim page, band invite, email-verified message, Claim link on Billing & Membership

Tested against the test backend (`testadmin`, a Prosaurus-only non-member, can use Greywire and was recorded; a Prosaurus-only account is hidden on Greywire). Migration 097 is applied to **`breakroom_test` only**.

**Waiting on**Legal text (1.12) done** on 2026-11-20 (user said go ahead with the draft and will review it later): Breakroom `5e20ce0` (web, not deployed) and Android `3516bce`. Effective date November 20, 2026 on both Greywire documents. iPhone still to match. Not covered (separate decision): subscription renewal and cancellation terms; neither EULA has them.

**Deploy check (2026-11-20):** the 1.6 changes don't affect Prosaurus. Every backend change is behind `req.site === 'greywire'` (or the socket's site check), and every web change is behind `isGreywire`. On Prosaurus `canUseServices()` and the layout condition work out the same as before. **Order matters:** apply migration 097 to production `breakroom` *before* the new backend starts, or Greywire requests will fail on the missing `greywire_joined_at` column (Prosaurus would still work). Deploying `main` also ships anything else on `main` that isn't deployed yet.

**Waiting on:** the deploy (migration 097, then backend, then frontend).

** Phase 2 on Android: 2.1 navigation shell, then 2.2 Sessions.

**Known test environment problems (fix before testing Sessions):** `/api/sessions` returns 500 in the test backend, on Prosaurus too: `breakroom_test` lacks migration 096 (`sessions.duration_ms`), and the test image lacks the `@tus/server` package.

**Waiting for iPhone (on the Mac):** 0.1, 1.1, 1.3 and 1.4 are done on Android, not yet on iPhone.

**Test environment state:**
- `breakroom_test`: migration 097 applied. `testuser` holds test pedal `00000000000000A1` (member); `testadmin` is now a Greywire user (non-member); test account `gwtest073722` exists. `npx ts-node database/setup.ts` in BreakTest resets everything (it syncs the schema from production, so 097 would be lost until production has it).
- To test again: start Docker Desktop, then from `../Breakroom` load `.env.test` and run `docker compose -f docker-compose.test.yml up -d --build backend-test`; start the `Pixel_9_API_35` emulator.

**Working style agreed:** small commits, one change each; push after each finished step update this file as steps finish.

## Decisions

- **2026-11-17: Greywire is for the whole band, not only pedal owners.** Buying a pedal and claiming its serial gives a free membership (no limits). Musicians who don't need a pedal (drummers, singers...) can still use Greywire the way people use Prosaurus: limited free use, then a **Greywire Studio subscription** with exactly the same limits and price as the Prosaurus subscription. This replaces the earlier "members only" rule, so the backend's `greywireAccess` middleware, the web route guard and the EULA ("The community is open to members only") all need changing (steps 1.6 to 1.11).
- **2026-11-17: One subscription for both sites.** Subscribing to Prosaurus or to Greywire Studio is the same subscription and covers both.
- **2026-11-17: Lyrics are shared between Prosaurus and Greywire**, like profiles and friends. `routes/lyrics.js` already works that way.
- **2026-11-18: Greywire users.** Friends, search, profiles and band invites on Greywire show accounts that have used Greywire (signed up, signed in or made a request there) or are members, not every account. Recorded in `users.greywire_joined_at` (migration 097).
- **2026-11-18: Email links open the website for now.** The verification and password reset emails link to the website, not the app. Deep links can come later.
- **2026-11-17: Switchyard connectivity (4.6) waits** until the hardware is further along.

## Phase 0: Requests count as Greywire

The backend serves both sites and works out the site from the `X-Site` header (native apps) or the host (browsers); see `backend/utilities/site.js`. Without `X-Site: greywire`, everything the app creates (rooms, sessions, bands) goes to Prosaurus, and friends lists aren't limited to members. With it, `middleware/greywireAccess.js` requires membership on everything except `/api/auth/`, `/api/greywire/`, `/api/eula/`, and public share links.

| Step | Backend | Web | Android | iPhone |
|------|---------|-----|---------|--------|
| 0.1 Send `X-Site: greywire` on every request | done | n/a (host) | done | ? |
| 0.2 Handle 403 `GREYWIRE_MEMBERSHIP_REQUIRED` (send to claim); goes away with 1.6 | done | done | n/a | n/a |
| 0.3 `greywire-android` / `greywire-ios` platform values for analytics | todo | n/a | todo | todo |

## Phase 1: Account basics (needed before launch)

| Step | Backend | Web | Android | iPhone |
|------|---------|-----|---------|--------|
| 1.1 Sign up (client-side password hashing, like Prosaurus) | done | done | done | todo |
| 1.2 Email verification: the email's link opens the website (no deep links for now) | done | done | n/a | n/a |
| 1.3 Forgot password (`/api/auth/forgot-password`; the reset link opens the website) | done | done | done | todo |
| 1.4 Claim a pedal: serial entry (`POST /api/greywire/claim`, 10 failed/hour limit) | done | done | done | todo |
| 1.5 Claim a pedal: QR scan | ? | n/a | todo | todo |
| 1.6 Open Greywire to non-members: remove members-only gating (`greywireAccess.js`, web `requiresMember`) | wip (committed, not deployed) | wip (committed, not deployed) | n/a | n/a |
| 1.7 Guest accounts (`isGuest`, can never be members) | done | ? | todo | todo |
| 1.8 One subscription for both sites (Prosaurus and Greywire Studio) | done | ? | n/a | n/a |
| 1.9 Free-tier limits on Greywire: members unlimited, others like Prosaurus (sessions 3+3, bands 1) | done | ? | n/a | n/a |
| 1.10 Paywall: handle 402 `requiresSubscription`, show plan status (`/api/billing/plan`) | done | ? | todo | todo |
| 1.11 In-app purchase (Google Play Billing / StoreKit) and backend verification for the Greywire app IDs | todo | n/a | todo | todo |
| 1.12 EULA / Privacy Policy: no longer members-only; add the subscription terms | n/a | done (not deployed) | done | todo |

Notes:
- **1.6**: until this is done on the backend, a non-member using the app gets 403 `GREYWIRE_MEMBERSHIP_REQUIRED` on everything outside `/api/auth`, `/api/greywire` and `/api/eula`.
- **1.9**: already in place: `checkFreeTierLimit` (`utilities/sessionRecords.js`) and the band limit in `routes/bands.js` let members past the limit on the Greywire site, and everyone else needs `isSubscribed`.
- **1.11**: `/api/subscriptions/google/verify` checks purchases against one package name (`GOOGLE_PLAY_PACKAGE_NAME`, Prosaurus's). The Greywire app (`com.cherryblossomdev.greywire`) needs its own; Apple verification likewise needs the Greywire bundle ID.

**1.8 (decided 2026-11-17):** one subscription covers both sites. `user_subscriptions` has no site column, so this is already how the backend works: subscribing in either app, or on either website, lifts the limits on both. The web Greywire billing page still needs checking.

## Phase 2: Music features the web community already has

Web routes: `/chat`, `/sessions`, `/friends`, `/user/:handle`, `/profile`. The Prosaurus Android app (`../Android`) has screens to adapt for each.

| Step | Backend | Web | Android | iPhone |
|------|---------|-----|---------|--------|
| 2.1 App navigation shell (somewhere to put the features below) | n/a | n/a | todo | todo |
| 2.2 Sessions: list, play/stream, upload, delete | done | done | todo | todo |
| 2.3 Sessions: record in-app | n/a | ? | todo | todo |
| 2.4 Sessions: comments, ratings, instrument tags, share links | done | done | todo | todo |
| 2.5 Bands: create, invite (link and email), members, band sessions | done | done | todo | todo |
| 2.6 Band setlists (`/bands/:id/setlists`) | done | ? | todo | todo |
| 2.7 Chat: rooms and direct messages (Socket.IO) | done | done | todo | todo |
| 2.8 Friends: list, requests, search (members only) | done | done | todo | todo |
| 2.9 Profile: own profile, settings, other members' profiles | done | done | todo | todo |
| 2.10 Musician profile (bio, "musician only"; Greywire only) | done | done | todo | todo |

## Phase 3: Prosaurus music features brought into Greywire

Decided 2026-11-17: Lyric Lab and band pages belong in Greywire. Neither is on the Greywire web route table yet.

| Step | Backend | Web | Android | iPhone |
|------|---------|-----|---------|--------|
| 3.1 Lyric Lab: songs, lyrics, collaborators (shared with Prosaurus) | done | todo | todo | todo |
| 3.2 Band pages: public page per band, songs, members, instruments | ? | todo | todo | todo |
| 3.3 Practice suggestions (`/sessions/practice-suggestions`) | done | ? | todo | todo |
| 3.4 EULA / Privacy Policy wording, if lyrics or band pages need mentioning | n/a | todo | todo | todo |

Lyrics are shared between sites (decided 2026-11-17). Band pages hang off bands, which are already separate per site, but this needs checking.

## Phase 4: Greywire-only features

| Step | Backend | Web | Android | iPhone |
|------|---------|-----|---------|--------|
| 4.1 My Pedals: claimed pedals with serial, model, claim date | ? | ? | todo | todo |
| 4.2 Pedal transfer request (sold or gifted pedal; described in the EULA) | todo | todo | todo | todo |
| 4.3 Pedal catalog from the backend (replaces hard-coded Home cards) | todo | todo | todo | todo |
| 4.4 Pedal illustrations (port the website's SVGs) | n/a | n/a | todo | todo |
| 4.5 Pedal docs and support: manuals, power/wiring, warranty, contact | ? | ? | todo | todo |
| 4.6 Switchyard: setlists and sounds stored on the pedal | todo | todo | todo | todo |

**4.6 is on hold.** How the phone talks to Switchyard (BLE, MIDI, USB...) depends on the hardware in `GreywireStudio`, and it's too early to decide. Come back to it later.

## Phase 5: Platform and release

| Step | Backend | Web | Android | iPhone |
|------|---------|-----|---------|--------|
| 5.1 Push notifications (chat, friend requests, band invites) | done (Prosaurus FCM) | n/a | todo | todo |
| 5.2 Release signing key (own key, not Prosaurus's) | n/a | n/a | todo | todo |
| 5.3 Store listings: icon, graphics, screenshots, data safety | n/a | n/a | todo | todo |
| 5.4 Seeded Greywire member in `breakroom_test` | n/a | n/a | todo | n/a |
| 5.5 BreakTest E2E tests for the Greywire apps | n/a | n/a | todo | todo |

Notes:
- **5.1**: Firebase isn't set up (no `google-services.json`). Prosaurus registers its FCM token on login and removes it on logout; do the same.
- **5.4**: `BreakTest/database/seed-data.sql` now has two unclaimed test pedals (`00000000000000A1`, `00000000000000A2`, Flashover) for testing the claim flow. Still needed: a user who is already a member when the tests start (a grant or a claimed pedal). Until then, Home's "Member" state has only been checked in code, not on screen.
- **5.4**: `seed-data.sql` has only the Prosaurus email templates, so in tests a Greywire request falls back to them (the reset email says Breakroom and links to local.prosaurus.com). Add the Greywire `signup_verification` and `password_reset` templates (Breakroom migration 091) to the seed.
- **5.5**: the test tags are listed in `CLAUDE.md`. Use `mobile: clearApp` with appId `com.cherryblossomdev.greywire` between tests.
- **4.3 / 4.4**: the Flashover and Switchyard cards on Home are hard-coded text copied from the website. The website's pedal SVGs are in `../GreywireStudio-Website/img/`.

## Log

Newest first. One line per finished step, with the commit.

- 2026-11-17: 0.1 Android: `X-Site: greywire` added in `RetrofitClient`. Checked: login and claim work against the local test backend with it.
- 2026-11-18: 1.3 Android: Forgot password (`ForgotPasswordScreen.kt`), linked from Login. Tested on the emulator: invalid email, a registered email (reset email written by the test backend), an unknown email (same message, as the backend intends), and back to sign in.
- 2026-11-17: 1.1 Android: Sign up (`SignupScreen.kt`), linked from Login; afterwards the EULA step, then Home. Tested on the emulator: mismatched passwords, a taken username (409), and a new account (recorded as `signup_site = greywire`, `signup_platform = android`, put in the three Greywire rooms). The account starts unverified; the verification email links to the website (see 1.2).
- 2026-11-17: 1.4 Android: Claim a pedal screen (`ClaimPedalScreen.kt`), reached from Home's membership card. Tested on the emulator against the local test backend: bad format, unknown serial, a valid claim (typed with dashes, lowercase), Home updating to Member, and re-claiming your own pedal.
- 2026-11-17: Decisions recorded: Greywire open to non-pedal musicians (free tier + subscription), lyrics shared.
- 2026-11-17: Roadmap written.
