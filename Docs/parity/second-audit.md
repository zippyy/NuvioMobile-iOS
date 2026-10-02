# Independent second audit: coverage and overlooked behavior

**Verdict: exhaustive file accounting, NOT exhaustive behavioral parity coverage.** Every tracked source path is classified and every original unresolved path has a full-text read or explicit binary metadata limitation. Loading text, parsing resources, expanding shorthand or finding a symbol is not proof of behavior. No build, test, live account, or device run was performed.

## Programmatically verified totals

- Source: `b9408cd5729fd99e9afe56cc334973fe0725bc7d`; target branch `port/nuvio-reshaped-ios`.
- **1760 tracked paths / 1760 unique per-file records**; no missing/extra paths.
- Original unresolved list: **898 / 898 accounted for**, including 836 full-text reads and 62 binary metadata reads.
- Initial `audited_files` assertions resolve to 1003 literal paths; 757 remain without such an assertion. These are **claims, not independently verified semantic coverage**.
- Independent second-pass semantic/resource backlog: **636 explicit paths**. Across the whole source, 1383 paths lack independently completed semantic/visual/binary proof in this pass (inherited audits are not thereby invalidated).
- All-source classification: {"config": 75, "platform-only": 35, "behavioral": 1320, "read-resource": 330}.
- Original 898 classification: {"platform-only": 34, "behavioral": 573, "config": 15, "read-resource": 276}.
- Findings: **14 granular gaps/refinements**, 7 missing and 7 partial at read snapshot.

## Normalization corrections

- Tracking shorthand says 12 files, directory actually contains **18**; Simkl shorthand says 16, directory contains **32**. Exact expanded members and original strings are in JSON. Expansion must not be represented as evidence each member was semantically read by the first audit.
- Uniquely relocated `ProtectedProfilePreferences`, update preferences, and Dolby Vision policy paths are recorded with original invalid tokens and separate resolved paths, not silently repaired. Generic `AppFeaturePolicy.kt` relocation is ambiguous across full/playstore flavors and receives no extra credit (both have separate literal audit entries).
- `values-hu/strings.xml` is a malformed initial claim: basename `strings.xml` matches multiple files and is **not credited**. The real Hungarian `values-hu/string.xml` is read in this pass; malformed source token remains recorded unchanged.
- Nonexistent workflow names `discontinue-legacy-main.yml` and `update-altstore.yml`, nonexistent `ThemeSelection.kt` / `core/connection/ConnectionSpeedEstimator.kt`, and unqualified “55+ tests” shorthand are **not credited**.
- Seven paths credited by prior coverage are cited elsewhere but not in literal `audited_files`; independently scanned here. See `initial_coverage_caveats`.

## Newly discovered granular gaps and corrections

### tmdb-entity-retry-stuck — missing
Target Retry only assigns Loading. LaunchedEffect is keyed solely to entity kind/id, so Retry does not restart a failed fetch. Add retry generation/load function and exercise failure→retry→success.
Discovery: new granular discovery. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/ui/screens/tmdb/TmdbEntityBrowseViewModel.kt:71–74`
Target: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/TmdbEntityBrowseScreen.kt:86–108`; `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/TmdbEntityBrowseScreen.kt:115–122`

### tmdb-entity-rail-pagination — partial
Source fetches currentPage+1, dedupes, preserves custom posters, and isolates in-flight/error state. Target exposes fetchEntityRailPage but only calls page=1 during fetchEntityBrowse; no load-more caller in tracked Kotlin/Swift.
Discovery: new granular discovery. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/ui/screens/tmdb/TmdbEntityBrowseViewModel.kt:76–128`
Target: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tmdb/TmdbMetadataService.kt:279–301`; `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/TmdbEntityBrowseScreen.kt:122–128`

### durable-progress-delete-outbox — partial
Target removes and persists the item before one-shot delete RPC; failure is only logged, with no persisted delete tombstone/outbox in that path. Server can restore removed item after reconnect. Target dirty progress upserts already exist; do not label all watch sync missing.
Discovery: new granular discovery. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/core/sync/WatchStateMutationStore.kt:58–76`; `app/src/main/java/com/nuvio/tv/core/sync/WatchStateMutationStore.kt:79–99`
Target: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressRepository.kt:1090–1098`; `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressRepository.kt:1328–1338`

### still-watching-countdown — missing
After configurable 2–6 consecutive autoplays (default 3 when enabled; enabled false), pause and prompt for 60 seconds; Continue resets count, timeout exits. Manual selection resets count. Current target next-episode flow has no such gate.
Discovery: new granular discovery. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerStillWatching.kt:8–75`; `app/src/main/java/com/nuvio/tv/data/local/PlayerSettingsDataStore.kt:939–945`
Target: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerNextEpisodeAutoPlay.kt:269–284`

### manual-binge-selection-precedence — partial
Source resolves matching binge group before returning for MANUAL; target early-returns when MANUAL && !bingeGroupOnly. A caller passing preferBingeGroup=true with normal MANUAL cannot obtain the matching stream. Some target next-episode callers compensate by converting mode to FIRST_STREAM; direct selector contract remains different.
Discovery: new granular discovery. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/core/player/StreamAutoPlaySelector.kt:83–99`
Target: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamAutoPlaySelector.kt:104–129`

