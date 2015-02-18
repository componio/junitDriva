/**
 *
 * @author Thomas
 */
/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org 
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package net.componio.opencms.junit.base;

import org.opencms.configuration.CmsParameterConfiguration;
import org.opencms.db.CmsDbPool;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsResource;
import org.opencms.importexport.CmsImportParameters;
import org.opencms.main.CmsException;
import org.opencms.main.CmsShell;
import org.opencms.main.CmsSystemInfo;
import org.opencms.main.OpenCms;
import org.opencms.report.CmsShellReport;
import org.opencms.setup.CmsSetupDb;
import org.opencms.util.CmsFileUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;
import org.opencms.main.CmsLog;

/**
 * Extends the JUnit standard with methods to handle an OpenCms database test
 * instance.<p>
 *
 * The required configuration files are located in the
 * <code>${test.data.path}/WEB-INF</code> folder structure.<p>
 *
 * To run this test you might have to change the database connection values in
 * the provided
 * <code>${test.data.path}/WEB-INF/config/opencms.properties</code> file.<p>
 *
 * @since 6.0.0
 */
public class OpenCmsTestBase {

    /**
     * Class to bundle the connection information.
     */
    protected static class ConnectionData {

        /**
         * The name of the database.
         */
        public String m_dbName;
        /**
         * The database driver.
         */
        public String m_jdbcDriver;
        /**
         * The database url.
         */
        public String m_jdbcUrl;
        /**
         * Additional database parameters.
         */
        public String m_jdbcUrlParams;
        /**
         * The name of the user.
         */
        public String m_userName;
        /**
         * The password of the user.
         */
        public String m_userPassword;
    }
    public static final String DB_MYSQL = "mysql";
    /**
     * Key for tests on Oracle database.
     */
    public static final String DB_ORACLE = "oracle";
    /**
     * The OpenCms/database configuration.
     */
    public static CmsParameterConfiguration m_configuration;
    /**
     * Name of the default tablespace (oracle only).
     */
    public static String m_defaultTablespace;
    /**
     * Name of the index tablespace (oracle only).
     */
    public static String m_indexTablespace;
    /**
     * The internal storages.
     */
    public static HashMap<String, OpenCmsTestResourceStorage> m_resourceStorages;
    /**
     * Name of the temporary tablespace (oracle only).
     */
    public static String m_tempTablespace;
    /**
     * Additional connection data.
     */
    protected static ConnectionData m_additionalConnection;
    /**
     * The user connection data.
     */
    protected static ConnectionData m_defaultConnection;
    /**
     * The setup connection data.
     */
    protected static ConnectionData m_setupConnection;
    /**
     * The cached list of OpenCms class names.
     */
    private static List<String> classNameList;
    /**
     * DB product used for the tests.
     */
    private static String m_dbProduct = DB_MYSQL;
    /**
     * The path to the default setup data files.
     */
    private static String m_setupDataPath;
    /**
     * The initialized OpenCms shell instance.
     */
    private static CmsShell m_shell;
    /**
     * The list of paths to the additional test data files.
     */
    private static List<String> m_testDataPath;
    /**
     * The current resource storage.
     */
    public OpenCmsTestResourceStorage m_currentResourceStrorage;

    /**
     * JUnit constructor.<p>
     *
     * @param arg0 JUnit parameters
     * @param initialize indicates if the configuration will be initialized
     */
    public OpenCmsTestBase(boolean initialize) throws IOException {
        CmsLog.INIT = LogFactory.getLog("org.opencms.init");
        if (initialize) {
            OpenCmsTestLogAppender.setBreakOnError(false);
            if (m_resourceStorages == null) {
                m_resourceStorages = new HashMap<String, OpenCmsTestResourceStorage>();
            }

            
            // initialize configuration
            initConfiguration();

            // set "OpenCmsLog" system property to enable the logger
            OpenCmsTestLogAppender.setBreakOnError(true);
        }
    }

    /**
     * Returns the currently used database/configuration.<p>
     *
     * @return he currently used database/configuration
     */
    public static String getDbProduct() {

        return m_dbProduct;
    }

