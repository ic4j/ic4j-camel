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

import org.apache.camel.AsyncCallback;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultAsyncProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ICProducer extends DefaultAsyncProducer {
	
	private static final Logger LOG = LoggerFactory.getLogger(ICProducer.class);
	
	private CamelContext camelContext;
	
	private ICService service;
	

	public ICProducer(ICEndpoint endpoint) {
		
		super(endpoint);
		this.camelContext = endpoint.getCamelContext();
		
		this.service = new ICService(endpoint);	
	}
	
    @Override
    public ICEndpoint getEndpoint() {
        return (ICEndpoint) super.getEndpoint();
    }	

	@Override
	public boolean process(Exchange exchange, AsyncCallback callback) {
        try {
            LOG.trace("Exchange Pattern {}", exchange.getPattern());
            if (this.getEndpoint().getMethodType().equals(ICConfiguration.QUERY_PREFIX)) {
                return service.processQuery(exchange, callback);
            } else {
                return service.processUpdate(exchange, callback);
            }
        } catch (Throwable e) {
            exchange.setException(e);
            callback.done(true);
            return true;
        }
	}


}
