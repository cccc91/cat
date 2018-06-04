package cclerc.cat.Configuration;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

/**
 * Internal class for email configuration
 */
public class EmailConfiguration extends AbstractConfiguration {

    // Bad address in address list exception
    public static class AddressListException extends Exception {
        public AddressListException(Throwable cause) {
            super(cause);
        }
    }

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(Arrays.asList("recipientList"));

    // Accessed by introspection
    protected static final String DEFAULT_RECIPIENT_LIST = "";

    protected String recipientList;
    private SmtpServersConfiguration smtpServersConfiguration;

    // SETTERS

    public void setRecipientList(String aInRecipientList) throws AddressListException {

        String lCurrentRecipient = "";
        String lNewRecipientList = lCurrentRecipient;
        try {
            String[] lRecipientList = aInRecipientList.split(";");
            for (String lRecipient : lRecipientList) {
                lCurrentRecipient = lRecipient;
                InternetAddress lInternetAddress = new InternetAddress(lCurrentRecipient);
                lInternetAddress.validate();
                lNewRecipientList += lCurrentRecipient + ';';
            }
            while ((lNewRecipientList.length() > 1) && (lNewRecipientList.substring(lNewRecipientList.length() - 1).equals(";"))) {
                lNewRecipientList = lNewRecipientList.substring(0, lNewRecipientList.length() - 1);
            }
            if (!lNewRecipientList.equals(recipientList)) {
                recipientList = lNewRecipientList;
            }
        } catch (AddressException e) {
            throw new AddressListException(new Throwable(lCurrentRecipient + ": " + e.getLocalizedMessage()));
        }

    }

    private void setSmtpServersConfiguration(SmtpServersConfiguration aInSmtpServersConfiguration) throws NullPointerException {
        if (aInSmtpServersConfiguration == null) throw new NullPointerException();
        smtpServersConfiguration = aInSmtpServersConfiguration;
        smtpServersConfiguration.setParentConfiguration(this);

    }

    // GETTERS

    public String getRecipientList() {
        return recipientList;
    }

    public SmtpServersConfiguration getSmtpServersConfiguration() {
        return smtpServersConfiguration;
    }

    // CONSTRUCTORS

    public EmailConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes
        super("email", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, false);

        if (aInElement != null) {

            // Add smtpServers element
            Element lSmtpServersConfigurationElement = aInElement.getChild("smtpServers");
            if (lSmtpServersConfigurationElement != null)
                setSmtpServersConfiguration(new SmtpServersConfiguration(aInConfiguration, aInConfigurationFile, aInDisplayError, lSmtpServersConfigurationElement));

        }

    }

    public  EmailConfiguration(Configuration aInConfiguration) {
        super("email", aInConfiguration, "email", ATTRIBUTE_NAMES);
        setSmtpServersConfiguration(new SmtpServersConfiguration(aInConfiguration));
    }

    // METHODS

    @Override
    public Element save() {

        Element lEmailElement = super.save();

        // Save smtpServersConfiguration
        if (smtpServersConfiguration != null) lEmailElement.addContent(smtpServersConfiguration.save());

        return lEmailElement;

    }

    public boolean isSameAs(EmailConfiguration aInEmailConfiguration) {
        return super.isSameAs(aInEmailConfiguration) &&
                ((smtpServersConfiguration == null && aInEmailConfiguration.getSmtpServersConfiguration() == null) ||
                        (smtpServersConfiguration != null && smtpServersConfiguration.isSameAs(aInEmailConfiguration.getSmtpServersConfiguration())));
    }

}
