package cclerc.cat.view;

import cclerc.cat.Cat;
import cclerc.cat.Configuration.Configuration;
import cclerc.cat.GlobalMonitoring;
import cclerc.cat.PeriodicSpeedTest;
import cclerc.cat.model.SpeedTestServer;
import cclerc.services.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.*;

public class ConfigureSpeedTestDialog {

    private static ConfigureSpeedTestDialog configureSpeedTestDialogInstance = new ConfigureSpeedTestDialog();

    private final int SPEED_TEST_TABLE_ROWS_PER_PAGE = 15;
    private SpeedTestServer selectedSpeedTestServer;
    private SortedList<SpeedTestServer> sortedSpeedTestServers;
    private boolean hasConfigurationChanged = false;
    private List<Object> erroredFields = new ArrayList<>();

    private static ObservableList<SpeedTestServer> speedTestServers = FXCollections.observableArrayList();
    private static boolean firstDisplay = true;
    private static List<String> serverCountryFilterList = new ArrayList<>();

    // Display management
    private static Stage dialogStage = new Stage();


    @FXML TableView<SpeedTestServer> serversTableView;
    @FXML TableColumn<SpeedTestServer, String> nameColumn;
    @FXML TableColumn<SpeedTestServer, String> countryColumn;
    @FXML TableColumn<SpeedTestServer, String> cityColumn;
    @FXML TableColumn<SpeedTestServer, String> sponsorColumn;
    @FXML TableColumn<SpeedTestServer, Double> distanceColumn;
    @FXML Pagination serversPagination;
    @FXML Label serversCountLabel;
    @FXML TextField serverNameFilterTextField;
    @FXML TextField serverSponsorFilterTextField;
    @FXML ChoiceBox<String> serverCountryFilterChoiceBox;
    @FXML TextField serverCityFilterTextField;
    @FXML TextField serverDistanceFilterTextField;
    @FXML Button clearButton;
    @FXML Button refreshButton;
    @FXML Button saveButton;
    @FXML Button closeButton;
    @FXML RadioButton KbsRadioButton;
    @FXML RadioButton MbsRadioButton;
    @FXML RadioButton KBsRadioButton;
    @FXML RadioButton MBsRadioButton;
    @FXML RadioButton minutesRadioButton;
    @FXML RadioButton hoursRadioButton;
    @FXML RadioButton daysRadioButton;
    @FXML TextField socketTimeoutTextField;
    @FXML TextField downloadSetupTimeTextField;
    @FXML TextField uploadSetupTimeTextField;
    @FXML TextField uploadFileSizeTextField;
    @FXML TextField repeatDurationTextField;
    @FXML TextField reportIntervalTextField;
    @FXML CheckBox periodicTestEnabledCheckBox;
    @FXML TextField periodicTestPeriodTextField;
    @FXML TextField periodicTestOffsetTextField;
    @FXML CheckBox periodicTestEmailEnabledCheckBox;
    @FXML TextField periodicTestEmailPeriodTextField;

    /**
     * Creates instance of ConfigureSpeedTestDialog controller
     * Controller must be re-created at each opening because Paginaton badly displays page when resetting filter
     * @param aInParentStage      Parent stage of configure speed test dialog stage
     */
    public static ConfigureSpeedTestDialog getInstance(Stage aInParentStage) {

        FXMLLoader lDialogLoader = new FXMLLoader();

        try {

            // As building server list is long, it is done only the first time and a wait dialog box is displayed meanwhile
            if (firstDisplay) {

                WaitDialog lWaitDialog = Display.waitDialog(
                        Cat.getInstance().getMainStage(), Display.getViewResourceBundle().getString("configureSpeedTestDialog.serverList.wait"), Constants.IMAGE_SPEED_TEST);
                lWaitDialog.show(() -> {
                    buildServersList();
                    Platform.runLater(lWaitDialog::close);
                });

                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.initOwner(aInParentStage);
                firstDisplay = false;

            }

            // Load the fxml file and create a new stage for the popup dialog.
            lDialogLoader.setLocation(Cat.class.getResource("view/ConfigureSpeedTestDialog.fxml"));
            lDialogLoader.setResources(Display.getViewResourceBundle());
            VBox lDialogPane = lDialogLoader.load();

            // Create the dialog stage
            Scene lScene = new Scene(lDialogPane);
            lScene.getStylesheets().add("resources/css/view.css");
            dialogStage.setScene(lScene);
            dialogStage.getIcons().add(Constants.APPLICATION_IMAGE);
            dialogStage.setResizable(false);
            dialogStage.setTitle(Display.getViewResourceBundle().getString("configureSpeedTest.title"));
            configureSpeedTestDialogInstance = lDialogLoader.getController();
            configureSpeedTestDialogInstance.initializeConfiguration();
            configureSpeedTestDialogInstance.serverCountryFilterChoiceBox.setItems(FXCollections.observableArrayList(serverCountryFilterList));
            configureSpeedTestDialogInstance.saveButton.setDisable(true);
            configureSpeedTestDialogInstance.addTooltips();

        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.displayDialog"), Utilities.getStackTrace(e)));
        }

        return configureSpeedTestDialogInstance;

    }

    /**
     * Clears all filters
     */
    public void clearFilter() {
        serverNameFilterTextField.setText("");
        serverSponsorFilterTextField.setText("");
        serverCountryFilterChoiceBox.getSelectionModel().select("");
        serverCityFilterTextField.setText("");
        serverDistanceFilterTextField.setText("");
    }

