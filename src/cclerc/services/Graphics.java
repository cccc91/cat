package cclerc.services;

import javafx.scene.image.Image;

public class Graphics {

    /**
     * Gets image corresponding to an address type and the state of this address type
     * @param aInAddressType Address type
     * @param aInStateOk     true if address type is ok, false otherwise
     * @return Image
     */
    public static Image getAddressTypeImage(EnumTypes.AddressType aInAddressType, boolean aInStateOk) {

        if (aInAddressType == null) return null;

        if (aInStateOk) {
            return (aInAddressType == EnumTypes.AddressType.WAN) ? Constants.IMAGE_WAN_OK : Constants.IMAGE_LAN_OK;
        } else {
            return (aInAddressType == EnumTypes.AddressType.WAN) ? Constants.IMAGE_WAN_NOK : Constants.IMAGE_LAN_NOK;
        }

    }

    /**
     * Gets image corresponding to an interface type and the state of this address type
     * @param aInInterfaceType Address type
     * @param aInStateOk       true if interface type is ok, false otherwise
     * @return Image
     */
    public static Image getInterfaceTypeImage(EnumTypes.InterfaceType aInInterfaceType, boolean aInStateOk) {

        if (aInInterfaceType == null) return null;

        switch (aInInterfaceType) {
            case ETH:
                return (aInStateOk) ? Constants.IMAGE_ETH_OK : Constants.IMAGE_ETH_NOK;
            case WIFI:
                return (aInStateOk) ? Constants.IMAGE_WIFI_OK : Constants.IMAGE_WIFI_NOK;
            default:
                return null;
        }

    }

    private Graphics() {
    }

}
