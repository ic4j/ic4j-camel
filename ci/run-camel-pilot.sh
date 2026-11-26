#!/usr/bin/env bash
set -euo pipefail
: "${RUNNER_TEMP:?}" "${GITHUB_WORKSPACE:?}" "${CI_MAVEN_REPO:?}"
[[ "$CI_MAVEN_REPO" == "$RUNNER_TEMP/ic4j-maven" ]]
cd "$GITHUB_WORKSPACE"
reports="$RUNNER_TEMP/camel-pilot-reports"
mkdir "$reports"

for project in candid agent transport dscope camel; do
  printf '%s %s\n' "$project" "$(git -C "$project" rev-parse HEAD)"
done | tee "$reports/sources.txt"
for project in candid agent transport dscope; do
  var="${project^^}_REV"
  [[ "$(git -C "$project" rev-parse HEAD)" == "${!var}" ]]
done

bash agent/ci/run-pilot.sh build
m() {
  mvn --batch-mode --no-transfer-progress -Dmaven.repo.local="$CI_MAVEN_REPO" "$@"
}
m -f dscope/pom.xml clean install | tee "$reports/dscope-build.log"

manifest="$reports/input-jars.sha256"
(
  cd "$CI_MAVEN_REPO"
  for artifact in ic4j-candid ic4j-agent ic4j-java11transport; do
    sha256sum "org/ic4j/$artifact/0.8.5/$artifact-0.8.5.jar"
  done
  sha256sum io/dscope/camel/camel-mcp/1.5.0/camel-mcp-1.5.0.jar
) | tee "$manifest"
cmp dscope/target/camel-mcp-1.5.0.jar "$CI_MAVEN_REPO/io/dscope/camel/camel-mcp/1.5.0/camel-mcp-1.5.0.jar"

archive="$RUNNER_TEMP/dfx-linux.tar.gz"
curl --fail --silent --show-error --location --retry 3 --max-time 180 \
  https://github.com/dfinity/sdk/releases/download/0.32.0/dfx-0.32.0-x86_64-linux.tar.gz -o "$archive"
printf '218dad11e0519e11c7a310b5c7cb1eadff109bb5db1ea3432f08c870432a80fc  %s\n' "$archive" | sha256sum -c -
dfx_bin="$(mktemp -d "$RUNNER_TEMP/dfx-bin.XXXXXX")"
tar -xzf "$archive" -C "$dfx_bin" dfx
export PATH="$dfx_bin:$PATH"
export DFX_CONFIG_ROOT
DFX_CONFIG_ROOT="$(mktemp -d "$RUNNER_TEMP/dfx-config.XXXXXX")"
dfx --version | tee "$reports/dfx-version.txt"
[[ "$(dfx --version)" == 'dfx 0.32.0' ]]
trap '(cd camel/samples/mcp-icp-motoko && dfx stop) >> "$reports/dfx-stop.log" 2>&1 || true' EXIT
(
  cd camel/samples/mcp-icp-motoko
  dfx start --host 127.0.0.1:4943 --background
  curl --fail --silent --show-error --retry 15 --retry-delay 1 --retry-connrefused --max-time 3 \
    http://127.0.0.1:4943/api/v2/status >/dev/null
  dfx deploy --network local --identity anonymous --no-wallet motoko_sample
) 2>&1 | tee "$reports/dfx-start-deploy.log"

export IC4J_CI_CANISTER
IC4J_CI_CANISTER="$(cd camel/samples/mcp-icp-motoko; dfx canister --network local id motoko_sample)"
[[ "$IC4J_CI_CANISTER" =~ ^[a-z0-9]+(-[a-z0-9]+)+$ ]]
# Keep even the legacy route definitions away from public endpoints when
# Camel initializes them in tests. Change only the throwaway CI checkout.
cat > camel/src/test/resources/application.properties <<PROPERTIES
ic.location=http://127.0.0.1:4943/
ic.canister=$IC4J_CI_CANISTER
PROPERTIES

m -f camel/pom.xml -Dic4j.test.local.sample=true -Dic4j.test.remote=false \
  -Dic4j.test.local.secp256k1=false clean install | tee "$reports/camel-build.log"
(cd "$CI_MAVEN_REPO"; sha256sum -c "$manifest")
python3 - <<'PY' | tee "$reports/test-counts.txt"
from pathlib import Path
from xml.etree import ElementTree as ET

for module, required in (
    ('dscope', set()),
    ('camel/ic4j-camel-core', {'testAgentJacksonLoadIdlMap', 'testCamelJacksonLoadIdlMapLocal'}),
    ('camel/ic4j-camel-mcp', set()),
):
    cases = [c for p in Path(module, 'target/surefire-reports').glob('TEST-*.xml')
             for c in ET.parse(p).getroot().iter('testcase')]
    ran = [c for c in cases if c.find('skipped') is None]
    assert ran and required <= {c.attrib['name'] for c in ran}, f'Missing tests: {module}'
    print(f'{module}: {len(ran)} executed, {len(cases) - len(ran)} skipped')
PY

bash camel/ci/run-kafka-smoke.sh
