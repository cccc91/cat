package cclerc.cat.model;

import javafx.beans.property.*;
import org.jdom2.Element;

public class SpeedTestServer {

    private StringProperty name;
    private StringProperty country;
    private StringProperty city;
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
    public SpeedTestServer(String aInName, String aInCountry, String aInCity, Double aInDistance, String aInUrl) {
        name = new SimpleStringProperty(aInName);
        country = new SimpleStringProperty(aInCountry);
        city = new SimpleStringProperty(aInCity);
        distance = new SimpleDoubleProperty(aInDistance);
        url = aInUrl;
        // TODO

    }

    /**
     * Builds the speed test server from XML parsing
     */
    public SpeedTestServer(Element aInElement) {
        name = new SimpleStringProperty(aInElement.getAttributeValue("host").replaceAll(":[0-9]*$", ""));
        country = new SimpleStringProperty(aInElement.getAttributeValue("cc"));
        city = new SimpleStringProperty(aInElement.getAttributeValue("name"));
        distance = new SimpleDoubleProperty(0);
        url = aInElement.getAttributeValue("url");
    }

}
