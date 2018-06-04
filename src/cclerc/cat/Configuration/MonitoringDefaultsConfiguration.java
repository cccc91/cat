package cclerc.cat.Configuration;

import cclerc.services.Constants;
import org.jdom2.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Internal class for monitoring defaults configuration
 */
public class MonitoringDefaultsConfiguration extends AbstractConfiguration {

    protected static final List<String> SERVER_ATTRIBUTE_NAMES = new ArrayList<>(Arrays.asList("maxRetries", "pollingPeriod", "timeout", "connectionLostThreshold", "ipv6"));
    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(SERVER_ATTRIBUTE_NAMES);

    // Accessed by introspection
    protected static final Integer DEFAULT_MAX_RETRIES = Constants.DEFAULT_MAX_RETRIES;
    protected static final Integer DEFAULT_POLLING_PERIOD = Constants.DEFAULT_POLLING_PERIOD;
    protected static final Integer DEFAULT_TIMEOUT = Constants.DEFAULT_TIMEOUT;
    protected static final Integer DEFAULT_CONNECTION_LOST_THRESHOLD = Constants.DEFAULT_CONNECTION_LOST_THRESHOLD;
    protected static final Boolean DEFAULT_IPV6 = false;

    protected Integer maxRetries;
    protected Integer pollingPeriod;
    protected Integer timeout;
    protected Integer connectionLostThreshold;
    protected Boolean ipv6;

    // SETTERS

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

    public MonitoringDefaultsConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes (ignore element on error)
        super("monitoringDefaultsConfiguration", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, false);

    }

    public MonitoringDefaultsConfiguration(Configuration aInConfiguration)  {

        // Add attributes (ignore element on error)
        super("monitoringDefaultsConfiguration", aInConfiguration, "defaults", ATTRIBUTE_NAMES);

    }

}
