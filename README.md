## Apache Camel Internet Computer Component

Apache Camel is an Open Source integration framework that empowers you to quickly and easily integrate various systems consuming or producing data.

<a href="https://camel.apache.org/">
https://camel.apache.org/
</a>

The IC4J Camel component allows native execution of Internet Computer smart contracts from Apache Camel.

## Modules

The repository now builds as a small reactor:

- `org.ic4j:ic4j-camel-core`: the Internet Computer Camel component and generated Camel metadata
- `org.ic4j:ic4j-camel-mcp`: reusable ICP MCP schema builders, MCP processors, and ICP MCP kamelets

The MCP module currently includes reusable `icp-mcp-rest-service` and `icp-mcp-ws-service` kamelets under [ic4j-camel-mcp/src/main/resources/kamelets](/Users/roman/Projects/eclipse-workspace/ic4j-camel/ic4j-camel-mcp/src/main/resources/kamelets).


# Sample

A local MCP plus ICP sample is available at [samples/mcp-icp-motoko/README.md](/Users/roman/Projects/eclipse-workspace/ic4j-camel/samples/mcp-icp-motoko/README.md).

It now reuses the split `ic4j-camel-core` and `ic4j-camel-mcp` modules, keeping the sample-specific business tools in Camel YAML routes while sharing ICP MCP schema and catalog behavior from the reusable MCP module.

Quick start:

```bash
cd /Users/roman/Projects/eclipse-workspace/ic4j-camel/samples/mcp-icp-motoko
./run-local.sh
```

This starts a local `dfx` replica, deploys the sample Motoko canister, and serves an MCP endpoint at `http://localhost:3000/mcp`.

When testing that endpoint with `curl`, send `Accept: application/json, text/event-stream`. The underlying `camel-mcp` consumer uses MCP streamable HTTP validation, and missing that header can look like an empty `200 OK` instead of a route failure.


# Downloads / Accessing Binaries

To add Java IC4J Apache Camel Internet Computer Component library to your Java project use Maven or Gradle import from Maven Central.

Version 0.8.5 is built and installed locally by the commands below; a local install does not publish it to Maven Central.

<a href="https://search.maven.org/artifact/ic4j/ic4j-camel/0.8.2/jar">
https://search.maven.org/artifact/ic4j/ic4j-camel/0.8.2/jar
</a>

```
<dependency>
  <groupId>org.ic4j</groupId>
  <artifactId>ic4j-camel-core</artifactId>
  <version>0.8.5</version>
</dependency>

<dependency>
  <groupId>org.ic4j</groupId>
  <artifactId>ic4j-camel-mcp</artifactId>
  <version>0.8.5</version>
</dependency>
```

```
implementation 'org.ic4j:ic4j-camel-core:0.8.5'
implementation 'org.ic4j:ic4j-camel-mcp:0.8.5'
```

To install IC4J Camel component to Apache Karavan Visual Studio Code plug-in add 

```
ic
```

to .vscode/extensions/camel-karavan.karavan-4.60.0/components/components.properties file

and content of [src/ic.json](./src/ic.json) file to .vscode/extensions/camel-karavan.karavan-4.60.0/components/components.json file.

To enable the loading of IC4J libraries, you should move the [src/application.properties](./src/application.properties) file to the root of your project. 


# Build

You need JDK 21+ to build IC4J Apache Camel Internet Computer Component.

The build uses Apache Camel 4.22.0 and requires `org.ic4j:ic4j-agent`,
`org.ic4j:ic4j-candid`, and `org.ic4j:ic4j-java11transport` version 0.8.5
to be installed in the local Maven repository first.

```bash
mvn clean install
```

The reactor installs `ic4j-camel-parent`, `ic4j-camel-core`, and `ic4j-camel-mcp`.
The local export build can also install the legacy `org.ic4j:ic4j-camel` coordinate:

```bash
mvn -f pom-export.xml clean install
```

## MCP Security Configuration

MCP schema calls and catalog requests cannot override server-configured
`location`, `canisterId`, `didResource`, or `className`. Configure these with
the corresponding `icp.mcp.location`, `icp.mcp.canisterId`,
`icp.mcp.didResource`, and `icp.mcp.defaultPojoClass` exchange properties or
JVM system properties. Inline `didText` remains supported. Calls to the Java
schema service directly are trusted application APIs, not remote authorization boundaries.

Schema discovery keeps the built-in Internet Computer root key by default.
Only for a trusted development replica, start the JVM with
`-Dicp.mcp.fetchRootKey=true`. The local sample launcher sets this explicitly.
Never enable root-key fetching against production endpoints.

See [the 0.8.5 security review](docs/security-review-0.8.5.md) for fixed issues,
remaining dependency advisories, and required private-key handling.

# Tests

Default local verification:

```bash
mvn test
cd /Users/roman/Projects/eclipse-workspace/ic4j-camel/samples/mcp-icp-motoko && mvn test
```

### Opt-in local secp256k1 regression

The Camel component supports a DFX-style `secp256k1` PEM through
`identityType=secp256k1`. The following opt-in test checks a signed query
against a locally deployed canister without enabling any production network
calls. It requires a local replica, a canister exposing the query
`get_status : () -> (...)`, its Candid file, and the corresponding DFX PEM:

```bash
mvn -pl ic4j-camel-core -Dtest=CamelTest#testCamelSignedSecp256k1QueryLocal test \
  -Dic4j.test.local.secp256k1=true \
  -Dic4j.test.local.url=http://127.0.0.1:4943/ \
  -Dic4j.test.local.canister=LOCAL_CANISTER_ID \
  -Dic4j.test.local.pem=/absolute/path/to/secp256k1.pem \
  -Dic4j.test.local.idl=/absolute/path/to/canister.did
```

This test is deliberately disabled by default. It is intended to catch
compatibility regressions between IC4J request signing and DFX/PocketIC.

Notes:

- The local Motoko `echoPojo` integration tests require `-Dic4j.test.local.sample=true` and a working, deployed sample; a saved canister-ID file alone does not enable network tests.
- The core test suite now skips PEM-dependent routes when the PEM files are not present in test resources.
- Network-dependent public IC integration tests are opt-in so local test runs do not hang on remote replica calls.
- To enable those remote integration tests explicitly, run:

```bash
mvn test -pl ic4j-camel-core -am -Dic4j.test.remote=true
```
