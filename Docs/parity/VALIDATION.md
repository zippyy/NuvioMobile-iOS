# Native build and parity validation

## Account sign-in configuration correction

After the repaired IPA installed, the owner reported Nuvio account sign-in failing. The workflow generated `SUPABASE_URL` / `SUPABASE_ANON_KEY`, while `generateRuntimeConfigs` consumes `NUVIO_SUPABASE_URL` / `NUVIO_SUPABASE_ANON_KEY`. Those legacy property names therefore produced empty runtime auth constants even if matching Actions secrets existed. CI now writes the exact consumed names, accepting either prefixed or legacy repository secret names, and refuses to build when the required account-service configuration is absent or the URL is not HTTPS. Values are not logged. Configuration-generation and missing-config failure paths are checked with noncredential fixtures. Actual sign-in must be tested with the rebuilt IPA and the existing Nuvio account-service configuration; compilation is not an auth acceptance test.

## Sideload packaging correction

The green build at `ff24f917` produced an archive with 27 unused MPVKit dummy frameworks embedded by Xcode. Each declared `MinimumOSVersion=100.0`; each device Mach-O slice also declared iOS 100.0 and had no symbols or exports. The app and widget load only system libraries, so none of these stubs is a runtime dependency. The user reported Feather and KSign hanging at installation. ZIP integrity alone did not catch this invalid embedded-bundle metadata and was insufficient installation evidence.

`scripts/prepare-ios-ipa.py` removes only verified empty, unreferenced `com.mpvkit.*` iOS-100 stubs. It refuses referenced stubs and nonempty frameworks, preserves real supported dylibs, checks device ARM64/platform and bundle metadata, verifies ZIP CRCs, and compares every retained payload file byte-for-byte with the source. The repaired original IPA removes 27 stubs while preserving both executable hashes. Four package-guard tests pass. No lowering of a framework's deployment target or alteration of app/widget code is performed.

The Full build workflow applies this correction to future IPAs. `repack-ios.yml` can repair a previously built unsigned `Nuvio-iOS-Full` artifact without compiling again, and uploads `Nuvio-iOS-Full-Sideload` with a package audit. Actual Feather/KSign installation remains unverified until the repaired IPA is tested on the user's device. Do not represent the original artifact as install-tested.

## Scope and evidence

