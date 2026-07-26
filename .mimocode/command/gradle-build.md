---
description: Build Android project with Gradle and show last N lines of output
---

# Gradle Build Command

## Overview

Builds the Android project using Gradle and shows the last N lines of output for quick error checking.

## Usage

```bash
./gradlew assembleDebug 2>&1 | tail -$1
```

## Parameters

- `$1`: Number of lines to show from the end of output (default: 5)

## Examples

```bash
# Build with default 5 lines
./gradlew assembleDebug 2>&1 | tail -5

# Build with 10 lines for more context
./gradlew assembleDebug 2>&1 | tail -10

# Build with 20 lines for detailed output
./gradlew assembleDebug 2>&1 | tail -20
```

## Error Handling

- Shows build success/failure status
- Displays last N lines for quick error identification
- Useful for CI/CD pipelines and automated builds

## Related Commands

- **gradle-clean** - Clean build artifacts
- **gradle-test** - Run unit tests

## Real-World Impact

From project sessions:
- Reduced build debugging time
- Quick identification of build failures
- Consistent build output format
