import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainApp extends Application {

    private final CityRepository cityRepository = new CityRepository();

    // Filter UI (Table tab)
    private final ComboBox<String> countryComboBox = new ComboBox<>();
    private final ComboBox<String> regionComboBox = new ComboBox<>();
    private final TextField citySearchTextField = new TextField();

    // Status + Table
    private final Label statusLabel = new Label("Ready.");
    private final TableView<CityRow> cityTableView = new TableView<>();

    // KPI labels (Analytics tab)
    private final Label kpiCitiesLabel = new Label("-");
    private final Label kpiAvgAirLabel = new Label("-");
    private final Label kpiAvgWaterLabel = new Label("-");

    // Charts (Analytics tab)
    private final BarChart<String, Number> airBarChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final BarChart<String, Number> waterBarChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final ScatterChart<Number, Number> scatterChart = new ScatterChart<>(new NumberAxis(), new NumberAxis());

    // Existing Analytics compare (country vs global)
    private final ComboBox<String> compareCountryComboBox = new ComboBox<>();
    private final ComboBox<String> compareTypeComboBox = new ComboBox<>();
    private final BarChart<String, Number> compareBarChart = new BarChart<>(new CategoryAxis(), new NumberAxis());

    // NEW Compare tab: two countries + metric
    private final ComboBox<String> compareCountryAComboBox = new ComboBox<>();
    private final ComboBox<String> compareCountryBComboBox = new ComboBox<>();
    private final ComboBox<String> compareMetricComboBox = new ComboBox<>();
    private final BarChart<String, Number> twoCountryBarChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final GridPane twoCountryCardsGrid = new GridPane();

    @Override
    public void start(Stage stage) {

        // ----------------------------
        // TABLE TAB UI
        // ----------------------------
        countryComboBox.setPromptText("Country");
        regionComboBox.setPromptText("Region");
        citySearchTextField.setPromptText("Search City...");

        Button applyButton = new Button("Apply");
        Button clearButton = new Button("Clear");
        Button exportButton = new Button("Export CSV");

        HBox filterBar = new HBox(10, countryComboBox, regionComboBox, citySearchTextField, applyButton, clearButton, exportButton);
        filterBar.setPadding(new Insets(10));

        setupTable();

        // ----------------------------
        // ANALYTICS TAB UI
        // ----------------------------
        VBox kpiBox = new VBox(
                6,
                new Label("KPIs"),
                new HBox(
                        20,
                        new VBox(4, new Label("Cities"), kpiCitiesLabel),
                        new VBox(4, new Label("Avg AirQuality"), kpiAvgAirLabel),
                        new VBox(4, new Label("Avg WaterPollution"), kpiAvgWaterLabel)
                )
        );
        kpiBox.setPadding(new Insets(10));

        airBarChart.setTitle("Top 10 Countries by Avg AirQuality");
        waterBarChart.setTitle("Top 10 Countries by Avg WaterPollution");

        scatterChart.setTitle("AirQuality vs WaterPollution (sample from current table view)");
        ((NumberAxis) scatterChart.getXAxis()).setLabel("AirQuality");
        ((NumberAxis) scatterChart.getYAxis()).setLabel("WaterPollution");

        // Analytics compare controls
        compareCountryComboBox.setPromptText("Choose country");
        compareTypeComboBox.setPromptText("Choose type");
        compareTypeComboBox.setItems(FXCollections.observableArrayList("Air", "Water"));
        compareTypeComboBox.setValue("Air");

        compareBarChart.setTitle("Selected Country vs Global Average");
        ((CategoryAxis) compareBarChart.getXAxis()).setLabel("Comparison");
        ((NumberAxis) compareBarChart.getYAxis()).setLabel("Average value");
        compareBarChart.setLegendVisible(false);

        compareCountryComboBox.setOnAction(event -> loadCountryComparisonChart());
        compareTypeComboBox.setOnAction(event -> loadCountryComparisonChart());

        HBox compareControls = new HBox(10, new Label("Compare:"), compareCountryComboBox, compareTypeComboBox);
        compareControls.setPadding(new Insets(10));

        makeChartBig(compareBarChart, 480);
        makeChartBig(airBarChart, 600);
        makeChartBig(waterBarChart, 600);
        makeChartBig(scatterChart, 700);

        VBox analyticsRoot = new VBox(10, compareControls, compareBarChart, kpiBox, airBarChart, waterBarChart, scatterChart);
        analyticsRoot.setPadding(new Insets(10));
        analyticsRoot.setFillWidth(true);
        analyticsRoot.setMaxWidth(Double.MAX_VALUE);
        analyticsRoot.getStyleClass().add("analytics-root");

        ScrollPane analyticsScroll = new ScrollPane(analyticsRoot);
        analyticsScroll.setFitToWidth(true);
        analyticsScroll.setFitToHeight(true);
        analyticsScroll.getStyleClass().add("analytics-scroll");

        // ----------------------------
        // NEW COMPARE TAB UI
        // ----------------------------
        VBox compareTwoRoot = buildTwoCountryComparePage();
        ScrollPane compareTwoScroll = new ScrollPane(compareTwoRoot);
        compareTwoScroll.setFitToWidth(true);
        compareTwoScroll.setFitToHeight(true);
        compareTwoScroll.getStyleClass().add("analytics-scroll"); // reuse dark scroll style

        // ----------------------------
        // TABS
        // ----------------------------
        TabPane tabPane = new TabPane();

        Tab tableTab = new Tab("Table", cityTableView);
        tableTab.setClosable(false);

        Tab analyticsTab = new Tab("Analytics", analyticsScroll);
        analyticsTab.setClosable(false);

        Tab compareTab = new Tab("Compare", compareTwoScroll);
        compareTab.setClosable(false);

        tabPane.getTabs().addAll(tableTab, analyticsTab, compareTab);

        VBox root = new VBox(10, filterBar, tabPane, statusLabel);
        root.setPadding(new Insets(10));
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/pink-theme.css").toExternalForm());

        stage.setTitle("Air & Water Pollution Dashboard");
        stage.setScene(scene);
        stage.show();

        // ----------------------------
        // EVENTS
        // ----------------------------
        countryComboBox.setOnAction(event -> loadRegions());
        applyButton.setOnAction(event -> refreshAll());
        clearButton.setOnAction(event -> clearFilters());
        exportButton.setOnAction(event -> exportTableToCsv(stage));

        // Initial load
        loadCountries();
    }

    // ----------------------------
    // NEW: Compare tab builder
    // ----------------------------
    private VBox buildTwoCountryComparePage() {

        compareCountryAComboBox.setPromptText("Country A");
        compareCountryBComboBox.setPromptText("Country B");

        compareMetricComboBox.setPromptText("Metric");
        compareMetricComboBox.setItems(FXCollections.observableArrayList("Air", "Water"));
        compareMetricComboBox.setValue("Air");

        twoCountryBarChart.setTitle("Country A vs Country B (selected metric)");
        ((CategoryAxis) twoCountryBarChart.getXAxis()).setLabel("Country");
        ((NumberAxis) twoCountryBarChart.getYAxis()).setLabel("Average value");
        twoCountryBarChart.setLegendVisible(false);

        makeChartBig(twoCountryBarChart, 520);

        // Grid with 2 rows (Air, Water) and 2 columns (country A, country B)
        twoCountryCardsGrid.setHgap(12);
        twoCountryCardsGrid.setVgap(12);
        twoCountryCardsGrid.setPadding(new Insets(10));

        compareCountryAComboBox.setOnAction(e -> loadTwoCountryPage());
        compareCountryBComboBox.setOnAction(e -> loadTwoCountryPage());
        compareMetricComboBox.setOnAction(e -> loadTwoCountryPage());

        Button refreshButton = new Button("Update");
        refreshButton.setOnAction(e -> loadTwoCountryPage());

        HBox controls = new HBox(10,
                new Label("Compare two countries:"),
                compareCountryAComboBox,
                compareCountryBComboBox,
                new Label("Metric:"),
                compareMetricComboBox,
                refreshButton
        );
        controls.setPadding(new Insets(10));

        VBox root = new VBox(12, controls, twoCountryBarChart, new Separator(), twoCountryCardsGrid);
        root.setPadding(new Insets(10));
        root.setFillWidth(true);
        root.setMaxWidth(Double.MAX_VALUE);
        root.getStyleClass().add("analytics-root");

        return root;
    }

    private void loadTwoCountryPage() {
        String countryA = compareCountryAComboBox.getValue();
        String countryB = compareCountryBComboBox.getValue();
        String metricText = compareMetricComboBox.getValue();

        if (countryA == null || countryB == null || metricText == null) {
            return;
        }
        if (countryA.equals(countryB)) {
            statusLabel.setText("Choose two different countries.");
            return;
        }

        Task<TwoCountryPageData> task = new Task<>() {
            @Override
            protected TwoCountryPageData call() {
                updateMessage("Loading 2-country comparison...");

                CityRepository.TwoCountryComparison air =
                        cityRepository.loadTwoCountriesVsGlobalAverage(countryA, countryB, CityRepository.PollutionType.AIR);

                CityRepository.TwoCountryComparison water =
                        cityRepository.loadTwoCountriesVsGlobalAverage(countryA, countryB, CityRepository.PollutionType.WATER);

                return new TwoCountryPageData(air, water);
            }
        };

        attachStatusListeners(task);

        task.setOnSucceeded(event -> {
            TwoCountryPageData data = task.getValue();

            // Top chart uses selected metric
            CityRepository.TwoCountryComparison selected =
                    metricText.equalsIgnoreCase("Air") ? data.air : data.water;

            updateTwoCountryChart(countryA, countryB, metricText, selected);

            // Cards always show BOTH rows: Air and Water
            updateTwoCountryCards(countryA, countryB, data.air, data.water);

            statusLabel.setText("Compare page updated.");
        });

        task.setOnFailed(event -> showTaskError("Failed to load compare page.", task));

        startTask(task);
    }

    private void updateTwoCountryChart(String countryA, String countryB, String metricText, CityRepository.TwoCountryComparison comparison) {
        twoCountryBarChart.getData().clear();
        twoCountryBarChart.setTitle(countryA + " vs " + countryB + " (" + metricText + ")");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>(countryA, comparison.countryAAvg()));
        series.getData().add(new XYChart.Data<>(countryB, comparison.countryBAvg()));
        series.getData().add(new XYChart.Data<>("Global Avg", comparison.globalAvg()));

        twoCountryBarChart.getData().add(series);
    }

    private void updateTwoCountryCards(
            String countryA,
            String countryB,
            CityRepository.TwoCountryComparison air,
            CityRepository.TwoCountryComparison water
    ) {
        twoCountryCardsGrid.getChildren().clear();

        // Header row
        Label empty = new Label("");
        Label headerA = new Label(countryA);
        Label headerB = new Label(countryB);

        headerA.getStyleClass().add("compare-header");
        headerB.getStyleClass().add("compare-header");

        twoCountryCardsGrid.add(empty, 0, 0);
        twoCountryCardsGrid.add(headerA, 1, 0);
        twoCountryCardsGrid.add(headerB, 2, 0);

        // Row labels
        Label airLabel = new Label("Air");
        airLabel.getStyleClass().add("compare-row-label");
        twoCountryCardsGrid.add(airLabel, 0, 1);

        Label waterLabel = new Label("Water");
        waterLabel.getStyleClass().add("compare-row-label");
        twoCountryCardsGrid.add(waterLabel, 0, 2);

        // Air cards (row 1)
        twoCountryCardsGrid.add(createMetricCard(countryA, "Air", air.countryAAvg(), air.globalAvg()), 1, 1);
        twoCountryCardsGrid.add(createMetricCard(countryB, "Air", air.countryBAvg(), air.globalAvg()), 2, 1);

        // Water cards (row 2)
        twoCountryCardsGrid.add(createMetricCard(countryA, "Water", water.countryAAvg(), water.globalAvg()), 1, 2);
        twoCountryCardsGrid.add(createMetricCard(countryB, "Water", water.countryBAvg(), water.globalAvg()), 2, 2);

        // Make columns stretch nicely
        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(70);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        twoCountryCardsGrid.getColumnConstraints().setAll(col0, col1, col2);
    }

    private VBox createMetricCard(String country, String metric, double countryAvg, double globalAvg) {
        boolean isAboveOrEqual = countryAvg >= globalAvg;

        Label title = new Label(country + " - " + metric);
        title.getStyleClass().add("metric-card-title");

        Label value = new Label(String.format("%.2f", countryAvg));
        value.getStyleClass().add("metric-card-value");

        String comparisonText = isAboveOrEqual
                ? "Above/Equal global avg (" + String.format("%.2f", globalAvg) + ")"
                : "Below global avg (" + String.format("%.2f", globalAvg) + ")";
        Label compare = new Label(comparisonText);
        compare.getStyleClass().add("metric-card-sub");

        VBox card = new VBox(6, title, value, compare);
        card.setPadding(new Insets(12));
        card.setMinHeight(120);
        card.setMaxWidth(Double.MAX_VALUE);

        card.getStyleClass().add("metric-card");
        card.getStyleClass().add(isAboveOrEqual ? "metric-good" : "metric-bad");

        return card;
    }

    // ----------------------------
    // Existing app methods
    // ----------------------------
    private void setupTable() {
        TableColumn<CityRow, String> cityColumn = new TableColumn<>("City");
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));

        TableColumn<CityRow, String> regionColumn = new TableColumn<>("Region");
        regionColumn.setCellValueFactory(new PropertyValueFactory<>("region"));

        TableColumn<CityRow, String> countryColumn = new TableColumn<>("Country");
        countryColumn.setCellValueFactory(new PropertyValueFactory<>("country"));

        TableColumn<CityRow, Double> airColumn = new TableColumn<>("AirQuality");
        airColumn.setCellValueFactory(new PropertyValueFactory<>("airQuality"));

        TableColumn<CityRow, Double> waterColumn = new TableColumn<>("WaterPollution");
        waterColumn.setCellValueFactory(new PropertyValueFactory<>("waterPollution"));

        cityTableView.getColumns().setAll(cityColumn, regionColumn, countryColumn, airColumn, waterColumn);
        cityTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadCountries() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                updateMessage("Loading countries...");
                return cityRepository.loadCountries();
            }
        };

        attachStatusListeners(task);

        task.setOnSucceeded(event -> {
            List<String> countries = task.getValue();

            // Table filter country list
            countryComboBox.setItems(FXCollections.observableArrayList(countries));

            // Analytics compare
            compareCountryComboBox.setItems(countryComboBox.getItems());
            if (compareCountryComboBox.getValue() == null && !compareCountryComboBox.getItems().isEmpty()) {
                compareCountryComboBox.setValue(compareCountryComboBox.getItems().get(0));
            }

            // NEW Compare tab: set both dropdowns
            compareCountryAComboBox.setItems(countryComboBox.getItems());
            compareCountryBComboBox.setItems(countryComboBox.getItems());

            if (compareCountryAComboBox.getValue() == null && !countries.isEmpty()) {
                compareCountryAComboBox.setValue(countries.get(0));
            }
            if (compareCountryBComboBox.getValue() == null && countries.size() >= 2) {
                compareCountryBComboBox.setValue(countries.get(1));
            } else if (compareCountryBComboBox.getValue() == null && !countries.isEmpty()) {
                compareCountryBComboBox.setValue(countries.get(0));
            }

            statusLabel.setText("Countries loaded.");
            refreshAll();
            loadCountryComparisonChart();
            loadTwoCountryPage();
        });

        task.setOnFailed(event -> showTaskError("Failed to load countries.", task));

        startTask(task);
    }

    private void loadRegions() {
        String selectedCountry = countryComboBox.getValue();
        regionComboBox.getItems().clear();
        regionComboBox.setValue(null);

        if (selectedCountry == null || selectedCountry.isBlank()) {
            return;
        }

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                updateMessage("Loading regions...");
                return cityRepository.loadRegionsForCountry(selectedCountry);
            }
        };

        attachStatusListeners(task);

        task.setOnSucceeded(event -> {
            regionComboBox.setItems(FXCollections.observableArrayList(task.getValue()));
            statusLabel.setText("Regions loaded.");
        });

        task.setOnFailed(event -> showTaskError("Failed to load regions.", task));

        startTask(task);
    }

    private void refreshAll() {
        loadTableRows();
        loadAnalytics();
    }

    private void clearFilters() {
        countryComboBox.setValue(null);
        regionComboBox.getItems().clear();
        regionComboBox.setValue(null);
        citySearchTextField.clear();
        refreshAll();
    }

    private void loadTableRows() {
        String selectedCountry = countryComboBox.getValue();
        String selectedRegion = regionComboBox.getValue();
        String citySearchText = citySearchTextField.getText();

        Task<List<CityRow>> task = new Task<>() {
            @Override
            protected List<CityRow> call() {
                updateMessage("Loading table rows...");
                return cityRepository.loadRows(selectedCountry, selectedRegion, citySearchText);
            }
        };

        attachStatusListeners(task);

        task.setOnSucceeded(event -> {
            List<CityRow> rows = task.getValue();
            cityTableView.setItems(FXCollections.observableArrayList(rows));
            statusLabel.setText("Loaded " + rows.size() + " rows (max 1000).");
            updateScatterFromCurrentTable();
        });

        task.setOnFailed(event -> showTaskError("Failed to load table rows.", task));

        startTask(task);
    }

    private void loadAnalytics() {
        String selectedCountry = countryComboBox.getValue();
        String selectedRegion = regionComboBox.getValue();
        String citySearchText = citySearchTextField.getText();

        Task<AnalyticsData> task = new Task<>() {
            @Override
            protected AnalyticsData call() {
                updateMessage("Loading analytics...");

                CityRepository.KpiSummary kpis = cityRepository.loadKpis(selectedCountry, selectedRegion, citySearchText);
                List<CityRepository.CountryMetric> topAir = cityRepository.loadTopCountriesByAverageAirQuality();
                List<CityRepository.CountryMetric> topWater = cityRepository.loadTopCountriesByAverageWaterPollution();

                return new AnalyticsData(kpis, topAir, topWater);
            }
        };

        attachStatusListeners(task);

        task.setOnSucceeded(event -> {
            AnalyticsData data = task.getValue();

            kpiCitiesLabel.setText(String.valueOf(data.kpis.cityCount()));
            kpiAvgAirLabel.setText(String.format("%.2f", data.kpis.averageAirQuality()));
            kpiAvgWaterLabel.setText(String.format("%.2f", data.kpis.averageWaterPollution()));

            updateBarChart(airBarChart, "Avg AirQuality", data.topAir);
            updateBarChart(waterBarChart, "Avg WaterPollution", data.topWater);

            statusLabel.setText("Analytics loaded.");
        });

        task.setOnFailed(event -> showTaskError("Failed to load analytics.", task));

        startTask(task);
    }

    private void loadCountryComparisonChart() {
        String selectedCountry = compareCountryComboBox.getValue();
        String selectedTypeText = compareTypeComboBox.getValue();

        if (selectedCountry == null || selectedCountry.isBlank() || selectedTypeText == null) {
            return;
        }

        CityRepository.PollutionType pollutionType =
                selectedTypeText.equalsIgnoreCase("Air")
                        ? CityRepository.PollutionType.AIR
                        : CityRepository.PollutionType.WATER;

        Task<CityRepository.TwoCountryComparison> task = new Task<>() {
            @Override
            protected CityRepository.TwoCountryComparison call() {
                updateMessage("Loading comparison chart...");
                // Reuse two-country method by comparing country vs itself (global still valid)
                return cityRepository.loadTwoCountriesVsGlobalAverage(selectedCountry, selectedCountry, pollutionType);
            }
        };

        attachStatusListeners(task);

        task.setOnSucceeded(event -> {
            CityRepository.TwoCountryComparison result = task.getValue();

            compareBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("Selected Country", result.countryAAvg()));
            series.getData().add(new XYChart.Data<>("Global Average", result.globalAvg()));
            compareBarChart.getData().add(series);

            statusLabel.setText("Comparison updated.");
        });

        task.setOnFailed(event -> showTaskError("Failed to load comparison chart.", task));

        startTask(task);
    }

    private void updateBarChart(BarChart<String, Number> barChart, String seriesName, List<CityRepository.CountryMetric> metrics) {
        barChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(seriesName);

        for (CityRepository.CountryMetric metric : metrics) {
            series.getData().add(new XYChart.Data<>(metric.country(), metric.value()));
        }

        barChart.getData().add(series);
    }

    private void updateScatterFromCurrentTable() {
        scatterChart.getData().clear();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Cities (current table view)");

        int limit = Math.min(300, cityTableView.getItems().size());
        for (int index = 0; index < limit; index++) {
            CityRow row = cityTableView.getItems().get(index);
            series.getData().add(new XYChart.Data<>(row.getAirQuality(), row.getWaterPollution()));
        }

        scatterChart.getData().add(series);
    }

    private void exportTableToCsv(Stage stage) {
        if (cityTableView.getItems() == null || cityTableView.getItems().isEmpty()) {
            statusLabel.setText("Nothing to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("export.csv");

        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile == null) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(selectedFile), StandardCharsets.UTF_8)
        )) {
            writer.write("City,Region,Country,AirQuality,WaterPollution");
            writer.newLine();

            for (CityRow row : cityTableView.getItems()) {
                writer.write(csv(row.getCity())); writer.write(",");
                writer.write(csv(row.getRegion())); writer.write(",");
                writer.write(csv(row.getCountry())); writer.write(",");
                writer.write(String.valueOf(row.getAirQuality())); writer.write(",");
                writer.write(String.valueOf(row.getWaterPollution()));
                writer.newLine();
            }

            statusLabel.setText("Exported: " + selectedFile.getAbsolutePath());

        } catch (Exception exception) {
            statusLabel.setText("Export failed: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    private String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    // Status updates without binding
    private void attachStatusListeners(Task<?> task) {
        task.messageProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                statusLabel.setText(newValue);
            }
        });
    }

    private void startTask(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showTaskError(String message, Task<?> task) {
        Throwable exception = task.getException();
        String details = (exception == null) ? "" : exception.getMessage();
        statusLabel.setText(message + (details == null ? "" : (" " + details)));
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    private void makeChartBig(Region region, double prefHeight) {
        region.setMinWidth(0);
        region.setMaxWidth(Double.MAX_VALUE);
        region.setPrefHeight(prefHeight);
        region.setMinHeight(prefHeight);
    }

    private record AnalyticsData(
            CityRepository.KpiSummary kpis,
            List<CityRepository.CountryMetric> topAir,
            List<CityRepository.CountryMetric> topWater
    ) {}

    private record TwoCountryPageData(
            CityRepository.TwoCountryComparison air,
            CityRepository.TwoCountryComparison water
    ) {}
}
