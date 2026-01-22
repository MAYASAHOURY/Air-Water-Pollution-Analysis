import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DbSmokeTest {

    public static void main(String[] args) {
        String databaseUrl =
                "jdbc:mysql://localhost:3306/air-water-pollution"
                        + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        String databaseUsername = "root";
        String databasePassword = "Simado2005!";

        String tableName = "`cities_air_quality_water_pollution.18-10-2021`";

        String sqlQuery =
                "SELECT " +
                        "City AS cityValue, " +
                        "`\"Region\"` AS regionValue, " +
                        "`\"Country\"` AS countryValue, " +
                        "`\"AirQuality\"` AS airValue, " +
                        "`\"WaterPollution\"` AS waterValue " +
                        "FROM " + tableName + " " +
                        "LIMIT 10";

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
             PreparedStatement statement = connection.prepareStatement(sqlQuery);
             ResultSet resultSet = statement.executeQuery()) {

            System.out.println("Connected. First 10 rows:");
            while (resultSet.next()) {
                String city = resultSet.getString("cityValue");
                String region = resultSet.getString("regionValue");
                String country = resultSet.getString("countryValue");
                double air = resultSet.getDouble("airValue");
                double water = resultSet.getDouble("waterValue");

                System.out.printf("%s | %s | %s | air=%.2f | water=%.2f%n",
                        city, region, country, air, water);
            }

        } catch (Exception exception) {
            System.out.println("FAILED: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}