### plugin-disabled-autoplay-fallback — partial
Source changes ENABLED_PLUGINS_ONLY to INSTALLED_ADDONS_ONLY when AppFeaturePolicy.pluginsEnabled=false. Target selector leaves saved source unchanged. Require policy-level fallback or caller proof on plugin-disabled build.
Discovery: new granular discovery. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/core/player/StreamAutoPlaySelector.kt:62–72`
Target: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamAutoPlaySelector.kt:87–99`

### external-launch-payload-ios — partial
Common request declares sourceHeaders/resumePositionMs/skipSegmentsJson, and preparer resolves segments, but none of iOS URL builders consumes them. Subtitles supported only Infuse/all, VLC/first; Outplayer/VidHub ignore. Report per-player supported protocol adaptation, not blanket Android-only exemption. Never put unsupported headers in a URL without a secure protocol.
Discovery: new granular discovery. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/core/player/ExternalPlayerResultContract.kt:21–30`; `app/src/main/java/com/nuvio/tv/core/player/ExternalPlayerResultContract.kt:58–98`
Target: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/ExternalPlayerPlatform.kt:14–25`; `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/ExternalPlayerPlatform.ios.kt:13–67`

### external-playback-return-ios — partial
Source distinguishes position, duration, completion, and user-ended return. iOS launcher accepts onResult but never invokes it. Add supported callback/session reconciliation or explicit unsupported UX; do not infer completion from merely opening a URL.
Discovery: refinement of external-auto-next / external URL groups. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/core/player/ExternalPlayerResultContract.kt:140–185`
Target: `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/ExternalPlayerLauncherEffect.ios.kt:8–28`

### audio-delay-route-control — partial
Concurrent player agent added clamped ±60000ms MPV audio-delay and AVAudioSession route-key UserDefaults restoration (re-read during audit). This closes the native-backend absence; user-selectable delay UI, remember-per-device toggle and hold/tap behavior remain unproven. Do NOT report audio delay wholly missing; new code is uncompiled/unexercised here.
Discovery: granular refinement of audio-output-route / device-local-player groups. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerPlaybackEvents.kt:34–56`; `app/src/main/java/com/nuvio/tv/ui/screens/player/AudioSelectionOverlay.kt:90–99`
Target: `iosApp/iosApp/Player/MPVPlayerBridge.swift:580–611`

### ffmpeg-downmix-dialogue-limiter — missing
Reference defaults channel layout 7.1; downmix only when app decoder preferred; per-layout 2.0–7.1 selection, center -10..30dB, normalization vs original level, and per-frame linked limiter with 25ms hold/100ms release. Target audio-channels=auto adapts output but does not prove these controls. MPV/FFmpeg can carry portable DSP; JNI imports do not make behavior platform-only.
Discovery: granular refinement of playback-speed-audio / prebuilt decoder groups. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/data/local/PlayerSettingsDataStore.kt:188–210`; `app/src/main/java/com/nuvio/tv/data/local/PlayerSettingsDataStore.kt:307–322`; `ffmpeg-decoder-downmix/src/main/jni/ffmpeg_jni.cc:648–665`; `ffmpeg-decoder-downmix/src/main/jni/ffmpeg_jni.cc:857–940`
Target: `iosApp/iosApp/Player/MPVPlayerBridge.swift:502–513`

### pause-metadata-cast-clock — missing
Source displays content metadata and selectable cast while paused; clock localized and minute-updated, independently enabled. Normal target player controls or detail page are not the same paused-playback capability.
Discovery: new granular discovery. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/ui/screens/player/PauseOverlay.kt:58–118`
Target: 

### loading-report-opt-in — missing
Source only offers report after 45s, opt-in enabled, before first frame, no fatal error, loading visible; captures bounded raw events/phase/progress/settings. Target must wire reporter and consent/redaction rather than only keeping telemetry DTOs.
Discovery: granular refinement of diagnostics/local settings groups. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerLoadingDiagnostics.kt:12–14`; `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerLoadingDiagnostics.kt:121–179`; `app/src/main/java/com/nuvio/tv/data/remote/api/PlaybackIssueReportApi.kt:9–16`
Target: 

### privacy-preserving-ios-crash-reports — missing
Target explicitly crashReportsSupported=false on iOS despite storing enabled preference. SDK mechanics differ, but opted-in crash reporting is portable. Preserve no default PII/screenshots/view hierarchy and disabled performance/network automatic collectors; source query/fragment scrub is not a sufficient general secret-redaction guarantee.
Discovery: refinement of initial Sentry/local stores group. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/core/diagnostics/SentryInitializer.kt:54–77`; `app/src/main/java/com/nuvio/tv/core/diagnostics/SentryNetworkBreadcrumbInterceptor.kt:82–87`
Target: `composeApp/src/iosMain/kotlin/com/nuvio/app/features/settings/SentrySettingsStorage.ios.kt:5–7`

