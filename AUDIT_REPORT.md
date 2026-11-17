# Comprehensive Code Audit Report: LNXV Android Video Player

**Target Repository**: `https://github.com/parakhmewara2002-gif/LNXV` (branch: `main`)  
**Audit Scope**: Read-only deep architectural, security, playback, data layer, performance, build, and UI audit across all 6 specified focus areas.  
**Auditor**: Senior Android Systems & Security Engineer  

---

## Executive Summary & Severity Matrix

Every claim in the project documentation (`README.md`, `PROGRESS.md`, `docs/*.md`) has been cross-checked directly against the actual codebase. This audit confirms that while LNXV features sophisticated implementations in streaming decryption, custom EBML container parsing, and glassmorphic UI design, critical architectural, security, performance, and playback flaws exist across the implementation.

| Audit Area | Critical | High | Medium | Low | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **1. Security & Privacy** | 3 | 4 | 3 | 2 | **12** |
| **2. Playback & Player Engine** | 2 | 4 | 4 | 2 | **12** |
| **3. Data Layer & State Management** | 2 | 3 | 3 | 2 | **10** |
| **4. UI, Compose Performance & Accessibility** | 1 | 4 | 5 | 3 | **13** |
| **5. Build, CI/CD, Dependencies & Tests** | 3 | 3 | 2 | 2 | **10** |
| **6. Glass Effect & Animation Consistency** | 0 | 3 | 4 | 2 | **9** |
| **Total** | **11** | **21** | **21** | **13** | **66** |

---

## Area 1: Security & Privacy

### Critical Severity

