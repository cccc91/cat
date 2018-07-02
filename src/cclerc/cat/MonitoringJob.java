package cclerc.cat;

import cclerc.cat.Configuration.Configuration;
import cclerc.cat.Configuration.ServerConfiguration;
import cclerc.cat.view.CatView;
import cclerc.cat.view.MonitoringJobView;
import cclerc.services.*;
import javafx.application.Platform;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Instant;
import java.util.*;

/**
 * Class implementing the runnable job that is monitoring a host
 */
public class MonitoringJob implements Runnable {

    // Class implementing parameters needed to monitor a specific server
    private class ServerParameters {

        // Configuration information
        private String host;
        private int pollingPeriod;
        private int maxRetries;
        private int timeout;
        private int connectionLostThreshold;

        // Computed information
        private String remoteHostname;
        private String remoteIp;
        private int port;

    }

    // Class implementing the server reachability
    private class ServerReachability {

        private boolean isReachable;
        private int retries;

        private boolean isReachable() {
            return isReachable;
        }

        private int getRetries() {
            return retries;
        }

        private boolean hasRetries() {
            return isReachable && retries > 0;
        }

    }

    // Class variables
    private static boolean displayGraphicalInterface;

    // Semaphore
    private final Object lock = new Object();

    // Monitoring jobs collection, stored by job identifier, and list of jobs stored by type
    private volatile static List<MonitoringJob> monitoringJobs = new ArrayList<>();
    private volatile static HashMap<EnumTypes.AddressType, HashMap<EnumTypes.InterfaceType, MonitoringJob>> monitoringJobsByType = new HashMap<>();

    // Number of running and paused jobs
    private volatile static int runningCount = 0;
    private volatile static int totalCount = 0;

    // Job states
    private boolean running = true;
    private boolean paused = false;
    private int activeServer = -1;
    private EnumTypes.HostState hostState = EnumTypes.HostState.REACHABLE;

    // Notifications
    private Email email;

    // Monitoring parameters
    private int networkInterfaceIndex;
    private NetworkInterface networkInterface;
    private String localHost;
    private Map<Integer, ServerParameters> serverParameters = new HashMap<>();

    // User interface
    private MonitoringJobView controller;
    private CatView catController;

    // Job properties
    private Integer jobIdentifier;
    private EnumTypes.AddressType addressType;   // lan, wan
    private EnumTypes.InterfaceType interfaceType; // wifi, ethernet
    private EnumTypes.ServerType serverType; // primary, backup
    private boolean useProxy;

    // Local summary information
    private long pingsCount = 0;
    private long retriesCount = 0;
    private long lostPingsCount = 0;
    private long consecutiveLostPingsCount = 0;
    private long lostConnectionsCount = 0;
    private long lostConnectionInterval = Utilities.NO_DURATION;
    private Date previousLostConnectionDate;
    private Date lastLostConnectionDate;
    private Date lastRecoveryConnectionDate;
    private long ongoingLossDuration = 0;
    private long totalLossDuration = 0;
    private long roundTrip = Integer.MAX_VALUE;
    private long totalRoundTrip = 0;
    private long minRoundTrip = Integer.MAX_VALUE;
    private long maxRoundTrip = 0;
    private int reachableCount = 0;
    private boolean connectionLost = false;

    // Messages
    private volatile List<Message> messages = new ArrayList<>();

    // Notifications
    private boolean forceNotifyCurrentJobState = false;

    // General display
    private enum GeneralDisplayNotification {
        INCREMENT_PING_COUNT, INCREMENT_LOST_PING_COUNT, LOSE_CONNECTION, RECOVER_CONNECTION
    }

    // CONSTRUCTOR