    /**
     * Refreshes servers list
     */
    public void refreshServersList() {
        WaitDialog lWaitDialog = Display.waitDialog(
                Cat.getInstance().getMainStage(), Display.getViewResourceBundle().getString("configureSpeedTestDialog.serverList.wait"), Constants.IMAGE_SPEED_TEST);
        lWaitDialog.show(() -> {
            buildServersList();
            Platform.runLater(lWaitDialog::close);
        });
        prepareDisplay();
    }

    /**
     * Builds the speed test servers list
     */
    public static void buildServersList() {

        speedTestServers.clear();
        try {

            Proxy lProxy = Proxy.NO_PROXY;
            if ((Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN) == null) ||
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN).getUseProxy()) {
                lProxy = Network.findHttpProxy(Constants.SPEED_TEST_GET_SERVERS_URL);
            }

            // Build HTTP GET request to retrieve servers list from speedtest.net
            URL lUrl = new URL(Constants.SPEED_TEST_GET_SERVERS_URL);
            HttpURLConnection lConnection = (HttpURLConnection) lUrl.openConnection(lProxy);
            lConnection.setRequestMethod("GET");
            lConnection.setRequestProperty("Accept", "application/json");

            // Check errors
            if (lConnection.getResponseCode() != 200) {
                throw new ConnectException(lConnection.getResponseCode() + ": " + lConnection.getResponseMessage());
            }

            // Parse the result
            SAXBuilder lBuilder = new SAXBuilder();
            Document lDocument = lBuilder.build(lConnection.getInputStream());
            Element lRoot = lDocument.getRootElement();

            List<Element> lSpeedTestServers = lRoot.getChild("servers").getChildren("server");
            if (lSpeedTestServers != null) {

                // Parse speed test servers
                for (Element lSpeedTestServer: lSpeedTestServers) {
                    speedTestServers.add(new SpeedTestServer(lSpeedTestServer));
                    if (!serverCountryFilterList.contains(lSpeedTestServer.getAttributeValue("cc"))) serverCountryFilterList.add(lSpeedTestServer.getAttributeValue("cc"));
                }

            }
            // Add some servers extra server
            speedTestServers.add(new SpeedTestServer(
                    "intuxication.lafibre.info", "FR", "Vitry-sur-Seine", "Intuxication", 14.5d, "http://intuxication.lafibre.info/speedtest/upload.php"));
            speedTestServers.sort(Comparator.comparing(SpeedTestServer::getDistance));

