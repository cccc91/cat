package cclerc.cat.view;

import cclerc.cat.Cat;
import cclerc.cat.Configuration.Configuration;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ConfigureSpeedTestDialog {

    private static ConfigureSpeedTestDialog configureSpeedTestDialogInstance = new ConfigureSpeedTestDialog();

    private final int SPEED_TEST_TABLE_ROWS_PER_PAGE = 15;
    private SpeedTestServer selectedSpeedTestServer;
    private SortedList<SpeedTestServer> sortedSpeedTestServers;
    private boolean hasConfigurationChanged = false;

    private static ObservableList<SpeedTestServer> speedTestServers = FXCollections.observableArrayList();
    private static boolean firstDisplay = true;
    private static String serverNameFilter;
    private static List<String> serverCountryFilterList = new ArrayList<>();
    private static String serverCountryFilter;
    private static String serverCityFilter;
    private static String serverDistanceFilter;

    // Display management
    private static Stage dialogStage = new Stage();


    @FXML TableView<SpeedTestServer> serversTableView;
    @FXML TableColumn<SpeedTestServer, String> nameColumn;
    @FXML TableColumn<SpeedTestServer, String> countryColumn;
    @FXML TableColumn<SpeedTestServer, String> cityColumn;
    @FXML TableColumn<SpeedTestServer, Double> distanceColumn;
    @FXML Pagination serversPagination;
    @FXML Label serversCountLabel;
    @FXML TextField serverNameFilterTextField;
    @FXML ChoiceBox<String> serverCountryFilterChoiceBox;
    @FXML TextField serverCityFilterTextField;
    @FXML TextField serverDistanceFilterTextField;
    @FXML Button refreshButton;
    @FXML Button saveButton;
    @FXML Button closeButton;
    @FXML TextField socketTimeout;
    @FXML TextField downloadSetupTime;
    @FXML TextField uploadSetupTime;
    @FXML TextField uploadFileSize;
    @FXML TextField repeatDuration;
    @FXML TextField reportInterval;
    @FXML TextField periodicTestEnabled;
    @FXML TextField periodicTestPeriod;
    @FXML TextField periodicTestOffset;
    @FXML TextField periodicTestEmailEnabled;
    @FXML TextField periodicTestEmailPeriod;

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
            configureSpeedTestDialogInstance.initializeSpeedTestServersTable();
            configureSpeedTestDialogInstance.serverCountryFilterChoiceBox.setItems(FXCollections.observableArrayList(serverCountryFilterList));
            configureSpeedTestDialogInstance.saveButton.setDisable(true);
            configureSpeedTestDialogInstance.addTooltips();

            // TODO: replace with something getting current localization
            GeoLocalization.getInstance().getLocalGeoLocalization();

        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.displayDialog"), Utilities.getStackTrace(e)));
        }

        return configureSpeedTestDialogInstance;

    }

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
            Document lDocument = (Document) lBuilder.build(lConnection.getInputStream());
            Element lRoot = lDocument.getRootElement();

            List<Element> lSpeedTestServers = lRoot.getChild("servers").getChildren("server");
            if (lSpeedTestServers != null) {

                // Parse speed test servers
                for (Element lSpeedTestServer: lSpeedTestServers) {
                    speedTestServers.add(new SpeedTestServer(lSpeedTestServer));
                    if (!serverCountryFilterList.contains(lSpeedTestServer.getAttributeValue("cc"))) serverCountryFilterList.add(lSpeedTestServer.getAttributeValue("cc"));
                }

            }
            serverCountryFilterList.add("");
            Collections.sort(serverCountryFilterList);
            lConnection.disconnect();

        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }

    }

    public void addTooltips() {

        String lTooltipText;
        Tooltip lTooltip;

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(serversTableView, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.name");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(serverNameFilterTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.country");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(serverCountryFilterChoiceBox, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.city");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(serverCityFilterTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.distance");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(serverDistanceFilterTextField, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.server.refresh");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(refreshButton, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.save");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(saveButton, lTooltip);

        lTooltipText = Display.getViewResourceBundle().getString("configureSpeedTestDialog.tooltip.close");
        lTooltip = new Tooltip(lTooltipText);
        Tooltip.install(closeButton, lTooltip);

    }

    /**
     * Initializes the speed test servers table with data and behaviour
     */
    public void initializeSpeedTestServersTable() {

        // Configure table view
        serversTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameColumn.setCellFactory(column -> stringFormatter());
        countryColumn.setCellValueFactory(cellData -> cellData.getValue().countryProperty());
        countryColumn.setCellFactory(column -> stringFormatter());
        cityColumn.setCellValueFactory(cellData -> cellData.getValue().cityProperty());
        cityColumn.setCellFactory(column -> stringFormatter());
        distanceColumn.setCellValueFactory(cellData -> cellData.getValue().distanceProperty().asObject());
        distanceColumn.setCellFactory(column -> doubleFormatter());
        serversPagination.setPageFactory(this::createPage);

        // Wrap the ObservableList in a FilteredList (initially display all data).
        FilteredList<SpeedTestServer> lFilteredSpeedTestServers = new FilteredList<>(speedTestServers, p -> true);

        // Set the filter predicate whenever the filters change.
        serverNameFilterTextField.textProperty().addListener(speedTestServersFilterListener(lFilteredSpeedTestServers));
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
                serverNameFilter = serverNameFilterTextField.getText();
            if (newValue.equals(serverCountryFilterChoiceBox.getSelectionModel().getSelectedItem()))
                serverCountryFilter = serverCountryFilterChoiceBox.getSelectionModel().getSelectedItem();
            if (newValue.equals(serverCityFilterTextField.getText()))
                serverCityFilter = serverCityFilterTextField.getText();
            if (newValue.equals(serverDistanceFilterTextField.getText()))
                serverDistanceFilter = serverDistanceFilterTextField.getText();

            aInSpeedTestServers.setPredicate(speedTestServer -> {

                // If filter text is empty or filter matches the server, display it.
                String lLowerCaseFilter = newValue.toLowerCase();
                if ((serverNameFilterTextField.getText().isEmpty() ||
                     speedTestServer.getName().toLowerCase().contains(serverNameFilterTextField.getText().toLowerCase())) &&
                    (serverCountryFilterChoiceBox.getSelectionModel().getSelectedItem() == null ||
                     speedTestServer.getCountry().contains(serverCountryFilterChoiceBox.getSelectionModel().getSelectedItem())) &&
                    (serverCityFilterTextField.getText().isEmpty() ||
                     speedTestServer.getCity().toLowerCase().contains(serverCityFilterTextField.getText().toLowerCase())) &&
                    (serverDistanceFilterTextField.getText().isEmpty() ||
                     Double.valueOf(serverDistanceFilterTextField.getText()) <=  Double.valueOf(speedTestServer.getDistance()))) {
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


    // FXML

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
        // TODO add listeners on values and table and activate button only if changes
        SpeedTestServer lSpeedTestServer = serversTableView.getSelectionModel().getSelectedItem();
        if (lSpeedTestServer != null) {
            Preferences.getInstance().saveValue(Constants.SPEED_TEST_SERVER_NAME_PREFERENCE, lSpeedTestServer.getName());
            Preferences.getInstance().saveValue(Constants.SPEED_TEST_SERVER_URL_PREFERENCE, lSpeedTestServer.getUrl());
            Cat.getInstance().getController().reloadSpeedTestConfiguration();
        }
        dialogStage.close();
    }

    /**
     * Prepares the display
     */
    private void prepareDisplay() {

        // Retrieve current url
        String lCurrentUrl = Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_URL_PREFERENCE);

        if (serverNameFilter != null) serverNameFilterTextField.setText(serverNameFilter);
        if (serverCountryFilter != null) serverCountryFilterChoiceBox.getSelectionModel().select(serverCountryFilter);
        if (serverCityFilter != null) serverCityFilterTextField.setText(serverCityFilter);
        if (serverDistanceFilter != null) serverDistanceFilterTextField.setText(serverDistanceFilter);

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


    }

    // PUBLIC

    /**
     * Displays the dialog box
     */
    public void show() {
        prepareDisplay();
        dialogStage.showAndWait();
    }

    public void checkChanges() {
        if (serversTableView.getSelectionModel().getSelectedItem().getName().equals(Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_NAME_PREFERENCE))) {
            hasConfigurationChanged = false;
            saveButton.setDisable(true);
        } else {
            hasConfigurationChanged = true;
            saveButton.setDisable(false);
        }
    }

}