The aggregate commit `6bf2b8e567d5f4010ad94e3ae403ad35e63fee7a` failed its Full iOS build: [run 37021150898](https://github.com/zippyy/NuvioMobile-iOS/actions/runs/37021150898). IPA packaging was skipped. Failures included common Kotlin `toSortedMap`, invalid `ProfileRepository.uiState` access, missing image alignment argument, and iOS NSDate/zlib interop errors. Takeover commit `7775fbe30afc93a109d6841cd1332c85f1c249ad` repairs those reported errors.

CI now runs the actual `iosSimulatorArm64Test` target before the Release device build and IPA packaging. It selects Live TV (including iOS gzip interop), connection estimators, connection-fit selection, shuffle, artwork, MDBList, TorrServer, YouTube resolver, subtitle transport/policy/encoding/fonts, seek-preview, volume policy and portable sync tests. XML reports are uploaded as `iOS-parity-test-results`. Existing component harness reports are not represented as a native application test pass.

Final commit/run/test-count/artifact evidence belongs in [PR #1](https://github.com/zippyy/NuvioMobile-iOS/pull/1) after the workflow finishes. A pending or failed workflow does not establish validation. An unsigned IPA is suitable for subsequent signing/sideload tooling; it is not an App Store submission or a signed install/device test.

## Release linker workaround

The repaired Kotlin compilation reached `linkReleaseFrameworkIosArm64`, which exhausted its 10 GiB CI heap in `DevirtualizationAnalysis` ([run 37029029141](https://github.com/zippyy/NuvioMobile-iOS/actions/runs/37029029141)). Release frameworks now skip the coupled IR phases `DevirtualizationAnalysis`, `Devirtualization`, `DCEPhase`, and `RemoveRedundantCallsToStaticInitializersPhase`; Release configuration and LLVM optimization remain enabled. This is a build workaround, not evidence of unchanged code size/performance. Device performance/size must be assessed and the workaround revisited after a compiler fix. See upstream [KT-84412](https://youtrack.jetbrains.com/issue/KT-84412). Debug simulator regression tests retain normal compilation.

## Touch reachability review

| Feature | Actual path / control | Static review result |
|---|---|---|
| Live TV | Home → Live TV → Channels / Guide / Sources / Categories → channel playback | Previously unreachable; takeover adds navigation entry and existing player launch. Channel launch sets `streamType=live` and avoids VOD progress writes. |
| TorrServer | Settings → Playback → TorrServer | Form is called by the Playback page; validation/error text present. |
| MDBList | Settings → Integrations → MDBList ratings → key and list browser | API-key path only; device-code OAuth/tracking absent. |
| Backup | Settings → Advanced → settings backup panel | Clipboard settings JSON; excludes account/library/history/addons/profile data. |
| Shuffle | Series details → episode shuffle / include watched / shuffle again / play | Existing touch controls present and profile-state compile reference repaired. |
| Artwork | Details → artwork controls; rendered hero/card overrides | Existing controls/rendering present; invalid wrapper alignment and profile references repaired. |
| Audio delay | Player → audio tracks → signed delay and reset | Connected through modal host/controller to native route persistence. |
| Volume boost | Player → audio tracks → 0–200% slider and reset | Takeover wires previously uncalled bridge, profile persistence and playback application. |
| Custom subtitle font | Validator only | No import/selection/reset/MPV-font consumer; incomplete. |
| Connection fit | Stream-load/next-episode policy with passive MPV throughput | Runtime integration present; no reachable enable toggle or manual speed-test page established. |
| Cache/read-ahead | Native fixed live/VOD MPV policy | No configurable Reshaped thresholds/low-memory controls established. |
| Seek preview | Existing seek callbacks → native generator | File URLs only; network VOD/Seekr missing. |

This is call-site and form review, not an iPhone/iPad interaction test. No user Mac workspace, Xcode UI, simulator interaction, real provider account or physical device is available from the Linux execution workspace. Remote macOS CI can prove native compilation/tests/packaging, but cannot stand in for those unperformed UI/provider checks.

## Retained network observer

The committed iOS observer uses `platform.Network` / NWPathMonitor. The earlier failing aggregate's compiler output did not report an observer error. The takeover retains it until native evidence requires a change, because returning null for network kind would suppress throughput samples and disable connection-fit ranking. Uncommitted Mac edits from the handoff are not present in this checkout.

## Remaining parity

See [PARITY_MATRIX.md](PARITY_MATRIX.md), [current-status.json](current-status.json), [second-audit.md](second-audit.md), and [implementation-sync.md](implementation-sync.md). Full parity is not asserted: automatic reference/speech subtitle synchronization, font import/application, MDBList OAuth/tracking, Drive sync, full backup, TV-preview/zapping adaptations, manual network diagnostics, and the independent semantic/resource backlog remain open.

## Takeover portable component execution

| Executed suite | Result | Scope |
|---|---|---|
| `scripts/test-sync-parity.py` | 7 passed | Actual pure sync/backup/PIN/migration policy code with adapted assertion runner. |
| Shuffle/artwork harness | 39 passed | Existing harness with Linux Java path; production policies/storage with declared platform/model adapters. |
| `scripts/test-metadata-streams.py` | 12 passed | Actual MDBList, TorrServer and YouTube resolver code. |
| Live TV common harness | 5 JUnit tests passed | Actual parser/provider/guide/persistence/transport-policy code; Compose Immutable annotation adapter only. One test contains seven multi-step provider/repository scenarios. |
| Connection/subtitle/player harness | 92 JUnit tests passed | Actual estimators/sampler/subtitle transport/policy/code-page/font/SDH/preview/volume code; storage/network-kind/charset platform adapters. |

Total: **155 portable component tests passed**. These tests use the Kotlin 2.3.0 compiler from Gradle 9.4.1. They do not prove native interop or live HTTP/provider/device behavior. Native simulator execution is still required by CI.

The test run revealed stale time API usage, clock/warmup/decimal-byte mistakes, and uninitialized/inaccurate charset fixtures. SDH expectations now match the pinned reference (dialogue dash and trailing whitespace preserved). Independent fixed-byte fixtures exposed real incorrect Windows code-page tables; production mappings and the Hebrew-before-CJK heuristic were repaired. Assertions were retained for actual encoding and sampler behavior rather than bypassed.