### network-diagnostic-matrix — missing
Reference offers 3-trial Cloudflare latency, Netflix/Fast.com CDN bandwidth, and last-stream baseline versus 1/4/8/16 range connections plus bitrate. Target connection throughput estimator exists; passive history is not user-run diagnostic matrix.
Discovery: refinement of coarse connection/speed groups. **Static comparison only.**
Source: `app/src/main/java/com/nuvio/tv/ui/screens/settings/NetworkSettingsScreen.kt:137–165`; `app/src/main/java/com/nuvio/tv/ui/screens/settings/NetworkSettingsScreen.kt:258–305`
Target: 

## Settings inventory beyond groups

JSON inventories **135 files**, **514 defaulted fields** (including explicitly named UI-state snapshots, not all user settings), **60 enum declarations**, **480 storage key/read sites**, and **239 settings widget calls**. Each default record carries source class/path/line, raw default expression, same-file storage/migration references, source runtime references, and target exact-symbol references. Widget records carry labels, callbacks and ranges. This is a lexical option inventory, not a certified exhaustive resolved-default inventory; renamed targets and platform/sync precedence still require semantic work.

**Do not port constructor defaults blindly:**

| Option | Constructor | Effective fresh-store behavior |
|---|---|---|
| `useLibass` | `true` | false |
| `streamReuseLastLinkEnabled` | `true` | false |
| `streamReuseLastLinkCacheHours` | `1` | 24; clamp 1..168 |
| `streamAutoPlayTimeoutSeconds` | `10` | 3 via applyLegacyTimeoutSentinelMigration(null); stored11→unlimited |
| `subtitleStyle.useForcedSubtitles` | `true` | false unless legacy forced sentinel |
| `subtitleStyle.stripSdh` | `true` | false |
| `subtitleStyle.size` | `120` | 100 |
| `bufferBudgetManaged` | `false` | migration sets true for non-native-memory profiles |

Notable option sets: player INTERNAL/EXTERNAL/ASK_EVERY_TIME; internal EXOPLAYER/MVP_PLAYER/AUTO; autoplay MANUAL/FIRST_STREAM/REGEX_MATCH and all/addons/plugins scope; 0–10,15,20,25,30/unlimited timeout; downmix layouts 2.0 through 7.1; device/default/original audio language; audio gain 0–10dB and center mix -10–30dB; still-watching 2–6 episodes; movie post-play threshold 80–100% default 96; next-episode 97–100% half-step or 0–3.5 minutes; subtitle organization NONE/BY_LANGUAGE/BY_ADDON; five ASS render modes; five DV7 handling modes; AFR OFF/START/START_STOP; auto/manual VOD cache 100–65536MB; parallel connections depend on native memory mode (up to 16 versus normal 4). The JSON keeps declaration and reader/migration evidence rather than flattening these into “player settings”.

## Portable behavior is not Android-only

- Matroska EBML/varints, nested SeekHead, truncated-tail recovery, zlib-compressed samples, codec policy, subtitle font attachments and thumbnail metadata are **behavioral**. Android imports are only integration mechanics. Native MPV may already support some: capability tests are required before declaring absent or equivalent.
- `ffmpeg-decoder-downmix` JNI contains portable rematrixing, metadata-adjusted center mix and linked peak limiting. The explicit iOS setting/control contract is missing; `audio-channels=auto` is not proof of the reference options.
- Launcher icon selection, crash reporting consent, route-specific audio delay, membership assets, LAN debug export, release channels and settings copy have portable product goals even when OS transport differs.
- Narrow APK installation, Android OS job/receiver/component registration and IDE artifacts are separately platform-only. Resources carry visible behavior: auto-sync thorough search/tolerance/failure messages, bubble feedback, local seek previews, custom fonts, ratings certification badges and branded wordmarks.

## Concurrent implementation changes

The target was actively edited during this audit. MPV bridge, next-episode flow and stream selector were reread. In-flight MPV audio-delay/AVAudioSession route persistence **changed that finding from missing to partial**. Native delay backend is now present; UI/toggle/hold behavior and runtime success remain unproven. Stream-selector connection-fit edits do not remove the separate MANUAL binge precedence or disabled-plugin fallback differences. Every cited source/target snippet has a SHA256 in JSON. Recheck before implementing from this report.
Evidence moving during refresh: bridge watchdog/failover edits were reread again; cited audio-delay/downmix boundaries were unchanged, and hashes refreshed.

## Evidence and unresolved paths

`second-audit.json` is the machine-readable ledger: `per_file_coverage` has every exact source path, classification, claim provenance, original-898 flag, byte/hash/line evidence, declaration indexes or parsed XML/archive/image metadata, and honest inspection depth. `unresolved_files` explicitly lists the second-pass backlog with reasons. `unresolved_independent_semantic_review_all_source_paths` is a stricter independent-review backlog, distinct from inherited audit claims.

**No full coverage claim:** selected-function comparisons cannot discharge the entire file, tests were only statically indexed, binary/model/visual behavior is unexercised, and directory-level initial assertions are weaker than file-level behavior proof. Large player/profile/settings implementations and inherited shorthand expansions remain semantic review work.

### Exact unresolved second-pass paths

