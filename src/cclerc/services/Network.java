package cclerc.services;

import com.btr.proxy.search.ProxySearch;

import java.net.*;
import java.time.Instant;
import java.util.*;

/**
 *   Static class implementing network services
 */
public class Network {

    private static Map<String, NetworkInterface> networkInterfaceList = buildNetworkInterfaceList();

    // Constants
    private final static int TTL = 0;

    /**
     * Builds list of network interfaces having a public IP and which are not the loopback interface
     * @return    network interfaces indexed by their name
     */
    public static HashMap<String, NetworkInterface> buildNetworkInterfaceList() {

        HashMap<String, NetworkInterface> lNetworkInterfaceList = new HashMap<>();

        // Get all interfaces
        try {

            Enumeration<NetworkInterface> lNetworkInterfaces = NetworkInterface.getNetworkInterfaces();

            // Parse interfaces
            for (NetworkInterface lNetworkInterface : Collections.list(lNetworkInterfaces)) {

                // Keep interfaces different from loopback and with public ip
                boolean lKeepInterface = false;
                java.util.List<InterfaceAddress> lInterfaceAddressList = lNetworkInterface.getInterfaceAddresses();
                for (InterfaceAddress lInterfaceAddress : lInterfaceAddressList) {
                    if (!lInterfaceAddress.getAddress().isLoopbackAddress() &&
                            !lInterfaceAddress.getAddress().isLinkLocalAddress()) {
                        lKeepInterface = true;
                        break;
                    }
                }
                if (lNetworkInterface.isPointToPoint()) lKeepInterface = false;

                if (lKeepInterface) {
                    lNetworkInterfaceList.put(lNetworkInterface.getName(), lNetworkInterface);
                }
            }
        } catch (Exception e) {
            lNetworkInterfaceList = null;
        }

        return lNetworkInterfaceList;

    }

    /**
     * Builds the list of descriptions of all network interfaces
     * @return network interfaces descriptions indexed by their name
     */
    public static HashMap<String, String> buildNetworkInterfacesDescriptionList() {
        HashMap<String, String> lDescriptionsList = new HashMap<>();
        HashMap<String, NetworkInterface> lNetworkInterfacesList = buildNetworkInterfaceList();
        for (String lInterfaceName: lNetworkInterfacesList.keySet()) {
            lDescriptionsList.put(lInterfaceName, lNetworkInterfacesList.get(lInterfaceName).getDisplayName());
        }
        return lDescriptionsList;
    }

    /**
     * Checks if at least an interface supports ipv6
     * @return true if at least one interface supports ipv6, false otherwise
     */
    public static boolean isIpv6Supported() {

        for (NetworkInterface lNetworkInterface: buildNetworkInterfaceList().values()) {
            try {
                getNetworkInterfaceInet6Address(lNetworkInterface).getHostAddress();
                return true;
            } catch (Exception e) {
            }
        }
        return false;
    }

    /**
     * Checks if an ip address is ipv4
     * @param aInIp IP adress to check
     * @return true if the address if ipv4, false otherwise
     */
    public static boolean isIPv4Address(String aInIp) {

        if (aInIp.isEmpty()) {
            return false;
        }
        try {
            Object lObject = InetAddress.getByName(aInIp);
            return lObject instanceof Inet4Address;
        } catch (final UnknownHostException ex) {
            return false;
        }
    }

    /**
     * Checks if an ip address is ipv6
     * @param aInIp IP address to check
     * @return true if the address if ipv6, false otherwise
     */
    public static boolean isIPv6Address(String aInIp) {

        if (aInIp.isEmpty()) {
            return false;
        }
        try {
            Object lObject = InetAddress.getByName(aInIp);
            return lObject instanceof Inet6Address;
        } catch (final UnknownHostException ex) {
            return false;
        }
    }

    /**
     * Gets Inet4Address of a network interface
     * @param aInNetworkInterface Network interface
     * @return Inet4Address
     */
    public static Inet4Address getNetworkInterfaceInet4Address(NetworkInterface aInNetworkInterface) {
        for (InetAddress lInetAddress: Collections.list(aInNetworkInterface.getInetAddresses())) {
            if (lInetAddress instanceof Inet4Address) {
                return (Inet4Address) lInetAddress;
            }
        }
        return null;
    }

