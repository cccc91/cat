package cclerc.cat.Configuration;

import cclerc.services.Display;
import cclerc.services.EnumTypes;
import cclerc.services.Network;
import cclerc.services.Utilities;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

import java.util.*;

/**
 * Internal class for network interfaces configuration
 */
public class NetworkInterfacesConfiguration extends AbstractConfiguration {

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(Arrays.asList("alertIfSecondaryIsDown"));

    // Accessed by introspection
    protected static final Boolean DEFAULT_ALERT_IF_SECONDARY_IS_DOWN = true;

    protected Boolean alertIfSecondaryIsDown;
    private List<NetworkInterfaceConfiguration> networkInterfaceConfigurations = new ArrayList<>();

    // SETTERS

    public void addNetworkInterfaceConfiguration(NetworkInterfaceConfiguration aInNetworkInterfaceConfiguration) {
        networkInterfaceConfigurations.add(aInNetworkInterfaceConfiguration);
        aInNetworkInterfaceConfiguration.setParentConfiguration(this);
    }

    public void removeAllNetworkInterfaceConfigurations() {
        networkInterfaceConfigurations.clear();
    }

    public void removeNetworkInterfaceConfiguration(NetworkInterfaceConfiguration aInNetworkInterfaceConfiguration) {
        networkInterfaceConfigurations.remove(aInNetworkInterfaceConfiguration);
    }

    public void removeNetworkInterfaceConfiguration(int lIndex) {
        networkInterfaceConfigurations.remove(lIndex);
    }

    public void setAlertIfSecondaryIsDown(String aInAlertIfSecondaryIsDown) {
        if ((aInAlertIfSecondaryIsDown != null) && !Boolean.valueOf(aInAlertIfSecondaryIsDown).equals(alertIfSecondaryIsDown)) {
            alertIfSecondaryIsDown = Boolean.valueOf(aInAlertIfSecondaryIsDown);
        }
    }

    // GETTERS

    public Boolean getAlertIfSecondaryIsDown() {
        return alertIfSecondaryIsDown;
    }

    public List<NetworkInterfaceConfiguration> getNetworkInterfaceConfigurations() {
        return networkInterfaceConfigurations;
    }

    // CONSTRUCTORS

    public NetworkInterfacesConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes
        super("networkInterfaceConfiguration", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, false);

        if (aInElement != null) {

             // Add all networkInterface elements
            for (Element lNetworkInterfaceConfigurationElement: aInElement.getChildren("networkInterface")) {
                // Add network interface only if it contains no error
                try {
                    NetworkInterfaceConfiguration lNetworkInterfaceConfiguration =
                            new NetworkInterfaceConfiguration(aInConfiguration, aInConfigurationFile, aInDisplayError, lNetworkInterfaceConfigurationElement);
                    addNetworkInterfaceConfiguration(lNetworkInterfaceConfiguration);
                } catch (Exception e) {
                    // If there's an error in a network interface, program must be stopped
                    throw e;
                }
            }

            // Sort network interfaces by priority
            Collections.sort(networkInterfaceConfigurations, new Comparator<NetworkInterfaceConfiguration>() {
                @Override
                public int compare(NetworkInterfaceConfiguration o1, NetworkInterfaceConfiguration o2) {
                    return o1.getPriority().compareTo(o2.getPriority());
                }
            });

            // Reset priority starting to 1
            int lPriority = 1;
            for (NetworkInterfaceConfiguration lNetworkInterfaceConfiguration: networkInterfaceConfigurations) {
                lNetworkInterfaceConfiguration.priority = lPriority++;
            }

        }

    }

    public NetworkInterfacesConfiguration(Configuration aInConfiguration) {
        super("networkInterfaceConfiguration", aInConfiguration, "networkInterfaces", ATTRIBUTE_NAMES);
        alertIfSecondaryIsDown = DEFAULT_ALERT_IF_SECONDARY_IS_DOWN;
        networkInterfaceConfigurations.add(new NetworkInterfaceConfiguration(aInConfiguration));
    }

    // METHODS

    @Override
    public Element save() {

        Element lNetworkInterfaceConfigurationElement = super.save();

        // Save all networkInterfaceConfigurations
        for (NetworkInterfaceConfiguration lNetworkInterfaceConfiguration : networkInterfaceConfigurations) {
            lNetworkInterfaceConfigurationElement.addContent(lNetworkInterfaceConfiguration.save());
        }

        return lNetworkInterfaceConfigurationElement;

    }

    public boolean isSameAs(NetworkInterfacesConfiguration aInNetworkInterfacesConfiguration) {

        // Compare networkInterfaceConfigurations - If they have not the same size, configurations are not the same. If they have, compare each network interface configuration
        boolean lSameNetworkInterfaceConfigurations = (networkInterfaceConfigurations == null && aInNetworkInterfacesConfiguration == null) ||
                (networkInterfaceConfigurations != null && aInNetworkInterfacesConfiguration != null &&
                        networkInterfaceConfigurations.size() == aInNetworkInterfacesConfiguration.getNetworkInterfaceConfigurations().size());
        if (lSameNetworkInterfaceConfigurations) {
            for (NetworkInterfaceConfiguration lLocalNetworkInterfaceConfiguration : networkInterfaceConfigurations) {
                boolean lFound = false;
                for (NetworkInterfaceConfiguration lRemoteNetworkInterfaceConfigurations : aInNetworkInterfacesConfiguration.getNetworkInterfaceConfigurations()) {
                    if (lLocalNetworkInterfaceConfiguration.isSameAs(lRemoteNetworkInterfaceConfigurations)) {
                        lFound = true;
                        break;
                    }
                }
                if (!lFound) {
                    lSameNetworkInterfaceConfigurations = false;
                    break;
                }
            }
        }

        return super.isSameAs(aInNetworkInterfacesConfiguration) && lSameNetworkInterfaceConfigurations;

    }

    /**
     * Finds networtk interface configuration corresponding to an interface type
     * @param aInInterfaceType
     * @return
     */
    public NetworkInterfaceConfiguration findNetworkInterfaceConfiguration(EnumTypes.InterfaceType aInInterfaceType) {
        for (NetworkInterfaceConfiguration lNetworkInterfaceConfiguration: networkInterfaceConfigurations) {
            if (Network.getInterfaceType(lNetworkInterfaceConfiguration.getName()).equals(aInInterfaceType)) return lNetworkInterfaceConfiguration;
        }
        return  null;
    }

}
