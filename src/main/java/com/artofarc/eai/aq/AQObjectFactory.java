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
package com.artofarc.eai.aq;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;

public class AQObjectFactory implements ObjectFactory {

	@Override
	public Object getObjectInstance(Object object, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
		Object result = null;
		if (object instanceof Reference) {
			Reference reference = (Reference) object;

			Class<?> class1 = Class.forName(reference.getClassName());
			if (ConnectionFactory.class.isAssignableFrom(class1)) {
				Class<?> class2 = Class.forName("oracle.jms.AQjmsFactory");

				RefAddr refAddr = reference.get("dataSource");
				if (refAddr != null) {
					String ds = (String) refAddr.getContent();

					refAddr = reference.get("compliant");
					Boolean compliant = refAddr != null ? new Boolean((String) refAddr.getContent()) : null;
					
					InitialContext initialContext = new InitialContext();
					try {
						DataSource dataSource = (DataSource) initialContext.lookup(ds);
						if (compliant != null) {
							Method method = class2.getMethod("getConnectionFactory", DataSource.class, Boolean.TYPE);
							result = method.invoke(null, dataSource, compliant);
						} else {
							Method method = class2.getMethod("getConnectionFactory", DataSource.class);
							result = method.invoke(null, dataSource);
						}
					} finally {
						initialContext.close();
					}
				} else {
					refAddr = reference.get("jdbcURL");
					if (refAddr != null) {
						String url = (String) refAddr.getContent();

						Method method = class2.getMethod("getConnectionFactory", String.class, Properties.class);
						result = method.invoke(null, url, new Properties());
					}
				}
			}
		} else {
			throw new RuntimeException("Object " + object + " is not a reference");
		}
		return result;
	}

}
