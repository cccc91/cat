package cclerc.cat.Configuration;

import cclerc.services.EnumTypes;
import cclerc.services.Network;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.*;

/**
 * Internal class for monitoring configuration
 */
public class MonitoringConfiguration extends AbstractConfiguration {

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(Arrays.asList("checkPreferredServerInterval"));

    // Accessed by introspection
    protected static final Integer DEFAULT_CHECK_PREFERRED_SERVER_INTERVAL = 5;

    protected Integer checkPreferredServerInterval;
    private NetworkInterfacesConfiguration networkInterfacesConfiguration;
    private MonitoringDefaultsConfiguration monitoringDefaultsConfiguration;
    private NetworkConfiguration wan;
    private NetworkConfiguration lan;

    // SETTERS

    public void setCheckPreferredServerInterval(String aInCheckPreferredServerInterval) throws NumberFormatException {
        if ((aInCheckPreferredServerInterval != null) && !Integer.valueOf(aInCheckPreferredServerInterval).equals(checkPreferredServerInterval)) {
            checkPreferredServerInterval = Integer.valueOf(aInCheckPreferredServerInterval);
        }
    }

    private void setNetworkInterfacesConfiguration(NetworkInterfacesConfiguration aInNetworkInterfacesConfiguration) throws NullPointerException {
        if (aInNetworkInterfacesConfiguration == null) throw new NullPointerException();
        networkInterfacesConfiguration = aInNetworkInterfacesConfiguration;
        networkInterfacesConfiguration.setParentConfiguration(this);
    }

    public void setMonitoringDefaultsConfiguration(MonitoringDefaultsConfiguration aInMonitoringDefaultsConfigurations) {
        monitoringDefaultsConfiguration = aInMonitoringDefaultsConfigurations;
        monitoringDefaultsConfiguration.setParentConfiguration(this);
        super.setDefaults(MonitoringDefaultsConfiguration.SERVER_ATTRIBUTE_NAMES, monitoringDefaultsConfiguration, ServerConfiguration.class);
    }

    public void setWan(NetworkConfiguration aInWan) {
        wan = aInWan;
        wan.setParentConfiguration(this);
    }

    public void setLan(NetworkConfiguration aInLan) {
        lan = aInLan;
        lan.setParentConfiguration(this);
    }

    // GETTERS


    public Integer getCheckPreferredServerInterval() {
        return checkPreferredServerInterval;
    }

    public NetworkInterfacesConfiguration getNetworkInterfacesConfiguration() {
        return networkInterfacesConfiguration;
    }

    public MonitoringDefaultsConfiguration getMonitoringDefaultsConfiguration() {
        return monitoringDefaultsConfiguration;
    }

    public NetworkConfiguration getWan() {
        return wan;
    }

    public NetworkConfiguration getLan() {
        return lan;
    }

    public NetworkConfiguration getNetworkConfiguration(EnumTypes.AddressType aInAddressType) {
        return (aInAddressType.equals(EnumTypes.AddressType.WAN)) ? getWan() : getLan();
    }

    // CONSTRUCTORS

    public MonitoringConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes (ignore element on error)
        super("monitoringConfiguration", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, true);

        if (aInElement != null) {

            // Add network interfaces element
            Element lNetworkInterfacesElement = aInElement.getChild("networkInterfaces");
            if (lNetworkInterfacesElement != null)
                setNetworkInterfacesConfiguration(new NetworkInterfacesConfiguration(aInConfiguration, aInConfigurationFile, aInDisplayError, lNetworkInterfacesElement));

            // Add defaults element
            Element lDefaultElement = aInElement.getChild("defaults");
            if (lDefaultElement != null)
                setMonitoringDefaultsConfiguration(new MonitoringDefaultsConfiguration(aInConfiguration, aInConfigurationFile, aInDisplayError, lDefaultElement));

            // Add wan and lan network elements
            Element lWan = aInElement.getChild("wan");
            if (lWan != null) setWan(new NetworkConfiguration(aInConfiguration, aInConfigurationFile, aInDisplayError, lWan));
            Element lLan = aInElement.getChild("lan");
            if (lLan != null) setLan(new NetworkConfiguration(aInConfiguration, aInConfigurationFile, aInDisplayError, lLan));

        }

    }

    public MonitoringConfiguration(Configuration aInConfiguration) {
        super("monitoringConfiguration", aInConfiguration, "monitoring", ATTRIBUTE_NAMES);
        setMonitoringDefaultsConfiguration(new MonitoringDefaultsConfiguration(aInConfiguration));
        setNetworkInterfacesConfiguration(new NetworkInterfacesConfiguration(aInConfiguration));
        setWan(new NetworkConfiguration(aInConfiguration, "wan"));
    }

    // METHODS

    public void addWan(Configuration aInConfiguration) {
        setWan(new NetworkConfiguration(aInConfiguration, "wan"));
    }

    public void addLan(Configuration aInConfiguration) {
        setLan(new NetworkConfiguration(aInConfiguration, "lan"));
    }

    @Override
    public Element save() {

        Element lMonitoringConfigurationElement = super.save();

        // Save networkInterfacesConfiguration
        if (networkInterfacesConfiguration != null) lMonitoringConfigurationElement.addContent(networkInterfacesConfiguration.save());

        // Save monitoringDefaultsConfiguration
        if (monitoringDefaultsConfiguration != null) lMonitoringConfigurationElement.addContent(monitoringDefaultsConfiguration.save());

        // Save networkConfiguration
        if (wan != null) {
            // Don't save invalid wan configurations
            Element lWanElement = wan.save();
            if (lWanElement != null) lMonitoringConfigurationElement.addContent(lWanElement);
        }
        if (lan != null) {
            // Don't save invalid lan configurations
            Element lLanElement = lan.save();
            if (lLanElement != null) lMonitoringConfigurationElement.addContent(lLanElement);
        }

        return lMonitoringConfigurationElement;

    }

    public boolean isSameAs(MonitoringConfiguration aInMonitoringConfiguration) {

        return super.isSameAs(aInMonitoringConfiguration) &&
               monitoringDefaultsConfiguration.isSameAs(aInMonitoringConfiguration.monitoringDefaultsConfiguration) &&
               ((networkInterfacesConfiguration == null && aInMonitoringConfiguration.getNetworkInterfacesConfiguration() == null) ||
                (networkInterfacesConfiguration != null && networkInterfacesConfiguration.isSameAs(aInMonitoringConfiguration.getNetworkInterfacesConfiguration()))) &&
               ((wan == null && aInMonitoringConfiguration.getWan() == null) || (wan != null && wan.isSameAs(aInMonitoringConfiguration.getWan()))) &&
               ((lan == null && aInMonitoringConfiguration.getLan() == null) || (lan != null && lan.isSameAs(aInMonitoringConfiguration.getLan())));

    }

}
