# Release Guide

This guide describes the automated release process for the `aws-secretsmanager-apim-sdk` project, as implemented in the GitHub Actions workflow (`.github/workflows/build-multi-version.yml`).

## Overview

Releases are fully automated and triggered by code changes pushed to the `master`, `main`, or `develop` branches. The workflow builds the SDK for all supported Axway API Gateway versions, applies semantic versioning, creates a tag if needed, and publishes a GitHub Release with all artifacts and **intelligent release notes**.

## Automated Release Flow

### Trigger
- **Push** to `master`, `main`, or `develop` branches (affecting code, build.gradle, gradle config, or axway-versions.json)
- **Manual dispatch** (workflow_dispatch) with optional version input
- **Pull requests** (for build/test only, no release)

### Main Steps
1. **Checkout code** (full history)
2. **Set up Docker Buildx**
3. **Parse Axway versions** from `axway-versions.json` (builds for all or a specific version)
4. **Run semantic versioning** (`scripts/version-bump.sh`)
   - Determines if a new version is needed
   - Updates `build.gradle` and `.version_info` as needed
5. **Build for each Axway version**
   - Uses Docker images for each version
   - Runs Gradle build and dependency copy
   - Produces a JAR for each version
6. **Upload JARs as workflow artifacts**
7. **Generate intelligent release notes** (`scripts/generate-release-notes.sh`)
   - Analyzes commit history and categorizes changes
   - Detects breaking changes, new features, bug fixes
   - Provides detailed technical analysis
8. **Create Release (on master only, not PRs)**
   - Determines the final version and tag (auto-increments if needed)
   - Updates `build.gradle` and pushes if version changed
   - Creates a tag (e.g., `v1.2.3`) and pushes it
   - Packages all build outputs and resources into ZIPs (one per Axway version)
   - Publishes a GitHub Release with intelligent release notes and all ZIPs attached

## Intelligent Release Notes

The system automatically generates comprehensive release notes that include:

### 📋 **Summary Section**
- Total number of commits
- Breakdown by change type (features, fixes, breaking changes, etc.)
- Visual indicators with emojis

### 🔍 **Detailed Changes**
- **Breaking Changes** 🚨 - API changes, removed methods/classes
- **New Features** ✨ - New functionality added
- **Bug Fixes** 🐛 - Issues resolved
- **Improvements** 🔧 - Performance and refactoring
- **Documentation** 📚 - Docs and guides updated
- **Maintenance** ⚙️ - Build and CI/CD changes

### 📊 **Change Analysis**
- File type breakdown (Java, Gradle, Docker, etc.)
- Code analysis (new/removed methods, classes)
- Build system changes
- Breaking change indicators
- Impact assessment

### 🔧 **Technical Details**
- Release date and commit range
- Contributor information
- Links to full changelog and documentation

## How to Trigger a Release

- **Just push your code to `master` or `main`** (or merge a PR). The workflow will:
  - Analyze if a release is needed (ignores doc-only or config-only changes)
  - Bump the version if required
  - Build and publish the release automatically
  - Generate intelligent release notes
- **Manual trigger:** Go to GitHub Actions > Build Multi-Version JARs > Run workflow, and optionally specify an Axway version.

## What Happens in a Release

- **Semantic versioning** is applied automatically based on commit messages and file changes.
- **A new tag** is created if a new version is detected (auto-incremented if the tag exists).
- **Intelligent release notes** are generated analyzing commit history and code changes.
- **A GitHub Release** is published with ZIPs for each supported Axway version and detailed release notes.
- **Each ZIP** contains everything needed for installation on Linux or Windows (via Gradle tasks).

## Release Notes Examples

### Example 1: Feature Release
```
# Release v2.1.0

## 📋 Summary
This release includes **15** commits with the following changes:

✨ **3 new feature(s)**
🐛 **2 bug fix(es)**
📚 **5 documentation update(s)**
⚙️ **3 maintenance task(s)**
🔧 **2 improvement(s)**

## 🔍 Detailed Changes

### ✨ New Features
- **feat: add AWS Lambda integration support** ([abc123](https://github.com/user/repo/commit/abc123)) - John Doe
- **feat: implement new filter configuration** ([def456](https://github.com/user/repo/commit/def456)) - Jane Smith

### 🐛 Bug Fixes
- **fix: resolve authentication issue** ([ghi789](https://github.com/user/repo/commit/ghi789)) - John Doe

## 📊 Change Analysis
- **Java files:** 8
- **Gradle files:** 2
- **Documentation files:** 5

### 🔍 Java Code Analysis
- **New methods added:** 12
- **New classes added:** 2
```

### Example 2: Breaking Change Release
```
# Release v3.0.0

## 📋 Summary
This release includes **8** commits with the following changes:

🚨 **2 breaking change(s)**
✨ **3 new feature(s)**
🔧 **3 improvement(s)**

## 🚨 Breaking Changes
- **feat!: refactor API interface** ([abc123](https://github.com/user/repo/commit/abc123)) - John Doe
- **fix!: remove deprecated methods** ([def456](https://github.com/user/repo/commit/def456)) - Jane Smith

## 📊 Change Analysis
- **Java files:** 6
- **Gradle files:** 1

### 🔍 Java Code Analysis
- **New methods added:** 8
- **Methods removed:** 3
- **New classes added:** 1
- **Classes removed:** 1

### 🚨 Breaking Change Indicators
⚠️ **Potential breaking changes detected:**
- 3 public method(s) removed
- 1 class(es) removed
- API configuration changes detected
```

## Release Artifacts

- **ZIP files**: One per Axway version, named `aws-secretsmanager-apim-sdk-<version>-<axway-version>-<date>.zip`
- **Contents:**
  - Main JAR (`aws-secretsmanager-apim-sdk-*.jar`)
  - `dependencies/` (external Gradle dependencies)
  - `src/main/resources/fed/` and `src/main/resources/yaml/` (Policy Studio resources)
  - `install-linux.sh` (Linux install script)
  - `gradlew`, `gradlew.bat`, `gradle/`, `build.gradle` (for Windows install via Gradle)
  - README with installation instructions

## Installation (from Release ZIP)

### Linux
1. Unzip the file
2. Set the `AXWAY_HOME` variable (e.g., `/opt/Axway/API_Gateway/7.7.0.20240830`)
3. Run: `./install-linux.sh`

### Windows (Policy Studio project)
1. Unzip the file
2. Open a terminal in the ZIP directory
3. Run: `./gradlew "-Dproject.path=C:\\path\\to\\your\\project" installWindowsToProject`

## Scripts Reference

### `scripts/generate-release-notes.sh`
- **Function:** Generates intelligent release notes based on commit history
- **Usage:** Automatic (GitHub Actions workflow)
- **Input:** Previous tag, new tag, release version
- **Output:** Comprehensive release notes with categorized changes

### `scripts/analyze-changes.sh`
- **Function:** Analyzes specific code changes and detects detailed change types
- **Usage:** Called by release notes generator
- **Input:** Commit range
- **Output:** Detailed change analysis with impact assessment

## Notes
- **No manual tag or release creation is needed.** The workflow handles versioning, tagging, and publishing.
- **If a tag already exists**, the workflow auto-increments the PATCH version and retries.
- **All build and release logs** are available in the GitHub Actions tab.
- **Only code, build, or resource changes** trigger a release. Documentation-only changes are ignored for versioning.
- **Release notes are automatically generated** with detailed analysis of changes and impact assessment.

## Useful Links
- **Workflow:** `.github/workflows/build-multi-version.yml`
- **Versioning:** `SEMANTIC_VERSIONING.md`
- **Documentation:** `README.md` 