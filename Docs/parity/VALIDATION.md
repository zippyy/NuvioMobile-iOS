# Native build and parity validation

## Scope and evidence

The aggregate commit `6bf2b8e567d5f4010ad94e3ae403ad35e63fee7a` failed its Full iOS build: [run 37021150898](https://github.com/zippyy/NuvioMobile-iOS/actions/runs/37021150898). IPA packaging was skipped. Failures included common Kotlin `toSortedMap`, invalid `ProfileRepository.uiState` access, missing image alignment argument, and iOS NSDate/zlib interop errors. Takeover commit `7775fbe30afc93a109d6841cd1332c85f1c249ad` repairs those reported errors.

CI now runs the actual `iosSimulatorArm64Test` target after the Release device build and before IPA packaging. It selects Live TV (including iOS gzip interop), connection estimators, connection-fit selection, shuffle, artwork, MDBList, TorrServer, YouTube resolver, subtitle transport/policy/encoding/fonts, seek-preview, volume policy and portable sync tests. XML reports are uploaded as `iOS-parity-test-results`. Existing component harness reports are not represented as a native application test pass.

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
