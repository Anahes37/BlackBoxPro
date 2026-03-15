## Plan: Code Review Fixes + NeoForge Multi-Module Restructure

### Part 1: Fix all 2nd round review issues (in existing files)

### Part 2: Restructure to multi-module Gradle project
- Root `build.gradle.kts` + `settings.gradle.kts` (multi-module)
- `fabric/` submodule (existing code, Fabric-specific)
- `neoforge/` submodule (new, NeoForge-specific)

### Part 3: Create NeoForge module
- `neoforge/build.gradle.kts` with NeoGradle
- `neoforge/src/main/resources/META-INF/neoforge.mods.toml`
- Platform-specific files (5 files) rewritten for NeoForge API

### Part 4: Update all documentation
