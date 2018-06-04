package cclerc.cat.Configuration;

import org.jdom2.Element;

import java.util.*;

/**
 * Internal class for network configuration
 */
public class NetworkConfiguration extends AbstractConfiguration {

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(Arrays.asList("useProxy"));

    // Accessed by introspection
    protected static final Boolean DEFAULT_USE_PROXY = false;

    protected Boolean useProxy;
    private ServerConfiguration preferredServer;
    private ServerConfiguration backupServer;

    // SETTERS

    public void setUseProxy(String aInUseProxy) {
        if ((aInUseProxy != null) && !Boolean.valueOf(aInUseProxy).equals(useProxy)) {
            useProxy = Boolean.valueOf(aInUseProxy);
        }
    }

    public void setPreferredServer(ServerConfiguration aInPreferredServer) {
        preferredServer = aInPreferredServer;
        preferredServer.setParentConfiguration(this);
    }

    public void setBackupServer(ServerConfiguration aInBackupServer) {
        backupServer = aInBackupServer;
        backupServer.setParentConfiguration(this);
    }

    // GETTERS

    public Boolean getUseProxy() {
        return useProxy;
    }

    public ServerConfiguration getPreferredServer() {
        return preferredServer;
    }

    public ServerConfiguration getBackupServer() {
        return backupServer;
    }

    public List<ServerConfiguration> getServers() {
        List<ServerConfiguration> lServers = new ArrayList<>();
        if (preferredServer != null) lServers.add(preferredServer);
        if (backupServer != null) lServers.add(backupServer);
        return lServers;
    }

    public ServerConfiguration getServer(int aInServerIndex) {
        return aInServerIndex == 0 ? preferredServer : backupServer;
    }

    // CONSTRUCTORS

    public NetworkConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes (ignore element on error)
        super("networkConfiguration", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, true);

        if (aInElement != null) {

            // Add preferred and backup server elements
            Element lPreferredServer = aInElement.getChild("preferredServer");
            if (lPreferredServer != null)  setPreferredServer(new ServerConfiguration(aInConfiguration, aInConfigurationFile, aInDisplayError, lPreferredServer));
            Element lBackupServer = aInElement.getChild("backupServer");
            if (lBackupServer != null)  setBackupServer(new ServerConfiguration(aInConfiguration, aInConfigurationFile, aInDisplayError, lBackupServer));

        }

    }

    public NetworkConfiguration(Configuration aInConfiguration, String aInElementName) {
        super("networkConfiguration", aInConfiguration, aInElementName, ATTRIBUTE_NAMES);
        useProxy = DEFAULT_USE_PROXY;
        setPreferredServer(new ServerConfiguration(aInConfiguration, "preferredServer"));
    }

    // METHODS

    public void addBackupServer(Configuration aInConfiguration) {
        setBackupServer(new ServerConfiguration(aInConfiguration, "backupServer"));
    }

    @Override
    public Element save() {

        Element lNetworkConfiguration = super.save();

        int lInvalidServersCount = 0;

        // Save serverConfiguration
        if (preferredServer != null) {
            // Don't save preferred servers which hostname is empty
            Element lPreferredServerElement = preferredServer.save();
            if (lPreferredServerElement != null) {
                lNetworkConfiguration.addContent(lPreferredServerElement);
            } else {
                lInvalidServersCount++;
            }
        }
        if (backupServer != null) {
            // Don't save backup servers which hostname is empty
            Element lBackupServerElement = backupServer.save();
            if (lBackupServerElement != null) {
                lNetworkConfiguration.addContent(lBackupServerElement);
            } else {
                lInvalidServersCount++;
            }
        }

        // If both preferred and backup servers are null, configuration is invalid
        if (lInvalidServersCount < 2) {
            return lNetworkConfiguration;
        } else {
            return null;
        }

    }

    public boolean isSameAs(NetworkConfiguration aInNetworkConfiguration) {

        if (aInNetworkConfiguration == null) {
            return (preferredServer== null || preferredServer.getHostname() == null || preferredServer.getHostname().equals("")) &&
                   (backupServer== null || backupServer.getHostname() == null || backupServer.getHostname().equals(""));
        } else {
            return super.isSameAs(aInNetworkConfiguration) &&
                   ((preferredServer == null && (aInNetworkConfiguration.getPreferredServer() == null || aInNetworkConfiguration.getPreferredServer().getHostname().equals(""))) ||
                    ((preferredServer == null || preferredServer.getHostname().equals("")) && (aInNetworkConfiguration.getPreferredServer() == null)) ||
                    (preferredServer != null && preferredServer.isSameAs(aInNetworkConfiguration.getPreferredServer()))) &&
                   ((backupServer == null && (aInNetworkConfiguration.getBackupServer() == null || aInNetworkConfiguration.getBackupServer().getHostname().equals(""))) ||
                    ((backupServer == null || backupServer.getHostname().equals("")) && (aInNetworkConfiguration.getBackupServer() == null)) ||
                    (backupServer != null && backupServer.isSameAs(aInNetworkConfiguration.getBackupServer())));
        }

    }

}
