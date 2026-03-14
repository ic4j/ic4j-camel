# MCP ICP Motoko Sample

This sample recreates the Motoko actor used by the test suite and exposes it through an MCP endpoint using only existing Camel components and YAML routes.

There is no custom Java application code in the sample. It uses:

1. `org.apache.camel.main.Main` as the runtime entrypoint.
2. `io.dscope.camel:camel-mcp` for the MCP consumer endpoint.
3. `org.ic4j:ic4j-camel` for the `ic:` calls.
4. Camel YAML DSL for all MCP request handling and response shaping.

## Prerequisites

- Java 21+
- Maven 3.9+
- `dfx`
- A local install of this repository artifact and the sibling Camel MCP component:

```bash
cd /Users/roman/Projects/DScope/CamelMcpComponent
mvn -DskipTests install

cd /Users/roman/Projects/eclipse-workspace/ic4j-camel
mvn -DskipTests install
```

This sample resolves `org.ic4j:ic4j-camel:0.8.0` from your local Maven repository. If you change code or resources in the root project and want the sample to pick them up, rerun `mvn -DskipTests install` from the repository root before restarting the sample.

## Run Everything

```bash
cd /Users/roman/Projects/eclipse-workspace/ic4j-camel/samples/mcp-icp-motoko
./run-local.sh
```

The script will:

1. Start a local `dfx` replica if needed.
2. Deploy the Motoko canister from this sample.
3. Resolve the generated canister id from `.dfx/local/canister_ids.json`.
4. Launch Camel Main with the YAML routes and the correct `sample.ic.canister` value.

## Manual Run

```bash
cd /Users/roman/Projects/eclipse-workspace/ic4j-camel/samples/mcp-icp-motoko
dfx start --background --clean
dfx deploy --yes

CANISTER_ID=$(python3 - <<'PY'
import json
with open('.dfx/local/canister_ids.json', 'r', encoding='utf-8') as handle:
    data = json.load(handle)
print(data['motoko_sample']['local'])
PY
)

mvn exec:java \
  -Dsample.ic.location=http://127.0.0.1:4943/ \
  -Dsample.ic.canister="$CANISTER_ID"
```

## MCP Tools

- `greet`: submits the Motoko `greet` mutation and then queries `getName` to return the current greeting through MCP
- `get-name`: query call to `getName() -> Text`
- `echo-pojo`: query call to `echoPojo(Entry) -> Entry` using Jackson input and Jackson output types on the `ic:` endpoint

## Notes

- The sample is intentionally YAML-only so it reuses the existing MCP and ICP Camel components directly.
- The sample runs from its compiled resources plus the installed Maven-local `ic4j-camel` jar, not directly from the root module's `target/classes`.
- On a local `dfx` replica, update calls can still emit certificate verification noise from the underlying agent. The sample route tolerates that for the `greet` demo and reads back state with `getName`.

## Quick Tests

```bash
curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":"2","method":"tools/list","params":{}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":"3","method":"tools/call","params":{"name":"greet","arguments":{"name":"Camel"}}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":"4","method":"tools/call","params":{"name":"get-name","arguments":{}}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":"5","method":"tools/call","params":{"name":"echo-pojo","arguments":{"bar":true,"foo":42}}}'
```