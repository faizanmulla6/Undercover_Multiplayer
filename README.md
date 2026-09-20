# Undercover — native Android app

A local pass-and-play social deduction party game. Same concept as before —
add your players, deal secret words, pass the phone for private reveals, then
argue and vote the undercover agents out — but now built as a **real native
Android app** instead of a WebView wrapping an HTML page.

## What changed

| Before | Now |
| --- | --- |
| Single `WebView` loading `assets/undercover.html` | Jetpack Compose UI, no WebView, no HTML |
| CSS variables, hand-rolled widgets | Material 3 components, real light/dark colour scheme |
| All logic in one 1,400-line JS file | Kotlin model + `GameViewModel` + composable screens |
| `localStorage` | `SharedPreferences` (`GameStore`), backed up by Android auto-backup |

## New in this version

- **Hold-to-reveal card** with a 3D flip. The word only shows while a finger is
  held on the card, so a phone set down mid-pass never leaks a word.
- **Blank agent last guess** — a blank agent who gets voted out may name the
  civilian word and steal the round outright (4 points).
- **Categorised word bank** — ~110 built-in pairs across Food, Animals, Places,
  Everyday Things, People & Jobs, Sport, Screen & Stage, Nature, Desi and a
  deliberately hard "Tricky" set. Categories can be toggled on and off.
- **Speaking order** is generated each round, with a civilian always opening so
  the blank agent is never forced to speak first.
- **Discussion timer** (off / 30 / 60 / 90 / 120s) with start, pause and reset.
- **Two voting styles** — quick tap with a confirmation, or a counted tally
  (tap to add a vote, long-press to take one back) that refuses to eliminate on
  a tie.
- **Undo** one elimination if the group changes its mind.
- **Round log** under the scoreboard: who won each round and which word pair
  was in play.
- Screen stays awake during a game, edge-to-edge layout, haptic feedback on
  reveal, back-button navigation, and content descriptions throughout.

Kept from the original: custom word pairs with bulk import/export, the
built-in/mix/custom word mode, persistent player names, cumulative scoring
(2 points per surviving civilian, 3 per surviving undercover/blank), rules
sheet, and fully offline play with **no INTERNET permission**.


## Two ways to play

When the app opens it asks how you're playing tonight.

### One phone
The original pass-and-play flow: add names, deal, hold the card to read your
word, pass it on, then vote from the shared screen.

### Multiple phones (local Wi-Fi)
One phone per player, everyone on the same Wi-Fi network. No internet, no
accounts, no server — the host phone *is* the server.

- **One person hosts.** They pick their name and tap *Host a game*. The app
  starts a TCP server on the phone and advertises it over mDNS, and also shows
  its `IP:port` for the case where a router blocks discovery.
- **Everyone else joins.** *Find a game* lists hosts found on the network; tap
  one, or type the address the host is showing.
- **Only the host can set up.** Number of undercover agents (auto-balanced as
  people join), blank agent on/off, discussion timer, kicking a player, dealing
  the round, opening voting, revealing roles, starting the next round. Joining
  phones have none of those controls.
- **Everyone else sees only their own card and the vote.** The host never sends
  another player's word down the wire, so there is nothing to peek at even in
  the raw traffic.
- **Voting is real.** Each living player taps one name on their own phone. The
  host counts them, refuses to resolve a tie (everyone votes again), reveals the
  eliminated player's role, and checks the win condition.
- **Blank agent guess** works over the network too: the eliminated blank gets a
  private prompt on their own phone.
- **Reconnects are handled.** If a phone drops, it keeps its seat and role — the
  player rejoins with the same name and picks up where they left off.

Round flow on every phone: **Lobby → Reveal → Discussion → Voting → Result**,
with the host driving the transitions.

#### How the network layer works

| File | Role |
| --- | --- |
| `net/Proto.kt` | Message types and the `NetState` snapshot, encoded as JSON |
| `net/GameServer.kt` | Host's `ServerSocket`, one thread per phone, per-player sends |
| `net/GameClient.kt` | Joining phone's socket and read loop |
| `net/Discovery.kt` | mDNS advertise/browse (`_undercover._tcp`) + multicast lock |
| `MultiplayerViewModel.kt` | Host authority: seats, dealing, vote tally, win checks |
| `ui/screens/multi/` | Mode picker, host/join entry, lobby, and the shared round screen |

Transport is newline-delimited JSON over TCP on port 45987 (it walks up to
45995 if that one is taken). Permissions added for this: `ACCESS_NETWORK_STATE`,
`ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`.

#### Troubleshooting

- Everyone must be on the **same** Wi-Fi. Guest networks with "client
  isolation" and mobile data will not work.
- If *Find a game* stays empty, use the `IP:port` shown on the host's lobby
  screen — that path doesn't rely on multicast at all.
- The host leaving ends the game for everyone; the lobby is the safe place to
  add or drop players.

## Source map

```
app/src/main/java/com/faizan/undercover/
├─ MainActivity.kt          app shell, top bar, screen switch, back handling
├─ GameViewModel.kt         all game rules and state transitions
├─ model/Game.kt            Role, WordMode, GameState, RoundPlayer, outcomes
├─ data/WordBank.kt         built-in categorised word pairs
├─ data/GameStore.kt        SharedPreferences persistence
└─ ui/
   ├─ theme/Theme.kt        dossier colour scheme + monospace display type
   ├─ components/           SectionCard, StatPill, rules & scoreboard sheets
   └─ screens/              SetupScreen, RevealScreen, DiscussionScreen
```

## Build

1. Open **Android Studio** (Koala or newer) → **File ▸ Open** → pick the folder
   containing `settings.gradle`.
2. Let Gradle sync (it downloads Gradle 8.6 and the AndroidX artifacts once).
3. Press **Run ▶**.

If Android Studio offers to create the `gradlew` wrapper scripts, accept —
they're only needed for command-line builds (`./gradlew assembleDebug`).

- `applicationId`: `com.faizan.undercover`
- `minSdk 24`, `compileSdk`/`targetSdk 34`, Kotlin 1.9.24, Compose BOM 2024.06.00
- No permissions requested.

## Tweaking the game

- **Words** → `data/WordBank.kt` (add pairs or a whole new category constant).
- **Scoring** → the constants at the bottom of `GameViewModel.kt`.
- **Colours and type** → `ui/theme/Theme.kt`.
- **Win conditions** → `GameViewModel.checkEnd()`.
