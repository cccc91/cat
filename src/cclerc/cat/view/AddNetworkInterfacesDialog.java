package cclerc.cat.view;

import cclerc.cat.model.ConfiguredInterface;
import cclerc.services.Constants;
import cclerc.services.Display;
import cclerc.services.Network;
import cclerc.services.Preferences;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.NetworkInterface;
import java.util.HashMap;

public class AddNetworkInterfacesDialog {

    // Display management
    private Stage dialogStage;

    @FXML TableView<ConfiguredInterface> interfacesTable;
    @FXML TableColumn<ConfiguredInterface, String> nameColumn;
    @FXML TableColumn<ConfiguredInterface, String> displayedNameColumn;
    @FXML TableColumn<ConfiguredInterface, String> ipv4Column;
    @FXML TableColumn<ConfiguredInterface, String> ipv6Column;

    @FXML Button cancelButton;
    @FXML Button confirmButton;

    private volatile ObservableList<ConfiguredInterface> configuredInterfaces = FXCollections.observableArrayList();
    private ConfigurationDialog configurationDialogController;

    /**
     * Initializes interfaces table
     */
    public void initializeInterfacesTable() {
        interfacesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameColumn.setCellFactory(column -> stringFormatter());
        displayedNameColumn.setCellValueFactory(cellData -> cellData.getValue().displayedNameProperty());
        displayedNameColumn.setCellFactory(column -> stringFormatter());
        ipv4Column.setCellValueFactory(cellData -> cellData.getValue().displayedIpv4Property());
        ipv4Column.setCellFactory(column -> stringFormatter());
        ipv6Column.setCellValueFactory(cellData -> cellData.getValue().displayedIpv6Property());
        ipv6Column.setCellFactory(column -> stringFormatter());
        interfacesTable.setItems(configuredInterfaces);

        // Add double click management
        interfacesTable.setRowFactory( tv -> {
            TableRow<ConfiguredInterface> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) confirm();
            });
            return row ;
        });

    }

    // FORMATTERS

    /**
     * Formats strings for table view columns display
     *
     * @return Formatted string
     */
    private TableCell<ConfiguredInterface, String> stringFormatter() {
        return new TableCell<ConfiguredInterface, String>() {
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

    // SETTERS

    /**
     * Sets the stage of this dialog
     * @param aInDialogStage Stage
     */
    public void setDialogStage(Stage aInDialogStage) {
        dialogStage = aInDialogStage;
    }

    // PRIVATE METHODS

    private ListChangeListener<ConfiguredInterface> interfacesTableListener = (selection) -> {
        // Disable confirm button when no interface is selected or when the total number of interfaces (current + added) reaches the limit
        confirmButton.setDisable(
                selection.getList().size() == 0 ||
                configurationDialogController.getConfiguredInterfaces().size() + selection.getList().size() > Constants.MAXIMUM_NUMBER_OF_MONITORED_INTERFACES);
    };

    // METHODS

    /**
     * Displays the add network interfaces dialog box
     */
    public void show(ConfigurationDialog aInConfigurationDialogController) {

        // Fill the table with current configuration
        configuredInterfaces.clear();
        configurationDialogController = aInConfigurationDialogController;

        // Add interfaces that are not in potentially modified current configuration
        int lPriority = aInConfigurationDialogController.getConfiguredInterfaces().size();
        HashMap<String, NetworkInterface> lNetworkInterfaces = Network.buildNetworkInterfaceList();
        for (String lName: lNetworkInterfaces.keySet()) {
            boolean lInterfaceAlreadyAdded = false;
            for (ConfiguredInterface lConfiguredInterface : aInConfigurationDialogController.getConfiguredInterfaces()) {
                if (lConfiguredInterface.getName().equals(lName)) {
                    lInterfaceAlreadyAdded = true;
                    break;
                }
            }
            if (!lInterfaceAlreadyAdded) {
                configuredInterfaces.add(new ConfiguredInterface(lNetworkInterfaces.get(lName), ++lPriority));
            }
        }

        // Set button states
        confirmButton.setDisable(true);

        // Add listener on selection
        interfacesTable.getSelectionModel().getSelectedItems().removeListener(interfacesTableListener);
        interfacesTable.getSelectionModel().getSelectedItems().addListener(interfacesTableListener);

        // Tooltips
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) {
            Tooltip lTooltipInterfacesConfirm = new Tooltip(Display.getViewResourceBundle().getString("addNetworkInterfaces.tooltip.confirm"));
            Tooltip.install(confirmButton, lTooltipInterfacesConfirm);
            Tooltip lTooltipInterfacesCancel = new Tooltip(Display.getViewResourceBundle().getString("addNetworkInterfaces.tooltip.cancel"));
            Tooltip.install(cancelButton, lTooltipInterfacesCancel);
            Tooltip lTooltipInterfacesNetworkInterfaces =
                    new Tooltip(String.format(Display.getViewResourceBundle().getString("addNetworkInterfaces.tooltip.networkInterfaces"),
                                              Constants.MAXIMUM_NUMBER_OF_MONITORED_INTERFACES));
            Tooltip.install(interfacesTable, lTooltipInterfacesNetworkInterfaces);
        }

        // Display
        dialogStage.setResizable(false);
        dialogStage.showAndWait();

    }

    /**
     * Confirms the changes
     */
    @FXML private void confirm() {

        // Add the selected network interfaces to current monitoring jobs configuration
        configurationDialogController.addInterfaces(interfacesTable.getSelectionModel().getSelectedItems());

        dialogStage.close();

    }

    /**
     * Cancels the changes
     */
    @FXML public void cancel() {
        dialogStage.close();
    }

}
