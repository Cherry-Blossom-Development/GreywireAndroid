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

## Where we left off (updated 2026-11-27)

**Done today on Android:** 2.8 is finished. The Friends tab has a third section, Find: search as you type, and Add sends a request (the person moves to Sent). Then 2.7's live updates: an open chat room gets new, edited and deleted messages over Socket.IO (`SocketManager`), and catches up after a reconnect.

**New backend route (Breakroom `c139691`, pushed, not deployed):** `GET /api/friends/find?q=` (at least 2 characters) matches handle or name and leaves out yourself, guests and anyone you're already connected to (friends, pending requests either way, blocks either way); on Greywire only Greywire users. The old note here was wrong: `/api/friends/search` only searches your existing friends (it's for @mentions), and the web's Find Users tab loads every account from `/api/user/all` and filters in the browser. **Production needs a backend deploy** for Find to work there (and for the 2026-11-26 notification settings fix, `5b941aa`).

**Waiting on Dallas (decisions, not code):** (1) deploy the Breakroom backend (`5b941aa`, `c139691`); (2) decide on the `/api/user/all` fix below (asked 2026-11-27, no answer yet); (3) pick the next step: finish 2.7 (typing indicators, "Message" from Friends) or start 2.5 bands.

**For iPhone (check while matching 2.7 live chat):** the socket handshake needs `X-Site: greywire` (and the token in `auth`), or the backend treats it as Prosaurus. REST sends are also broadcast as `new_message`, so de-duplicate by id. `roomId` in socket events can arrive as a number or a string.

**Security issue found (not fixed, Breakroom):** `GET /api/user/all` has no authentication. Anyone can call it and get every account's email, alternate email and subscription status. It's used by the web's Find Users tab (and probably admin pages). It should require an admin, and the web's Find Users should switch to `/api/friends/find`.

**Next on Android (suggested):** the rest of 2.7 (typing indicators, starting a DM from Friends, images), or 2.5 bands. Also open: Block and Flag on other members' profiles (`TODO.md`).

**iPhone still to match:** 2.2 upload and delete (and the paywall on a 402), 2.8 Find (use `/api/friends/find`), and all of 2.9/2.10: the Profile tab with editing, other members' profiles from Friends, and Settings.

**For iPhone (check while matching 2.2):** the session's recording date (`recorded_at`) comes as UTC midnight (`2026-11-24T00:00:00.000Z`); shown in local time it's a day early west of UTC. Android now formats it in UTC (`formatCalendarDate` in `Dates.kt`). iPhone probably has the same bug.

**For iPhone (check while matching the Profile):** `PUT /api/profile` writes all four fields (names, bio, work bio), and a field missing from the JSON makes the update fail. Send `workBio` back as loaded, as an explicit `null` when it's null.

**For iPhone (check while matching Settings):** `GET /api/user/notification-settings` sends `true`/`false` when there's no row yet but `1`/`0` once one is saved, and `/api/user/alternate-email` always sends `0`/`1`, so decode these booleans from numbers too (Android's `LenientBooleanAdapter`).

**Test environment (fixed 2026-11-24):**
- `breakroom_test` was missing migrations 049 (`sessions.duration_ms`) and 096 (`master_s3_key`, `master_file_size`); both are applied now, so `/api/sessions` works.
- The test backend lacked `@tus/server` because the `breakroom_backend_test_node_modules` volume kept old packages even after `--build`. Removing the volume and rebuilding fixed it. If a new backend package is missing again: `docker volume rm breakroom_backend_test_node_modules`, then rebuild.
- To start it: start Docker Desktop, then from `../Breakroom` run `docker compose --env-file .env.test -f docker-compose.test.yml up -d --build backend-test`; start the `Pixel_9_API_35` emulator.
- The test backend stores uploads in the real S3 bucket (`prosaurus-breakroom-uploads`), so delete test sessions afterwards (deleting removes the S3 files).
- `testadmin` has a pending friend request to `testuser` (sent 2026-11-26), handy for opening a profile from Friends.
- `testuser` isn't in the Greywire default rooms (General 37, Gear and Tone 38, Gigs and Bands 39); `testadmin` is. To test live chat between two accounts, add testuser to `users_rooms` first. Posting with curl as testadmin while the app has the room open works for one account.
- Everything was shut down at the end of 2026-11-27 (emulator, test backend, Docker Desktop).
- Accounts: `testuser` holds test pedal `00000000000000A1` (member, no limits); `testadmin` is a Greywire user without a pedal (free tier: 3 band + 3 solo sessions), handy for testing the paywall. Test account `gwtest073722` exists. `npx ts-node database/setup.ts` in BreakTest resets everything.

