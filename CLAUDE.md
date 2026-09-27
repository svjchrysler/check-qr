# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`README.md` is current and substantive: it covers the four platform constraints that shape the design, the privacy model, the correctness guarantees enforced by the database, and what is still missing before the pilot. Read it before changing anything in `capture/`, `voice/`, or the database layer. This file covers what it does not: the commands, and the things you only learn by reading several files at once.

## Language

**Everything user-facing and everything in the source is Spanish** — class names (`IngestorDePagos`, `PantallaDeCaja`), functions (`dedupKeyDe`, `diaSiguiente`), test names in backticks (`` `dos cobros del mismo monto en el mismo minuto no se funden` ``), comments, and commit messages. Match it. English identifiers in a new file will look like they came from somewhere else.

Comments explain **why**, not what, and they are worth writing at length where a decision is non-obvious or where the obvious alternative is wrong. The existing ones are the model to follow.

## Commands

```bash
./gradlew test                       # every JVM/Robolectric test, no emulator
./gradlew :app:installDebug          # build + install on the connected device
./gradlew assembleRelease            # verifies R8 still works; do this before calling a change done
./gradlew lintDebug                  # AGP lint (no ktlint/detekt/spotless in this project)
```

**Running a subset — the module type decides the task name.** `core:model`, `core:common` and `capture:parser` apply `checkqr.jvm.library` (pure Kotlin, no Android):

```bash
./gradlew :capture:parser:test --tests '*NoticeParserTest*'
```

Every other module is an Android library, where `test` runs all variants and `testDebugUnitTest` is the one you want:

```bash
./gradlew :core:data:testDebugUnitTest --tests '*IngestorDePagos*'
```

Backend (`server/`, Go + Postgres):

```bash
cd server && make test               # unit
cd server && make test-integracion   # runs migrar-test first; needs a local Postgres
cd server && make correr             # run it
cd server && make claves             # generate the Ed25519 pair
```

There are **no instrumented tests** (`src/androidTest` is empty). Database tests run under Robolectric in `src/test`, which is why `AndroidLibraryConventionPlugin` pins `testOptions.targetSdk` to `robolectricSdk` (36) — Robolectric 4.16 cannot run at 37. Don't remove that pin to "match" compileSdk.

## Testing on the device

The debug variant injects fake notices, so you never need a real transfer. `README.md` has the full command; two things silently waste time if you get them wrong:

- The action is `com.seef.checkqr.INYECTAR_AVISO` — **no `.debug`**, even though the applicationId has one. A wrong action is logged as ignored by `InyectorDeAvisos` and nothing else happens.
- Wrap the whole `am broadcast` in double quotes and the values with spaces in single quotes. Otherwise the device-side shell splits the text and `am` takes the first word as a different argument.

`--es clave '<key>'` simulates `StatusBarNotification.key`, which is what exercises the dedup paths. `adb logcat -s InyectorDeAvisos` shows what the parser decided.

Verifying the foreground service actually started (the riskiest platform requirement — see the README):

```bash
adb shell dumpsys activity services com.seef.checkqr | grep -E "isForeground|uidState"
```

Want `isForeground=true`, `types=0x00000002` (mediaPlayback) and `uidState: TOP`.

## Architecture

### Module tiers

Three pure-Kotlin modules hold the logic that changes most: `core:model` (domain, the dedup key), `core:common` (money, `MoneySpeller`, `Reloj`), `capture:parser` (bank templates + the notice corpus). They have no Android dependency on purpose — their tests run in a second. **New business logic belongs here unless it genuinely needs the framework.**

`core:data` is the seam: `IngestorDePagos` is the single path from a raw notice to a stored payment, and everything downstream (voice, widget, sync) hangs off `BusDeCaptura`. If you're tracing "what happens when a payment arrives", read `ServicioDeAvisosBancarios` → `NoticeParser.parse` → `IngestorDePagos.ingerir` in that order; nothing else is in the path.

### Build

There is no `android {}` block in feature modules. Seven convention plugins in the included build `build-logic/` own all of it (`checkqr.android.application|library|library.compose|feature|hilt|room`, `checkqr.jvm.library`). A new module applies one of them plus its own dependencies — copying another module's `build.gradle.kts` is the fast path. Versions live only in `gradle/libs.versions.toml`.

### Correctness is delegated to SQL

Deduplication (`UNIQUE` on `dedup_key` + `INSERT OR IGNORE`) and one-time receipt claiming (a single `UPDATE ... WHERE claimed_by_receipt_id IS NULL`) are enforced by the database, not by Kotlin. Both are documented in the README with the race each one closes. **Do not "simplify" either into a read-then-write.**

`Payment.dedupKeyDe` picks the strongest discriminator available — reference, else the system notification key plus a time bucket, else the time bucket alone. The ordering encodes a deliberate asymmetry: over-counting shows the merchant a duplicate they can correct, under-counting loses money invisibly, so the key prefers to separate. Read the KDoc before changing it.

### Money

Always `Long` cents, never `Float`/`Double`. The parser returns `null` rather than guess at an ambiguous amount.

### Variant-split debug tooling

The class that records every notification lives in `src/debug`; `src/main` has a no-op, and a per-variant Hilt module binds one or the other. The guarantee that release cannot record foreign notifications is structural, not a `BuildConfig.DEBUG` check. Keep it that way — new debug-only tooling goes in `src/debug` with its own binding, not behind a flag.

## Before the pilot

The bank package names in `capture/parser/src/main/resources/plantillas_base.json` are **unverified** and the corpus cases are synthetic. A wrong allowlist means zero captures, silently. The app warns on screen until real notices are captured. Treat any parser work as provisional until then — the README's "Lo que falta antes del piloto" has the full list, including the Play distribution constraint that makes a loose APK untestable.

---

You have OpenAI Codex and Gemini CLI configs at the user level. If you want their MCP servers, commands, subagents, skills or instructions brought into Claude Code, reply `/import` to scan and list what's importable, then `/import --yes=<digest>` (the scan output names the digest) to apply the user-level items. If `/import` isn't available here, run `claude import` from a terminal.
