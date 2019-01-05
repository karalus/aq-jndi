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
		Class<?> classAQjmsFactory = Class.forName("oracle.jms.AQjmsFactory");

		Object result = null;
		if (object instanceof Reference) {
			Reference reference = (Reference) object;

			Class<?> class1 = Class.forName(reference.getClassName());
			if (ConnectionFactory.class.isAssignableFrom(class1)) {
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
						result = method.invoke(null, url, new Properties(), compliant);
					}
				}
			}
		} else {
			String url = (String) environment.get("jdbcURL");
			if (url != null && !url.isEmpty()) {
				String compliantStr = (String) environment.get("compliant");
				Boolean compliant = compliantStr != null ? new Boolean(compliantStr) : Boolean.TRUE;
				Method method = classAQjmsFactory.getMethod("getConnectionFactory", String.class, Properties.class, Boolean.TYPE);
				result = method.invoke(null, url, new Properties(), compliant);
			}
		}
		return result;
	}

}
