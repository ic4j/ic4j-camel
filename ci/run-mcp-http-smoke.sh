#!/usr/bin/env bash
set -euo pipefail
: "${RUNNER_TEMP:?}" "${GITHUB_WORKSPACE:?}" "${CI_MAVEN_REPO:?}" "${IC4J_CI_CANISTER:?}"
[[ "$IC4J_CI_CANISTER" =~ ^[a-z0-9]+(-[a-z0-9]+)+$ ]]
reports="$RUNNER_TEMP/camel-pilot-reports"
sample="$GITHUB_WORKSPACE/camel/samples/mcp-icp-motoko"
classpath="$reports/mcp-http-classpath.txt"
cd "$sample"
mvn --batch-mode --no-transfer-progress -Dmaven.repo.local="$CI_MAVEN_REPO" \
  -Dmdep.outputFile="$classpath" compile dependency:build-classpath \
  | tee "$reports/mcp-http-build.log"
# Prove the sample is running against the artifacts built in this job.
for jar in \
  org/ic4j/ic4j-candid/0.8.5/ic4j-candid-0.8.5.jar \
  org/ic4j/ic4j-agent/0.8.5/ic4j-agent-0.8.5.jar \
  org/ic4j/ic4j-java11transport/0.8.5/ic4j-java11transport-0.8.5.jar \
  org/ic4j/ic4j-camel-core/0.8.5/ic4j-camel-core-0.8.5.jar \
  org/ic4j/ic4j-camel-mcp/0.8.5/ic4j-camel-mcp-0.8.5.jar \
  io/dscope/camel/camel-mcp/1.5.0/camel-mcp-1.5.0.jar; do
  tr ':' '\n' <"$classpath" | grep -Fx "$CI_MAVEN_REPO/$jar"
done
java -Dsample.mcp.bind=127.0.0.1:13000 \
  -Dsample.ic.location=http://127.0.0.1:4943/ \
  -Dsample.ic.canister="$IC4J_CI_CANISTER" \
  -cp "target/classes:$(cat "$classpath")" org.apache.camel.main.Main \
  </dev/null >"$reports/mcp-http-server.log" 2>&1 &
server=$!
trap 'kill "$server" 2>/dev/null || true; wait "$server" || true' EXIT
timeout 90s python3 "$GITHUB_WORKSPACE/camel/ci/mcp-http-smoke.py" \
  | tee "$reports/mcp-http-smoke.log"
