package cclerc.services;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;

import javax.rmi.CORBA.Util;
import java.io.File;
import java.net.InetAddress;

/**
 * Singleton class implementing geo localization services
 */
public class GeoLocalization {

    private static GeoLocalization geoLocInstance = new GeoLocalization();

    private DatabaseReader geoLocalizationDatabase;
    private CityResponse cityResponse;

    private GeoLocalization() {
        try {
            // Copy the geo localization database locally
            String lTemporaryLocalizationDatabase = Utilities.exportResource("resources/geoLocalization/" + Constants.GEO_LOC_DATABASE, System.getProperty("java.io.tmpdir"));

            // Read the database
            File lDatabase = new File(lTemporaryLocalizationDatabase);
            geoLocalizationDatabase = new DatabaseReader.Builder(lDatabase).build();

            // Get localization of local ip
            InetAddress lLocalIp = InetAddress.getByName(Network.getExternalIp());
            cityResponse = geoLocalizationDatabase.city(lLocalIp);


        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.localization.notFound"), e.getMessage()));
        }
    };

    // SINGLETON

    /**
     * Returns the singleton
     * @return Geo localization singleton instance
     */
    public static GeoLocalization getInstance() {
        return geoLocInstance;
    }

    public void getLocalGeoLocalization() {

        if (cityResponse != null) {

            System.out.println(
                    String.format("Country=%s, City=%s, Postal=%s, State=%s, lat=%s, long=%s",
                            cityResponse.getCountry().getNames().get(LocaleUtilities.getInstance().getCurrentLocale().getLanguage()),
                            cityResponse.getCity().getName(), cityResponse.getPostal().getCode(),
                            cityResponse.getLeastSpecificSubdivision().getName(), cityResponse.getLocation().getLatitude(), cityResponse.getLocation().getLongitude()));

        } else {
            System.out.println("No localization found for local ip");
        }

    }

    public Double computeDistanceToLocal(Double aInLatitudePoint, Double aInLongitudePoint) {
        if (cityResponse != null && cityResponse.getLocation() != null && cityResponse.getLocation().getLatitude() != null && cityResponse.getLocation().getLongitude() != null) {
            return computeDistance(
                    aInLatitudePoint, aInLongitudePoint, Double.valueOf(cityResponse.getLocation().getLatitude()), Double.valueOf(cityResponse.getLocation().getLongitude()));
        } else {
            return 0d;
        }
    }

    public Double computeDistance(Double aInLatitudePoint1, Double aInLongitudePoint1, Double aInLatitudePoint2, Double aInLongitudePoint2) {

        final Double lRadius = 6371d;

        Double lLatitudeAngle = Math.toRadians(aInLatitudePoint2 - aInLatitudePoint1);
        Double lLongitudeAngle = Math.toRadians(aInLongitudePoint2 - aInLongitudePoint1);
        Double a = (Math.sin(lLatitudeAngle / 2) * Math.sin(lLatitudeAngle / 2) +
             Math.cos(Math.toRadians(aInLatitudePoint1)) *
             Math.cos(Math.toRadians(aInLatitudePoint2)) * Math.sin(lLongitudeAngle / 2) *
             Math.sin(lLongitudeAngle / 2));
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        Double d = lRadius * c;

        return Utilities.round(d, 1);

    }

}
