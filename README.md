# aq-jndi
JNDI provider für Oracle AQ

### Problem ###

When using Oracle AQ inside a standard JEE environment you need to lookup a `javax.jms.ConnectionFactory` instance via JNDI but the Java client for Oracle AQ is not providing a JNDI compliant factory class. 

### Solution ###

This is a simple wrapper class that provides for JNDI lookup of an AQ ConnectionFactory.
This is done using Java reflection mechanism in order to be independent of proprietary Oracle JARs.

### Usage within Tomcat ###

Add to Tomcat's `server.xml` a new resource element.

Using direct JDBC access to Oracle AQ:

```XML
	 <Resource auth="Container" 
	    name="jms/AQConnectionFactory" 
	    type="oracle.jms.AQjmsConnectionFactory" 
	    description="JMS Connection Factory" 
	    factory="com.artofarc.eai.aq.AQObjectFactory"
	    jdbcURL="jdbc:oracle:thin:user/passwd@localhost:1521:xe"/> 
```

You can also use it via DataSource:

```XML
	 <Resource auth="Container" 
	    name="jms/AQConnectionFactory" 
	    type="oracle.jms.AQjmsConnectionFactory" 
	    description="JMS Connection Factory" 
	    factory="com.artofarc.eai.aq.AQObjectFactory"
	    dataSource="java:/jdbc/OracleDB-With-AQ"/> 
```

Important: You need to configure the DataSource via Oracle's UCP - the internal Tomcat JDBC Connection Pool does not work.

### Release notes ###

1.0 Initial release

1.1 Support for JBoss EAP 7.1, make compliant=true the default, allow user/password to specified separately when using jdbcURL
