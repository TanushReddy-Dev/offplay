# Android Hi‑Res Music Player: Architecture and Build Plan

**Research date:** 2026-10-04  
**Scope:** Android app with fluid UI, high-resolution playback, in-app discovery, and offline downloads where the source explicitly permits them.

## Executive recommendation

Build a **greenfield, native Kotlin Android app** with:

- **Jetpack Compose + Material 3** for the UI
- **AndroidX Media3 / ExoPlayer** for playback, sessions, background controls and authorized offline downloads
- **Room** for normalized library, queue, metadata, provenance and download state
- **WorkManager** for indexing, metadata/artwork sync, cleanup and entitlement revalidation
- **MediaSessionService / MediaLibraryService** for playback that survives Activity lifecycle changes
- **Hilt + Kotlin coroutines/Flow** for dependency injection and reactive state
- A strict, replaceable **provider boundary** for catalog search, playback, account authorization and downloads
- **MediaStore + Storage Access Framework (SAF)** for local/user-authorized files

Do **not** fork one of the existing players as the product foundation. The survey found that most are local-only, GPL-licensed, stale/unavailable, or coupled to undocumented source extraction. Use them as architecture and UX references, not as a shortcut around provider permissions.

> A playable URL is not automatically a downloadable file, and a FLAC file is not automatically bit-perfect at the physical output. Model rights and measure the end-to-end audio route explicitly.

---

## 1. What the repository survey found

