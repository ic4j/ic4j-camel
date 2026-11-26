package org.ic4j.ci;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.consumer.KafkaManualCommit;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.ic4j.agent.Agent;
import org.ic4j.agent.http.ReplicaJavaHttpTransport;
import org.ic4j.camel.ICComponent;
import org.ic4j.candid.parser.IDLArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

class KafkaCanisterTest {
    @Test
    @Timeout(120)
    void kafkaRecordUpdatesCanisterAndCommitsExactlyOneOffset() throws Exception {
        var canister = System.getenv("IC4J_CI_CANISTER");
        assertNotNull(canister, "CI must deploy the fresh local canister");
        assertTrue(canister.matches("[a-z0-9]+(-[a-z0-9]+)+"));
        String topic = "ic4j-camel-smoke";
        String group = "ic4j-camel-smoke-group";
        String brokers = "127.0.0.1:19092";
        String kafka = "kafka:" + topic + "?brokers=" + brokers;
        String ic = "?url=http://127.0.0.1:4943/"
                + "&canisterId=" + canister
                + "&identityType=anonymous&transportType=java&fetchRootKey=true";
        String payload = "ci-" + UUID.randomUUID();
        var completed = new CompletableFuture<String>();
        var localRepo = Path.of(System.getenv("CI_MAVEN_REPO")).toRealPath();
        for (Class<?> dependency : new Class<?>[]{ICComponent.class, Agent.class, ReplicaJavaHttpTransport.class, IDLArgs.class}) {
            var jar = Path.of(dependency.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            assertTrue(Files.isRegularFile(jar) && jar.startsWith(localRepo.resolve("org/ic4j")),
                    () -> "Not a same-job IC4J artifact: " + jar);
        }

        try (var context = new DefaultCamelContext();
             var admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers))) {
            context.addRoutes(new RouteBuilder() {
                @Override public void configure() {
                    errorHandler(noErrorHandler());
                    from(kafka + "&groupId=" + group
                            + "&autoOffsetReset=earliest&autoCommitEnable=false&allowManualCommit=true")
                        .routeId("kafka-to-local-icp")
                        .to("ic:update" + ic + "&method=greet")
                        .process(exchange -> {
                            var result = exchange.getMessage().getBody(String.class);
                            exchange.getMessage().getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class).commit();
                            completed.complete(result);
                        });
                }
            });
            context.start();
            try (ProducerTemplate template = context.createProducerTemplate()) {
                assertEquals("Me", template.requestBody("ic:query" + ic + "&method=getName", null, String.class));
                template.sendBody(kafka, payload);
                assertEquals("Hello, " + payload + "!", completed.get(60, TimeUnit.SECONDS));
                assertEquals(payload, template.requestBody("ic:query" + ic + "&method=getName", null, String.class));
            }
            var partition = new TopicPartition(topic, 0);
            var committed = admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata()
                    .get(10, TimeUnit.SECONDS).get(partition);
            assertNotNull(committed);
            assertEquals(1L, committed.offset());
            assertEquals(1L, admin.listOffsets(Map.of(partition, OffsetSpec.latest()))
                    .partitionResult(partition).get(10, TimeUnit.SECONDS).offset());
            System.out.println("PASS: Kafka -> Camel -> local ICP UPDATE + QUERY; one record, committed offset 1");
        }
    }
}
