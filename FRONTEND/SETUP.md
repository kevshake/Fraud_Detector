# React Frontend Setup Guide

## ⚠️ IMPORTANT: Node.js Required

Before you can run the frontend, you **MUST** install Node.js.

### Install Node.js

1. Download Node.js LTS from: https://nodejs.org/
2. Run the installer
3. Restart your terminal/PowerShell
4. Verify installation:
   ```bash
   node --version
   npm --version
   ```

## Quick Start

Once Node.js is installed:

```bash
# Navigate to frontend directory
cd FRONTEND

# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at: **http://localhost:5173**

## Backend Connection

The frontend is configured to connect to the backend at:
- **URL:** http://localhost:2637
- **API Path:** /api/v1

Make sure the backend is running before starting the frontend.

## Original UI Files

All original static UI files are preserved in:
```
FRONTEND/public/legacy_ui/
```

Use these as reference when building React components.

## Next Steps

1. Install Node.js
2. Run `npm install`
3. Run `npm run dev`
4. Start migrating features from legacy UI to React components

See `README.md` for detailed documentation.
