/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.componio.opencms.junit.base;

/**
 *
 * @author Thomas
 */
public class OpenCmsJunitProjectConstants {
    public static final String TEST_PROPERTIES_PATH ="./";
    
    public static String getTestPropertiesPath() {
        String path = System.getProperty("test.properties");
        return (path == null) ? TEST_PROPERTIES_PATH : path;
    }
}
