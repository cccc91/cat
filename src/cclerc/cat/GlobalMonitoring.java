package cclerc.cat;

import cclerc.cat.Configuration.Configuration;
import cclerc.cat.Configuration.GlobalMonitoringConfiguration;
import cclerc.cat.model.Alarm;
import cclerc.services.*;
import fr.bmartel.speedtest.SpeedTestReport;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.net.InetAddress;
import java.util.*;
import java.util.List;

/**
 * Singleton class implementing global monitoring
 */
public class GlobalMonitoring {

    class JobDetails {

        private EnumTypes.HostState state;
        private long startMonitoringDate;
        private long lastPingLostDate;
        private long pingsLostCount = 0;
        private Alarm pingLostAlarm;
        private Alarm connectionLostAlarm;

        // CONSTRUCTORS

        public JobDetails(EnumTypes.HostState aInState, long aInStartMonitoringDate) {
            state = aInState;
            startMonitoringDate = aInStartMonitoringDate;
        }

        // SETTERS

        public void resetStartMonitoringDate() {
            startMonitoringDate = System.currentTimeMillis();
        }

        public void setState(EnumTypes.HostState aInState) {
            state = aInState;
        }

        public void setLastPingLostDate(long aInLastPingLostDate) {
            lastPingLostDate = aInLastPingLostDate;
        }

        public void incrementPingsLostCount() {
            pingsLostCount++;
        }

        public void setPingLostAlarm(Alarm aInAlarm) {
            pingLostAlarm = aInAlarm;
        }

        public void resetPingLostAlarm() {
            pingLostAlarm = null;
        }

        public void setConnectionLostAlarm(Alarm aInAlarm) {
            connectionLostAlarm = aInAlarm;
        }

        public void resetConnectionLostAlarm() {
            connectionLostAlarm = null;
        }

        // GETTERS

        public EnumTypes.HostState getState() {
            return state;
        }

        public long getStartMonitoringDate() {
            return startMonitoringDate;
        }

        public long getLastPingLostDate() {
            return lastPingLostDate;
        }

        public long getPingsLostCount() {
            return pingsLostCount;
        }

        public Alarm getPingLostAlarm() {
            return pingLostAlarm;
        }

        public Alarm getConnectionLostAlarm() {
            return connectionLostAlarm;
        }

        // METHODS

        public void resetConnectionLost() {
            lastPingLostDate = 0;
            pingsLostCount = 0;
        }


    }

    /**
     * Class allowing to simulate lost pings to test unstability alarms
     * Press 1 to lose ping on all wan jobs
     * Press 2 to lose ping on all lan jobs
     * Press 3 to lose ping on all eth jobs
     * Press 4 to lose ping on all wifi jobs
     */
    class Test implements Runnable {

        private boolean running = true;

