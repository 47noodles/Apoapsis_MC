# Releases and patch notes

Every Apoapsis release ships patch notes. No exceptions, including hotfixes.

## Version scheme

`MAJOR.MINOR.PATCH+MC`, for example `0.1.0+26.3`.

- **MINOR** steps with each roadmap stage before 1.0: 0.1 Engineers, 0.2 Orbits, 0.3 Round Worlds, 0.4 Landfall, 0.5 Together, 0.6 Expanse, 0.7 Beyond, then 1.0.
- **PATCH** covers fixes and small additions within a stage.
- **`+MC`** is the Minecraft version the build targets. A Minecraft version hop is listed in the notes and always comes with a tested save migration.
- Pre-releases use `-alpha.N`, `-beta.N` or `-rc.N`. Dev snapshots are nightly builds with no release number.

## What every release includes

1. **Player patch notes**: `docs/releases/<version>.md`, from the [template](TEMPLATE.md). The same text goes on Modrinth and CurseForge and, from 0.4 (when the new title screen arrives), into the in-game **What's new** screen shown on first launch after an update.
2. **A `CHANGELOG.md` entry**: a short technical summary under Added, Changed, Deprecated, Fixed, Removed, Security.
3. **Upgrade notes** when they apply: save-migration steps, server config changes, API changes for addon authors (semver, with a deprecation policy), and the Minecraft version hop.
4. **Credits**: contributors to that release, and new CC BY assets with their authors.

## Released versions

| Version | Date | Notes |
|---|---|---|
| Design draft 0.0.1 | 2026-10-11 | [CHANGELOG](../../CHANGELOG.md#design-draft-001--2026-10-11) (docs only, no game build) |
| 0.0.2-alpha.1 (Stage 0 prototype) | Unreleased, branch `main` | [Patch notes](0.0.2-alpha.1.md), [CHANGELOG](../../CHANGELOG.md#002-alpha1--stage-0-prototype) |
