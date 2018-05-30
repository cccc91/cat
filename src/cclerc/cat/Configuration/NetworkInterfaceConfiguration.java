package cclerc.cat.Configuration;

import cclerc.services.Display;
import cclerc.services.Network;
import org.jdom2.Element;

import java.net.NetworkInterface;
import java.util.*;

/**
 * Internal class for network interface configuration
 */
public class NetworkInterfaceConfiguration extends AbstractConfiguration {

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(Arrays.asList("name", "priority"));
    private final static HashMap<String, NetworkInterface> NETWORK_INTERFACE_LIST = Network.buildNetworkInterfaceList();

    protected String name;
    protected Integer priority;

    // SETTERS

    public void setName(String aInName) throws IllegalArgumentException {
        name = findNetworkInterface(aInName);
    }

    public void setPriority(String aInPriority) throws NumberFormatException {
        if ((aInPriority != null) && !Integer.valueOf(aInPriority).equals(priority)) {
            priority = Integer.valueOf(aInPriority);
        }
    }

    public void incrementPriority() {
        priority++;
    }

    public void decrementPriority() {
        priority--;
    }

    public void changePriority(Integer aInPriority) {
        priority = aInPriority;
    }

    // GETTERS

    public String getName() {
        return name;
    }

    public Integer getPriority() {
        return priority;
    }

    // CONSTRUCTORS

    public NetworkInterfaceConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes (ignore element on error)
        super("networkInterfaceConfiguration", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, true);

    }

    public NetworkInterfaceConfiguration(Configuration aInConfiguration) {

        super("networkInterfaceConfiguration", aInConfiguration, "networkInterface", ATTRIBUTE_NAMES);
        // Add first found interface
        priority = 1;
        name = Network.buildNetworkInterfacesDescriptionList().keySet().iterator().next();

    }

    public NetworkInterfaceConfiguration(Configuration aInConfiguration, int aInPriority, String aInName) {
        super("networkInterfaceConfiguration", aInConfiguration, "networkInterface", ATTRIBUTE_NAMES);
        priority = aInPriority;
        name = aInName;
    }

    // PRIVATE METHODS

    private String findNetworkInterface(String aInNetworkInterface) throws IllegalArgumentException {

        String lNetworkInterface = "";
        if (aInNetworkInterface != null) {
            lNetworkInterface = findMatchingNetworkInterface(aInNetworkInterface);
        } else {
            List<String> lNetworkInterfaceList = new ArrayList<>(Arrays.asList("eth", "wifi"));
            for (String lInterfaceName: lNetworkInterfaceList) {
                lNetworkInterface = findMatchingNetworkInterface(lInterfaceName);
                if (!lNetworkInterface.equals("")) break;
            }
        }
        if (lNetworkInterface.equals(""))  throw new IllegalArgumentException(new Throwable(aInNetworkInterface));
        else return lNetworkInterface;
    }

    private String findMatchingNetworkInterface(String aInNetworkInterface) {

        String lMatchingNetworkInterface = "";
        if (!NETWORK_INTERFACE_LIST.containsKey(aInNetworkInterface)) {

            // Try to find another interface of the same type
            for (String lNetworkInterface: NETWORK_INTERFACE_LIST.keySet()) {
                if (aInNetworkInterface.replaceAll("\\d", "").equals(lNetworkInterface.replaceAll("\\d", ""))) {
                    lMatchingNetworkInterface = lNetworkInterface;
                    if (displayError) {
                        Display.getLogger().warn(String.format(Display.getMessagesResourceBundle().getString("log.configuration.interfaceReplaced"), aInNetworkInterface, configurationFile, lNetworkInterface));
                    }
                    break;
                }
            }

        } else {
            lMatchingNetworkInterface = aInNetworkInterface;
        }

        return lMatchingNetworkInterface;

    }

    // PUBLIC METHODS

    @Override
    public Element save() {
        return super.save("name");
    }

}
