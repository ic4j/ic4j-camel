package org.ic4j.camel.mcp.processor;

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

	Map<String, Object> withDefaults(Exchange exchange, Map<String, Object> parameters) {
		return SchemaArguments.configured(exchange, parameters);
	}

	private String text(Object value) {
		return value == null ? "" : value.toString();
	}
}