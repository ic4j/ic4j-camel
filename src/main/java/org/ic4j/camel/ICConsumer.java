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

import org.apache.camel.Processor;
import org.apache.camel.Suspendable;
import org.apache.camel.support.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ICConsumer extends DefaultConsumer implements Suspendable {
	
	private static final Logger LOG = LoggerFactory.getLogger(ICConsumer.class);
	private ICEndpoint endpoint;

	public ICConsumer(ICEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
	}

}
