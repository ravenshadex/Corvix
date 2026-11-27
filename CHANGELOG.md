# Corvix Changelog

## v1.0.4 (Build 5)
### Real Liquid Glass UI, Now Playing Redesign & Release Signing
- **Real Liquid Glass Material System:** Updated `LiquidGlass.kt` with rounded-rectangle SDF edge-refraction AGSL shaders (eliminating inner rectangle artifacts), outer-only `ClipOp.Difference` drop shadows, stronger `0..100` glass blur & prism dispersion, and frosted `94%` smoked-crystal backing on the Mini-Player, Dock, and Lyrics Manager.
- **Redesigned Now Playing & Pull-Up Queue:** Edge-to-edge status bar & flush bottom `UP NEXT` sheet with live vertical drag-to-expand, drag-to-reorder (`=`) tracks, shuffle-synced queue order, iOS-style heart favorite icon, 4-pill utility bar (`EQ`, `Speed`, `Sleep Timer`, `Lyrics`), and spacious in-place Lyrics view.
- **2-Page Liquid Glass Onboarding & Clean Fresh Install:** Replaced the onboarding flow with a 2-page Real Liquid Glass welcome & permissions experience and removed all wireframe/sample track defaults and initial playback state.
- **Live IEM / USB DAC Detection & Skin Polish:** Added `AudioDeviceCallback` hardware output detection (showing connected USB DAC / Wired IEM / Bluetooth device names), collapsible `Liquid Glass Engine` settings, interactive Profile category drill-down, Classic iPod skin chooser submenu, Winamp Classic clutterbar cleanup, and tablet/landscape layout polish.
- **Signed Release Universal APK:** Configured v1 + v2 + v3 release signing in `app/build.gradle.kts`.

## v1.0.3 (Build 4)
### Hotfix
- **Restored Media Notification Bar & Seek Bar Slider:** Replaced Kotlin interface delegation (`Player.Listener by listener`) in `CorvixPlaybackService` with an explicit `CommandAugmentingPlayerListener` forwarding all 37 `Player.Listener` callbacks (including `onTimelineChanged`, `onPlaybackStateChanged`, `onIsPlayingChanged`, `onMediaItemTransition`, `onPositionDiscontinuity`, and `onAvailableCommandsChanged`). Kotlin delegation over Java default interface methods previously dropped playback/timeline updates to `MediaSession` & `MediaController`, which hid the notification bar and blocked seeking while still advertising Next/Previous skip commands.
- **Refined Recorder Pause Filter:** Excluded silenced clients, `REMOTE_SUBMIX`, and system `HOTWORD` sources in `AudioRecordingCallback` so background assistant listeners do not interrupt playback while active recorder apps still pause playback immediately.

## v1.0.2 (Build 3)
### Bug Fixes & Stability
- **Pause automatically when turning on Recorder / Mic apps:** Added `AudioManager.AudioRecordingCallback` monitoring inside `CorvixPlaybackService` alongside Media3 audio focus handling so playback pauses immediately when a voice recorder or external mic capture starts.
- **Continuous Playback Reliability & Auto-Recovery:**
  - Enabled `C.WAKE_MODE_LOCAL` on `ExoPlayer` so the CPU stays awake across track transitions when the screen is off.
  - Guarded `CorvixAudioController` track transitions (`loadJob` cancellation, `isSwitchingTrack` protection against transient `isPlaying = false` flickers, duplicate `STATE_ENDED` protection, and `onPlayerError` recovery with up to 3 automatic skips on corrupted/unplayable files).
  - Removed competing timer auto-advance races in `CorvixViewModel` while ExoPlayer position is active, and added `CorvixServiceQueue` fallback so `CorvixPlaybackService` continues advancing tracks even if the UI/ViewModel is destroyed in the background.
- **Notification "Next" Skip Control Fix:** Fixed `ForwardingPlayer` listener delegation (`onAvailableCommandsChanged` and `onEvents`) plus `MediaSession.Callback.onConnect` so `COMMAND_SEEK_TO_NEXT` and `COMMAND_SEEK_TO_NEXT_MEDIA_ITEM` remain enabled in Android's media notification alongside Previous.

## v1.0.1 (Build 2)
- **Search & Animations:** Multi-token search across title, artist, album, genre, format, and language with title-prefix relevance ranking; lively Liquid Glass search bar animations (spring focus lift, border shimmer, cycling placeholder hints, live animated match count, clear button, smooth carousel collapse, and `animateItem` result transitions).
- **Versioning:** Added build version tracking and a `Corvix Version` card in Settings.

## v1.0.0 (Build 1)
- Initial release of Corvix offline lossless audio player.
