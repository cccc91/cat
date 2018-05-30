package cclerc.cat.Configuration;

import cclerc.services.Constants;
import org.jdom2.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Internal class for monitoring server configuration
 */
public class ServerConfiguration extends AbstractConfiguration {

    private static final List<String> ATTRIBUTE_NAMES =
            new ArrayList<>(Arrays.asList("hostname", "maxRetries", "pollingPeriod", "timeout", "connectionLostThreshold", "ipv6"));

    // Accessed by introspection
    protected static Integer DEFAULT_MAX_RETRIES = MonitoringDefaultsConfiguration.DEFAULT_MAX_RETRIES;
    protected static Integer DEFAULT_POLLING_PERIOD = MonitoringDefaultsConfiguration.DEFAULT_POLLING_PERIOD;
    protected static Integer DEFAULT_TIMEOUT = MonitoringDefaultsConfiguration.DEFAULT_TIMEOUT;
    protected static Integer DEFAULT_CONNECTION_LOST_THRESHOLD = MonitoringDefaultsConfiguration.DEFAULT_CONNECTION_LOST_THRESHOLD;
    protected static Boolean DEFAULT_IPV6 = MonitoringDefaultsConfiguration.DEFAULT_IPV6;

    private String hostname;
    protected Integer maxRetries;
    protected Integer pollingPeriod;
    protected Integer timeout;
    protected Integer connectionLostThreshold;
    protected Boolean ipv6;

    // DEFAULT SETTERS

    public static void setDefaultMaxRetries(String aInDefaultMaxRetries) {
        DEFAULT_MAX_RETRIES = Integer.valueOf(aInDefaultMaxRetries);
    }

    public static void setDefaultPollingPeriod(String aInDefaultPollingPeriod) {
        DEFAULT_POLLING_PERIOD = Integer.valueOf(aInDefaultPollingPeriod);
    }

    public static void setDefaultTimeout(String aInDefaultTimeout) {
        DEFAULT_TIMEOUT = Integer.valueOf(aInDefaultTimeout);
    }

    public static void setDefaultConnectionLostThreshold(String aInDefaultConnectionLostThreshold) {
        DEFAULT_CONNECTION_LOST_THRESHOLD = Integer.valueOf(aInDefaultConnectionLostThreshold);
    }

    public static void setDefaultIpv6(String aInDefaultIpv6) {
        DEFAULT_IPV6 = Boolean.valueOf(aInDefaultIpv6);
    }

    // SETTERS

    public void setHostname(String aInHostname) throws NullPointerException {
        if (!aInHostname.equals(hostname)) {
            hostname = aInHostname;
        }
    }

    public void setMaxRetries(String aInMaxRetries) throws NumberFormatException {
        if ((aInMaxRetries != null) && !Integer.valueOf(aInMaxRetries).equals(maxRetries)) {
            if (Integer.valueOf(aInMaxRetries) > Constants.MAX_MAX_RETRIES || Integer.valueOf(aInMaxRetries) < Constants.MIN_MAX_RETRIES)
                throw new NumberFormatException();
            maxRetries = Integer.valueOf(aInMaxRetries);
        }
    }

    public void setPollingPeriod(String aInPollingPeriod) throws NumberFormatException {
        if ((aInPollingPeriod != null) && !Integer.valueOf(aInPollingPeriod).equals(pollingPeriod)) {
            if (Integer.valueOf(aInPollingPeriod) > Constants.MAX_POLLING_PERIOD || Integer.valueOf(aInPollingPeriod) < Constants.MIN_POLLING_PERIOD)
                throw new NumberFormatException();
            pollingPeriod = Integer.valueOf(aInPollingPeriod);
        }
    }

    public void setTimeout(String aInTimeout) throws NumberFormatException {
        if ((aInTimeout != null) && !Integer.valueOf(aInTimeout).equals(timeout)) {
            if (Integer.valueOf(aInTimeout) > Constants.MAX_TIMEOUT || Integer.valueOf(aInTimeout) < Constants.MIN_TIMEOUT)
                throw new NumberFormatException();
            timeout = Integer.valueOf(aInTimeout);
        }
    }

    public void setConnectionLostThreshold(String aInConnectionLostThreshold) throws NumberFormatException {
        if ((aInConnectionLostThreshold != null) && !Integer.valueOf(aInConnectionLostThreshold).equals(connectionLostThreshold)) {
            if (Integer.valueOf(aInConnectionLostThreshold) > Constants.MAX_CONNECTION_LOST_THRESHOLD ||
                Integer.valueOf(aInConnectionLostThreshold) < Constants.MIN_CONNECTION_LOST_THRESHOLD) throw new NumberFormatException();
            connectionLostThreshold = Integer.valueOf(aInConnectionLostThreshold);
        }
    }

    public void setIpv6(String aInIpv6) throws IllegalArgumentException {
        if ((aInIpv6 != null) && !Boolean.valueOf(aInIpv6).equals(ipv6)) {
            ipv6 = Boolean.valueOf(aInIpv6);
        }
    }

    // GETTERS

    public String getHostname() {
        return hostname;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public Integer getPollingPeriod() {
        return pollingPeriod;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public Integer getConnectionLostThreshold() {
        return connectionLostThreshold;
    }

    public Boolean getIpv6() {
        return ipv6;
    }

    // CONSTRUCTORS

    public ServerConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes (ignore element on error)
        super("serverConfiguration", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, false);

    }

    public ServerConfiguration(Configuration aInConfiguration, String aInElementName) {
        super("serverConfiguration", aInConfiguration, aInElementName, ATTRIBUTE_NAMES);
        hostname = "";
        maxRetries = DEFAULT_MAX_RETRIES;
        pollingPeriod = DEFAULT_POLLING_PERIOD;
        timeout = DEFAULT_TIMEOUT;
        connectionLostThreshold = DEFAULT_CONNECTION_LOST_THRESHOLD;
        ipv6 = DEFAULT_IPV6;
    }

    // PUBLIC METHODS

    @Override
    public Element save() {
        if (!hostname.equals("")) return super.save("name");
        return null;
    }

}
