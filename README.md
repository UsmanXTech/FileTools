# FileTools - Android App

A minimal, offline-first file conversion & tools app built with Kotlin + Android Native.

---

## Features

| Plugin | Tools |
|---|---|
| 🖼️ Image Tools | Convert, Compress, Resize, Rotate, Crop |
| 🎵 Audio Tools | Trim, Extract from Video, Convert, Merge |
| 🎬 Video Tools | Trim, Remove Audio, Convert, Compress |
| 📄 PDF Tools | Image→PDF, Merge, Split, Compress |
| 📁 Archive Tools | ZIP, Unzip, View Contents |

---

## Setup Instructions

### 1. Open in Android Studio
- File → Open → Select `FileToolsApp` folder

### 2. Add Fonts (Required)
Download Inter font from https://fonts.google.com/specimen/Inter

Place these 3 files in `app/src/main/res/font/`:
- `inter_regular.ttf` (Weight 400)
- `inter_semibold.ttf` (Weight 600)
- `inter_bold.ttf` (Weight 700)

### 3. Set SDK Path
Edit `local.properties`:
```
sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

### 4. Sync & Build
- Click "Sync Now" when prompted
- Build → Make Project
- Run on device/emulator

---

## Architecture

```
Plugin-Based Modular Architecture (Approach 1)

core/
  BasePlugin.kt        ← Abstract base all plugins implement
  PluginManager.kt     ← Singleton registry
  ToolItem.kt          ← Data class for each tool
  ProcessingService.kt ← Background processing service

plugins/
  image/ImagePlugin.kt
  audio/AudioPlugin.kt
  video/VideoPlugin.kt
  pdf/PdfPlugin.kt
  archive/ArchivePlugin.kt

ui/
  splash/SplashActivity.kt
  home/HomeActivity.kt + PluginAdapter.kt
  tool/ToolActivity.kt + ToolAdapter.kt
```

### Adding a New Plugin

1. Create `plugins/newplugin/NewPlugin.kt`
2. Extend `BasePlugin`
3. Implement `executeTool()`
4. Register in `PluginManager.init()`

That's it — home screen auto-updates!

---

## Pro Features (Monetization)

Tools marked `isPro = true` show a PRO badge and are locked.
To unlock: integrate Google Play Billing Library.

Recommended flow:
- Free: all basic single-file tools
- Pro ($1.99 one-time): batch convert, remove background, advanced formats

---

## Dependencies

| Library | Purpose |
|---|---|
| Material Components | UI theme & components |
| RecyclerView | Plugin & tool lists |
| Glide | Image loading/preview |
| Lottie | Smooth animations |
| WorkManager | Background processing |
| Coroutines | Async file operations |
| iText7 | PDF operations |

---

## Design

- Dark theme: `#0F1117` background
- Surface: `#1A1D27`
- Accent: `#4F8EF7` blue
- Each plugin has unique accent color
- Font: Inter (Regular, SemiBold, Bold)
- Card radius: 16-20dp
- Staggered animations on home screen

---

## File Output Location

Processed files saved to:
```
Android/data/com.filetoolsapp/files/FileTools/<plugin_id>/
```

---

## Next Steps / Roadmap

- [ ] Add FFmpeg for full video/audio conversion
- [ ] Add AdMob ads between operations
- [ ] Google Play Billing for Pro
- [ ] Recent files history
- [ ] Dark/Light theme toggle
- [ ] Batch processing (Pro)
