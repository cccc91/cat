package cclerc.cat.model;

import cclerc.cat.Configuration.Configuration;
import cclerc.cat.Configuration.NetworkInterfaceConfiguration;
import cclerc.services.Network;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.net.NetworkInterface;
import java.util.HashMap;

public class Interface {

    private static HashMap<String, String> networkInterfacesDescriptionsList = Network.buildNetworkInterfacesDescriptionList();

    private IntegerProperty priority;
    private StringProperty name;
    private StringProperty displayedName;
    private StringProperty ipv4;
    private StringProperty ipv6;

    // GETTERS

    public int getPriority() {
        return priority.get();
    }

    public String getName() {
        return name.get();
    }

    public String getDisplayedName() {
        return displayedName.get();
    }

    public String getIpv4() {
        return ipv4.get();
    }

    public String getIpv6() {
        return ipv6.get();
    }

    // PROPERTY GETTERS

    public IntegerProperty priorityProperty() {
        return priority;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty displayedNameProperty() {
        return displayedName;
    }

    public StringProperty displayedIpv4Property() {
        return ipv4;
    }

    public StringProperty displayedIpv6Property() {
        return ipv6;
    }

    // SETTERS

    public void setPriority(int aInPriority) {
        priority.set(aInPriority);
    }

    public void incrementPriority() {
        priority.set(priority.get() + 1);
    }

    public void decrementPriority() {
        priority.set(priority.get() - 1);
    }

    public void setName(String aInName) {
        name.set(aInName);
    }

    public void setDisplayedName(String aInDescription) {
        displayedName.set(aInDescription);
    }

    public void setIpv4(String aInIpv4) {
        ipv4.set(aInIpv4);
    }

    public void setIpv6(String aInIpv6) {
        ipv6.set(aInIpv6);
    }

    // CONSTRUCTORS

    /**
     * Builds the displayed interface from a network interface defined in the network interfaces configuration
     * @param aInNetworkInterfaceConfiguration Network interface configuration
     */
    public Interface(NetworkInterfaceConfiguration aInNetworkInterfaceConfiguration) {

        NetworkInterface lNetworkInterface = Network.buildNetworkInterfaceList().get(aInNetworkInterfaceConfiguration.getName());

        priority = new SimpleIntegerProperty(aInNetworkInterfaceConfiguration.getPriority());
        name = new SimpleStringProperty(aInNetworkInterfaceConfiguration.getName());
        displayedName = new SimpleStringProperty(networkInterfacesDescriptionsList.get(aInNetworkInterfaceConfiguration.getName()));
        try {
            ipv4 = new SimpleStringProperty(Network.getNetworkInterfaceInet4Address(lNetworkInterface).getHostAddress());
        } catch (Exception e) {
            ipv4 = new SimpleStringProperty("");
        }
        try {
            ipv6 = new SimpleStringProperty(Network.getNetworkInterfaceInet6Address(lNetworkInterface).getHostAddress());
        } catch (Exception e) {
            ipv6 = new SimpleStringProperty("");
        }

    }

    /**
     * Builds the displayed interface from a physical network interface on the machine
     * @param aInNetworkInterface Network interface on the machine
     * @param aInPriority         Priority to give to this interface
     */
    public Interface(NetworkInterface aInNetworkInterface, int aInPriority) {

        priority = new SimpleIntegerProperty(aInPriority);
        name = new SimpleStringProperty(aInNetworkInterface.getName());
        displayedName = new SimpleStringProperty(aInNetworkInterface.getDisplayName());

        try {
            ipv4 = new SimpleStringProperty(Network.getNetworkInterfaceInet4Address(aInNetworkInterface).getHostAddress());
        } catch (Exception e) {
            ipv4 = new SimpleStringProperty("");
        }
        try {
            ipv6 = new SimpleStringProperty(Network.getNetworkInterfaceInet6Address(aInNetworkInterface).getHostAddress());
        } catch (Exception e) {
            ipv6 = new SimpleStringProperty("");
        }

    }

}
