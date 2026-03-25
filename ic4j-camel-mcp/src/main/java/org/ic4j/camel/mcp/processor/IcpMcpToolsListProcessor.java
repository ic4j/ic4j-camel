package org.ic4j.camel.mcp.processor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.ic4j.camel.mcp.schema.CandidToolSchemaService;

import io.dscope.camel.mcp.processor.AbstractMcpResponseProcessor;
import io.dscope.camel.mcp.processor.McpToolsListProcessor;

@BindToRegistry("icpMcpToolsList")
public class IcpMcpToolsListProcessor extends AbstractMcpResponseProcessor {

	private final CandidToolSchemaService schemaService;
	private final McpToolsListProcessor defaultToolsListProcessor;

	public IcpMcpToolsListProcessor() {
		this(new CandidToolSchemaService(), new McpToolsListProcessor());
	}

	IcpMcpToolsListProcessor(CandidToolSchemaService schemaService, McpToolsListProcessor defaultToolsListProcessor) {
		this.schemaService = schemaService;
		this.defaultToolsListProcessor = defaultToolsListProcessor;
	}

	@Override
	protected void handleResponse(Exchange exchange) throws Exception {
		Map<String, Object> parameters = withDefaults(exchange, getRequestParameters(exchange));
		String catalogSource = text(parameters.get("catalogSource"));
		if ("did".equalsIgnoreCase(catalogSource)) {
			writeResult(exchange, schemaService.toolsListFromDid(parameters));
			return;
		}
		if ("endpoint".equalsIgnoreCase(catalogSource)) {
			writeResult(exchange, schemaService.toolsListFromEndpoint(parameters));
			return;
		}
		defaultToolsListProcessor.process(exchange);
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
		return value != null && !text(value).isBlank();
	}

	private String text(Object value) {
		return value == null ? "" : value.toString();
	}
}