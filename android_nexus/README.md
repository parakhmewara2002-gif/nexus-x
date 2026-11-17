# NEXUS X — Ultimate Android File Manager & Privilege Center

NEXUS X is a next-generation Android file manager built strictly according to the **iOS 26 Light Liquid Glass** design system and specifications supplied in the Stitch project base.

---

## 💎 Visual Identity: iOS 26 Light Liquid Glass (Mandatory Default)
- **Materials**: Translucent milky-white glass (`#FFFFFF` with 65%–88% alpha), multi-tier backdrop diffusion, delicate specular border reflections (`rgba(255, 255, 255, 0.75)`).
- **Dock Navigation**: 5-tab floating pill navigation dock (`Home`, `Files`, `Device`, `Tools`, `Settings`) with glowing Sky-Blue active indicator.
- **Palette**:
  - Primary Sky Blue: `#0284C7`
  - Accent Sky Blue: `#0EA5E9`
  - Radiant Lavender: `#8B5CF6`
  - Luminous Cyan: `#06B6D4`
  - Emerald Success: `#10B981`
  - Amber Warning: `#F59E0B`
  - Crimson Danger: `#EF4444`
  - Main Slate Text: `#0F172A`
  - Ground Surface: `#F0F4F9` with ambient pastel mesh gradient backdrops.

---

## 🛠️ Architecture & Core Features

- **Jetpack Compose + Material 3**: Complete native UI implementation of all supplied Stitch templates.
- **Full File Manager**:
  - Storage browsing (Internal, SD Card, OTG).
  - Folder & File creation, copy, cut, move, rename, delete, batch selection.
  - Sorting (Name, Date, Size, Type) and category filters (Images, Videos, Audio, Docs, APKs, Archives).
  - Breadcrumb path navigation with real-time directory updates.
- **Storage Analyzer**:
  - Accurate breakdown of used vs total storage.
  - Interactive multi-category storage distribution bar.
  - Large file finder & duplicate file detector (SHA-256 hash detection).
  - Empty directory cleaner recommendations.
- **Device & Privilege Center**:
  - Real hardware diagnostics (RAM, internal storage, battery health, CPU architecture, active network, sensors).
  - Root detection (`su` binary verification) and Shizuku API availability detection.
- **ADB & Terminal Center**:
  - Local permission-aware shell executor (`sh -c`).
  - System property explorer (`getprop`), logcat reader, and command history logging.
- **Application Manager**:
  - List user & system packages.
  - APK exporter (`.apk` extraction directly to `/sdcard/NEXUS_APKs`).
- **Encrypted File Vault**:
  - PIN pad authentication stage with protected storage vault.
- **Liquid Media Preview**:
  - Image preview, source code/text viewer with monospace fonts, and Media3 ExoPlayer video integration.

---

## 🚀 Build & Test Commands

To build the project and execute unit tests:

```bash
cd android_nexus

# Run Unit Tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```

Output Artifact:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