- `DV7/libdovi/android-arm64/include/libdovi/rpu_parser.h` — Lexical read/classification is not semantic coverage.
- `DV7/libdovi/android-armeabi-v7a/include/libdovi/rpu_parser.h` — Lexical read/classification is not semantic coverage.
- `DV7/libdovi/android-armeabi-v7a/lib/libdovi.a` — Binary runtime body not disassembled/exercised.
- `DV7/libdovi/android-x86/include/libdovi/rpu_parser.h` — Lexical read/classification is not semantic coverage.
- `DV7/libdovi/android-x86/lib/libdovi.a` — Binary runtime body not disassembled/exercised.
- `DV7/libdovi/android-x86_64/include/libdovi/rpu_parser.h` — Lexical read/classification is not semantic coverage.
- `DV7/libdovi/android-x86_64/lib/libdovi.a` — Binary runtime body not disassembled/exercised.
- `app/src/androidTest/java/androidx/media3/exoplayer/source/SampleDataQueueNativeTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/androidx/media3/exoplayer/upstream/DefaultAllocatorNativeLeakTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/androidx/media3/exoplayer/upstream/DefaultAllocatorNativeStressTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/androidx/media3/exoplayer/upstream/DefaultAllocatorNativeTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/androidx/media3/exoplayer/upstream/DefaultAllocatorTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/androidx/media3/exoplayer/upstream/PerformanceBenchmark.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/com/nuvio/tv/core/profile/ProtectedProfilePreferencesTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/com/nuvio/tv/data/mdblist/MdbListLiveAccountTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/com/nuvio/tv/data/mdblist/MdbListLiveLibraryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/com/nuvio/tv/data/mdblist/MdbListLiveScrobbleTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/com/nuvio/tv/data/mdblist/MdbListTimestampTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/com/nuvio/tv/ui/screens/settings/MdbListAccountDialogTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/com/nuvio/tv/ui/screens/settings/PlaybackSettingsOrganizationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/androidTest/java/com/nuvio/tv/ui/screens/settings/TrackingSettingsOverviewTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/debug/java/com/nuvio/tv/ui/screens/settings/CustomThemePreviews.kt` — Lexical read/classification is not semantic coverage.
- `app/src/full/java/com/nuvio/tv/updater/AbiSelector.kt` — Lexical read/classification is not semantic coverage.
- `app/src/full/java/com/nuvio/tv/updater/ApkDownloader.kt` — Lexical read/classification is not semantic coverage.
- `app/src/full/java/com/nuvio/tv/updater/ReshapedApkAssets.kt` — Lexical read/classification is not semantic coverage.
- `app/src/full/java/com/nuvio/tv/updater/ReshapedBridge.kt` — Lexical read/classification is not semantic coverage.
- `app/src/full/java/com/nuvio/tv/updater/UpdatePreferences.kt` — Lexical read/classification is not semantic coverage.
- `app/src/full/java/com/nuvio/tv/updater/model/AppUpdate.kt` — Lexical read/classification is not semantic coverage.
- `app/src/full/java/com/nuvio/tv/updater/ui/ReshapedMigrationHost.kt` — Lexical read/classification is not semantic coverage.
- `app/src/full/java/com/nuvio/tv/updater/ui/UpdateBanner.kt` — Lexical read/classification is not semantic coverage.
- `app/src/full/java/com/nuvio/tv/updater/ui/UpdateBannerHost.kt` — Lexical read/classification is not semantic coverage.
- `app/src/full/java/com/nuvio/tv/updater/ui/UpdateDialogs.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/androidx/media3/decoder/ffmpeg/AudioSyncFfmpegDecoder.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ModernSidebarBlurPanel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/di/MdbListModule.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/diagnostics/SentryInitializer.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/core/diagnostics/SentryNetworkBreadcrumbInterceptor.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/core/network/IPv4FirstDns.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/network/NetworkResult.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/network/SafeApiCall.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/ExternalPlaybackKeepAliveService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/ExternalPlaybackTracker.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/ExternalPlayerLauncher.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/ExternalPlayerResultContract.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/core/player/LetterboxRenderPolicy.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/LocalTrailerPlayerPool.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/StreamAutoPlayPolicy.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/StreamAutoPlaySelector.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/core/player/TrailerPlayerPool.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/ZidooPlayerMonitor.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/ChunkIndexProvider.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/DefaultEbmlReader.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/DolbyVisionCompatibility.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/DtsUtil.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/EbmlProcessor.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/EbmlReader.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/MatroskaExtractor.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/MatroskaZlibSampleDecompressor.java` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/Sniffer.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/ThumbnailMetadata.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/TrackAwareSeekMap.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/VarintReader.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/player/dvmkv/package-info.java` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/server/AddonConfigChangeSanitizer.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/server/AddonConfigServerModels.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/sync/HomeCatalogSyncSupport.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/sync/WatchProgressSyncService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/sync/WatchStateMutationStore.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/core/sync/WatchedItemsSyncService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/sync/library/LibrarySyncLocalStore.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/sync/library/LibrarySyncPaging.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/sync/library/LibrarySyncRemoteDataSource.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tmdb/TmdbCollectionSourceResolver.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tmdb/TmdbMetadataService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tmdb/TmdbService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingDiagnosticIdentity.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingLibraryMembership.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingLibraryProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingLibrarySorter.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingListManager.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingMedia.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingNextUpSeedPolicy.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingProgressProjection.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingProgressProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingProgressRefreshCoordinator.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingRefresh.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingRefreshGate.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingScrobbleCoordinator.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingScrobbleDiagnostics.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingSourceController.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingSources.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/tracking/TrackingWrites.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/trakt/TraktImageUtils.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/core/trakt/TraktPublicListSourceResolver.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/local/MDBListSettingsDataStore.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/local/MemberCatalogStorage.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/local/WatchProgressBuckets.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/local/WatchProgressPreferences.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/local/WatchedItemsPreferences.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/local/WatchedSeriesStateHolder.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mapper/AddonMapper.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mapper/CatalogMapper.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mapper/MetaMapper.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mapper/MetadataFieldMappers.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mapper/StreamMapper.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/AndroidMdbListAuthPersistence.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListApiClient.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListAuthModels.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListAuthRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListAuthStore.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListHistoryPayload.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListHistoryService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListHttpClient.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListHttpModels.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListLibraryDecoder.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListLibraryModels.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListLibraryMutation.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListLibraryProjection.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListLibraryRemote.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListLibraryService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListLibrarySorter.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListLibraryWriter.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListMediaIndex.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListMutationTarget.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListProgressProjection.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListRatingsClient.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListRatingsLoader.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListResponseValues.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListScrobbleReceipt.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListScrobbleService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListSyncDecoders.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListSyncEngine.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListSyncModels.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListSyncRemote.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListSyncRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListSyncState.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListSyncStorage.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListTrackingLibraryProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListTrackingProgressProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListTrackingProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListWatchedDecoder.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/MdbListWriteReconciliation.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/mdblist/OkHttpMdbListEngine.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/AddonApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/AuthDiagnosticReportApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/GitHubReleaseApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/MDBListApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/ParentalGuideApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/PlaybackIssueReportApi.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/data/remote/api/PremiumizeApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/RealDebridApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/SeriesGraphApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/SkipIntroApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/SupportersApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/TmdbApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/TorboxApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/TrailerApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/TraktApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/api/UniqueContributionsApi.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/AddonManifestDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/AuthDiagnosticReportDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/CatalogResponseDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/GitHubContributorDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/GitHubReleaseDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/MetaResponseDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/PlaybackIssueReportDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/PremiumizeDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/RealDebridDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/StreamResponseDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/SupportersWallDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/TorboxDto.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/mdblist/MDBListRatingDtos.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/trakt/TraktAuthDtos.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/trakt/TraktCommentsDtos.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/trakt/TraktListsDtos.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/trakt/TraktMediaDtos.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/trakt/TraktScrobbleDtos.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/dto/trakt/TraktSyncDtos.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/supabase/AvatarRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/supabase/MemberAccessRemoteDataSource.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/supabase/MembershipOverviewRemoteDataSource.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/supabase/ProfileBackgroundRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/remote/supabase/SupabaseLibrarySyncRemoteDataSource.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/AddonRepositoryImpl.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/AuthDiagnosticReportRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/CatalogRepositoryImpl.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/GitHubContributorsRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/ImdbEpisodeRatingsRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/LibraryRepositoryImpl.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/MDBListRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/MemberAccessRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/MembershipOverviewRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/MetaRepositoryImpl.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/ParentalGuideRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/PlaybackIssuePlaybackAnalyticsInput.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/PlaybackIssueReportRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/SimklIdResolver.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/SkipIntroRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/SponsorsRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/StreamSearchSessionCache.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/SupportersRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/SyncRepositoryImpl.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktAuthService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktCommentsService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktEpisodeMapping.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktEpisodeMappingService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktIdUtils.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktLibraryService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktProgressService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktRelatedService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktScrobbleService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktTrackingHistoryWriter.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktTrackingLibraryProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktTrackingProgressProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktTrackingProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/TraktTrackingScrobbler.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/repository/WatchProgressRepositoryImpl.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/AndroidSimklAuthStorage.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/OkHttpSimklEngine.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklAnimeIdPreference.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklAnimeIdPreferenceHolder.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklApiClient.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklApiMetadata.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklAuthModels.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklAuthRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklAuthStorage.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklContinueWatchingDiagnostics.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklLibraryProjection.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklLibraryService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklMediaProjections.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklMutationBodies.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklMutationReceipt.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklMutationReconciliation.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklMutationService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklPlaybackMerge.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklPlaybackReconciliation.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklRefreshPolicy.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklRelatedService.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklScrobbleReconciliation.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklScrobbleResult.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklSnapshotProjection.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklSyncEngine.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklSyncModels.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklSyncRemote.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklSyncRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklSyncStorage.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklTrackingHistoryWriter.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklTrackingProgressProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/data/simkl/SimklTrackingProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/domain/model/MDBListRatings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/domain/model/MDBListSettings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/domain/model/WatchProgress.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/domain/model/WatchedItem.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/domain/repository/AddonRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/domain/repository/CatalogRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/domain/repository/LibraryRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/domain/repository/MetaRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/domain/repository/StreamRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/domain/repository/SyncRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/domain/repository/WatchProgressRepository.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/launcher/AppIconManager.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/components/MDBListRatingsRow.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/components/WatchedMarker.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/reshaped/debuglog/DebugLogQrDialog.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/reshaped/pillnav/PillGlassBackdrop.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/reshaped/pillnav/PillGlassShader.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/reshaped/pillnav/PillNavScaffold.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/reshaped/pillnav/PillNavigationBar.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/home/HomeViewModelMdbListBatchPrefetch.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/AudioDelayMediaSource.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/AudioOutputRouteDetector.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/AudioSelectionOverlay.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/BufferedReadDataSource.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/CustomDefaultTrackNameProvider.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/DisplayModeOverlay.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/DolbyVisionBaseLayerPolicy.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/DolbyVisionCodecFallback.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/EpisodesSidePanel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/LastPlaybackDiagnostics.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/LoadingOverlay.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/LoggingDataSource.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/MpvHi10pFallback.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/NextEpisodeEndPromptOverlay.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/NuvioAssMatroskaExtractor.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/ParallelRangeRetryAfter.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/ParentalGuideOverlay.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PauseOverlay.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlaybackConnectionEvents.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerAutoplaySessionRules.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerBitrateEstimator.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerDebugStatsOverlay.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerDisplayModeUtils.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerLibassCompat.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerLibassExtensions.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerLoadingDiagnostics.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerMediaSessionMetadata.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerMemoryReporter.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerNavigationArgs.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerNextEpisodeRules.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerOverlayScaffold.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerPlaybackAnalyticsDiagnostics.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerObservers.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerPlaybackEvents.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerScrobble.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerStartup.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerStillWatching.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerTorrent.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerScrobblePolicy.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerScrubRates.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerStartupLoadingPolicy.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerStartupPlaybackPolicy.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerViewModel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PostPlayOverlay.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PostPlayRecommendationController.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PostPlayRecommendationMetadata.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PostPlayRecommendationOverlay.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PostPlayRecommendationPlayerWindow.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PostPlayRecommendationState.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/PostPlayRecommendationTiming.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/RewrappableExtractor.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/SafeMediaSessionPlayer.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/SkipIntroButton.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/SkipIntroVisibilityRules.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/StreamComponents.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/StreamInfoOverlay.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/StreamSourcesSidePanel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/TorrentOverlay.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/player/Vc1VideoFormatHeuristics.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/profile/ProfileSelectionScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/profile/ProfileSelectionViewModel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/profile/ProfileSettingsCopyDialogs.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/AboutScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/AnimeSkipSettingsScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/ConnectionSpeedSettingsItems.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/CustomThemeEditor.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/DebridSettingsScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/DebugMemberTierCard.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/DebugSettingsScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/DiagnosticsCard.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/EssentialPlaybackSettingsContent.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/ExperienceModeConfirmationDialog.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/HexColorDialog.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/LayoutCardSettings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/LayoutDetailSettings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/LayoutHomeSettings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/LayoutSettingsModel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/LayoutSettingsScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/LayoutSettingsViewModel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/LicensesAttributionsScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/LiveTvSettingsItems.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/MDBListSettingsScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/MDBListSettingsViewModel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/MdbListAccountDialog.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/MdbListTrackerViewModel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/MemoryBudget.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/NetworkSettingsScreen.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/PillNavSettingsItems.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackAudioSettings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackAutoPlaySettings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackBufferNetworkSettings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackP2pSettings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackPlayerSettings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackSettingsModel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackSettingsScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackSettingsSections.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackVideoSettings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/ProfileSettingsContent.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/SeekBufferSettingsItems.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/SeekPreviewSettingsItems.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/SeekrKeySettingsItems.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/SentrySettingsDialog.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/SimklSettingsConnectionPolicy.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/SupporterMembershipPanel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/SupportersContributorsScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/ThemeColorPicker.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/ThemeSettingsScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/TmdbSettingsScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/TmdbSettingsViewModel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/TrackingProviderDialogs.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/TrackingSettingsScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/UpdateChannelSettings.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/tmdb/TmdbEntityBrowseScreen.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/tmdb/TmdbEntityBrowseUiState.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/screens/tmdb/TmdbEntityBrowseViewModel.kt` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
- `app/src/main/java/com/nuvio/tv/ui/theme/Color.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/theme/FocusRingStyle.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/theme/LayoutMediaTokens.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/theme/MotionFocusTokens.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/theme/PrimitiveTokens.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/theme/ShapeTokens.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/theme/SizeTokens.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/theme/SpacingTokens.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/theme/StrokeElevationEffectTokens.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/java/com/nuvio/tv/ui/theme/Type.kt` — Lexical read/classification is not semantic coverage.
- `app/src/main/res/drawable-nodpi/tv_banner.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/app_logo_wordmark_arctic_blue.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/app_logo_wordmark_gold.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/app_logo_wordmark_graphite.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/app_logo_wordmark_jade.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/app_logo_wordmark_rose_gold.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/ic_chevron_compact_left.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/ic_launcher.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/introdb_favicon.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/mdblist_audience.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/mdblist_audience_stale.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/mdblist_audience_verified_hot.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/mdblist_metacritic.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/nuvio_text.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/rating_tmdb.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/drawable/tv_banner.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-hdpi/ic_launcher.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-hdpi/ic_launcher_arctic_blue.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-hdpi/ic_launcher_copper.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-hdpi/ic_launcher_emerald.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-hdpi/ic_launcher_graphite.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-hdpi/ic_launcher_rose_gold.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-mdpi/ic_launcher.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-mdpi/ic_launcher_arctic_blue.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-mdpi/ic_launcher_copper.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-mdpi/ic_launcher_emerald.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-mdpi/ic_launcher_graphite.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-mdpi/ic_launcher_rose_gold.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/banner.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/banner_arctic_blue.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/banner_copper.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/banner_emerald.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/banner_graphite.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/banner_rose_gold.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/ic_launcher.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/ic_launcher_arctic_blue.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/ic_launcher_copper.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/ic_launcher_emerald.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/ic_launcher_graphite.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xhdpi/ic_launcher_rose_gold.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_arctic_blue.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_copper.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_emerald.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_graphite.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_rose_gold.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_arctic_blue.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_copper.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_emerald.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_graphite.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_rose_gold.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `app/src/playstore/java/com/nuvio/tv/updater/UpdateViewModel.kt` — Lexical read/classification is not semantic coverage.
- `app/src/playstore/java/com/nuvio/tv/updater/ui/UpdateBannerHost.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/androidx/media3/datasource/AesCipherDataSourceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/androidx/media3/datasource/DefaultDataSourceRoutingTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/androidx/media3/datasource/LocalhostZeroCopyDataSourceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/androidx/media3/exoplayer/upstream/DefaultAllocatorTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/MainDispatcherRule.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/TestPreferencesStore.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/auth/AuthRefreshResponsePolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/auth/AuthSessionValidationPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/auth/DeviceSessionRegistrationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/auth/SupabaseAuthSessionTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/network/BackendRateLimitTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/player/DolbyVisionBaseLayerPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/player/FrameRateModeSelectionTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/player/FrameRateUtilsAfrCancellationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/player/FrameRateUtilsAfrTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/player/FrameRateUtilsMkvSparseTracksTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/player/FrameRateUtilsMp4PeekLoopTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/player/MatroskaAfrProbeTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/player/StreamAutoPlaySelectorTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/player/dvmkv/MatroskaNestedSeekHeadTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/player/dvmkv/MatroskaTruncatedTailTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/player/dvmkv/MatroskaZlibSampleDecompressorTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/server/AddonConfigServerTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/server/DebridFormatterConfigServerTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/server/StreamBadgeConfigServerTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/HomeCatalogSettingsSyncServiceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/HomeCatalogSyncSupportTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/LibrarySyncServiceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/ProfilePullFreshnessTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/ProfileSettingsCredentialPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/ProfileSettingsPluginSyncTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/ProviderCredentialModelsTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/SurfacePullFreshnessTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/WatchStateMutationStoreTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/WatchStateSyncServiceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/androidtv/AndroidTvChannelManagerTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/sync/library/LibrarySyncPagingTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/tmdb/TmdbCollectionSourceResolverTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/tmdb/TmdbMetadataServiceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/tracking/TrackingLibraryMembershipTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/tracking/TrackingLibrarySourceControllerTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/tracking/TrackingMediaTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/tracking/TrackingNextUpSeedPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/tracking/TrackingProgressProjectionTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/tracking/TrackingProgressRefreshCoordinatorTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/tracking/TrackingProviderRegistryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/tracking/TrackingScrobbleCoordinatorTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/tracking/TrackingSourcesTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/trakt/TraktImageUtilsTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/trakt/TraktPublicListSourceResolverTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/util/EpisodeReleaseDateParserTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/util/ReleaseInfoUtilsTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/core/util/RuntimeParserTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/local/CollectionsDataStoreSourceMigrationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/local/DeviceLocalPlayerPreferencesTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/local/DiscoverLocationMigrationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/local/EpisodeShuffleStoreTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/local/PlayerSettingsTimeoutMigrationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/local/PlayerSettingsTimeoutPredicateTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/local/TraktAuthDataStoreTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/local/WatchProgressBucketsTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/local/WatchProgressPreferencesStorageTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/local/WatchedItemsPreferencesSyncTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/local/WatchedItemsPullPreservationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mapper/MetaMapperTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListApiClientTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListAuthRepositoryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListAuthStoreTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListHistoryServiceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListHttpClientTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListLibraryDecoderTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListLibraryServiceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListLibrarySorterTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListLibraryTestFixtures.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListLibraryWriterTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListMutationTargetTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListProgressProjectionTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListRatingsClientTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListRatingsLoaderTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListScrobbleServiceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListSyncDecoderTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListSyncEngineTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListSyncRemoteTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListSyncRepositoryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListSyncTestFixtures.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListTestFixtures.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/MdbListTrackingProgressProviderTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/mdblist/OkHttpMdbListEngineTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/remote/ServerDiscoveryPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/remote/dto/mdblist/MDBListMediaResponseDtoTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/remote/supabase/AvatarCatalogRefreshTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/remote/supabase/SupabaseProfileSetupCopyResultTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/AddonManifestPlaceholderTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/CatalogRepositoryMalformedEntryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/CatalogRepositoryTypeTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/GitHubContributorsRepositoryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/LibraryRepositoryTrackingTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/MDBListRepositoryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/MemberAccessRepositoryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/MetaRepositoryAddonCacheTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/MetaRepositoryCandidateTypeTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/MetaRepositoryNotFoundClassificationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/MetaRepositoryPreferredAddonTypeTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/RemoteProgressWriteDeduplicatorTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/SponsorsRepositoryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/StreamRepositoryPluginIsolationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/SupportersRepositoryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/TraktAuthServiceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/TraktCommentsServiceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/TraktProgressPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/TraktTrackingListManagerTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/TraktTrackingScrobblerTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/repository/WatchProgressRepositoryProfileIsolationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/OkHttpSimklEngineTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklAnimeWatchedResolutionTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklApiClientTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklAuthRepositoryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklContinueWatchingDiagnosticsTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklLibraryRemovalPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklMutationReconciliationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklMutationServiceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklPlaybackReconciliationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklProjectionsTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklRefreshPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklScrobbleReconciliationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklSettingsPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklSnapshotProjectionTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklSyncEngineTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/data/simkl/SimklTestFixtures.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/domain/model/WatchProgressTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/reshaped/sync/SyncDocTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/AfrPreflightPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/AudioTrackFailureClassificationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/AutoplaySessionCountTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/BluetoothAudioRoutePolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/MpvPositionFromMediaRequestTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/NaturalPlaybackCompletionRulesTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/NuvioExoPlayerPerformanceHelperTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/ParallelRangeDataSourceTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PlaybackNetworkingSslVerificationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PlaybackProfileRouteTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PlayerFirstFrameCodecRecoveryPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PlayerFrameRateHeuristicsTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PlayerMediaSourceFactoryTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PlayerNextEpisodeRulesTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PlayerPlaybackAnalyticsDiagnosticsTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PlayerScrobblePolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PlayerScrubRatesTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PlayerStartupPlaybackPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PostPlayModeTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PostPlayRecommendationStateTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/PostPlayResetRulesTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/SidecarCueOrderingTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/SkipIntroVisibilityRulesTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/StillWatchingGatingTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/TrackSelectionInvestigationTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/TunneledSurfaceResizeModeTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/Vc1VideoFormatHeuristicsTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/VodCacheAutoSizeTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/seekpreview/SeekPreviewCueStepperTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/player/seekpreview/SeekrContentMappingTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/settings/MDBListSettingsViewModelTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/settings/MemoryBudgetTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/settings/SettingsCatalogTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/settings/SettingsIconsTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/settings/SettingsStructureTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/settings/SimklSettingsConnectionPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/settings/ThemeSettingsViewModelTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/screens/settings/TrackingSettingsViewModelTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/theme/CustomThemePaletteTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/theme/FocusRingStyleTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/ui/theme/ThemeBrandingTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/updater/ReleaseSelectorTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/updater/UpdateBannerPolicyTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/test/java/com/nuvio/tv/updater/VersionUtilsTest.kt` — Lexical read/classification is not semantic coverage.
- `app/src/testFull/java/com/nuvio/tv/updater/ReshapedApkAssetsTest.kt` — Lexical read/classification is not semantic coverage.
- `assets/brand/app_logo_mark.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `assets/brand/app_logo_wordmark.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `assets/nuviotv.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `baselineprofile/src/main/java/com/nuvio/tv/baselineprofile/BaselineProfileGenerator.kt` — Lexical read/classification is not semantic coverage.
- `branding/nuvio-rs-logo.png` — Visual/font/model resource appearance or inference behavior not exercised.
- `ffmpeg-decoder-downmix/src/main/java/androidx/media3/decoder/ffmpeg/FfmpegAudioDecoder.java` — Lexical read/classification is not semantic coverage.
- `ffmpeg-decoder-downmix/src/main/java/androidx/media3/decoder/ffmpeg/FfmpegAudioRenderer.java` — Lexical read/classification is not semantic coverage.
- `ffmpeg-decoder-downmix/src/main/java/androidx/media3/decoder/ffmpeg/FfmpegDecoderException.java` — Lexical read/classification is not semantic coverage.
- `ffmpeg-decoder-downmix/src/main/java/androidx/media3/decoder/ffmpeg/FfmpegLibrary.java` — Lexical read/classification is not semantic coverage.
- `ffmpeg-decoder-downmix/src/main/java/androidx/media3/decoder/ffmpeg/package-info.java` — Lexical read/classification is not semantic coverage.
- `ffmpeg-decoder-downmix/src/main/jni/ffmpeg_jni.cc` — Selected functions only; complete behavioral/caller/dependency/test parity not proven.
