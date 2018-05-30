package cclerc.services;

import cclerc.cat.model.Interface;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class EnumTypes {

    /**
     * Find the list of values of an enum type given by its name
     * @param aInEnumName Enum name
     * @return List of values of the enum
     */
    public static List<String> findValues(String aInEnumName) {

        List<String> lEnumValues = new ArrayList<>();
        try {
            for (Object lObject: Class.forName(EnumTypes.class.getPackage().getName() + ".EnumTypes$" + aInEnumName).getEnumConstants()) {
                lEnumValues.add(lObject.toString());
            }
        } catch (ClassNotFoundException e) {
            Display.logUnexpectedError(e);
        }

        return lEnumValues;
    }

    // Message level
    public enum  MessageLevel {
        INFO, WARNING, OK, ERROR;

        public Color getColor() {
            switch (this) {
                case INFO:
                    return Color.BLACK;
                case WARNING:
                    return Color.ORANGE;
                case OK:
                    return Color.GREEN;
                case ERROR:
                    return Color.RED;
                default:
                    return Color.BLACK;
            }
        }

    }

    // Types of connections
    public enum InterfaceType {
        WIFI, ETH;  // Order is important, to be written from lowest priority to highest one

        public static String valueOf(InterfaceType aInInterfaceType) {
            switch (aInInterfaceType) {
                case ETH:
                    return "ETH";
                case WIFI:
                    return "WIFI";
                default:
                    return "ETH";
            }
        }

        @Override
        public String toString() {
            switch (this) {
                case ETH:
                    return Display.getMessagesResourceBundle().getString("enum.interfaceType.eth");
                case WIFI:
                    return Display.getMessagesResourceBundle().getString("enum.interfaceType.wifi");
                default:
                    return "";
            }
        }

    }

    public enum AddressType {
        WAN, LAN;

        public static String valueOf(AddressType aInAddressType) {
            switch (aInAddressType) {
                case WAN:
                    return "WAN";
                case LAN:
                    return "LAN";
                default:
                    return "LAN";
            }
        }

        @Override
        public String toString() {
            switch (this) {
                case WAN:
                    return Display.getMessagesResourceBundle().getString("enum.addressType.wan");
                case LAN:
                    return Display.getMessagesResourceBundle().getString("enum.addressType.lan");
                default:
                    return "";
            }
        }

    }

    public enum ConnectionType {
        ETH, WIFI, WAN, LAN;

        public static String valueOf(ConnectionType aInConnectionType) {
            switch (aInConnectionType) {
                case WAN:
                    return "WAN";
                case LAN:
                    return "LAN";
                case ETH:
                    return "ETH";
                case WIFI:
                    return "WIFI";
                default:
                    return "ETH";
            }
        }

        public static ConnectionType valueOf(InterfaceType aInInterfaceType) {
            switch (aInInterfaceType) {
                case ETH:
                    return ETH;
                case WIFI:
                    return WIFI;
                default:
                    return ETH;
            }
        }

        public static ConnectionType valueOf(AddressType aInAddressType) {
            switch (aInAddressType) {
                case WAN:
                    return WAN;
                case LAN:
                    return LAN;
                default:
                    return WAN;
            }
        }

        @Override
        public String toString() {
            switch (this) {
                case ETH:
                    return Display.getMessagesResourceBundle().getString("enum.interfaceType.eth");
                case WIFI:
                    return Display.getMessagesResourceBundle().getString("enum.interfaceType.wifi");
                case WAN:
                    return Display.getMessagesResourceBundle().getString("enum.addressType.wan");
                case LAN:
                    return Display.getMessagesResourceBundle().getString("enum.addressType.lan");
                default:
                    return "";
            }
        }

    }

    // Host states
    public enum HostState {REACHABLE, PING_LOST, UNREACHABLE}

    // Active server
    public enum ServerType {
        PRIMARY(0), BACKUP(1);

        private int value;

        ServerType(int aInValue) {
            value = aInValue;
        }

        public Integer getValue() {
            return value;
        }

        public ServerType toggle() {
            if (this.equals(PRIMARY))
                return BACKUP;
            else
                return PRIMARY;
        }

        public static String valueOf(ServerType aInServerType) {
            switch (aInServerType) {
                case BACKUP:
                    return "BACKUP";
                case PRIMARY:
                    return "PRIMARY";
                default:
                    return "PRIMARY";
            }
        }

        @Override
        public String toString() {
            return  name().toLowerCase();
        }

    }

    // Alarms
    public enum AlarmId {
        PING_LOST(1), CONNECTION_LOST(2),
        ETHERNET_DOWN(3), WIFI_DOWN(4),
        INTERNET_DOWN(5), LAN_DOWN(6),
        NETWORK_DOWN(7),
        ETHERNET_UNSTABLE(8), WIFI_UNSTABLE(9),
        INTERNET_UNSTABLE(10), LAN_UNSTABLE(11),
        NETWORK_UNSTABLE(12);

        private int value;

        AlarmId(int aInValue) {
            this.value = aInValue;
        }

        public int getValue() {
            return value;
        }

    }
    public enum AlarmState {RAISED, CLEARED, ACKNOWLEDGED}
    public enum AlarmSeverity {INFO, WARNING, MINOR, MAJOR, CRITICAL;
        @Override
        public String toString() {
            return  name().toLowerCase();
        }
        public String getDisplayedValue() {
            return Display.getViewResourceBundle().getString("catView.alarmView.severity." + this.toString());

        }
    }
    public enum AlarmType {SITE_UNREACHABLE, INTERFACE_DOWN, NETWORK_DOWN, INTERFACE_UNSTABLE, NETWORK_UNSTABLE}
    public enum AlarmObjectType {INTERFACE, NETWORK}

    // SMTP servers
    public enum TlsMode {none, starttls};

}
