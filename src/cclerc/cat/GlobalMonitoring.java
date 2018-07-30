package cclerc.cat;

import cclerc.cat.Configuration.Configuration;
import cclerc.cat.Configuration.GlobalMonitoringConfiguration;
import cclerc.cat.model.Alarm;
import cclerc.services.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Singleton class implementing global monitoring
 */
public class GlobalMonitoring {

    class JobDetails {

        private EnumTypes.HostState state;
        private long startMonitoringDate;
        private long lastLostPingDate;
        private long lostPingsCount = 0;
        private Alarm pingLostAlarm;
        private Alarm connectionLostAlarm;

        // Reports
        private long lostPingsSinceLastReport = 0;
        private long lostConnectionsSinceLastReport = 0;

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

        public void setLastLostPingDate(long aInLastPingLostDate) {
            lastLostPingDate = aInLastPingLostDate;
        }

        public void incrementPingsLostCount() {
            lostPingsCount++;
            lostPingsSinceLastReport++;
        }

        public void setPingLostAlarm(Alarm aInAlarm) {
            pingLostAlarm = aInAlarm;
        }

        public void resetPingLostAlarm() {
            pingLostAlarm = null;
        }

        public void setConnectionLostAlarm(Alarm aInAlarm) {
            connectionLostAlarm = aInAlarm;
            lostConnectionsSinceLastReport++;
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

        public long getLastLostPingDate() {
            return lastLostPingDate;
        }

        public long getLostPingsCount() {
            return lostPingsCount;
        }

        public Alarm getPingLostAlarm() {
            return pingLostAlarm;
        }

        public Alarm getConnectionLostAlarm() {
            return connectionLostAlarm;
        }

        public long getLostPingsSinceLastReport() {
            return lostPingsSinceLastReport;
        }

        public long getLostConnectionsSinceLastReport() {
            return lostConnectionsSinceLastReport;
        }

        // METHODS

        public void resetConnectionLost() {
            lastLostPingDate = 0;
            lostPingsCount = 0;
        }

        public void resetReport() {
            lostPingsSinceLastReport = 0;
            lostConnectionsSinceLastReport = 0;
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

            Email email = new Email((!Cat.getInstance().displayGraphicalInterface() || Cat.getInstance().getController().isButtonGeneralEmailEnabled()),
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
            while (Cat.getInstance() == null || Cat.getInstance().isInitializationInProgress()) {
                Utilities.sleep(1000);
            }

            // Retrieve information from CSS
            InputStream lCssInputStream = getClass().getResourceAsStream("/resources/css/view.css");
            BufferedReader lCssBuffer = new BufferedReader(new InputStreamReader(lCssInputStream));
            String lCss = lCssBuffer.lines().collect(Collectors.joining());
            colorStateOk = lCss.replaceAll(".*#state-ok", "").replaceAll("}.*", "").replaceAll(".*-fx-text-fill[^:]*:[ ]*([^;]*);.*", "$1");
            colorStateDegraded = lCss.replaceAll(".*#state-degraded", "").replaceAll("}.*", "").replaceAll(".*-fx-text-fill[^:]*:[ ]*([^;]*);.*", "$1");
            colorStateNok = lCss.replaceAll(".*#state-nok", "").replaceAll("}.*", "").replaceAll(".*-fx-text-fill[^:]*:[ ]*([^;]*);.*", "$1");
            colorAlarmInfo = lCss.replaceAll(".*#state-alarm-info", "").replaceAll("}.*", "").replaceAll(".*-fx-text-fill[^:]*:[ ]*([^;]*);.*", "$1");
            colorAlarmWarning = lCss.replaceAll(".*#state-alarm-warning", "").replaceAll("}.*", "").replaceAll(".*-fx-text-fill[^:]*:[ ]*([^;]*);.*", "$1");
            colorAlarmMinor = lCss.replaceAll(".*#state-alarm-minor", "").replaceAll("}.*", "").replaceAll(".*-fx-text-fill[^:]*:[ ]*([^;]*);.*", "$1");
            colorAlarmMajor = lCss.replaceAll(".*#state-alarm-major", "").replaceAll("}.*", "").replaceAll(".*-fx-text-fill[^:]*:[ ]*([^;]*);.*", "$1");
            colorAlarmCritical = lCss.replaceAll(".*#state-alarm-critical", "").replaceAll("}.*", "").replaceAll(".*-fx-text-fill[^:]*:[ ]*([^;]*);.*", "$1");

            // Load report body template
            InputStream lReportBodyInputStream = getClass().getResourceAsStream("/resources/templates/globalReportBody.html");
            BufferedReader lReportBodyBuffer = new BufferedReader(new InputStreamReader(lReportBodyInputStream));
            reportBodyTemplate = lReportBodyBuffer.lines().collect(Collectors.joining("\n"));

            reportBodyTemplate =
                    reportBodyTemplate.replaceAll("#GLOBAL_TITLE#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.title"))
                                      .replaceAll("#SUMMARY_RESULTS_TITLE#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general"))
                                      .replaceAll("#JOB_RESULTS_TITLE#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.jobs"))
                                      .replaceAll("#NETWORK_STATE_LABEL#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.networkState"))
                                      .replaceAll("#WAN_STATE_LABEL#",
                                                  (wanMonitored) ? Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.wanState") : "")
                                      .replaceAll("#LAN_STATE_LABEL#",
                                                  (lanMonitored) ? Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.lanState") : "")
                                      .replaceAll("#ETH_STATE_LABEL#",
                                                  (ethernetMonitored) ? Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.ethState") : "")
                                      .replaceAll("#WIFI_STATE_LABEL#",
                                                  (wifiMonitored) ? Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.wifiState") : "")
                                      .replaceAll("#LOST_PINGS_COUNT_LABEL#",
                                                  Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.lostPingsCount"))
                                      .replaceAll("#LOST_CONNECTIONS_LABEL#",
                                                  Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.lostConnectionsCount"))
                                      .replaceAll("#ACTIVE_ALARMS_COUNT_LABEL#",
                                                  Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.activeAlarmsCount"))
                                      .replaceAll("#ACTIVE_INFO_ALARMS_COUNT_LABEL#",
                                                  Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.activeInfoAlarmsCount"))
                                      .replaceAll("#ACTIVE_WARNING_ALARMS_COUNT_LABEL#",
                                                  Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.activeWarningAlarmsCount"))
                                      .replaceAll("#ACTIVE_MINOR_ALARMS_COUNT_LABEL#",
                                                  Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.activeMinorAlarmsCount"))
                                      .replaceAll("#ACTIVE_MAJOR_ALARMS_COUNT_LABEL#",
                                                  Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.activeMajorAlarmsCount"))
                                      .replaceAll("#ACTIVE_CRITICAL_ALARMS_COUNT_LABEL#",
                                                  Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.activeCriticalAlarmsCount"))
                                      .replaceAll("#JOB_SERVER_NAME_HEADER#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.jobs.serverName"))
                                      .replaceAll("#JOB_SERVER_IP_HEADER#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.jobs.serverIp"))
                                      .replaceAll("#JOB_SERVER_TYPE_HEADER#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.jobs.serverType"))
                                      .replaceAll("#JOB_INTERFACE_HEADER#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.jobs.interface"))
                                      .replaceAll("#JOB_ADDRESS_TYPE_HEADER#",
                                                  Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.jobs.addressType"))
                                      .replaceAll("#JOB_LOST_PINGS_HEADER#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.jobs.lostPings"))
                                      .replaceAll("#JOB_LOST_CONNECTIONS_HEADER#",
                                                  Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.jobs.lostConnections"))
                                      .replaceAll("#JOB_ROUND_TRIP_HEADER#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.jobs.roundTrip"))
                                      .replaceAll("#COUNT_TYPE#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.countType"));

            // Load report job result template
            InputStream lReportJobResultInputStream = getClass().getResourceAsStream("/resources/templates/globalReportJobResult.html");
            BufferedReader lReportJobResultBuffer = new BufferedReader(new InputStreamReader(lReportJobResultInputStream));
            reportJobResultTemplate = lReportJobResultBuffer.lines().collect(Collectors.joining("\n"));

            // Run the thread
            while (running) {

                // Initializations
                long lNow = System.currentTimeMillis();
                GlobalMonitoringConfiguration lConfiguration = Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration();
                HashMap<EnumTypes.ConnectionType, Double> lStatsPerConnectionType = new HashMap<>();
                Double lNetworkStats = 0.0;

                // Parse all monitoring jobs
                lStatsPerConnectionType.clear();
                for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {

                    // Check job details
                    JobDetails lJobDetails = monitoringJobStates.get(lMonitoringJob);

                    // If no connection is lost during configurable period, reset the counters
                    if (lJobDetails.getLastLostPingDate() != 0 &&
                        (lNow - lJobDetails.getLastLostPingDate() >= lConfiguration.getConnectionsLostForgetTime())) {
                        lJobDetails.resetConnectionLost();
                    }

                    // Compute mean time between 2 connections lost. First lost connection, or unreachable host don't trigger alarms
                    double lMeanTimeBetweenTwoConnectionsLost =
                            (lJobDetails.lostPingsCount > 1)
                            ? (lNow - lJobDetails.startMonitoringDate) / lJobDetails.lostPingsCount
                            : Double.MAX_VALUE;

                    // For each connection type, store the max mean time (the most favorable case among the same connection type jobs is considered)
                    EnumTypes.ConnectionType lAddressType = EnumTypes.ConnectionType.valueOf(lMonitoringJob.getAddressType());
                    if (!lStatsPerConnectionType.containsKey(lAddressType) || lMeanTimeBetweenTwoConnectionsLost > lStatsPerConnectionType.get(lAddressType)) {
                        lStatsPerConnectionType.put(lAddressType, lMeanTimeBetweenTwoConnectionsLost);;
                    }
                    EnumTypes.ConnectionType lInterfaceType = EnumTypes.ConnectionType.valueOf(lMonitoringJob.getInterfaceType());
                    if (!lStatsPerConnectionType.containsKey(lInterfaceType) || lMeanTimeBetweenTwoConnectionsLost > lStatsPerConnectionType.get(lInterfaceType)) {
                        lStatsPerConnectionType.put(lInterfaceType, lMeanTimeBetweenTwoConnectionsLost);
                    }
                    if (lMeanTimeBetweenTwoConnectionsLost > lNetworkStats) {
                        lNetworkStats = lMeanTimeBetweenTwoConnectionsLost;
                    }

                }
                lNetworkStats = (lNetworkStats == 0.0) ? Double.MAX_VALUE : lNetworkStats;

                // Check unstable connection types
                for (EnumTypes.ConnectionType lConnectionType: lStatsPerConnectionType.keySet()) {

                    if (!lStatsPerConnectionType.containsKey(lConnectionType)) lStatsPerConnectionType.put(lConnectionType, Double.MAX_VALUE);

                    if ((lStatsPerConnectionType.get(lConnectionType) == Double.MAX_VALUE && connectionTypeUnstableAlarm.get(lConnectionType) != null) ||
                        lNetworkStats != Double.MAX_VALUE) {
                        clearAlarm(connectionTypeUnstableAlarm.get(lConnectionType),
                                   Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.unstableAlarm"));
                        connectionTypeUnstableAlarm.put(lConnectionType, null);
                    } else {
                        // Raise unstable alarm for current connection type only if the whole network is not declared unstable
                        if (lNetworkStats == Double.MAX_VALUE &&
                            lStatsPerConnectionType.get(lConnectionType) != Double.MAX_VALUE &&
                            lStatsPerConnectionType.get(lConnectionType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold1()) {
                            connectionTypeUnstableAlarm.computeIfAbsent(lConnectionType, t -> raiseAlarm(connectionTypeAlarmId.get(t)));
                            if (lStatsPerConnectionType.get(lConnectionType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold3()) {
                                if (!connectionTypeUnstableAlarm.get(lConnectionType).getSeverity().equals(EnumTypes.AlarmSeverity.MAJOR)) {
                                    changeSeverity(connectionTypeUnstableAlarm.get(lConnectionType), EnumTypes.AlarmSeverity.MAJOR);
                                    if (Cat.getInstance().displayGraphicalInterface()) Cat.getInstance().getController().refreshActiveAlarmsListAndRemoveSelection();
                                    sendMail(lConnectionType, connectionTypeUnstableAlarm.get(lConnectionType), true);
                                }
                            } else if (lStatsPerConnectionType.get(lConnectionType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold2()) {
                                if (!connectionTypeUnstableAlarm.get(lConnectionType).getSeverity().equals(EnumTypes.AlarmSeverity.MINOR)) {
                                    changeSeverity(connectionTypeUnstableAlarm.get(lConnectionType), EnumTypes.AlarmSeverity.MINOR);
                                    if (Cat.getInstance().displayGraphicalInterface()) Cat.getInstance().getController().refreshActiveAlarmsListAndRemoveSelection();
                                    sendMail(lConnectionType, connectionTypeUnstableAlarm.get(lConnectionType), true);
                                }
                            } else if (lStatsPerConnectionType.get(lConnectionType) <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold1()) {
                                if (!connectionTypeUnstableAlarm.get(lConnectionType).getSeverity().equals(EnumTypes.AlarmSeverity.WARNING)) {
                                    changeSeverity(connectionTypeUnstableAlarm.get(lConnectionType), EnumTypes.AlarmSeverity.WARNING);
                                    if (Cat.getInstance().displayGraphicalInterface()) Cat.getInstance().getController().refreshActiveAlarmsListAndRemoveSelection();
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

                // Check unstable network
                // Raise unstable alarm for current connection type only if the whole network is not declared unstable
                if (lNetworkStats  <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold1()) {

                    if (networkUnstableAlarm == null) {
                        networkUnstableAlarm = raiseAlarm(EnumTypes.AlarmId.NETWORK_UNSTABLE);
                    }
                    if (lNetworkStats <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold3()) {
                        if (!networkUnstableAlarm.getSeverity().equals(EnumTypes.AlarmSeverity.MAJOR)) {
                            changeSeverity(networkUnstableAlarm, EnumTypes.AlarmSeverity.MAJOR);
                            if (Cat.getInstance().displayGraphicalInterface()) Cat.getInstance().getController().refreshActiveAlarmsListAndRemoveSelection();
                        }
                    } else if (lNetworkStats <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold2()) {
                        if (!networkUnstableAlarm.getSeverity().equals(EnumTypes.AlarmSeverity.MINOR)) {
                            changeSeverity(networkUnstableAlarm, EnumTypes.AlarmSeverity.MINOR);
                            if (Cat.getInstance().displayGraphicalInterface()) Cat.getInstance().getController().refreshActiveAlarmsListAndRemoveSelection();
                        }
                    } else if (lNetworkStats <= lConfiguration.getMeanTimeBetweenTwoConnectionsLostThreshold1()) {
                        if (!networkUnstableAlarm.getSeverity().equals(EnumTypes.AlarmSeverity.WARNING)) {
                            changeSeverity(networkUnstableAlarm, EnumTypes.AlarmSeverity.WARNING);
                            if (Cat.getInstance().displayGraphicalInterface()) Cat.getInstance().getController().refreshActiveAlarmsListAndRemoveSelection();
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

    // Global monitoring instance
    private static GlobalMonitoring globalMonitoringInstance = new GlobalMonitoring();

    // Monitoring jobs information
    private HashMap<MonitoringJob, JobDetails> monitoringJobStates = new HashMap<>();

    private boolean wifiMonitored = false;
    private boolean ethernetMonitored = false;
    private boolean wanMonitored = false;
    private boolean lanMonitored = false;

    private Integer networkState = Constants.STATE_OK;

    private Alarm ethernetDownAlarm;
    private Alarm wifiDownAlarm;
    private Alarm wanDownAlarm;
    private Alarm lanDownAlarm;
    private Alarm networkDownAlarm;

    private volatile ObservableList<Alarm> activeAlarmsList = FXCollections.observableArrayList();
    private volatile ObservableList<Alarm> historicalAlarmsList = FXCollections.observableArrayList();

    private String rawResultTemplate;

    private String colorStateOk;
    private String colorStateDegraded;
    private String colorStateNok;
    private String colorAlarmInfo;
    private String colorAlarmWarning;
    private String colorAlarmMinor;
    private String colorAlarmMajor;
    private String colorAlarmCritical;
    private String reportBodyTemplate;
    private String reportJobResultTemplate;
    private PeriodicCheck periodicCheck = new PeriodicCheck();

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
        Thread lThread = new Thread(periodicCheck);
        lThread.start();

        // Launch periodic speed test and periodic reports
        PeriodicSpeedTest.getInstance().start();
        PeriodicReports.getInstance().start();

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
        if (Cat.getInstance().displayGraphicalInterface()) {
            Cat.getInstance().getController().setInterfaceTypeImageView(aInMonitoringJob.getInterfaceType(), true);
            Cat.getInstance().getController().setAddressTypeStateImageView(aInMonitoringJob.getAddressType(), true);
            Cat.getInstance().getController().setGlobalStateImageView(networkState);
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
                lJobDetails.setLastLostPingDate(System.currentTimeMillis());
                lJobDetails.incrementPingsLostCount();
                if (lJobDetails.getLostPingsCount() == 1) lJobDetails.resetStartMonitoringDate();
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
            if (Cat.getInstance().displayGraphicalInterface()) Cat.getInstance().getController().setAddressTypeStateImageView(aInMonitoringJob.getAddressType(), false);
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
            if (Cat.getInstance().displayGraphicalInterface()) Cat.getInstance().getController().setInterfaceTypeImageView(aInMonitoringJob.getInterfaceType(), false);
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
        networkState = Constants.STATE_NOK;
        for (MonitoringJob lMonitoringJob : monitoringJobStates.keySet()) {
            if (!monitoringJobStates.get(lMonitoringJob).getState().equals(EnumTypes.HostState.UNREACHABLE)) {
                networkState = Constants.STATE_DEGRADED;
                break;
            }
        }
        boolean lAllReachable = true;
        for (MonitoringJob lMonitoringJob : monitoringJobStates.keySet()) {
            if (monitoringJobStates.get(lMonitoringJob).getState().equals(EnumTypes.HostState.UNREACHABLE)) {
                lAllReachable = false;
                break;
            }
        }
        if (lAllReachable) networkState = Constants.STATE_OK;

        if (Cat.getInstance().displayGraphicalInterface()) Cat.getInstance().getController().setGlobalStateImageView(networkState);

        if (networkState == Constants.STATE_NOK) {

            // Raise network down alarm
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

            if (networkDownAlarm != null) {
                sendMail("network", networkDownAlarm);
                clearAlarm(networkDownAlarm, Display.getViewResourceBundle().getString("globalMonitoring.alarms.autoClear.siteRecovered.networkDown"));
            }

            if (Cat.getInstance().displayGraphicalInterface()) Cat.getInstance().getController().setInterfaceTypeImageView(aInMonitoringJob.getInterfaceType(), true);
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
            if (Cat.getInstance().displayGraphicalInterface()) Cat.getInstance().getController().setAddressTypeStateImageView(aInMonitoringJob.getAddressType(), true);
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

        Email email = new Email((!Cat.getInstance().displayGraphicalInterface() || Cat.getInstance().getController().isButtonGeneralEmailEnabled()) &&
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
                Cat.getInstance().getController().printConsole(new Message(lMessage, lAlarm.getSeverity().getMessageLevel()));
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
            Cat.getInstance().getController().printConsole(new Message(lMessage, lAlarm.getSeverity().getMessageLevel()));

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
                    Cat.getInstance().getController().printConsole(new Message(lMessage, EnumTypes.MessageLevel.OK));
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
            Cat.getInstance().getController().printConsole(new Message(lMessage, EnumTypes.MessageLevel.INFO));
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
            Cat.getInstance().getController().printConsole(new Message(lMessage, EnumTypes.MessageLevel.INFO));
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
            Cat.getInstance().getController().printConsole(new Message(lMessage, aInNewSeverity.getMessageLevel()));

            aInAlarm.changeSeverity(aInNewSeverity);

        }

    }

    /**
     * Resets report, i.e. resets statistics for all jobs
     */
    public void resetReport() {
        for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
            monitoringJobStates.get(lMonitoringJob).resetReport();
        }
    }

    /**
     * Builds the periodic global report
     * @return Periodic global report
     */
    public String buildReport() {

        String lReport = reportBodyTemplate;

        long lLostPingsCount = 0; long lLostPingsSinceLastReport = 0;
        long lLostConnectionsCount = 0; long lLostConnectionsSinceLastReport = 0;
        for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
            lLostPingsCount += lMonitoringJob.getLostPingsCount();
            lLostPingsSinceLastReport += monitoringJobStates.get(lMonitoringJob).getLostPingsSinceLastReport();
            lLostConnectionsCount += lMonitoringJob.getLostConnectionsCount();
            lLostConnectionsSinceLastReport += monitoringJobStates.get(lMonitoringJob).getLostConnectionsSinceLastReport();
        }

        int lInfo = 0; int lWarning = 0; int lMinor = 0; int lMajor = 0; int lCritical = 0;
        int lHighestSeverity = 0;
        for (Alarm lAlarm: activeAlarmsList) {
            if (lAlarm.getSeverity() == EnumTypes.AlarmSeverity.INFO) {
                lInfo++;
                if (lHighestSeverity < 1) lHighestSeverity = 1;
            } else if (lAlarm.getSeverity() == EnumTypes.AlarmSeverity.WARNING) {
                lWarning++;
                if (lHighestSeverity < 2) lHighestSeverity = 2;
            } else if (lAlarm.getSeverity() == EnumTypes.AlarmSeverity.MINOR) {
                lMinor++;
                if (lHighestSeverity < 3) lHighestSeverity = 3;
            } else if (lAlarm.getSeverity() == EnumTypes.AlarmSeverity.MAJOR) {
                lMajor++;
                if (lHighestSeverity < 4) lHighestSeverity = 5;
            } else if (lAlarm.getSeverity() == EnumTypes.AlarmSeverity.CRITICAL) {
                lCritical++;
                if (lHighestSeverity < 5) lHighestSeverity = 5;
            }
        }
        String lActiveAlarmColor = (lHighestSeverity == 0) ? colorStateOk :
                                   (lHighestSeverity == 1) ? colorAlarmInfo :
                                   (lHighestSeverity == 2) ? colorAlarmWarning :
                                   (lHighestSeverity == 3) ? colorAlarmMinor :
                                   (lHighestSeverity == 4) ? colorAlarmMajor :
                                   colorAlarmCritical;


        // Build summary report
        lReport = lReport
                .replaceAll("#NETWORK_STATE#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.state." + networkState))
                .replaceAll("#NETWORK_STATE_COLOR#", (networkState == Constants.STATE_OK) ?
                                                     colorStateOk : ((networkState == Constants.STATE_DEGRADED) ? colorStateDegraded : colorStateNok))
                .replaceAll("#WAN_STATE#", (wanMonitored) ?
                                           ((wanDownAlarm == null) ? Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.state.0")
                                                                   : Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.state.2"))
                                                                 : "")
                .replaceAll("#WAN_STATE_COLOR#", (wanMonitored) ? ((wanDownAlarm == null) ? colorStateOk : colorStateNok) : "")
                .replaceAll("#LAN_STATE#", (lanMonitored) ?
                                           ((lanDownAlarm == null) ? Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.state.0")
                                                                   : Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.state.2"))
                                                          : "")
                .replaceAll("#LAN_STATE_COLOR#", (lanMonitored) ? ((lanDownAlarm == null) ? colorStateOk : colorStateNok) : "")
                .replaceAll("#ETH_STATE#", (ethernetMonitored) ?
                                           ((ethernetDownAlarm == null) ? Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.state.0")
                                                                        : Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.state.2"))
                                                               : "")
                .replaceAll("#ETH_STATE_COLOR#", (ethernetMonitored) ? ((ethernetDownAlarm == null) ? colorStateOk : colorStateNok) : "")
                .replaceAll("#WIFI_STATE#", (wifiMonitored) ?
                                            ((wifiDownAlarm == null) ? Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.state.0")
                                                                     : Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.global.general.state.2"))
                                                            : "")
                .replaceAll("#WIFI_STATE_COLOR#", (wifiMonitored) ? ((wifiDownAlarm == null) ? colorStateOk : colorStateNok) : "")
                .replaceAll("#LOST_PINGS_COUNT#", lLostPingsSinceLastReport + " / " + lLostPingsCount)
                .replaceAll("#LOST_PINGS_COUNT_COLOR#", (lLostPingsSinceLastReport == 0) ? colorStateOk : colorStateDegraded)
                .replaceAll("#LOST_CONNECTIONS_COUNT#", lLostConnectionsSinceLastReport + " / " + lLostConnectionsCount)
                .replaceAll("#LOST_CONNECTIONS_COUNT_COLOR#", (lLostConnectionsSinceLastReport == 0) ? colorStateOk : colorStateNok)
                .replaceAll("#ACTIVE_ALARMS_COUNT#", String.valueOf(activeAlarmsList.size()))
                .replaceAll("#ACTIVE_ALARMS_COLOR#", lActiveAlarmColor)
                .replaceAll("#ACTIVE_INFO_ALARMS_COUNT#", String.valueOf(lInfo))
                .replaceAll("#ACTIVE_INFO_ALARMS_COLOR#", (lInfo == 0) ? colorStateOk : colorAlarmInfo)
                .replaceAll("#ACTIVE_WARNING_ALARMS_COUNT#", String.valueOf(lWarning))
                .replaceAll("#ACTIVE_WARNING_ALARMS_COLOR#", (lWarning == 0) ? colorStateOk : colorAlarmWarning)
                .replaceAll("#ACTIVE_MINOR_ALARMS_COUNT#", String.valueOf(lMinor))
                .replaceAll("#ACTIVE_MINOR_ALARMS_COLOR#", (lMinor == 0) ? colorStateOk : colorAlarmMinor)
                .replaceAll("#ACTIVE_MAJOR_ALARMS_COUNT#", String.valueOf(lMajor))
                .replaceAll("#ACTIVE_MAJOR_ALARMS_COLOR#", (lMajor == 0) ? colorStateOk : colorAlarmMajor)
                .replaceAll("#ACTIVE_CRITICAL_ALARMS_COUNT#", String.valueOf(lCritical))
                .replaceAll("#ACTIVE_CRITICAL_ALARMS_COLOR#", (lCritical == 0) ? colorStateOk : colorAlarmCritical)
        ;

        List<MonitoringJob> lSortedMonitoringJobs = new ArrayList<>(monitoringJobStates.keySet());
        lSortedMonitoringJobs.sort(Comparator.comparing(MonitoringJob::getNetworkInterfaceIndex));

// TODO        for (MonitoringJob lMonitoringJob: monitoringJobStates.keySet()) {
        for (MonitoringJob lMonitoringJob: lSortedMonitoringJobs) {

            String lReportJobResult =
                    reportJobResultTemplate
                            .replaceAll("#JOB_STATE_COLOR#", (lMonitoringJob.getHostState().equals(EnumTypes.HostState.UNREACHABLE) ? "red" : "green"))
                            .replaceAll("#JOB_SERVER_NAME#", lMonitoringJob.getRemoteHostname())
                            .replaceAll("#JOB_SERVER_IP#", lMonitoringJob.getRemoteIp())
                            .replaceAll("#JOB_SERVER_TYPE#", Display.getMessagesResourceBundle()
                                                                    .getString("generalEmail.periodicReports.global.jobs.serverType." + lMonitoringJob.getServerType().toString()))
                            .replaceAll("#JOB_INTERFACE#", lMonitoringJob.getInterfaceType().toString())
                            .replaceAll("#JOB_ADDRESS_TYPE#", lMonitoringJob.getAddressType().toString())
                            .replaceAll("#JOB_LOST_PINGS#", monitoringJobStates.get(lMonitoringJob).getLostPingsSinceLastReport() + "&nbsp;/&nbsp;" + lMonitoringJob.getLostPingsCount())
                            .replaceAll("#JOB_LOST_CONNECTIONS#", monitoringJobStates.get(lMonitoringJob).getLostConnectionsSinceLastReport() + "&nbsp;/&nbsp;" +
                                                                  lMonitoringJob.getLostConnectionsCount())
                            .replaceAll("#JOB_ROUND_TRIP#", lMonitoringJob.getMinRoundTrip() + "&nbsp;/&nbsp;" + lMonitoringJob.getMaxRoundTrip() + "&nbsp;/&nbsp;" +
                                                            String.format("%.1f", lMonitoringJob.getAverageRoundTrip()));

            lReport = lReport.replaceAll("#JOB_RESULT#", lReportJobResult + "\n#JOB_RESULT#");

        }

        lReport = lReport.replaceAll("#JOB_RESULT#", "");

        // Build by job report
        return lReport;

    }

}
