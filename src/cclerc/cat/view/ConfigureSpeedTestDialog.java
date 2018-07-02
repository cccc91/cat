package cclerc.cat.view;

import cclerc.cat.Cat;
import cclerc.cat.model.SpeedTestServer;
import cclerc.services.*;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConfigureSpeedTestDialog {

    private static ConfigureSpeedTestDialog configureSpeedTestDialogInstance;

    private final int SPEED_TEST_TABLE_ROWS_PER_PAGE = 15;
    private SpeedTestServer selectedSpeedTestServer;

    private volatile ObservableList<SpeedTestServer> speedTestServers = FXCollections.observableArrayList();
    private volatile SortedList<SpeedTestServer> sortedSpeedTestServers;

    @FXML TableView<SpeedTestServer> speedTestServersTableView;
    @FXML TableColumn<SpeedTestServer, String> nameColumn;
    @FXML TableColumn<SpeedTestServer, String> countryColumn;
    @FXML TableColumn<SpeedTestServer, String> cityColumn;
    @FXML TableColumn<SpeedTestServer, Double> distanceColumn;
    @FXML Pagination speedTestServersPagination;
    @FXML Label speedTestServersCountLabel;
    @FXML TextField speedTestNameFilter;

    // Display management
    private static Stage dialogStage = new Stage();

    /**
     * Creates instance of ConfigureSpeedTestDialog controller
     * @param aInParentStage Parent stage of configure speed test dialog stage
     */
    public static ConfigureSpeedTestDialog getInstance(Stage aInParentStage) {

        FXMLLoader lDialogLoader = new FXMLLoader();

        try {

            // Load the fxml file and create a new stage for the popup dialog.
            lDialogLoader.setLocation(Cat.class.getResource("view/ConfigureSpeedTestDialog.fxml"));
            lDialogLoader.setResources(Display.getViewResourceBundle());
            VBox lDialogPane = lDialogLoader.load();

            // Create the dialog stage
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(aInParentStage);
            Scene lScene = new Scene(lDialogPane);
            lScene.getStylesheets().add("resources/css/view.css");
            dialogStage.setScene(lScene);
            dialogStage.getIcons().add(Constants.APPLICATION_IMAGE);
            dialogStage.setResizable(false);
            dialogStage.setTitle(Display.getViewResourceBundle().getString("configureSpeedTest.title"));
            configureSpeedTestDialogInstance = lDialogLoader.getController();
            configureSpeedTestDialogInstance.initializeSpeedTestServersTable();

        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.displayDialog"), Utilities.getStackTrace(e)));
        }

        return configureSpeedTestDialogInstance;

    }

     public void initializeSpeedTestServersTable() {

        // TODO
        for (int i = 0; i< 51; i++) {
            speedTestServers.add(new SpeedTestServer("test" + i, "FR", "Nozay", Double.valueOf(i), "www.toto" + i + ".com"));
        }

        // Configure table view
        speedTestServersTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameColumn.setCellFactory(column -> stringFormatter());
        countryColumn.setCellValueFactory(cellData -> cellData.getValue().countryProperty());
        countryColumn.setCellFactory(column -> stringFormatter());
        cityColumn.setCellValueFactory(cellData -> cellData.getValue().cityProperty());
        cityColumn.setCellFactory(column -> stringFormatter());
        distanceColumn.setCellValueFactory(cellData -> cellData.getValue().distanceProperty().asObject());
        distanceColumn.setCellFactory(column -> doubleFormatter());
        speedTestServersPagination.setPageFactory(this::createPage);
        //speedTestServersTableView.setItems(speedTestServers);

         // Wrap the ObservableList in a FilteredList (initially display all data).
         FilteredList<SpeedTestServer> lFilteredSpeedTestServers = new FilteredList<>(speedTestServers, p -> true);

         // Set the filter Predicate whenever the filter changes.
         speedTestNameFilter.textProperty().addListener(speedTestServersNameFilterListener(lFilteredSpeedTestServers));

         // Wrap the FilteredList in a SortedList.
         sortedSpeedTestServers = new SortedList<>(lFilteredSpeedTestServers);

         // Bind the SortedList comparator to the TableView comparator.
         sortedSpeedTestServers.comparatorProperty().bind(speedTestServersTableView.comparatorProperty());

         // Add sorted (and filtered) data to the table.
         speedTestServersTableView.setItems(sortedSpeedTestServers);
         speedTestServersCountLabel.setText(String.valueOf(sortedSpeedTestServers.size()));

         // Set the speed test server count and refresh pages whenever the sorted speed test servers list changes
         sortedSpeedTestServers.addListener(speedTestServersListChangeListener(sortedSpeedTestServers));

     }

    /**
     * Listener on changes on a sorted speed test servers list
     * @param aInSortedSpeedTestServers Sorted speed test servers list the listener is applied to
     * @return Listener
     */
    private ListChangeListener<SpeedTestServer> speedTestServersListChangeListener(SortedList<SpeedTestServer> aInSortedSpeedTestServers) {
        return new ListChangeListener<SpeedTestServer>(){
            @Override
            public void onChanged(Change<? extends SpeedTestServer> aInChanges) {
                Platform.runLater(() -> {
                    speedTestServersCountLabel.setText(String.valueOf(aInSortedSpeedTestServers.size()));
                    speedTestServersTableView.setItems(sortedSpeedTestServers);
                    // TODO: faire une fonction
                    speedTestServersPagination.setPageCount(sortedSpeedTestServers.size() / SPEED_TEST_TABLE_ROWS_PER_PAGE + ((sortedSpeedTestServers.size() % SPEED_TEST_TABLE_ROWS_PER_PAGE) != 0 ? 1 : 0));
                    speedTestServersPagination.setCurrentPageIndex(sortedSpeedTestServers.indexOf(selectedSpeedTestServer) / SPEED_TEST_TABLE_ROWS_PER_PAGE);
                    speedTestServersTableView.getSelectionModel().select(selectedSpeedTestServer);
                });
            }
        };
    }

    /**
     * Listener on changes on speed test servers filter name field
     * @param aInSpeedTestServers Filtered speed test servers list to which the change of filter applies
     * @return Listener
     */
    private ChangeListener<String> speedTestServersNameFilterListener(FilteredList<SpeedTestServer> aInSpeedTestServers) {

        return (observable, oldValue, newValue) -> {

            aInSpeedTestServers.setPredicate(speedTestServer -> {

                // If filter text is empty or filter matches the server, display it.
                String lLowerCaseFilter = newValue.toLowerCase();
                if (newValue == null || newValue.isEmpty() || speedTestServer.getName().toLowerCase().contains(lLowerCaseFilter)) {
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
     * @param aInPageIndex
     * @return
     */
    private Node createPage(int aInPageIndex) {

        speedTestServersPagination.setPageCount(sortedSpeedTestServers.size() / SPEED_TEST_TABLE_ROWS_PER_PAGE + ((sortedSpeedTestServers.size() % SPEED_TEST_TABLE_ROWS_PER_PAGE) != 0 ? 1 : 0));
        int fromIndex = aInPageIndex * SPEED_TEST_TABLE_ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + SPEED_TEST_TABLE_ROWS_PER_PAGE, sortedSpeedTestServers.size());
        speedTestServersTableView.setItems(FXCollections.observableArrayList(sortedSpeedTestServers.subList(fromIndex, toIndex)));

        return new VBox(speedTestServersTableView);

    }


    /**
     * Displays the dialog box
     */
    public void show() {

        // TODO: change
        GeoLocalization.getInstance().getLocalGeoLocalization();

        // Retrieve current url
        String lCurrentUrl = Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_URL_PREFERENCE);

        // TODO: change
        lCurrentUrl = "www.toto30.com";

        // Select it in the table view if it exists
        for (SpeedTestServer lSpeedTestServer: sortedSpeedTestServers) {
            if (lSpeedTestServer.getUrl().equals(lCurrentUrl)) {
                // Goto relevant page and select server
                selectedSpeedTestServer = lSpeedTestServer;
                speedTestServersPagination.setCurrentPageIndex(sortedSpeedTestServers.indexOf(selectedSpeedTestServer) / SPEED_TEST_TABLE_ROWS_PER_PAGE);
                speedTestServersTableView.getSelectionModel().select(selectedSpeedTestServer);
                break;
            }
        }

        dialogStage.showAndWait();
    }

    /**
     * Cancels changes if confirmed and closes dialog box
     */
    public void close() {
        // TODO check changes and confirm
        dialogStage.close();
    }

    /**
     * Saves configuration and exits dialog box
     */
    public void save() {
        SpeedTestServer lSpeedTestServer = speedTestServersTableView.getSelectionModel().getSelectedItem();
        dialogStage.close();
    }

}
