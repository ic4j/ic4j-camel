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

<a href="https://search.maven.org/artifact/ic4j/ic4j-camel/0.8.1/jar">
https://search.maven.org/artifact/ic4j/ic4j-camel/0.8.1/jar
</a>

```
<dependency>
  <groupId>org.ic4j</groupId>
  <artifactId>ic4j-camel-core</artifactId>
  <version>0.8.1</version>
</dependency>

<dependency>
  <groupId>org.ic4j</groupId>
  <artifactId>ic4j-camel-mcp</artifactId>
  <version>0.8.1</version>
</dependency>
```

```
implementation 'org.ic4j:ic4j-camel-core:0.8.1'
implementation 'org.ic4j:ic4j-camel-mcp:0.8.1'
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

```bash
mvn -DskipTests install
```

# Tests

Default local verification:

```bash
mvn test -pl ic4j-camel-core -am
cd /Users/roman/Projects/eclipse-workspace/ic4j-camel/samples/mcp-icp-motoko && mvn test
```

Notes:

- The core test suite now skips PEM-dependent routes when the PEM files are not present in test resources.
- Network-dependent public IC integration tests are opt-in so local test runs do not hang on remote replica calls.
- To enable those remote integration tests explicitly, run:

```bash
mvn test -pl ic4j-camel-core -am -Dic4j.test.remote=true
```