    /**
     * Monitoring job constructor
     *
     * @param aInAddressType              Configured address type of the monitored server, should be the same as the actual address type of the monitored server
     * @param aInNetworkInterfaceIndex    Index in the network interfaces configuration of the network interface to be used to monitor the server
     * @param aInNetworkInterfaceList     Network interfaces list
     * @param aInMonitoringJobController  Controller corresponding to the job
     * @param aInCatController            Controller corresponding to the cat view
     * @throws Exception When host is unknown
     */
    public MonitoringJob(EnumTypes.AddressType aInAddressType, int aInNetworkInterfaceIndex,
                         HashMap<String, NetworkInterface> aInNetworkInterfaceList,
                         MonitoringJobView aInMonitoringJobController, CatView aInCatController)
            throws Exception {

        // Compute job identifier
        synchronized (lock) {jobIdentifier = ++totalCount;}

        networkInterfaceIndex = aInNetworkInterfaceIndex;

        // Local network configuration
        InetAddress lLocalInetAddress = InetAddress.getLocalHost();
        localHost = lLocalInetAddress.getHostName();
        networkInterface = aInNetworkInterfaceList.get(
                Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().getNetworkInterfaceConfigurations().get(aInNetworkInterfaceIndex).getName());

        useProxy = Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(aInAddressType).getUseProxy();
        // Find information for all servers of the configuration for expected address type
        for (int lServerIndex = 0; lServerIndex < 2; lServerIndex++) {

            // Retrieve current server configuration
            ServerConfiguration lServerConfiguration = Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(aInAddressType).getServer(lServerIndex);

            if (lServerConfiguration != null && !lServerConfiguration.getHostname().equals("")) {

                // Create the server parameters instance
                ServerParameters lServerParameters = new ServerParameters();

                // Get server configuration information
                lServerParameters.host = lServerConfiguration.getHostname();
                lServerParameters.maxRetries = lServerConfiguration.getMaxRetries();
                lServerParameters.pollingPeriod = lServerConfiguration.getPollingPeriod();
                lServerParameters.timeout = lServerConfiguration.getTimeout();
                lServerParameters.connectionLostThreshold = lServerConfiguration.getConnectionLostThreshold();

                // Compute server instance InetAddress, and try to find required ip version and if no ip found in this version, try the other one
                InetAddress lInetAddress;
                try {
                    if (lServerConfiguration.getIpv6()) {
                        lInetAddress = Network.getHostInet6Address(lServerParameters.host);
                        if (lInetAddress == null) {
                            Display.getLogger().warn(String.format(Display.getMessagesResourceBundle().getString("log.monitoringJob.incorrectIpV6"), lServerParameters.host));
                            lInetAddress = Network.getHostInet4Address(lServerParameters.host);
                        }
                    } else {
                        lInetAddress = Network.getHostInet4Address(lServerParameters.host);
                        if (lInetAddress == null) {
                            Display.getLogger().warn(String.format(Display.getMessagesResourceBundle().getString("log.monitoringJob.incorrectIpV4"), lServerParameters.host));
                            lInetAddress = Network.getHostInet6Address(lServerParameters.host);
                        }
                    }
                } catch (Exception e) {
                    lInetAddress = null;
                }

                lServerParameters.remoteHostname = (lInetAddress != null) ? lInetAddress.getHostName() : lServerConfiguration.getHostname();
                lServerParameters.remoteIp = (lInetAddress != null) ? lInetAddress.getHostAddress() : "?";
                lServerParameters.port = Network.findPort(lServerParameters.remoteIp, networkInterface, lServerParameters.timeout, useProxy);

                // If remote ip can be found, and if either a port that can be opened exists or remote ip replies to ping requests, job can be created
                if (lInetAddress != null && (lServerParameters.port != 0 ||
                        (Network.isReachable(lServerParameters.remoteIp, networkInterface, lServerParameters.timeout, lServerParameters.maxRetries,
                                lServerParameters.pollingPeriod)
                                <= lServerParameters.maxRetries))) {

                    // First reachable server is declared as the active server
                    if (activeServer == -1) activeServer = lServerIndex;

                    // Compute interface used to monitor host and  monitored host address type
                    interfaceType = Network.getInterfaceType(networkInterface.getName());
                    addressType = (lInetAddress.isSiteLocalAddress()) ? EnumTypes.AddressType.LAN : EnumTypes.AddressType.WAN;

                    // Check if address type is the same as the one required in the configuration
                    if (!addressType.equals(aInAddressType)) {
                        Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.configuration.addressTypeInconsistent"),
                                lServerParameters.remoteHostname, aInAddressType, addressType));
                        throw new IllegalArgumentException();
                    }

                    // Add current monitoring job in monitoring jobs collections
                    if (controller == null) {

                        monitoringJobs.add(this);

                        HashMap<EnumTypes.InterfaceType, MonitoringJob> lMonitoringJobByInterfaceType;
                        if (monitoringJobsByType.containsKey(addressType)) {
                            lMonitoringJobByInterfaceType = monitoringJobsByType.get(addressType);
                        } else {
                            lMonitoringJobByInterfaceType = new HashMap<>();
                            monitoringJobsByType.put(addressType, lMonitoringJobByInterfaceType);
                        }
                        lMonitoringJobByInterfaceType.put(interfaceType, this);

                    }

                    // Add current monitoring job in global monitoring
                    GlobalMonitoring.getInstance().addMonitoringJob(this);

                    // Display initializations
                    if (displayGraphicalInterface && controller == null) {

                        // Attach current monitoring job to the monitoring job view for further back reference
                        controller = aInMonitoringJobController;
                        controller.setMonitoringJob(this);
                        catController = aInCatController;

                        // Set scroll policy
                        controller.setDetailsScrollPolicy();

                    }
                    if (displayGraphicalInterface) controller.getCat().getController().setMonitoringJobTooltip(addressType, networkInterfaceIndex + 1, networkInterface, lServerParameters.remoteIp);

                    serverParameters.put(lServerIndex, lServerParameters);

                } else {
                    Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.monitoringJob.incorrectIp"),
                                                            lServerParameters.remoteHostname, aInAddressType, Network.getInterfaceType(networkInterface.getName())));
                }

                // Case no job can be created, controller must be created anyway if this is the last server of the list
                if (controller == null && lServerIndex == 1 && displayGraphicalInterface) {
                    interfaceType = Network.getInterfaceType(networkInterface.getName());
                    addressType = (lInetAddress != null && lInetAddress.isSiteLocalAddress()) ? EnumTypes.AddressType.LAN : EnumTypes.AddressType.WAN;
                    controller = aInMonitoringJobController;
                }

            }

        }

        if (serverParameters.keySet().size() != 0) {
            // Monitoring job can be created, finish setup

            paused = States.getInstance().getBooleanValue(BuildStatePropertyName(Constants.PAUSE_STATE));
            email = new Email(isEmailAllowed() && States.getInstance().getBooleanValue(BuildStatePropertyName(Constants.SEND_MAIL_STATE), true),
                              Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer());
            email.setRecipients(Arrays.asList(Configuration.getCurrentConfiguration().getEmailConfiguration().getRecipientList().split(";")));
            if (!isEmailAllowed() && displayGraphicalInterface) controller.disableEmailButton();

            // Set active server to primary if no reachable server has been found
            if (activeServer == -1) activeServer = serverParameters.keySet().iterator().next();
            serverType = (activeServer == 0) ? EnumTypes.ServerType.PRIMARY : EnumTypes.ServerType.BACKUP;

        } else {
            // Monitoring job cannot be created, disable the related tab
            if (displayGraphicalInterface) aInMonitoringJobController.disableMonitoringJobTab();
        }

    }

    // THREADS

    /**
     * Runs monitoring job:
     * - checks periodically if a host is reachable within a given timeout through the given network interface
     * - collects statistics
     */
    @Override
    public void run() {

        // Launch job only if parameters of at least one server are correct
        if (serverParameters.keySet().size() == 0) return;

        // Initialize charts
        if (displayGraphicalInterface) Platform.runLater(() -> {catController.addPingSeries(addressType, networkInterface);});

        // Wait for cat end of initialization
        while (Cat.getInstance() == null || Cat.getInstance().isInitializationInProgress()) {
            Utilities.sleep(1000);
        }

        ServerParameters lActiveServerParameters = serverParameters.get(activeServer);

        // Name thread
        Thread.currentThread().setName(String.format("Monitoring job %s Thread", jobIdentifier));

        // Increment number of running and total jobs
        synchronized (lock) {runningCount++;}

        // Retrieve information on remote host and local interface
        String lInterfaceName = networkInterface.getName();
        String lLocalIp = Network.getNetworkInterfaceInetAddress(networkInterface, Network.isIPv4Address(lActiveServerParameters.remoteIp)).getHostAddress();

        if (serverParameters.keySet().size() == 2) {
            Display.getLogger().debug(String.format(Display.getMessagesResourceBundle().getString("log.monitoringJob.start"), addressType, lInterfaceName, lLocalIp,
                                                    serverParameters.get(0).remoteHostname, serverParameters.get(0).remoteIp, serverParameters.get(0).port,
                                                    serverParameters.get(1).remoteHostname, serverParameters.get(1).remoteIp, serverParameters.get(1).port));
            messages.add(new Message(String.format(Display.getViewResourceBundle().getString("monitoringJob.message.startJob"), lInterfaceName, lLocalIp,
                                      serverParameters.get(0).remoteHostname, serverParameters.get(0).remoteIp, serverParameters.get(0).port,
                                      serverParameters.get(1).remoteHostname, serverParameters.get(1).remoteIp, serverParameters.get(1).port),
                                     EnumTypes.MessageLevel.INFO, new Date()));

            // Enable the active server slider only when 2 servers are monitored
            controller.enableActiveServerSlider();

        } else {
            int lIndex = serverParameters.keySet().iterator().next();
            Display.getLogger().debug(String.format(Display.getMessagesResourceBundle().getString("log.monitoringJob." + serverType.toString() + ".start"),
                                                    addressType, lInterfaceName, lLocalIp,
                                                    serverParameters.get(lIndex).remoteHostname, serverParameters.get(lIndex).remoteIp, serverParameters.get(lIndex).port));
            messages.add(new Message(String.format(Display.getViewResourceBundle().getString("monitoringJob.message."+ serverType.toString() + ".startJob"),
                                                   lInterfaceName, lLocalIp, serverParameters.get(lIndex).remoteHostname, serverParameters.get(lIndex).remoteIp,
                                                   serverParameters.get(lIndex).port),
                                     EnumTypes.MessageLevel.INFO, new Date()));

        }

        changeActiveServer(activeServer, "startup");
        refreshDisplay();

        long lStartTime = Instant.now().toEpochMilli();

        // Run the job until it is stopped by user or an error occurs
        boolean lFirstLostPing = true;
        while (running) {

            long lPollingStartTime = Instant.now().toEpochMilli();
            lActiveServerParameters = serverParameters.get(activeServer);
            ServerParameters lAlternativeServerParameters = serverParameters.get(1 - activeServer);

            try {

                // Monitor remote host only if job is not paused
                if (!paused) {

                    pingsCount++;
                    refreshGeneralDisplay(GeneralDisplayNotification.INCREMENT_PING_COUNT);

                    // Reachability test and time measurement
                    long lBeforeTime = Instant.now().toEpochMilli();

// TODO - Force primary to be down (eth or wifi)
//if (pingsCount > 3 && activeServer == 0 && lActiveServerParameters.host.equals("www.google.fr") && interfaceType.equals(EnumTypes.InterfaceType.ETH)) lActiveServerParameters.remoteIp = "135.117.205.22";
//if (pingsCount > 3 && activeServer == 0 && lActiveServerParameters.host.equals("www.google.fr") && interfaceType.equals(EnumTypes.InterfaceType.WIFI)) lActiveServerParameters.remoteIp = "135.117.205.22";                    // Check active server reachability
                    ServerReachability lServerReachability = checkServerReachability(lActiveServerParameters);

                    // Log in case of retries
                    if (lServerReachability.hasRetries()) {
                        Display.getLogger().log(Constants.FILE, String.format(
                                Display.getMessagesResourceBundle().getString("log.monitoringJob.retry"),
                                lServerReachability.getRetries(), lActiveServerParameters.remoteHostname, lActiveServerParameters.remoteIp, lInterfaceName, lLocalIp));
                        retriesCount += lServerReachability.getRetries();
                    }

                    // Polling period start after ping has answered (or not...)
                    lPollingStartTime = Instant.now().toEpochMilli();
                    roundTrip = lPollingStartTime - lBeforeTime;

                    // Add statistics
                    catController.addPingSeriesData(
                            serverType, addressType, networkInterface, lPollingStartTime, lPollingStartTime - lStartTime, roundTrip, lServerReachability.isReachable);

                    // Ping is lost: build statistics, log and display
                    if (!lServerReachability.isReachable) {

                        consecutiveLostPingsCount++;
                        lostPingsCount++;
                        refreshGeneralDisplay(GeneralDisplayNotification.INCREMENT_LOST_PING_COUNT);

                        // Log ping loss and change job state
                        String lPingLostMessage;
                        if (lFirstLostPing) {
                            lPingLostMessage = String.format(
                                    Display.getMessagesResourceBundle().getString(
                                            "log.monitoringJob.lostPing"), lActiveServerParameters.remoteHostname, lActiveServerParameters.remoteIp, lInterfaceName, lLocalIp,
                                    lostPingsCount);
                            lastLostConnectionDate = new Date();
                            Display.getLogger().warn(lPingLostMessage);
                            messages.add(new Message(Display.getViewResourceBundle().getString("monitoringJob.message.pingLost"), EnumTypes.MessageLevel.WARNING, new Date()));
                            hostState = EnumTypes.HostState.PING_LOST;
                            if (alertAllowed()) GlobalMonitoring.getInstance().changeJobState(this, hostState);
                            lFirstLostPing = false;
                        }

                        // Connection loss
                        if (!connectionLost && (consecutiveLostPingsCount >= lActiveServerParameters.connectionLostThreshold)) {

                            // Alternative server is reachable, switch on it if it exists
                            if (lAlternativeServerParameters != null && checkServerReachability(lAlternativeServerParameters).isReachable()) {

                                messages.remove(messages.size() - 1);
                                String lConnectionLostMessage = String.format(
                                        Display.getMessagesResourceBundle().getString("log.monitoringJob." + serverType.toString() + ".connectionLost"),
                                        lActiveServerParameters.remoteHostname, lActiveServerParameters.remoteIp, lInterfaceName, lLocalIp);
                                Display.getLogger().error(lConnectionLostMessage);
                                messages.add(new Message(String.format(
                                        Display.getViewResourceBundle().getString("monitoringJob.message." + serverType.toString() + ".connectionLost"),
                                        serverParameters.get(activeServer).remoteHostname, serverParameters.get(activeServer).remoteIp),
                                                         EnumTypes.MessageLevel.WARNING, new Date()));
                                changeActiveServer(1 - activeServer, "automatic");

                            // Alternative server is not reachable, connection is lost on current interface
                            } else {

                                lostConnectionsCount++;
                                connectionLost = true;

                                // Compute interval since last connection loss if not the first one
                                if (previousLostConnectionDate != null) {
                                    lostConnectionInterval = lastLostConnectionDate.toInstant().toEpochMilli() - previousLostConnectionDate.toInstant().toEpochMilli();
                                }
                                previousLostConnectionDate = lastLostConnectionDate;
                                hostState = EnumTypes.HostState.UNREACHABLE;

                                // Log connection loss
                                messages.remove(messages.size() - 1);
                                messages.add(new Message(Display.getViewResourceBundle().getString("monitoringJob.message.connectionLost"),
                                                         EnumTypes.MessageLevel.ERROR, new Date()));

                                if (alertAllowed()) {

                                    // Set this job to unreachable in global monitoring
                                    GlobalMonitoring.getInstance().changeJobState(this, hostState);

                                    String lConnectionLostMessage = String.format(
                                            Display.getMessagesResourceBundle().getString("log.monitoringJob.connectionLost"), lInterfaceName, lLocalIp, lostConnectionsCount);
                                    Display.getLogger().error(lConnectionLostMessage);

                                    refreshGeneralDisplay(GeneralDisplayNotification.LOSE_CONNECTION);

                                }

                            }

                        }

                        // Force notify global monitoring after a connection loss
                        if (forceNotifyCurrentJobState && alertAllowed()) {
                            GlobalMonitoring.getInstance().changeJobState(this, EnumTypes.HostState.UNREACHABLE);
                            forceNotifyCurrentJobState = false;
                        }

                        // Connection is lost, compute loss connection durations
                        if (connectionLost) ongoingLossDuration = new Date().toInstant().toEpochMilli() - lastLostConnectionDate.toInstant().toEpochMilli();

                        // Ping is ok: build statistics and display
                    } else {

                        consecutiveLostPingsCount = 0;
                        reachableCount++;
                        lFirstLostPing = true;

                        totalRoundTrip += roundTrip;
                        if (roundTrip > maxRoundTrip) maxRoundTrip = roundTrip;
                        if (roundTrip < minRoundTrip) minRoundTrip = roundTrip;

                        hostState = EnumTypes.HostState.REACHABLE;

                        // Connection recovery
                        if (connectionLost) {

                            totalLossDuration += ongoingLossDuration;
                            ongoingLossDuration = 0;
                            lastRecoveryConnectionDate = new Date();

                            long lDuration = lastRecoveryConnectionDate.toInstant().toEpochMilli() - lastLostConnectionDate.toInstant().toEpochMilli();

                            // Log connection recovery
                            String lConnectionLostMessage;
                            lConnectionLostMessage = String.format(
                                    Display.getMessagesResourceBundle().getString("log.monitoringJob.connectionRecovered"), lInterfaceName, lLocalIp,
                                    Utilities.formatDuration(lDuration, 0));
                            Display.getLogger().info(lConnectionLostMessage);

                            messages.add(new Message(String.format(
                                    Display.getViewResourceBundle().getString("monitoringJob.message.connectionRecovered"), Utilities.formatDuration(lDuration, 0)),
                                                     EnumTypes.MessageLevel.OK, new Date()));

                            if (alertAllowed()) {

                                // Email the recovery
                                int lSendRetries = 0;
                                boolean lSendMailSuccessful = false;
                                while (!lSendMailSuccessful && lSendRetries++ <= Constants.MAX_RETRIES_SEND_MAIL) {
                                    try {
                                        email.sendMail(
                                                String.format(Display.getMessagesResourceBundle().getString("email.subject"), localHost, lActiveServerParameters.host, interfaceType,
                                                              Utilities
                                                                      .formatDuration(lDuration, 0)),
                                                String.format(Display.getMessagesResourceBundle().getString("email.content"), lActiveServerParameters.host, lActiveServerParameters
                                                                      .remoteIp,
                                                              interfaceType, addressType,
                                                              LocaleUtilities.getInstance().getDateFormat().format(lastLostConnectionDate),
                                                              LocaleUtilities.getInstance().getTimeFormat().format(lastLostConnectionDate.getTime()),
                                                              LocaleUtilities.getInstance().getDateFormat().format(lastRecoveryConnectionDate),
                                                              LocaleUtilities.getInstance().getTimeFormat().format(lastRecoveryConnectionDate.getTime()),
                                                              Utilities.formatDuration(lDuration, 0)));
                                        lSendMailSuccessful = true;
                                    } catch (Exception e) {
                                        if (lSendRetries <= Constants.MAX_RETRIES_SEND_MAIL) {
                                            Display.getLogger().warn(Display.getMessagesResourceBundle().getString("log.email.sendMailRetry"));
                                        } else {
                                            Display.getLogger().error(Display.getMessagesResourceBundle().getString("log.email.sendMailNoMoreRetry"));
                                        }
                                        Utilities.sleep(1000);
                                    }
                                }

                                refreshGeneralDisplay(GeneralDisplayNotification.RECOVER_CONNECTION);

                            }

                            connectionLost = false;
                            if (alertAllowed()) GlobalMonitoring.getInstance().changeJobState(this, hostState);

                        }

                        // Force notify global monitoring after a connection recovery
                        if (forceNotifyCurrentJobState && alertAllowed()) {
                            GlobalMonitoring.getInstance().changeJobState(this, hostState);
                            forceNotifyCurrentJobState = false;
                        }


                    }
                }

                refreshDisplay();

                // Wait for next period
                long lDelay = lActiveServerParameters.pollingPeriod - Instant.now().toEpochMilli() + lPollingStartTime;
                final long MIN_WAIT_TIME = 500;
                Utilities.sleep((lDelay >= MIN_WAIT_TIME) ? lDelay : MIN_WAIT_TIME);

            } catch (Exception e) {
                // Log the error
                Display.getLogger().error(
                        String.format(
                                Display.getMessagesResourceBundle().getString("log.monitoringJob.error"),
                                lActiveServerParameters.remoteHostname, lActiveServerParameters.remoteIp, lInterfaceName, lLocalIp, Utilities.getStackTrace(e)));
                Utilities.sleep(1000);
            }

        }
    }

    // GETTERS

    /**
     * Gets monitoring jobs
     *
     * @return Monitoring jobs
     */
    public static List<MonitoringJob> getMonitoringJobs() {
        return monitoringJobs;
    }

    /**
     * Gets monitoring jobs per address type
     *
     * @return Monitoring jobs per address type
     */
    public static HashMap<EnumTypes.AddressType, HashMap<EnumTypes.InterfaceType, MonitoringJob>> getMonitoringJobsByType() {
        return monitoringJobsByType;
    }

    /**
     * Gets number running monitoring jobs
     *
     * @return Number of monitoring jobs
     */
    public static int getRunningCount() {
        return runningCount;
    }

    /**
     * Gets total number of created monitoring jobs
     *
     * @return Total number of jobs
     */
    public static int getTotalCount() {
        return totalCount;
    }

    /**
     * Gets monitoring job address type (wan, lan, ...)
     *
     * @return Job address type
     */
    public EnumTypes.AddressType getAddressType() {
        return addressType;
    }

    /**
     * Gets monitoring job interface type (wifi, eth, ...)
     *
     * @return Job interface type
     */
    public EnumTypes.InterfaceType getInterfaceType() {
        return interfaceType;
    }

    /**
     * Gets job remote host name
     *
     * @return Job remote host name
     */
    public String getRemoteHostname() {
        return serverParameters.get(activeServer).remoteHostname;
    }

    /**
     * Gets job remote ip
     *
     * @return Job remote ip
     */
    public String getRemoteIp() {
        return serverParameters.get(activeServer).remoteIp;
    }

    /**
     * Indicates if current job is running
     *
     * @return true if current job is running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Indicates if current job is paused
     *
     * @return true if current job is paused, false otherwise
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Indicates if current job email is audibleEnabled
     *
     * @return true if current job is paused, false otherwise
     */
    public boolean isEmailEnabled() {
        return email.isEnabled();
    }

    /**
     * Gets the summary controller
     *
     * @return controller
     */
    public MonitoringJobView getController() {
        return controller;
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
     * Forces to notify current job state to global monitoring
     */
    public void forceNotifyCurrentJobState() {
        forceNotifyCurrentJobState = true;
    }

    /**
     * Displays a message
     * @param aInMessage       Message to be displayed
     * @param aInMessageLevel  Message level
     */
    public void displayMessage(String aInMessage, EnumTypes.MessageLevel aInMessageLevel) {
        messages.add(new Message(aInMessage, aInMessageLevel));
        refreshDisplay();
    }

    /**
     * Resets all messages
     */
    public void clearConsole() {
        messages.clear();
        refreshDisplay();
        Display.getLogger().debug(String.format(Display.getMessagesResourceBundle().getString("log.monitoringJob.clearConsole"), addressType, interfaceType));
    }

    // PRIVATE METHODS

    /**
     * Check if a server is reachable using either tcp connection or isReachable, depending if a tcp port is available or not
     *
     * @param aInServerParameters Parameters of the servers for which reachability must be checked
     * @return Server reachability information
     */
    private ServerReachability checkServerReachability(ServerParameters aInServerParameters) {

        ServerReachability lServerReachability = new ServerReachability();

        // Use tcp sockets to test reachability if a port has been found, otherwise use standard isReachable method
        lServerReachability.retries = (aInServerParameters.port != 0)
                                      ? Network.isReachable(aInServerParameters.remoteIp, aInServerParameters.port, networkInterface, aInServerParameters.timeout,
                                                            aInServerParameters.maxRetries, useProxy)
                                      : Network.isReachable(aInServerParameters.remoteIp, networkInterface, aInServerParameters.timeout, aInServerParameters.maxRetries,
                                                            aInServerParameters.pollingPeriod);
        lServerReachability.isReachable = (lServerReachability.retries <= aInServerParameters.maxRetries);

        return lServerReachability;
    }

    /**
     * Checks if a job running monitoring same address type is reachable
     * @return true if another job is reachable, false otherwise
     */
    private boolean isJobWithSameAddressTypeReachable() {

        for (EnumTypes.InterfaceType lInterfaceType: EnumTypes.InterfaceType.values()) {
            if (monitoringJobsByType.get(addressType).get(lInterfaceType) != null &&
                !monitoringJobsByType.get(addressType).get(lInterfaceType).hostState.equals(EnumTypes.HostState.UNREACHABLE)) return true;
        }
        return false;
    }

    /**
     * Check if email is allowed for current job depending on saved state of the email button and if recipient list and smtp servers are defined
     * @return true if email is allowed, false otherwise
     */
    private boolean isEmailAllowed() {
        return
                Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations().size() != 0 &&
                !Configuration.getCurrentConfiguration().getEmailConfiguration().getRecipientList().isEmpty();
    }

    /**
     * Refreshes the display with the information related to the server to be displayed
     */
    private void refreshDisplay() {

        // Refresh only if display is audibleEnabled and this job is active
        if (displayGraphicalInterface) {

            Platform.runLater(() -> {

                ServerParameters lServerParameters = serverParameters.get(activeServer);

                // Active server information display
                controller.setHostNameLabel(lServerParameters.remoteHostname);
                controller.setHostIpLabel('(' + lServerParameters.remoteIp + ')');
                controller.setHostIpLabel('(' + lServerParameters.remoteIp + ')');
                controller.setPingsCount(pingsCount, consecutiveLostPingsCount, lostPingsCount);

                controller.setHostTooltip(lServerParameters.pollingPeriod, lServerParameters.timeout, lServerParameters.maxRetries, retriesCount);

                controller.changeState(hostState);

                // Active interface information display
                controller.setInterfaceType(interfaceType);

                // Ping information display
                controller.setPingsCount(pingsCount, consecutiveLostPingsCount, lostPingsCount);
                controller.setRoundTrip(roundTrip);
                controller.setRoundTripStats(minRoundTrip, maxRoundTrip, (reachableCount == 0) ? 0d : (double) totalRoundTrip / reachableCount);


                // Connection information display
                controller.setLostConnectionsCount(lostConnectionsCount);
                controller.setLostConnectionDuration(ongoingLossDuration, totalLossDuration + ongoingLossDuration, lostConnectionsCount);
                controller.setLastTwoLostConnectionsIntervalLabel(lostConnectionInterval);

                // Set job states image
                controller.setPlayPauseButtonImageView(!paused);
                if (isEmailAllowed()) controller.setEmailButtonImageView(email.isEnabled());

                // Change state images
                controller.setInterfaceTypeImageView(!hostState.equals(EnumTypes.HostState.UNREACHABLE));
                controller.setAddressTypeStateImageView(isJobWithSameAddressTypeReachable());


                // Messages
                controller.replaceMessages(messages);

            });

        }

    }

    /**
     * Refreshes general display
     *
     * @param aInGeneralDisplayNotification Type of refresh to perform
     */
    private void refreshGeneralDisplay(GeneralDisplayNotification aInGeneralDisplayNotification) {

        if (displayGraphicalInterface) {

            Platform.runLater(() -> {
                switch (aInGeneralDisplayNotification) {
                    case INCREMENT_PING_COUNT:
                        controller.getCat().getController().incrementPingsCount();
                        break;
                    case INCREMENT_LOST_PING_COUNT:
                        controller.getCat().getController().incrementLostPingsCount();
                        break;
                    case LOSE_CONNECTION:
                        controller.getCat().getController().loseConnection(new Date(), serverParameters.get(activeServer).remoteHostname, addressType, interfaceType);
                        break;
                    case RECOVER_CONNECTION:
                        controller.getCat().getController().recoverConnection();
                        break;
                    default:
                        break;
                }
            });

        }

    }

    /**
     * Indicates if alerts can be displayed depending on the monitored interface and the users preference
     *
     * @return true if alerts can be displayed, false otherwise
     */
    private boolean alertAllowed() {
        return (networkInterfaceIndex == 0) || (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().getAlertIfSecondaryIsDown());
    }

    /**
     * Builds state property name ins states properties file
     * @param aInState State for which property name must be built
     * @return State property name
     */
    private String BuildStatePropertyName(String aInState) {
        return addressType.name() + '.' + interfaceType.name() + '.' + aInState;
    }

    // PUBLIC METHODS

    /**
     * Switches primary and backup server
     *
     * @param aInActiveServer Active server index
     * @param aInSwitchType   Switch type: startup, automatic or manual
     */
    public void changeActiveServer(int aInActiveServer, String aInSwitchType) {
        if (serverParameters.get(aInActiveServer) == null) {
            if (displayGraphicalInterface) controller.setActiveServerSliderValue(activeServer);
        } else {
            //String lServerType = (aInActiveServer == 0) ? "primary" : "backup";
            if (!aInSwitchType.equals("startup")) serverType = serverType.toggle();
            String lInterfaceName = networkInterface.getName();
            String lLocalIp = Network.getNetworkInterfaceInetAddress(networkInterface, Network.isIPv4Address(serverParameters.get(activeServer).remoteIp)).getHostAddress();
            Display.getLogger().info(
                    String.format(
                            Display.getMessagesResourceBundle().getString("log.monitoringJob." + serverType.toString() + "." + aInSwitchType),
                            addressType, lInterfaceName, lLocalIp, serverParameters.get(aInActiveServer).remoteHostname, serverParameters.get(aInActiveServer).remoteIp));
            messages.add(
                    new Message(String.format(
                            Display.getViewResourceBundle().getString("monitoringJob.message." + serverType.toString() + "." + aInSwitchType),
                            serverParameters.get(aInActiveServer).remoteHostname, serverParameters.get(aInActiveServer).remoteIp), EnumTypes.MessageLevel.INFO, new Date()));
            activeServer = aInActiveServer;
            if (displayGraphicalInterface) controller.setActiveServerSliderValue(activeServer);
            refreshDisplay();
            if (displayGraphicalInterface)
                controller.getCat().getController().setMonitoringJobTooltip(addressType, networkInterfaceIndex + 1, networkInterface, serverParameters.get(activeServer).remoteIp);
        }
    }

    /**
     * Pauses current job and save it in the configuration
     */
    public void pause() {
        paused = true;
        controller.setPlayPauseButtonImageView(false);
        States.getInstance().saveValue(BuildStatePropertyName(Constants.PAUSE_STATE), paused);
        Display.getLogger().debug(String.format(Display.getMessagesResourceBundle().getString("log.monitoringJob.pause"), addressType, interfaceType));
    }

    /**
     * Resumes current job and save it in the configuration
     */
    public void resume() {
        paused = false;
        controller.setPlayPauseButtonImageView(true);
        States.getInstance().saveValue(BuildStatePropertyName(Constants.PAUSE_STATE), paused);
        Display.getLogger().debug(String.format(Display.getMessagesResourceBundle().getString("log.monitoringJob.resume"), addressType, interfaceType));
    }

    /**
     * Enables email for current job and save it in the configuration
     */
    public void enableEmail() {
        email.enable();
        States.getInstance().saveValue(BuildStatePropertyName(Constants.SEND_MAIL_STATE), true);
        controller.setEmailButtonImageView(email.isEnabled());
        Display.getLogger().debug(String.format(Display.getMessagesResourceBundle().getString("log.monitoringJob.enableEmail"), addressType, interfaceType));
    }

    /**
     * Disables email for current job and save it in the configuration
     */
    public void disableEmail() {
        email.disable();
        States.getInstance().saveValue(BuildStatePropertyName(Constants.SEND_MAIL_STATE), false);
        controller.setEmailButtonImageView(email.isEnabled());
        Display.getLogger().debug(String.format(Display.getMessagesResourceBundle().getString("log.monitoringJob.disableEmail"), addressType, interfaceType));
    }

    /**
     * Resets statistics for current job
     */
    public void resetStatistics() {

        Display.getLogger().info(String.format(Display.getMessagesResourceBundle().getString("log.monitoringJob.resetStatistics"), addressType, interfaceType));
        pingsCount = 0;
        lostPingsCount = 0;
        retriesCount = 0;
        consecutiveLostPingsCount = 0;
        lostConnectionsCount = 0;
        previousLostConnectionDate = null;
        lastLostConnectionDate = null;
        lastRecoveryConnectionDate = null;
        ongoingLossDuration = 0;
        totalLossDuration = 0;
        totalRoundTrip = 0;
        minRoundTrip = Integer.MAX_VALUE;
        maxRoundTrip = 0;
        reachableCount = 0;
        connectionLost = false;
        messages.clear();
        messages.add(new Message(Display.getViewResourceBundle().getString("monitoringJob.message.resetStatistics"), EnumTypes.MessageLevel.INFO, new Date()));

        controller.resetFirstDisplay();
        refreshDisplay();

    }

}
