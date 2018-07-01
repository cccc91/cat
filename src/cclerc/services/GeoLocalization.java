package cclerc.services;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;

import java.io.File;
import java.net.InetAddress;

/**
 * Singleton class implementing geo localization services
 */
public class GeoLocalization {

    private static GeoLocalization geoLocInstance = new GeoLocalization();

    private DatabaseReader geoLocalizationDatabase;

    private GeoLocalization() {
        try {
            // Copy the geo localization database locally
            String lTemporaryLocalizationDatabase = Utilities.exportResource("resources/geoLocalization/" + Constants.GEO_LOC_DATABASE, System.getProperty("java.io.tmpdir"));

            // Read the database
            File lDatabase = new File(lTemporaryLocalizationDatabase);
            geoLocalizationDatabase = new DatabaseReader.Builder(lDatabase).build();

        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }
    };

    // SINGLETON

    /**
     * Returns the singleton
     * @return GEo localization singleton instance
     */
    public static GeoLocalization getInstance() {
        return geoLocInstance;
    }

    public void getLocalGeoLocalization() {

        try {

            // Retrieve local ip address
            InetAddress lLocalIp = InetAddress.getByName(Network.getExternalIp());


            CityResponse response = geoLocalizationDatabase.city(lLocalIp);

            System.out.println(
                    String.format("Country=%s, City=%s, Postal=%s, State=%s, lat=%s, long=%s",
                                  response.getCountry().getNames().get(LocaleUtilities.getInstance().getCurrentLocale().getLanguage()),
                                  response.getCity().getName(), response.getPostal().getCode(),
                                  response.getLeastSpecificSubdivision().getName(), response.getLocation().getLatitude(), response.getLocation().getLongitude()));

        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }

    }

}
