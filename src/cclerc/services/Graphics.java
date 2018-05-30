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
            return (aInAddressType == EnumTypes.AddressType.WAN) ?
                        new Image(Thread.currentThread().getContextClassLoader().getResource("resources/images/" + Constants.IMAGE_WAN_OK).toString()) :
                        new Image(Thread.currentThread().getContextClassLoader().getResource("resources/images/" + Constants.IMAGE_LAN_OK).toString());
        } else {
            return (aInAddressType == EnumTypes.AddressType.WAN) ?
                        new Image(Thread.currentThread().getContextClassLoader().getResource("resources/images/" + Constants.IMAGE_WAN_NOK).toString()) :
                        new Image(Thread.currentThread().getContextClassLoader().getResource("resources/images/" + Constants.IMAGE_LAN_NOK).toString());
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
                return (aInStateOk) ?
                            new Image(Thread.currentThread().getContextClassLoader().getResource("resources/images/" + Constants.IMAGE_ETH_OK).toString()) :
                            new Image(Thread.currentThread().getContextClassLoader().getResource("resources/images/" + Constants.IMAGE_ETH_NOK).toString());
            case WIFI:
                return (aInStateOk) ?
                            new Image(Thread.currentThread().getContextClassLoader().getResource("resources/images/" + Constants.IMAGE_WIFI_OK).toString()) :
                            new Image(Thread.currentThread().getContextClassLoader().getResource("resources/images/" + Constants.IMAGE_WIFI_NOK).toString());
            default:
                return null;
        }

    }

    private Graphics() {
    }

}
