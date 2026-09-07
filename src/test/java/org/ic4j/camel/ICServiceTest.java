package org.ic4j.camel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;

import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.identity.AnonymousIdentity;
import org.junit.jupiter.api.Test;

class ICServiceTest {

    @Test
    void honorsInjectedTransportAndIdentity() {
        ICEndpoint endpoint = endpoint();
        endpoint.setTransport(transport());
        endpoint.setIdentity(new AnonymousIdentity());
        endpoint.setIdentityType("configured-by-injection");
        endpoint.setTransportType("configured-by-injection");

        ICService service = assertDoesNotThrow(() -> new ICService(endpoint));
        assertNotNull(service.proxyBuilder);
    }

    @Test
    void rejectsUnknownTransport() {
        ICEndpoint endpoint = endpoint();
        endpoint.setTransportType("unsupported");
        assertThrows(IllegalStateException.class, () -> new ICService(endpoint));
    }

    @Test
    void rejectsUnknownIdentityInsteadOfUsingAnonymous() {
        ICEndpoint endpoint = endpoint();
        endpoint.setTransport(transport());
        endpoint.setIdentityType("unsupported");
        assertThrows(IllegalStateException.class, () -> new ICService(endpoint));
    }

    private ICEndpoint endpoint() {
        return new ICEndpoint("ic:query", new ICComponent());
    }

    private ReplicaTransport transport() {
        return (ReplicaTransport) Proxy.newProxyInstance(
                ReplicaTransport.class.getClassLoader(), new Class<?>[] { ReplicaTransport.class },
                (proxy, method, arguments) -> {
                    throw new AssertionError("Unexpected transport call: " + method.getName());
                });
    }
}