    /**
     * Gets Inet4Address of a network interface
     * @param aInNetworkInterface Network interface
     * @return Inet4Address
     */
    public static Inet6Address getNetworkInterfaceInet6Address(NetworkInterface aInNetworkInterface) {
        Inet6Address lLocalAddress = null;
        for (InetAddress lInetAddress: Collections.list(aInNetworkInterface.getInetAddresses())) {
            // Filter site local addresses
            if (lInetAddress instanceof Inet6Address && !lInetAddress.isSiteLocalAddress()) {
                if (!lInetAddress.getHostAddress().contains("fe80")) {
                    return (Inet6Address) lInetAddress;
                } else if (lLocalAddress == null) {
                    lLocalAddress = (Inet6Address) lInetAddress;
                }
            }
        }
        // No public address found, return local address (might be null)
        return lLocalAddress;
    }

    /**
     * Gets InetAddress of a network interface depending on the ip version
     * @param aInNetworkInterface Network interface
     * @param aInIpv4             True if ipv4 is required, false otherwise
     * @return InetAddress
     */
    public static InetAddress getNetworkInterfaceInetAddress(NetworkInterface aInNetworkInterface, boolean aInIpv4) {
        if (aInIpv4) {
            return getNetworkInterfaceInet4Address(aInNetworkInterface);
        } else {
            return getNetworkInterfaceInet6Address(aInNetworkInterface);
        }
    }

