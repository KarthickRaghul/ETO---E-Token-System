#!/bin/bash
# Script to run the ETO Backend Server

# Navigate to the server directory relative to this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/server" || exit 1

# Check if node_modules exists, install if missing
if [ ! -d "node_modules" ]; then
    echo "Installing backend dependencies..."
    npm install
fi

echo "Starting ETO Node.js Backend Server..."
# Run with --watch mode if Node version supports it (Node 18+), otherwise fallback
if node --watch -e "process.exit(0)" &>/dev/null; then
    exec node --watch index.js
else
    if grep -q '"dev"' package.json; then
        exec npm run dev
    else
        exec node index.js
    fi
fi
