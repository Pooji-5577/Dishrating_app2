#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if rg -n "functions\\.invoke|io\\.github\\.jan\\.supabase\\.functions|install\\(Functions\\)|libs\\.supabase\\.functions" \
  composeApp/src composeApp/build.gradle.kts; then
  echo "Supabase Edge Function usage is not allowed in Dishrating_app2. Route through backend instead." >&2
  exit 1
fi

echo "Backend ownership guard passed."
