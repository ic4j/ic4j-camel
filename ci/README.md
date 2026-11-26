# IC4J Camel dependency pilot

An explicit `[run camel pilot]` commit to `ci/camel-dependency-pilot` allows
pre-merge testing; manual dispatch becomes available once the workflow is on
the default branch. The pilot uses Java 21 because both Camel modules and
the pinned DScope component target Java 21. Agent's separate Java 8/11/21
matrix is unchanged.

Candid (including Gson), Agent and Java HTTP Transport build first from pinned
Git revisions; DScope Camel MCP builds from the candidate in
[dscope-io/dscope-camel-mcp#9](https://github.com/dscope-io/dscope-camel-mcp/pull/9).
All five dependencies and Camel modules resolve through a fresh, same-job
Maven repository, with no package publication or shared cache. The logs
record source SHAs, artifact hashes and executed/skipped tests. Before merging
the pilot, review the upstream candidates and verify their accepted revisions.

The reactor runs both Camel modules. The two Jackson/Candid integration tests
run against a disposable loopback Motoko canister. Remote tests and tests that
require an external signed fixture are intentionally disabled. This initial
coverage does not validate a Kafka route or the MCP HTTP protocol.
