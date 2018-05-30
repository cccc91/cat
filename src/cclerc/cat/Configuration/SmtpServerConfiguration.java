package cclerc.cat.Configuration;

import cclerc.services.Constants;
import cclerc.services.EnumTypes;
import org.jdom2.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Internal class for smtp server configuration
 */
public class SmtpServerConfiguration extends AbstractConfiguration {

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(Arrays.asList("name", "tlsMode", "port", "user", "login", "password", "connectionTimeout", "timeout"));

    protected static final EnumTypes.TlsMode DEFAULT_TLS_MODE = Constants.DEFAULT_SMTP_TLS_MODE;
    protected static final Integer DEFAULT_PORT = Constants.DEFAULT_SMTP_PORT;
    protected static final Integer DEFAULT_CONNECTION_TIMEOUT = Constants.DEFAULT_SMTP_CONNECTION_TIMEOUT;
    protected static final Integer DEFAULT_TIMEOUT = Constants.DEFAULT_SMTP_TIMEOUT;

    private String name;
    protected String tlsMode;
    protected Integer port;
    private String user;
    private String login;
    private String password;
    protected Integer connectionTimeout;
    protected Integer timeout;

    // SETTERS

    public void setName(String aInName) throws IllegalArgumentException {
        if ((aInName == null) || (aInName.equals("")))  throw new IllegalArgumentException();
        if (!aInName.equals(name)) {
            name = aInName;
        }
    }

    public void setTlsMode(String aInTlsMode) throws IllegalArgumentException {
        EnumTypes.TlsMode lTlsMode = (aInTlsMode != null) ? EnumTypes.TlsMode.valueOf(aInTlsMode) : DEFAULT_TLS_MODE;
        if (!lTlsMode.equals(tlsMode)) {
            tlsMode = lTlsMode.toString();
        }
    }

    public void setPort(String aInPort) throws NumberFormatException {
        Integer lPort = (aInPort != null) ? Integer.valueOf(aInPort) : DEFAULT_PORT;
        if (lPort != port) {
            port = lPort;
        }
    }

    public void setUser(String aInUser) throws IllegalArgumentException {
        if ((aInUser == null) || (aInUser.equals("")))  throw new IllegalArgumentException();
        if (!aInUser.equals(user)) {
            user = aInUser;
        }
    }

    public void setLogin(String aInLogin) {
        if (!aInLogin.equals(login)) {
            login = aInLogin;
        }
    }

    public void setPassword(String aInPassword) {
        if (!aInPassword.equals(password)) {
            password = aInPassword;
        }
    }

    public void setConnectionTimeout(String aInConnectionTimeout) throws NumberFormatException {
        Integer lConnectionTimeout = (aInConnectionTimeout != null) ? Integer.valueOf(aInConnectionTimeout) : DEFAULT_CONNECTION_TIMEOUT;;
        if (!lConnectionTimeout.equals(connectionTimeout)) {
            connectionTimeout = lConnectionTimeout;
        }
    }

    public void setTimeout(String aInTimeout) throws NumberFormatException {
        Integer lTimeout = (aInTimeout != null) ? Integer.valueOf(aInTimeout) : DEFAULT_TIMEOUT;
        if (!lTimeout.equals(timeout)) {
            timeout = lTimeout;
        }
    }

    // GETTERS

    public String getName() {
        return name;
    }

    public String getTlsMode() {
        return tlsMode;
    }

    public Integer getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public Integer getTimeout() {
        return timeout;
    }

    // CONSTRUCTOR

    public SmtpServerConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes (ignore element on error)
        super("smtpServerConfiguration", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, true);

    }

    public SmtpServerConfiguration(Configuration aInConfiguration) {
        super("smtpServerConfiguration", aInConfiguration, "smtpServer", ATTRIBUTE_NAMES);
    }

    // METHODS

    @Override
    public Element save() {
        return super.save("name");
    }

}
