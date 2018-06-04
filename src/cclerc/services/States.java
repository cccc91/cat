package cclerc.services;

import java.io.File;
import java.util.Properties;

/**
 * Singleton class implementing preferences services
 */
public class States extends cclerc.services.Properties {

    private static final String PROPERTY_FILE_NAME = "states.properties";
    private Properties properties;
    private File propertyFile;

    // Preferences instance
    private static States statesInstance = new States(PROPERTY_FILE_NAME);

    /**
     * Preferences constructor
     */
    private States(String aInPropertyFileName) {
        super(aInPropertyFileName);
    }

    // SINGLETON

    /**
     * Returns the singleton
     * @return Locale utility singleton instance
     */
    public static States getInstance() {
        return statesInstance;
    }

}