**Working style agreed:** small commits, one change each; push after each finished step; update this file as steps finish.

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
| 1.1 Sign up (client-side password hashing, like Prosaurus) | done | done | done | done |
| 1.2 Email verification: the email's link opens the website (no deep links for now) | done | done | n/a | n/a |
| 1.3 Forgot password (`/api/auth/forgot-password`; the reset link opens the website) | done | done | done | done |
| 1.4 Claim a pedal: serial entry (`POST /api/greywire/claim`, 10 failed/hour limit) | done | done | done | done |
| 1.5 Claim a pedal: QR scan | n/a | n/a | done | done |
| 1.6 Open Greywire to non-members: remove members-only gating (`greywireAccess.js`, web `requiresMember`) | done | done | n/a | n/a |
| 1.7 Guest accounts (`isGuest`, can never be members) | done | ? | done | done |
| 1.8 One subscription for both sites (Prosaurus and Greywire Studio) | done | ? | n/a | n/a |
| 1.9 Free-tier limits on Greywire: members unlimited, others like Prosaurus (sessions 3+3, bands 1) | done | ? | n/a | n/a |
| 1.10 Paywall: handle 402 `requiresSubscription`, show plan status (`/api/billing/plan`) | done | ? | done | wip |
| 1.11 In-app purchase (Google Play Billing / StoreKit) and backend verification for the Greywire app IDs | todo | n/a | todo | todo |
| 1.12 EULA / Privacy Policy: no longer members-only; add the subscription terms | n/a | done | done | todo |