    /**
     * Gets Inet4Address of a host
     * @param aInHost Hostname
     * @return Inet4Address
     */
    public static Inet4Address getHostInet4Address(String aInHost) {

        try {
            InetAddress[] lInetAddresses = InetAddress.getAllByName(aInHost);
            for (InetAddress lInetAddress : lInetAddresses) {
                if (lInetAddress instanceof Inet4Address) {
                    return (Inet4Address) lInetAddress;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * Gets Inet6Address of a host
     * @param aInHost Hostname
     * @return Inet6Address
     */
    public static Inet6Address getHostInet6Address(String aInHost) {

        try {
            InetAddress[] lInetAddresses = InetAddress.getAllByName(aInHost);
            for (InetAddress lInetAddress : lInetAddresses) {
                if (lInetAddress instanceof Inet6Address) {
                    return (Inet6Address) lInetAddress;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * Gets InetAddress of a host depending on the ip version
     * @param aInHost Hostname
     * @param aInIpv4 True if ipv4 is required, false otherwise
     * @return InetAddress
     */
    public static InetAddress getHostInetAddress(String aInHost, boolean aInIpv4) {
        if (aInIpv4) {
            return getHostInet4Address(aInHost);
        } else {
            return getHostInet6Address(aInHost);
        }

    }

    /**
     * Finds system SOCKS proxy
     * @param aInIp   Remote ip address to reach
     * @param aInPort Remote port to reach
     * @return System proxy or null if no proxy is defined
     */
    public static Proxy findSocksProxy(String aInIp, int aInPort) {

        final int SIZE = 32;
        final int TTL = 1000 * 60 * 5;

        Proxy lProxy = Proxy.NO_PROXY;
        try {

            System.setProperty("java.net.useSystemProxies", "true");
            URI lUri = new URI("socket://" + aInIp + ":" + aInPort);

            // Use proxy vole library to find the default proxy
            ProxySearch lProxySearch = ProxySearch.getDefaultProxySearch();
            lProxySearch.setPacCacheSettings(SIZE, TTL);
            List lObjectList = lProxySearch.getProxySelector().select(lUri);

            for (Object lObject: lObjectList) {
                Proxy lProxyObject = (Proxy) lObject;
                InetSocketAddress lInetAddress = (InetSocketAddress) lProxyObject.address();
                if (lInetAddress != null) {
                    lProxy = lProxyObject;
                    break;
                }
            }

        } catch (Exception e) {
        }

        return lProxy;

    }

    /**
     * Finds system HTTP/S proxy
     * @param aInUrl Remote url to reach
     * @return System proxy or null if no proxy is defined
     */
    public static Proxy findHttpProxy(String aInUrl) {

        final int SIZE = 32;
        final int TTL = 1000 * 60 * 5;

        Proxy lProxy = Proxy.NO_PROXY;
        try {

            System.setProperty("java.net.useSystemProxies", "true");
            URI lUri = new URI(aInUrl);

            // Use proxy vole library to find the default proxy
            ProxySearch lProxySearch = ProxySearch.getDefaultProxySearch();
            lProxySearch.setPacCacheSettings(SIZE, TTL);
            List<Proxy> lProxies = lProxySearch.getProxySelector().select(lUri);

            // Find first proxy for HTTP/S. Any DIRECT proxy in the list returned is only second choice
            if (lProxies != null) {
                loop: for (Proxy p : lProxies) {
                    switch (p.type()) {
                        case HTTP:
                            lProxy = p;
                            break loop;
                        case DIRECT:
                            lProxy = p;
                            break;
                    }
                }
            }

        } catch (Exception e) {
        }

        return lProxy;

    }

    /**
     * Finds a port that can be opened on a remote ip address through a specific network interface
     * @param aInIp               Remote ip address to reach
     * @param aInNetworkInterface Network interface through which the IP address must be reached
     * @param aInTimeout          Timeout (ms)
     * @param aInUseProxy         Use proxy to reach remote address
     * @return                    true if the remote ip address is reachable, false otherwise
     */
    public static int findPort(String aInIp, NetworkInterface aInNetworkInterface, int aInTimeout, boolean aInUseProxy) {

        InetAddress lSourceInetAddress = (isIPv4Address(aInIp)) ? getNetworkInterfaceInet4Address(aInNetworkInterface) : getNetworkInterfaceInet6Address(aInNetworkInterface);
        // http, https, ssh, smtp, ftp, echo, telnet, alternate http, alternate https
        int[] lPortsList = {80, 443, 22, 25, 7, 21, 23, 8080, 8443};
        for (Integer lPort: lPortsList) {
            try {
                Proxy lProxy = findSocksProxy(aInIp, lPort);
                Socket lSocket = (lProxy != null && aInUseProxy) ? new Socket(lProxy) : new Socket();
                lSocket.bind(new InetSocketAddress(lSourceInetAddress, 0));
                lSocket.connect(new InetSocketAddress(aInIp, lPort), aInTimeout);
                return lPort;
            } catch (Exception e) {
            }
        }
        return 0;

    }

    /**
     * Tests if a remote ip is reachable on a specific port through a specific network interface - Socket version
     * @param aInIp               Remote ip address to reach
     * @param aInPort             Port on which the ip address needs to be reached
     * @param aInNetworkInterface Network interface through which the IP address must be reached
     * @param aInTimeout          Timeout (ms)
     * @param aInMaxRetries       Number max of retries
     * @param aInUseProxy         Use proxy to reach remote address
     * @return                    Number of retries (if > maxRetries, means remote ip is not reachable
     */
    public static int isReachable(String aInIp, int aInPort, NetworkInterface aInNetworkInterface, int aInTimeout, int aInMaxRetries, boolean aInUseProxy) {

        // If interface is down, declare ip unreachable
        if (!networkInterfaceList.containsKey(aInNetworkInterface.getName())) return aInMaxRetries + 1;

        InetAddress lInetAddress = (isIPv4Address(aInIp)) ? getNetworkInterfaceInet4Address(aInNetworkInterface) : getNetworkInterfaceInet6Address(aInNetworkInterface);

        boolean lIsReachable = false;
        int lRetries = 0;
        while ((lRetries <= aInMaxRetries) && (!lIsReachable)) {

            try {
                Proxy lProxy = aInUseProxy ? findSocksProxy(aInIp, aInPort) : Proxy.NO_PROXY;
                Socket lSocket = new Socket(lProxy);
                lSocket.bind(new InetSocketAddress(lInetAddress, 0));
                lSocket.connect(new InetSocketAddress(aInIp, aInPort), aInTimeout);
                lIsReachable = true;
            } catch (Exception e) {
                lRetries++;
                Utilities.sleep(500);
            }

        }

        return lRetries;

    }

    /**
     * Tests if a remote ip is reachable through a specific network interface - legacy isReachable version
     * @param aInIp               Remote ip address to reach
     * @param aInNetworkInterface Network interface through which the IP address must be reached
     * @param aInTimeout          Timeout (ms)
     * @param aInMaxRetries       Number max of retries
     * @param aInPollingPeriod    Wait time between 2 retries
     * @return                    Number of retries (if > maxRetries, means remote ip is not reachable
     */
    public static int isReachable(String aInIp, NetworkInterface aInNetworkInterface, int aInTimeout, int aInMaxRetries, int aInPollingPeriod) {

        boolean lIsReachable = false;

        int lRetries = 0;
        while ((lRetries <= aInMaxRetries) && (!lIsReachable)) {

            try {
                InetAddress lInetAddress = InetAddress.getByName(aInIp);
                lIsReachable = lInetAddress.isReachable(aInNetworkInterface, TTL, aInTimeout);
            } catch (Exception e) {
                lRetries++;
            } finally {
                if (!lIsReachable) {
                    lRetries++;
                    Utilities.sleep(aInPollingPeriod);
                }
            }

        }

        return lRetries;

    }

    /**
     * Gets interface type
     * @param aInInterfaceName Name of the interface for which the type must be retrieved
     * @return Interface type
     */
    public static EnumTypes.InterfaceType getInterfaceType(String aInInterfaceName) {
        return (aInInterfaceName.startsWith("w")) ? EnumTypes.InterfaceType.WIFI : EnumTypes.InterfaceType.ETH;
    }

}
