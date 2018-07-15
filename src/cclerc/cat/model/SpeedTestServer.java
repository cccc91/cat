package cclerc.cat.model;

import cclerc.services.GeoLocalization;
import cclerc.services.Utilities;
import javafx.beans.property.*;
import org.jdom2.Element;

public class SpeedTestServer {

    private StringProperty name;
    private StringProperty country;
    private StringProperty city;
    private StringProperty sponsor;
    private DoubleProperty distance;

    private String url;

    // GETTERS

    public String getName() {
        return name.get();
    }

    public String getCountry() {
        return country.get();
    }

    public String getCity() {
        return city.get();
    }

    public String getSponsor() {
        return sponsor.get();
    }

    public Double getDistance() {
        return distance.get();
    }

    public String getUrl() {
        return url;
    }

    // PROPERTY GETTERS

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty countryProperty() {
        return country;
    }

    public StringProperty cityProperty() {
        return city;
    }

    public StringProperty sponsorProperty() {
        return sponsor;
    }

    public DoubleProperty distanceProperty() {
        return distance;
    }

    // SETTERS

    public void setName(String aInName) {
        name.set(aInName);
    }

    public void setCountry(String aInCountry) {
        country.set(aInCountry);
    }

    public void setCity(String aInCity) {
        city.set(aInCity);
    }

    public void setSponsor(String aInSponsor) {
        sponsor.set(aInSponsor);
    }

    public void setDistance(Double aInDistance) {
        distance.set(aInDistance);
    }

    public void setUrl(String aInUrl) {
        url = aInUrl;
    }

    // CONSTRUCTORS

    /**
     * Builds the speed test server from specified attributes
     * @param aInName     Server name
     * @param aInCountry  Country of the server
     * @param aInCity     City of the server
     * @param aInDistance Distance of the server from current location
     * @param aInUrl      Server url
     */
    public SpeedTestServer(String aInName, String aInCountry, String aInCity, String aInSponsor, Double aInDistance, String aInUrl) {
        name = new SimpleStringProperty(aInName);
        country = new SimpleStringProperty(aInCountry);
        city = new SimpleStringProperty(aInCity);
        sponsor = new SimpleStringProperty(aInSponsor);
        distance = new SimpleDoubleProperty(aInDistance);
        url = aInUrl;
    }

    /**
     * Builds the speed test server from XML parsing
     */
    public SpeedTestServer(Element aInElement) {
        name = new SimpleStringProperty(aInElement.getAttributeValue("host").replaceAll(":[0-9]*$", ""));
        country = new SimpleStringProperty(aInElement.getAttributeValue("cc"));
        city = new SimpleStringProperty(aInElement.getAttributeValue("name"));
        sponsor = new SimpleStringProperty(aInElement.getAttributeValue("sponsor"));
        Double lLatitude = (aInElement.getAttributeValue("lat") == null) ? 0d :Double.valueOf(aInElement.getAttributeValue("lat"));
        Double lLongitude = (aInElement.getAttributeValue("lat") == null) ? 0d :Double.valueOf(aInElement.getAttributeValue("lon"));
        distance = new SimpleDoubleProperty(GeoLocalization.getInstance().computeDistanceToLocal(lLatitude, lLongitude));
        url = aInElement.getAttributeValue("url");
    }

}
