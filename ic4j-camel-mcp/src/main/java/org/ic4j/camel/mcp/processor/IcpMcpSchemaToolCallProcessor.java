package org.ic4j.camel.mcp.processor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.ic4j.camel.mcp.schema.CandidToolSchemaService;

import io.dscope.camel.mcp.processor.AbstractMcpRequestProcessor;

@BindToRegistry("icpMcpSchemaToolsCall")
public class IcpMcpSchemaToolCallProcessor extends AbstractMcpRequestProcessor {

	private static final List<String> SUPPORTED_TOOLS = List.of(
			"schema-from-endpoint",
			"schema-from-did",
			"schema-catalog-from-endpoint",
			"schema-catalog-from-did",
			"schema-from-pojo");

	private final CandidToolSchemaService schemaService;

	public IcpMcpSchemaToolCallProcessor() {
		this(new CandidToolSchemaService());
	}

	IcpMcpSchemaToolCallProcessor(CandidToolSchemaService schemaService) {
		this.schemaService = schemaService;
	}

	@Override
	protected void handleRequest(Exchange exchange, Map<String, Object> parameters) throws Exception {
		String toolName = getToolName(exchange);
		Map<String, Object> merged = withDefaults(exchange, parameters);
		Map<String, Object> payload = switch (toolName) {
		case "schema-from-endpoint" -> schemaService.fromEndpoint(merged);
		case "schema-from-did" -> schemaService.fromDid(merged);
		case "schema-catalog-from-endpoint" -> schemaService.fromEndpointCatalog(merged);
		case "schema-catalog-from-did" -> schemaService.fromDidCatalog(merged);
		case "schema-from-pojo" -> schemaService.fromPojo(merged);
		default -> null;
		};

		if (payload == null) {
			writeError(exchange, -32602, "Unknown tool: " + toolName, 400);
			return;
		}

		writeStructuredResult(exchange, payload);
	}

	public boolean supports(Exchange exchange) {
		return SUPPORTED_TOOLS.contains(getToolName(exchange));
	}

	private Map<String, Object> withDefaults(Exchange exchange, Map<String, Object> parameters) {
		Map<String, Object> merged = new LinkedHashMap<>();
		if (parameters != null) {
			merged.putAll(parameters);
		}
		applyDefault(exchange, merged, "location", CandidToolSchemaService.PROPERTY_LOCATION);
		applyDefault(exchange, merged, "canisterId", CandidToolSchemaService.PROPERTY_CANISTER_ID);
		applyDefault(exchange, merged, "didResource", CandidToolSchemaService.PROPERTY_DID_RESOURCE);
		applyDefault(exchange, merged, "className", CandidToolSchemaService.PROPERTY_DEFAULT_POJO_CLASS);
		return merged;
	}

	private void applyDefault(Exchange exchange, Map<String, Object> merged, String argumentName, String propertyName) {
		if (hasValue(merged.get(argumentName))) {
			return;
		}
		Object exchangeValue = exchange.getProperty(propertyName);
		if (exchangeValue != null) {
			merged.put(argumentName, exchangeValue);
		}
	}

	private boolean hasValue(Object value) {
		return value != null && !value.toString().isBlank();
	}

	private void writeStructuredResult(Exchange exchange, Map<String, Object> payload) {
		Map<String, Object> envelope = createEnvelopeSkeleton();
		envelope.put("id", getJsonRpcId(exchange));

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("content", List.of(Map.of("type", "text", "text", "JSON payload returned")));
		result.put("structuredContent", payload);
		envelope.put("result", result);

		writeJson(exchange, envelope);
		applyJsonResponseHeaders(exchange, 200);
	}

	private void writeError(Exchange exchange, int code, String message, int statusCode) {
		Map<String, Object> envelope = createEnvelopeSkeleton();
		envelope.put("id", getJsonRpcId(exchange));
		envelope.put("error", Map.of("code", code, "message", message));
		writeJson(exchange, envelope);
		applyJsonResponseHeaders(exchange, statusCode);
	}
}