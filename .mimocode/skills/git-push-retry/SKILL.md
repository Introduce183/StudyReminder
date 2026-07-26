---
name: git-push-retry
description: Push to git remote with automatic retry logic for handling transient failures
---

# Git Push with Retry

## Overview

Network issues, authentication timeouts, and server-side rate limits can cause git push to fail intermittently. This skill provides a robust push with automatic retry logic.

## When to Use

Use when:
- Pushing to a remote repository
- Experiencing transient network failures
- GitHub/GitLab returns 4xx/5xx errors intermittently
- Push fails due to authentication timeout
- You want reliable pushes without manual retry

## The Command

```bash
for i in $(seq 1 10); do 
  git push origin main 2>&1 && break || echo "Attempt $i failed, retrying..."
done
```

## Parameters

- **Branch**: Replace `main` with your target branch
- **Max retries**: Adjust `10` to your preferred maximum attempts
- **Remote**: Replace `origin` with your remote name

## Example Usage

```bash
# Push to main with retry
for i in $(seq 1 10); do 
  git push origin main 2>&1 && break || echo "Attempt $i failed, retrying..."
done

# Push to develop branch
for i in $(seq 1 10); do 
  git push origin develop 2>&1 && break || echo "Attempt $i failed, retrying..."
done

# Push to specific remote
for i in $(seq 1 10); do 
  git push upstream main 2>&1 && break || echo "Attempt $i failed, retrying..."
done
```

## Error Handling

The command will:
1. Attempt to push
2. If successful, break out of loop
3. If failed, print error message and retry
4. Continue up to 10 attempts

## Related Skills

- **compose:debug** - For debugging persistent push failures
- **compose:verify** - Verify push succeeded

## Real-World Impact

From project sessions:
- Reduced manual retry overhead
- Handles transient network issues automatically
- Prevents interrupted workflows due to temporary failures
