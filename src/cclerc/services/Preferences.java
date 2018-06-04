package cclerc.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Singleton class implementing preferences services
 */
public class Preferences extends cclerc.services.Properties {

    private static final String PROPERTY_FILE_NAME = "preferences.properties";
    private Properties properties;
    private File propertyFile;

    // Preferences instance
    private static Preferences preferencesInstance = new Preferences(PROPERTY_FILE_NAME);

    /**
     * Preferences constructor
     */
    private Preferences(String aInPropertyFileName) {
        super(aInPropertyFileName);
    }

    // SINGLETON

    /**
     * Returns the singleton
     * @return Locale utility singleton instance
     */
    public static Preferences getInstance() {
        return preferencesInstance;
    }

}
