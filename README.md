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
1. Download the required library and add it to your project according to your OpenCms version from [here]
(https://github.com/tpinkowski/junitDriva/tree/master/lib-repo/net/componio/opencms.junit.base/ "lib-repo")<br/>
**Note: lib-repo is a Maven Repository and can also be added as a dependency into you project**
2. Download one of the supported [OpenCms Versions] (http://www.opencms.org/en/download/archive.html)
3. Unzip the downloaded package to a folder of your choice<br/>
**Note: This folder is now the relevant parent-folder for the OpenCms test environment**
4. Keep only following subfolders in the **parent-folder** (setup, WEB-INF, update).<br/>
**Only these folders are relevant in this case. **
5. Copy the [initial-scripts] (https://github.com/tpinkowski/junitDriva/tree/master/initial_scripts) folder from junitDriva to a folder  of your choise
6. Copy the [test.properties] (https://github.com/tpinkowski/junitDriva/blob/master/test.properties) to your project folder
7. Edit the properties in [test.properties] (https://github.com/tpinkowski/junitDriva/blob/master/test.properties) like this<br/>

    db.product=mysql
    test.data.path=[parent-folder]
    test.webapp.path=[parent-folder]
    test.build.folder=[folder of your choise]
    test.config.path=[parent-folder]/WEB-INF/config
    test.cmsshell.scripts.path=[initial-scripts-folder]

Other repositories
_______
* [nbDriva] (https://github.com/componio/nbDriva)
* [shellDriva] (https://github.com/tpinkowski/shellDriva)
