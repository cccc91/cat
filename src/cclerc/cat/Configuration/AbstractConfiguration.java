package cclerc.cat.Configuration;

import cclerc.services.Display;
import cclerc.services.Utilities;
import org.jdom2.Element;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract configuration class
 */
public abstract class AbstractConfiguration {

    private String name;

    /**
     * Finds the default value of a configuration attribute from the method getDefault(<AttributeName>) in the abstract configuration class
     * @param aInConfiguration Configuration
     * @param aInAttribute     Attribute name
     * @return Default value of the attribute
     */
    public static String findDefaultValue(AbstractConfiguration aInConfiguration, String aInAttribute) {

        if (aInConfiguration == null) return null;

        try {
            Method lGetDefaultMethod = AbstractConfiguration.class.getDeclaredMethod("getDefault", String.class);
            try {
                aInAttribute = aInAttribute.substring(0, 1).toLowerCase() + aInAttribute.substring(1);
                Object lDefaultValueObject =  lGetDefaultMethod.invoke(aInConfiguration, aInAttribute);
                if (lDefaultValueObject != null) return lDefaultValueObject.toString();
                return null;
            } catch (InvocationTargetException|IllegalAccessException ex) {
                return null;
            }
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Finds the value of a configuration attribute from the method get(<AttributeName>) in the abstract configuration class
     * @param aInConfiguration Configuration
     * @param aInAttribute     Attribute name
     * @return Value of the attribute
     */
    public static String findValue(AbstractConfiguration aInConfiguration, String aInAttribute) {

        if (aInConfiguration == null) return null;

        try {
            Method lGetMethod = AbstractConfiguration.class.getDeclaredMethod("get", String.class);
            try {
                aInAttribute = aInAttribute.substring(0, 1).toLowerCase() + aInAttribute.substring(1);
                Object lValueObject =  lGetMethod.invoke(aInConfiguration, aInAttribute);
                if (lValueObject != null) return lValueObject.toString();
                return null;
            } catch (InvocationTargetException|IllegalAccessException ex) {
                return null;
            }
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    // Bad address in address list exception
    public static class AddressListException extends Exception {
        public AddressListException(Throwable cause) {
            super(cause);
        }
    }

    private List<String> attributeNames = new ArrayList<>();
    private String elementName;

    protected Configuration configuration;
    protected AbstractConfiguration parentConfiguration;
    protected String configurationFile;
    protected boolean displayError;

    // SETTERS

    /**
     * Generic setter of an property to its default value. Tries to invoke a specific setter to default value in the child class.
     * If it does not exist, tries to access to the property declaration (must be declared protected in the child class)
     * and sets it to the default value retrieved using default value generic getter
     *
     * @param aInProperty Property name
     */
    public void setToDefault(String aInProperty) throws Exception {

        // Try to invoke setter "setToDefault<PropertyName>"
        String aInMethodName = "setToDefault" + aInProperty.substring(0, 1).toUpperCase() + aInProperty.substring(1);
        try {
            Method lMethod = this.getClass().getDeclaredMethod(aInMethodName);
            lMethod.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            try {
                // If setter does not exist, try to set property directly
                Field lProperty = this.getClass().getDeclaredField(aInProperty);
                if (lProperty.getType().equals(String.class)) {
                    lProperty.set(this, getDefault(aInProperty));
                } else {
                    Method lParseMethod = lProperty.getType().getMethod("valueOf", new Class[]{String.class});
                    lProperty.set(this, lParseMethod.invoke(lProperty, getDefault(aInProperty)));
                }
            } catch (Exception ex) {
            }
        }

    }

    /**
     * Generic setter of static default values properties in a specific class with values defined in an object containing the values of these default values
     *
     * @param aInPropertiesNames Default values properties to set (must be declared static and NOT final)
     * @param aInDefaultsObject  Object containing the values of all default values properties
     * @param aInClassToSet      Class to which the default values properties must be set
     */
    public void setDefaults(List<String> aInPropertiesNames, AbstractConfiguration aInDefaultsObject, Class aInClassToSet) {

        // Parse all properties
        for (String aInPropertyName : aInPropertiesNames) {
            String aInMethodName = "setDefault" + aInPropertyName.substring(0, 1).toUpperCase() + aInPropertyName.substring(1);
            try {
                Method lMethod = aInClassToSet.getDeclaredMethod(aInMethodName, String.class);
                lMethod.invoke(this, aInDefaultsObject.get(aInPropertyName));
            } catch (Exception e) {
                System.out.println("Program error: " + e);
            }
        }
    }

    /**
     * Generic setter of a property. Tries to invoke a specific setter in the child class.
     * If it does not exist, tries to access to the property declaration (must be declared protected in the child class)  and sets it to the provided value
     *
     * @param aInProperty Parameter name
     */
    public void set(String aInProperty, String aInValue) throws Exception {

        // Try to invoke setter "set<PropertyName>"
        try {
            String lMethodName = "set" + aInProperty.substring(0, 1).toUpperCase() + aInProperty.substring(1);
            Method lMethod = this.getClass().getDeclaredMethod(lMethodName, String.class);
            lMethod.invoke(this, aInValue);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // If setter does not exist, try to set property directly
            Field lProperty = this.getClass().getDeclaredField(aInProperty);
            if (lProperty.getType().equals(String.class)) {
                if ((lProperty.get(this) != null) && !(lProperty.get(this).equals(aInValue))) {
                    lProperty.set(this, aInValue);
                }
            } else {
                Method lParseMethod = lProperty.getType().getMethod("valueOf", new Class[]{String.class});
                if ((lProperty.get(this) != null) && !(lProperty.get(this).equals(aInValue))) {
                    lProperty.set(this, lParseMethod.invoke(lProperty, aInValue));
                }
            }
        }

    }

    /**
     * Sets configuration
     * @param aInConfiguration Configuration
     */
    public void setConfiguration(Configuration aInConfiguration) {
        configuration = aInConfiguration;
    }

    /**
     * Sets configuration file
     * @param aInConfigurationFile Configuration file path
     */
    public void setConfigurationFile(String aInConfigurationFile) {
        configurationFile = aInConfigurationFile;
    }

    /**
     * Sets parent configuration
     * @param aInParentConfiguration Parent configuration
     */
    public void setParentConfiguration(AbstractConfiguration aInParentConfiguration) {
        parentConfiguration = aInParentConfiguration;
    }

    // GETTERS

    public String getName() {
        return name;
    }

    /**
     * Generic getter of default value of a property. Tries to invoke a specific default value getter in the child class.
     * If it does not exist, tries to access to the default property declaration
     * (must be declared protected in the child class and be named DEFAULT_<upper case property with words separated with _)
     * If child class contains neither default getter nor default property declaration, returns null
     *
     * @param aInProperty Property name
     * @return Property default value
     */
    public String getDefault(String aInProperty) {

        // Try to invoke getter "getDefault<PropertyName>"
        String aInMethodName = "getDefault" + aInProperty.substring(0, 1).toUpperCase() + aInProperty.substring(1);
        try {
            Method lMethod = this.getClass().getDeclaredMethod(aInMethodName);
            return (lMethod.invoke(this) == null) ? null : String.valueOf(lMethod.invoke(this));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // If getter does not exist, try to get attribute directly
            try {
                List<String> lWords = Arrays.asList(aInProperty.split("(?=\\p{Upper})"));
                String lDefaultProperty = "DEFAULT";
                for (String lWord : lWords) {
                    lDefaultProperty += "_" + lWord.toUpperCase();
                }
                return (this.getClass().getDeclaredField(lDefaultProperty).get(this) == null) ? null : this.getClass().getDeclaredField(lDefaultProperty).get(this).toString();
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                return null;
            }
        }

    }

    /**
     * Generic getter of a property. Tries to invoke a specific getter in the child class.
     * If it does not exist, tries to access to the property declaration (must be declared protected in the child class)
     * If child class contains neither getter nor parpropertyameter declaration, returns null
     *
     * @param aInProperty Parameter name
     * @return Parameter value
     */
    public String get(String aInProperty) throws Exception {

        // Try to invoke getter "get<PropertyName>"
        try {
            String aInMethodName = "get" + aInProperty.substring(0, 1).toUpperCase() + aInProperty.substring(1);
            Method lMethod = this.getClass().getDeclaredMethod(aInMethodName);
            return String.valueOf(lMethod.invoke(this));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // If getter does not exist, try to get property directly
            try {
                return (this.getClass().getDeclaredField(aInProperty).get(this) == null) ? null : this.getClass().getDeclaredField(aInProperty).get(this).toString();
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                return null;
            }
        }

    }

    /**
     * Gets configuration
     * @return Configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Gets parent configuration
     * @return Parent configuration
     */
    public AbstractConfiguration getParentConfiguration() {
        return parentConfiguration;
    }

    // CONSTRUCTORS

    /**
     * Configuration constructor
     *
     * @param aInConfiguration        Global configuration to which the current configuration element is attached
     * @param aInConfigurationFile    Name of the configuration file containing configuration to instantiate, for displaying in error messages
     * @param aInDisplayError         Flag for displaying some errors
     * @param aInElement              Element of the configuration file containing the current configuration to instantiate
     * @param aInAttributeNames       List of attribute names that must be read in the element. Protected properties with the same names must exist in the child class
     * @param aInIgnoreElementOnError Flag to ignore errors in the configuration
     * @throws Exception Thrown if errors are not ignored, represents the error on an attribute
     */
    public AbstractConfiguration(String aInName, Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError,
                                 Element aInElement, List<String> aInAttributeNames, boolean aInIgnoreElementOnError) throws Exception {
        configuration = aInConfiguration;
        configurationFile = aInConfigurationFile;
        displayError = aInDisplayError;
        attributeNames = aInAttributeNames;
        name = aInName;

        if (aInElement != null) {

            elementName = aInElement.getName();

            // Parse expected attributes
            for (String lAttributeName : attributeNames) {
                try {
                    setToDefault(lAttributeName);
                    set(lAttributeName, aInElement.getAttributeValue(lAttributeName));
                } catch (Exception e) {
                    Configuration.configurationError(e, configurationFile, elementName, lAttributeName, displayError);
                    if (aInIgnoreElementOnError) {
                        Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.configuration.elementIgnored"), elementName, lAttributeName));
                        throw e;
                    }
                }
            }

        }
    }

    public AbstractConfiguration(String aInName, Configuration aInConfiguration, String aInElementName, List<String> aInAttributeNames) {
        configuration = aInConfiguration;
        elementName = aInElementName;
        attributeNames = aInAttributeNames;
        name = aInName;

        // Parse expected attributes
        for (String lAttributeName : attributeNames) {
            try {
                setToDefault(lAttributeName);
            } catch (Exception e) {
                Configuration.configurationError(e, configurationFile, elementName, lAttributeName, displayError);
            }
        }

    }

    // METHODS

    /**
     * Computes file name from expected file name and default value.
     * If file name is null, returns default file name path in resources directory
     * If file name is not null, replaces \ with /, converts its path if file is in resources directory, and raises eFileNotFoundException if file is not valid
     *
     * @param aInFile    Expected file name
     * @param aInDefault Default file name
     * @return Computed file name
     * @throws FileNotFoundException
     */
    protected String computeFileName(String aInFile, String aInDefault) throws FileNotFoundException {

        // TODO remplacer par resources/... si path absolu correspondant au répertoire resource actuel

        if (aInFile != null) {
//            File lResourceFile = new File("resources/dummy.txt");
//            String lResourcesPath = lResourceFile.getParentFile().getAbsolutePath();
//            aInFile = aInFile.replace(lResourcesPath, "");
            File lFile = new File(aInFile);
            if (aInFile.startsWith("resources/")) {
                try {
                    getClass().getClassLoader().getResource(aInFile).toURI().toString();
                } catch (Exception e) {
                    throw new FileNotFoundException(aInFile);
                }
            } else {
                if (!lFile.exists() || lFile.isDirectory()) {
                    throw new FileNotFoundException(aInFile);
                }
            }
        } else {
            aInFile = aInDefault;
        }
        return aInFile;

    }

    /**
     * Saves the current configuration as a jdom element so that it can be written in xml configuration file
     *
     * @param aInAttributeName Name of the key attribute for which value must be displayed in case of error in one of the attributes of the configuration (can be null)
     * @return Element that can be added in the global configuration for saving xml configuration file
     */
    public Element save(String aInAttributeName) {

        Element lElement = new Element(elementName);
        for (String lAttributeName : attributeNames) {
            try {
                String lAttributeValue = get(lAttributeName);
                // Paths in "resources/" are set to a relative path starting at "resources"
                if (lAttributeValue.contains("resources/")) {
                    lAttributeValue = lAttributeValue.substring(lAttributeValue.indexOf("resources/"));
                }
                if (!lAttributeValue.equals("null") && !lAttributeValue.equals(getDefault(lAttributeName))) lElement.setAttribute(lAttributeName, lAttributeValue);
            } catch (Exception e) {
                if (aInAttributeName == null) {
                    Display.getLogger().warn(String.format(Display.getMessagesResourceBundle().getString("log.configuration.parameterGetError"), e.getLocalizedMessage(),
                                                           lAttributeName, elementName));
                } else {
                    try {
                        Display.getLogger().warn(String.format(Display.getMessagesResourceBundle().getString("log.configuration.parameterGetError"), e.getLocalizedMessage(),
                                                               lAttributeName + " " + get(aInAttributeName), elementName));
                    } catch (Exception ex) {
                        Display.getLogger().warn(String.format(Display.getMessagesResourceBundle().getString("log.configuration.parameterGetError"), e.getLocalizedMessage(),
                                                               lAttributeName, elementName));
                    }
                }
            }
        }

        return lElement;

    }

    public Element save() {
        return save(null);
    }

    /**
     * Checks if two configurations are the same, i.e. if all attributes have the same values
     *
     * @param aInConfiguration Configuration to compare current configuration with
     * @return Status of the comparison
     */
    public boolean isSameAs(AbstractConfiguration aInConfiguration) {

        if (aInConfiguration == null) return false;

        // Compare attributes
        for (String lAttributeName : attributeNames) {
            try {
                if (!get(lAttributeName).equals(aInConfiguration.get(lAttributeName))) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;

    }

    /**
     * Checks if two configurations are the same except for some excluded attributes,
     * i.e. if all non excluded attributes have the same values
     *
     * @param aInConfiguration       Configuration to compare current configuration with
     * @param aInExcludedAttributes  List of excluded attributes
     * @return Status of the comparison
     */
    public boolean isSameAs(AbstractConfiguration aInConfiguration, List<String> aInExcludedAttributes) {

        if (aInConfiguration == null) return false;

        // Compare attributes
        for (String lAttributeName : attributeNames) {
            if (!aInExcludedAttributes.contains(lAttributeName)) {
                try {
                    if (!get(lAttributeName).equals(aInConfiguration.get(lAttributeName))) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return true;

    }

    public void copy(AbstractConfiguration aInConfiguration) throws Exception {
        for (String lAttributeName : attributeNames) {
            set(lAttributeName, aInConfiguration.get(lAttributeName));
        }
   }

}
