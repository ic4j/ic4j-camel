package org.ic4j.camel.test;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.prowidesoftware.swift.model.mx.MxPain00100103;

public final class LegacyJaxbCamelTest {
	static final String SWIFT_XML_NODE_FILE = "CustomerCreditTransferInitiationV03.xml";

	@Test
	public void testLegacySwiftJaxbParsing() throws Exception {
		javax.xml.bind.JAXBContext legacyContext = javax.xml.bind.JAXBContext.newInstance(MxPain00100103.class);

		MxPain00100103 swiftJaxbValue = (MxPain00100103) legacyContext.createUnmarshaller()
				.unmarshal(new File(getClass().getClassLoader().getResource(SWIFT_XML_NODE_FILE).getFile()));

		Assertions.assertNotNull(swiftJaxbValue);
	}
}