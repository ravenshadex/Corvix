# Winamp Classic Skin Package for Corvix (`com.corvix.player`)

This folder stores the **1:1 Winamp Classic (`WINAMP` + `WINAMP EQUALIZER` + `WINAMP PLAYLIST`)** skin specification, exact color/coordinate manifest, pixel reference screenshot, and standalone interactive preview ready to be wired into **Corvix** when requested.

> **Status:** Saved only as a skin asset package. No Corvix Kotlin or Gradle files have been modified yet.

## Contents
- [reference_screenshot.png](file:///E:/AI%20Creation/Corvix/skins/winamp_classic/reference_screenshot.png) — Original target Winamp skin layout to match 1:1.
- [skin_manifest.json](file:///E:/AI%20Creation/Corvix/skins/winamp_classic/skin_manifest.json) — Exact `275`-unit virtual grid coordinates, color hex tokens, `70–16K` EQ labels, and `CorvixUiState` / `CorvixViewModel` bindings.
- [preview.html](file:///E:/AI%20Creation/Corvix/skins/winamp_classic/preview.html) — Live interactive 1:1 HTML/Canvas mockup of the exact skin.

## Planned Corvix Integration Map (When Instructed to Implement)
1. **`CorvixModels.kt`**: Add `WINAMP_CLASSIC("Winamp Classic", "2.91 3-window rack, 10-band EQ & terminal playlist")` to `enum class AppSkin`.
2. **`CorvixTheme.kt`**: Support `isWinamp = skin == AppSkin.WINAMP_CLASSIC` using the hex tokens in `skin_manifest.json`.
3. **New Screen/Shell Component (`ui/screens/WinampSkinScreen.kt`)**:
   - Render the 3 docked windows inside a `275f`-unit uniform-scaled Compose layout (`MAIN` `275×116`, `EQUALIZER` `275×116`, `PLAYLIST` `275×fillRemaining`).
   - Connect directly to `CorvixViewModel` (`uiState`, `elapsedSecFlow`, `playTrackAt`, `togglePlayPause`, `nextTrack`, `previousTrack`, `seekTo`, `setVolume`, `updateEqBand`, `toggleShuffle`, `cycleRepeat`, plus `SettingsScreen` to switch back to `Liquid Shell` or `Classic iPod`).
4. **`CorvixRootApp.kt` & `SettingsScreen.kt`**: Render `WinampSkinScreen` when `state.appSkin == AppSkin.WINAMP_CLASSIC`, with quick entry back to `Settings` / `Folders` via the `O` clutterbar button, `⏏` Eject, or `ADD` / `LIST OPTS`.
