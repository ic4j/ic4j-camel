#!/usr/bin/env bash
set -euo pipefail
: "${RUNNER_TEMP:?}" "${GITHUB_WORKSPACE:?}" "${CI_MAVEN_REPO:?}" "${IC4J_CI_CANISTER:?}"
reports="$RUNNER_TEMP/camel-pilot-reports"
work="$(mktemp -d "$RUNNER_TEMP/kafka.XXXXXX")"
archive="$work/kafka.tgz"
version=4.3.1
distribution="kafka_2.13-$version"
for mirror in downloads.apache.org archive.apache.org/dist; do
  if curl --fail --silent --show-error --location --retry 2 --max-time 180 \
    "https://$mirror/kafka/$version/$distribution.tgz" -o "$archive"; then break; fi
done
printf 'c7d7b2318cb51aa0c61d3246a51c349210073c5c9b754947ef965a439f2f939e8600f204e134a75ac31faf3829c9370960ef7c6a9886c8a1dbf0339a21f4c54c  %s\n' "$archive" | sha512sum -c -
tar -xzf "$archive" -C "$work"
cd "$work/$distribution"
cat > "$work/broker.properties" <<PROPERTIES
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@127.0.0.1:19093
listeners=PLAINTEXT://127.0.0.1:19092,CONTROLLER://127.0.0.1:19093
advertised.listeners=PLAINTEXT://127.0.0.1:19092
controller.listener.names=CONTROLLER
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
inter.broker.listener.name=PLAINTEXT
num.partitions=1
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1
group.initial.rebalance.delay.ms=0
auto.create.topics.enable=false
log.dirs=$work/data
PROPERTIES
bin/kafka-topics.sh --version | tee "$reports/kafka-version.txt"
export KAFKA_HEAP_OPTS="-Xms256m -Xmx512m"
bin/kafka-storage.sh format -t "$(bin/kafka-storage.sh random-uuid)" -c "$work/broker.properties"
bin/kafka-server-start.sh "$work/broker.properties" >"$reports/kafka-broker.log" 2>&1 &
broker=$!
trap 'kill "$broker" 2>/dev/null || true; wait "$broker" || true' EXIT
ready=false
for _ in {1..20}; do
  kill -0 "$broker"
  if timeout 5s bin/kafka-topics.sh --bootstrap-server 127.0.0.1:19092 --list; then ready=true; break; fi
  sleep 1
done
"$ready"
timeout 30s bin/kafka-topics.sh --bootstrap-server 127.0.0.1:19092 \
  --create --topic ic4j-camel-smoke --partitions 1 --replication-factor 1
timeout 180s mvn --batch-mode --no-transfer-progress -Dmaven.repo.local="$CI_MAVEN_REPO" \
  -f "$GITHUB_WORKSPACE/camel/ci/kafka-smoke/pom.xml" -Dtest=KafkaCanisterTest test \
  | tee "$reports/kafka-smoke.log"
grep -Fq 'PASS: Kafka -> Camel -> local ICP UPDATE + QUERY; one record, committed offset 1' "$reports/kafka-smoke.log"
