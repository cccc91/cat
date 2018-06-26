package cclerc.cat.Configuration;

import cclerc.services.Constants;
import org.jdom2.Element;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Internal class for global monitoring configuration
 */
public class GlobalMonitoringConfiguration extends AbstractConfiguration {

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(Arrays.asList("pollingPeriod", "meanTimeBetweenTwoConnectionsLostThreshold1",
                                                                                      "meanTimeBetweenTwoConnectionsLostThreshold2", "meanTimeBetweenTwoConnectionsLostThreshold3",
                                                                                      "connectionsLostForgetTime"));

    // Accessed by introspection
    protected static final Integer DEFAULT_POLLING_PERIOD = Constants.DEFAULT_GLOBAL_MONITORING_POLLING_PERIOD;
    protected static final Long DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD1 = Constants.DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD1;
    protected static final Long DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD2 = Constants.DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD2;
    protected static final Long DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD3 = Constants.DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD3;
    protected static final Long DEFAULT_CONNECTIONS_LOST_FORGET_TIME = Constants.DEFAULT_CONNECTIONS_LOST_FORGET_TIME;


    protected Integer pollingPeriod;
    protected Long meanTimeBetweenTwoConnectionsLostThreshold1;
    protected Long meanTimeBetweenTwoConnectionsLostThreshold2;
    protected Long meanTimeBetweenTwoConnectionsLostThreshold3;
    protected Long connectionsLostForgetTime;

    // SETTERS

    public void setPollingPeriod(String aInPollingPeriod) throws NumberFormatException {
        if ((aInPollingPeriod != null) && !Integer.valueOf(aInPollingPeriod).equals(pollingPeriod)) {
            if (Integer.valueOf(aInPollingPeriod) > Constants.MAX_POLLING_PERIOD || Integer.valueOf(aInPollingPeriod) < Constants.MIN_POLLING_PERIOD)
                throw new NumberFormatException();
            pollingPeriod = Integer.valueOf(aInPollingPeriod);
        }
    }

    public void setMeanTimeBetweenTwoConnectionsLostThreshold1(String aInThreshold) throws NumberFormatException {
        if ((aInThreshold != null) && !Long.valueOf(aInThreshold).equals(meanTimeBetweenTwoConnectionsLostThreshold1)) {
            meanTimeBetweenTwoConnectionsLostThreshold1 = Long.valueOf(aInThreshold);
        }
    }

    public void setMeanTimeBetweenTwoConnectionsLostThreshold2(String aInThreshold) throws NumberFormatException {
        if ((aInThreshold != null) && !Long.valueOf(aInThreshold).equals(meanTimeBetweenTwoConnectionsLostThreshold2)) {
            meanTimeBetweenTwoConnectionsLostThreshold2 = Long.valueOf(aInThreshold);
        }
    }

    public void setMeanTimeBetweenTwoConnectionsLostThreshold3(String aInThreshold) throws NumberFormatException {
        if ((aInThreshold != null) && !Long.valueOf(aInThreshold).equals(meanTimeBetweenTwoConnectionsLostThreshold3)) {
            meanTimeBetweenTwoConnectionsLostThreshold3 = Long.valueOf(aInThreshold);
        }
    }

    public void setConnectionsLostForgetTime(String aInConnectionsLostForgetTime) throws NumberFormatException {
        if ((aInConnectionsLostForgetTime != null) && !Long.valueOf(aInConnectionsLostForgetTime).equals(connectionsLostForgetTime)) {
            connectionsLostForgetTime = Long.valueOf(aInConnectionsLostForgetTime);
        }
    }

    // GETTERS

    public Integer getPollingPeriod() {
        return pollingPeriod;
    }

    public Long getMeanTimeBetweenTwoConnectionsLostThreshold1() {
        return meanTimeBetweenTwoConnectionsLostThreshold1;
    }

    public Long getMeanTimeBetweenTwoConnectionsLostThreshold2() {
        return meanTimeBetweenTwoConnectionsLostThreshold2;
    }

    public Long getMeanTimeBetweenTwoConnectionsLostThreshold3() {
        return meanTimeBetweenTwoConnectionsLostThreshold3;
    }

    public Long getConnectionsLostForgetTime() {
        return connectionsLostForgetTime;
    }

    // CONSTRUCTORS

    public GlobalMonitoringConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes (ignore element on error)
        super("globalMonitoring", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, false);

    }

    public GlobalMonitoringConfiguration(Configuration aInConfiguration) {
        super("globalMonitoringConfiguration", aInConfiguration, "globalMonitoring", ATTRIBUTE_NAMES);
        pollingPeriod = DEFAULT_POLLING_PERIOD;
        meanTimeBetweenTwoConnectionsLostThreshold1 = DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD1;
        meanTimeBetweenTwoConnectionsLostThreshold2 = DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD2;
        meanTimeBetweenTwoConnectionsLostThreshold3 = DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD3;
        connectionsLostForgetTime = DEFAULT_CONNECTIONS_LOST_FORGET_TIME;
    }

}