Notes:
- **1.5**: the QR code holds the serial as plain text; nothing is needed on the backend. Android uses Google's code scanner (Play services), iOS AVFoundation.
- **1.10**: the paywall screen is built in both apps, but nothing opens it yet: none of the features the apps have so far can hit a free-tier limit (uploads and bands can). Wire it up with those features (2.2 upload, 2.5 bands).
- **1.6**: until this is done on the backend, a non-member using the app gets 403 `GREYWIRE_MEMBERSHIP_REQUIRED` on everything outside `/api/auth`, `/api/greywire` and `/api/eula`.
- **1.9**: already in place: `checkFreeTierLimit` (`utilities/sessionRecords.js`) and the band limit in `routes/bands.js` let members past the limit on the Greywire site, and everyone else needs `isSubscribed`.
- **1.11**: `/api/subscriptions/google/verify` checks purchases against one package name (`GOOGLE_PLAY_PACKAGE_NAME`, Prosaurus's). The Greywire app (`com.cherryblossomdev.greywire`) needs its own; Apple verification likewise needs the Greywire bundle ID.

**1.8 (decided 2026-11-17):** one subscription covers both sites. `user_subscriptions` has no site column, so this is already how the backend works: subscribing in either app, or on either website, lifts the limits on both. The web Greywire billing page still needs checking.

## Phase 2: Music features the web community already has

Web routes: `/chat`, `/sessions`, `/friends`, `/user/:handle`, `/profile`. The Prosaurus Android app (`../Android`) has screens to adapt for each.

| Step | Backend | Web | Android | iPhone |
|------|---------|-----|---------|--------|
| 2.1 App navigation shell (somewhere to put the features below) | n/a | n/a | done | done |
| 2.2 Sessions: list, play/stream, upload, delete | done | done | done | wip |
| 2.3 Sessions: record in-app | n/a | ? | todo | todo |
| 2.4 Sessions: comments, ratings, instrument tags, share links | done | done | wip | wip |
| 2.5 Bands: create, invite (link and email), members, band sessions | done | done | todo | todo |
| 2.6 Band setlists (`/bands/:id/setlists`) | done | ? | todo | todo |
| 2.7 Chat: rooms and direct messages (Socket.IO) | done | done | wip | wip |
| 2.8 Friends: list, requests, search (members only) | done | done | done | wip |
| 2.9 Profile: own profile, settings, other members' profiles | done | done | done | todo |
| 2.10 Musician profile (bio, "musician only"; Greywire only) | done | done | done | todo |

Notes:
- **2.2 / 2.4**: both apps list the user's sessions and their bandmates' (`/api/sessions`, `/api/sessions/band-members`), play them (`/stream` with the token) and rate them 1 to 10. Android also uploads (`POST /api/sessions`, multipart: `audio`, `name`, `session_type`, `recorded_at`, `band_id`, `instrument_id`) and deletes your own sessions (`DELETE /api/sessions/:id`). Still to do: upload and delete on iPhone (2.2), comments and share links (2.4). Upload is where the paywall (1.10) first matters: the free tier allows 3 + 3 sessions, and Android shows the paywall on the 402.
- **2.7**: both apps list rooms and direct messages, open them, load earlier messages and send text. Android also has live updates (2026-11-27): an open room joins its socket room (`join_room`), and `new_message`, `message_edited` and `message_deleted` update it; after a reconnect it rejoins and reloads the newest messages. Sending stays on REST (the backend broadcasts REST messages too, so they're de-duplicated by id). The socket handshake sends `X-Site: greywire`. Still to do: live updates on iPhone, typing indicators (`typing_start`/`typing_stop`, `user_typing`), live unread counts on the room list (`chat_badge_update`), images and videos (shown as a placeholder), starting a DM from Friends, and joining or creating rooms.
- **2.7 iOS bugs (found while matching it on Android, 2026-11-23):** `GET /rooms/:id/messages` already returns oldest first, but `ChatRoomViewModel` reverses it, so messages show newest at the top and "Load earlier" uses the wrong `before`; `POST /rooms/:id/messages` answers `{ message: {...} }` but iOS decodes the message itself, so sending reports an error even though the message was saved; and `before` isn't URL-encoded. Android follows the backend.
- **2.9 / 2.10** (Android, started 2026-11-24): the Profile tab shows your own profile and edits all of it: name and About bio (`PUT /api/profile`), the music bio and musician-only switch (`PUT /api/profile/musician`), instruments and bands (`/musician/instruments`, `/musician/bands`), and the photo (`POST`/`DELETE /api/profile/photo`). Bios use a formatting toolbar matching the web's. Other members' profiles (`GET /api/profile/user/:handle`) open read only from any person on the Friends tab, with About hidden for musician-only profiles. Settings (from the Profile tab) has the web's three sections: notifications (`/api/user/notification-settings`), the alternate email (`/api/user/alternate-email`) and the account deletion request (`POST /api/profile/deletion-request`). Done on Android 2026-11-26. Not on the app's other-profile screen yet: the web's Block and Flag buttons (see `TODO.md`). Test data: testadmin has a music bio, two instruments and two bands in `breakroom_test`.
- **2.8**: both apps have the list and requests (accept, decline, cancel, remove). Android also has Find (2026-11-27): `GET /api/friends/find?q=` (added for the apps; the web still filters `/api/user/all` in the browser) and `POST /api/friends/request/:id`. Still to do: Find on iPhone. Not `/api/friends/search`, which only searches existing friends.

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
| 4.4 Pedal illustrations (port the website's SVGs) | n/a | n/a | done | done |
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

- 2026-11-27: 2.7 Android: live updates over Socket.IO (`7d46dc2`). Tested on the emulator against the test backend, in General as testadmin: messages posted with curl appeared without reopening the room; one sent from the app appeared once; an edit and a delete from curl showed live; with the emulator's network off, a message posted meanwhile appeared after it came back (the backend log shows the reconnect and rejoin). Test messages deleted afterwards. Note: testuser isn't in the Greywire default rooms in `breakroom_test`.
- 2026-11-27: 2.8 Android finished: Find in the Friends tab (`2607d6e`), with the new backend route `GET /api/friends/find` (Breakroom `c139691`). Tested with curl (self, pending requests and non-Greywire users left out; under 2 characters refused; `%` and `_` matched literally) and on the emulator as testadmin: searched "gw", added gwtest073722, saw them under Sent, then cancelled the request.
- 2026-11-26: 2.9 Android finished: other members' profiles from Friends (`1b1c406`); Settings with notifications (`e8599c4`), the alternate email (`6fed6b8`) and the account deletion request (`0749ec2`). Backend fix (Breakroom `5b941aa`): `PUT /api/user/:id` caught `PUT /notification-settings` and `/audio-defaults` and refused them for non-admins, so those settings couldn't be saved on the web either. Tested on the emulator against the test backend: a profile opened from a sent request and Back; each notification switch saved and reloaded; an alternate email set, confirmed (in the database, as the test DB has no confirmation email template), its notices switch, and removed; a deletion request sent (then deleted from `breakroom_test`).
- 2026-11-25: 2.9/2.10 Android: Profile editing, one commit per step: names, musician-only switch, instruments, bands, photo, then the bios with a formatting toolbar. Fixes: names failed to save with a null work bio; the tab's scroll position after an edit; dialog and menu test tags for Appium. Tested on the emulator against the test backend (add, edit, remove and validation for each; photo upload from a 4000x3000 JPEG came out 1024x768; bios saved unchanged come back byte for byte).
- 2026-11-24: 2.2 Android: upload and delete, and the paywall on a 402 (1.10). Tested on the emulator: upload as a solo take with an instrument, playback of the upload, delete, and the paywall for testadmin's 4th solo session. Also fixed the recording date showing a day early. Test environment fixed (migrations 049 and 096 on `breakroom_test`, fresh `node_modules` volume).
- 2026-11-23: 4.4 Android: pedal illustrations on Home's cards, and the 632dp tablet width cap on the tabs, matching GreywireiPhone `1de7915`.
- 2026-11-23: 2.2 and 2.4 (list, play, rate) Android, matching GreywireiPhone `6c44986`.
- 2026-11-23: 2.7 (rooms and DMs over REST) Android, matching GreywireiPhone `392e675`.
- 2026-11-23: 2.8 (list and requests) Android, matching GreywireiPhone `d3c5e86`. iPhone did the same on 2026-11-22.
- 2026-11-23: 1.5, 1.7 and 1.10 (screen only) Android, matching GreywireiPhone `29dd271`: QR scan on the claim screen, guest accounts, paywall.
- 2026-11-21: 1.1, 1.3, 1.4 and 2.1 iPhone: sign up, forgot password, claim a pedal and the five tabs (GreywireiPhone `575fc88`).
- 2026-11-20: 2.1 Android: navigation shell, five bottom tabs (`ca10737`, `097c109`). Tested on the emulator: tabs switch, Back from a tab goes Home, Claim is full screen without the bar, Sign out on Profile goes to Login.
- 2026-11-20: 1.6 and 1.12 deployed to production (Breakroom); 1.12 Android legal text `3516bce`.

- 2026-11-17: 0.1 Android: `X-Site: greywire` added in `RetrofitClient`. Checked: login and claim work against the local test backend with it.
- 2026-11-18: 1.3 Android: Forgot password (`ForgotPasswordScreen.kt`), linked from Login. Tested on the emulator: invalid email, a registered email (reset email written by the test backend), an unknown email (same message, as the backend intends), and back to sign in.
- 2026-11-17: 1.1 Android: Sign up (`SignupScreen.kt`), linked from Login; afterwards the EULA step, then Home. Tested on the emulator: mismatched passwords, a taken username (409), and a new account (recorded as `signup_site = greywire`, `signup_platform = android`, put in the three Greywire rooms). The account starts unverified; the verification email links to the website (see 1.2).
- 2026-11-17: 1.4 Android: Claim a pedal screen (`ClaimPedalScreen.kt`), reached from Home's membership card. Tested on the emulator against the local test backend: bad format, unknown serial, a valid claim (typed with dashes, lowercase), Home updating to Member, and re-claiming your own pedal.
- 2026-11-17: Decisions recorded: Greywire open to non-pedal musicians (free tier + subscription), lyrics shared.
- 2026-11-17: Roadmap written.
