# IC4J Camel dependency pilot

An explicit `[run camel pilot]` commit to `ci/camel-dependency-pilot` allows
pre-merge testing. The workflow is also available for manual dispatch on
`master` or a follow-up branch. The pilot uses Java 21 because both Camel modules and
the pinned DScope component target Java 21. Agent's separate Java 8/11/21
matrix is unchanged.

Candid (including Gson), Agent, Java HTTP Transport and DScope Camel MCP
build from pinned Git revisions. The Agent and DScope pins use the commits
that landed on their default branches in [ic4j/ic4j-agent#9](https://github.com/ic4j/ic4j-agent/pull/9)
and [dscope-io/dscope-camel-mcp#9](https://github.com/dscope-io/dscope-camel-mcp/pull/9).
The Camel modules resolve the four dependencies through a fresh, same-job
Maven repository, with no package publication or shared cache. The logs
record source SHAs, artifact hashes and executed/skipped tests.

The reactor runs both Camel modules. The two Jackson/Candid integration tests
run against a disposable loopback Motoko canister. Remote tests and tests that
require an external signed fixture are intentionally disabled. The Kafka 4.3.1 smoke sends one synthetic record
through Camel to the local
canister, confirms the update by query, and verifies one committed offset. It
does not run the full IC4J Samples application or the MCP HTTP protocol.