            serverCountryFilterList.add("");
            Collections.sort(serverCountryFilterList);
            lConnection.disconnect();

        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }

    }

    /**
     * Creates tooltips
     */
    private void addTooltips() {

        String lTooltipText;
        Tooltip lTooltip;

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(serversTableView, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.name");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(serverNameFilterTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.sponsor");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(serverSponsorFilterTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.country");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(serverCountryFilterChoiceBox, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.city");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(serverCityFilterTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.distance");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(serverDistanceFilterTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.clear");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(clearButton, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.refresh");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(refreshButton, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.save");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(saveButton, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.close");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(closeButton, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.socketTimeout");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(socketTimeoutTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.downloadSetupTime");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(downloadSetupTimeTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.uploadSetupTime");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(uploadSetupTimeTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.uploadFileSize");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(uploadFileSizeTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.repeatDuration");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(repeatDurationTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.reportInterval");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(reportIntervalTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.periodicTest.enabled");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(periodicTestEnabledCheckBox, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.periodicTest.period");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(periodicTestPeriodTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.periodicTest.offset");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(periodicTestOffsetTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.periodicTest.email.enabled");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(periodicTestEmailEnabledCheckBox, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.periodicTest.email.period");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(periodicTestEmailPeriodTextField, lTooltip);

    }

    /**
     * Initializes all configuration data with data and behaviour
     */
    private void initializeConfiguration() {

        // Configure table view
        serversTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameColumn.setCellFactory(column -> stringFormatter());
        countryColumn.setCellValueFactory(cellData -> cellData.getValue().countryProperty());
        countryColumn.setCellFactory(column -> stringFormatter());
        cityColumn.setCellValueFactory(cellData -> cellData.getValue().cityProperty());
        cityColumn.setCellFactory(column -> stringFormatter());
        sponsorColumn.setCellValueFactory(cellData -> cellData.getValue().sponsorProperty());
        sponsorColumn.setCellFactory(column -> stringFormatter());
        distanceColumn.setCellValueFactory(cellData -> cellData.getValue().distanceProperty().asObject());
        distanceColumn.setCellFactory(column -> doubleFormatter());
        serversPagination.setPageFactory(this::createPage);

        // Wrap the ObservableList in a FilteredList (initially display all data).
        FilteredList<SpeedTestServer> lFilteredSpeedTestServers = new FilteredList<>(speedTestServers, p -> true);

        // Set the filter predicate whenever the filters change.
        serverNameFilterTextField.textProperty().addListener(speedTestServersFilterListener(lFilteredSpeedTestServers));
        serverSponsorFilterTextField.textProperty().addListener(speedTestServersFilterListener(lFilteredSpeedTestServers));
        serverCountryFilterChoiceBox.getSelectionModel().selectedItemProperty().addListener(speedTestServersFilterListener(lFilteredSpeedTestServers));
        serverCityFilterTextField.textProperty().addListener(speedTestServersFilterListener(lFilteredSpeedTestServers));
        serverDistanceFilterTextField.textProperty().addListener(speedTestServersFilterListener(lFilteredSpeedTestServers));

        // Wrap the FilteredList in a SortedList.
        sortedSpeedTestServers = new SortedList<>(lFilteredSpeedTestServers);

        // Bind the SortedList comparator to the TableView comparator.
        sortedSpeedTestServers.comparatorProperty().bind(serversTableView.comparatorProperty());

        // Add sorted (and filtered) data to the table.
        serversTableView.setItems(sortedSpeedTestServers);
        serversCountLabel.setText(String.valueOf(sortedSpeedTestServers.size()));

        // Set the speed test server count and refresh pages whenever the sorted speed test servers list changes
        sortedSpeedTestServers.addListener(speedTestServersListChangeListener());

        // Add listeners on different fields
        serversTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                checkChanges();
            }
        });
        socketTimeoutTextField.textProperty().addListener(integerTextFieldChangeListener(
                socketTimeoutTextField, Constants.SPEED_TEST_SOCKET_TIMEOUT_PREFERENCE, Constants.DEFAULT_SPEED_TEST_SOCKET_TIMEOUT));
        downloadSetupTimeTextField.textProperty().addListener(longTextFieldChangeListener(
                downloadSetupTimeTextField, Constants.SPEED_TEST_DOWNLOAD_SETUP_TIME_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DOWNLOAD_SETUP_TIME));
        uploadSetupTimeTextField.textProperty().addListener(longTextFieldChangeListener(
                uploadSetupTimeTextField, Constants.SPEED_TEST_UPLOAD_SETUP_TIME_PREFERENCE, Constants.DEFAULT_SPEED_TEST_UPLOAD_SETUP_TIME));
        uploadFileSizeTextField.textProperty().addListener(integerTextFieldChangeListener(
                uploadFileSizeTextField, Constants.SPEED_TEST_UPLOAD_FILE_SIZE_PREFERENCE, Constants.DEFAULT_SPEED_TEST_UPLOAD_FILE_SIZE));
        repeatDurationTextField.textProperty().addListener(integerTextFieldChangeListener(
                repeatDurationTextField, Constants.SPEED_TEST_REPEAT_DURATION_PREFERENCE, Constants.DEFAULT_SPEED_TEST_REPEAT_DURATION));
        reportIntervalTextField.textProperty().addListener(integerTextFieldChangeListener(
                reportIntervalTextField, Constants.SPEED_TEST_REPORT_INTERVAL_PREFERENCE, Constants.DEFAULT_SPEED_TEST_REPORT_INTERVAL));
        periodicTestEnabledCheckBox.selectedProperty().addListener(booleanTextFieldChangeListener(
                periodicTestEnabledCheckBox, Constants.SPEED_TEST_PERIODIC_TEST_ENABLED_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_ENABLED));
        periodicTestPeriodTextField.textProperty().addListener(integerTextFieldChangeListener(
                periodicTestPeriodTextField, Constants.SPEED_TEST_PERIODIC_TEST_PERIOD_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_PERIOD));
        periodicTestOffsetTextField.textProperty().addListener(integerTextFieldChangeListener(
                periodicTestOffsetTextField, Constants.SPEED_TEST_PERIODIC_TEST_OFFSET_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_OFFSET));
        periodicTestEmailEnabledCheckBox.selectedProperty().addListener(booleanTextFieldChangeListener(
                periodicTestEmailEnabledCheckBox, Constants.SPEED_TEST_EMAIL_REPORT_ENABLED_PREFERENCE, Constants.DEFAULT_SPEED_TEST_EMAIL_REPORT_ENABLED));
        periodicTestEmailPeriodTextField.textProperty().addListener(integerTextFieldChangeListener(
                periodicTestEmailPeriodTextField, Constants.SPEED_TEST_EMAIL_REPORT_PERIOD_PREFERENCE, Constants.DEFAULT_SPEED_TEST_EMAIL_PERIOD));

    }

    /**
     * Listener on changes on an integer text field
     * @return Listener
     */
    private ChangeListener<String> integerTextFieldChangeListener(TextField aInTextField, String aInPreference, Integer aInDefaultValue) {
        return (obs, oldValue, newValue) -> {
            try {
                Integer.valueOf(newValue);
                erroredFields.remove(aInTextField);
                setTextFieldStyle(aInTextField, Preferences.getInstance().getIntegerValue(aInPreference, aInDefaultValue), Integer.valueOf(newValue), aInDefaultValue);
                checkChanges();
                if (erroredFields.size() == 0 && hasConfigurationChanged) saveButton.setDisable(false);
            } catch (NumberFormatException e) {
                aInTextField.setId("bad-value");
                erroredFields.add(aInTextField);
                saveButton.setDisable(true);
            }
        };
    }

    /**
     * Listener on changes on an long text field
     * @return Listener
     */
    private ChangeListener<String> longTextFieldChangeListener(TextField aInTextField, String aInPreference, Long aInDefaultValue) {
        return (obs, oldValue, newValue) -> {
            try {
                Long.valueOf(newValue);
                erroredFields.remove(aInTextField);
                setTextFieldStyle(aInTextField, Preferences.getInstance().getLongValue(aInPreference, aInDefaultValue), Long.valueOf(newValue), aInDefaultValue);
                checkChanges();
                if (erroredFields.size() == 0 && hasConfigurationChanged) saveButton.setDisable(false);
            } catch (NumberFormatException e) {
                aInTextField.setId("bad-value");
                erroredFields.add(aInTextField);
                saveButton.setDisable(true);
            }
        };
    }

    /**
     * Listener on changes on an boolean text field
     * @return Listener
     */
    private ChangeListener<Boolean> booleanTextFieldChangeListener(CheckBox aInCheckBox, String aInPreference, Boolean aInDefaultValue) {

        return (obs, oldValue, newValue) -> {
            erroredFields.remove(aInCheckBox);
            setCheckBoxStyle(aInCheckBox, Preferences.getInstance().getBooleanValue(aInPreference, aInDefaultValue), newValue, aInDefaultValue);
            checkChanges();
            if (erroredFields.size() == 0 && hasConfigurationChanged) saveButton.setDisable(false);

            if (aInCheckBox.equals(periodicTestEnabledCheckBox)) {
                if (newValue) {
                    periodicTestPeriodTextField.setDisable(false);
                    periodicTestOffsetTextField.setDisable(false);
                    periodicTestEmailEnabledCheckBox.setDisable(false);
                    periodicTestEmailPeriodTextField.setDisable(false);
                } else {
                    periodicTestPeriodTextField.setDisable(true);
                    periodicTestOffsetTextField.setDisable(true);
                    periodicTestEmailEnabledCheckBox.setDisable(true);
                    periodicTestEmailPeriodTextField.setDisable(true);

                }
            } else if (aInCheckBox.equals(periodicTestEmailEnabledCheckBox)) {
                if (newValue) {
                    periodicTestEmailPeriodTextField.setDisable(false);
                } else {
                    periodicTestEmailPeriodTextField.setDisable(true);
                }
            }

        };

    }

    /**
     * Listener on changes on a sorted speed test servers list
     * @return Listener
     */
    private ListChangeListener<SpeedTestServer> speedTestServersListChangeListener() {
        return aInChanges -> Platform.runLater(() -> {
            serversCountLabel.setText(String.valueOf(sortedSpeedTestServers.size()));
            serversTableView.setItems(sortedSpeedTestServers);
            serversPagination.setPageCount(
                    Math.max(1, sortedSpeedTestServers.size() / SPEED_TEST_TABLE_ROWS_PER_PAGE + ((sortedSpeedTestServers.size() % SPEED_TEST_TABLE_ROWS_PER_PAGE) != 0 ? 1 : 0)));

            int lSelectedIndex = Math.max(0, sortedSpeedTestServers.indexOf(selectedSpeedTestServer));
            serversPagination.setCurrentPageIndex(lSelectedIndex / SPEED_TEST_TABLE_ROWS_PER_PAGE);
        });
    }

    /**
     * Listener on changes on speed test servers filter name field
     * @param aInSpeedTestServers Filtered speed test servers list to which the change of filter applies
     * @return Listener
     */
    private ChangeListener<String> speedTestServersFilterListener(FilteredList<SpeedTestServer> aInSpeedTestServers) {

        return (observable, oldValue, newValue) -> {

            if (newValue.equals(serverNameFilterTextField.getText()))
                Preferences.getInstance().saveValue(Constants.SPEED_TEST_NAME_FILTER_PREFERENCE, newValue);
            if (newValue.equals(serverSponsorFilterTextField.getText()))
                Preferences.getInstance().saveValue(Constants.SPEED_TEST_SPONSOR_FILTER_PREFERENCE, newValue);
            if (newValue.equals(serverCountryFilterChoiceBox.getSelectionModel().getSelectedItem()))
                Preferences.getInstance().saveValue(Constants.SPEED_TEST_COUNTRY_FILTER_PREFERENCE, newValue);
            if (newValue.equals(serverCityFilterTextField.getText()))
                Preferences.getInstance().saveValue(Constants.SPEED_TEST_CITY_FILTER_PREFERENCE, newValue);
            if (newValue.equals(serverDistanceFilterTextField.getText()))
                Preferences.getInstance().saveValue(Constants.SPEED_TEST_DISTANCE_FILTER_PREFERENCE, newValue);

            aInSpeedTestServers.setPredicate(speedTestServer -> {

                // If filter text is empty or filter matches the server, display it.
                if ((serverNameFilterTextField.getText().isEmpty() ||
                     speedTestServer.getName().toLowerCase().contains(serverNameFilterTextField.getText().toLowerCase())) &&
                    (serverSponsorFilterTextField.getText().isEmpty() ||
                     speedTestServer.getSponsor().toLowerCase().contains(serverSponsorFilterTextField.getText().toLowerCase())) &&
                    (serverCountryFilterChoiceBox.getSelectionModel().getSelectedItem() == null ||
                     speedTestServer.getCountry().contains(serverCountryFilterChoiceBox.getSelectionModel().getSelectedItem())) &&
                    (serverCityFilterTextField.getText().isEmpty() ||
                     speedTestServer.getCity().toLowerCase().contains(serverCityFilterTextField.getText().toLowerCase())) &&
                    (serverDistanceFilterTextField.getText().isEmpty() ||
                     Double.valueOf(serverDistanceFilterTextField.getText()) >=  Double.valueOf(speedTestServer.getDistance()))) {
                    return true;
                }
                return false; // Does not match.

            });

        };

    }

    // FORMATTERS

    /**
     * Formats strings for table view columns display
     * @return Formatted string
     */
    private TableCell<SpeedTestServer, String> stringFormatter() {
        return new TableCell<SpeedTestServer, String>() {
            @Override
            protected void updateItem(String aInItem, boolean aInEmpty) {
                super.updateItem(aInItem, aInEmpty);
                if (aInItem != null && !aInEmpty) {
                    setText(aInItem);
                } else {
                    setText("");
                }
            }
        };
    }

    /**
     * Formats double for table view columns display
     * @return Formatted string
     */
    private TableCell<SpeedTestServer, Double>doubleFormatter() {
        return new TableCell<SpeedTestServer, Double>() {
            @Override
            protected void updateItem(Double aInItem, boolean aInEmpty) {
                super.updateItem(aInItem, aInEmpty);
                if (aInItem != null && !aInEmpty) {
                    setText(aInItem.toString());
                    setStyle("-fx-alignment: top-center");
                } else {
                    setText("");
                }
            }
        };
    }

    /**
     * Creates page in the speed test table
     * @param aInPageIndex Page index
     * @return
     */
    private Node createPage(int aInPageIndex) {

        serversPagination.setPageCount(sortedSpeedTestServers.size() / SPEED_TEST_TABLE_ROWS_PER_PAGE + ((sortedSpeedTestServers.size() % SPEED_TEST_TABLE_ROWS_PER_PAGE) != 0 ? 1 : 0));
        int fromIndex = aInPageIndex * SPEED_TEST_TABLE_ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + SPEED_TEST_TABLE_ROWS_PER_PAGE, sortedSpeedTestServers.size());
        serversTableView.setItems(FXCollections.observableArrayList(sortedSpeedTestServers.subList(fromIndex, toIndex)));
        if (selectedSpeedTestServer != null &&
            sortedSpeedTestServers.indexOf(selectedSpeedTestServer) >= fromIndex &&  sortedSpeedTestServers.indexOf(selectedSpeedTestServer) <= toIndex) {
            serversTableView.getSelectionModel().select(selectedSpeedTestServer);
        }
        return new FlowPane(serversTableView);

    }

    /**
     * Sets integer text fields style depending on the value
     * @param aInTextField    Text field to style
     * @param aInInitialValue Initial value of the text field
     * @param aInNewValue     New value of the text field
     * @param aInDefaultValue Default value of the text field
     */
    private void setTextFieldStyle(TextField aInTextField, Integer aInInitialValue, Integer aInNewValue, Integer aInDefaultValue) {
        if (Integer.valueOf(aInNewValue).equals(aInDefaultValue)) {
            if (aInNewValue.equals(aInInitialValue)) {
                aInTextField.setId("default-value");
            } else {
                aInTextField.setId("default-new-value");
            }
        } else {
            if (aInNewValue.equals(aInInitialValue)) {
                aInTextField.setId("");
            } else {
                aInTextField.setId("new-value");
            }
        }
    }

    /**
     * Sets long text fields style depending on the value
     * @param aInTextField    Text field to style
     * @param aInInitialValue Initial value of the text field
     * @param aInNewValue     New value of the text field
     * @param aInDefaultValue Default value of the text field
     */
    private void setTextFieldStyle(TextField aInTextField, Long aInInitialValue, Long aInNewValue, Long aInDefaultValue) {
        if (Long.valueOf(aInNewValue).equals(aInDefaultValue)) {
            if (aInNewValue.equals(aInInitialValue)) {
                aInTextField.setId("default-value");
            } else {
                aInTextField.setId("default-new-value");
            }
        } else {
            if (aInNewValue.equals(aInInitialValue)) {
                aInTextField.setId("");
            } else {
                aInTextField.setId("new-value");
            }
        }
    }

    /**
     * Sets check boxes style depending on the value
     * @param aInCheckBox     Check box to style
     * @param aInInitialValue Initial value of the check box
     * @param aInNewValue     New value of the check box
     * @param aInDefaultValue Default value of the check box
     */
    private void setCheckBoxStyle(CheckBox aInCheckBox, Boolean aInInitialValue, Boolean aInNewValue, Boolean aInDefaultValue) {
        if (aInNewValue == aInDefaultValue) {
            if (aInNewValue == aInInitialValue) {
                aInCheckBox.setId("default-value");
            } else {
                aInCheckBox.setId("default-new-value");
            }
        } else {
            if (aInNewValue == aInInitialValue) {
                aInCheckBox.setId("");
            } else {
                aInCheckBox.setId("new-value");
            }
        }
    }

    /**
     * Set style of all controls
     */
    private void setStyles() {
        setTextFieldStyle(socketTimeoutTextField, Integer.valueOf(socketTimeoutTextField.getText()), Integer.valueOf(socketTimeoutTextField.getText()),
                Constants.DEFAULT_SPEED_TEST_SOCKET_TIMEOUT);
        setTextFieldStyle(downloadSetupTimeTextField, Long.valueOf(downloadSetupTimeTextField.getText()), Long.valueOf(downloadSetupTimeTextField.getText()),
                Constants.DEFAULT_SPEED_TEST_DOWNLOAD_SETUP_TIME);
        setTextFieldStyle(uploadSetupTimeTextField, Long.valueOf(uploadSetupTimeTextField.getText()), Long.valueOf(uploadSetupTimeTextField.getText()),
                          Constants.DEFAULT_SPEED_TEST_UPLOAD_SETUP_TIME);
        setTextFieldStyle(uploadFileSizeTextField, Integer.valueOf(uploadFileSizeTextField.getText()), Integer.valueOf(uploadFileSizeTextField.getText()),
                          Constants.DEFAULT_SPEED_TEST_UPLOAD_FILE_SIZE);
        setTextFieldStyle(repeatDurationTextField, Integer.valueOf(repeatDurationTextField.getText()), Integer.valueOf(repeatDurationTextField.getText()),
                          Constants.DEFAULT_SPEED_TEST_REPEAT_DURATION);
        setTextFieldStyle(reportIntervalTextField, Integer.valueOf(reportIntervalTextField.getText()), Integer.valueOf(reportIntervalTextField.getText()),
                          Constants.DEFAULT_SPEED_TEST_REPORT_INTERVAL);
        setCheckBoxStyle(periodicTestEnabledCheckBox, periodicTestEnabledCheckBox.isSelected(), periodicTestEnabledCheckBox.isSelected(),
                Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_ENABLED);
        setTextFieldStyle(periodicTestPeriodTextField, Integer.valueOf(periodicTestPeriodTextField.getText()), Integer.valueOf(periodicTestPeriodTextField.getText()),
                          Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_PERIOD);
        setTextFieldStyle(periodicTestOffsetTextField, Integer.valueOf(periodicTestOffsetTextField.getText()), Integer.valueOf(periodicTestOffsetTextField.getText()),
                          Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_OFFSET);
        setCheckBoxStyle(periodicTestEmailEnabledCheckBox, periodicTestEmailEnabledCheckBox.isSelected(), periodicTestEmailEnabledCheckBox.isSelected(),
                         Constants.DEFAULT_SPEED_TEST_EMAIL_REPORT_ENABLED);
        setTextFieldStyle(periodicTestEmailPeriodTextField, Integer.valueOf(periodicTestEmailPeriodTextField.getText()), Integer.valueOf(periodicTestEmailPeriodTextField.getText()),
                          Constants.DEFAULT_SPEED_TEST_EMAIL_PERIOD);
    }

    // FXML

    /**
     * Set displayed unit to Kbs
     */
    @FXML private void setUnitToKbs() {
        KBsRadioButton.setSelected(false);
        MbsRadioButton.setSelected(false);
        MBsRadioButton.setSelected(false);
        checkChanges();
    }

    /**
     * Set displayed unit to Mbs
     */
    @FXML private void setUnitToMbs() {
        KbsRadioButton.setSelected(false);
        KBsRadioButton.setSelected(false);
        MBsRadioButton.setSelected(false);
        checkChanges();
    }

    /**
     * Set displayed unit to KBs
     */
    @FXML private void setUnitToKBs() {
        KbsRadioButton.setSelected(false);
        MbsRadioButton.setSelected(false);
        MBsRadioButton.setSelected(false);
        checkChanges();
    }

    /**
     * Set displayed unit to MBs
     */
    @FXML private void setUnitToMBs() {
        KbsRadioButton.setSelected(false);
        KBsRadioButton.setSelected(false);
        MbsRadioButton.setSelected(false);
        checkChanges();
    }

    /**
     * Set unit to minutes
     */
    @FXML private void setUnitToMinutes() {
        hoursRadioButton.setSelected(false);
        daysRadioButton.setSelected(false);
        checkChanges();
    }

    /**
     * Set unit to hours
     */
    @FXML private void setUnitToHours() {
        minutesRadioButton.setSelected(false);
        daysRadioButton.setSelected(false);
        checkChanges();
    }

    /**
     * Set unit to days
     */
    @FXML private void setUnitToDays() {
        minutesRadioButton.setSelected(false);
        hoursRadioButton.setSelected(false);
        checkChanges();
    }

    /**
     * Cancels changes if confirmed and closes dialog box
     */
    @FXML private void close() {

        // Ask user for confirmation if needed
        if (hasConfigurationChanged) {
            Alert lConfirmation =
                    new Alert(Alert.AlertType.CONFIRMATION, Display.getViewResourceBundle().getString("confirm.cancelConfiguration.question"), ButtonType.YES, ButtonType.NO);
            lConfirmation.setHeaderText(Display.getViewResourceBundle().getString("confirm.cancelConfiguration.title"));
            lConfirmation.initModality(Modality.APPLICATION_MODAL);

            // Display confirmation dialog box
            Optional<ButtonType> lResponse = lConfirmation.showAndWait();

            // OK is pressed
            if (lResponse.isPresent() && lResponse.get().equals(ButtonType.YES)) {
                dialogStage.close();
            }
        } else {
            dialogStage.close();
        }

    }

    /**
     * Saves configuration and exits dialog box
     */
    @FXML private  void save() {

        SpeedTestServer lSpeedTestServer = serversTableView.getSelectionModel().getSelectedItem();
        if (lSpeedTestServer != null) {
            Preferences.getInstance().saveValue(Constants.SPEED_TEST_SERVER_NAME_PREFERENCE, lSpeedTestServer.getName());
            Preferences.getInstance().saveValue(Constants.SPEED_TEST_SERVER_SPONSOR_PREFERENCE, lSpeedTestServer.getSponsor() + " (" + lSpeedTestServer.getCity() + ')');
            Preferences.getInstance().saveValue(Constants.SPEED_TEST_SERVER_URL_PREFERENCE, lSpeedTestServer.getUrl());
        }
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_SOCKET_TIMEOUT_PREFERENCE, socketTimeoutTextField.getText());
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_DOWNLOAD_SETUP_TIME_PREFERENCE, downloadSetupTimeTextField.getText());
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_UPLOAD_SETUP_TIME_PREFERENCE, uploadSetupTimeTextField.getText());
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_UPLOAD_FILE_SIZE_PREFERENCE, uploadFileSizeTextField.getText());
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_REPEAT_DURATION_PREFERENCE, repeatDurationTextField.getText());
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_REPORT_INTERVAL_PREFERENCE, reportIntervalTextField.getText());
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_PERIODIC_TEST_ENABLED_PREFERENCE, periodicTestEnabledCheckBox.isSelected());
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_PERIODIC_TEST_PERIOD_PREFERENCE, periodicTestPeriodTextField.getText());
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_PERIODIC_TEST_OFFSET_PREFERENCE, periodicTestOffsetTextField.getText());
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_EMAIL_REPORT_ENABLED_PREFERENCE, periodicTestEmailEnabledCheckBox.isSelected());
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_EMAIL_REPORT_PERIOD_PREFERENCE, periodicTestEmailPeriodTextField.getText());

        Long lDisplayedUnitRatio; String lDisplayedUnitKey;
        if (KbsRadioButton.isSelected()) {
            lDisplayedUnitRatio = Constants.Kbs;
            lDisplayedUnitKey = "bitRate.1";
        } else if (MbsRadioButton.isSelected()) {
            lDisplayedUnitRatio = Constants.Mbs;
            lDisplayedUnitKey = "bitRate.2";
        } else if (KBsRadioButton.isSelected()) {
            lDisplayedUnitRatio = Constants.KBs;
            lDisplayedUnitKey = "octetRate.1";
        } else {
            lDisplayedUnitRatio = Constants.MBs;
            lDisplayedUnitKey = "octetRate.2";
        }
        boolean hasUnitChanged =
                !Preferences.getInstance().getValue(Constants.SPEED_TEST_DISPLAY_UNIT_KEY_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DISPLAY_UNIT_KEY).equals(lDisplayedUnitKey);
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_DISPLAY_UNIT_RATIO_PREFERENCE, lDisplayedUnitRatio);
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_DISPLAY_UNIT_KEY_PREFERENCE, lDisplayedUnitKey);

        Integer lDisplayedUnitPeriod;
        if (minutesRadioButton.isSelected()) {
            lDisplayedUnitPeriod = Constants.MINUTES;
        } else if (hoursRadioButton.isSelected()) {
            lDisplayedUnitPeriod = Constants.HOURS;
        } else {
            lDisplayedUnitPeriod = Constants.DAYS;
        }
        Preferences.getInstance().saveValue(Constants.SPEED_TEST_DISPLAY_UNIT_PERIOD_PREFERENCE, lDisplayedUnitPeriod);

        setStyles();

        Cat.getInstance().getController().reloadSpeedTestConfiguration(hasUnitChanged);
        PeriodicSpeedTest.getInstance().loadConfiguration();

        hasConfigurationChanged = false;
        saveButton.setDisable(true);

    }

    /**
     * Checks if configuration has changed
     */
    private void checkChanges() {
        Long lThroughputDisplayedUnit = Preferences.getInstance().getLongValue(Constants.SPEED_TEST_DISPLAY_UNIT_RATIO_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DISPLAY_UNIT);
        Integer lPeriodDisplayedUnit = Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_DISPLAY_UNIT_PERIOD_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DISPLAY_UNIT_PERIOD);
        if (erroredFields.size() == 0 &&
            serversTableView.getSelectionModel().getSelectedItem() != null &&
            serversTableView.getSelectionModel().getSelectedItem().getName().equals(Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_NAME_PREFERENCE)) &&
            Integer.valueOf(socketTimeoutTextField.getText()).equals(
                    Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_SOCKET_TIMEOUT_PREFERENCE, Constants.DEFAULT_SPEED_TEST_SOCKET_TIMEOUT)) &&
            Long.valueOf(downloadSetupTimeTextField.getText()).equals(
                    Preferences.getInstance().getLongValue(Constants.SPEED_TEST_DOWNLOAD_SETUP_TIME_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DOWNLOAD_SETUP_TIME)) &&
            Long.valueOf(uploadSetupTimeTextField.getText()).equals(
                    Preferences.getInstance().getLongValue(Constants.SPEED_TEST_UPLOAD_SETUP_TIME_PREFERENCE, Constants.DEFAULT_SPEED_TEST_UPLOAD_SETUP_TIME)) &&
            Integer.valueOf(uploadFileSizeTextField.getText()).equals(
                    Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_UPLOAD_FILE_SIZE_PREFERENCE, Constants.DEFAULT_SPEED_TEST_UPLOAD_FILE_SIZE)) &&
            Integer.valueOf(repeatDurationTextField.getText()).equals(
                    Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_REPEAT_DURATION_PREFERENCE, Constants.DEFAULT_SPEED_TEST_REPEAT_DURATION)) &&
            Integer.valueOf(reportIntervalTextField.getText()).equals(
                    Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_REPORT_INTERVAL_PREFERENCE, Constants.DEFAULT_SPEED_TEST_REPORT_INTERVAL)) &&
            periodicTestEnabledCheckBox.isSelected() ==
            Preferences.getInstance().getBooleanValue(Constants.SPEED_TEST_PERIODIC_TEST_ENABLED_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_ENABLED) &&
            Integer.valueOf(periodicTestPeriodTextField.getText()).equals(
                    Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_PERIODIC_TEST_PERIOD_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_PERIOD)) &&
            Integer.valueOf(periodicTestOffsetTextField.getText()).equals(
                    Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_PERIODIC_TEST_OFFSET_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_OFFSET)) &&
            periodicTestEmailEnabledCheckBox.isSelected() ==
            Preferences.getInstance().getBooleanValue(Constants.SPEED_TEST_EMAIL_REPORT_ENABLED_PREFERENCE, Constants.DEFAULT_SPEED_TEST_EMAIL_REPORT_ENABLED) &&
            Integer.valueOf(periodicTestEmailPeriodTextField.getText()).equals(
                    Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_EMAIL_REPORT_PERIOD_PREFERENCE, Constants.DEFAULT_SPEED_TEST_EMAIL_PERIOD)) &&
            ((lThroughputDisplayedUnit.equals(Constants.Kbs) && KbsRadioButton.isSelected()) || (lThroughputDisplayedUnit.equals(Constants.Mbs) && MbsRadioButton.isSelected()) ||
             (lThroughputDisplayedUnit.equals(Constants.KBs) && KBsRadioButton.isSelected()) || (lThroughputDisplayedUnit.equals(Constants.MBs) && MBsRadioButton.isSelected()))  &&
            ((lPeriodDisplayedUnit.equals(Constants.MINUTES) && minutesRadioButton.isSelected()) ||
             (lPeriodDisplayedUnit.equals(Constants.HOURS) && hoursRadioButton.isSelected()) ||
             (lPeriodDisplayedUnit.equals(Constants.DAYS) && daysRadioButton.isSelected()))
                ) {
            hasConfigurationChanged = false;
            saveButton.setDisable(true);
        } else {
            hasConfigurationChanged = true;
            saveButton.setDisable(false);
        }
    }

    /**
     * Prepares the display
     */
    private void prepareDisplay() {

        // Retrieve current url
        String lCurrentUrl = Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_URL_PREFERENCE);

        serverNameFilterTextField.setText(Preferences.getInstance().getValue(Constants.SPEED_TEST_NAME_FILTER_PREFERENCE, ""));
        serverSponsorFilterTextField.setText(Preferences.getInstance().getValue(Constants.SPEED_TEST_SPONSOR_FILTER_PREFERENCE, ""));
        serverCountryFilterChoiceBox.getSelectionModel().select(Preferences.getInstance().getValue(Constants.SPEED_TEST_COUNTRY_FILTER_PREFERENCE, ""));
        serverCityFilterTextField.setText(Preferences.getInstance().getValue(Constants.SPEED_TEST_CITY_FILTER_PREFERENCE, ""));
        serverDistanceFilterTextField.setText(Preferences.getInstance().getValue(Constants.SPEED_TEST_DISTANCE_FILTER_PREFERENCE, ""));

        Long lDisplayedThroughputUnit = Preferences.getInstance().getLongValue(Constants.SPEED_TEST_DISPLAY_UNIT_RATIO_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DISPLAY_UNIT);
        if (lDisplayedThroughputUnit.equals(Constants.Kbs)) KbsRadioButton.setSelected(true);
        else if (lDisplayedThroughputUnit.equals(Constants.Mbs)) MbsRadioButton.setSelected(true);
        else if (lDisplayedThroughputUnit.equals(Constants.KBs)) KBsRadioButton.setSelected(true);
        else MBsRadioButton.setSelected(true);

        Integer lDisplayedPeriodUnit =
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_DISPLAY_UNIT_PERIOD_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DISPLAY_UNIT_PERIOD);
        if (lDisplayedPeriodUnit.equals(Constants.MINUTES)) minutesRadioButton.setSelected(true);
        else if (lDisplayedPeriodUnit.equals(Constants.HOURS)) hoursRadioButton.setSelected(true);
        else daysRadioButton.setSelected(true);

        // Select it in the table view if it exists
        for (SpeedTestServer lSpeedTestServer: sortedSpeedTestServers) {
            if (lSpeedTestServer.getUrl().equals(lCurrentUrl)) {
                // Goto relevant page and select server
                selectedSpeedTestServer = lSpeedTestServer;
                Platform.runLater(() -> {
                    serversPagination.setCurrentPageIndex(sortedSpeedTestServers.indexOf(selectedSpeedTestServer) / SPEED_TEST_TABLE_ROWS_PER_PAGE);
                    serversTableView.getSelectionModel().select(selectedSpeedTestServer);
                });
                break;
            }
        }

        // Initialize different fields
        socketTimeoutTextField.setText(
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_SOCKET_TIMEOUT_PREFERENCE, Constants.DEFAULT_SPEED_TEST_SOCKET_TIMEOUT).toString());
        downloadSetupTimeTextField.setText(
                Preferences.getInstance().getLongValue(Constants.SPEED_TEST_DOWNLOAD_SETUP_TIME_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DOWNLOAD_SETUP_TIME).toString());
        uploadSetupTimeTextField.setText(
                Preferences.getInstance().getLongValue(Constants.SPEED_TEST_UPLOAD_SETUP_TIME_PREFERENCE, Constants.DEFAULT_SPEED_TEST_UPLOAD_SETUP_TIME).toString());
        uploadFileSizeTextField.setText(
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_UPLOAD_FILE_SIZE_PREFERENCE, Constants.DEFAULT_SPEED_TEST_UPLOAD_FILE_SIZE).toString());
        repeatDurationTextField.setText(
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_REPEAT_DURATION_PREFERENCE, Constants.DEFAULT_SPEED_TEST_REPEAT_DURATION).toString());
        reportIntervalTextField.setText(
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_REPORT_INTERVAL_PREFERENCE, Constants.DEFAULT_SPEED_TEST_REPORT_INTERVAL).toString());
        periodicTestEnabledCheckBox.setSelected(
                Preferences.getInstance().getBooleanValue(Constants.SPEED_TEST_PERIODIC_TEST_ENABLED_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_ENABLED));
        periodicTestPeriodTextField.setText(
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_PERIODIC_TEST_PERIOD_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_PERIOD).toString());
        periodicTestOffsetTextField.setText(
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_PERIODIC_TEST_OFFSET_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_OFFSET).toString());
        periodicTestEmailEnabledCheckBox.setSelected(
                Preferences.getInstance().getBooleanValue(Constants.SPEED_TEST_EMAIL_REPORT_ENABLED_PREFERENCE, Constants.DEFAULT_SPEED_TEST_EMAIL_REPORT_ENABLED));
        periodicTestEmailPeriodTextField.setText(
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_EMAIL_REPORT_PERIOD_PREFERENCE, Constants.DEFAULT_SPEED_TEST_EMAIL_PERIOD).toString());

        if (periodicTestEnabledCheckBox.isSelected()) {
            periodicTestPeriodTextField.setDisable(false);
            periodicTestOffsetTextField.setDisable(false);
            periodicTestEmailEnabledCheckBox.setDisable(false);
            if (periodicTestEmailEnabledCheckBox.isSelected()) {
                periodicTestEmailPeriodTextField.setDisable(false);
            } else {
                periodicTestEmailPeriodTextField.setDisable(true);
            }
        } else {
            periodicTestPeriodTextField.setDisable(true);
            periodicTestOffsetTextField.setDisable(true);
            periodicTestEmailEnabledCheckBox.setDisable(true);
            periodicTestEmailPeriodTextField.setDisable(true);

        }

    }

    // PUBLIC

    /**
     * Displays the dialog box
     */
    public void show() {
        prepareDisplay();
        setStyles();
        dialogStage.showAndWait();
    }

}
