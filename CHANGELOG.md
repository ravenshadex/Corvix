# Corvix Changelog

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
