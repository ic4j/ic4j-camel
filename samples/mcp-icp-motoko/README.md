# MCP ICP Motoko Sample

This sample recreates the Motoko actor used by the test suite and exposes it through an MCP endpoint using Camel YAML routes plus the reusable ICP MCP module from this repository.

The default sample entrypoint has no custom Java application class. It uses:

1. `org.apache.camel.main.Main` as the runtime entrypoint.
2. `io.dscope.camel:camel-mcp` for the MCP consumer endpoint.
3. `org.ic4j:ic4j-camel-core` for the `ic:` calls.
4. `org.ic4j:ic4j-camel-mcp` for reusable Candid schema generation and generated `tools/list` handling.
5. Camel YAML DSL for the sample-specific business tools and response shaping.

An alternate kamelet-driven entrypoint is also included. It instantiates `icp-mcp-rest-service` via Camel Main route-template properties and uses one small sample-local processor bean for the business tools that remain sample-specific.

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

This sample resolves `org.ic4j:ic4j-camel-core:0.8.2` and `org.ic4j:ic4j-camel-mcp:0.8.2` from your local Maven repository. It still uses the base `org.ic4j` libraries such as `ic4j-agent`, `ic4j-java11transport`, and `ic4j-candid` at `0.8.0`, because this repository release only bumps the Camel artifacts. If you change code or resources in the root project and want the sample to pick them up, rerun `mvn -DskipTests install` from the repository root before restarting the sample.

## Build And Test

From the sample directory:

```bash
cd /Users/roman/Projects/eclipse-workspace/ic4j-camel/samples/mcp-icp-motoko
mvn test
```

At the moment this sample has no Java test sources, so `mvn test` is mainly a fast verification that Maven can resolve the locally installed Camel artifacts and compile the sample successfully.

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

## Run The Kamelet Variant

```bash
cd /Users/roman/Projects/eclipse-workspace/ic4j-camel/samples/mcp-icp-motoko
./run-kamelet-local.sh
```

This starts the default sample on port `3000` and also materializes the reusable `icp-mcp-rest-service` kamelet on port `3001` using [src/main/resources/application-kamelet.properties](/Users/roman/Projects/eclipse-workspace/ic4j-camel/samples/mcp-icp-motoko/src/main/resources/application-kamelet.properties). The kamelet-backed endpoint is available at `http://localhost:3001/mcp`.

The kamelet template uses `replicaUrl` rather than `location` as its configuration parameter name. Camel’s kamelet component already reserves `location` for locating kamelet definition files, so reusing that name for the ICP replica URL breaks route-template materialization.

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

The reusable kamelet provides `initialize`, `ping`, generated `tools/list`, and generated schema tools. The sample-local business tool processor used by the kamelet variant is [src/main/java/org/ic4j/sample/mcp/SampleIcpBusinessToolsCallProcessor.java](/Users/roman/Projects/eclipse-workspace/ic4j-camel/samples/mcp-icp-motoko/src/main/java/org/ic4j/sample/mcp/SampleIcpBusinessToolsCallProcessor.java).

## Notes

- The sample is intentionally YAML-only so it reuses the existing MCP and ICP Camel components directly.
- The alternate kamelet variant adds one small Java processor because the kamelet delegates sample-specific business tools to a bean while keeping shared MCP behavior reusable.
- The sample runs from its compiled resources plus the installed Maven-local `ic4j-camel-core` and `ic4j-camel-mcp` jars, not directly from the root reactor `target/classes`.
- The schema tools and generated `tools/list` output now come from the reusable ICP MCP processors in this repository, not from sample-local helper code.
- On a local `dfx` replica, update calls can still emit certificate verification noise from the underlying agent. The sample route tolerates that for the `greet` demo and reads back state with `getName`.
- The `camel-mcp` consumer uses MCP streamable HTTP validation. Your client must send `Accept: application/json, text/event-stream` and should send `MCP-Protocol-Version: 2025-06-18` on POST requests.
- If you omit the `Accept` header, the current `camel-mcp` consumer can respond with an empty `200 OK` before the route logic runs. That symptom usually means the HTTP transport headers were wrong, not that the sample route failed.

## Quick Tests

```bash
curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{"protocolVersion":"2025-06-18","clientInfo":{"name":"curl","version":"1.0.0"},"capabilities":{}}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":"2","method":"tools/list","params":{}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":"2b","method":"tools/list","params":{"catalogSource":"did"}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":"3","method":"tools/call","params":{"name":"greet","arguments":{"name":"Camel"}}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":"4","method":"tools/call","params":{"name":"get-name","arguments":{}}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":"5","method":"tools/call","params":{"name":"echo-pojo","arguments":{"bar":true,"foo":42}}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":"6","method":"tools/call","params":{"name":"schema-from-did","arguments":{"method":"echoOptionPojo"}}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":"6b","method":"tools/call","params":{"name":"schema-catalog-from-did","arguments":{}}}'

curl -s http://localhost:3000/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":"7","method":"tools/call","params":{"name":"schema-from-pojo","arguments":{"toolName":"complex-pojo","description":"Inspect generic POJO support","className":"org.ic4j.sample.mcp.SampleComplexPojo"}}}'

curl -s http://localhost:3001/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":"k1","method":"tools/list","params":{"catalogSource":"did"}}'

curl -s http://localhost:3001/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":"k2","method":"tools/call","params":{"name":"greet","arguments":{"name":"Kamelet"}}}'
```