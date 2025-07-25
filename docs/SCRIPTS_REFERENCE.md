# Scripts Reference

This document lists all essential scripts maintained in the project and their functions.

## Main Scripts

### 🔧 **Build and Release**

#### `scripts/check-release-needed.sh`
- **Function:** Analyzes changes and determines if a release is needed
- **Usage:** Automatic (GitHub Actions workflow)
- **Input:** List of modified files
- **Output:** `.release_check` file with information

#### `scripts/version-bump.sh`
- **Function:** Performs automatic semantic versioning
- **Usage:** Automatic (GitHub Actions workflow)
- **Input:** Detected changes
- **Output:** New calculated version and `.version_info` file

#### `scripts/build-with-docker-image.sh`
- **Function:** Build the JAR using the published Docker image
- **Usage:** Manual (development)
- **Command:** `./scripts/build-with-docker-image.sh`
- **Output:** JAR in `build/libs/aws-secretsmanager-apim-sdk-*.jar`

#### `scripts/generate-release-notes.sh`
- **Function:** Generates intelligent release notes based on commit history
- **Usage:** Automatic (GitHub Actions workflow)
- **Input:** Previous tag, new tag, release version
- **Output:** Comprehensive release notes with categorized changes

#### `scripts/analyze-changes.sh`
- **Function:** Analyzes specific code changes and detects detailed change types
- **Usage:** Called by release notes generator
- **Input:** Commit range
- **Output:** Detailed change analysis with impact assessment



### 📁 **Platform Scripts**

#### **Linux** (`scripts/linux/`)

##### `scripts/linux/install-filter.sh`
- **Function:** Installs the AWS Lambda filter on Linux
- **Usage:** Automatic (Gradle task `installLinux`)
- **Command:** `./gradlew installLinux`
- **Output:** Filter installed in Axway API Gateway

#### **Windows** (Gradle Tasks)

##### `./gradlew installWindows`
- **Function:** Interactive installation for Windows
- **Usage:** Manual (Windows)
- **Command:** `./gradlew installWindows`
- **Output:** YAML files installed in the Policy Studio project

##### `./gradlew installWindowsToProject`
- **Function:** Installation in a specific project
- **Usage:** Manual (Windows)
- **Command:** `./gradlew -Dproject.path=C:\path\to\project installWindowsToProject`
- **Output:** YAML files installed in the specific project

##### `./gradlew showAwsJars`
- **Function:** Shows AWS SDK JAR links
- **Usage:** Manual (Windows)
- **Command:** `./gradlew showAwsJars`
- **Output:** Links to download required JARs



## Final Structure

```
scripts/
├── 🔧 Main Scripts
│   ├── check-release-needed.sh          # Release analysis
│   ├── version-bump.sh                  # Semantic versioning
│   └── build-with-docker-image.sh       # Docker build
└── 📁 linux/
    └── install-filter.sh                # Linux installation

📋 **Gradle Tasks for Windows:**
├── ./gradlew installWindows             # Interactive installation
├── ./gradlew installWindowsToProject    # Installation in specific project
└── ./gradlew showAwsJars               # AWS JAR links
```

## Removed Scripts

The following scripts were removed as they were not essential:

### 🧪 **Test/Validation Scripts (Removed):**
- `verify-aws-lambda-values.sh` - AWS values verification
- `verify-filter-structure.sh` - Filter structure verification
- `test-preserve-other-filters.sh` - Filter preservation test
- `clean-and-reinstall.sh` - Clean and reinstall

### 🔧 **Fix Scripts (Removed):**
- `fix-internationalization-simple.sh` - Simple internationalization fix
- `fix-internationalization-correct.sh` - Correct internationalization fix
- `fix-internationalization-duplication.sh` - Duplication fix
- `test-internationalization-fix.sh` - Fix test

### 🪟 **Windows Scripts (Replaced by Gradle Tasks):**
- `install-filter-windows.ps1` - Replaced by `./gradlew installWindows`
- `install-filter-windows.cmd` - Replaced by `./gradlew installWindowsToProject`
- `configurar-projeto-windows.ps1` - Functionality integrated into tasks
- `test-internationalization.ps1` - Functionality integrated into tasks

### 🐳 **Docker Scripts (Removed):**
- `check-axway-jars.sh` - Axway JARs verification
- `debug-image.sh` - Image debug
- `docker-helper.sh` - Docker helper
- `start-gateway.sh` - Start gateway

## Recommended Usage

### 🔄 **Daily Development:**
```bash
# Local build
./scripts/build-with-docker-image.sh

# Test image
./scripts/test-published-image.sh

# Install on Linux
./gradlew installLinux
```

### 🏷️ **Releases:**
```bash
# Automatic via GitHub Actions
# (no manual commands needed)
```

### 🐳 **Docker:**
```bash
# Build the image
./scripts/docker/build-image.sh

# Build with Docker
./scripts/docker/build-with-docker.sh
```

### 🪟 **Windows:**
```powershell
# Configure project
.\scripts\windows\configurar-projeto-windows.ps1

# Install filter
.\scripts\windows\install-filter-windows.ps1

# Test internationalization
.\scripts\windows\test-internationalization.ps1
```

## Cleanup Benefits

### ✅ **Organization:**
- Essential scripts maintained
- Clear documentation
- Logical structure

### ✅ **Maintenance:**
- Fewer scripts to maintain
- Focus on essentials
- Reduced complexity

### ✅ **Performance:**
- Fewer files in the repository
- Faster builds
- Less overhead

## Next Steps

1. **Test** the maintained scripts
2. **Document** usage experiences
3. **Improve** scripts as needed
4. **Add** new scripts only if essential 