        @Override
        public void run() {

            // Name thread
            Thread.currentThread().setName("Test Thread");

            // Run the thread
            while (running) {

                Scanner lScanner = new Scanner(System.in);
                int lChoice = lScanner.nextInt();

                switch (lChoice) {

                    case 1:
                        for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
                            if (lMonitoringJob.getAddressType().equals(EnumTypes.AddressType.WAN)) {
                                changeJobState(lMonitoringJob, EnumTypes.HostState.PING_LOST);
                            }
                        }
                        break;
                    case 2:
                        for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
                            if (lMonitoringJob.getAddressType().equals(EnumTypes.AddressType.LAN)) {
                                changeJobState(lMonitoringJob, EnumTypes.HostState.PING_LOST);
                            }
                        }
                        break;
                    case 3:
                        for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
                            if (lMonitoringJob.getInterfaceType().equals(EnumTypes.InterfaceType.ETH)) {
                                changeJobState(lMonitoringJob, EnumTypes.HostState.PING_LOST);
                            }
                        }
                        break;
                    case 4:
                        for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
                            if (lMonitoringJob.getInterfaceType().equals(EnumTypes.InterfaceType.WIFI)) {
                                changeJobState(lMonitoringJob, EnumTypes.HostState.PING_LOST);
                            }
                        }
                        break;
                    default:
                        break;

                }
                Utilities.sleep(1000);

            }

        }

        public synchronized void terminate() {
            running = false;
        }
    }

    class PeriodicCheck implements Runnable {

        private boolean running = true;
        private HashMap<EnumTypes.ConnectionType, EnumTypes.AlarmId> connectionTypeAlarmId = new HashMap<>();
        {
            connectionTypeAlarmId.put(EnumTypes.ConnectionType.valueOf(EnumTypes.InterfaceType.ETH), EnumTypes.AlarmId.ETHERNET_UNSTABLE);
            connectionTypeAlarmId.put(EnumTypes.ConnectionType.valueOf(EnumTypes.InterfaceType.WIFI), EnumTypes.AlarmId.WIFI_UNSTABLE);
            connectionTypeAlarmId.put(EnumTypes.ConnectionType.valueOf(EnumTypes.AddressType.WAN), EnumTypes.AlarmId.INTERNET_UNSTABLE);
            connectionTypeAlarmId.put(EnumTypes.ConnectionType.valueOf(EnumTypes.AddressType.LAN), EnumTypes.AlarmId.LAN_UNSTABLE);
        }
        private HashMap<EnumTypes.ConnectionType, Alarm> connectionTypeUnstableAlarm = new HashMap<>();
        private Alarm networkUnstableAlarm;

        /**
         * Sends email when an alarm starts or ends
         * @param aInConnectionType  Type of alert
         * @param aInAlarm           AlarmConfiguration that starts or ends
         * @param aInStart           true if the alarm starts, false if it ends
         */
        private void sendMail(EnumTypes.ConnectionType aInConnectionType, Alarm aInAlarm, boolean aInStart) {

            Email email = new Email((!displayGraphicalInterface || cat.getController().isButtonGeneralEmailEnabled()),
                                    Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer());

            String lLocalHostName = "";
            try {
                lLocalHostName = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                Display.logUnexpectedError(e);
            }

            String lMessageType = ((aInStart) ? "start" : "end") + ".unstable." + EnumTypes.ConnectionType.valueOf(aInConnectionType);
            long lRate;
            switch (aInAlarm.getSeverity()) {
                case MAJOR:
                    lRate = Constants.DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD3;
                    break;
                case MINOR:
                    lRate = Constants.DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD2;
                    break;
                case WARNING:
                    lRate = Constants.DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD1;
                    break;
                default:
                    lRate = 0;
                    break;
            };
            lRate = lRate / (60 * 1000);

            Date lStartDate = aInAlarm.getRaiseDate();
            Date lEndDate = new Date();
            long lDuration = lEndDate.toInstant().toEpochMilli() - lStartDate.toInstant().toEpochMilli();
            email.sendMail(
                    String.format(Display.getMessagesResourceBundle().getString("generalEmail." +  lMessageType + ".subject"),
                            lLocalHostName,
                            aInConnectionType,
                            (aInStart) ? String.valueOf(aInAlarm.getSeverity()).toLowerCase() : Utilities.formatDuration(lDuration, 0)),
                    String.format(Display.getMessagesResourceBundle().getString("generalEmail." +  lMessageType + ".content"),
                            (aInStart) ? lRate : Utilities.formatDuration(lDuration, 0)));

        }

        @Override
        public void run() {

            // Name thread
            Thread.currentThread().setName("Global monitoring Thread");

            // Wait for cat end of initialization
            while (cat == null || cat.isInitializationInProgress()) {
                Utilities.sleep(1000);
            }

            speedTest = buildSpeedTest();
            speedTest.startDownloadRepeat("http://intuxication.lafibre.info/speedtest/random4000x4000.jpg");
            speedTest.startUploadRepeat("http://intuxication.lafibre.info/speedtest/upload.php");

            // Run the thread
            while (running) {

                // Initializations
                long lNow = System.currentTimeMillis();
                GlobalMonitoringConfiguration lConfiguration = Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration();

                HashMap<EnumTypes.AddressType, Double> lStatsPerAddressType = new HashMap<>();
                HashMap<EnumTypes.InterfaceType, Double> lStatsPerInterfaceType = new HashMap<>();
                Double lNetworkStats = 0.0;

                // Parse all monitoring jobs
                for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {

                    // Check job details
                    JobDetails lJobDetails = monitoringJobStates.get(lMonitoringJob);

                    // If no connection is lost during configurable period, reset the counters
                    if (lJobDetails.getLastPingLostDate() != 0 &&
                        (lNow - lJobDetails.getLastPingLostDate() >=  lConfiguration.getConnectionsLostForgetTime())) {
                        lJobDetails.resetConnectionLost();
                    }

                    // Compute mean time between 2 connections lost. First lost connection, or unreachable host don't trigger alarms
                    double lMeanTimeBetweenTwoConnectionsLost =
                            (lJobDetails.pingsLostCount > 1)
                            ? (lNow - lJobDetails.startMonitoringDate) / lJobDetails.pingsLostCount
                            : Double.MAX_VALUE;

                    // For each connection type, store the max mean time (the most favorable case is considered)
                    if (!lStatsPerAddressType.containsKey(lMonitoringJob.getAddressType()) ||
                        lMeanTimeBetweenTwoConnectionsLost > lStatsPerAddressType.get(lMonitoringJob.getAddressType())) {
                        lStatsPerAddressType.put(lMonitoringJob.getAddressType(), lMeanTimeBetweenTwoConnectionsLost);
                    }
                    if (!lStatsPerInterfaceType.containsKey(lMonitoringJob.getInterfaceType()) ||
                        lMeanTimeBetweenTwoConnectionsLost > lStatsPerInterfaceType.get(lMonitoringJob.getInterfaceType())) {
                        lStatsPerInterfaceType.put(lMonitoringJob.getInterfaceType(), lMeanTimeBetweenTwoConnectionsLost);
                    }
                    if (lMeanTimeBetweenTwoConnectionsLost > lNetworkStats) {
                        lNetworkStats = lMeanTimeBetweenTwoConnectionsLost;
                    }

                }
                lNetworkStats = (lNetworkStats == 0.0) ? Double.MAX_VALUE : lNetworkStats;

                // Check unstable address types
                for (EnumTypes.AddressType lAddressType: lStatsPerAddressType.keySet()) {

                    EnumTypes.ConnectionType lConnectionType = EnumTypes.ConnectionType.valueOf(lAddressType);
                    if ((lStatsPerAddressType.get(lAddressType) == Double.MAX_VALUE && connectionTypeUnstableAlarm.get(lConnectionType) != null) ||
                        lNetworkStats != Double.MAX_VALUE) {
                        clearAlarm(connectionTypeUnstableAlarm.get(lConnectionType),
                                   Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.unstableAlarm"));
                        connectionTypeUnstableAlarm.put(lConnectionType, null);
                    } else {
                        // Raise unstable alarm for current connection type only if the whole network is not declared unstable
                        if (lNetworkStats == Double.MAX_VALUE &&
                            lStatsPerAddressType.get(lAddressType) != Double.MAX_VALUE &&
                            lStatsPerAddressType.get(lAddressType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold1()) {
                            connectionTypeUnstableAlarm.computeIfAbsent(lConnectionType, t -> raiseAlarm(connectionTypeAlarmId.get(t)));
                            if (lStatsPerAddressType.get(lAddressType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold3()) {
                                if (!connectionTypeUnstableAlarm.get(lConnectionType).getSeverity().equals(EnumTypes.AlarmSeverity.MAJOR)) {
                                    changeSeverity(connectionTypeUnstableAlarm.get(lConnectionType), EnumTypes.AlarmSeverity.MAJOR);
                                    if (displayGraphicalInterface) cat.getController().refreshActiveAlarmsListAndRemoveSelection();
                                    sendMail(lConnectionType, connectionTypeUnstableAlarm.get(lConnectionType), true);
                                }
                            } else if (lStatsPerAddressType.get(lAddressType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold2()) {
                                if (!connectionTypeUnstableAlarm.get(lConnectionType).getSeverity().equals(EnumTypes.AlarmSeverity.MINOR)) {
                                    changeSeverity(connectionTypeUnstableAlarm.get(lConnectionType), EnumTypes.AlarmSeverity.MINOR);
                                    if (displayGraphicalInterface) cat.getController().refreshActiveAlarmsListAndRemoveSelection();
                                    sendMail(lConnectionType, connectionTypeUnstableAlarm.get(lConnectionType), true);
                                }
                            } else if (lStatsPerAddressType.get(lAddressType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold1()) {
                                if (!connectionTypeUnstableAlarm.get(lConnectionType).getSeverity().equals(EnumTypes.AlarmSeverity.WARNING)) {
                                    changeSeverity(connectionTypeUnstableAlarm.get(lConnectionType), EnumTypes.AlarmSeverity.WARNING);
                                    if (displayGraphicalInterface) cat.getController().refreshActiveAlarmsListAndRemoveSelection();
                                    sendMail(lConnectionType, connectionTypeUnstableAlarm.get(lConnectionType), true);
                                }
                            }
                        } else {
                            if (connectionTypeUnstableAlarm.get(lConnectionType) != null) {
                                sendMail(lConnectionType, connectionTypeUnstableAlarm.get(lConnectionType), false);
                                clearAlarm(connectionTypeUnstableAlarm.get(lConnectionType), Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.unstableAlarm"));
                                connectionTypeUnstableAlarm.put(lConnectionType, null);
                            }
                        }
                    }

                }

                // Check unstable interface types
                for (EnumTypes.InterfaceType lInterfaceType: lStatsPerInterfaceType.keySet()) {

                    EnumTypes.ConnectionType lConnectionType = EnumTypes.ConnectionType.valueOf(lInterfaceType);
                    if ((lStatsPerInterfaceType.get(lInterfaceType) == Double.MAX_VALUE && connectionTypeUnstableAlarm.get(lConnectionType) != null) ||
                        lNetworkStats != Double.MAX_VALUE ) {
                        clearAlarm(connectionTypeUnstableAlarm.get(lConnectionType),  Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.unstableAlarm"));
                        connectionTypeUnstableAlarm.put(lConnectionType, null);
                    } else {
                        // Raise unstable alarm for current connection type only if the whole network is not declared unstable
                        if (lNetworkStats == Double.MAX_VALUE &&
                            lStatsPerInterfaceType.get(lInterfaceType) != Double.MAX_VALUE &&
                            lStatsPerInterfaceType.get(lInterfaceType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold1()) {
                            connectionTypeUnstableAlarm.computeIfAbsent(lConnectionType, t -> raiseAlarm(connectionTypeAlarmId.get(t)));
                            if (lStatsPerInterfaceType.get(lInterfaceType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold3()) {
                                if (!connectionTypeUnstableAlarm.get(lConnectionType).getSeverity().equals(EnumTypes.AlarmSeverity.MAJOR)) {
                                    changeSeverity(connectionTypeUnstableAlarm.get(lConnectionType), EnumTypes.AlarmSeverity.MAJOR);
                                    if (displayGraphicalInterface) cat.getController().refreshActiveAlarmsListAndRemoveSelection();
                                    sendMail(lConnectionType, connectionTypeUnstableAlarm.get(lConnectionType), true);
                                }
                            } else if (lStatsPerInterfaceType.get(lInterfaceType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold2()) {
                                if (!connectionTypeUnstableAlarm.get(lConnectionType).getSeverity().equals(EnumTypes.AlarmSeverity.MINOR)) {
                                    changeSeverity(connectionTypeUnstableAlarm.get(lConnectionType), EnumTypes.AlarmSeverity.MINOR);
                                    if (displayGraphicalInterface) cat.getController().refreshActiveAlarmsListAndRemoveSelection();
                                    sendMail(lConnectionType, connectionTypeUnstableAlarm.get(lConnectionType), true);
                                }
                            } else if (lStatsPerInterfaceType.get(lInterfaceType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold1()) {
                                if (!connectionTypeUnstableAlarm.get(lConnectionType).getSeverity().equals(EnumTypes.AlarmSeverity.WARNING)) {
                                    changeSeverity(connectionTypeUnstableAlarm.get(lConnectionType), EnumTypes.AlarmSeverity.WARNING);
                                    if (displayGraphicalInterface) cat.getController().refreshActiveAlarmsListAndRemoveSelection();
                                    sendMail(lConnectionType, connectionTypeUnstableAlarm.get(lConnectionType), true);
                                }
                            }
                        } else {
                            if (connectionTypeUnstableAlarm.get(lConnectionType) != null) {
                                sendMail(lConnectionType, connectionTypeUnstableAlarm.get(lConnectionType), false);
                                clearAlarm(connectionTypeUnstableAlarm.get(lConnectionType),
                                           Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.unstableAlarm"));
                                connectionTypeUnstableAlarm.put(lConnectionType, null);
                            }
                        }
                    }

                }

                // Check unstable network
                // Raise unstable alarm for current connection type only if the whole network is not declared unstable
                if (lNetworkStats  <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold1()) {

                    if (networkUnstableAlarm == null) {
                        networkUnstableAlarm = raiseAlarm(EnumTypes.AlarmId.NETWORK_UNSTABLE);
                    }
                    if (lNetworkStats <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold3()) {
                        if (!networkUnstableAlarm.getSeverity().equals(EnumTypes.AlarmSeverity.MAJOR)) {
                            networkUnstableAlarm.changeSeverity(EnumTypes.AlarmSeverity.MAJOR);
                            if (displayGraphicalInterface) cat.getController().refreshActiveAlarmsListAndRemoveSelection();
                        }
                    } else if (lNetworkStats <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold2()) {
                        if (!networkUnstableAlarm.getSeverity().equals(EnumTypes.AlarmSeverity.MINOR)) {
                            networkUnstableAlarm.changeSeverity(EnumTypes.AlarmSeverity.MINOR);
                            if (displayGraphicalInterface) cat.getController().refreshActiveAlarmsListAndRemoveSelection();
                        }
                    } else if (lNetworkStats <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold1()) {
                        if (!networkUnstableAlarm.getSeverity().equals(EnumTypes.AlarmSeverity.WARNING)) {
                            networkUnstableAlarm.changeSeverity(EnumTypes.AlarmSeverity.WARNING);
                            if (displayGraphicalInterface) cat.getController().refreshActiveAlarmsListAndRemoveSelection();
                        }
                    }

                } else {
                    clearAlarm(networkUnstableAlarm,  Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.unstableAlarm"));
                    networkUnstableAlarm = null;
                }

                Utilities.sleep(lConfiguration.getPollingPeriod());

            }

        }

        public synchronized void terminate() {
            running = false;
        }
    }

    // Class variables
    private static Cat cat;
    private static boolean displayGraphicalInterface;

    // Global monitoring instance
    private static GlobalMonitoring globalMonitoringInstance = new GlobalMonitoring();

    // Monitoring jobs information
    private HashMap<MonitoringJob, JobDetails> monitoringJobStates = new HashMap<>();

    private boolean wifiMonitored = false;
    private boolean ethernetMonitored = false;
    private boolean wanMonitored = false;
    private boolean lanMonitored = false;

    private Alarm ethernetDownAlarm;
    private Alarm wifiDownAlarm;
    private Alarm wanDownAlarm;
    private Alarm lanDownAlarm;
    private Alarm networkDownAlarm;

    private volatile ObservableList<Alarm> activeAlarmsList = FXCollections.observableArrayList();
    private volatile ObservableList<Alarm> historicalAlarmsList = FXCollections.observableArrayList();

    // Speed test
    SpeedTest speedTest;

    // SINGLETON

    /**
     * Returns the singleton
     * @return Global monitoring singleton instance
     */
    public static GlobalMonitoring getInstance() {
        return globalMonitoringInstance;
    }

    // CONSTRUCTOR

    private GlobalMonitoring() {

        // Constants
        final boolean TEST_ENABLE = false;

        // Launch periodic check
        Thread lThread = new Thread(new PeriodicCheck());
        lThread.start();
        // For test only - to be disabled when not useful
        if (TEST_ENABLE) {
            Thread lThreadTest = new Thread(new Test());
            lThreadTest.start();
        }

    }

    // GETTERS

    public ObservableList<Alarm> getActiveAlarmsList() {
        return activeAlarmsList;
    }

    public ObservableList<Alarm> getHistoricalAlarmsList() {
        return historicalAlarmsList;
    }

    // SETTERS

    /**
     * Sets display of graphical interface flag
     *
     * @param aInDisplayGraphicalInterface Display of graphical interface flag
     */
    public static void setDisplayGraphicalInterface(boolean aInDisplayGraphicalInterface) {
        displayGraphicalInterface = aInDisplayGraphicalInterface;
    }

    /**
     * Sets a back reference to Cat main class
     * @param aInCat Cat main class
     */
    public static void setCat(Cat aInCat) {
        cat = aInCat;
    }

    // METHODS

    /**
     * Add a monitoring job
     * @param aInMonitoringJob Monitoring job to add
     */
    public synchronized void addMonitoringJob(MonitoringJob aInMonitoringJob) {

        monitoringJobStates.put(aInMonitoringJob, new JobDetails(EnumTypes.HostState.REACHABLE, System.currentTimeMillis()));

        // Memorize the type of interface and address that are monitored
        if (aInMonitoringJob.getAddressType().equals(EnumTypes.AddressType.WAN)) {
            wanMonitored = true;
        } else {
            lanMonitored = true;
        }
        if (aInMonitoringJob.getInterfaceType().equals(EnumTypes.InterfaceType.ETH)) {
            ethernetMonitored = true;
        } else {
            wifiMonitored = true;
        }

        // Display
        if (displayGraphicalInterface) {
            cat.getController().setInterfaceTypeImageView(aInMonitoringJob.getInterfaceType(), true);
            cat.getController().setAddressTypeStateImageView(aInMonitoringJob.getAddressType(), true);
            cat.getController().setGlobalStateImageView(true);
        }

    }

    /**
     * Removes a monitoring job
     * @param aInMonitoringJob Monitoring job to remove
     */
    public synchronized void removeMonitoringJob(MonitoringJob aInMonitoringJob) {

        monitoringJobStates.remove(aInMonitoringJob);

        wanMonitored = false; lanMonitored = false;
        ethernetMonitored = false; wifiMonitored = false;
        for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
            if (lMonitoringJob.getAddressType().equals(EnumTypes.AddressType.WAN)) {
                wanMonitored = true;
            } else {
                lanMonitored = true;
            }
            if (lMonitoringJob.getInterfaceType().equals(EnumTypes.InterfaceType.ETH)) {
                ethernetMonitored = true;
            } else {
                wifiMonitored = true;
            }
        }

    }

    /**
     * Gets the current state of a monitoring job
     * @param aInMonitoringJob Monitoring job
     * @return Current monitoring job state
     */
    public EnumTypes.HostState getJobState(MonitoringJob aInMonitoringJob) {
        return monitoringJobStates.get(aInMonitoringJob).getState();
    }

    /**
     * Changes monitoring job state
     * @param aInMonitoringJob Monitoring job
     * @param aInState         New state
     */
    public synchronized void changeJobState(MonitoringJob aInMonitoringJob, EnumTypes.HostState aInState) {

        // Memorize the new state and state change time
        JobDetails lJobDetails = monitoringJobStates.get(aInMonitoringJob);
        lJobDetails.setState(aInState);

        // Raise or clear alarm on the job itself depending on the state
        switch (aInState) {

            case PING_LOST:
                lJobDetails.setLastPingLostDate(System.currentTimeMillis());
                lJobDetails.incrementPingsLostCount();
                if (lJobDetails.getPingsLostCount() == 1) lJobDetails.resetStartMonitoringDate();
                lJobDetails.setPingLostAlarm(raiseAlarm(EnumTypes.AlarmId.PING_LOST, aInMonitoringJob.getRemoteHostname(), String.valueOf(aInMonitoringJob.getInterfaceType())));
                break;
            case UNREACHABLE:
                lJobDetails.setConnectionLostAlarm(raiseAlarm(EnumTypes.AlarmId.CONNECTION_LOST, aInMonitoringJob.getRemoteHostname(), String.valueOf(aInMonitoringJob.getInterfaceType())));
                clearAlarm(lJobDetails.getPingLostAlarm(), String.format(
                        Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.siteLost"),
                        lJobDetails.getConnectionLostAlarm().getName(), lJobDetails.getConnectionLostAlarm().getId()));
                break;
            case REACHABLE:
                clearAlarm(lJobDetails.getConnectionLostAlarm(), Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.siteRecovered"));
                break;
        }

        // Raise alarm on the address type of the current job if all other jobs of the same type are down
        boolean lAllAddressTypesHaveSameState = true;
        for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
            if (lMonitoringJob.getAddressType().equals(aInMonitoringJob.getAddressType()) &&
                    !monitoringJobStates.get(lMonitoringJob).getState().equals(lJobDetails.getState())) {
                lAllAddressTypesHaveSameState = false;
                break;
            }
        }

        if (lAllAddressTypesHaveSameState && aInState == EnumTypes.HostState.UNREACHABLE) {
            if (displayGraphicalInterface) cat.getController().setAddressTypeStateImageView(aInMonitoringJob.getAddressType(), false);
            Alarm lCurrentAlarm;
            if (aInMonitoringJob.getAddressType().equals(EnumTypes.AddressType.WAN)) {
                wanDownAlarm = raiseAlarm(EnumTypes.AlarmId.INTERNET_DOWN, null, String.valueOf(aInMonitoringJob.getAddressType()));
                lCurrentAlarm = wanDownAlarm;
            } else {
                lanDownAlarm = raiseAlarm(EnumTypes.AlarmId.LAN_DOWN, null, String.valueOf(aInMonitoringJob.getAddressType()));
                lCurrentAlarm = lanDownAlarm;
            }
            // Clear connection lost alarm on jobs of the same address type
            for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
                if (lMonitoringJob.getAddressType().equals(aInMonitoringJob.getAddressType())) {
                    Alarm lAlarm = monitoringJobStates.get(lMonitoringJob).getConnectionLostAlarm();
                    clearAlarm(lAlarm, String.format(
                            Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.interfaceOrAddressTypeDown"),
                            lCurrentAlarm.getName(), lCurrentAlarm.getId(), lMonitoringJob.getAddressType().toString()));
                }
            }
        }

        // Raise alarm on the interface type of the current job if all other jobs of the same type are down
        boolean lAllInterfaceTypesHaveSameState = true;
        for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
            if (lMonitoringJob.getInterfaceType().equals(aInMonitoringJob.getInterfaceType()) &&
                    !monitoringJobStates.get(lMonitoringJob).getState().equals(lJobDetails.getState())) {
                lAllInterfaceTypesHaveSameState = false;
                break;
            }
        }

        if (lAllInterfaceTypesHaveSameState && aInState == EnumTypes.HostState.UNREACHABLE) {
            if (displayGraphicalInterface) cat.getController().setInterfaceTypeImageView(aInMonitoringJob.getInterfaceType(), false);
            Alarm lCurrentAlarm;
            if (aInMonitoringJob.getInterfaceType().equals(EnumTypes.InterfaceType.ETH)) {
                ethernetDownAlarm = raiseAlarm(EnumTypes.AlarmId.ETHERNET_DOWN, null, String.valueOf(aInMonitoringJob.getInterfaceType()));
                lCurrentAlarm = ethernetDownAlarm;
            } else {
                wifiDownAlarm = raiseAlarm(EnumTypes.AlarmId.WIFI_DOWN, null, String.valueOf(aInMonitoringJob.getInterfaceType()));
                lCurrentAlarm = wifiDownAlarm;
            }
            // Clear connection lost alarm on jobs of the same interface type
            for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
                if (lMonitoringJob.getInterfaceType().equals(aInMonitoringJob.getInterfaceType())) {
                    Alarm lAlarm = monitoringJobStates.get(lMonitoringJob).getConnectionLostAlarm();
                    clearAlarm(lAlarm, String.format(
                            Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.interfaceOrAddressTypeDown"),
                            lCurrentAlarm.getName(), lCurrentAlarm.getId(), lMonitoringJob.getInterfaceType().toString()));
                }
            }
        }

        // Raise alarm on network when everything is down
        boolean lAllDown = true;
        for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
            if (!monitoringJobStates.get(lMonitoringJob).getState().equals(EnumTypes.HostState.UNREACHABLE)) {
                lAllDown = false;
                break;
            }
        }

        if (lAllDown) {
//        if (lAllDown && wifiMonitored && ethernetMonitored && lanMonitored && wanMonitored) {

            // Raise network down alarm
            if (displayGraphicalInterface) cat.getController().setGlobalStateImageView(false);
            networkDownAlarm = raiseAlarm(EnumTypes.AlarmId.NETWORK_DOWN);

            // Clear connection lost alarm on jobs of the same interface type
            for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
                clearAlarm(monitoringJobStates.get(lMonitoringJob).getConnectionLostAlarm(),
                        String.format(Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.networkDown"), networkDownAlarm.getName(), networkDownAlarm.getId()));
            }

            // Clear interface and address type down alarms
            clearAlarm(ethernetDownAlarm,
                    String.format(Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.networkDown"), networkDownAlarm.getName(), networkDownAlarm.getId()));

            clearAlarm(wifiDownAlarm,
                    String.format(Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.networkDown"), networkDownAlarm.getName(), networkDownAlarm.getId()));

            clearAlarm(wanDownAlarm,
                    String.format(Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.networkDown"), networkDownAlarm.getName(), networkDownAlarm.getId()));

            clearAlarm(lanDownAlarm,
                    String.format(Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.networkDown"), networkDownAlarm.getName(), networkDownAlarm.getId()));

        }

        // If the site becomes reachable, clear any alarm raised on the interface/address type of this site
        if (aInState == EnumTypes.HostState.REACHABLE) {

            if (displayGraphicalInterface) cat.getController().setGlobalStateImageView(true);
            if (networkDownAlarm != null) {
                sendMail("network", networkDownAlarm);
                clearAlarm(networkDownAlarm, Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.siteRecovered.networkDown"));
            }

            if (displayGraphicalInterface) cat.getController().setInterfaceTypeImageView(aInMonitoringJob.getInterfaceType(), true);
            if (aInMonitoringJob.getInterfaceType().equals(EnumTypes.InterfaceType.ETH)) {
                if (ethernetDownAlarm != null) {
                    sendMail("ethernet", ethernetDownAlarm);
                    clearAlarm(ethernetDownAlarm,
                            String.format(Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.siteRecovered.interfaceOrAddressTypeDown"), aInMonitoringJob.getInterfaceType()));
                }
            } else {
                if (wifiDownAlarm != null) {
                    sendMail("wifi", wifiDownAlarm);
                    clearAlarm(wifiDownAlarm,
                            String.format(Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.siteRecovered.interfaceOrAddressTypeDown"), aInMonitoringJob.getInterfaceType()));
                }
            }
            if (displayGraphicalInterface) cat.getController().setAddressTypeStateImageView(aInMonitoringJob.getAddressType(), true);
            if (aInMonitoringJob.getAddressType().equals(EnumTypes.AddressType.WAN)) {
                if (wanDownAlarm != null) {
                    sendMail("wan", wanDownAlarm);
                    clearAlarm(wanDownAlarm,
                            String.format(Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.siteRecovered.interfaceOrAddressTypeDown"), aInMonitoringJob.getAddressType()));
                }
            } else {
                if (lanDownAlarm != null) {
                    sendMail("lan", lanDownAlarm);
                    clearAlarm(lanDownAlarm,
                            String.format(Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.siteRecovered.interfaceOrAddressTypeDown"), aInMonitoringJob.getAddressType()));
                }
            }

            // Force all not reachable jobs to notify their state
            for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
                if (monitoringJobStates.get(lMonitoringJob).getState().equals(EnumTypes.HostState.UNREACHABLE)) {
                    lMonitoringJob.forceNotifyCurrentJobState();
                }
            }

        }

    }

    /**
     * Sends email when an alarm ends
     * @param aInType  Type of alert
     * @param aInAlarm AlarmConfiguration that ends
     */
    private void sendMail(String aInType, Alarm aInAlarm) {

        Email email = new Email((!displayGraphicalInterface || cat.getController().isButtonGeneralEmailEnabled()) &&
                                Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations().size() != 0 &&
                                !Configuration.getCurrentConfiguration().getEmailConfiguration().getRecipientList().isEmpty(),
                                Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer());

        String lLocalHostName = "";
        try {
            lLocalHostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }

        Date lStartDate = aInAlarm.getRaiseDate();
        Date lEndDate = new Date();
        long lDuration = lEndDate.toInstant().toEpochMilli() - lStartDate.toInstant().toEpochMilli();
        email.sendMail(
                String.format(Display.getMessagesResourceBundle().getString("generalEmail." +  aInType + ".subject"),
                              lLocalHostName,
                              Utilities.formatDuration(lDuration, 0)),
                String.format(Display.getMessagesResourceBundle().getString("generalEmail." +  aInType + ".content"),
                        LocaleUtilities.getInstance().getDateFormat().format(lStartDate),
                        LocaleUtilities.getInstance().getTimeFormat().format(lStartDate.getTime()),
                        LocaleUtilities.getInstance().getDateFormat().format(lEndDate),
                        LocaleUtilities.getInstance().getTimeFormat().format(lEndDate.getTime()),
                        Utilities.formatDuration(lDuration, 0)));

    }

    /**
     * Raises an alarm:
     * - creates the alarm in active alarms if the alarm is not present
     * - increments occurrences if the alarm already exists in active alarms
     * @param aInAlarmId                Id of ths alarm to create
     * @param aInSite                   Site to which the alarm is related. Can be null
     * @param aInObjectName             Name of the object on which the alarm is raised. Can be null
     * @return Raised alarm
     */
    public synchronized Alarm raiseAlarm(EnumTypes.AlarmId aInAlarmId, String aInSite, String aInObjectName) {

        // Check if the alarm exists
        for (Alarm lAlarm : activeAlarmsList) {

            // Increment occurrences if an existing alarm matches the criteria
            if (lAlarm.getId() == aInAlarmId.getValue() &&
                    ((aInSite == null && lAlarm.getSite() == null) || (aInSite != null && aInSite.equals(lAlarm.getSite()))) &&
                    ((aInObjectName == null && lAlarm.getObjectName() == null) || (aInObjectName != null && aInObjectName.equals(lAlarm.getObjectName())))) {
                lAlarm.incrementOccurrences();
                String lMessage = String.format(
                        Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.raise.incrementOccurrences"),
                        lAlarm.getOccurrences(),
                        lAlarm.getSeverity().getDisplayedValue(),
                        lAlarm.getName(),
                        (aInSite == null) ?
                        Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noSite") :
                        Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.site") + " " + lAlarm.getSite(),
                        (aInObjectName == null) ?
                        Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noObject") :
                        Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.object") + " " + lAlarm.getObjectName());
                Display.getLogger().info(lMessage);
                cat.getController().printMessage(new Message(lMessage, lAlarm.getSeverity().getMessageLevel()));
                return lAlarm;
            }
        }

        // Create the alarm if no existing alarm matches the criteria
        Alarm lAlarm = new Alarm(aInAlarmId.getValue());
        lAlarm.setSite(aInSite);
        lAlarm.setObjectName(aInObjectName);

        // Check if alarm is filtered
        if (!lAlarm.isFiltered()) {

            activeAlarmsList.add(lAlarm);
            String lMessage = String.format(
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.raise.newAlarm"),
                    lAlarm.getSeverity().getDisplayedValue(),
                    lAlarm.getName(),
                    (aInSite == null) ?
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noSite") :
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.site") + " " + lAlarm.getSite(),
                    (aInObjectName == null) ?
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noObject") :
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.object") + " " + lAlarm.getObjectName());
            Display.getLogger().info(lMessage);
            cat.getController().printMessage(new Message(lMessage, lAlarm.getSeverity().getMessageLevel()));

        }

        return lAlarm;

    }

    /**
     * Raises an alarm without site and object name:
     * - creates the alarm in active alarms if the alarm is not present
     * - increments occurrences if the alarm already exists in active alarms
     * @param aInAlarmId                Id of ths alarm to create
     * @return Raised alarm
     */
    public synchronized Alarm raiseAlarm(EnumTypes.AlarmId aInAlarmId) {
        return raiseAlarm(aInAlarmId, null, null);
    }

    /**
     * Clears an alarm:
     * - moves alarm from active to historical alarms
     * - changes alarm status
     * @param aInAlarm                 AlarmConfiguration to clear
     * @param aInAdditionalInformation Additional information to append to the alarm
     */
    public synchronized void clearAlarm(Alarm aInAlarm, String aInAdditionalInformation) {
        ArrayList<Alarm> lAlarmsList = new ArrayList<>();
        lAlarmsList.add(aInAlarm);
        clearAlarms(lAlarmsList, aInAdditionalInformation);
    }

    /**
     * Clears list of alarms
     * - moves alarms from active to historical alarms
     * - changes alarms status
     * @param aInAlarms                Alarms to clear
     * @param aInAdditionalInformation Additional information to append to the alarms
     */
    public synchronized void clearAlarms(List<Alarm> aInAlarms, String aInAdditionalInformation) {


        for (Alarm lAlarm: aInAlarms) {

            if (lAlarm != null) {

                // Check if alarm is filtered
                if (!lAlarm.isFiltered()) {
                    GlobalMonitoring.getInstance().getActiveAlarmsList().remove(lAlarm);
                    GlobalMonitoring.getInstance().getHistoricalAlarmsList().add(lAlarm);
                }

                // Add clear reason in additional information
                if (aInAdditionalInformation != null) {
                    lAlarm.appendAdditionalInformation(aInAdditionalInformation);
                }

                // Remove references to current alarm
                for (MonitoringJob lMonitoringJob : monitoringJobStates.keySet()) {
                    if (monitoringJobStates.get(lMonitoringJob).getPingLostAlarm() != null && monitoringJobStates.get(lMonitoringJob).getPingLostAlarm().equals(lAlarm)) {
                        monitoringJobStates.get(lMonitoringJob).resetPingLostAlarm();
                    }
                    if (monitoringJobStates.get(lMonitoringJob).getConnectionLostAlarm() != null && monitoringJobStates.get(lMonitoringJob).getConnectionLostAlarm().equals(lAlarm)) {
                        monitoringJobStates.get(lMonitoringJob).resetConnectionLostAlarm();
                    }
                }
                if (lanDownAlarm != null && lanDownAlarm.equals(lAlarm)) {
                    lanDownAlarm = null;
                }
                if (wanDownAlarm != null && wanDownAlarm.equals(lAlarm)) {
                    wanDownAlarm = null;
                }
                if (ethernetDownAlarm != null && ethernetDownAlarm.equals(lAlarm)) {
                    ethernetDownAlarm = null;
                }
                if (wifiDownAlarm != null && wifiDownAlarm.equals(lAlarm)) {
                    wifiDownAlarm = null;
                }
                if (networkDownAlarm != null && networkDownAlarm.equals(lAlarm)) {
                    networkDownAlarm = null;
                }

                // Clear alarm and log
                lAlarm.clear();
                if (!lAlarm.isFiltered()) {
                    String lMessage = String.format(
                            Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.clear"),
                            lAlarm.getName(),
                            (lAlarm.getSite() == null) ?
                            Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noSite") :
                            Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.site") + " " + lAlarm.getSite(),
                            (lAlarm.getObjectName() == null) ?
                            Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noObject") :
                            Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.object") + " " + lAlarm.getObjectName());
                    Display.getLogger().info(lMessage);
                    cat.getController().printMessage(new Message(lMessage, EnumTypes.MessageLevel.OK));
                }

            }
        }
    }

    /**
     * Acknowledges list of alarms
     * @param aInAlarms Alarms to acknowledge
     */
    public synchronized void acknowledgeAlarms(List<Alarm> aInAlarms) {
        Date lDate = new Date();
        for (Alarm lAlarm: aInAlarms) {
            lAlarm.appendAdditionalInformation(String.format(
                    Display.getViewResourceBundle().getString("globalMonitoring.alarms.acknowledge"),
                    System.getProperty("user.name"),
                    LocaleUtilities.getInstance().getDateFormat().format(lDate),
                    LocaleUtilities.getInstance().getTimeFormat().format(lDate.getTime())));
            lAlarm.acknowledge();
            String lMessage = String.format(
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.acknowledge"),
                    lAlarm.getName(),
                    (lAlarm.getSite() == null) ?
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noSite") :
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.site") + " " + lAlarm.getSite(),
                    (lAlarm.getObjectName() == null) ?
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noObject") :
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.object") + " " + lAlarm.getObjectName());
            Display.getLogger().info(lMessage);
            cat.getController().printMessage(new Message(lMessage, EnumTypes.MessageLevel.INFO));
        }
    }

    /**
     * Un-acknowledges list of alarms
     * @param aInAlarms Alarms to un-acknowledge
     */
    public synchronized void unAcknowledgeAlarms(List<Alarm> aInAlarms) {
        Date lDate = new Date();
        for (Alarm lAlarm: aInAlarms) {
            lAlarm.appendAdditionalInformation(String.format(
                    Display.getViewResourceBundle().getString("globalMonitoring.alarms.unAcknowledge"),
                    System.getProperty("user.name"),
                    LocaleUtilities.getInstance().getDateFormat().format(lDate),
                    LocaleUtilities.getInstance().getTimeFormat().format(lDate.getTime())));
            lAlarm.unAcknowledge();
            String lMessage = String.format(
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.unAcknowledge"),
                    lAlarm.getName(),
                    (lAlarm.getSite() == null) ?
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noSite") :
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.site") + " " + lAlarm.getSite(),
                    (lAlarm.getObjectName() == null) ?
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noObject") :
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.object") + " " + lAlarm.getObjectName());
            Display.getLogger().info(lMessage);
            cat.getController().printMessage(new Message(lMessage, EnumTypes.MessageLevel.INFO));
        }
    }

    /**
     * Changes the severity of an alarm
     * @param aInAlarm        Alarm which severity needs to be changed
     * @param aInNewSeverity  New alarm severity
     */
    public void changeSeverity(Alarm aInAlarm, EnumTypes.AlarmSeverity aInNewSeverity) {

        if (aInAlarm.getSeverity() != aInNewSeverity) {

            String lMessage = String.format(
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.raise.changeSeverity"),
                    aInAlarm.getName(),
                    (aInAlarm.getSite() == null) ?
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noSite") :
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.site") + " " + aInAlarm.getSite(),
                    (aInAlarm.getObjectName() == null) ?
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.noObject") :
                    Display.getMessagesResourceBundle().getString("log.globalMonitoring.alarms.object") + " " + aInAlarm.getObjectName(),
                    aInAlarm.getSeverity().getDisplayedValue(), aInNewSeverity.getDisplayedValue());
            Display.getLogger().info(lMessage);
            cat.getController().printMessage(new Message(lMessage, aInNewSeverity.getMessageLevel()));

            aInAlarm.changeSeverity(aInNewSeverity);

        }

    }

    public SpeedTest buildSpeedTest() {
        return new SpeedTest(new SpeedTestInterface() {
            @Override
            public void printMessage(String aInMessage) {
                if (!speedTest.isFirstReport()) {
                    cat.getController().replaceLastMessage(new Message(aInMessage, EnumTypes.MessageLevel.INFO));
                } else {
                    cat.getController().printMessage(new Message(aInMessage, EnumTypes.MessageLevel.INFO));
                }
            }

            @Override
            public void printError(String aInError) {
                cat.getController().printMessage(new Message(aInError, EnumTypes.MessageLevel.ERROR));
            }

            @Override
            public void storeResult(SpeedTestReport report) {

            }

            @Override
            // TODO: compute
            public String getType() {
                return "periodic";
            }
        },  (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN) == null) ? true :
                Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN).getUseProxy());
    }

}
