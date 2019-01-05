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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;

public final class AQObjectFactory implements ObjectFactory {

	@Override
	public Object getObjectInstance(Object object, Name name, Context nameCtx, Hashtable<?, ?> environment) throws ReflectiveOperationException, NamingException {
		Class<?> classAQjmsFactory;
		try {
			classAQjmsFactory = Class.forName("oracle.jms.AQjmsFactory");
		} catch (ClassNotFoundException e) {
			throw new ClassNotFoundException("aqapi.jar not in classpath");
		}

		Object result = null;
		if (object instanceof Reference) {
			Reference reference = (Reference) object;

			Class<?> class1 = Class.forName(reference.getClassName());
			if (javax.jms.ConnectionFactory.class.isAssignableFrom(class1)) {
				// For meaning of compliant refer to https://docs.oracle.com/cd/B13789_01/server.101/b10785/jm_create.htm
				RefAddr refAddr = reference.get("compliant");
				Boolean compliant = refAddr != null ? new Boolean((String) refAddr.getContent()) : Boolean.TRUE;

				refAddr = reference.get("dataSource");
				if (refAddr != null) {
					String ds = (String) refAddr.getContent();

					InitialContext initialContext = new InitialContext();
					try {
						DataSource dataSource = (DataSource) initialContext.lookup(ds);
						Method method = classAQjmsFactory.getMethod("getConnectionFactory", DataSource.class, Boolean.TYPE);
						result = method.invoke(null, dataSource, compliant);
					} finally {
						initialContext.close();
					}
				} else {
					refAddr = reference.get("jdbcURL");
					if (refAddr != null) {
						String url = (String) refAddr.getContent();

						Method method = classAQjmsFactory.getMethod("getConnectionFactory", String.class, Properties.class, Boolean.TYPE);
						Properties info = new Properties();
						refAddr = reference.get("user");
						if (refAddr != null) {
							info.put("user", refAddr.getContent());
							refAddr = reference.get("password");
							if (refAddr != null) {
								info.put("password", refAddr.getContent());
							}
						}
						result = method.invoke(null, url, info, compliant);
					}
				}
			}
		} else if (environment != null) {
			String url = (String) environment.get("jdbcURL");
			if (url != null && !url.isEmpty()) {
				Method method = classAQjmsFactory.getMethod("getConnectionFactory", String.class, Properties.class, Boolean.TYPE);
				String compliantStr = (String) environment.get("compliant");
				Boolean compliant = compliantStr != null ? new Boolean(compliantStr) : Boolean.TRUE;
				Properties info = new Properties();
				info.putAll(environment);
				result = method.invoke(null, url, info, compliant);
			}
		}
		return result;
	}

}
