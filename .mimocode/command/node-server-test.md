---
description: Start Node.js server and test with curl
---

# Node.js Server Test Command

## Overview

Starts a Node.js server in the background and tests it with curl to verify it's running correctly.

## Usage

```bash
node server/index.js &
sleep 2 && curl -s http://localhost:3000 | head -3
```

## Parameters

- **Port**: Replace `3000` with your server port
- **Server path**: Replace `server/index.js` with your server entry point
- **Wait time**: Adjust `sleep 2` for slower startup times

## Examples

```bash
# Default server on port 3000
node server/index.js &
sleep 2 && curl -s http://localhost:3000 | head -3

# Server on different port
node server/index.js &
sleep 2 && curl -s http://localhost:8080 | head -3

# Longer wait for slow startups
node server/index.js &
sleep 5 && curl -s http://localhost:3000 | head -3
```

## Error Handling

- Starts server in background
- Waits for server to initialize
- Tests with curl to verify response
- Shows first 3 lines of response for quick verification

## Related Commands

- **node-server-stop** - Stop the running server
- **node-server-logs** - View server logs

## Real-World Impact

From project sessions:
- Quick server verification after changes
- Consistent testing pattern
- Reduced manual server testing overhead
