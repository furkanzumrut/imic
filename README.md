IMIC project
============
imic is an application that allows to scale and optimize before serving images.

For WebLogic users, it might need to have `weblogic.xml` in _WEB-INF/_ directory to override libraries come with WebLogic.

Example WEB-INF/weblogic.xml

```
<?xml version="1.0" encoding="UTF-8"?>
<weblogic-web-app xmlns:wls="http://xmlns.oracle.com/weblogic/weblogic-web-app"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://java.sun.com/xml/ns/javaee 
        http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd 
        http://xmlns.oracle.com/weblogic/weblogic-web-app 
        http://xmlns.oracle.com/weblogic/weblogic-web-app/1.7/weblogic-web-app.xsd">
	<container-descriptor>
		<prefer-application-packages>
			<package-name>org.joda.*</package-name>
			<package-name>org.slf4j.*</package-name>
            <package-name>org.apache.commons.*</package-name>
            <package-name>net.sf.ehcache.*</package-name>
		</prefer-application-packages>
	</container-descriptor>
</weblogic-web-app>
```


