package cclerc.cat.Configuration;

/**
 * Created by Christophe on 04/04/2017.
 */

import cclerc.services.Display;
import cclerc.services.Utilities;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderXSDFactory;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class implementing the monitoring currentConfiguration
 */
public class Configuration {

    private static Configuration currentConfiguration;
    private static Configuration initialConfiguration;
    private static String configurationFilePath;

    // Instance properties
    private boolean displayError = true;

    private String file;
    private Document document;
    private Element root;

    // Parsed elements
    private EmailConfiguration emailConfiguration;
    private AlarmsConfiguration alarmsConfiguration;
    private MonitoringConfiguration monitoringConfiguration;
    private GlobalMonitoringConfiguration globalMonitoringConfiguration;

    // Listeners
    private List<ConfigurationChangeListener> listeners = new ArrayList();

    /**
     * Logs the correct error depending on the exception that is raised
     *
     * @param aInException         Exception
     * @param aInConfigurationFile Configuration file name
     * @param aInNode              XML node of the currentConfiguration file in which the error is found
     * @param aInParameter         Parameter in error in the node
     * @param aInDisplayError      Flag to indicate if the error must be displayed
     */
    public static void configurationError(Exception aInException, String aInConfigurationFile, String aInNode, String aInParameter, boolean aInDisplayError) {

        if (aInException.getClass().getCanonicalName().contains("InvocationTargetException")) {

            Exception lException = (Exception) ((InvocationTargetException) aInException).getTargetException();
            String lExceptionName = lException.getClass().getCanonicalName().replaceAll(".*\\.", "");

            switch (lExceptionName) {
                case "IllegalArgumentException":
                case "AddressListException":
                case "FileNotFoundException":
                case "ParseException":
                    if (aInDisplayError) {
                        Display.getLogger().warn(String.format(Display.getMessagesResourceBundle().getString("log.configuration.parameterValueError"),
                                                               lException.getLocalizedMessage().replaceAll("\\S+: ", ""), aInParameter, aInNode, aInConfigurationFile));
                    }
                    break;
                case "NullPointerException":
                    if (aInDisplayError) {
                        Display.getLogger().warn(String.format(Display.getMessagesResourceBundle().getString("log.configuration.parameterValueNotDefined"),
                                                               aInParameter, aInNode, aInConfigurationFile));
                    }
                    break;
                default:
                    if (aInDisplayError) {
                        Display.getLogger().warn(String.format(Display.getMessagesResourceBundle().getString("log.configuration.parameterUnexpectedError"),
                                                               lException.getLocalizedMessage(), aInParameter, aInNode, aInConfigurationFile));
                    }
                    break;
            }

        }

    }

    /**
     * Configuration constructor
     *
     * @param aInConfigurationFile Configuration
     * @param aInDisplayErrors     Flag to indicate if errors must be displayed
     */
    public Configuration(String aInConfigurationFile, boolean aInDisplayErrors) throws Exception {

        file = aInConfigurationFile;
        SAXBuilder lSaxBuilder = new SAXBuilder(new XMLReaderXSDFactory(getClass().getClassLoader().getResource("resources/schema/configuration.xsd")));

        displayError = aInDisplayErrors;

        try {

            // Load currentConfiguration file
            document = lSaxBuilder.build(new File(aInConfigurationFile));
            root = document.getRootElement();

            // Parse children of root element
            emailConfiguration = new EmailConfiguration(this, aInConfigurationFile, displayError, root.getChild("email"));
            alarmsConfiguration = new AlarmsConfiguration(this, aInConfigurationFile, displayError, root.getChild("alarms"));
            monitoringConfiguration = new MonitoringConfiguration(this, aInConfigurationFile, displayError, root.getChild("monitoring"));
            globalMonitoringConfiguration = new GlobalMonitoringConfiguration(this, aInConfigurationFile, displayError, root.getChild("globalMonitoring"));

        } catch (IOException e) {
            if (displayError)
                Display.getLogger().error(
                        String.format(Display.getMessagesResourceBundle().getString("log.configuration.file.readError"), aInConfigurationFile, e.getLocalizedMessage()));
            throw e;
        } catch (JDOMException e) {
            if (displayError)
                Display.getLogger().error(
                        String.format(Display.getMessagesResourceBundle().getString("log.configuration.parsingError"), aInConfigurationFile, e.getLocalizedMessage()));
            throw e;
        }

    }

    public Configuration() {

        emailConfiguration = new EmailConfiguration(this);
        alarmsConfiguration = new AlarmsConfiguration(this);
        monitoringConfiguration = new MonitoringConfiguration(this);
        globalMonitoringConfiguration = new GlobalMonitoringConfiguration(this);

    }

    // SETTERS

    /**
     * Sets configuration file
     * @param aInFile configuration file
     */
    public void setFile(String aInFile) {
        file = aInFile;
    }

    // GETTERS

    /**
     * Gets configuration file
     * @return configuration file
     */
    public String getFile() {
        return file;
    }

    /**
     * Gets current configuration
     * @return current configuration
     */
    public static Configuration getCurrentConfiguration() {
        return currentConfiguration;
    }

    /**
     * Gets initial configuration
     * @return initial configuration
     */
    public static Configuration getInitialConfiguration() {
        return initialConfiguration;
    }

    /**
     * Gets email currentConfiguration
     *
     * @return Email currentConfiguration
     */
    public EmailConfiguration getEmailConfiguration() {
        return emailConfiguration;
    }

