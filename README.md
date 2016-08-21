junitDriva
=======
A helper utility, which provides a simple way to test OpenCms Modules.
Therefore the CmsShell is utilized to setup an OpenCms test environment and to have access to a CmsObject and other functionalities of the OpenCms API.

How to
=======
### Requirements ###
* Installed OpenCms Version (currently supported: 9.0.x or 9.5.x)
* MySQL

### Installation ###
* Add the [opencms-core](https://mvnrepository.com/artifact/org.opencms/opencms-core), [opencms-setup](https://mvnrepository.com/artifact/org.opencms/opencms-setup) and other relevant libraries to your project(classpath)
* Download the required junitDriva library from [lib-repo]
(https://github.com/tpinkowski/junitDriva/tree/master/lib-repo/net/componio/opencms.junit.base/ "lib-repo") and add it to your project according to your OpenCms version<br/>
**Note: lib-repo is a Maven Repository and can also be added as a dependency into your project**
* Download one of the supported [OpenCms Versions] (http://www.opencms.org/en/download/archive.html)
* Unzip the downloaded package to a folder of your choice<br/>
**Note: This folder is now the relevant parent-folder for the OpenCms test environment**<br/>
* Keep only following subfolders in the **parent-folder** (setup, WEB-INF, update).<br/>
**Only these folders are relevant in this case.**<br/>
* Copy the [initial_scripts] (https://github.com/tpinkowski/junitDriva/tree/master/initial_scripts) folder from junitDriva to a folder  of your choice
* Copy the [test.properties] (https://github.com/tpinkowski/junitDriva/blob/master/test.properties) to your project folder
* Edit the properties in [test.properties] (https://github.com/tpinkowski/junitDriva/blob/master/test.properties) like this<br/>
```PROPERTIES
db.product=mysql
test.data.path=[parent-folder]
test.webapp.path=[parent-folder]
test.build.folder=[folder of your choice]
test.config.path=[parent-folder]/WEB-INF/config
test.cmsshell.scripts.path=[initial-scripts-folder]
```
* Edit/Add the relevant properties in **[parent-folder]/WEB-INF/config/opencms.properties**
```PROPERTIES
# Database setup (used only in tests) 
#################################################################################
db.pool.setup.dbName=[test-schema-name]
db.pool.setup.jdbcDriver=org.gjt.mm.mysql.Driver
db.pool.setup.jdbcUrl=jdbc:mysql://localhost:3306/
db.pool.setup.jdbcUrl.params=?characterEncoding\=UTF-8
db.pool.setup.user=[user]
db.pool.setup.password=[password]
#
# Configuration of the default database pool
#################################################################################
# name of the database (used only in tests)
db.pool.default.dbName=[test-schema-name]
# name of the JDBC driver
db.pool.default.jdbcDriver=org.gjt.mm.mysql.Driver
# URL of the JDBC driver
db.pool.default.jdbcUrl=jdbc:mysql://localhost:3306/[test-schema-name]
# user name to connect to the database
db.pool.default.user=[user]
# password to connect to the database
db.pool.default.password=[password]
```
* Create a JUNIT class in your project. e.g. like this <br/>
```Java
import java.io.IOException;
import net.componio.opencms.junit.base.OpenCmsTestBase;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencms.file.CmsObject;
import org.opencms.main.CmsException;
 
public class ModuleTest {
    
    public static OpenCmsTestBase cmsTestBase;
 
    @BeforeClass
    public static void initialize() throws IOException {
        cmsTestBase = new OpenCmsTestBase(true);
        OpenCmsTestBase.setupOpenCms("", "", true);
    }
 
    @Test
    public void testSomething() throws CmsException {
        CmsObject cms = cmsTestBase.getCmsObject();
        Assert.assertNotEquals("Do Some Stuff With the CmsObject","Do Some Stuff With the CmsObject", "Hello World");
    }
    
    @AfterClass
    public static void removeOpenCms() throws IOException {
        OpenCmsTestBase.removeOpenCms();
    }
}
```

Other repositories
=======
* [nbDriva] (https://github.com/componio/nbDriva)
* [shellDriva] (https://github.com/tpinkowski/shellDriva)
