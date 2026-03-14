#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

if ! command -v dfx >/dev/null 2>&1; then
  echo "dfx is required" >&2
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "mvn is required" >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required" >&2
  exit 1
fi

if ! dfx ping local >/dev/null 2>&1; then
  dfx start --background --clean
fi

dfx deploy

CANISTER_ID="$(python3 - <<'PY'
import json
with open('.dfx/local/canister_ids.json', 'r', encoding='utf-8') as handle:
    data = json.load(handle)
print(data['motoko_sample']['local'])
PY
)"

exec mvn exec:java \
  -Dsample.ic.location=http://127.0.0.1:4943/ \
  -Dsample.ic.canister="$CANISTER_ID"