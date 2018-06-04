package cclerc.cat.model;

import cclerc.cat.Configuration.SmtpServerConfiguration;
import cclerc.services.EnumTypes;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ConfiguredSmtpServer {

    private StringProperty name;
    private EnumTypes.TlsMode tlsMode;
    private IntegerProperty port;
    private StringProperty user;
    private StringProperty login;
    private StringProperty password;
    private IntegerProperty connectionTimeout;
    private IntegerProperty timeout;

    // GETTERS

    public String getName() {
        return name.get();
    }

    public EnumTypes.TlsMode getTlsMode() {
        return tlsMode;
    }

    public Integer getPort() {
        return  port.get();
    }

    public String getUser() {
        return user.get();
    }

    public String getLogin() {
        return login.get();
    }

    public String getPassword() {
        return password.get();
    }

    public Integer getConnectionTimeout() {
        return  connectionTimeout.get();
    }

    public Integer getTimeout() {
        return  timeout.get();
    }


    // PROPERTY GETTERS

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty tlsModeProperty() {
        return new SimpleStringProperty(tlsMode.toString());
    }

    public IntegerProperty portProperty() {
        return port;
    }

    public StringProperty userProperty() {
        return user;
    }

    public StringProperty loginProperty() {
        return login;
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public IntegerProperty connectionTimeoutProperty() {
        return connectionTimeout;
    }

    public IntegerProperty timeoutProperty() {
        return timeout;
    }

    // SETTERS

    public void setName(String aInName) {
        name.set(aInName);
    }

    public void setTlsMode(EnumTypes.TlsMode aInTlsMode) {
        tlsMode = aInTlsMode;
    }

    public void setPort(int aInPort) {
        port.set(aInPort);
    }

    public void setUser(String aInUser) {
        user.set(aInUser);
    }

    public void setLogin(String aInLogin) {
        login.set(aInLogin);
    }

    public void setPassword(String aInPassword) {
        password.set(aInPassword);
    }

    public void setConnectionTimeout(int aInConnectionTimeout) {
        connectionTimeout.set(aInConnectionTimeout);
    }

    public void setTimeout(int aInTimeout) {
        timeout.set(aInTimeout);
    }

    // CONSTRUCTORS

    /**
     * Builds the displayed SMTP server from an SMTP server defined in the SMTP servers configuration
     * @param aInSmtpServerConfiguration SMTP server configuration
     */
    public ConfiguredSmtpServer(SmtpServerConfiguration aInSmtpServerConfiguration) {

        name = new SimpleStringProperty(aInSmtpServerConfiguration.getName());
        tlsMode = EnumTypes.TlsMode.valueOf(aInSmtpServerConfiguration.getTlsMode());
        port = new SimpleIntegerProperty(aInSmtpServerConfiguration.getPort());
        user = new SimpleStringProperty(aInSmtpServerConfiguration.getUser());
        login = new SimpleStringProperty(aInSmtpServerConfiguration.getLogin());
        password = new SimpleStringProperty(aInSmtpServerConfiguration.getPassword());
        connectionTimeout = new SimpleIntegerProperty(aInSmtpServerConfiguration.getConnectionTimeout());
        timeout = new SimpleIntegerProperty(aInSmtpServerConfiguration.getTimeout());

    }

}
