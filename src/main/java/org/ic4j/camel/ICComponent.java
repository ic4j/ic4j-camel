/*
 * Copyright 2021 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.ic4j.camel;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriPath;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("ic")
public class ICComponent extends DefaultComponent {
	
	private static final Logger LOG = LoggerFactory.getLogger(ICComponent.class);

	@Override
	protected ICEndpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
		ICEndpoint endpoint = new ICEndpoint(uri, this);
		
		setProperties(endpoint, parameters);
		
		if (remaining.startsWith(ICConfiguration.QUERY_PREFIX))
			endpoint.setMethodType(ICConfiguration.QUERY_PREFIX);
		else
			endpoint.setMethodType(ICConfiguration.UPDATE_PREFIX);
		return endpoint;
	}

}
