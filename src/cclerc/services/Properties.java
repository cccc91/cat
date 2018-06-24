package cclerc.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Singleton class implementing preferences services
 */
public class Properties {

    private java.util.Properties properties;
    private File propertyFile;

    /**
     * Properties constructor
     * @param aInPropertyFileName Property file name
     */
    protected Properties(String aInPropertyFileName) {

        try {

            // Preferences are saved in config directory under installation directory
            String lPath = Constants.CURRENT_DIRECTORY + "/config/";
            File lPropertyPath = new File(lPath);
            propertyFile = new File(lPath + aInPropertyFileName);

            // Create property file with path if needed
            if (!lPropertyPath.exists()) lPropertyPath.mkdir();
            if (!propertyFile.exists()) propertyFile.createNewFile();

            // Load properties
            properties = new java.util.Properties();
            properties.load(new FileInputStream(propertyFile.getPath()));


        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }

    }

    /**
     * Removes a property
     * @param aInKey Property key
     */
    public void removeKey(String aInKey) {
        properties.remove(aInKey);
    }

    /**
     * Gets a property value
     * @param aInKey Property key
     * @return Property value
     */
    public String getValue(String aInKey) {
        return properties.getProperty(aInKey);
    }

    /**
     * Gets a property value
     * @param aInKey Property key
     * @param aInDefaultValue Property default value if not found
     * @return Property value
     */
    public String getValue(String aInKey, String aInDefaultValue) {
        return (properties.getProperty(aInKey) != null) ? properties.getProperty(aInKey) : aInDefaultValue;
    }

    /**
     * Gets a property boolean value
     * @param aInKey Property key
     * @return true if the value of the property is "true", false otherwise
     */
    public Boolean getBooleanValue(String aInKey) {
        try {
            return Boolean.valueOf(getValue(aInKey));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets a property boolean value
     * @param aInKey Property key
     * @param aInDefaultValue Property default value if not found
     * @return true if the value of the property is "true", default value otherwise
     */
    public Boolean getBooleanValue(String aInKey, Boolean aInDefaultValue) {
        try {
            return Boolean.valueOf(getValue(aInKey, String.valueOf(aInDefaultValue)));
        } catch (Exception e) {
            return aInDefaultValue;
        }
    }

    /**
     * Gets a property integer value
     * @param aInKey Property key
     * @param aInDefaultValue Property default value if not found
     * @return integer value if found and valid, default value otherwise
     */
    public Integer getIntegerValue(String aInKey, Integer aInDefaultValue) {
        try {
            return Integer.valueOf(getValue(aInKey, String.valueOf(aInDefaultValue)));
        } catch (Exception e) {
            return aInDefaultValue;
        }
    }

    /**
     * Gets a property long value
     * @param aInKey Property key
     * @param aInDefaultValue Property default value if not found
     * @return long value if found and valid, default value otherwise
     */
    public Long getLongValue(String aInKey, Long aInDefaultValue) {
        try {
            return Long.valueOf(getValue(aInKey, String.valueOf(aInDefaultValue)));
        } catch (Exception e) {
            return aInDefaultValue;
        }
    }

    /**
     * Gets a property double value
     * @param aInKey Property key
     * @param aInDefaultValue Property default value if not found
     * @return double value if found and valid, default value otherwise
     */
    public Double getDoubleValue(String aInKey, Double aInDefaultValue) {
        try {
            return Double.valueOf(getValue(aInKey, String.valueOf(aInDefaultValue)));
        } catch (Exception e) {
            return aInDefaultValue;
        }
    }

    /**
     * Saves a property value
     * @param aInKey   Property key
     * @param aInValue Property value
     */
    public void saveValue(String aInKey, Object aInValue) {
        properties.setProperty(aInKey, String.valueOf(aInValue));
        savePropertyFile();
    }

    /**
     * Sets a property value
     * @param aInKey   Property key
     * @param aInValue Property value
     */
    public void setValue(String aInKey, Object aInValue) {
        properties.setProperty(aInKey, String.valueOf(aInValue));
    }

    /**
     * Saves property file
     */
    public void savePropertyFile() {
        try {
            FileOutputStream lPropertyFileOutputStream = new FileOutputStream(propertyFile.getPath());
            properties.store(lPropertyFileOutputStream, null);
        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }
    }

}