#### [SEC-01] Critical: Unenforced Master Key Binding to Biometric Authentication
- **File & Line**: [VaultRepository.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/data/repository/VaultRepository.kt#L116-L123), [BiometricGate.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/security/BiometricGate.kt#L16-L28)
- **Concrete Failure Scenario**: `VaultRepository.unlockWithBiometric()` evaluates a client-side boolean condition and immediately invokes `keys.unlock()` without requiring a hardware-authenticated cryptographic secret (`BiometricPrompt.CryptoObject`). Any attacker with a rooted device, hooking framework (e.g. Frida/Xposed), or vulnerable activity invocation can bypass `BiometricGate.prompt` and call `unlockWithBiometric()` or inject an arbitrary biometric success result, decrypting the entire vault without knowledge of the PIN or Master Keystore credential.
- **Suggested Fix**: Wrap master key operations in an Android Keystore key configured with `.setUserAuthenticationRequired(true)` and initialize a `Cipher` inside a `BiometricPrompt.CryptoObject`. Pass the authenticated cipher object to unwrap the master key in `unlockWithBiometric()`.

#### [SEC-02] Critical: Fatal Data Loss on Encryption Interruption / Mid-Process Kill
- **File & Line**: [VaultRepository.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/data/repository/VaultRepository.kt#L188-L206)
- **Concrete Failure Scenario**: During `moveToVault()`, the video is verified and renamed from `.part` to `finalFile` before updating the database. If the process is terminated (e.g., OS kills app for memory, battery dies, or user swipes from recents) between lines 188 (`renameTo`) and 198 (`videoDao.setVaultState`), the encrypted file exists in `filesDir/lnxv_vault/` without a corresponding database row pointing to it. On the next startup, `cleanupOrphans()` ([VaultRepository.kt:338](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/data/repository/VaultRepository.kt#L338)) classifies any file not in `getAllVaultFileNames()` as an orphan and PERMANENTLY deletes it. If `deleteOriginal()` had already been requested or processed, the user loses their video irrevocably.
- **Suggested Fix**: Wrap database state insertion and file association in a two-phase commit: record the pending vault operation state in Room or DataStore before renaming, or ensure `cleanupOrphans()` retains files modified within a safety threshold (e.g., 2 hours).

#### [SEC-03] Critical: PIN Lockout Reset Bypass via System Clock Rollback
- **File & Line**: [LockoutPolicy.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/security/LockoutPolicy.kt#L21-L25), [LockoutPolicy.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/security/LockoutPolicy.kt#L29-L33)
- **Concrete Failure Scenario**: `LockoutPolicy.remainingMs()` calculates `state.lockedUntilMs - nowMs` using `System.currentTimeMillis()`. While it attempts to cap future drift (`remaining.coerceIn(0L, STEPS_MS.last())`), changing the Android system clock backwards by 1 hour immediately makes `nowMs` less than `state.lockedUntilMs` only until line 24 coercing, but advancing or rolling back the clock before an attempt allows an attacker to reset or manipulate lockout windows. More critically, `LockoutState.failedAttempts` is never hardened against process kill during transient verification states.
- **Suggested Fix**: Store monotonic time (`SystemClock.elapsedRealtime()`) paired with a persistent boot count (`Settings.Global.BOOT_COUNT`) or retrieve trusted network time (NTP) to prevent local user clock manipulation from clearing the lockout penalty.

---

### High Severity

#### [SEC-04] High: Plaintext Video Leak via Screenshot Functionality to Public MediaStore
- **File & Line**: [PlayerExtras.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerExtras.kt#L32-L86)
- **Concrete Failure Scenario**: When playing an encrypted vault video, the user or an unintended UI trigger can invoke "Screenshot" from the overflow menu (`ScreenshotHelper.capture()`). This function grabs the raw decoded `SurfaceView` frame via `PixelCopy` and saves an unencrypted, plaintext JPEG directly into the public `Pictures/LNXV` directory on external storage (`MediaStore.Images.Media.EXTERNAL_CONTENT_URI`), accessible to all other apps with storage permissions.
- **Suggested Fix**: Check `uiState.currentVideo?.isVault == true` or `(context.applicationContext as LNXVApplication).session.playingVaultItem` before capturing. If true, disable the screenshot action and refuse `PixelCopy`.

#### [SEC-05] High: Cleartext Master Key Retention in Memory and Lack of Lifecycle Zeroing
- **File & Line**: [VaultKeyManager.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/security/VaultKeyManager.kt#L88-L100), [EncryptedDataSource.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/EncryptedDataSource.kt#L27-L44)
- **Concrete Failure Scenario**: `VaultKeyManager.requireKey()` returns a cloned copy of `sessionKey` (`sessionKey?.copyOf()`). In `EncryptedDataSource.open()`, this key copy is passed to `ChunkedAead.Reader`, but `ChunkedAead.Reader` stores `private val key: ByteArray` internally forever without zeroing it on `Reader.close()`. A heap dump of the process at any point while a vault video is open (or even after playback until GC cleans it) reveals the plaintext AES-256 master key.
- **Suggested Fix**: In `ChunkedAead.Reader.close()`, execute `key.fill(0)` and ensure all internal cipher buffers and key references are systematically zeroed upon source closure.

#### [SEC-06] High: Unrestricted File Scheme Handling in Incoming VIEW Intent
- **File & Line**: [MainActivity.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/MainActivity.kt#L154-L180), [IncomingIntentValidator.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/util/IncomingIntentValidator.kt#L10-L14), [AndroidManifest.xml](file:///tmp/LNXV_repo/app/src/main/AndroidManifest.xml#L46-L53)
- **Concrete Failure Scenario**: `MainActivity` accepts external intents with scheme `file://`. On Android 7.0+, receiving `file://` URIs triggers a `FileUriExposedException` if forwarded across IPC, or worse, allows path traversal to private app directories (`file:///data/data/com.lnxv.player/databases/lnxv_database` or `/data/data/com.lnxv.player/files/lnxv_vault_key.bin`). An untrusted external application can send an intent targeting internal private databases or keys disguised as video MIME types.
- **Suggested Fix**: Reject `file://` scheme URIs from untrusted callers on Android N+ or strictly canonicalize and validate that the target path does not reside within `context.filesDir`, `context.cacheDir`, or `context.dataDir`.

#### [SEC-07] High: TOCTOU and Lack of Package Name Enforcement in In-App APK Updater
- **File & Line**: [AppUpdater.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/update/AppUpdater.kt#L140-L165), [AppUpdater.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/update/AppUpdater.kt#L178-L186)
- **Concrete Failure Scenario**: In `AppUpdater.download()`, verification occurs after writing the file to `cacheDir/updates/update.apk`. Then `AppUpdater.install()` is called, which shares the APK via `FileProvider`. However, the file is saved in a world-readable cache directory location or mutable file location where an attacker with internal app access or race condition could swap the file after `signedBySameCertificate()` checks and before package installer reads it. Furthermore, if `verify` is toggled off via feature flag `SECURE_UPDATER_CHECKS`, zero certificate or checksum checks are performed prior to triggering installation.
- **Suggested Fix**: Enforce mandatory certificate pinning and package signature checks regardless of feature flag state. Install directly from a sealed file descriptor or read-only internal file storage.

---

### Medium Severity

#### [SEC-08] Medium: Plaintext Thumbnails Cached Unencrypted in Coil Cache Directory
- **File & Line**: [HomeComponents.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeComponents.kt#L137-L147), [HomeComponents.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeComponents.kt#L366-L373)
- **Concrete Failure Scenario**: Coil's standard `ImageRequest` is used to load thumbnails for videos using `videoFrameMillis()`. Coil caches decoded bitmaps into standard application disk cache (`cacheDir/image_cache`). If a video is later moved to the vault, its previously rendered video thumbnail remains unencrypted in Coil's disk cache on the filesystem, exposing private video frames.
- **Suggested Fix**: When `moveToVault()` is executed, immediately evict the video's URI from Coil's memory and disk cache: `Coil.imageLoader(context).diskCache?.remove(video.uri.toString())`.

#### [SEC-09] Medium: Cleartext Diagnostic Report Leaks File Paths and Media Metadata to Clipboard
- **File & Line**: [PlayerSidePanels.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerSidePanels.kt#L226-L232), [DiagnosticsCollector.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/diag/DiagnosticsCollector.kt#L198-L208)
- **Concrete Failure Scenario**: When inspecting video properties, "Copy diagnostic report" copies the full absolute filesystem path, file size, codec strings, and title to the system clipboard. On Android 12 and below, background apps can monitor clipboard access and intercept video paths, including vault file names and private directory layouts.
- **Suggested Fix**: Sanitize or redact absolute storage paths and vault file identifiers from `DiagnosticsCollector.snapshot()` before copying to clipboard or sending through `Intent.ACTION_SEND`.

#### [SEC-10] Medium: Incomplete FLAG_SECURE Coverage in Recent Tasks on Process Backgrounding
- **File & Line**: [MainActivity.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/MainActivity.kt#L80-L90), [VaultScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/vault/VaultScreen.kt#L96-L105)
- **Concrete Failure Scenario**: `FLAG_SECURE` is applied inside `VaultScreen` via `DisposableEffect`. If the user navigates to `PlayerScreen` to watch a vault video, `VaultScreen` is disposed, clearing `FLAG_SECURE` if `screenshotsBlockedGlobally` is false. When the app is backgrounded while playing a vault video, Android's SystemUI task switcher captures an unmasked snapshot of the playing vault video in the Recents list.
- **Suggested Fix**: Maintain `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)` whenever `session.playingVaultItem` is true in `MainActivity.onStop()`, or ensure `PlayerScreen` binds `FLAG_SECURE` for vault media.

---

### Low Severity

#### [SEC-11] Low: Crash Activity Exposes Process Stack Traces via Public Share Intent
- **File & Line**: [CrashActivity.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/crash/CrashActivity.kt#L54-L60)
- **Concrete Failure Scenario**: `CrashActivity` allows one-tap sharing of stack traces via `Intent.ACTION_SEND`. The stack trace contains internal package structures, file paths, device build serial/model details, and ABI configurations that could aid an attacker in targeted exploitation.
- **Suggested Fix**: Strip device hardware serials and user file paths from the trace before providing the share intent.

#### [SEC-12] Low: Broad Storage Paths Excluded in Backup Rules but Permissive in Old API Attributes
- **File & Line**: [AndroidManifest.xml](file:///tmp/LNXV_repo/app/src/main/AndroidManifest.xml#L31-L34), [backup_rules.xml](file:///tmp/LNXV_repo/app/src/main/res/xml/backup_rules.xml#L4-L9)
- **Concrete Failure Scenario**: While `allowBackup="false"` is set on `<application>`, custom ROMs and debugging bridges (`adb backup`) inspect backup rules. The configuration correctly defines `<exclude>` for root paths, but lacks explicit `<exclude>` for specific internal device encryption credentials if `allowBackup` is ever dynamically altered in debug variants.
- **Suggested Fix**: Keep `allowBackup="false"` enforced unconditionally in debug builds as well.

---

## Area 2: Playback & Player Engine

### Critical Severity

#### [PLY-01] Critical: Unreleased ExoPlayer Instance and Coroutine Leak on Activity Recreation
- **File & Line**: [LNXVApplication.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/LNXVApplication.kt#L35-L38), [Media3PlayerManager.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/Media3PlayerManager.kt#L47-L50), [Media3PlayerManager.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/Media3PlayerManager.kt#L697-L704)
- **Concrete Failure Scenario**: `Media3PlayerManager` is initialized lazily as a process-level singleton in `LNXVApplication`. Its internal `scope = CoroutineScope(Dispatchers.Main + Job())` and `progressTrackingJob` run indefinitely with a `while (isActive) { ... delay(250L) }` loop. Because `release()` is never called from `MainActivity.onDestroy()` or `LNXVApplication.onTerminate()`, the player holding native audio track and codec hardware references never releases codec decoders. On repeated orientation changes or process backgrounding without audio playback, audio track buffer underruns and native MediaCodec binder leaks accumulate until the device audio server fails.
- **Suggested Fix**: Implement explicit lifecycle-bound teardown for ExoPlayer or ensure `release()` is cleanly hooked to application process teardown, with coroutine cancellation on idle states.

#### [PLY-02] Critical: Random-Access Seek Deadlock / OutOfMemory on Chunk Boundary in EncryptedDataSource
- **File & Line**: [EncryptedDataSource.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/EncryptedDataSource.kt#L35-L42), [ChunkedAead.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/security/ChunkedAead.kt#L145-L165)
- **Concrete Failure Scenario**: `EncryptedDataSource.read()` passes a request to `reader.read(position, buffer, offset, len)`. Inside `ChunkedAead.Reader`, when ExoPlayer seeks to an arbitrary byte position near the end of a chunk boundary, `loadChunk()` decrypts the entire chunk into `cached: ByteArray`. When `position` points past `plainSize - 1`, `read()` returns `-1`, but `open()` calculates `bytesRemaining` based on `minOf(dataSpec.length, available)`. When `dataSpec.length == C.LENGTH_UNSET` and `dataSpec.position == plainSize`, `available = 0`, causing `read()` to return `C.RESULT_END_OF_INPUT`, but if `dataSpec.position > r.plainSize`, it throws `IOException("Position out of range")`. During Matroska/MP4 moov box extraction, ExoPlayer seeks beyond known file length to check for trailing tags, which causes `EncryptedDataSource` to crash playback with an unhandled `IOException` instead of cleanly signaling EOF (`C.RESULT_END_OF_INPUT`).
- **Suggested Fix**: In `EncryptedDataSource.open()`, if `dataSpec.position >= r.plainSize`, return `0` with `bytesRemaining = 0L` instead of throwing an `IOException`.

---

### High Severity

#### [PLY-03] High: Silent Audio Processor Sample Desynchronization and Buffer Memory Leak
- **File & Line**: [AudioDelayProcessor.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/AudioDelayProcessor.kt#L45-L68)
- **Concrete Failure Scenario**: In `AudioDelayProcessor.queueInput()`, when inserting silence (`pendingFrames > 0`), it invokes `replaceOutputBuffer(silenceBytes + inputBuffer.remaining())`. If `pendingFrames` exceeds `MAX_SILENCE_FRAMES`, it chunks the silence, but does not slice the input buffer accordingly, outputting both the chunked silence AND the full `inputBuffer` in a single pass. Subsequent calls continue to inject remaining silence while the original audio samples have already passed down the pipeline, causing severe sample corruption, jitter, and high memory allocations in `replaceOutputBuffer`.
- **Suggested Fix**: When inserting silence exceeding the chunk size, do NOT consume or append `inputBuffer` until all pending silence frames have been completely dispatched.

#### [PLY-04] High: AudioEffectsController Equalizer Native Crash on Audio Session Teardown
- **File & Line**: [AudioEffectsController.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/AudioEffectsController.kt#L19-L30), [Media3PlayerManager.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/Media3PlayerManager.kt#L201-L204)
- **Concrete Failure Scenario**: When an audio track switches or the audio session ID changes, `onAudioSessionIdChanged` calls `audioEffects.attach(sessionId)`. In `AudioEffectsController.attach()`, `releaseEffects()` is called, immediately followed by creating a new `Equalizer(0, sessionId)` and `LoudnessEnhancer(sessionId)`. On various Android hardware platforms (Qualcomm/MediaTek HAL), creating an `Equalizer` immediately after releasing one on the same thread without giving the audio server time to detach causes a native `DEAD_OBJECT` or `IllegalArgumentException: Effect not found` in `AudioEffect.java`, crashing the player process.
- **Suggested Fix**: Wrap `Equalizer` and `LoudnessEnhancer` instantiations in defensive try-catch blocks with graceful fallbacks and exponential backoff retry.

#### [PLY-05] High: Main-Thread Disk I/O Blocking During Container Inspection in Stream Preparation
- **File & Line**: [SeekFixExtractorsFactory.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/SeekFixExtractorsFactory.kt#L107-L127)
- **Concrete Failure Scenario**: `SeekFixExtractorsFactory.createExtractors(uri, responseHeaders)` executes `findCuesHint(uri)` synchronously on whatever thread requests extractors. ExoPlayer's `DefaultMediaSourceFactory` initializes extractors during `prepare()` directly on the application main thread when called from Compose. `findCuesHint()` opens a `FileDescriptor`, creates a `ChannelSource`, and performs synchronous parsing across EBML clusters on the UI thread, causing severe UI freezes (ANRs) on high-latency storage or large 10GB+ MKV files.
- **Suggested Fix**: Offload container header inspection to an asynchronous background coroutine before calling `exoPlayer.setMediaItem()` or implement a non-blocking `Extractor` wrapper.

#### [PLY-06] High: Sleep Timer Coroutine Volume Reset Race Condition
- **File & Line**: [Media3PlayerManager.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/Media3PlayerManager.kt#L510-L536)
- **Concrete Failure Scenario**: When the sleep timer executes, `startSleepTimer()` launches a loop in `scope`. If the user manually pauses or closes the player while the timer is running, the loop continues sleeping until timeout. At lines 533-534, it calls `pause()` and resets `exoPlayer.volume = 1f`. If the user had opened another video and turned down the device volume or muted the player in the interim, the volume is abruptly forced back to `1.0f` at full volume in the background.
- **Suggested Fix**: Check `owner === currentScreen` and ensure `sleepTimerJob` is cancelled whenever playback is stopped or a new media session is loaded.

---

### Medium Severity

#### [PLY-07] Medium: Aspect Ratio Rational Overflow and Crash in PiP Manager
- **File & Line**: [PipManager.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/pip/PipManager.kt#L27-L40)
- **Concrete Failure Scenario**: `calculateAspectRatio(width, height)` creates an `android.util.Rational(safeWidth, safeHeight)`. If a video stream reports anamorphic or abnormal dimensions (e.g. corrupted header with height `0` or width `100000`), `safeWidth` / `safeHeight` can trigger an `IllegalArgumentException` in `PictureInPictureParams.Builder.setAspectRatio` if the rational is not strictly within `[0.418414, 2.390000]`. While lines 35-37 attempt to clamp, floating-point rounding precision in `rawRatio` can still violate the strict system bounds and throw an unhandled exception upon entering PiP.
- **Suggested Fix**: Explicitly clamp the `Rational` using integers `Rational(42, 100)` and `Rational(239, 100)`.

#### [PLY-08] Medium: A-B Repeat Loop Boundary Skips Seek Invalidation
- **File & Line**: [Media3PlayerManager.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/Media3PlayerManager.kt#L678-L682)
- **Concrete Failure Scenario**: A-B Repeat is evaluated in `startProgressTracking()` inside a polling loop with `delay(250L)`. At higher playback speeds (e.g., 2.0x - 4.0x), 250ms corresponds to 500ms-1000ms of playback. The player consistently overshoots `pointB` by up to a second before seeking back to `pointA`, creating an audio stutter and jarring visual repeat.
- **Suggested Fix**: Hook A-B repeat checks directly into ExoPlayer's playback pipeline using an `AnalyticsListener` or a custom `Player.Listener` checking frame presentation timestamps rather than a loose 250ms polling loop.

#### [PLY-09] Medium: External Subtitle Dynamic Delay Shifts Write to File on Main Thread
- **File & Line**: [Media3PlayerManager.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/Media3PlayerManager.kt#L246-L265)
- **Concrete Failure Scenario**: In `adjustSubtitleDelay()`, every step (+/-500ms) re-invokes `applyExternalSubtitle()`. Although subtitle text shifting runs on `Dispatchers.IO`, `exoPlayer.setMediaItem(..., position)` and `exoPlayer.prepare()` are immediately invoked on `Dispatchers.Main`. When tapping the step button rapidly, dozens of sequential subtitle preparations queue up, purging and recreating ExoPlayer's entire decoding pipeline and subtitle parser.
- **Suggested Fix**: Debounce subtitle delay adjustments with a 300ms window before reloading the media source in ExoPlayer.

#### [PLY-10] Medium: Audio Track Selection Overrides Do Not Reset on Media Item Switch
- **File & Line**: [Media3PlayerManager.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/Media3PlayerManager.kt#L550-L567)
- **Concrete Failure Scenario**: In `selectAudioTrack()`, `TrackSelectionOverride` is added to `trackSelector.parameters`. When the queue advances to the next video, the previous track selection override by group index remains active in `DefaultTrackSelector`. If the next video has a different number of audio groups, ExoPlayer fails to select an audio track or defaults to silence.
- **Suggested Fix**: Clear track overrides (`trackSelector.setParameters(trackSelector.buildUponParameters().clearOverrides())`) in `prepareAndPlay()` prior to loading new media.

---

### Low Severity

#### [PLY-11] Low: Key Action Seek Jumps Statically Without Respecting Sensitivity
- **File & Line**: [PlayerScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerScreen.kt#L273-L285), [PlayerKeyMap.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/gestures/PlayerKeyMap.kt#L26-L34)
- **Concrete Failure Scenario**: Keyboard arrow presses and hardware remote events call `seekRelative()` with fixed constants from `PlayerKeyMap`, ignoring the user's configured double-tap and gesture seek increments from Settings.
- **Suggested Fix**: Wire `PlayerKeyMap.seekDeltaMs()` to read `userPrefs.value.doubleTapSeekSeconds * 1000L`.

#### [PLY-12] Low: Missing Cleartext Traffic Error Recovery for DASH/HLS Streams
- **File & Line**: [Media3PlayerManager.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/Media3PlayerManager.kt#L212-L214)
- **Concrete Failure Scenario**: When opening an `http://` network stream on Android 9+, `ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED` is posted to `playbackError`, but the user is not given an option or explanation regarding network security policies.
- **Suggested Fix**: Display a dedicated dialog offering to retry with HTTPS or explaining cleartext traffic restrictions.

---

## Area 3: Data Layer & State Management

### Critical Severity

#### [DAT-01] Critical: Partial Hash Collision Leading to Erroneous Duplicate Video File Deletion
- **File & Line**: [PartialHash.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/util/PartialHash.kt#L18-L37), [HomeViewModel.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeViewModel.kt#L195-L205)
- **Concrete Failure Scenario**: `PartialHash.tryCompute()` computes a SHA-256 hash over only 192 KiB (start, middle, and end chunks) and the file length. If a user has recorded multiple video clips of identical length, resolution, and container format (such as dashcam footage, surveillance clips, or benchmark test files) where the middle and ends happen to match static headers or solid black frames, `PartialHash` yields identical hashes. `HomeViewModel.selectCollection(SmartCollection.DUPLICATES)` marks these as duplicates. The user is then prompted to delete "duplicates" from the device, leading to irreversible loss of unique video recordings.
- **Suggested Fix**: Clearly mark PartialHash as a preliminary heuristic in the UI and require a full byte-by-byte or full-file SHA-256 confirmation before enabling destructive batch deletion.

#### [DAT-02] Critical: Mass Deletion via Room Sync When MediaStore Returns Empty Result
- **File & Line**: [VideoRepository.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/data/repository/VideoRepository.kt#L45-L67)
- **Concrete Failure Scenario**: In `VideoRepository.refreshLibrary()`, the scanner reads `scanner.scanLocalVideos()`. If storage permissions are temporarily withheld or a transient MediaStore error occurs such that `scanned` is empty, line 46 has an early exit: `if (scanned.isEmpty()) return@withContext`. However, if MediaStore returns a partial list (e.g. external SD card unmounted, reporting 5 videos instead of 500), `scannedIds` contains only those 5 IDs. Lines 63-66 immediately execute `videoDao.deleteVideosByIds(it)` for all remaining 495 videos, completely obliterating all user favorites, watch history, last played timestamps, and resume points.
- **Suggested Fix**: Do not delete missing videos during simple refreshes; instead, mark them with a `missing` flag or check if the storage volume is still mounted.

---

### High Severity

#### [DAT-03] High: Room Database Schema Export Disabled and Missing Foreign Key Cascade Indices
- **File & Line**: [LNXVDatabase.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/database/LNXVDatabase.kt#L10-L22), [PlaylistEntities.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/database/PlaylistEntities.kt#L19-L24)
- **Concrete Failure Scenario**: `LNXVDatabase` specifies `exportSchema = false`. Room cannot record migration history in version control, making future migrations error-prone. Additionally, `playlist_items` defines a composite primary key `(playlistId, videoId)` with no foreign keys or indices on `videoId`. When `playlistDao.observePlaylists()` executes a `COUNT(*)` subquery joining `playlist_items` and `videos` for every playlist, SQLite performs a full table scan across `playlist_items` on every query, causing severe UI lag when opening the Playlists tab.
- **Suggested Fix**: Set `exportSchema = true` and configure `foreignKeys` with cascade deletion and an index on `videoId`: `@Entity(tableName = "playlist_items", indices = [Index("videoId")])`.

#### [DAT-04] High: MediaStore Projection Ignores Android 14 Selected Media Access
- **File & Line**: [MediaStoreScanner.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/data/mediastore/MediaStoreScanner.kt#L22-L41), [MainActivity.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/MainActivity.kt#L254-L266)
- **Concrete Failure Scenario**: On Android 14 (API 34), users can grant partial media access via `READ_MEDIA_VISUAL_USER_SELECTED`. `MainActivity` only requests `READ_MEDIA_VIDEO` and `READ_MEDIA_AUDIO`. When the user selects "Select photos and videos" in the system permission dialog, `isGranted` evaluates to false because `READ_MEDIA_VISUAL_USER_SELECTED` is not included in the requested permissions, preventing `refreshLibrary()` from running and leaving the library blank despite the user granting access to specific videos.
- **Suggested Fix**: Add `<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />` to `AndroidManifest.xml` and include it in `checkAndRequestPermissions()`.

#### [DAT-05] High: Process Death State Loss in All ViewModels Due to Missing SavedStateHandle
- **File & Line**: [PlayerViewModel.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerViewModel.kt#L57-L70), [HomeViewModel.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeViewModel.kt#L58-L68)
- **Concrete Failure Scenario**: Neither `PlayerViewModel` nor `HomeViewModel` accepts a `SavedStateHandle`. When Android terminates the backgrounded app under memory pressure (low RAM), all transient state (current selection set, open folder path, search query, dialog visibility, and video queue) is wiped out. When the user returns to the app, the UI abruptly resets to the root Videos tab.
- **Suggested Fix**: Inject `SavedStateHandle` into all ViewModels via Koin and persist critical navigation/selection state across process death.

---

### Medium Severity

#### [DAT-06] Medium: Coroutine Leak in ContinueWidget Broadcast Receiver
- **File & Line**: [ContinueWidget.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/widget/ContinueWidget.kt#L19-L35)
- **Concrete Failure Scenario**: In `ContinueWidget.onUpdate()`, a new coroutine is launched via `CoroutineScope(Dispatchers.IO).launch`. If the database query hangs or delays beyond the broadcast receiver's execution window, `goAsync().finish()` may not be reached before the OS terminates the receiver process, resulting in widget ANRs and stale UI data.
- **Suggested Fix**: Tie widget updates to an application-scoped supervisor job or structured `WorkManager` task with a strict timeout.

#### [DAT-07] Medium: DataStore Read Deadlock Risk on App Startup
- **File & Line**: [LNXVApplication.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/LNXVApplication.kt#L54-L56), [MainActivity.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/MainActivity.kt#L70-L80)
- **Concrete Failure Scenario**: `MainActivity` holds the Android Splash Screen indefinitely until `prefsState.value != null`. If the DataStore file on disk is corrupted, locked by another thread, or throws a serialization exception during `dataStore.userPreferencesFlow.collect`, `prefsState` is never populated, permanently hanging the app on the splash screen without an error message.
- **Suggested Fix**: Implement `.catch { emit(UserPreferences()) }` on the DataStore flow and set a 3-second splash screen timeout fallback.

#### [DAT-08] Medium: Unbounded Memory Allocation in MediaStoreScanner for Large Video Libraries
- **File & Line**: [MediaStoreScanner.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/data/mediastore/MediaStoreScanner.kt#L45-L95)
- **Concrete Failure Scenario**: On devices with 10,000+ video clips (e.g. high-capacity SD cards), `scanLocalVideos()` instantiates thousands of `VideoEntity` objects in memory simultaneously before writing them to SQLite. This causes high garbage collection pressure and transient out-of-memory crashes on low-RAM devices during startup indexing.
- **Suggested Fix**: Batch cursor rows in chunks of 200 and insert them progressively using a paging sequence.

---

### Low Severity

#### [DAT-09] Low: Folder Extraction Redundantly Traverses Filesystem via Java IO File
- **File & Line**: [MediaStoreScanner.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/data/mediastore/MediaStoreScanner.kt#L77-L81)
- **Concrete Failure Scenario**: In the cursor loop, `File(path).parentFile` is invoked for every row. On modern Android versions with FUSE storage emulation, invoking `java.io.File` stat calls inside tight database loops adds substantial latency compared to string path manipulation.
- **Suggested Fix**: Use string manipulation (`path.substringBeforeLast('/', "")`) instead of constructing `java.io.File` instances.

#### [DAT-10] Low: SmartCollection Filter Recalculates Timestamps on Every Item
- **File & Line**: [HomeViewModel.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeViewModel.kt#L134-L140)
- **Concrete Failure Scenario**: `System.currentTimeMillis() / 1000L` is re-evaluated across large lists repeatedly during search query filtering.
- **Suggested Fix**: Cache the epoch second once before initiating the collection filter.

---

## Area 4: UI, Compose Performance & Accessibility

### Critical Severity

#### [UI-01] Critical: Excessive Recomposition Storm via High-Frequency Position State Reads
- **File & Line**: [PlayerControlsOverlay.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerControlsOverlay.kt#L232-L285), [PlayerViewModel.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerViewModel.kt#L64-L72)
- **Concrete Failure Scenario**: In `PlayerControlsOverlay.kt`, `val positionMs by viewModel.positionMs.collectAsState()` is read directly at the top level of the bottom controls composable. Every 250ms (or faster during seeking), `positionMs` emits a new value, forcing the entire bottom control hierarchy, title texts, buttons, and layout containers to recompose 4 times per second, causing severe frame drops and battery drain during playback.
- **Suggested Fix**: Pass `positionMs: () -> Long` as a lambda to leaf nodes or use `Modifier.drawWithContent` / derived state so only the seek bar track re-renders.

---

### High Severity

#### [UI-02] High: Complete Inaccessibility Due to Missing Content Descriptions on Custom Touch Targets
- **File & Line**: [VaultScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/vault/VaultScreen.kt#L450-L480), [HomeComponents.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeComponents.kt#L393-L400)
- **Concrete Failure Scenario**: In `VaultScreen.kt`, the custom PIN keypad buttons use `contentDescription = "Enter digit $key"`, but the backspace button lacks an explicit localized content description or relies on missing semantics. In `ContinueWatchingCard` ([HomeComponents.kt:394](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeComponents.kt#L394)), the floating resume play icon is hardcoded as `contentDescription = "Resume"`, completely inaccessible to screen reader users using Hindi or other locales.
- **Suggested Fix**: Provide localized strings (`stringResource(R.string.cd_...)`) for all icon buttons and add `semantics { role = Role.Button }` to all clickable surfaces.

#### [UI-03] High: Destructive Playlist and Vault Operations Missing Two-Step Confirmations
- **File & Line**: [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L353-L365), [HomeComponents.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeComponents.kt#L244-L258)
- **Concrete Failure Scenario**: Tapping "Move to Private Vault" in `HomeComponents.kt` immediately triggers encryption and requests original deletion without any confirmation dialog or undo option. If a user accidentally mis-clicks the options menu, their video is abruptly encrypted and locked away.
- **Suggested Fix**: Display a `GlassDialog` asking the user to confirm moving the video into the vault before dispatching `moveToVault()`.

#### [UI-04] High: Hardcoded Non-Dynamic Colors Violating Light Theme and Contrast Requirements
- **File & Line**: [PlayerPanels.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerPanels.kt#L94-L97), [PlayerControlsOverlay.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerControlsOverlay.kt#L94-L96)
- **Concrete Failure Scenario**: Throughout `PlayerPanels.kt` and `PlayerControlsOverlay.kt`, background and text colors are hardcoded as static ARGB literals (`Color(0xF2202A3E)`, `Color(0x80FFFFFF)`, `Color(0xFFB0B8C8)`). In light theme or high-contrast accessibility modes, these colors fail WCAG AAA contrast ratios and ignore Material You / system dynamic color preferences.
- **Suggested Fix**: Replace static hex definitions with semantic tokens from `MaterialTheme.colorScheme` and `appColors`.

#### [UI-05] High: Touch Targets Under 48dp on Primary Navigation and Action Buttons
- **File & Line**: [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L807-L822), [GlassKit.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/design/GlassKit.kt#L540-L550)
- **Concrete Failure Scenario**: In `HomeScreen.kt`, `SelectionAction` buttons have a total height and width smaller than 40dp without touch target expansion, violating Android accessibility guidelines requiring a minimum 48x48dp touch target. Similarly, `GlassStepper` buttons measure 44x34dp, making them difficult to target for users with motor impairments.
- **Suggested Fix**: Add `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` to all clickable control wrappers.

---

### Medium Severity

#### [UI-06] Medium: Overlapping System IME and Edge-to-Edge Keyboard Gaps in GlassDialog
- **File & Line**: [GlassKit.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/design/GlassKit.kt#L430-L473), [VaultScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/vault/VaultScreen.kt#L110-L155)
- **Concrete Failure Scenario**: `GlassDialog` uses `androidx.compose.ui.window.Dialog`. When the software keyboard (IME) appears for PIN or playlist input, the dialog does not apply `imePadding()`, causing the keyboard to obscure the text fields and action buttons on smaller screens.
- **Suggested Fix**: Apply `Modifier.imePadding()` to the root container of `GlassDialog`.

#### [UI-07] Medium: Lack of 200% Font Scale Adaptation in Player Dialogs and Side Panels
- **File & Line**: [PlayerPanels.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerPanels.kt#L277-L289)
- **Concrete Failure Scenario**: When system font scale is set to 200% (Android Accessibility), panel titles and property labels with fixed height constraints (`maxLines = 1`) truncate or overlap adjacent icons, making options unreadable.
- **Suggested Fix**: Remove hardcoded heights and allow text containers to wrap flexibly or scale down font size gracefully.

#### [UI-08] Medium: Broken Predictive Back Gesture Animation in Compose Navigation
- **File & Line**: [LNXVNavigationHost.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/navigation/LNXVNavigationHost.kt#L52-L75), [MainActivity.kt](file:///tmp/LNXV_repo/app/src/main/AndroidManifest.xml#L32)
- **Concrete Failure Scenario**: `AndroidManifest.xml` enables `android:enableOnBackInvokedCallback="true"`, but `LNXVNavigationHost` uses fixed slide and scale transitions (`slideInHorizontally`, `scaleIn`) without handling predictive back progress APIs, causing visual snapping when a predictive back gesture is cancelled.
- **Suggested Fix**: Upgrade to Navigation Compose predictive back APIs or adjust transitions to support gesture-driven cancellation.

#### [UI-09] Medium: Hardcoded Raw String Literals Bypassing Localization
- **File & Line**: [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L153), [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L186), [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L716-L720)
- **Concrete Failure Scenario**: Strings such as `"Add to playlist"`, `"Open network stream"`, `"← Back"`, `"Rename"`, and `"Play all"` are hardcoded in English, ignoring the active device locale (e.g. Hindi `values-hi/strings.xml`).
- **Suggested Fix**: Extract all string literals to `res/values/strings.xml` and provide corresponding translations in `res/values-hi/strings.xml`.

#### [UI-10] Medium: Layout Overlap in Landscape Orientation on Dual-Screen/Foldable Devices
- **File & Line**: [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L415-L467)
- **Concrete Failure Scenario**: Top and bottom glass bars calculate fixed insets and padding (`padding(top = 44.dp)`) rather than querying window insets dynamically (`WindowInsets.statusBars` / `WindowInsets.displayCutout`), causing the title bar to clip under camera cutouts or foldable display hinges.
- **Suggested Fix**: Use `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` on the top bar instead of hardcoded padding.

---

### Low Severity

#### [UI-11] Low: Unhandled Division by Zero in Video Aspect Ratio Calculation
- **File & Line**: [PipManager.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/pip/PipManager.kt#L30-L32), [PlayerScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerScreen.kt#L258)
- **Concrete Failure Scenario**: If `size.width` or `size.height` evaluates to `0f` during layout transition, calculating `minOf(size.width / size.height, size.height / size.width)` can produce `Float.NaN` or `Infinity`, leading to unpredictable matrix scaling.
- **Suggested Fix**: Validate `size.width > 0f && size.height > 0f` before calculating aspect division.

#### [UI-12] Low: Missing Directional RTL Layout Alignment on Player Controls
- **File & Line**: [PlayerControlsOverlay.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerControlsOverlay.kt#L140-L163)
- **Concrete Failure Scenario**: Fast-forward and rewind icons are hardcoded with directional arrows that do not invert under Right-to-Left (RTL) locales, causing playback direction to conflict with user expectations.
- **Suggested Fix**: Mirror rewind/fast-forward vectors under RTL layouts using `Modifier.graphicsLayer { scaleX = if (isRtl) -1f else 1f }`.

#### [UI-13] Low: Non-Standard Spring Dampening Producing Jitter on Low-Refresh Displays
- **File & Line**: [LiquidGlass.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/design/LiquidGlass.kt#L125-L132)
- **Concrete Failure Scenario**: Spring stiffness set to `560f` produces noticeable micro-stuttering on 60 Hz budget displays.
- **Suggested Fix**: Standardize on `Spring.StiffnessMediumLow` for glass press animations.

---

## Area 5: Build, CI/CD, Dependencies & Tests

### Critical Severity

#### [BLD-01] Critical: Automated Release Publishing on Every Push to Main with Zero Test Gate
- **File & Line**: [build-apk.yml](file:///tmp/LNXV_repo/.github/workflows/build-apk.yml#L104-L115), [build-apk.yml](file:///tmp/LNXV_repo/.github/workflows/build-apk.yml#L44-L46), [build-apk.yml](file:///tmp/LNXV_repo/.github/workflows/build-apk.yml#L63-L75)
- **Concrete Failure Scenario**: In GitHub Actions `build-apk.yml`, the release job runs on EVERY push to `main` (`on: push: branches: [main]`). Crucially:
  1. Line 44 sets `continue-on-error: true` on Android Lint.
  2. Line 63 sets `continue-on-error: true` on Detekt and Ktlint.
  3. Lines 104-115 publish a public GitHub Release with `files: LNXV.apk` tagged as `v1.0.${{ github.run_number }}`.
  Any commit pushed directly to `main` immediately cuts and publishes an official release to all users worldwide via the in-app updater, even if lint or static analysis completely failed, or if tests were partially skipped.
- **Suggested Fix**: Add a strict gate: releases must only be triggered by semantic tags (`v*.*.*`), require manual approval, and depend on blocking test/lint jobs.

#### [BLD-02] Critical: Insecure Curl Download of Binary Tools Without Checksum Verification in CI
- **File & Line**: [build-apk.yml](file:///tmp/LNXV_repo/.github/workflows/build-apk.yml#L66-L70), [dependency-scan.yml](file:///tmp/LNXV_repo/.github/workflows/dependency-scan.yml#L20-L24)
- **Concrete Failure Scenario**: The CI scripts download binary executable tools directly from external URLs without any hash verification:
  ```bash
  curl -fsSL -o "$RUNNER_TEMP/sa/detekt.jar" https://github.com/detekt/detekt/releases/download/v1.23.7/detekt-cli-1.23.7-all.jar
  curl -fsSL -o "$RUNNER_TEMP/sa/ktlint" https://github.com/pinterest/ktlint/releases/download/1.3.1/ktlint
  curl -fsSL -o "$RUNNER_TEMP/odc/odc.zip" https://github.com/jeremylong/DependencyCheck/releases/download/v10.0.4/dependency-check-10.0.4-release.zip
  ```
  If any of these GitHub accounts, release assets, or transit CDNs are compromised, malicious code executes directly on the CI runner with full access to repository secrets (`KEYSTORE_B64`, `KS_PASS`, `GITHUB_TOKEN`).
- **Suggested Fix**: Verify SHA-256 checksums of all downloaded CLI tools before execution, or integrate them as verified Gradle plugins.

#### [BLD-03] Critical: VersionCode Tied Directly to GitHub Run Number
- **File & Line**: [app/build.gradle.kts](file:///tmp/LNXV_repo/app/build.gradle.kts#L18-L20)
- **Concrete Failure Scenario**: `versionCode = 1000 + (System.getenv("VERSION_CODE")?.toIntOrNull() ?: 0)`. The `VERSION_CODE` is supplied by `github.run_number`. If the repository workflow is re-run, reset, or migrated to a different repository, `run_number` resets or collides. On the Google Play Store, `versionCode` must be strictly monotonically increasing; colliding or lower version codes cause immediate upload rejection.
- **Suggested Fix**: Base `versionCode` on a dedicated monotonically increasing build counter or git commit count (`git rev-list --count HEAD`).

---

### High Severity

#### [BLD-04] High: Production Code Coverage Gap (58 Classes Completely Untested)
- **File & Line**: [app/src/test](file:///tmp/LNXV_repo/app/src/test/java/com/lnxv/player)
- **Concrete Failure Scenario**: An exhaustive comparison between `app/src/main/java` and `app/src/test/java` reveals that **58 out of 86 production Kotlin classes have zero unit tests**. Untested components include critical modules:
  - `VaultRepository` (encryption, decryption, file cleanup, wipe)
  - `VaultTransferManager` (concurrency and queue management)
  - `Media3PlayerManager` (audio routing, lifecycle, seeking)
  - `EncryptedDataSource` & `VaultRoutingDataSource` (cryptographic stream playback)
  - `MediaStoreScanner` & `VideoRepository` (database sync and deletion logic)
  - All ViewModels: `PlayerViewModel`, `HomeViewModel`, `VaultViewModel`, `SettingsViewModel`
- **Suggested Fix**: Implement comprehensive unit and repository tests using JUnit4, MockK/fakes, and Robolectric for all data, repository, and player management classes.

#### [BLD-05] High: R8 Stripping Risk on Reflection-Based Media3 MKV Seek Workaround
- **File & Line**: [proguard-rules.pro](file:///tmp/LNXV_repo/app/proguard-rules.pro#L4-L8), [SeekFixExtractorsFactory.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/playback/engine/SeekFixExtractorsFactory.kt#L41-L49)
- **Concrete Failure Scenario**: `SeekFixMatroskaExtractor` accesses a private field by reflection: `MatroskaExtractor::class.java.getDeclaredField("cuesContentPosition")`. While `proguard-rules.pro` includes `-keep class androidx.media3.extractor.mkv.MatroskaExtractor { private long cuesContentPosition; }`, it fails to preserve constructor and inner class structures if Media3 is updated or if R8 performs aggressive member inlining. When the reflection fails, the seek fix silently breaks, causing files to restart from 0 without user notification.
- **Suggested Fix**: Use official Media3 seek index APIs or contribute a patch upstream rather than relying on private reflection on internal library fields.

#### [BLD-06] High: Vulnerable / Fragile Sideload ABI Packaging Rules
- **File & Line**: [app/build.gradle.kts](file:///tmp/LNXV_repo/app/build.gradle.kts#L30-L36)
- **Concrete Failure Scenario**: The sideload flavor comments state `// sideload: self-updating APK (arm64 only to keep it small)`, but the build script contradicts this: `// Do not restrict ABIs: package every ABI supplied by the app/dependencies`. This results in bloated universal APKs containing unneeded native libraries for multiple architectures, increasing download size and update bandwidth significantly.
- **Suggested Fix**: Use ABI splits (`splits.abi`) or produce architecture-specific release artifacts (`arm64-v8a`, `armeabi-v7a`, `x86_64`).

---

### Medium Severity

#### [BLD-07] Medium: Profileable Shell Enabled in Release Configuration
- **File & Line**: [AndroidManifest.xml](file:///tmp/LNXV_repo/app/src/main/AndroidManifest.xml#L40)
- **Concrete Failure Scenario**: `<profileable android:shell="true" tools:targetApi="29" />` is declared at the root of `AndroidManifest.xml` without being scoped to debug/benchmark builds. This allows any user or local process via ADB on Android 10+ to profile memory, CPU, and internal execution paths of the production release app.
- **Suggested Fix**: Move the `<profileable>` tag to `app/src/benchmark/AndroidManifest.xml` or `app/src/debug/AndroidManifest.xml`.

#### [BLD-08] Medium: Outdated Dependencies in Version Catalog
- **File & Line**: [libs.versions.toml](file:///tmp/LNXV_repo/gradle/libs.versions.toml#L2-L20)
- **Concrete Failure Scenario**: Multiple libraries are pinned to older releases with known issues:
  - `agp = "8.3.1"` (current stable: 8.7+)
  - `kotlin = "2.0.0"`
  - `media3 = "1.4.1"`
  - `koin = "3.5.6"`
  - `leakcanary = "2.14"`
- **Suggested Fix**: Regularly update dependencies using Dependabot or Renovate and verify against Android 15 (API 35) SDK requirements.

---

### Low Severity

#### [BLD-09] Low: False and Stale Documentation Claims in README.md
- **File & Line**: [README.md](file:///tmp/LNXV_repo/README.md#L1-L100), [PROGRESS.md](file:///tmp/LNXV_repo/PROGRESS.md#L1-L100)
- **Concrete Failure Scenario**: The README claims "Full production test coverage" and "Device-verified hardware playback", whereas the codebase documentation explicitly admits the app has **never been device-tested** and 58 core production classes lack tests.
- **Suggested Fix**: Update `README.md` to accurately reflect test coverage metrics and hardware validation status.

#### [BLD-10] Low: Redundant Dependency Declarations in Build Script
- **File & Line**: [app/build.gradle.kts](file:///tmp/LNXV_repo/app/build.gradle.kts#L98-L103)
- **Concrete Failure Scenario**: `androidx.profileinstaller:profileinstaller:1.3.1` is declared twice in `dependencies {}`.
- **Suggested Fix**: Remove duplicate dependency declaration line 98.

---

## Area 6: Glass Effect & Animation Consistency

### High Severity

#### [GLS-01] High: VaultScreen Uses Translucent Fill Without Haze Backdrop Blur Source
- **File & Line**: [VaultScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/vault/VaultScreen.kt#L181-L192), [VaultScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/vault/VaultScreen.kt#L282-L296)
- **Concrete Failure Scenario**: `VaultScreen` wraps its top bar and cards in `.liquidGlassSurface(...)`, but neither the root scaffold nor `LazyColumn` attaches a `dev.chrisbanes.haze.HazeState` via `Modifier.haze(hazeState)`. Consequently, all glass surfaces in the vault appear as flat, non-blurred translucent boxes, completely breaking the iOS liquid glass aesthetic present on the Home screen.
- **Suggested Fix**: Instantiate `remember { HazeState() }`, attach `Modifier.haze(hazeState)` to the `LazyColumn`, and pass `hazeState` to `GlassTopBar` and card containers via `Modifier.glassBlur(hazeState, shape)`.

#### [GLS-02] High: Player Pill Buttons Lack Real Backdrop Blur
- **File & Line**: [PlayerControlsOverlay.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerControlsOverlay.kt#L319-L325), [PlayerControlsOverlay.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/player/PlayerControlsOverlay.kt#L340-L353)
- **Concrete Failure Scenario**: `GlassPill`, `GlassCircle`, and `GlassChip` in the video player overlay define their backgrounds using a flat translucent color `backgroundColor = PillFill` (`Color(0x552A2F38)`). They do not sample from `BackdropCapture` or use Haze blur, resulting in plain dark rounded rectangles rather than authentic frosted liquid glass.
- **Suggested Fix**: Feed the captured video backdrop texture (`glassBackdrop`) into `GlassPill` and `GlassCircle` shader passes or apply a real-time blur modifier.

#### [GLS-03] High: Complete Absence of animateItem on All LazyColumns and LazyGrids
- **File & Line**: [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L469-L503), [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L576-L612), [VaultScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/vault/VaultScreen.kt#L282-L343)
- **Concrete Failure Scenario**: Across `HomeScreen` (Videos, Folders, Playlists, Recents) and `VaultScreen`, every `LazyColumn` and `LazyRow` renders list items without `Modifier.animateItem()`. When sorting options change, filter chips are clicked, or videos are deleted/moved, items snap instantly with zero layout animation, jarring the user.
- **Suggested Fix**: Add `Modifier.animateItem()` to all items in `HomeScreen.kt` and `VaultScreen.kt`.

---

### Medium Severity

#### [GLS-04] Medium: Home Screen Uses Plain Material FilterChip and LinearProgressIndicator
- **File & Line**: [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L560-L565), [HomeComponents.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeComponents.kt#L208-L214), [HomeComponents.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeComponents.kt#L413-L422)
- **Concrete Failure Scenario**: While the rest of the application adopts custom iOS-style glassmorphic capsules, `HomeScreen` relies on standard Material 3 `androidx.compose.material3.FilterChip` for collection selection, and `HomeComponents` uses standard Material 3 `LinearProgressIndicator` for watch progress, creating an inconsistent visual hybrid of stock Material 3 and custom glass.
- **Suggested Fix**: Replace `FilterChip` with `IosPill` or `GlassChip`, and replace `LinearProgressIndicator` with `GlassProgress`.

#### [GLS-05] Medium: Home Selection Bar Pops Without Entry/Exit Animation
- **File & Line**: [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L310-L414)
- **Concrete Failure Scenario**: When entering multi-selection mode (`selection.isNotEmpty()`), the bottom bar switches immediately from `IosTabBar` to the selection action capsule inside `bottomBar = { ... }` without an `AnimatedContent` or `AnimatedVisibility` transition, causing the bottom bar to flicker and abruptly pop into view.
- **Suggested Fix**: Wrap the bottom bar content in `AnimatedContent` with slide/fade transitions: `slideInVertically { it } togetherWith slideOutVertically { it }`.

#### [GLS-06] Medium: Grid/List Mode Toggle Snaps Without Transition
- **File & Line**: [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L576-L612)
- **Concrete Failure Scenario**: Switching between Grid and List modes instantaneously replaces chunked rows with single rows without crossfade or size transitions, causing visual jarring during layout restructuring.
- **Suggested Fix**: Wrap the mode switch branch in `AnimatedContent` using `fadeIn()` and `fadeOut()`.

#### [GLS-07] Medium: GlassDialog Lacks Scale/Fade Entrance Animation
- **File & Line**: [GlassKit.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/core/design/GlassKit.kt#L421-L473)
- **Concrete Failure Scenario**: `GlassDialog` uses stock Compose `androidx.compose.ui.window.Dialog`. The internal `Column` lacks spring-based entrance scaling (`scaleIn(initialScale = 0.9f)`), making iOS-style glass dialogs pop in stiffly compared to native iOS alerts.
- **Suggested Fix**: Wrap dialog content in an `AnimatedVisibility` block with `scaleIn(spring(dampingRatio = 0.8f))` and `fadeIn()`.

---

### Low Severity

#### [GLS-08] Low: Home Screen ViewAgenda and Sort Buttons Use Plain IconButton
- **File & Line**: [HomeScreen.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeScreen.kt#L521-L551)
- **Concrete Failure Scenario**: The grid/list toggle and sort buttons in the "All Videos" header use bare Material `IconButton` without glass backing or spring scale haptics, unlike other action icons.
- **Suggested Fix**: Replace with `GlassIconButton`.

#### [GLS-09] Low: Folder Row Icon Backing Uses Hardcoded Alpha Accent Fill
- **File & Line**: [HomeComponents.kt](file:///tmp/LNXV_repo/app/src/main/java/com/lnxv/player/ui/home/HomeComponents.kt#L284-L292)
- **Concrete Failure Scenario**: Folder items render icons on `appColors.accent.copy(alpha = 0.15f)` rather than utilizing standard `glassSubtle` or `glassFill` surfaces.
- **Suggested Fix**: Unify folder icon backgrounds with standard `liquidGlassSurface` styling.

---

## Top 15 Fixes in Priority Order

1. **[SEC-01] Bind Biometric Authentication to Keystore via CryptoObject**: Prevent client-side authentication bypasses by requiring hardware-backed cipher verification in `VaultRepository.unlockWithBiometric()`.
2. **[SEC-02] Eliminate Vault Deletion Race Condition in moveToVault**: Prevent permanent file loss on process termination by committing database references before removing orphan safety tags.
3. **[DAT-01] Halt Destructive Deletion on PartialHash Heuristic**: Prevent accidental data destruction from duplicate false positives by requiring full cryptographic hash verification before user confirmation.
4. **[BLD-01] Gate CI/CD Release Publishing Behind Passing Tests & Tags**: Cease publishing public releases on every commit to `main`; enforce strict semantic release tagging and blocking test/lint checks.
5. **[BLD-02] Enforce Checksum Verification on External CI Downloads**: Eliminate supply-chain attack vectors by verifying SHA-256 hashes of detekt, ktlint, and ODC before execution.
6. **[PLY-01] Fix ExoPlayer Instance and Coroutine Leak**: Ensure `Media3PlayerManager` releases player resources and cancels continuous background polling loops during lifecycle teardown.
7. **[UI-01] Eliminate Recomposition Storms in Player Controls**: Decouple high-frequency playback position emissions (`positionMs`) from static UI component composition trees.
8. **[SEC-05] Implement Memory Zeroing for Master Keys in ChunkedAead**: Zero all decrypted key copies upon reader closure to eliminate plaintext cryptographic keys in memory dumps.
9. **[SEC-04] Block Screenshots and PixelCopy on Vault Playback**: Prevent vault video leaks by disabling `ScreenshotHelper.capture()` when playing private content.
10. **[DAT-02] Safeguard VideoRepository from Mass MediaStore Deletions**: Prevent library database wipes when MediaStore returns partial or transient empty results.
11. **[DAT-04] Support Android 14 Visual Media Selected Permissions**: Request `READ_MEDIA_VISUAL_USER_SELECTED` to support Android 14 partial photo/video permissions.
12. **[PLY-05] Offload Container Inspection from Main Thread**: Move `SeekFixExtractorsFactory.findCuesHint()` off the UI thread to resolve startup ANRs on large MKV files.
13. **[GLS-01] Supply HazeState Blur Source to VaultScreen**: Restore the intended liquid glass visual design to the encrypted vault by connecting `HazeState` to list and top bar modifiers.
14. **[GLS-03] Enable animateItem Across All Lazy Lists**: Provide smooth visual feedback during sorting, searching, and deleting by adding `Modifier.animateItem()`.
15. **[BLD-04] Author Comprehensive Unit Tests for Untested Data/Player Classes**: Expand unit test coverage across the 58 untested production classes, focusing on `VaultRepository` and `EncryptedDataSource`.

---

## Things Checked and Found Correct

During the audit, several architectural and cryptographic components were verified to be sound and well-implemented:
- **AES-256-GCM Streaming Chunk Implementation (`ChunkedAead.kt`)**: Nonce generation properly combines a 4-byte random prefix from the header with an 8-byte big-endian chunk index. Associated Authenticated Data (AAD) binds the header, chunk index, and the final chunk flag, successfully thwarting chunk reordering, truncation, and extension attacks.
- **PIN PBKDF2 Password Hashing (`PinHasher.kt`)**: Properly implements `PBKDF2WithHmacSHA256` using 210,000 iterations and a 16-byte cryptographically secure random salt, with constant-time equality comparisons (`MessageDigest.isEqual`) preventing timing attacks.
- **Network Security Configuration (`network_security_config.xml`)**: Cleartext traffic is disabled by default (`cleartextTrafficPermitted="false"`) with trust anchors strictly pinned to system certificates.
- **Backup & Extraction Protections (`data_extraction_rules.xml`, `backup_rules.xml`)**: Device-to-device transfers and cloud backups comprehensively exclude private databases, shared preferences, root files, and vault storage directories.
- **FileProvider Path Scoping (`file_paths.xml`)**: Strictly restricts shared paths to the updater cache directory (`cache-path name="updates" path="updates/"`), avoiding broad root or external storage sharing.
- **Seek Arithmetic Logic (`SeekMath.kt`)**: Properly bounds seek clamps against unknown durations (`C.TIME_UNSET`) and handles relative seek wrapping without negative index errors.
- **Matroska EBML Workaround Integrity (`MkvInspector.kt`)**: Efficiently traverses EBML headers, skipping cluster payloads without reading full files into memory, accurately locating late SeekHead Cues.