| Repository/reference | Useful pattern | Important caveat |
|---|---|---|
| [Auxio](https://github.com/OxygenCobalt/Auxio) | Explore → Extract → Evaluate library pipeline; SAF; normalized music graph; Room cache; Media3 service; gapless/ReplayGain/Android Auto | GPLv3; local-first; substantial native/Media3 build complexity; no remote catalog/downloader |
| [Gramophone](https://github.com/FoedusProgramme/Gramophone) | Media3 service/session, MediaStore + folder browsing, queue, baseline profile, audio-output experiments | GPLv3; current canonical repo differs from the originally requested identity; local-only; device codec limits remain |
| [Vinyl Music Player](https://github.com/VinylMusicPlayer/VinylMusicPlayer) | Simple local-library UX, queue synchronization, notification/widgets/Android Auto, tag editing | GPLv3; maintenance risk and legacy MediaPlayer/storage/foreground-service behavior |
| [Music Player GO](https://github.com/enricocid/Music-Player-GO) | Lightweight UI, MediaStore query, service boundary, low-friction preferences | The requested owner/repo was not found; reference repo uses legacy MediaPlayer and is not a hi-res engine |
| [Spotube](https://github.com/team-spotube/spotube) | Strong separation between metadata providers and audio-source providers; capability badges; URL refresh; fallback candidates | BSD-4-Clause for the app; extractor-oriented source integrations are fragile and must not be treated as permission to download |
| [InnerTune](https://github.com/z-huang/InnerTune) | Compose + Room + Media3; format metadata; separate player cache and download cache; bounded downloads | GPLv3; undocumented provider endpoints/cookies; provider quality is not proof of lossless/hi-res rights |
| [ViMusic](https://github.com/vfsfitvnm/ViMusic) | Room migrations, persistent queue/lyrics/history, Compose player UX, transient cache | Archived; GPLv3; undocumented source; cache is not a durable authorized download |
| [RiMusic](https://github.com/fast4x/RiMusic) | Multimodule Compose; Media3 service; streaming cache vs DownloadService separation | Requested repo identity was unavailable; successor is closed; source/provider model has compliance and maintenance risks |
| [Harmonoid](https://github.com/harmonoid/harmonoid) | Pooled tag parsing, cached SQLite media library, media_kit/mpv, adaptive UI, platform controls | PolyForm Strict license; not a conventional permissive license; broad codec support does not guarantee Android bit-perfect output |
| [BlackHole reference] | Historical examples of source/cache/offline product concepts | Exact requested repo was unavailable; related projects have conflicting licenses and service-term risks; do not copy extraction/download behavior |
| [OtoMusic reference] | No verifiable source findings | Exact requested repo was unavailable; do not assume its license or implementation from a similarly named Play app |
| [AndroidX Media3](https://github.com/androidx/media) | Official Player, MediaSession, MediaLibraryService, DownloadService/DownloadManager, cache, audio sink/offload reporting | Infrastructure is not content permission; output and codec support are device-dependent |

### Key conclusion

The only durable architecture across the good projects is **separation of concerns**:

1. Source/provider discovery
2. Authorization and entitlement
3. Metadata normalization
4. Offline storage/download lifecycle
5. Local library/indexing
6. Playback/session/output diagnostics
7. UI

Keep those boundaries from day one.

---

## 2. Recommended module structure

```text
app/                         Compose navigation, permissions, DI, lifecycle
core:model/                  Track, album, artist, playlist, source, entitlement models
core:database/               Room entities, DAOs, migrations and indexes
core:library/                MediaStore/SAF scanning, tag extraction, normalization
core:playback/               Media3 player, session/service, queue and diagnostics
core:provider-api/           Stable provider interfaces and capability/rights contracts
provider:local/              MediaStore, SAF and user-owned file provider
provider:<licensed-source>/  Official API/SDK adapter; no UI or Media3 leakage
core:download/               Download policy, manifests, checksums and storage lifecycle
worker/                      WorkManager indexing, artwork, sync, cleanup, revalidation
feature:home/
feature:search/
feature:library/
feature:player/
feature:downloads/
feature:settings/
```

Use **Compose only at the presentation layer**. Large lists, downloads and playback must not be owned by Composables. The service owns ExoPlayer; a repository/`MediaController` exposes immutable state to ViewModels.

### Provider contracts

```kotlin
interface CatalogProvider {
    suspend fun search(query: String, page: String?): Page<CatalogItem>
}

interface MetadataProvider {
    suspend fun album(id: ProviderId): AlbumDetails
}

interface PlaybackProvider {
    suspend fun resolve(item: CatalogItem): PlayableResource
}

interface OfflineProvider {
    suspend fun entitlement(item: CatalogItem): OfflineEntitlement
    suspend fun createDownload(item: CatalogItem): AuthorizedDownload
}
```

Each provider advertises capabilities such as:

- `SEARCH`
- `STREAM`
- `DOWNLOAD`
- `LOSSLESS`
- `HI_RES`
- `DRM`
- `EXPIRING_URL`
- `OFFLINE_WINDOW`

The app must **fail closed** when a provider does not explicitly grant an operation. Start with compile-time provider modules; only consider community plugins in v2, with signed manifests, declared capabilities, permission review and isolated execution.

---

## 3. Hi-res audio strategy

### Start with a defensible format target

MVP should support and test:

- **FLAC** — primary lossless format, including common 16/24-bit and 44.1/48/96/192 kHz files where the device/output permits
- **WAV/PCM** — preserve sample rate, channels and bit depth; validate malformed headers
- **AAC, MP3, Opus** — compatibility formats, clearly labeled as lossy

Defer AIFF, APE, WMA, DSF/DSD, WAVPack and ALAC until there is a maintained decoder, license review, ABI/package-size review and device test matrix. Use the [Media3 supported-formats guide](https://developer.android.com/media/media3/exoplayer/supported-formats) as the baseline, not as a promise of universal output.

### Record four different quality facts

For every playable track, retain and display separately:

1. **Source format:** codec, container, sample rate, bit depth/sample format, channels, bitrate and provider quality tier
2. **Decoder output:** PCM format and platform/software decoder used
3. **Audio sink:** negotiated encoding, sample rate, channel mask, offload/passthrough status and whether Android resampled
4. **Physical route:** phone speaker, wired headset, USB DAC, Bluetooth codec/profile or cast target

Add an **Audio diagnostics** screen. For example:

```text
Source       FLAC · 24-bit · 96 kHz · stereo
Decoder      Media3 FLAC decoder
Output       PCM · 24-bit · 96 kHz
Route        USB DAC · 96 kHz
Processing   ReplayGain OFF · EQ OFF
Offload      unavailable: route/device limitation
```

### Offload and processing

Use Media3’s audio capability reporting and test real devices. Offload depends on Android API level, encoding, sample rate, channel configuration, `AudioManager`, gapless support and variable-rate support. When offload is unavailable, fall back cleanly to PCM.

ReplayGain, EQ, crossfade, speed/pitch, normalization and other processors can disable offload or change the bit-perfect path. Make that visible instead of silently claiming hi-res.

### Minimum device test matrix

- Android API 26, 29, 33 and 35+
- At least two vendors and low/mid/high-tier hardware
- Wired output, Bluetooth SBC/AAC and available high-quality codecs
- Two USB DACs with different maximum sample rates
- 44.1/48/96/192 kHz; 16/24-bit FLAC/WAV
- Gapless albums, seek, screen-off, process death and route changes
- ReplayGain/EQ on and off
- Storage-full, decoder error and malformed-file fixtures

---

## 4. Downloads, cache and rights

Use three explicit concepts:

### A. Playback cache
Bounded, evictable, app-private bytes for smooth streaming. It is **not** a permanent download and conveys no rights.

### B. Authorized offline asset
Durable provider-permitted media with user-visible state, terms, expiry and revalidation.

### C. Local/user-owned asset
A file or URI selected/imported by the user through MediaStore or SAF.

Use Media3 `DownloadService`/`DownloadManager` for authorized media jobs. Use WorkManager for provider sync, metadata/artwork, cleanup, entitlement revalidation and local-library reconciliation.

### Suggested Room entities

- `SourceAccount(providerId, accountId, region, authState, tokenRef)`
- `CatalogItem(providerId, providerItemId, title, album, artists, artworkRef)`
- `TrackVersion(canonicalTrackId, sourceId, codec, mime, sampleRate, bitDepth, channels, bitrate)`
- `Entitlement(assetId, providerId, accountId, permission, termsVersion, territory, expiresAt, revocable)`
- `OfflineAsset(assetId, localUri, state, bytes, checksum, createdAt, lastValidatedAt, expiresAt)`
- `DownloadJob(assetId, requestId, state, progress, retryCount, errorCode)`
- `LocalFile(uri, documentId, size, modifiedAt, fingerprint, scanState)`
- `Track`, `Album`, `Artist`, `Genre`, `Playlist` and join tables
- `PlaybackEvent`, `QueueItem`, `Favorite`, `Lyrics`, `Artwork`

Use stable provider IDs plus a local canonical ID. Never use an expiring stream URL as identity. Use atomic temp-file-to-final-URI moves, resumable jobs, checksums where permitted, bounded concurrency, migration/versioning, orphan cleanup, retry UI and clear deletion behavior.

Do not export provider-controlled DRM assets as normal audio files. Store tokens and sensitive entitlement state in Keystore-backed secure storage. Handle logout, entitlement expiry, revocation and account removal according to provider policy.

---

## 5. UX that stays simple and fast

Use a persistent mini-player and one primary action per screen:

1. **Home:** recently played, favorites, downloads in progress, local/authorized sources and quality badges
2. **Search:** debounced/paginated search, source attribution and clear download-eligibility status
3. **Library:** Songs, Albums, Artists, Genres, Playlists, Folders and Downloads
4. **Detail:** artwork, metadata, quality/availability badges, queue/add/download actions and licensed lyrics
5. **Now Playing:** transport, seek, queue, output route and diagnostics
6. **Queue:** reorder, remove, save as playlist and persistent state
7. **Downloads:** active/completed/expired/failed, retry, storage use, terms/expiry and delete
8. **Settings:** accounts, library locations, quality policy, quotas, processing, diagnostics, privacy and licenses

Performance rules:

- Stable keys and immutable UI models in Compose lists
- Paging or bounded lists for large libraries
- Coil thumbnail caching with cancellation and memory limits
- Baseline Profiles and Macrobenchmark from the first usable build
- Incremental scan cache keyed by URI/document identity, size and modification time
- Bounded coroutine concurrency and per-file failure isolation
- No disk/network/tag parsing on the main thread
- Pre-prepare the next track, but test format changes and gapless transitions
- Handle audio focus, becoming-noisy, media buttons, Bluetooth/USB route changes, process death, expired URLs and revoked SAF permissions

---

## 6. Phased delivery plan

### Phase 0 — Product and rights gate

Before coding a provider integration, decide:

- Which licensed provider(s) will launch?
- Does each permit search, playback, durable offline bytes or only a time-limited encrypted license?
- Can the user export/backup files?
- What Android API and USB/Bluetooth device matrix is required?
- Is “hi-res” a measured USB output requirement or only a source-quality label?
- Is GPL source reuse acceptable, or must the app remain proprietary/permissively licensed?

### Phase 1 — Native local-player foundation

- Kotlin/Compose/Material 3
- Media3 `MediaSessionService` / `MediaLibraryService`
- MediaStore + user-selected SAF folders
- Room library with incremental scan and full-rescan recovery
- FLAC/WAV/AAC/MP3/Opus according to tested support
- Queue, playlists, favorites, history and notification controls
- Android Auto-compatible metadata
- Audio diagnostics screen
- Baseline Profile, Macrobenchmark, lint and license scanning

### Phase 2 — One verified provider

- Official API/SDK only
- OAuth/browser authorization where needed
- Search and metadata first
- Playback only after entitlement and terms review
- Download only if the provider explicitly grants offline use
- Provider-specific errors, region/account handling and attribution

### Phase 3 — Durable offline and polish

- Resumable downloads and checksums where allowed
- Expiry/revalidation and migration/orphan repair
- Storage quotas and cleanup UI
- Better tag normalization and MusicBrainz IDs where licensed
- Licensed lyrics/artwork
- Tablet/foldable layouts, widgets, sleep timer and ReplayGain
- USB DAC/Bluetooth compatibility matrix

### Phase 4 — Advanced providers and output

- Second licensed provider
- Signed capability-declared provider SDK
- Official DRM/offline-license integration where available
- Optional ALAC/AIFF/DSD after decoder/legal/device validation
- Cloud sync of metadata/playlists, not unauthorized audio bytes
- Automated device-lab regression testing

---

## 7. Antigravity / Manus tools and MCPs

### Core development tools

- Android Studio, Android SDK/ADB, emulator and a physical USB DAC test setup
- Gradle Kotlin DSL, Kotlin, pinned JDK 17/21 toolchain
- Jetpack Compose, Material 3, Navigation Compose, Lifecycle/ViewModel
- Hilt/KSP, Room, WorkManager, Media3 ExoPlayer/Session/Database/Download
- Android Profiler, Perfetto, Macrobenchmark, Baseline Profile and StrictMode
- Android Lint, Detekt, Ktlint, dependency locking and version catalogs
- SBOM/license scanning and reproducible CI builds
- `ffprobe`/MediaInfo and generated audio fixtures
- `adb shell dumpsys media.player`, `media_session` and audio diagnostics

### MCPs / integrations to enable

1. **GitHub MCP** — inspect pinned commits, source trees, licenses, submodules, issues and release history. Use it for provenance and inspiration, not blind code copying.
2. **Web/search/fetch MCP** — official AndroidX Media3 docs, provider developer docs/terms, codec/native-library licenses and release notes.
3. **Filesystem/terminal MCP** — Gradle builds, unit/instrumentation tests, static analysis, APK inspection and fixture generation.
4. **Android/ADB device MCP, if available** — install APKs, drive playback/download flows, capture `dumpsys`, and execute device-route matrices.
5. **Issue/project MCP, if available** — track provider approvals, legal review, device compatibility, and release gates.
6. **Manus workflow MCP** — parallel repository research, provider-document comparison and structured architecture reviews.
7. **Manus Tasks MCP** — turn the phased plan into tracked implementation tasks and milestones.
8. **Webdev MCP** — not needed for the Android runtime itself; useful only if you also want a web admin, landing page or documentation site.
9. **Image/audio tools** — optional for app icon, artwork placeholders and generating controlled audio test fixtures; do not use generated/found artwork without the required rights.

### Recommended Antigravity workflow

```text
Plan → pin references/versions → scaffold modules → local playback →
local indexing → diagnostics → provider rights review → provider adapter →
authorized offline → device matrix → performance/security review → release
```

Keep provider acceptance, license decisions and audio-output test results as release-blocking artifacts rather than informal notes.

---

## 8. Decisions to avoid

- Do not use undocumented provider endpoints, scraping, cookie reuse to evade access controls, signature bypass, DRM circumvention or rate-limit evasion.
- Do not call an automatic cache a legal permanent download.
- Do not promise “bit-perfect,” “24-bit,” “192 kHz” or “hi-res” from a file extension alone.
- Do not mix UI, provider networking, scanning, downloads and playback in one monolithic module.
- Do not reuse GPL/PolyForm code without an explicit licensing decision and full dependency audit.
- Do not ship moving branches or unpinned Media3/native dependencies.
- Do not grant arbitrary community plugins unrestricted code, network or filesystem access.
- Do not request broad storage permissions when MediaStore/SAF is sufficient.

## Bottom line

The best build is a **small, native, capability-aware Android core** first. Make lawful provider integration replaceable, model rights independently from bytes, keep playback cache separate from authorized offline assets, and market hi-res only after measuring the full source → decoder → Android sink → physical output path on real hardware.
