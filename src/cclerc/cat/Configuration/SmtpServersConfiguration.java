package cclerc.cat.Configuration;

import cclerc.services.Display;
import cclerc.services.Utilities;
import org.jdom2.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Internal class for smtp servers configuration
 */
public class SmtpServersConfiguration extends AbstractConfiguration {

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(Arrays.asList("preferredSmtpServer"));

    protected String preferredSmtpServer;
    private List<SmtpServerConfiguration> smtpServerConfigurations = new ArrayList<>();

    // SETTERS

    public void setPreferredSmtpServer(String aInPreferredSmtpServer) {
        if (aInPreferredSmtpServer != null && !aInPreferredSmtpServer.equals(preferredSmtpServer)) {
            preferredSmtpServer = aInPreferredSmtpServer;
        }
    }

    public void removeAllSmtpServersConfiguration() {
        smtpServerConfigurations.clear();
    }

    public void addSmtpServerConfiguration(SmtpServerConfiguration aInSmtpServerConfiguration) {
        smtpServerConfigurations.add(aInSmtpServerConfiguration);
    }

    public void removeSmtpServerConfiguration(int lIndex) {
        smtpServerConfigurations.remove(lIndex);
    }

    // GETTERS

    public String getPreferredSmtpServer() {
        return preferredSmtpServer;
    }

    public List<SmtpServerConfiguration> getSmtpServerConfigurations() {
        return smtpServerConfigurations;
    }

    public SmtpServerConfiguration findSmtpServerConfiguration(String aInSmtpServerConfigurationName) {
        for (SmtpServerConfiguration lSmtpServerConfiguration: smtpServerConfigurations) {
            if (lSmtpServerConfiguration.getName().equals(aInSmtpServerConfigurationName)) return lSmtpServerConfiguration;
        }
        return null;
    }

    public int countSmtpSmtpServerConfiguration(String aInSmtpServerConfigurationName) {
        int lCount = 0;
        for (SmtpServerConfiguration lSmtpServerConfiguration: smtpServerConfigurations) {
            if (lSmtpServerConfiguration.getName().equals(aInSmtpServerConfigurationName)) lCount++;
        }
        return lCount;
    }

    // CONSTRUCTORS

    public SmtpServersConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes
        super("smtpServersConfiguration", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, false);

        if (aInElement != null) {

            boolean lPreferredSmtpServerValid = false;

            // Add all smtpServer elements
            for (Element lSmtpServerConfigurationElement : aInElement.getChildren("smtpServer")) {
                // Add smtpServer only if it contains no error
                try {
                    SmtpServerConfiguration lSmtpServerConfiguration =
                            new SmtpServerConfiguration(aInConfiguration, aInConfigurationFile, aInDisplayError, lSmtpServerConfigurationElement);
                    smtpServerConfigurations.add(lSmtpServerConfiguration);
                    if ((preferredSmtpServer != null) &&
                        preferredSmtpServer.equals(lSmtpServerConfiguration.getUser() + '@' + lSmtpServerConfiguration.getName())) lPreferredSmtpServerValid = true;
                } catch (Exception e) {
                    Display.logUnexpectedError(e);
                }
            }

            // Check if preferredSmtpServer is valid (i.e. is one of the smtpServer)
            if (!lPreferredSmtpServerValid) {
                if (getPreferredSmtpServer() != null) Display.getLogger().error(String.format(
                        Display.getMessagesResourceBundle().getString("log.configuration.elementIgnored"), "preferredSmtpServer", preferredSmtpServer));
                preferredSmtpServer = null;
            }

        }

    }

    public SmtpServersConfiguration(Configuration aInConfiguration) {
        super("smtpServersConfiguration", aInConfiguration, "smtpServers", ATTRIBUTE_NAMES);
    }

    // METHODS

    @Override
    public Element save() {

        Element lSmtpServersConfigurationElement = super.save();

        // Save all smtpServerConfiguration
        for (SmtpServerConfiguration lSmtpServerConfiguration : smtpServerConfigurations) {
            lSmtpServersConfigurationElement.addContent(lSmtpServerConfiguration.save());
        }

        return lSmtpServersConfigurationElement;

    }

    public boolean isSameAs(SmtpServersConfiguration aInSmtpServersConfiguration) {

        // Compare smtpServerConfigurations - If they have not the same size, configurations are not the same. If they have, compare each smtp configuration
        boolean lSameSmtpServerConfigurations = (smtpServerConfigurations == null && aInSmtpServersConfiguration.getSmtpServerConfigurations() == null) ||
                                                (smtpServerConfigurations != null && aInSmtpServersConfiguration.getSmtpServerConfigurations() != null &&
                                                 smtpServerConfigurations.size() == aInSmtpServersConfiguration.getSmtpServerConfigurations().size());

        // SMTP servers configurations are the same if all elements are identical and at the same position
        if (lSameSmtpServerConfigurations) {
            for (int lIndex = 0; lIndex < aInSmtpServersConfiguration.getSmtpServerConfigurations().size(); lIndex++) {
                if (!aInSmtpServersConfiguration.getSmtpServerConfigurations().get(lIndex).isSameAs(getSmtpServerConfigurations().get(lIndex))) {
                    lSameSmtpServerConfigurations = false;
                    break;
                }
            }
        }

        return super.isSameAs(aInSmtpServersConfiguration) && lSameSmtpServerConfigurations;

    }

}
