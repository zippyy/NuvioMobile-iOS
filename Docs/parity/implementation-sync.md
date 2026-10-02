# Sync / backup / profile-security implementation

## Implemented

- **Reachable settings backup/restore:** Settings → Advanced → Settings backup and restore. Exports credential-free JSON to the clipboard; the user can save this in a file. Imports copied JSON with section preview, explicit section selection, destructive-action confirmation, and account/profile identity checks before applying. Uses existing `ProfileSettingsSync` export/storage/repository reload conventions, not a second preferences store.
- **Selective restore:** only selected feature objects replace the current snapshot. Missing/unselected sections remain current. All feature models decode before storage writes begin. Existing API credentials are preserved by `preservingLocalProfileCredentials`; PIN/auth/session data is never part of the backup.
- **Input boundaries:** backup format/version checks, 2,000,000-character limit, feature allowlist, JSON object/string-payload type checks. Unsupported/future/malformed documents fail before persistence.
- **Portable Reshaped SyncDoc v1:** wire keys `v`, `s`, entry `v`/`t`; per-key last-write-wins, remote tie winner, zero-time first-device defaults, tombstones, 60-day pruning, slow-clock stamping, preservation of other sections/profiles. Located in `core/sync/ReshapedSyncDoc.kt` (internal `SyncDoc`, `SyncEntry`, `SyncSections`). This is the merge model, **not a connected Google Drive service**.
- **iOS secure credential persistence:** `ProfileSecureStorage.ios.kt` stores debrid provider keys, TMDB/MDBList keys, AnimeSkip client ID, IntroDB key, and cached PIN verifiers in Keychain (`WhenUnlockedThisDeviceOnly`), using existing profile key names. On access, legacy NSUserDefaults values migrate only after secure write/read verification. Saves use SecItemUpdate rather than deleting the previous credential first. Failures are generic and no secret values are logged.
- **Offline PIN retry control:** five failed cached-verifier attempts impose a 30-second session-local lock, isolated by account/profile; success resets attempts. Unknown profiles fail closed. Preserved existing remote PIN setup/clear/password reset/lock APIs and profile selection dialogs.

## Existing target features preserved / audit corrections

The coarse audit is stale for several capabilities:

- `ProviderCredentialSync.kt` already implements per-profile provider snapshot, remote merge/seeding and scope cancellation protection. It includes TMDB and IntroDB beyond the reference's examples. `ProfileSettingsSync.startObserving()` already starts it; no duplicate credential sync service was added.
- `ProfileRepository.kt`, `ProfileSelectionScreen.kt`, `ProfileSwitcherTab.kt`, `ProfileEditScreen.kt` already implement PIN setup, change, clear, recovery, online verification and cached offline verification. The work hardens storage/retries rather than replacing this UI.
- `SyncManager.kt` already orders addon/plugin/profile-settings/provider-credential sync and then library/watch-source/collection/home-catalog pulls. Existing orchestration remains intact.
- `SyncClientIdentity.kt` already persists a mobile client identity and supplies `p_origin_client_id`; retained rather than creating another competing device identifier.
- Reference `ReshapedIdentity.kt` actually identifies Android packages and signature-validated migration providers; it is not the per-device sync client identity. Reference `ReshapedMigration.kt` copies an Android bridge provider ZIP into staged app-private roots. That Android container format is not claimed to be imported by this JSON settings importer.

## Verification evidence

Tests were added before their production slices. Lightweight Kotlin compiler runs demonstrated missing `SyncDoc`, `SettingsBackup`, `ProfilePinThrottle`, and `migrateCredential` references before implementation; the resulting tests now pass.

```
python3 scripts/test-sync-parity.py
PASS 7 tests

git diff --check
# exit 0, no output
```

The runner compiles the actual pure common Kotlin production files and adapted copies of the commonTest methods using cached Kotlin/serialization jars, without Gradle/Xcode. Assertions and exception checks run on real code. Tests cover default-vs-remote conflicts, deletion round trip, slow clocks, other profiles, tombstone expiry, newer format rejection, selective restore, malformed/future backups, account/profile retry isolation and verified credential migration / failed-write plaintext retention.

**Not verified here:** Compose application compile, iOS native interop compile, on-device Keychain migration/read-back, and touch UI interaction. Heavy builds were reserved for the build worker. Compiler prints JVM native-access/Unsafe warnings; these are not test failures. Parent should run standard commonTest/native compilation and on-device restore smoke tests.

## Still-open gaps (not platform impossibilities)

1. Google OAuth configuration/sign-in, Drive appDataFolder transport, persisted Reshaped base document, duplicate Drive-file reconciliation, lifecycle/debounce scheduling and sync status UI are not implemented. SyncDoc is portable and ready for those bindings, but is not wired to `settings/shared` / Live TV. No claim of TV↔phone Drive sync completion.
2. No automatic import of Android legacy bridge ZIP data; no cross-platform migration marker/state workflow. JSON backup is a new explicit mobile settings format, not a legacy Reshaped export conversion.
3. Backup is clipboard JSON, not a document-picker/share-sheet file workflow. It includes settings only: no library/watch history, addon/plugin configurations, home catalog ordering, profile metadata/avatars or credentials. The UI states these exclusions. Profile setup copy can use manual export/switch/import but has no dedicated copy dialog.
4. Restore is prevalidated but writes multiple existing stores sequentially, not a crash-atomic transaction. There is no rollback after a storage error; the user is instructed to export first.
5. Offline PIN retry state is session-local, so process restart resets it; server locks remain authoritative online. Existing salted SHA-256 PIN cache format remains; no PBKDF2 migration or secure random salt replacement was made. Keychain reduces offline verifier exposure but does not change its KDF.
6. Keychain accounts retain the target's existing profile-index key naming, not a new user-ID namespace. Account-wide secure-data erasure and device/profile deletion cleanup need explicit verification/integration; this change does not claim those missing audit capabilities complete.
7. Google Drive source credentials / Seekr key and new settings owned by other workers are not silently added to backup. Shared/TV/device-local sync policy needs integration with those actual consumers. Existing target iOS playback fields and normal Supabase sync behavior are preserved.
8. Backup labels are currently English and not indexed as individual settings-search entries.

No Android LAN HTTP server was ported. Missing portable logic is listed as unfinished work, not dismissed as unsupported on iOS.
