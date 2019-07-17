/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.artofarc.eai.naming;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

/**
 * The SPI seems to be ambiguous. Some JNDI Implementations (e.g. Tomcat) rely on <code>RefAddr</code> for the config params
 * while others (e.g. WildFly) provide config params through the <code>environment</code> parameter.
 * <br>This Adapter can be used in WildFly to use ObjectFactories from IBM MQ, MongoDB, etc.
 */
public class ObjectFactoryAdapter implements ObjectFactory {

	@Override
	public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
		if (environment == null) {
			throw new IllegalArgumentException("Environment expected");
		}
		@SuppressWarnings("unchecked")
		Map<String, String> env = new Hashtable<>((Map<String, String>) environment);
		String factory = env.remove("factory");
		if (factory == null) {
			throw new IllegalArgumentException("factory must not be null");
		}
		String type = env.remove("type");
		if (type == null) {
			throw new IllegalArgumentException("type must not be null");
		}
		Class<?> objectFactoryClass = Class.forName(factory);
		ObjectFactory objectFactory = (ObjectFactory) objectFactoryClass.newInstance();
		Reference reference = new Reference(type);
		for (Map.Entry<String, String> entry : env.entrySet()) {
			reference.add(new StringRefAddr(entry.getKey(), entry.getValue()));
		}
		return objectFactory.getObjectInstance(reference, name, nameCtx, null);
	}

}
