package org.ic4j.camel.mcp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ic4j.camel.mcp.schema.CandidToolSchemaService;
import org.junit.jupiter.api.Test;

class IcpMcpSchemaToolCallProcessorTest {

    @Test
    void usesServerConfigurationAndPreservesMethodArguments() throws Exception {
        try (var context = new DefaultCamelContext()) {
            var exchange = new DefaultExchange(context);
            exchange.setProperty(CandidToolSchemaService.PROPERTY_LOCATION, "https://icp-api.io/");
            var processor = new IcpMcpSchemaToolCallProcessor();
            var arguments = processor.withDefaults(exchange, Map.of("method", "greet"));
            assertEquals("https://icp-api.io/", arguments.get("location"));
            assertEquals("greet", arguments.get("method"));
        }
    }

    @Test
    void requiresResourceArgumentsToMatchServerConfiguration() throws Exception {
        try (var context = new DefaultCamelContext()) {
            var exchange = new DefaultExchange(context);
            var processor = new IcpMcpSchemaToolCallProcessor();
            var listProcessor = new IcpMcpToolsListProcessor();
            for (String argument : new String[] { "location", "canisterId", "didResource", "className" }) {
                assertThrows(IllegalArgumentException.class,
                        () -> processor.withDefaults(exchange, Map.of(argument, "not-configured")));
                assertThrows(IllegalArgumentException.class,
                        () -> listProcessor.withDefaults(exchange, Map.of(argument, "not-configured")));
            }
        }
    }

    @Test
    void acceptsExplicitlyConfiguredClass() throws Exception {
        try (var context = new DefaultCamelContext()) {
            var exchange = new DefaultExchange(context);
            exchange.setProperty(CandidToolSchemaService.PROPERTY_DEFAULT_POJO_CLASS, "sample.ConfiguredPojo");
            var processor = new IcpMcpSchemaToolCallProcessor();
            var arguments = processor.withDefaults(exchange, Map.of("className", "sample.ConfiguredPojo"));
            assertEquals("sample.ConfiguredPojo", arguments.get("className"));
        }
    }
}