package cclerc.cat.model;

import javafx.beans.property.*;

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
     * Builds the speed test server from XML parsing
     */
    public SpeedTestServer(String aInName, String aInCountry, String aInCity, Double aInDistance, String aInUrl) {
        name = new SimpleStringProperty(aInName);
        country = new SimpleStringProperty(aInCountry);
        city = new SimpleStringProperty(aInCity);
        distance = new SimpleDoubleProperty(aInDistance);
        url = aInUrl;
        // TODO

    }

}