    /**
     * Gets monitoring currentConfiguration
     *
     * @return Monitoring currentConfiguration
     */
    public MonitoringConfiguration getMonitoringConfiguration() {
        return monitoringConfiguration;
    }

    /**
     * Gets global monitoring currentConfiguration
     *
     * @return Global monitoring currentConfiguration
     */
    public GlobalMonitoringConfiguration getGlobalMonitoringConfiguration() {
        return globalMonitoringConfiguration;
    }

    /**
     * Gets alarms currentConfiguration
     *
     * @return Alarms currentConfiguration
     */
    public AlarmsConfiguration getAlarmsConfiguration() {
        return alarmsConfiguration;
    }

    // METHODS

    /**
     * Loads current configuration from XML configuration file and saves it as the initial configuration
     * @param aInConfigurationFilePath XML configuration file path
     */
    public static void loadConfiguration(String aInConfigurationFilePath) throws Exception {
        configurationFilePath = aInConfigurationFilePath;
        initialConfiguration = new Configuration(aInConfigurationFilePath, true);
        currentConfiguration = new Configuration(aInConfigurationFilePath, false);
        addConfigurationChangeObserver();
    }

    public static void createDefaultConfiguration() {
        initialConfiguration = new Configuration();
        currentConfiguration = new Configuration();
        currentConfiguration.addListener(new ConfigurationChangeObserver());
    }

    public static void addConfigurationChangeObserver() {
        currentConfiguration.addListener(new ConfigurationChangeObserver());
    }

    /**
     * Changes initial configuration after a save
     */
    public static void resetInitialConfiguration() {
        try {
            initialConfiguration = new Configuration(configurationFilePath, true);
        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.incorrectConfigurationFile"), configurationFilePath));
        }
    }

    /**
     * Changes configuration after creation
     */
    public static void resetConfiguration() {
        try {
            initialConfiguration = new Configuration(configurationFilePath, true);
            currentConfiguration = new Configuration(configurationFilePath, false);
        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.incorrectConfigurationFile"), configurationFilePath));
        }
    }

    public boolean isSameAs(Configuration aInConfiguration) {

        return ((emailConfiguration == null && aInConfiguration.getAlarmsConfiguration() == null) ||
                (emailConfiguration != null && emailConfiguration.isSameAs(aInConfiguration.getEmailConfiguration()))) &&
               ((alarmsConfiguration == null && aInConfiguration.getAlarmsConfiguration() == null) ||
                (alarmsConfiguration.isSameAs(aInConfiguration.getAlarmsConfiguration()))) &&
               ((globalMonitoringConfiguration == null && aInConfiguration.getGlobalMonitoringConfiguration() == null) ||
                (globalMonitoringConfiguration.isSameAs(aInConfiguration.getGlobalMonitoringConfiguration()))) &&
               ((monitoringConfiguration == null && aInConfiguration.getMonitoringConfiguration() == null) ||
                (monitoringConfiguration != null && monitoringConfiguration.isSameAs((aInConfiguration.getMonitoringConfiguration()))));

    }

    public void save() {

        // Document creation
        Document lDocument = new Document();
        Namespace lNamespace = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        // Root element creation
        if (root == null)  root = new Element("configuration");
        Element lRootElement = new Element(root.getName());
        for (Attribute lAttribute: root.getAttributes()) {
            if (lAttribute.getName().equals("xmlns")  || lAttribute.getName().equals("noNamespaceSchemaLocation")) {
                lRootElement.setAttribute(lAttribute.getName(), lAttribute.getValue(), lNamespace);
            } else {
                lRootElement.setAttribute(lAttribute.getName(), lAttribute.getValue());
            }
        }
        lDocument.setRootElement(lRootElement);

        // Create children
        if (emailConfiguration != null) lRootElement.addContent(emailConfiguration.save());
        if (alarmsConfiguration != null) lRootElement.addContent(alarmsConfiguration.save());
        if (monitoringConfiguration != null) lRootElement.addContent(monitoringConfiguration.save());
        if (globalMonitoringConfiguration != null) lRootElement.addContent(globalMonitoringConfiguration.save());

        // Save the currentConfiguration
        try {
            XMLOutputter lOutputter = new XMLOutputter(Format.getPrettyFormat().setIndent("    "));
            lOutputter.output(lDocument, new FileOutputStream(file));
        } catch (java.io.IOException e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.configuration.file.writeError"), file, Utilities.getStackTrace(e)));
        }
    }

    public void saveAs(String aInConfigurationFile) {
        file = aInConfigurationFile;
        configurationFilePath = aInConfigurationFile;
        save();
    }

    public void addListener(ConfigurationChangeListener aInConfigurationChangeListener) {
        if (aInConfigurationChangeListener != null) listeners.add(aInConfigurationChangeListener);
    }

    /**
     * Warn listeners of a change of configuration
     * @param aInChangeType
     * @param aInObjectChanged
     * @param aInAttributeChanged
     * @param aInNewAttributeValue
     */
    public void fireEvent(ConfigurationChangeListener.ChangeType aInChangeType, Object aInObjectChanged, String aInAttributeChanged, Object aInNewAttributeValue) {
        for (ConfigurationChangeListener lConfigurationChangeListener : listeners) {
            lConfigurationChangeListener.configurationChanged(aInChangeType, aInObjectChanged, aInAttributeChanged, aInNewAttributeValue);
        }
    }

}
