# IC4J Camel 0.8.5 Review

Review date: 2026-09-06. Scope: core component initialization, reusable MCP
schema/catalog entry points, build metadata, and resolved Maven dependencies.
This is a source review and advisory lookup, not a penetration test or a
guarantee that every vulnerability has been found.

## Remaining Findings

### High: Tracked Private Keys Require Owner Review

[Ed25519_identity.pem](../Ed25519_identity.pem) and
[Secp256k1_identity.pem](../Secp256k1_identity.pem) are tracked and contain
private-key headers. Only headers and Git metadata were inspected; no private
key contents were printed. If either key has ever been trusted outside disposable
tests, consider it exposed and replace its identity/permissions. Removing the
current file does not revoke a key or remove it from Git history. Rotation,
authorization changes, and coordinated history removal require owner action;
this update does not delete or rotate these keys.

### High, Conditional: Undertow Multipart Denial of Service

OSV reports [CVE-2026-3260 / GHSA-3x3v-w654-m28m](https://osv.dev/vulnerability/GHSA-3x3v-w654-m28m)
for `io.undertow:undertow-core`, reachable as a dependency through
`camel-mcp` and `camel-undertow`. Both 2.4.2.Final and the updated latest
2.4.3.Final match the advisory. The advisory lists no fixed release.
Exploitability of this application's particular routes was not demonstrated.

Keep public MCP ingress behind authenticated, rate-limited access with request
size/time limits. Reject multipart requests, especially GET requests with a
multipart Content-Type, at the ingress layer when not required. No ingress
configuration is changed here. Track an upstream fix; upgrading to 2.4.3.Final
alone must not be represented as resolving this advisory.

## Fixed Findings

- **High: Untrusted root-key replacement.** MCP schema discovery previously
  fetched a root key unconditionally. It now retains the built-in trust anchor
  unless the server explicitly sets `icp.mcp.fetchRootKey=true` for a trusted
  development replica. Remote arguments cannot set this property.
- **High: Caller-controlled outbound destinations and resource selection.**
  Both MCP tools/call and tools/list previously allowed callers to replace
  location, canister, classpath DID resource, and POJO class defaults. Requests
  must now match server configuration, preventing arbitrary target selection
  through these entry points. Deployments must not derive those server
  properties from untrusted headers or request fields.
- **Medium: Class initialization during schema inspection.** POJO lookup now
  uses non-initializing class loading. A regression test proves inspecting a
  POJO does not execute its static initializer. Server-selected classes must
  still be trusted, including enum classes used during reflection.
- **Medium: Silent identity fallback and broken startup.** Unknown identity or
  transport names now fail startup; injected identity and transport instances
  are honored. Initialization failures are propagated instead of logged and
  suppressed.
- **Medium: Discovery connection lifecycle.** Schema discovery closes its
  transport in a finally block, including failure paths.
- **Build correctness:** Attached fat-JAR creation no longer removes dependency
  declarations from the normal installed artifact POM. The core build compiles
  its module-local generated Camel sources and publishes 0.8.5 metadata.

## Dependency Decisions

- Local IC4J agent, Candid, and Java 11 transport: 0.8.5.
- Apache Camel: 4.22.0, verified from Maven Central release metadata.
- Jackson 2.22.2, Gson 2.14.0, OkHttp JVM 5.5.0, Commons Lang 3.20.0,
  HttpClient 5.6.4, SLF4J 2.0.19, JUnit 6.1.3, Jakarta JAXB API 4.0.5,
  JAXB runtime 4.0.9, BC provider 1.85.2 and PKIX 1.85.
- BOMs align related Camel, Jackson, OkHttp, SLF4J, and JUnit artifacts.
- Latest stable releases are used, excluding alpha/beta/milestone releases.
  Jackson stays on the compatible 2.x namespace required by IC4J and Camel.
- Legacy JAXB coverage deliberately retains `javax.xml.bind:jaxb-api:2.3.1`,
  `jaxb-impl:2.3.9`, and Prowide `SRU2023-9.4.7`. The newest Prowide major
  release fails the existing javax JAXB regression because it uses Jakarta.

## Verification And Limits

Nine focused offline regressions cover core initialization, resource selection
in both MCP processors, default root-key trust, inline DID schema generation,
and non-initializing POJO lookup. The legacy JAXB parsing test also runs.
Six integration tests require explicit configuration and are skipped in default
builds. Two local Motoko tests failed when automatically activated by a saved
canister-ID file in the export build; the local fixture was not redeployed.
They now require `-Dic4j.test.local.sample=true`, and enabled failures still
fail the build. Public network tests and secp256k1 fixture tests remain opt-in.

The final OSV query of 125 unique resolved reactor coordinates (including test
dependencies) found only the Undertow advisory above. The editor's dependency
scanner separately reports a medium Jackson databind 2.22.2 warning without an
advisory ID; the live OSV API returned no match for that version. Treat this
scanner discrepancy as untriaged, not proof of safety. Local unpublished IC4J artifacts may not
have advisory coverage. Build plugins, Java/JDK vulnerabilities, infrastructure,
and the untracked OKTE fixture were not included in that dependency scan.
The local JDK is 21.0.2; use a currently patched JDK 21 distribution before
production deployment.

Verified commands: `mvn clean install`, `mvn -f pom-export.xml clean install`,
`mvn -f samples/mcp-icp-motoko/pom.xml verify`, `gradle test`, and
`gradle -b build-export.gradle test`. The sample has no automated tests, so its
verification establishes build/dependency compatibility only. Both installed
Maven fat JARs passed a standalone JVM smoke test for Camel component discovery,
producer startup, and 0.8.5 metadata without network calls.

The Gradle builds still emit Gradle 9 deprecation warnings. Maven is the
canonical reactor/local installation path. The local `pom-export.xml` and
`build-export.gradle` files are ignored by the repository's existing Git rules;
changes to them remain local unless deliberately tracked. Editor classpath
diagnostics remain despite successful clean Maven and Gradle compilation.
No remote publication is performed.