    /**
     * Returns the path to a file in the test data configuration, or
     * <code>null</code> if the given file can not be found.<p>
     *
     * This methods searches the given file in all configured test data paths.
     * It returns the file found first.<p>
     *
     * @param filename the file name to look up
     * @return the path to a file in the test data configuration
     */
    public static String getTestDataPathTmp(String filename) {

        for (int i = 0; i < m_testDataPath.size(); i++) {

            String path = m_testDataPath.get(i);
            File file = new File(path + filename);
            if (file.exists()) {
                if (file.isDirectory()) {
                    return CmsFileUtil.normalizePath(file.getAbsolutePath() + File.separator);
                } else {
                    return CmsFileUtil.normalizePath(file.getAbsolutePath());
                }
            }
        }

        return null;
    }

    /**
     * Does a database import from the given RFS folder to the given VFS
     * folder.<p>
     *
     * @param importFolder the RFS folder to import from
     * @param targetFolder the VFS folder to import into
     */
    public static void importData(String targetFolder) {

        // turn off exceptions after error logging during setup (won't work otherwise)
        OpenCmsTestLogAppender.setBreakOnError(false);
        // output a message 
        System.out.println("\n\n\n----- Starting test case: Importing OpenCms VFS data -----");

        // kill any old shell that might have remained from a previous test 
        if (m_shell != null) {
            try {
                m_shell.exit();
                m_shell = null;
            } catch (Throwable t) {
                // ignore
            }
        }

        // create a shell instance
        m_shell = new CmsShell(getTestDataPathTmp("WEB-INF" + File.separator), null, null, "${user}@${project}>", null);

        // open the test script 
        File script;
        FileInputStream stream = null;
        CmsObject cms = null;

        try {
            // start the shell with the base script
            script = new File(OpenCmsTestProperties.getInstance().getTestCmsShellScriptsPath() + "/" + "script_import.txt");
            stream = new FileInputStream(script);
            m_shell.start(stream);

            // log in the Admin user and switch to the setup project
            cms = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserGuest());
            cms.loginUser("Admin", "admin");
            cms.getRequestContext().setCurrentProject(cms.readProject("tempFileProject"));

            importResources(cms, targetFolder);


            // publish the current project by script
            script = new File(OpenCmsTestProperties.getInstance().getTestCmsShellScriptsPath() + "/" + "script_import_publish.txt");
            stream = new FileInputStream(script);
            m_shell.start(stream);
            OpenCms.getPublishManager().waitWhileRunning();

            // switch to the "Offline" project
            cms.getRequestContext().setCurrentProject(cms.readProject("Offline"));
            cms.getRequestContext().setSiteRoot("/sites/default/");

            // output a message 
            System.out.println("----- Starting test cases -----");
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            TestCase.fail("Unable to setup OpenCms\n" + CmsException.getStackTraceAsString(t));
        }
        // turn on exceptions after error logging
        OpenCmsTestLogAppender.setBreakOnError(true);
    }

    /**
     * Initializes the path to the test data configuration files using the
     * default path.<p>
     */
    public static synchronized void initTestDataPathTmp() throws IOException {

        if (m_testDataPath == null) {
            m_testDataPath = new ArrayList<String>();

            try {
                OpenCmsTestProperties.getInstance();
            } catch (RuntimeException rte) {
                OpenCmsTestProperties.initialize(OpenCmsJunitProjectConstants.getTestPropertiesPath());
            }
            // copy the data path to a temporary directory
            copyDataPath();
            // set data path 
            addTestDataPathTmp(OpenCmsTestProperties.getInstance().getTestDataPathTmp());
        }
    }

    /**
     * Removes the initialized OpenCms database and all temporary files created
     * during the test run.<p>
     */
    public static void removeOpenCms() throws IOException {

        // ensure logging does not throw exceptions
        OpenCmsTestLogAppender.setBreakOnError(false);

        // output a message
        m_shell.printPrompt();
        System.out.println("----- Test cases finished -----");

        // exit the shell
        m_shell.exit();

        try {
            // sleep 0.5 seconds - sometimes other Threads need to finish before the next test case can start
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // ignore
        }

        // remove the database
        removeDatabase();
        deleteDataPathTmp();

        String path;

        // remove potentially created "classes, "lib", "backup" etc. folder
        path = getTestDataPathTmp("WEB-INF/classes/");
        if (path != null) {
            CmsFileUtil.purgeDirectory(new File(path));
        }
        path = getTestDataPathTmp("WEB-INF/logs/publish");
        if (path != null) {
            CmsFileUtil.purgeDirectory(new File(path));
        }
        path = getTestDataPathTmp("WEB-INF/lib/");
        if (path != null) {
            CmsFileUtil.purgeDirectory(new File(path));
        }
        path = getTestDataPathTmp("WEB-INF/" + CmsSystemInfo.FOLDER_CONFIG_DEFAULT + "backup/");
        if (path != null) {
            CmsFileUtil.purgeDirectory(new File(path));
        }
        path = getTestDataPathTmp("WEB-INF/index/");
        if ((path != null) && !m_configuration.containsKey("test.keep.searchIndex")) {
            CmsFileUtil.purgeDirectory(new File(path));
        }
        path = getTestDataPathTmp("export/");
        if (path != null) {
            CmsFileUtil.purgeDirectory(new File(path));
        }
        //delete the rest of the files that could be generated or created during the tests
        File[] deleteRest = new File(OpenCmsTestProperties.getInstance().getTestDataPath()).listFiles();
        for (File rest : deleteRest) {
            if (!CmsFileUtil.normalizePath(rest.getAbsolutePath() + File.separator).equals(getTestDataPathTmp("WEB-INF"))) {
                rest.delete();
            }
        }
    }

    /**
     * Restarts the OpenCms shell.<p>
     */
    public static void restartOpenCms() {

        // turn off exceptions after error logging during setup (won't work otherwise)
        OpenCmsTestLogAppender.setBreakOnError(false);
        // output a message 
        System.out.println("\n\n\n----- Restarting OpenCms -----");

        // kill any old shell that might have remained from a previous test 
        if (m_shell != null) {
            try {
                m_shell.exit();
                m_shell = null;
            } catch (Throwable t) {
                // ignore
            }
        }

        // create a shell instance
        m_shell = new CmsShell(getTestDataPathTmp("WEB-INF" + File.separator), null, null, "${user}@${project}>", null);

        // turn on exceptions after error logging
        OpenCmsTestLogAppender.setBreakOnError(true);
    }

    /**
     * Sets up a complete OpenCms instance with configuration from the
     * config-ori folder, creating the usual projects, and importing a default
     * database.<p>
     *
     * @param importFolder the folder to import in the "real" FS
     * @param targetFolder the target folder of the import in the VFS
     * @return an initialized OpenCms context with "Admin" user in the "Offline"
     * project with the site root set to "/"
     */
    public static CmsObject setupOpenCms(String targetFolder) throws IOException {
        //return setupOpenCms(targetFolder, getTestDataPath("WEB-INF/config." + m_dbProduct + "/"), true);

        File configPath = new File(getTestDataPathTmp("WEB-INF/config"));
        return setupOpenCms(targetFolder, CmsFileUtil.normalizePath(configPath.getAbsolutePath()) + File.separator, true);
    }

    /**
     * Sets up a complete OpenCms instance with configuration from the
     * config-ori folder, creating the usual projects, and importing a default
     * database.<p>
     *
     * @param importFolder the folder to import in the "real" FS
     * @param targetFolder the target folder of the import in the VFS
     * @param publish flag to signalize if the publish script should be called
     * @return an initialized OpenCms context with "Admin" user in the "Offline"
     * project with the site root set to "/"
     */
    public static CmsObject setupOpenCms(String targetFolder, boolean publish) throws IOException {
        //return setupOpenCms(targetFolder, getTestDataPath("WEB-INF/config." + m_dbProduct + "/"), publish);
        File configPath = new File(getTestDataPathTmp("WEB-INF/config"));
        return setupOpenCms(targetFolder, CmsFileUtil.normalizePath(configPath.getAbsolutePath()) + File.separator, publish);
    }

    /**
     * Sets up a complete OpenCms instance, creating the usual projects, and
     * importing a default database.<p>
     *
     * @param importFolder the folder to import in the "real" FS
     * @param targetFolder the target folder of the import in the VFS
     * @param configFolder the folder to copy the configuration files
     * @param publish publish only if set
     *
     * @return an initialized OpenCms context with "Admin" user in the "Offline"
     * project with the site root set to "/"
     */
    public static CmsObject setupOpenCms(String targetFolder, String configFolder, boolean publish) throws IOException {
        return setupOpenCms(targetFolder, configFolder, null, publish);
    }

    /**
     * Sets up a complete OpenCms instance, creating the usual projects, and
     * importing a default database.<p>
     *
     * @param importFolder the folder to import in the "real" FS
     * @param targetFolder the target folder of the import in the VFS
     * @param configFolder the folder to copy the standard configuration files
     * from
     * @param specialConfigFolder the folder that contains the special
     * configuration fiiles for this setup
     *
     * @param publish publish only if set
     *
     * @return an initialized OpenCms context with "Admin" user in the "Offline"
     * project with the site root set to "/"
     */
    public static CmsObject setupOpenCms(
            String targetFolder,
            String configFolder,
            String specialConfigFolder,
            boolean publish) throws IOException {

        // intialize a new resource storage
        m_resourceStorages = new HashMap<String, OpenCmsTestResourceStorage>();

        // turn off exceptions after error logging during setup (won't work otherwise)
        OpenCmsTestLogAppender.setBreakOnError(false);
        // output a message 
        System.out.println("\n\n\n----- Starting test case: Importing OpenCms VFS data -----");

        // kill any old shell that might have remained from a previous test 
        if (m_shell != null) {
            try {
                m_shell.exit();
                m_shell = null;
            } catch (Throwable t) {
                // ignore
            }
        }
        // create a new database first
        setupDatabase();


        // create a shell instance
        m_shell = new CmsShell(getTestDataPathTmp("WEB-INF" + File.separator), null, null, "${user}@${project}>", null);

        // open the test script 
        File script;
        FileInputStream stream = null;
        CmsObject cms = null;

        try {
            // start the shell with the base script
            script = new File(OpenCmsTestProperties.getInstance().getTestCmsShellScriptsPath() + "/" + "script_base.txt");
            stream = new FileInputStream(script);
            m_shell.start(stream);

            // add the default folders by script
            script = new File(OpenCmsTestProperties.getInstance().getTestCmsShellScriptsPath() + "/" + "script_default_folders.txt");
            stream = new FileInputStream(script);
            m_shell.start(stream);

            // log in the Admin user and switch to the setup project
            cms = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserGuest());
            cms.loginUser("Admin", "admin");
            cms.getRequestContext().setCurrentProject(cms.readProject("_setupProject"));

            //imports the resources
            importResources(cms, targetFolder);

            // create the default projects by script
            script = new File(OpenCmsTestProperties.getInstance().getTestCmsShellScriptsPath() + "/" + "script_default_projects.txt");
            stream = new FileInputStream(script);
            m_shell.start(stream);

            if (publish) {
                // publish the current project by script
                script = new File(OpenCmsTestProperties.getInstance().getTestCmsShellScriptsPath() + "/" + "script_publish.txt");
                stream = new FileInputStream(script);
                m_shell.start(stream);
                OpenCms.getPublishManager().waitWhileRunning();
            } else {
                cms.unlockProject(cms.readProject("_setupProject").getUuid());
            }

            // switch to the "Offline" project
            cms.getRequestContext().setCurrentProject(cms.readProject("Offline"));
            cms.getRequestContext().setSiteRoot("/sites/default/");

            // output a message 
            System.out.println("----- Starting test cases -----");
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            TestCase.fail("Unable to setup OpenCms\n" + CmsException.getStackTraceAsString(t));
        }
        // turn on exceptions after error logging
        OpenCmsTestLogAppender.setBreakOnError(true);
        // return the initialized cms context Object
        return cms;
    }

    /**
     * Adds an additional path to the list of test data configuration files.<p>
     *
     * @param dataPath the path to add
     */
    protected static synchronized void addTestDataPathTmp(String dataPath) {

        // check if the db data folder is available
        File testDataFolder = new File(dataPath);
        if (!testDataFolder.exists()) {
            TestCase.fail("DB setup data not available at " + testDataFolder.getAbsolutePath());
        }
        String path = CmsFileUtil.normalizePath(testDataFolder.getAbsolutePath() + File.separator);
        if (!m_testDataPath.contains(path)) {
            m_testDataPath.add(path);
        }
    }

    /**
     * Check the setup DB for errors that might have occurred.<p>
     *
     * @param setupDb the setup DB object to check
     */
    protected static void checkErrors(CmsSetupDb setupDb) {

        if (!setupDb.noErrors()) {
            List<String> errors = setupDb.getErrors();
            for (Iterator<String> i = errors.iterator(); i.hasNext();) {
                String error = i.next();
                System.out.println(error);
            }
            TestCase.fail(setupDb.getErrors().get(0));
        }
    }

    /**
     * Returns an initialized replacer map.<p>
     *
     * @param connectionData the connection data to derive the replacer
     * information
     *
     * @return an initialized replacer map
     */
    protected static Map<String, String> getReplacer(ConnectionData connectionData) {

        Map<String, String> replacer = new HashMap<String, String>();
        replacer.put("${database}", connectionData.m_dbName);
        replacer.put("${user}", connectionData.m_userName);
        replacer.put("${password}", connectionData.m_userPassword);
        replacer.put("${defaultTablespace}", m_defaultTablespace);
        replacer.put("${indexTablespace}", m_indexTablespace);
        replacer.put("${temporaryTablespace}", m_tempTablespace);

        return replacer;
    }

    /**
     * Returns the path to the data files used by the setup wizard.<p>
     *
     * Whenever possible use this path to ensure that the files used for testing
     * are actually the same as for the setup.<p>
     *
     * @return the path to the data files used by the setup wizard
     */
    protected static synchronized String getSetupDataPath() {
        if (m_setupDataPath == null) {
            // check if the db setup files are available
            File setupDataFolder = new File(OpenCmsTestProperties.getInstance().getTestWebappPath());
            if (!setupDataFolder.exists()) {
                TestCase.fail("DB setup data not available at " + setupDataFolder.getAbsolutePath());
            }
            m_setupDataPath = setupDataFolder.getAbsolutePath() + File.separator;
        }
        // return the path name
        return m_setupDataPath;
    }

    /**
     * Returns an initialized DB setup object.<p>
     *
     * @param connection the connection data
     *
     * @return the initialized setup DB object
     */
    protected static CmsSetupDb getSetupDb(ConnectionData connection) {

        // create setup DB instance
        CmsSetupDb setupDb = new CmsSetupDb(getSetupDataPath());

        // connect to the DB
        setupDb.setConnection(
                connection.m_jdbcDriver,
                connection.m_jdbcUrl,
                connection.m_jdbcUrlParams,
                connection.m_userName,
                connection.m_userPassword);

        // check for errors 
        if (!DB_ORACLE.equals(m_dbProduct)) {
            checkErrors(setupDb);
        }

        return setupDb;
    }

    /**
     * Imports a resource into the Cms.<p>
     *
     * @param cms an initialized CmsObject
     * @param importFile the name (absolute Path) of the import resource (zip or
     * folder)
     * @param targetPath the name (absolute Path) of the target folder in the
     * VFS
     * @throws CmsException if something goes wrong
     */
    protected static void importResources(CmsObject cms, String targetPath) throws CmsException, IOException {

        String impFoldersProp = OpenCmsTestProperties.getInstance().getTestImportFolders();
        String impModulesProp = OpenCmsTestProperties.getInstance().getTestImportModulesInOrder();

        String[] importFolders = (impFoldersProp != null && !impFoldersProp.isEmpty()) ? impFoldersProp.split(",") : null;
        String[] importModules = (impModulesProp != null && !impModulesProp.isEmpty()) ? impModulesProp.split(",") : null;

        if (importModules != null) {
            for (String module : importModules) {
                File file = new File(module);
                if (file.isFile()) {
                    OpenCms.getImportExportManager().importData(
                            cms,
                            new CmsShellReport(cms.getRequestContext().getLocale()),
                            new CmsImportParameters(
                            file.getAbsolutePath(),
                            targetPath,
                            true));
                }
            }
        } else if (importFolders != null) {
            for (String folder : importFolders) {
                if (!folder.isEmpty() && (new File(folder)).isDirectory()) {
                    File[] files = new File(folder.trim()).listFiles();
                    for (File file : files) {
                        if (file.isFile()) {
                            OpenCms.getImportExportManager().importData(
                                    cms,
                                    new CmsShellReport(cms.getRequestContext().getLocale()),
                                    new CmsImportParameters(
                                    file.getAbsolutePath(),
                                    targetPath,
                                    true));
                        }
                    }
                }
            }
        }
    }

    /**
     * Imports a resource from the RFS test directories to the VFS.<p>
     *
     * The imported resource will be automatically unlocked.<p>
     *
     * @param cms the current users OpenCms context
     * @param rfsPath the RTF path of the resource to import, must be a path
     * accessibly by the current class loader
     * @param vfsPath the VFS path for the imported resource
     * @param type the type for the imported resource
     * @param properties the properties for the imported resource
     * @return the imported resource
     *
     * @throws Exception if the import fails
     */
    protected static CmsResource importTestResource(
            CmsObject cms,
            String rfsPath,
            String vfsPath,
            int type,
            List<CmsProperty> properties) throws Exception {

        byte[] content = CmsFileUtil.readFile(rfsPath);
        CmsResource result = cms.createResource(vfsPath, type, content, properties);
        cms.unlockResource(vfsPath);
        return result;
    }

    /**
     * Removes the OpenCms database test instance.<p>
     */
    protected static void removeDatabase() {

        if (m_defaultConnection != null) {
            removeDatabase(m_setupConnection, m_defaultConnection, false);
        }
        if (m_additionalConnection != null) {
            removeDatabase(m_setupConnection, m_additionalConnection, false);
        }
    }

    /**
     * Removes the OpenCms database test instance.<p>
     *
     * @param setupConnection the setup connection
     * @param defaultConnection the default connection
     * @param handleErrors flag to indicate if errors should be handled/checked
     */
    protected static void removeDatabase(
            ConnectionData setupConnection,
            ConnectionData defaultConnection,
            boolean handleErrors) {

        CmsSetupDb setupDb = null;
        boolean noErrors = true;

        try {
            setupDb = getSetupDb(defaultConnection);
            setupDb.dropTables(m_dbProduct, getReplacer(defaultConnection), handleErrors);
            noErrors = setupDb.noErrors();
        } catch (Exception e) {
            noErrors = false;
        } finally {
            if (setupDb != null) {
                setupDb.closeConnection();
            }
        }

        if (!handleErrors || noErrors) {
            try {
                setupDb = getSetupDb(setupConnection);
                setupDb.dropDatabase(m_dbProduct, getReplacer(defaultConnection), handleErrors);
                setupDb.closeConnection();
            } catch (Exception e) {
                noErrors = false;
            } finally {
                if (setupDb != null) {
                    setupDb.closeConnection();
                }
            }
        }

        if (handleErrors) {
            checkErrors(setupDb);
        }
    }

    /**
     * makes a temporary test data directory for generated and modified files
     *
     * @throws IOException
     */
    public static void copyDataPath() throws IOException {

        File testDataPathOri = new File(OpenCmsTestProperties.getInstance().getTestDataPath());
        File testDataPathTmp = new File(OpenCmsTestProperties.getInstance().getTestDataPathTmp());
        if (!testDataPathTmp.exists()) {
            testDataPathTmp.mkdir();
        }
        FileUtils.copyDirectory(testDataPathOri, testDataPathTmp);
    }

    /**
     * deletes the temporary directory
     */
    public static void deleteDataPathTmp() throws IOException {
        File testDataPathTmp = new File(OpenCmsTestProperties.getInstance().getTestDataPathTmp());
        if (testDataPathTmp.exists()) {
            FileUtils.deleteDirectory(testDataPathTmp);
        }
    }

    /**
     * Creates a new OpenCms test database including the tables.<p>
     *
     * Any existing instance of the test database is forcefully removed
     * first.<p>
     */
    protected static void setupDatabase() {

        if (m_defaultConnection != null) {
            setupDatabase(m_setupConnection, m_defaultConnection, true);
        }
        if (m_additionalConnection != null) {
            setupDatabase(m_setupConnection, m_additionalConnection, true);
        }
    }

    /**
     * Creates a new OpenCms test database including the tables.<p>
     *
     * @param setupConnection the setup connection
     * @param defaultConnection the default connection
     * @param handleErrors flag to indicate if errors should be handled/checked
     */
    protected static void setupDatabase(
            ConnectionData setupConnection,
            ConnectionData defaultConnection,
            boolean handleErrors) {

        CmsSetupDb setupDb = null;
        boolean noErrors = true;

        try {
            setupDb = getSetupDb(setupConnection);
            setupDb.createDatabase(m_dbProduct, getReplacer(defaultConnection), handleErrors);
            noErrors = setupDb.noErrors();
            setupDb.closeConnection();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            noErrors = false;
        } finally {
            if (setupDb != null) {
                setupDb.closeConnection();
            }
        }

        if (!handleErrors || noErrors) {
            try {
                setupDb = getSetupDb(defaultConnection);
                setupDb.createTables(m_dbProduct, getReplacer(defaultConnection), handleErrors);
                noErrors = setupDb.noErrors();
                setupDb.closeConnection();
            } catch (Exception e) {
                noErrors = false;
            } finally {
                if (setupDb != null) {
                    setupDb.closeConnection();
                }
            }
        }

        if (noErrors) {
            return;
        } else if (handleErrors) {
            removeDatabase(setupConnection, defaultConnection, false);
            setupDatabase(setupConnection, defaultConnection, false);
        } else {
            checkErrors(setupDb);
        }
    }

    /**
     * Creates a new storage object.<p>
     *
     * @param name the name of the storage
     */
    public static void createStorage(String name) {

        OpenCmsTestResourceStorage storage = new OpenCmsTestResourceStorage(name);
        m_resourceStorages.put(name, storage);
    }

    /**
     * Should return the additional connection name.<p>
     *
     * @return the name of the additional connection
     */
    public String getConnectionName() {

        return "additional";
    }

    /**
     * Switches the internal resource storage.<p>
     *
     * @param name the name of the storage
     * @throws CmsException if the storage was not found
     */
    public void switchStorage(String name) throws CmsException {

        OpenCmsTestResourceStorage storage = m_resourceStorages.get(name);
        if (storage != null) {
            m_currentResourceStrorage = storage;
        } else {
            throw new CmsException(Messages.get().container(Messages.ERR_RESOURCE_STORAGE_NOT_FOUND_0));
        }
    }

    /**
     * Writes a message to the current output stream.<p>
     *
     * @param message the message to write
     */
    public static void echo(String message) {

        try {
            System.out.println();
            m_shell.printPrompt();
            System.out.println(message);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Returns an initialized CmsObject with admin user permissions, running in
     * the "/sites/default" site root.<p>
     *
     * @return an initialized CmsObject with admin user permissions
     * @throws CmsException in case of OpenCms access errors
     */
    public CmsObject getCmsObject() throws CmsException {

        // log in the Admin user and switch to the setup project
        CmsObject cms = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserGuest());
        cms.loginUser("Admin", "admin");
        // switch to the "Offline" project
        cms.getRequestContext().setCurrentProject(cms.readProject("Offline"));
        cms.getRequestContext().setSiteRoot("/sites/default/");

        // init the storage
        createStorage(OpenCmsTestResourceStorage.DEFAULT_STORAGE);
        switchStorage(OpenCmsTestResourceStorage.DEFAULT_STORAGE);

        // return the initialized cms context Object
        return cms;
    }

    /**
     * Imports a module (zipfile) from the default module directory, creating a
     * temporary project for this.<p>
     *
     * @param importFile the name of the import module located in the default
     * module directory
     *
     * @throws Exception if something goes wrong
     *
     * @see
     * org.opencms.importexport.CmsImportExportManager#importData(CmsObject,
     * org.opencms.report.I_CmsReport, CmsImportParameters)
     */
    protected void importModuleFromDefault(String importFile) throws Exception {

        String exportPath = OpenCms.getSystemInfo().getPackagesRfsPath();
        String fileName = OpenCms.getSystemInfo().getAbsoluteRfsPathRelativeToWebInf(
                exportPath + CmsSystemInfo.FOLDER_MODULES + importFile);

        CmsImportParameters params = new CmsImportParameters(fileName, "/", true);

        OpenCms.getImportExportManager().importData(
                getCmsObject(),
                new CmsShellReport(getCmsObject().getRequestContext().getLocale()),
                params);
    }

    /**
     * Restarts the cms.<p>
     */
    protected void restart() {

        OpenCmsTestLogAppender.setBreakOnError(false);

        // output a message 
        System.out.println("\n\n\n----- Restarting shell -----");

        m_shell.exit();

        m_shell = new CmsShell(getTestDataPathTmp("WEB-INF" + File.separator), null, null, "${user}@${project}>", null);

        OpenCmsTestLogAppender.setBreakOnError(true);
    }

    /**
     * Initializes the OpenCms/database configuration by reading the appropriate
     * values from opencms.properties.<p>
     */
    private void initConfiguration() throws IOException {

        if (m_configuration == null) {
            initTestDataPathTmp();
            m_configuration = OpenCmsTestProperties.getInstance().getConfiguration();
            m_dbProduct = OpenCmsTestProperties.getInstance().getDbProduct();
            int index = 0;
            boolean cont;
            do {
                cont = false;
                if (m_configuration.containsKey(OpenCmsTestProperties.PROP_TEST_DATA_PATH + "." + index)) {
                    addTestDataPathTmp(m_configuration.get(OpenCmsTestProperties.PROP_TEST_DATA_PATH + "." + index));
                    cont = true;
                    index++;
                }
            } while (cont);
            String propertyPath = "";
            try {
                //propertyFile = getTestDataPathTmp("WEB-INF/config." + m_dbProduct + "/opencms.properties");
                propertyPath = getTestDataPathTmp("WEB-INF/config" + File.separator + "opencms.properties");
                File propertyFile = new File(propertyPath);
                propertyPath = CmsFileUtil.normalizePath(propertyFile.getAbsolutePath());
                m_configuration = new CmsParameterConfiguration(propertyPath);
            } catch (Exception e) {
                TestCase.fail("Error while reading configuration from '" + propertyPath + "'\n" + e.toString());
                return;
            }

            String key = "setup";
            m_setupConnection = new ConnectionData();
            m_setupConnection.m_dbName = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL + "." + key + "." + "dbName");
            m_setupConnection.m_jdbcUrl = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL + "." + key + "." + "jdbcUrl");
            m_setupConnection.m_userName = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL + "." + key + "." + "user");
            m_setupConnection.m_userPassword = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                    + "."
                    + key
                    + "."
                    + "password");
            m_setupConnection.m_jdbcDriver = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                    + "."
                    + key
                    + "."
                    + CmsDbPool.KEY_JDBC_DRIVER);
            m_setupConnection.m_jdbcUrl = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                    + "."
                    + key
                    + "."
                    + CmsDbPool.KEY_JDBC_URL);
            m_setupConnection.m_jdbcUrlParams = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                    + "."
                    + key
                    + "."
                    + CmsDbPool.KEY_JDBC_URL_PARAMS);

            key = "default";
            m_defaultConnection = new ConnectionData();
            m_defaultConnection.m_dbName = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL + "." + key + "." + "dbName");
            m_defaultConnection.m_userName = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                    + "."
                    + key
                    + "."
                    + CmsDbPool.KEY_USERNAME);
            m_defaultConnection.m_userPassword = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                    + "."
                    + key
                    + "."
                    + CmsDbPool.KEY_PASSWORD);
            m_defaultConnection.m_jdbcDriver = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                    + "."
                    + key
                    + "."
                    + CmsDbPool.KEY_JDBC_DRIVER);
            m_defaultConnection.m_jdbcUrl = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                    + "."
                    + key
                    + "."
                    + CmsDbPool.KEY_JDBC_URL);
            m_defaultConnection.m_jdbcUrlParams = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                    + "."
                    + key
                    + "."
                    + CmsDbPool.KEY_JDBC_URL_PARAMS);

            key = getConnectionName();
            if (m_configuration.get(CmsDbPool.KEY_DATABASE_POOL + "." + key + "." + "dbName") != null) {
                m_additionalConnection = new ConnectionData();
                m_additionalConnection.m_dbName = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                        + "."
                        + key
                        + "."
                        + "dbName");
                m_additionalConnection.m_userName = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                        + "."
                        + key
                        + "."
                        + CmsDbPool.KEY_USERNAME);
                m_additionalConnection.m_userPassword = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                        + "."
                        + key
                        + "."
                        + CmsDbPool.KEY_PASSWORD);
                m_additionalConnection.m_jdbcDriver = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                        + "."
                        + key
                        + "."
                        + CmsDbPool.KEY_JDBC_DRIVER);
                m_additionalConnection.m_jdbcUrl = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                        + "."
                        + key
                        + "."
                        + CmsDbPool.KEY_JDBC_URL);
                m_additionalConnection.m_jdbcUrlParams = m_configuration.get(CmsDbPool.KEY_DATABASE_POOL
                        + "."
                        + key
                        + "."
                        + CmsDbPool.KEY_JDBC_URL_PARAMS);
            }

            m_defaultTablespace = m_configuration.get("db.oracle.defaultTablespace");
            m_indexTablespace = m_configuration.get("db.oracle.indexTablespace");
            m_tempTablespace = m_configuration.get("db.oracle.temporaryTablespace");

            System.out.println("----- Starting tests on database "
                    + m_dbProduct
                    + " ("
                    + m_setupConnection.m_jdbcUrl
                    + ") "
                    + "-----");
        }
    }
}
