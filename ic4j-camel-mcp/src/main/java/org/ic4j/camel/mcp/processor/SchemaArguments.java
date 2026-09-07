package org.ic4j.camel.mcp.processor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.ic4j.camel.mcp.schema.CandidToolSchemaService;

final class SchemaArguments {

    private SchemaArguments() {
    }

    static Map<String, Object> configured(Exchange exchange, Map<String, Object> parameters) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (parameters != null) {
            merged.putAll(parameters);
        }
        applyConfiguredValue(exchange, merged, "location", CandidToolSchemaService.PROPERTY_LOCATION,
                System.getProperty("sample.ic.location", CandidToolSchemaService.DEFAULT_LOCATION));
        applyConfiguredValue(exchange, merged, "canisterId", CandidToolSchemaService.PROPERTY_CANISTER_ID,
                System.getProperty("sample.ic.canister", ""));
        applyConfiguredValue(exchange, merged, "didResource", CandidToolSchemaService.PROPERTY_DID_RESOURCE,
                CandidToolSchemaService.DEFAULT_DID_RESOURCE);
        applyConfiguredValue(exchange, merged, "className", CandidToolSchemaService.PROPERTY_DEFAULT_POJO_CLASS, "");
        return merged;
    }

    private static void applyConfiguredValue(Exchange exchange, Map<String, Object> merged, String argumentName,
            String propertyName, String fallback) {
        String configured = exchange.getProperty(propertyName, String.class);
        if (!hasValue(configured)) {
            configured = System.getProperty(propertyName, fallback);
        }
        Object requested = merged.get(argumentName);
        if (hasValue(requested) && !requested.toString().equals(configured)) {
            throw new IllegalArgumentException(argumentName + " must match server configuration");
        }
        merged.put(argumentName, configured);
    }

    private static boolean hasValue(Object value) {
        return value != null && !value.toString().isBlank();
    }
}