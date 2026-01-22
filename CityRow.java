import javafx.beans.property.*;

public final class CityRow {

    private final StringProperty city = new SimpleStringProperty();
    private final StringProperty region = new SimpleStringProperty();
    private final StringProperty country = new SimpleStringProperty();
    private final DoubleProperty airQuality = new SimpleDoubleProperty();
    private final DoubleProperty waterPollution = new SimpleDoubleProperty();

    public CityRow(String cityValue, String regionValue, String countryValue, double airQualityValue, double waterPollutionValue) {
        city.set(cityValue);
        region.set(regionValue);
        country.set(countryValue);
        airQuality.set(airQualityValue);
        waterPollution.set(waterPollutionValue);
    }

    public String getCity() { return city.get(); }
    public StringProperty cityProperty() { return city; }

    public String getRegion() { return region.get(); }
    public StringProperty regionProperty() { return region; }

    public String getCountry() { return country.get(); }
    public StringProperty countryProperty() { return country; }

    public double getAirQuality() { return airQuality.get(); }
    public DoubleProperty airQualityProperty() { return airQuality; }

    public double getWaterPollution() { return waterPollution.get(); }
    public DoubleProperty waterPollutionProperty() { return waterPollution; }
}
