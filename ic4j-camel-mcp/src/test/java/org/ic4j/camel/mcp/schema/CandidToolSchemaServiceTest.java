package org.ic4j.camel.mcp.schema;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.ic4j.agent.ReplicaTransport;
import org.junit.jupiter.api.Test;

class CandidToolSchemaServiceTest {

    @Test
    void preservesTrustedRootKeyByDefault() {
        String previous = System.getProperty(CandidToolSchemaService.PROPERTY_FETCH_ROOT_KEY);
        System.clearProperty(CandidToolSchemaService.PROPERTY_FETCH_ROOT_KEY);
        try {
            ReplicaTransport transport = (ReplicaTransport) Proxy.newProxyInstance(
                    ReplicaTransport.class.getClassLoader(), new Class<?>[] { ReplicaTransport.class },
                    (proxy, method, arguments) -> {
                        throw new AssertionError("Unexpected transport call: " + method.getName());
                    });
            assertNotNull(new CandidToolSchemaService().createAgent(transport));
        } finally {
            if (previous == null) {
                System.clearProperty(CandidToolSchemaService.PROPERTY_FETCH_ROOT_KEY);
            } else {
                System.setProperty(CandidToolSchemaService.PROPERTY_FETCH_ROOT_KEY, previous);
            }
        }
    }

    @Test
    void generatesSchemaFromInlineDid() throws Exception {
        var catalog = new CandidToolSchemaService().fromDidCatalog(
                Map.of("didText", "service : { greet : (text) -> (text) query; }"));
        assertEquals(1, catalog.get("count"));
    }

    @Test
    void generatesPojoSchemaWithoutInitializingClass() throws Exception {
        var schema = new CandidToolSchemaService().fromPojo(Map.of(
                "toolName", "example", "description", "Example", "className", SchemaPojo.class.getName()));
        assertNotNull(schema.get("function"));
        assertEquals(false, InitializationState.initialized);
    }

    public static class SchemaPojo {
        static {
            InitializationState.initialized = true;
        }
        public String value;
    }

    static class InitializationState {
        static boolean initialized;
    }
}