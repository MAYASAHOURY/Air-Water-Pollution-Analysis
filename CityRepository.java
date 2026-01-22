import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class CityRepository {

    private static final String DATABASE_URL =
            "jdbc:mysql://localhost:3306/air-water-pollution"
                    + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private static final String DATABASE_USERNAME = "root";
    private static final String DATABASE_PASSWORD = "Simado2005!";

    // Table name contains dots -> must be backticked
    private static final String TABLE_NAME = "`cities_air_quality_water_pollution.18-10-2021`";

    // Column names literally contain quotes -> must be referenced as `\"Region\"` etc
    private static final String COLUMN_REGION = "`\"Region\"`";
    private static final String COLUMN_COUNTRY = "`\"Country\"`";
    private static final String COLUMN_AIR = "`\"AirQuality\"`";
    private static final String COLUMN_WATER = "`\"WaterPollution\"`";

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
    }

    public List<String> loadCountries() {
        String sqlQuery =
                "SELECT DISTINCT " + COLUMN_COUNTRY + " AS countryValue " +
                        "FROM " + TABLE_NAME + " " +
                        "ORDER BY countryValue";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery);
             ResultSet resultSet = statement.executeQuery()) {

            List<String> countries = new ArrayList<>();
            while (resultSet.next()) {
                countries.add(resultSet.getString("countryValue"));
            }
            return countries;

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load countries.", exception);
        }
    }

    public List<String> loadRegionsForCountry(String selectedCountry) {
        String sqlQuery =
                "SELECT DISTINCT " + COLUMN_REGION + " AS regionValue " +
                        "FROM " + TABLE_NAME + " " +
                        "WHERE " + COLUMN_COUNTRY + " = ? " +
                        "ORDER BY regionValue";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery)) {

            statement.setString(1, selectedCountry);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> regions = new ArrayList<>();
                while (resultSet.next()) {
                    regions.add(resultSet.getString("regionValue"));
                }
                return regions;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load regions.", exception);
        }
    }

    public List<CityRow> loadRows(String selectedCountry, String selectedRegion, String citySearchText) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ")
                .append("City AS cityValue, ")
                .append(COLUMN_REGION).append(" AS regionValue, ")
                .append(COLUMN_COUNTRY).append(" AS countryValue, ")
                .append(COLUMN_AIR).append(" AS airValue, ")
                .append(COLUMN_WATER).append(" AS waterValue ")
                .append("FROM ").append(TABLE_NAME).append(" ");

        List<Object> parameters = new ArrayList<>();
        boolean hasWhere = false;

        if (selectedCountry != null && !selectedCountry.isBlank()) {
            sqlBuilder.append(hasWhere ? "AND " : "WHERE ");
            sqlBuilder.append(COLUMN_COUNTRY).append(" = ? ");
            parameters.add(selectedCountry);
            hasWhere = true;
        }

        if (selectedRegion != null && !selectedRegion.isBlank()) {
            sqlBuilder.append(hasWhere ? "AND " : "WHERE ");
            sqlBuilder.append(COLUMN_REGION).append(" = ? ");
            parameters.add(selectedRegion);
            hasWhere = true;
        }

        if (citySearchText != null && !citySearchText.isBlank()) {
            sqlBuilder.append(hasWhere ? "AND " : "WHERE ");
            sqlBuilder.append("City LIKE ? ");
            parameters.add("%" + citySearchText.trim() + "%");
        }

        sqlBuilder.append("ORDER BY countryValue, regionValue, cityValue ");
        sqlBuilder.append("LIMIT 1000");

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sqlBuilder.toString())) {

            for (int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++) {
                statement.setObject(parameterIndex + 1, parameters.get(parameterIndex));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                List<CityRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new CityRow(
                            resultSet.getString("cityValue"),
                            resultSet.getString("regionValue"),
                            resultSet.getString("countryValue"),
                            resultSet.getDouble("airValue"),
                            resultSet.getDouble("waterValue")
                    ));
                }
                return rows;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load rows.", exception);
        }
    }

    public KpiSummary loadKpis(String selectedCountry, String selectedRegion, String citySearchText) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ")
                .append("COUNT(*) AS cityCount, ")
                .append("AVG(").append(COLUMN_AIR).append(") AS avgAir, ")
                .append("AVG(").append(COLUMN_WATER).append(") AS avgWater ")
                .append("FROM ").append(TABLE_NAME).append(" ");

        List<Object> parameters = new ArrayList<>();
        boolean hasWhere = false;

        if (selectedCountry != null && !selectedCountry.isBlank()) {
            sqlBuilder.append(hasWhere ? "AND " : "WHERE ");
            sqlBuilder.append(COLUMN_COUNTRY).append(" = ? ");
            parameters.add(selectedCountry);
            hasWhere = true;
        }

        if (selectedRegion != null && !selectedRegion.isBlank()) {
            sqlBuilder.append(hasWhere ? "AND " : "WHERE ");
            sqlBuilder.append(COLUMN_REGION).append(" = ? ");
            parameters.add(selectedRegion);
            hasWhere = true;
        }

        if (citySearchText != null && !citySearchText.isBlank()) {
            sqlBuilder.append(hasWhere ? "AND " : "WHERE ");
            sqlBuilder.append("City LIKE ? ");
            parameters.add("%" + citySearchText.trim() + "%");
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sqlBuilder.toString())) {

            for (int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++) {
                statement.setObject(parameterIndex + 1, parameters.get(parameterIndex));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                int cityCount = resultSet.getInt("cityCount");
                double avgAir = resultSet.getDouble("avgAir");
                double avgWater = resultSet.getDouble("avgWater");
                return new KpiSummary(cityCount, avgAir, avgWater);
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load KPIs.", exception);
        }
    }

    public List<CountryMetric> loadTopCountriesByAverageAirQuality() {
        String sqlQuery =
                "SELECT " + COLUMN_COUNTRY + " AS countryValue, AVG(" + COLUMN_AIR + ") AS avgAir " +
                        "FROM " + TABLE_NAME + " " +
                        "GROUP BY " + COLUMN_COUNTRY + " " +
                        "ORDER BY avgAir DESC " +
                        "LIMIT 10";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery);
             ResultSet resultSet = statement.executeQuery()) {

            List<CountryMetric> metrics = new ArrayList<>();
            while (resultSet.next()) {
                metrics.add(new CountryMetric(resultSet.getString("countryValue"), resultSet.getDouble("avgAir")));
            }
            return metrics;

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load top air quality countries.", exception);
        }
    }

    public List<CountryMetric> loadTopCountriesByAverageWaterPollution() {
        String sqlQuery =
                "SELECT " + COLUMN_COUNTRY + " AS countryValue, AVG(" + COLUMN_WATER + ") AS avgWater " +
                        "FROM " + TABLE_NAME + " " +
                        "GROUP BY " + COLUMN_COUNTRY + " " +
                        "ORDER BY avgWater DESC " +
                        "LIMIT 10";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery);
             ResultSet resultSet = statement.executeQuery()) {

            List<CountryMetric> metrics = new ArrayList<>();
            while (resultSet.next()) {
                metrics.add(new CountryMetric(resultSet.getString("countryValue"), resultSet.getDouble("avgWater")));
            }
            return metrics;

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load top water pollution countries.", exception);
        }
    }

    // NEW: fetch both countries and global in ONE query (for a given metric)
    public TwoCountryComparison loadTwoCountriesVsGlobalAverage(String countryA, String countryB, PollutionType pollutionType) {
        if (countryA == null || countryA.isBlank() || countryB == null || countryB.isBlank()) {
            throw new IllegalArgumentException("Both countries must be selected.");
        }

        String metricColumn = (pollutionType == PollutionType.AIR) ? COLUMN_AIR : COLUMN_WATER;

        String sqlQuery =
                "SELECT " +
                        "AVG(CASE WHEN " + COLUMN_COUNTRY + " = ? THEN " + metricColumn + " END) AS countryAAvg, " +
                        "AVG(CASE WHEN " + COLUMN_COUNTRY + " = ? THEN " + metricColumn + " END) AS countryBAvg, " +
                        "AVG(" + metricColumn + ") AS globalAvg " +
                        "FROM " + TABLE_NAME;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery)) {

            statement.setString(1, countryA);
            statement.setString(2, countryB);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();

                double countryAAvg = resultSet.getDouble("countryAAvg");
                boolean aWasNull = resultSet.wasNull();

                double countryBAvg = resultSet.getDouble("countryBAvg");
                boolean bWasNull = resultSet.wasNull();

                double globalAvg = resultSet.getDouble("globalAvg");

                if (aWasNull) {
                    throw new RuntimeException("No rows found for: " + countryA);
                }
                if (bWasNull) {
                    throw new RuntimeException("No rows found for: " + countryB);
                }

                return new TwoCountryComparison(countryAAvg, countryBAvg, globalAvg);
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load 2-country comparison.", exception);
        }
    }

    public enum PollutionType { AIR, WATER }

    public record KpiSummary(int cityCount, double averageAirQuality, double averageWaterPollution) {}
    public record CountryMetric(String country, double value) {}

    // NEW record for compare page
    public record TwoCountryComparison(double countryAAvg, double countryBAvg, double globalAvg) {}
}
