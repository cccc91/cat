package cclerc.cat.view;

import cclerc.cat.Cat;
import cclerc.cat.Configuration.*;
import cclerc.cat.model.Alarm;
import cclerc.cat.model.ConfiguredAlarm;
import cclerc.cat.model.ConfiguredInterface;
import cclerc.cat.model.ConfiguredSmtpServer;
import cclerc.services.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

public class ConfigurationDialog {

    private static ConfigurationDialog configurationDialogInstance;

    // Display management
    private static Stage dialogStage = new Stage();

    @FXML TabPane configurationTabPane;

    // Servers tab

    @FXML TabPane monitoringJobsTabPane;

    @FXML TableView<ConfiguredInterface> interfacesTable;
    @FXML TableColumn<ConfiguredInterface, Integer> interfacePriorityColumn;
    @FXML TableColumn<ConfiguredInterface, String> interfaceNameColumn;
    @FXML TableColumn<ConfiguredInterface, String> interfaceDisplayedNameColumn;
    @FXML TableColumn<ConfiguredInterface, String> interfaceIpv4Column;
    @FXML TableColumn<ConfiguredInterface, String> interfaceIpv6Column;
    @FXML CheckBox alertIfSecondaryIsDownCheckBox;
    @FXML Button addInterfaceButton;
    @FXML Button deleteInterfaceButton;
    @FXML Button upInterfaceButton;
    @FXML Button downInterfaceButton;

    @FXML CheckBox wanUseProxy;
    @FXML TextField wanPreferredHostname;
    @FXML TextField wanPreferredTimeoutTextField;
    @FXML TextField wanPreferredMaxRetriesTextField;
    @FXML TextField wanPreferredPollingPeriodTextField;
    @FXML TextField wanPreferredConnectionLostThresholdTextField;
    @FXML CheckBox wanPreferredIpv6CheckBox;
    @FXML TextField wanBackupHostname;
    @FXML TextField wanBackupTimeoutTextField;
    @FXML TextField wanBackupMaxRetriesTextField;
    @FXML TextField wanBackupPollingPeriodTextField;
    @FXML TextField wanBackupConnectionLostThresholdTextField;
    @FXML CheckBox wanBackupIpv6CheckBox;

    @FXML CheckBox lanUseProxy;
    @FXML TextField lanPreferredHostname;
    @FXML TextField lanPreferredTimeoutTextField;
    @FXML TextField lanPreferredMaxRetriesTextField;
    @FXML TextField lanPreferredPollingPeriodTextField;
    @FXML TextField lanPreferredConnectionLostThresholdTextField;
    @FXML CheckBox lanPreferredIpv6CheckBox;
    @FXML TextField lanBackupHostname;
    @FXML TextField lanBackupTimeoutTextField;
    @FXML TextField lanBackupMaxRetriesTextField;
    @FXML TextField lanBackupPollingPeriodTextField;
    @FXML TextField lanBackupConnectionLostThresholdTextField;
    @FXML CheckBox lanBackupIpv6CheckBox;

    @FXML TextField defaultTimeoutTextField;
    @FXML TextField defaultMaxRetriesTextField;
    @FXML TextField defaultPollingPeriodTextField;
    @FXML TextField defaultConnectionLostThresholdTextField;
    @FXML CheckBox defaultIpv6CheckBox;

    // Global tab
    @FXML TextField globalPollingPeriodTextField;
    @FXML TextField periodicSpeedTestPollingPeriodTextField;
    @FXML TextField periodicReportsPollingPeriodTextField;
    @FXML TextField globalConnectionsLostForgetTimeTextField;
    @FXML TextField globalMeanTimeBetweenTwoConnectionsLostThreshold1TextField;
    @FXML TextField globalMeanTimeBetweenTwoConnectionsLostThreshold2TextField;
    @FXML TextField globalMeanTimeBetweenTwoConnectionsLostThreshold3TextField;
    @FXML TextField globalMaxStoredPingDurationTextField;
    @FXML TextField globalMinDisplayedPingDurationTextField;
    @FXML TextField globalMaxDisplayedPingDurationTextField;
    @FXML TextField globalMaxStoredSpeedTestDurationTextField;
    @FXML TextField globalMinDisplayedSpeedTestDurationTextField;
    @FXML TextField globalMaxDisplayedSpeedTestDurationTextField;

    // Alarms tab

    @FXML TableView<ConfiguredAlarm> alarmsTable;
    @FXML TableColumn<ConfiguredAlarm, Integer> alarmIdColumn;
    @FXML TableColumn<ConfiguredAlarm, String> alarmNameColumn;
    @FXML TableColumn<ConfiguredAlarm, Boolean> alarmIsFilteredColumn;
    @FXML TableColumn<ConfiguredAlarm, String> alarmDefaultSeverityColumn;
    @FXML TableColumn<ConfiguredAlarm, String> alarmNewSeverityColumn;

    @FXML CheckBox audibleAlarmsCheckBox;
    @FXML Spinner<Integer> muteStartTimeHourSpinner;
    @FXML Spinner<Integer> muteStartTimeMinuteSpinner;
    @FXML Spinner<Integer> muteEndTimeHourSpinner;
    @FXML Spinner<Integer> muteEndTimeMinuteSpinner;
    @FXML TextField clearSoundFileTextField;
    @FXML TextField infoSoundFileTextField;
    @FXML TextField warningSoundFileTextField;
    @FXML TextField minorSoundFileTextField;
    @FXML TextField majorSoundFileTextField;
    @FXML TextField criticalSoundFileTextField;
    @FXML Button clearSoundFileButton;
    @FXML Button infoSoundFileButton;
    @FXML Button warningSoundFileButton;
    @FXML Button minorSoundFileButton;
    @FXML Button majorSoundFileButton;
    @FXML Button criticalSoundFileButton;

    // Emails tab
    @FXML TableView<ConfiguredSmtpServer> smtpServersTable;
    @FXML TableColumn<ConfiguredSmtpServer, String> smtpServerNameColumn;
    @FXML TableColumn<ConfiguredSmtpServer, String> smtpServerTlsModeColumn;
    @FXML TableColumn<ConfiguredSmtpServer, Integer> smtpServerPortColumn;
    @FXML TableColumn<ConfiguredSmtpServer, String> smtpServerUserColumn;
    @FXML TableColumn<ConfiguredSmtpServer, String> smtpServerLoginColumn;
    @FXML TableColumn<ConfiguredSmtpServer, String> smtpServerPasswordColumn;
    @FXML TableColumn<ConfiguredSmtpServer, Integer> smtpServerConnectionTimeoutColumn;
    @FXML TableColumn<ConfiguredSmtpServer, Integer> smtpServerTimeoutColumn;
    @FXML Button addSmtpServerButton;
    @FXML Button deleteSmtpServerButton;
    @FXML Button editSmtpServerButton;
    @FXML Button preferredSmtpServerButton;
    @FXML Button upSmtpServerButton;
    @FXML Button downSmtpServerButton;

    @FXML TextField recipientListTextField;

    // Common

    @FXML Button cancelButton;
    @FXML Button saveButton;
    @FXML Button closeButton;

    private Map<AbstractConfiguration, AbstractConfiguration> initialConfigurations = new HashMap<>();

    private Map<AbstractConfiguration, Map<String, TextField>> textFields = new HashMap<>();
    private Map<AbstractConfiguration, Map<String, CheckBox>> checkBoxes = new HashMap<>();
    private Map<Spinner, Spinner> spinners = new HashMap<>();
    private List<Object> invalidConfigurationData = new ArrayList<>();

    // Listeners
    private Map<TextField, ChangeListener<? super String>> textFieldListeners = new HashMap<>();
    private Map<CheckBox, ChangeListener<? super Boolean>> checkBoxListeners = new HashMap<>();

    private boolean saveState;
    private volatile ObservableList<ConfiguredInterface> configuredInterfaces = FXCollections.observableArrayList();
    private volatile ObservableList<ConfiguredAlarm> configuredAlarms = FXCollections.observableArrayList();
    private volatile ObservableList<ConfiguredSmtpServer> configuredSmtpServers = FXCollections.observableArrayList();
    private static AddNetworkInterfacesDialog addNetworkInterfacesDialogController;
    private static AddEditSmtpServersDialog addEditSmtpServerDialogController;

    /**
     * Creates instance of ConfigurationDialog controller
     * @param aInParentStage Parent stage of configuration dialog stage
     */
    public static ConfigurationDialog getInstance(Stage aInParentStage) {

        FXMLLoader lDialogLoader = new FXMLLoader();

        try {

            // Load the fxml file and create a new stage for the popup dialog.
            lDialogLoader.setLocation(Cat.class.getResource("view/ConfigurationDialog.fxml"));
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
            dialogStage.setTitle(Display.getViewResourceBundle().getString("configuration.title"));
            configurationDialogInstance = lDialogLoader.getController();
            dialogStage.setOnCloseRequest(event -> {
                configurationDialogInstance.close();
                event.consume();
            });
            configurationDialogInstance.initialize();
        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.displayDialog"), Utilities.getStackTrace(e)));
        }

        return configurationDialogInstance;

    }

    private void initialize() {

        // NETWORK INTERFACES CREATION DIALOG
        addNetworkInterfacesDialogController = AddNetworkInterfacesDialog.getInstance(dialogStage);

        // SMTP SERVER CREATION AND EDITION DIALOG
        addEditSmtpServerDialogController = new AddEditSmtpServersDialog().getInstance(dialogStage);

        initializeTabs();
        initializeInitialConfiguration();
        initializeSimpleFields();
        addSimpleFieldsTooltips();

        // Add listener on tab selection
        configurationTabPane.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTab, newTab) ->
                        States.getInstance().saveValue("configuration.selectedTab", newTab.getTabPane().getSelectionModel().getSelectedIndex()));
        monitoringJobsTabPane.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTab, newTab) ->
                        States.getInstance().saveValue("configuration.monitoringJobs.selectedTab", newTab.getTabPane().getSelectionModel().getSelectedIndex()));

        // Change buttons states on selection in tables
        interfacesTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<ConfiguredInterface>) selection -> setInterfacesButtonsStates());
        smtpServersTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<ConfiguredSmtpServer>) selection -> setSmtpServersButtonsStates());

        // Add listener on file selection buttons
        clearSoundFileButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                new FileSelector.FileSelectorBuilder(dialogStage, FileSelector.FileSelectorType.CONFIGURATION)
                        .setFileIdentification("clearAlarmSoundFile")
                        .setFilenameTextField(clearSoundFileTextField)
                        .addPattern("Audio files", Arrays.asList("*.mp3", "*.wav"))
                        .setDefaultPattern("*.mp3")
                        .build());
        infoSoundFileButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                new FileSelector.FileSelectorBuilder(dialogStage, FileSelector.FileSelectorType.CONFIGURATION)
                        .setFileIdentification("infoAlarmSoundFile")
                        .setFilenameTextField(infoSoundFileTextField)
                        .addPattern("Audio files", Arrays.asList("*.mp3", "*.wav"))
                        .setDefaultPattern("*.mp3")
                        .build());
        warningSoundFileButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                new FileSelector.FileSelectorBuilder(dialogStage, FileSelector.FileSelectorType.CONFIGURATION)
                        .setFileIdentification("warningAlarmSoundFile")
                        .setFilenameTextField(warningSoundFileTextField)
                        .addPattern("Audio files", Arrays.asList("*.mp3", "*.wav"))
                        .setDefaultPattern("*.mp3")
                        .build());
        minorSoundFileButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                new FileSelector.FileSelectorBuilder(dialogStage, FileSelector.FileSelectorType.CONFIGURATION)
                        .setFileIdentification("minorAlarmSoundFile")
                        .setFilenameTextField(minorSoundFileTextField)
                        .addPattern("Audio files", Arrays.asList("*.mp3", "*.wav"))
                        .setDefaultPattern("*.mp3")
                        .build());
        majorSoundFileButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                new FileSelector.FileSelectorBuilder(dialogStage, FileSelector.FileSelectorType.CONFIGURATION)
                        .setFileIdentification("majorAlarmSoundFile")
                        .setFilenameTextField(majorSoundFileTextField)
                        .addPattern("Audio files", Arrays.asList("*.mp3", "*.wav"))
                        .setDefaultPattern("*.mp3")
                        .build());
        criticalSoundFileButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                new FileSelector.FileSelectorBuilder(dialogStage, FileSelector.FileSelectorType.CONFIGURATION)
                        .setFileIdentification("criticalAlarmSoundFile")
                        .setFilenameTextField(criticalSoundFileTextField)
                        .addPattern("Audio files", Arrays.asList("*.mp3", "*.wav"))
                        .setDefaultPattern("*.mp3")
                        .build());

        // Set tooltips
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) {

            // Buttons tooltips

            Tooltip lTooltipInterfacesSave = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.networkInterfaceConfiguration.tooltip.save"));
            Tooltip.install(saveButton, lTooltipInterfacesSave);
            Tooltip lTooltipInterfacesCancel = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.networkInterfaceConfiguration.tooltip.cancel"));
            Tooltip.install(cancelButton, lTooltipInterfacesCancel);
            Tooltip lTooltipInterfacesClose;
            if (Preferences.getInstance().getBooleanValue("autoSaveConfiguration")) {
                lTooltipInterfacesClose = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.networkInterfaceConfiguration.tooltip.saveAndClose"));
            } else {
                lTooltipInterfacesClose = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.networkInterfaceConfiguration.tooltip.close"));
            }
            Tooltip.install(closeButton, lTooltipInterfacesClose);

            // Complex fields tooltips

            Tooltip lTooltipInterfacesConfiguredInterfaces =
                    new Tooltip(String.format(Display.getViewResourceBundle().getString("configuration.monitoringJobs.networkInterfaceConfiguration.tooltip.configuredInterfaces"),
                                              Constants.MAXIMUM_NUMBER_OF_MONITORED_INTERFACES));
            Tooltip.install(interfacesTable, lTooltipInterfacesConfiguredInterfaces);
            Tooltip lTooltipInterfacesAdd = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.networkInterfaceConfiguration.tooltip.add"));
            Tooltip.install(addInterfaceButton, lTooltipInterfacesAdd);
            Tooltip lTooltipInterfacesDelete = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.networkInterfaceConfiguration.tooltip.remove"));
            Tooltip.install(deleteInterfaceButton, lTooltipInterfacesDelete);
            Tooltip lTooltipInterfacesUp = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.networkInterfaceConfiguration.tooltip.up"));
            Tooltip.install(upInterfaceButton, lTooltipInterfacesUp);
            Tooltip lTooltipInterfacesDown = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.networkInterfaceConfiguration.tooltip.down"));
            Tooltip.install(downInterfaceButton, lTooltipInterfacesDown);

            Tooltip lTooltipAlarms = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.audibleAlarms.tooltip.configuredAlarms"));
            Tooltip.install(alarmsTable, lTooltipAlarms);
            Tooltip lToolMuteStartTime = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.audibleAlarms.tooltip.muteStartTime"));
            Tooltip.install(muteStartTimeHourSpinner.getEditor(), lToolMuteStartTime);
            Tooltip.install(muteStartTimeMinuteSpinner.getEditor(), lToolMuteStartTime);
            Tooltip lToolMuteEndTime = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.audibleAlarms.tooltip.muteEndTime"));
            Tooltip.install(muteEndTimeHourSpinner.getEditor(), lToolMuteEndTime);
            Tooltip.install(muteEndTimeMinuteSpinner.getEditor(), lToolMuteEndTime);

            Tooltip lTooltipEmailsConfiguredSmtpServers =
                    new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.email.tooltip.configuredSmtpServers"));
            Tooltip.install(smtpServersTable, lTooltipEmailsConfiguredSmtpServers);
            Tooltip lTooltipEmailsConfiguredSmtpServersAdd = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.email.tooltip.add"));
            Tooltip.install(addSmtpServerButton, lTooltipEmailsConfiguredSmtpServersAdd);
            Tooltip lTooltipEmailsConfiguredSmtpServersDelete = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.email.tooltip.remove"));
            Tooltip.install(deleteSmtpServerButton, lTooltipEmailsConfiguredSmtpServersDelete);
            Tooltip lTooltipEmailsConfiguredSmtpServersEdit = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.email.tooltip.edit"));
            Tooltip.install(editSmtpServerButton, lTooltipEmailsConfiguredSmtpServersEdit);
            Tooltip lTooltipEmailsConfiguredSmtpServersPreferred = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.email.tooltip.preferred"));
            Tooltip.install(preferredSmtpServerButton, lTooltipEmailsConfiguredSmtpServersPreferred);
            Tooltip lTooltipEmailsConfiguredSmtpServersUp = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.email.tooltip.up"));
            Tooltip.install(upSmtpServerButton, lTooltipEmailsConfiguredSmtpServersUp);
            Tooltip lTooltipEmailsConfiguredSmtpServersDown = new Tooltip(Display.getViewResourceBundle().getString("configuration.monitoringJobs.email.tooltip.down"));
            Tooltip.install(downSmtpServerButton, lTooltipEmailsConfiguredSmtpServersDown);

        }

    }

    /**
     * Initializes the different tabs
     */
    private void initializeTabs() {
        initializeInterfacesTab();
        initializeAlarmsTab();
        initializeEmailsTab();
    }

    /**
     * Initializes interfaces tab
     */
    private void initializeInterfacesTab() {

        interfacesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        interfacePriorityColumn.setCellValueFactory(cellData -> cellData.getValue().priorityProperty().asObject());
        interfacePriorityColumn.setCellFactory(column -> cellIntegerFormatter());
        interfaceNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        interfaceNameColumn.setCellFactory(column -> cellStringFormatter());
        interfaceDisplayedNameColumn.setCellValueFactory(cellData -> cellData.getValue().displayedNameProperty());
        interfaceDisplayedNameColumn.setCellFactory(column -> cellStringFormatter());
        interfaceIpv4Column.setCellValueFactory(cellData -> cellData.getValue().displayedIpv4Property());
        interfaceIpv4Column.setCellFactory(column -> cellStringFormatter());
        interfaceIpv6Column.setCellValueFactory(cellData -> cellData.getValue().displayedIpv6Property());
        interfaceIpv6Column.setCellFactory(column -> cellStringFormatter());

        interfacesTable.setItems(configuredInterfaces);

    }

    /**
     * Initializes alarms tab
     */
    private void initializeAlarmsTab() {

        // Initialize alarms table

        alarmsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        alarmIdColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        alarmIdColumn.setCellFactory(column -> cellIntegerFormatter());
        alarmNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        alarmNameColumn.setCellFactory(column -> cellStringFormatter());

        alarmIsFilteredColumn.setCellValueFactory( new PropertyValueFactory<>("isFiltered" ));
        alarmIsFilteredColumn.setCellFactory(CheckBoxTableCell.forTableColumn(param -> {
            changeAlarmConfiguration(param);
            return configuredAlarms.get(param).isFilteredProperty();
        }));

        alarmDefaultSeverityColumn.setCellValueFactory(cellData -> cellData.getValue().defaultSeverityProperty());
        alarmDefaultSeverityColumn.setCellFactory(column -> cellEnumFormatter("catView.alarmView.severity."));

        alarmNewSeverityColumn.setCellValueFactory(cellData -> cellData.getValue().newDisplayedSeverityProperty());
        ObservableList<String> lSeverityValues = FXCollections.observableArrayList(
                EnumTypes.AlarmSeverity.INFO.getDisplayedValue(),
                EnumTypes.AlarmSeverity.WARNING.getDisplayedValue(),
                EnumTypes.AlarmSeverity.MINOR.getDisplayedValue(),
                EnumTypes.AlarmSeverity.MAJOR.getDisplayedValue(),
                EnumTypes.AlarmSeverity.CRITICAL.getDisplayedValue());
        alarmNewSeverityColumn.setCellFactory((column) -> new ComboBoxTableCell<>(lSeverityValues));
        alarmNewSeverityColumn.setOnEditCommit(
                (TableColumn.CellEditEvent<ConfiguredAlarm, String> t) -> {

                    // Find severity corresponding to the displayed severity
                    EnumTypes.AlarmSeverity lNewSeverity = null;
                    for (EnumTypes.AlarmSeverity lSeverity: EnumTypes.AlarmSeverity.values()) {
                        if (lSeverity.getDisplayedValue().equals(t.getNewValue())) {
                            lNewSeverity = lSeverity;
                            break;
                        }
                    }
                    (t.getTableView().getItems().get(t.getTablePosition().getRow())).setNewSeverity(lNewSeverity);
                    changeAlarmConfiguration(t.getTablePosition().getRow());
                });
        alarmsTable.setItems(configuredAlarms);

        // Initialize spinners

        if (!spinners.containsKey(muteStartTimeHourSpinner)) spinners.put(muteStartTimeHourSpinner, muteStartTimeMinuteSpinner);
        if (!spinners.containsKey(muteStartTimeMinuteSpinner)) spinners.put(muteStartTimeMinuteSpinner, muteStartTimeHourSpinner);
        if (!spinners.containsKey(muteEndTimeHourSpinner)) spinners.put(muteEndTimeHourSpinner, muteEndTimeMinuteSpinner);
        if (!spinners.containsKey(muteEndTimeMinuteSpinner)) spinners.put(muteEndTimeMinuteSpinner, muteEndTimeHourSpinner);

        AudibleAlarmsConfiguration lAudibleAlarmsConfiguration = Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration();

        SpinnerValueFactory<Integer> lMuteStartTimeHourSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23);
        SpinnerValueFactory<Integer> lMuteStartTimeMinuteSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59);
        SpinnerValueFactory<Integer> lMuteEndTimeHourSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23);
        SpinnerValueFactory<Integer> lMuteEndTimeMinuteSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59);

        lMuteStartTimeHourSpinnerFactory.setValue(
                (lAudibleAlarmsConfiguration != null && lAudibleAlarmsConfiguration.getMuteStartTime() != null) ?
                lAudibleAlarmsConfiguration.getMuteStartHour() : AudibleAlarmsConfiguration.getDefaultMuteStartHour());
        lMuteStartTimeMinuteSpinnerFactory.setValue(
                (lAudibleAlarmsConfiguration != null && lAudibleAlarmsConfiguration.getMuteStartTime() != null) ?
                lAudibleAlarmsConfiguration.getMuteStartMinute() : AudibleAlarmsConfiguration.getDefaultMuteStartMinute());
        lMuteEndTimeHourSpinnerFactory.setValue(
                (lAudibleAlarmsConfiguration != null && lAudibleAlarmsConfiguration.getMuteEndTime() != null) ?
                lAudibleAlarmsConfiguration.getMuteEndHour() : AudibleAlarmsConfiguration.getDefaultMuteEndHour());
        lMuteEndTimeMinuteSpinnerFactory.setValue(
                (lAudibleAlarmsConfiguration != null && lAudibleAlarmsConfiguration.getMuteEndTime() != null) ?
                lAudibleAlarmsConfiguration.getMuteEndMinute() : AudibleAlarmsConfiguration.getDefaultMuteEndMinute());

        lMuteStartTimeHourSpinnerFactory.setWrapAround(true);
        lMuteStartTimeMinuteSpinnerFactory.setWrapAround(true);
        lMuteEndTimeHourSpinnerFactory.setWrapAround(true);
        lMuteEndTimeMinuteSpinnerFactory.setWrapAround(true);

        muteStartTimeHourSpinner.setValueFactory(lMuteStartTimeHourSpinnerFactory);
        muteStartTimeMinuteSpinner.setValueFactory(lMuteStartTimeMinuteSpinnerFactory);
        muteEndTimeHourSpinner.setValueFactory(lMuteEndTimeHourSpinnerFactory);
        muteEndTimeMinuteSpinner.setValueFactory(lMuteEndTimeMinuteSpinnerFactory);

        muteStartTimeHourSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL);
        muteStartTimeMinuteSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL);
        muteEndTimeHourSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL);
        muteEndTimeMinuteSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL);

        muteStartTimeHourSpinner.getEditor().textProperty().addListener(
                (obs, oldValue, newValue) -> checkTime(true, muteStartTimeHourSpinner, oldValue, newValue, "MuteStartTime"));
        muteStartTimeHourSpinner.valueProperty().addListener(
                (obs, oldValue, newValue) -> checkTime(true, muteStartTimeHourSpinner, oldValue.toString(), newValue.toString(), "MuteStartTime"));
        muteStartTimeMinuteSpinner.getEditor().textProperty().addListener(
                (obs, oldValue, newValue) -> checkTime(false, muteStartTimeMinuteSpinner, oldValue, newValue, "MuteStartTime"));
        muteStartTimeMinuteSpinner.valueProperty().addListener(
                (obs, oldValue, newValue) -> checkTime(false, muteStartTimeMinuteSpinner, oldValue.toString(), newValue.toString(), "MuteStartTime"));
        muteEndTimeHourSpinner.getEditor().textProperty().addListener(
                (obs, oldValue, newValue) -> checkTime(true, muteEndTimeHourSpinner, oldValue, newValue, "MuteEndTime"));
        muteEndTimeHourSpinner.valueProperty().addListener(
                (obs, oldValue, newValue) -> checkTime(true, muteEndTimeHourSpinner, oldValue.toString(), newValue.toString(), "MuteEndTime"));
        muteEndTimeMinuteSpinner.getEditor().textProperty().addListener(
                (obs, oldValue, newValue) -> checkTime(false, muteEndTimeMinuteSpinner, oldValue, newValue, "MuteEndTime"));
        muteEndTimeMinuteSpinner.valueProperty().addListener(
                (obs, oldValue, newValue) -> checkTime(false, muteEndTimeMinuteSpinner, oldValue.toString(), newValue.toString(), "MuteEndTime"));

    }

    /**
     * Initializes interfaces tab
     */
    private void initializeEmailsTab() {

        smtpServersTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        smtpServerNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        smtpServerNameColumn.setCellFactory(column -> cellStringFormatter());
        smtpServerTlsModeColumn.setCellValueFactory(cellData -> cellData.getValue().tlsModeProperty());
        smtpServerTlsModeColumn.setCellFactory(column -> cellStringFormatter());
        smtpServerPortColumn.setCellValueFactory(cellData -> cellData.getValue().portProperty().asObject());
        smtpServerPortColumn.setCellFactory(column -> cellIntegerFormatter());
        smtpServerUserColumn.setCellValueFactory(cellData -> cellData.getValue().userProperty());
        smtpServerUserColumn.setCellFactory(column -> cellStringFormatter());
        smtpServerLoginColumn.setCellValueFactory(cellData -> cellData.getValue().loginProperty());
        smtpServerLoginColumn.setCellFactory(column -> cellStringFormatter());
        smtpServerPasswordColumn.setCellValueFactory(cellData -> cellData.getValue().passwordProperty());
        smtpServerPasswordColumn.setCellFactory(column -> cellPasswordFormatter());
        smtpServerConnectionTimeoutColumn.setCellValueFactory(cellData -> cellData.getValue().connectionTimeoutProperty().asObject());
        smtpServerConnectionTimeoutColumn.setCellFactory(column -> cellIntegerFormatter());
        smtpServerTimeoutColumn.setCellValueFactory(cellData -> cellData.getValue().timeoutProperty().asObject());
        smtpServerTimeoutColumn.setCellFactory(column -> cellIntegerFormatter());

        final PseudoClass lDefaultSmptServerPseudoClass = PseudoClass.getPseudoClass("preferredSmtpServer");
        smtpServersTable.setRowFactory(new Callback<TableView<ConfiguredSmtpServer>, TableRow<ConfiguredSmtpServer>>() {

            @Override
            public TableRow<ConfiguredSmtpServer> call(TableView<ConfiguredSmtpServer> lTableView) {

                final TableRow<ConfiguredSmtpServer> lRow = new TableRow<ConfiguredSmtpServer>() {
                    @Override
                    protected void updateItem(ConfiguredSmtpServer aInConfiguredSmtpServer, boolean aInEmpty) {
                        super.updateItem(aInConfiguredSmtpServer, aInEmpty);
                        // Change style of default SMTP server using .table-row-cell:preferredSmtpServer pseudo class
                        pseudoClassStateChanged(lDefaultSmptServerPseudoClass, (!aInEmpty && (aInConfiguredSmtpServer.getUser() + "@" + aInConfiguredSmtpServer.getName()).equals(
                                Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer())));
                    }
                };

                // Double click event
                lRow.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && (! lRow.isEmpty()) ) {
                        addEditSmtpServer(smtpServersTable.getSelectionModel().getSelectedItem());
                    }
                });

                return lRow;

            }
        });


        smtpServersTable.setItems(configuredSmtpServers);

    }

    /**
     * Checks time (hour or minute) in spinners is valid and changes style depending on the value
     * @param aInIsHour        true if the time to check is an hour, false if it is a minute
     * @param aInSpinner       Spinner that has called the method
     * @param aInOldValue      Old time value
     * @param aInNewValue      New time value
     * @param aInAttributeName Name of the attribute to update in configuration
     */
    private void checkTime(boolean aInIsHour, Spinner aInSpinner, String aInOldValue, String aInNewValue, String aInAttributeName) {

        AudibleAlarmsConfiguration lCurrentConfiguration = Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration();
        AudibleAlarmsConfiguration lInitialConfiguration = Configuration.getInitialConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration();

        try {

            removeInvalidConfigurationData(aInSpinner);

            // Retrieve values
            Integer lDefaultValue = Integer.valueOf(AbstractConfiguration.findDefaultValue(lCurrentConfiguration, aInAttributeName).replaceAll((aInIsHour) ? ":.*" : ".*:", ""));
            Integer lNewValue = Integer.valueOf(aInNewValue);

            // Retrieve set/get methods from configuration class and method name
            Method lSetMethod = lCurrentConfiguration.getClass().getDeclaredMethod("set" + aInAttributeName, String.class);
            Method lGetMethod = lInitialConfiguration.getClass().getDeclaredMethod("get" + aInAttributeName);

            // Invoke set method
            String lNewTimeValue = (aInIsHour) ?
                                   String.format("%02d", lNewValue) + ':' + String.format("%2s", spinners.get(aInSpinner).getValue()).replace(' ', '0') :
                                   String.format("%2s", spinners.get(aInSpinner).getValue()).replace(' ', '0') + ':' + String.format("%02d", lNewValue);
            lSetMethod.invoke(lCurrentConfiguration, lNewTimeValue);
            String lInitialValue = lGetMethod.invoke(lInitialConfiguration).toString();

            if (lNewValue > ((aInIsHour) ? 23 : 59) || lNewValue < 0) {
                aInSpinner.setId("bad-value-spinner");
                addInvalidConfigurationData(aInSpinner);
            } else if (!lNewTimeValue.equals(lInitialValue)) {
                if (!lNewValue.equals(lDefaultValue)) {
                    aInSpinner.setId("new-value-spinner");
                } else {
                    aInSpinner.setId("default-new-value-spinner");
                }
            } else {
                if (!lNewValue.equals(lDefaultValue)) {
                    aInSpinner.setId("");
                } else {
                    aInSpinner.setId("default-value-spinner");
                }
            }

        } catch (NumberFormatException e) {
            aInSpinner.getEditor().setText(aInOldValue);
            //Platform.runLater(() -> aInSpinner.getEditor().setText(aInOldValue));
        } catch (InvocationTargetException e) {
            // If the set method has raised an error (bad type for instance), change color and disable save
            aInSpinner.setId("bad-value-spinner");
            addInvalidConfigurationData(aInSpinner);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            Display.logUnexpectedError(e);
        }
    }

    /**
     * Takes into account alarms selected line changes into the configuration
     * @param aInRowIndex Alarms table modified row
     */
    private void changeAlarmConfiguration(Integer aInRowIndex) {

        boolean lInConfiguration = false;
        for (AlarmConfiguration lAlarmConfiguration: Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations()) {

            if (lAlarmConfiguration.getId() == configuredAlarms.get(aInRowIndex).getId()) {
                // Alarm is in the alarms configuration
                lInConfiguration = true;
                if (configuredAlarms.get(aInRowIndex).getDefaultSeverity().equals(configuredAlarms.get(aInRowIndex).getNewSeverity())) {
                    // Case severity has not been changed => alarm must be in the configuration only if it is filtered (otherwise it is the default value, no need add it)
                    if (configuredAlarms.get(aInRowIndex).getIsFiltered()) {
                        // Alarm is filtered => only is filtered must be kept in the configuration
                        Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations().remove(lAlarmConfiguration);
                        Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations().add(
                                new AlarmConfiguration(
                                        Configuration.getCurrentConfiguration(), configuredAlarms.get(aInRowIndex).getId(), configuredAlarms.get(aInRowIndex).getIsFiltered()));
                    } else {
                        // Alarm is not filtered, default case
                        Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations().remove(lAlarmConfiguration);
                    }
                } else {
                    // Case severity has been changed
                    if (configuredAlarms.get(aInRowIndex).getIsFiltered()) {
                        // Alarm is filtered => change the value
                        lAlarmConfiguration.setIsFiltered("true");
                        lAlarmConfiguration.setNewSeverity(configuredAlarms.get(aInRowIndex).getNewSeverity().toString());
                    } else {
                        // Alarm is not filtered => only new severity must be kept in the configuration
                        Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations().remove(lAlarmConfiguration);
                        Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations().add(
                                new AlarmConfiguration(
                                        Configuration.getCurrentConfiguration(), configuredAlarms.get(aInRowIndex).getId(), configuredAlarms.get(aInRowIndex).getNewSeverity()));
                    }
                }

                break;

            }

        }

        // Case alarm is not in the configuration
        if (!lInConfiguration) {
            if (configuredAlarms.get(aInRowIndex).getIsFiltered()) {
                if (configuredAlarms.get(aInRowIndex).getDefaultSeverity().equals(configuredAlarms.get(aInRowIndex).getNewSeverity())) {
                    Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations().add(
                            new AlarmConfiguration(Configuration.getCurrentConfiguration(), configuredAlarms.get(aInRowIndex).getId(), true));
                } else {
                    Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations().add(
                            new AlarmConfiguration(
                                    Configuration.getCurrentConfiguration(), configuredAlarms.get(aInRowIndex).getId(), true, configuredAlarms.get(aInRowIndex).getNewSeverity()));
                }
            } else {
                if (!configuredAlarms.get(aInRowIndex).getDefaultSeverity().equals(configuredAlarms.get(aInRowIndex).getNewSeverity())) {
                    Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations().add(
                            new AlarmConfiguration(
                                    Configuration.getCurrentConfiguration(), configuredAlarms.get(aInRowIndex).getId(), configuredAlarms.get(aInRowIndex).getNewSeverity()));
                }
            }
        }

        // Sort alarms by id
        Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations().sort(Comparator.comparing(AlarmConfiguration::getId));

        // Check changes
        validateConfiguration("Alarms", Configuration.getCurrentConfiguration().getAlarmsConfiguration(), alarmsTable);
        checkConfigurationChanges();

    }

    // FORMATTERS

    /**
     * Formats integers for table view columns display
     * @return Formatted integer
     */
    private <T> TableCell<T, Integer> cellIntegerFormatter() {
        return new TableCell<T, Integer>() {
            @Override
            protected void updateItem(Integer aInItem, boolean aInEmpty) {
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
     * Formats enums for table display using locale resource
     * @param aInKeyPrefix Key prefix in resource to display enum value. End of key must be equal to the enum value
     * @return Formatted enum
     */
    private <T> TableCell<T, String> cellEnumFormatter(String aInKeyPrefix) {
        return new TableCell<T, String>() {
            @Override
            protected void updateItem(String aInItem, boolean aInEmpty) {
                super.updateItem(aInItem, aInEmpty);
                if (aInItem != null && !aInEmpty) {
                    String lKey = aInKeyPrefix + aInItem;
                    String lText = (Display.getViewResourceBundle().containsKey(lKey)) ? Display.getViewResourceBundle().getString(lKey) :
                                   Display.getViewResourceBundle().getString(aInKeyPrefix + "unknown");
                    setText(lText);
                } else {
                    setText("");
                }
            }
        };
    }

    /**
     * Formats strings for table view columns display
     * @return Formatted string
     */
    private <T> TableCell<T, String> cellStringFormatter() {
        return new TableCell<T, String>() {
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
     * Formats passwords for table view columns display
     * @return Formatted string
     */
    private <T> TableCell<T, String> cellPasswordFormatter() {
        return new TableCell<T, String>() {
            @Override
            protected void updateItem(String aInItem, boolean aInEmpty) {
                super.updateItem(aInItem, aInEmpty);
                if (aInItem != null && !aInEmpty) {
                    setText("********");
                } else {
                    setText("");
                }
            }
        };
    }

    // GETTERS

    /**
     * Gets interfaces list
     * @return interfaces list
     */
    public ObservableList<ConfiguredInterface> getConfiguredInterfaces() {
        return configuredInterfaces;
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

    /**
     * Initializes the mapping between current and initial configuration
     */
    private void initializeInitialConfiguration() {

        initialConfigurations.put(
                Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration(),
                Configuration.getInitialConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration());
        initialConfigurations.put(
                Configuration.getCurrentConfiguration().getMonitoringConfiguration().getMonitoringDefaultsConfiguration(),
                Configuration.getInitialConfiguration().getMonitoringConfiguration().getMonitoringDefaultsConfiguration());
        if (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan() != null &&
            Configuration.getInitialConfiguration().getMonitoringConfiguration().getWan() != null) {
            initialConfigurations.put(
                    Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan().getPreferredServer(),
                    Configuration.getInitialConfiguration().getMonitoringConfiguration().getWan().getPreferredServer());
            initialConfigurations.put(
                    Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan().getBackupServer(),
                    Configuration.getInitialConfiguration().getMonitoringConfiguration().getWan().getBackupServer());
        }
        if (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan() != null &&
            Configuration.getInitialConfiguration().getMonitoringConfiguration().getLan() != null) {
            initialConfigurations.put(
                    Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan().getPreferredServer(),
                    Configuration.getInitialConfiguration().getMonitoringConfiguration().getLan().getPreferredServer());
            initialConfigurations.put(
                    Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan().getBackupServer(),
                    Configuration.getInitialConfiguration().getMonitoringConfiguration().getLan().getBackupServer());
        }
        initialConfigurations.put(
                Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration(),
                Configuration.getInitialConfiguration().getGlobalMonitoringConfiguration());
        initialConfigurations.put(
                Configuration.getCurrentConfiguration().getAlarmsConfiguration(),
                Configuration.getInitialConfiguration().getAlarmsConfiguration());
        initialConfigurations.put(
                Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration(),
                Configuration.getInitialConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration());
        initialConfigurations.put(
                Configuration.getCurrentConfiguration().getEmailConfiguration(),
                Configuration.getInitialConfiguration().getEmailConfiguration());

    }

    /**
     * Initialize simple fields associated to their configuration class and to the set/get method name prefix
     */
    private void initializeSimpleFields() {
        initializeTextFields();
        initializeCheckBoxes();
    }

    /**
     * Initialize text fields associated to their configuration class and to the set/get method name prefix
     */
    private void initializeTextFields() {

        // If no wan is defined in the configuration, add the default wan (won't be saved if undefined)
        if (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan() == null) {
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().addWan(Configuration.getCurrentConfiguration());
        }

        // If no backup server is defined (preferred is always defined) in the wan configuration, add the default backup server (won't be saved if undefined)
        if (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan().getBackupServer() == null) {
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan().addBackupServer(Configuration.getCurrentConfiguration());
        }

        // If no lan is defined in the configuration, add the default lan (won't be saved if undefined)
        if (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan() == null) {
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().addLan(Configuration.getCurrentConfiguration());
        }

        // If no backup server is defined (preferred is always defined) in the lan configuration, add the default backup server (won't be saved if undefined)
        if (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan().getBackupServer() == null) {
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan().addBackupServer(Configuration.getCurrentConfiguration());
        }

        textFields.keySet().removeAll(textFields.keySet());

        Map<String, TextField> lDefaultConfigurationTextFields = new HashMap<>();  // Default configuration text fields
        lDefaultConfigurationTextFields.put("Timeout", defaultTimeoutTextField);
        lDefaultConfigurationTextFields.put("MaxRetries", defaultMaxRetriesTextField);
        lDefaultConfigurationTextFields.put("PollingPeriod", defaultPollingPeriodTextField);
        lDefaultConfigurationTextFields.put("ConnectionLostThreshold", defaultConnectionLostThresholdTextField);
        textFields.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getMonitoringDefaultsConfiguration(), lDefaultConfigurationTextFields);

        Map<String, TextField> lWanPreferredServerConfigurationTextFields = new HashMap<>();  // WAN preferred server configuration text fields
        lWanPreferredServerConfigurationTextFields.put("Hostname", wanPreferredHostname);
        lWanPreferredServerConfigurationTextFields.put("Timeout", wanPreferredTimeoutTextField);
        lWanPreferredServerConfigurationTextFields.put("MaxRetries", wanPreferredMaxRetriesTextField);
        lWanPreferredServerConfigurationTextFields.put("PollingPeriod", wanPreferredPollingPeriodTextField);
        lWanPreferredServerConfigurationTextFields.put("ConnectionLostThreshold", wanPreferredConnectionLostThresholdTextField);
        textFields.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan().getPreferredServer(), lWanPreferredServerConfigurationTextFields);

        Map<String, TextField> lWanBackupServerConfigurationTextFields = new HashMap<>();  // WAN backup server configuration text fields
        lWanBackupServerConfigurationTextFields.put("Hostname", wanBackupHostname);
        lWanBackupServerConfigurationTextFields.put("Timeout", wanBackupTimeoutTextField);
        lWanBackupServerConfigurationTextFields.put("MaxRetries", wanBackupMaxRetriesTextField);
        lWanBackupServerConfigurationTextFields.put("PollingPeriod", wanBackupPollingPeriodTextField);
        lWanBackupServerConfigurationTextFields.put("ConnectionLostThreshold", wanBackupConnectionLostThresholdTextField);
        textFields.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan().getBackupServer(), lWanBackupServerConfigurationTextFields);

        Map<String, TextField> lLanPreferredServerConfigurationTextFields = new HashMap<>();  // LAN preferred server configuration text fields
        lLanPreferredServerConfigurationTextFields.put("Hostname", lanPreferredHostname);
        lLanPreferredServerConfigurationTextFields.put("Timeout", lanPreferredTimeoutTextField);
        lLanPreferredServerConfigurationTextFields.put("MaxRetries", lanPreferredMaxRetriesTextField);
        lLanPreferredServerConfigurationTextFields.put("PollingPeriod", lanPreferredPollingPeriodTextField);
        lLanPreferredServerConfigurationTextFields.put("ConnectionLostThreshold", lanPreferredConnectionLostThresholdTextField);
        textFields.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan().getPreferredServer(), lLanPreferredServerConfigurationTextFields);

        Map<String, TextField> lLanBackupServerConfigurationTextFields = new HashMap<>();  // LAN backup server configuration text fields
        lLanBackupServerConfigurationTextFields.put("Hostname", lanBackupHostname);
        lLanBackupServerConfigurationTextFields.put("Timeout", lanBackupTimeoutTextField);
        lLanBackupServerConfigurationTextFields.put("MaxRetries", lanBackupMaxRetriesTextField);
        lLanBackupServerConfigurationTextFields.put("PollingPeriod", lanBackupPollingPeriodTextField);
        lLanBackupServerConfigurationTextFields.put("ConnectionLostThreshold", lanBackupConnectionLostThresholdTextField);
        textFields.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan().getBackupServer(), lLanBackupServerConfigurationTextFields);

        Map<String, TextField> lGlobalConfigurationTextFields = new HashMap<>();  // Global configuration text fields
        lGlobalConfigurationTextFields.put("PollingPeriod", globalPollingPeriodTextField);
        lGlobalConfigurationTextFields.put("PeriodicSpeedTestPollingPeriod", periodicSpeedTestPollingPeriodTextField);
        lGlobalConfigurationTextFields.put("PeriodicReportsPollingPeriod", periodicReportsPollingPeriodTextField);
        lGlobalConfigurationTextFields.put("ConnectionsLostForgetTime", globalConnectionsLostForgetTimeTextField);
        lGlobalConfigurationTextFields.put("MeanTimeBetweenTwoConnectionsLostThreshold1", globalMeanTimeBetweenTwoConnectionsLostThreshold1TextField);
        lGlobalConfigurationTextFields.put("MeanTimeBetweenTwoConnectionsLostThreshold2", globalMeanTimeBetweenTwoConnectionsLostThreshold2TextField);
        lGlobalConfigurationTextFields.put("MeanTimeBetweenTwoConnectionsLostThreshold3", globalMeanTimeBetweenTwoConnectionsLostThreshold3TextField);
        lGlobalConfigurationTextFields.put("MaxStoredPingDuration", globalMaxStoredPingDurationTextField);
        lGlobalConfigurationTextFields.put("MinDisplayedPingDuration", globalMinDisplayedPingDurationTextField);
        lGlobalConfigurationTextFields.put("MaxDisplayedPingDuration", globalMaxDisplayedPingDurationTextField);
        lGlobalConfigurationTextFields.put("MaxStoredSpeedTestDuration", globalMaxStoredSpeedTestDurationTextField);
        lGlobalConfigurationTextFields.put("MinDisplayedSpeedTestDuration", globalMinDisplayedSpeedTestDurationTextField);
        lGlobalConfigurationTextFields.put("MaxDisplayedSpeedTestDuration", globalMaxDisplayedSpeedTestDurationTextField);
        textFields.put(Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration(), lGlobalConfigurationTextFields);

        Map<String, TextField> lAudibleAlarmsTextFields = new HashMap<>(); // Audible alarms configuration text fields
        lAudibleAlarmsTextFields.put("Clear", clearSoundFileTextField);
        lAudibleAlarmsTextFields.put("Info", infoSoundFileTextField);
        lAudibleAlarmsTextFields.put("Warning", warningSoundFileTextField);
        lAudibleAlarmsTextFields.put("Minor", minorSoundFileTextField);
        lAudibleAlarmsTextFields.put("Major", majorSoundFileTextField);
        lAudibleAlarmsTextFields.put("Critical", criticalSoundFileTextField);
        textFields.put(Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration(), lAudibleAlarmsTextFields);

        Map<String, TextField> lEmailConfigurationTextFields = new HashMap<>(); // Email configuration text fields
        lEmailConfigurationTextFields.put("RecipientList", recipientListTextField);
        textFields.put(Configuration.getCurrentConfiguration().getEmailConfiguration(), lEmailConfigurationTextFields);
    }

    /**
     * Initialize check boxes associated to their configuration class and to the set/get method name prefix
     */
    private void initializeCheckBoxes() {

        checkBoxes.keySet().removeAll(checkBoxes.keySet());

        Map<String, CheckBox> lInterfacesConfigurationCheckBoxes = new HashMap<>();  // Interfaces check boxes
        lInterfacesConfigurationCheckBoxes.put("AlertIfSecondaryIsDown", alertIfSecondaryIsDownCheckBox);
        checkBoxes.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration(), lInterfacesConfigurationCheckBoxes);

        Map<String, CheckBox> lDefaultConfigurationCheckBoxes = new HashMap<>();  // Default configuration check boxes
        lDefaultConfigurationCheckBoxes.put("Ipv6", defaultIpv6CheckBox);
        checkBoxes.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getMonitoringDefaultsConfiguration(), lDefaultConfigurationCheckBoxes);

        Map<String, CheckBox> lWanConfigurationCheckBoxes = new HashMap<>();  // WAN configuration check boxes
        lWanConfigurationCheckBoxes.put("UseProxy", wanUseProxy);
        checkBoxes.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan(), lWanConfigurationCheckBoxes);

        Map<String, CheckBox> lWanPreferredServerConfigurationCheckBoxes = new HashMap<>();  // WAN preferred server configuration check boxes
        lWanPreferredServerConfigurationCheckBoxes.put("Ipv6", wanPreferredIpv6CheckBox);
        checkBoxes.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan().getPreferredServer(), lWanPreferredServerConfigurationCheckBoxes);

        Map<String, CheckBox> lWanBackupServerConfigurationCheckBoxes = new HashMap<>();  // WAN backup server configuration check boxes
        lWanBackupServerConfigurationCheckBoxes.put("Ipv6", wanBackupIpv6CheckBox);
        checkBoxes.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan().getBackupServer(), lWanBackupServerConfigurationCheckBoxes);

        Map<String, CheckBox> lLanConfigurationCheckBoxes = new HashMap<>();  // LAN configuration check boxes
        lLanConfigurationCheckBoxes.put("UseProxy", lanUseProxy);
        checkBoxes.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan(), lLanConfigurationCheckBoxes);

        Map<String, CheckBox> lLanPreferredServerConfigurationCheckBoxes = new HashMap<>();  // LAN preferred server configuration check boxes
        lLanPreferredServerConfigurationCheckBoxes.put("Ipv6", lanPreferredIpv6CheckBox);
        checkBoxes.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan().getPreferredServer(), lLanPreferredServerConfigurationCheckBoxes);

        Map<String, CheckBox> lLanBackupServerConfigurationCheckBoxes = new HashMap<>();  // LAN backup server configuration check boxes
        lLanBackupServerConfigurationCheckBoxes.put("Ipv6", lanBackupIpv6CheckBox);
        checkBoxes.put(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan().getBackupServer(), lLanBackupServerConfigurationCheckBoxes);

        Map<String, CheckBox> lAlarmsCheckBoxes = new HashMap<>();  // Alarms check boxes
        lAlarmsCheckBoxes.put("AudibleEnabled", audibleAlarmsCheckBox);
        checkBoxes.put(Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration(), lAlarmsCheckBoxes);

    }

    /**
     * Adds tooltips to text fields
     */
    private void addTextFieldsTooltips() {

        // Parse configuration class for text fields
        for (AbstractConfiguration lConfiguration : textFields.keySet()) {

            // Retrieve method name prefixes for this configuration class
            for (String lTextFieldName : textFields.get(lConfiguration).keySet()) {

                String lTooltipName = "configuration.monitoringJobs." + lConfiguration.getName() + ".tooltip." + lTextFieldName.substring(0, 1).toLowerCase() + lTextFieldName.substring(1);
                String lTooltipText = Display.getViewResourceBundle().getString(lTooltipName);

                // Specific cases
                // Hostname of backup server
                if ((lConfiguration == null ||
                     lConfiguration.equals(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan().getBackupServer()) ||
                     lConfiguration.equals(Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan().getBackupServer())) && lTextFieldName.equals("Hostname"))
                    lTooltipText += Display.getViewResourceBundle().getString("configuration.monitoringJobs.serverConfiguration.tooltip.emptyBackup");

                Tooltip lTooltip = new Tooltip(lTooltipText);
                Tooltip.install(textFields.get(lConfiguration).get(lTextFieldName), lTooltip);

            }

        }

    }

    /**
     * Adds tooltips to check boxes
     */
    private void addCheckBoxesTooltips() {

        // Parse configuration class for check boxes
        for (AbstractConfiguration lConfiguration : checkBoxes.keySet()) {

            // Retrieve method name prefixes for this configuration class
            for (String lCheckBoxName : checkBoxes.get(lConfiguration).keySet()) {

                String lTooltipName = "configuration.monitoringJobs." + lConfiguration.getName() + ".tooltip." + lCheckBoxName.substring(0, 1).toLowerCase() + lCheckBoxName.substring(1);
                Tooltip lTooltip = new Tooltip(Display.getViewResourceBundle().getString(lTooltipName));
                Tooltip.install(checkBoxes.get(lConfiguration).get(lCheckBoxName), lTooltip);

            }

        }

    }
    /**
     * Adds tooltips to simple fields
     */
    private void addSimpleFieldsTooltips() {

        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) {
            addTextFieldsTooltips();
            addCheckBoxesTooltips();
        }
    }

    /**
     * Sets text field to their initial value
     */
    private void initializeTextFieldValues() {

        // Parse configuration class for text fields
        for (AbstractConfiguration lConfiguration : textFields.keySet()) {

            // Retrieve method name prefixes for this configuration class
            for (String lMethodName : textFields.get(lConfiguration).keySet()) {

                // Retrieve text field for this configuration class and get method name
                TextField lTextField = textFields.get(lConfiguration).get(lMethodName);

                String lDefaultValue = AbstractConfiguration.findDefaultValue(lConfiguration, lMethodName);

                try {
                    // Retrieve get method from configuration class and method name
                    Method lGetMethod = lConfiguration.getClass().getDeclaredMethod("get" + lMethodName);
                    // Invoke get method
                    String lValue = lGetMethod.invoke(lConfiguration).toString();
                    lTextField.textProperty().setValue(lValue);
                    // Set different style for attribute which is set to the default value
                    if (lDefaultValue != null && lValue.equals(lDefaultValue)) {
                        lTextField.setId("default-value");
                    } else {
                        lTextField.setId("");
                    }
                    validateConfiguration(lMethodName, lConfiguration, lTextField);
                } catch (NoSuchMethodException | IllegalAccessException|InvocationTargetException e) {
                    Display.logUnexpectedError(e);
                }


            }

        }

    }

    /**
     * Sets check boxes to their initial value
     */
    private void initializeCheckBoxesValues() {

        // Parse configuration class for check boxes
        for (AbstractConfiguration lConfiguration : checkBoxes.keySet()) {

            // Retrieve method name prefixes for this configuration class
            for (String lMethodName : checkBoxes.get(lConfiguration).keySet()) {

                // Retrieve check box for this configuration class and get method name
                CheckBox lCheckBox = checkBoxes.get(lConfiguration).get(lMethodName);

                Boolean lDefaultValue = Boolean.valueOf(AbstractConfiguration.findDefaultValue(lConfiguration, lMethodName));

                try {
                    // Retrieve get method from configuration class and method name
                    Method lGetMethod = lConfiguration.getClass().getDeclaredMethod("get" + lMethodName);
                    // Invoke get method
                    Boolean lValue = (Boolean) lGetMethod.invoke(lConfiguration);
                    lCheckBox.setSelected(lValue);
                    // Set different style for attribute which is set to the default value
                    if (lValue.equals(lDefaultValue)) {
                        lCheckBox.setId("default-value");
                    } else {
                        lCheckBox.setId("");
                    }
                    // Check if configuration has changed in order to enable or disable buttons
                } catch (NoSuchMethodException | IllegalAccessException|InvocationTargetException e) {
                    Display.logUnexpectedError(e);
                }


            }

        }

    }

    /**
     * Sets simple fields to their initial value
     */
    private void initializeSimpleFieldsValues() {
        initializeTextFieldValues();
        initializeCheckBoxesValues();
    }

    /**
     * Sets configured interfaces to their initial values
     */
    private void initializeConfiguredInterfacesValues() {
        configuredInterfaces.clear();
        for (NetworkInterfaceConfiguration lNetworkInterfaceConfiguration:
                Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().getNetworkInterfaceConfigurations()) {
            configuredInterfaces.add(new ConfiguredInterface(lNetworkInterfaceConfiguration));
        }
    }

    /**
     * Sets configured alarms to their initial values
     */
    private void initializeConfiguredAlarmsValues() {
        configuredAlarms.clear();
        for (Integer lAlarmId: Alarm.getAlarmDictionary().keySet()) {
            configuredAlarms.add(new ConfiguredAlarm(Alarm.getAlarmDictionary().get(lAlarmId)));
        }
    }

    /**
     * Sets configured SMTP servers to their initial values
     */
    private void initializeConfiguredSmtpServersValues() {
        configuredSmtpServers.clear();
        for (SmtpServerConfiguration lSmtpServerConfiguration:
                Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations()) {
            configuredSmtpServers.add(new ConfiguredSmtpServer(lSmtpServerConfiguration));
        }

    }

    /**
     * Sets complex fields to their initial values
     */
    private void initializeComplexFieldValues() {
        initializeConfiguredInterfacesValues();
        initializeConfiguredAlarmsValues();
        initializeConfiguredSmtpServersValues();
    }

    /**
     * Adds listeners text fields
     */
    private void addTextFieldsListeners() {

        initializeTextFields();

        // Parse configuration class for text fields
        for (AbstractConfiguration lConfiguration: textFields.keySet()) {

            // Retrieve method name prefixes for this configuration class
            for (String lMethodName: textFields.get(lConfiguration).keySet()) {

                // Retrieve text field for this configuration class and set method name
                TextField lTextField = textFields.get(lConfiguration).get(lMethodName);

                if (textFieldListeners.containsKey(lTextField)) {
                    // Remove existing listener on the text field
                    lTextField.textProperty().removeListener(textFieldListeners.get(lTextField));
                    textFieldListeners.remove(lTextField);
                }

                textFieldListeners.put(lTextField, (observable, oldValue, newValue) -> {

                    String lDefaultValue = AbstractConfiguration.findDefaultValue(lConfiguration, lMethodName);
                    String lInitialValue = AbstractConfiguration.findValue(initialConfigurations.get(lConfiguration), lMethodName);

                    try {
                        // Retrieve set method from configuration class and method name
                        Method lSetMethod = lConfiguration.getClass().getDeclaredMethod("set" + lMethodName, String.class);
                        // Invoke set method
                        lSetMethod.invoke(lConfiguration, newValue);
                        // Set different style for attribute which is set to the default value
                        if (lDefaultValue != null && newValue.equals(lDefaultValue)) {
                            if (newValue.equals(lInitialValue)) {
                                lTextField.setId("default-value");
                            } else {
                                lTextField.setId("default-new-value");
                            }
                        } else if (!newValue.equals(lInitialValue)) {
                            lTextField.setId("new-value");
                        } else {
                            lTextField.setId("");
                        }
                        removeInvalidConfigurationData(lTextField);
                        validateConfiguration(lMethodName, lConfiguration, lTextField);
                    } catch (InvocationTargetException e) {
                        // If the set method has raised an error (bad type for instance), change color and disable save
                        lTextField.setId("bad-value");
                        addInvalidConfigurationData(lTextField);
                    } catch (NoSuchMethodException | IllegalAccessException e) {
                        Display.logUnexpectedError(e);
                    }
                });
                lTextField.textProperty().addListener(textFieldListeners.get(lTextField));

            }

        }

    }

    /**
     * Adds listeners to check boxes
     */
    private void addCheckBoxesListeners() {

        initializeCheckBoxes();

        // Parse configuration class for check boxes
        for (AbstractConfiguration lConfiguration: checkBoxes.keySet()) {

            // Retrieve method name prefixes for this configuration class
            for (String lMethodName: checkBoxes.get(lConfiguration).keySet()) {

                // Retrieve check box for this configuration class and set method name
                CheckBox lCheckBox = checkBoxes.get(lConfiguration).get(lMethodName);

                if (checkBoxListeners.containsKey(lCheckBox)) {
                    // Remove  existing listener on the check box
                    lCheckBox.selectedProperty().removeListener(checkBoxListeners.get(lCheckBox));
                    checkBoxListeners.remove(lCheckBox);
                }

                checkBoxListeners.put(lCheckBox, (observable, oldValue, newValue) -> {

                    Boolean lDefaultValue = Boolean.valueOf(AbstractConfiguration.findDefaultValue(lConfiguration, lMethodName));
                    Boolean lInitialValue = Boolean.valueOf(AbstractConfiguration.findValue(initialConfigurations.get(lConfiguration), lMethodName));

                    try {
                        // Retrieve set method from configuration class and method name
                        Method lSetMethod = lConfiguration.getClass().getDeclaredMethod("set" + lMethodName, String.class);
                        // Invoke set method
                        lSetMethod.invoke(lConfiguration, newValue.toString());
                        // Set different style for attribute which is set to the default value
                        if (newValue.equals(lDefaultValue)) {
                            if (newValue.equals(lInitialValue)) {
                                lCheckBox.setId("default-value");
                            } else {
                                lCheckBox.setId("default-new-value");
                            }
                        } else if (!newValue.equals(lInitialValue)) {
                            lCheckBox.setId("new-value");
                        } else {
                            lCheckBox.setId("");
                        }
                        removeInvalidConfigurationData(lCheckBox);
                    } catch (InvocationTargetException e) {
                        // If the set method has raised an error (bad type for instance), change color and disable save
                        lCheckBox.setId("bad-value");
                        addInvalidConfigurationData(lCheckBox);
                    } catch (NoSuchMethodException | IllegalAccessException e) {
                        Display.logUnexpectedError(e);
                    }

                    // Audible alarms specific cases
                    if (lCheckBox.equals(audibleAlarmsCheckBox)) {
                        changeAudibleAlarmsState(newValue);
                    }

                });
                lCheckBox.selectedProperty().addListener(checkBoxListeners.get(lCheckBox));
            }
        }

    }

    /**
     * Changes audible alarm configuration fields state depending on audible alarms are enabled or not
     * @param aInEnabled true if audible alarms are enabled, false otherwise
     */
    private void changeAudibleAlarmsState(boolean aInEnabled) {
        muteStartTimeHourSpinner.setDisable(!aInEnabled);
        muteStartTimeMinuteSpinner.setDisable(!aInEnabled);
        muteEndTimeHourSpinner.setDisable(!aInEnabled);
        muteEndTimeMinuteSpinner.setDisable(!aInEnabled);
    }

    /**
     * Adds listeners to fields (text fields, check boxes) that are dependent on the current configuration and that must be recreated when the configuration changes
     */
    private void addSimpleFieldListeners() {
        initializeInitialConfiguration();
        addTextFieldsListeners();
        addCheckBoxesListeners();
    }

    /**
     * Checks if the configuration has been modified
     */
    private void checkConfigurationChanges() {
        boolean lConfigurationHasChanged =
                !Configuration.getCurrentConfiguration().isSameAs(Configuration.getInitialConfiguration());
        saveButton.setDisable(!lConfigurationHasChanged || !saveButton.isVisible());
        cancelButton.setDisable(!lConfigurationHasChanged || !cancelButton.isVisible());
    }

    /**
     * Validates specific elements in the configuration
     * @param aInConfigurationDataId Identification of the configuration data to check
     * @param aInConfiguration       Configuration object to which the configuration data belongs to
     * @param aInConfigurationObject Configuration object in the configuration panel (text field, check box, ...
     */
    private void validateConfiguration(String aInConfigurationDataId, AbstractConfiguration aInConfiguration, Object aInConfigurationObject) {

        NetworkConfiguration lWanConfiguration = Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan();
        NetworkConfiguration lLanConfiguration = Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan();

        switch (aInConfigurationDataId) {
            case "Hostname":
                // Modification of hostname in wan or lan monitoring configuration, check valid empty cases
                if (aInConfiguration.getParentConfiguration().equals(lWanConfiguration) || aInConfiguration.getParentConfiguration().equals(lLanConfiguration)) {

                    String lWanPreferredHostname = lWanConfiguration.getPreferredServer().getHostname();
                    String lWanBackupHostname = lWanConfiguration.getBackupServer().getHostname();
                    String lLanPreferredHostname = lLanConfiguration.getPreferredServer().getHostname();
                    String lLanBackupHostname = lLanConfiguration.getBackupServer().getHostname();

                    if ((lWanPreferredHostname.equals("") && lWanBackupHostname.equals("") && lLanPreferredHostname.equals("") && lLanBackupHostname.equals(""))) {
                        addInvalidConfigurationData(wanPreferredHostname);
                        wanPreferredHostname.setId("bad-value");
                        addInvalidConfigurationData(wanBackupHostname);
                        wanBackupHostname.setId("bad-value");
                        addInvalidConfigurationData(lanPreferredHostname);
                        lanPreferredHostname.setId("bad-value");
                        addInvalidConfigurationData(lanBackupHostname);
                        lanBackupHostname.setId("bad-value");
                    } else {

                        if (lWanPreferredHostname.equals("")) {
                            if (!lWanBackupHostname.equals("")) {
                                addInvalidConfigurationData(wanPreferredHostname);
                                wanPreferredHostname.setId("bad-value");
                            } else {
                                removeInvalidConfigurationData(wanPreferredHostname);
                                wanPreferredHostname.setId("");
                            }
                        } else {
                            removeInvalidConfigurationData(wanPreferredHostname);
                            wanPreferredHostname.setId("");
                        }
                        removeInvalidConfigurationData(wanBackupHostname);
                        wanBackupHostname.setId("");

                        if (lLanPreferredHostname.equals("")) {
                            if (!lLanBackupHostname.equals("")) {
                                addInvalidConfigurationData(lanPreferredHostname);
                                lanPreferredHostname.setId("bad-value");
                            } else {
                                removeInvalidConfigurationData(lanPreferredHostname);
                                lanPreferredHostname.setId("");
                            }
                        } else {
                            removeInvalidConfigurationData(lanPreferredHostname);
                            lanPreferredHostname.setId("");
                        }
                        removeInvalidConfigurationData(lanBackupHostname);
                        lanBackupHostname.setId("");

                    }

                }
                break;
            case "Interfaces":
                if (((TableView) aInConfigurationObject).getItems().size() == 0) {
                    ((TableView) aInConfigurationObject).setId("bad-list");
                    addInvalidConfigurationData(aInConfigurationObject);
                } else {
                    List<NetworkInterfaceConfiguration> lNetworkInterfaceConfigurations = ((NetworkInterfacesConfiguration) aInConfiguration).getNetworkInterfaceConfigurations();
                    NetworkInterfaceConfiguration lDefaultNetworkInterfaceConfiguration = new NetworkInterfaceConfiguration(aInConfiguration.getConfiguration());
                    if (lNetworkInterfaceConfigurations.size() == 1 && lDefaultNetworkInterfaceConfiguration.getName().equals(lNetworkInterfaceConfigurations.get(0).getName())) {
                        if (!((NetworkInterfacesConfiguration) aInConfiguration).isSameAs(
                                Configuration.getInitialConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration())) {
                            ((TableView) aInConfigurationObject).setId("default-new-list");
                        } else {
                            ((TableView) aInConfigurationObject).setId("default-list");
                        }
                    } else if (!((NetworkInterfacesConfiguration) aInConfiguration).isSameAs(
                            Configuration.getInitialConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration())) {
                        ((TableView) aInConfigurationObject).setId("new-list");
                    } else {
                        ((TableView) aInConfigurationObject).setId("");
                    }
                    removeInvalidConfigurationData(aInConfigurationObject);
                }
                break;
            case "Alarms":
                boolean lIsDefault = true;
                for (AlarmConfiguration lAlarmConfiguration: ((AlarmsConfiguration) aInConfiguration).getAlarmConfigurations()) {
                    if ((lAlarmConfiguration.getNewSeverity() != null) || lAlarmConfiguration.getIsFiltered()) {
                        lIsDefault = false;
                        break;
                    }
                }
                if (lIsDefault) {
                    if (!((AlarmsConfiguration) aInConfiguration).hasSameAlarmConfigurations(Configuration.getInitialConfiguration().getAlarmsConfiguration())) {
                        ((TableView) aInConfigurationObject).setId("default-new-list");
                    } else {
                        ((TableView) aInConfigurationObject).setId("default-list");
                    }
                } else if (!((AlarmsConfiguration) aInConfiguration).hasSameAlarmConfigurations(Configuration.getInitialConfiguration().getAlarmsConfiguration())) {
                    ((TableView) aInConfigurationObject).setId("new-list");
                } else {
                    ((TableView) aInConfigurationObject).setId("");
                }
                break;
            case "SmtpServers":
                // No items -> default value
                if (((TableView) aInConfigurationObject).getItems().size() == 0) {
                    if (!((SmtpServersConfiguration) aInConfiguration).isSameAs(Configuration.getInitialConfiguration().getEmailConfiguration().getSmtpServersConfiguration())) {
                        ((TableView) aInConfigurationObject).setId("default-new-list");
                    } else {
                        ((TableView) aInConfigurationObject).setId("default-list");
                    }
                } else if (!((SmtpServersConfiguration) aInConfiguration).isSameAs(
                        Configuration.getInitialConfiguration().getEmailConfiguration().getSmtpServersConfiguration()) ||
                        (((SmtpServersConfiguration) aInConfiguration).getPreferredSmtpServer() != null &&
                                !((SmtpServersConfiguration) aInConfiguration).getPreferredSmtpServer().equals(
                                   Configuration.getInitialConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer()))) {
                    ((TableView) aInConfigurationObject).setId("new-list");
                } else {
                    ((TableView) aInConfigurationObject).setId("");
                }
                break;
            default:
                break;
        }
    }

    /**
     * Adds an invalid configuration data and changes buttons state accordingly
     * @param aInConfigurationData Configuration data object that is invalid (text field, check box, ...)
     */
    private void addInvalidConfigurationData(Object aInConfigurationData) {
        if (!invalidConfigurationData.contains(aInConfigurationData)) invalidConfigurationData.add(aInConfigurationData);
        saveButton.setDisable(true);
        cancelButton.setDisable(false);
    }

    /**
     * Removes an invalid configuration data and changes buttons state accordingly
     * @param aInConfigurationData Configuration data object that is invalid (text field, check box, ...)
     */
    private void removeInvalidConfigurationData(Object aInConfigurationData) {
        if (invalidConfigurationData.contains(aInConfigurationData)) invalidConfigurationData.remove(aInConfigurationData);
        if (invalidConfigurationData.size() == 0) {
            checkConfigurationChanges();
        }
    }

    /**
     * Changes the state of general buttons
     * @param aInSaveState Initial state of the save button (enabled if true, disabled otherwise)
     */
    private void setGeneralButtonsStates(boolean aInSaveState) {
        saveButton.setVisible(!Preferences.getInstance().getBooleanValue("autoSaveConfiguration"));
        cancelButton.setVisible(Preferences.getInstance().getBooleanValue("autoSaveConfiguration"));
        saveButton.setDisable(!aInSaveState || !saveButton.isVisible());
        cancelButton.setDisable(!aInSaveState || !cancelButton.isVisible());
    }

    /**
     * Changes the state of specific buttons
     */
    private void setSpecificButtonsStates() {
        setInterfacesButtonsStates();
        setSmtpServersButtonsStates();
    }

    /**
     * Changes the state of interfaces table buttons
     */
    private void setInterfacesButtonsStates() {
        addInterfaceButton.setDisable(configuredInterfaces.size() >= Constants.MAXIMUM_NUMBER_OF_MONITORED_INTERFACES);
        deleteInterfaceButton.setDisable((interfacesTable.getSelectionModel().getSelectedIndices().size() == 0) ||
                                         (interfacesTable.getItems().size() <= 0));
        upInterfaceButton.setDisable((interfacesTable.getSelectionModel().getSelectedIndices().size() == 0) ||
                                     (interfacesTable.getSelectionModel().getSelectedIndices().get(0) == 0));
        downInterfaceButton.setDisable((interfacesTable.getSelectionModel().getSelectedIndices().size() == 0) ||
                                       (interfacesTable.getSelectionModel().getSelectedIndices().get(interfacesTable.getSelectionModel().getSelectedIndices().size() - 1) ==
                                        interfacesTable.getItems().size() - 1));
    }

    /**
     * Changes the state of SMTP servers table buttons
     */
    private void setSmtpServersButtonsStates() {
        deleteSmtpServerButton.setDisable((smtpServersTable.getSelectionModel().getSelectedIndices().size() == 0) ||
                                         (smtpServersTable.getItems().size() <= 0));
        editSmtpServerButton.setDisable(smtpServersTable.getSelectionModel().getSelectedIndices().size() != 1);
        preferredSmtpServerButton.setDisable((smtpServersTable.getSelectionModel().getSelectedIndices().size() != 1) ||
                                             (((smtpServersTable.getSelectionModel().getSelectedItem() != null) &&
                                                     Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer() != null &&
                                                     Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer().equals(
                                                     smtpServersTable.getSelectionModel().getSelectedItem().getUser() + "@" +
                                                     smtpServersTable.getSelectionModel().getSelectedItem().getName()))));
        upSmtpServerButton.setDisable((smtpServersTable.getSelectionModel().getSelectedIndices().size() == 0) ||
                                     (smtpServersTable.getSelectionModel().getSelectedIndices().get(0) == 0));
        downSmtpServerButton.setDisable((smtpServersTable.getSelectionModel().getSelectedIndices().size() == 0) ||
                                       (smtpServersTable.getSelectionModel().getSelectedIndices().get(smtpServersTable.getSelectionModel().getSelectedIndices().size() - 1) ==
                                        smtpServersTable.getItems().size() - 1));
    }

    /**
     * Changes the state of all buttons
     * @param aInSaveState Initial state of the save button (enabled if true, disabled otherwise)
     */
    private void setButtonsStates(boolean aInSaveState) {
        setGeneralButtonsStates(aInSaveState);
        setSpecificButtonsStates();
    }

    private void addEditSmtpServer(ConfiguredSmtpServer aInConfiguredSmtpServer) {

        // Memorize lines to reselect at the end
        List<Integer> lSelectedRows = new ArrayList<>(smtpServersTable.getSelectionModel().getSelectedIndices());

        addEditSmtpServerDialogController.show(aInConfiguredSmtpServer);

        // Display SMTP servers
        initializeConfiguredSmtpServersValues();

        // Restore selection
        for (Integer lRow: lSelectedRows) {
            smtpServersTable.getSelectionModel().select(lRow);
        }

        validateConfiguration("SmtpServers", Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration(), smtpServersTable);
        checkConfigurationChanges();

    }

    // METHODS

    /**
     * Displays the monitoring jobs configuration dialog box
     * @param aInSaveState Initial state of the save button (enabled if true, disabled otherwise)
     */
    public void show(boolean aInSaveState) {

        saveState = aInSaveState;

        // Select last selected tabs
        configurationTabPane.getSelectionModel().select(States.getInstance().getIntegerValue("configuration.selectedTab", 0));
        monitoringJobsTabPane.getSelectionModel().select(States.getInstance().getIntegerValue("configuration.monitoringJobs.selectedTab", 0));

        // Add listeners on simple fields
        addSimpleFieldListeners();

        // Set initial values to simple fields
        initializeSimpleFieldsValues();

        // Set initial values to complex fields
        initializeComplexFieldValues();

        AudibleAlarmsConfiguration lAudibleAlarmsConfiguration = Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration();
        muteStartTimeHourSpinner.getEditor().setText(
                String.valueOf((lAudibleAlarmsConfiguration != null && lAudibleAlarmsConfiguration.getMuteStartTime() != null) ?
                lAudibleAlarmsConfiguration.getMuteStartHour() : AudibleAlarmsConfiguration.getDefaultMuteStartHour()));
        checkTime(true, muteStartTimeHourSpinner, "", muteStartTimeHourSpinner.getEditor().getText(), "MuteStartTime");
        muteStartTimeMinuteSpinner.getEditor().setText(
                String.valueOf((lAudibleAlarmsConfiguration != null && lAudibleAlarmsConfiguration.getMuteStartTime() != null) ?
                lAudibleAlarmsConfiguration.getMuteStartMinute() : AudibleAlarmsConfiguration.getDefaultMuteStartMinute()));
        checkTime(false, muteStartTimeMinuteSpinner, "", muteStartTimeMinuteSpinner.getEditor().getText(), "MuteStartTime");
        muteEndTimeHourSpinner.getEditor().setText(
                String.valueOf((lAudibleAlarmsConfiguration != null && lAudibleAlarmsConfiguration.getMuteEndTime() != null) ?
                lAudibleAlarmsConfiguration.getMuteEndHour() : AudibleAlarmsConfiguration.getDefaultMuteEndHour()));
        checkTime(true, muteEndTimeHourSpinner, "", muteEndTimeHourSpinner.getEditor().getText(), "MuteEndTime");
        muteEndTimeMinuteSpinner.getEditor().setText(
                String.valueOf((lAudibleAlarmsConfiguration != null && lAudibleAlarmsConfiguration.getMuteEndTime() != null) ?
                lAudibleAlarmsConfiguration.getMuteEndMinute() : AudibleAlarmsConfiguration.getDefaultMuteEndMinute()));
        checkTime(false, muteEndTimeMinuteSpinner, "", muteEndTimeMinuteSpinner.getEditor().getText(), "MuteEndTime");
        changeAudibleAlarmsState(!audibleAlarmsCheckBox.isDisable());

        validateConfiguration("Interfaces", Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration(), interfacesTable);
        validateConfiguration("SmtpServers", Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration(), smtpServersTable);
        setButtonsStates(aInSaveState);

        // Set ipv6 state
        boolean lIpv6Supported = Network.isIpv6Supported();
        defaultIpv6CheckBox.setDisable(!lIpv6Supported);
        wanPreferredIpv6CheckBox.setDisable(!lIpv6Supported);
        wanBackupIpv6CheckBox.setDisable(!lIpv6Supported);
        lanPreferredIpv6CheckBox.setDisable(!lIpv6Supported);
        lanBackupIpv6CheckBox.setDisable(!lIpv6Supported);

        // Display
        dialogStage.showAndWait();

    }

    /**
     * Adds network interfaces to selection
     */
    public void addInterfaces(ObservableList<ConfiguredInterface> aInConfiguredInterfaces) {

        // Add added interfaces to the displayed interfaces table
        configuredInterfaces.addAll(aInConfiguredInterfaces);

        List<Integer> lSelectedRows = new ArrayList<>();

        // Add added interfaces to the configuration
        for (ConfiguredInterface lConfiguredInterface : aInConfiguredInterfaces) {
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().addNetworkInterfaceConfiguration(
                    new NetworkInterfaceConfiguration(Configuration.getCurrentConfiguration(), lConfiguredInterface.getPriority(), lConfiguredInterface.getName()));
            lSelectedRows.add(lConfiguredInterface.getPriority());
        }

        // Display interfaces
        initializeConfiguredInterfacesValues();

        // Restore selection
        for (Integer lRow: lSelectedRows) {
            interfacesTable.getSelectionModel().select(lRow - 1);
        }

        validateConfiguration("Interfaces", Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration(), interfacesTable);
        setInterfacesButtonsStates();
        //addInterfaceButton.setDisable(configuredInterfaces.size() >= Constants.MAXIMUM_NUMBER_OF_MONITORED_INTERFACES);

    }

    // FXML METHODS

    /**
     * Adds network interfaces
     */
    @FXML private void addInterfaces() {
        addNetworkInterfacesDialogController.show(this);
    }

    /**
     * Adds SMTP server
     */
    @FXML private void addSmtpServer() {
        addEditSmtpServer(null);
    }

    @FXML private void editSmtpServer() {
        addEditSmtpServer(smtpServersTable.getSelectionModel().getSelectedItem());
    }

    /**
     * Deletes network interfaces and changes priority accordingly
     */
    @FXML private void deleteInterfaces() {

        List<Integer> lSelectedRows = new ArrayList<>();

        for (ConfiguredInterface lSelectedConfiguredInterface : interfacesTable.getSelectionModel().getSelectedItems()) {
            // Memorize lines to reselect at the end
            lSelectedRows.add(lSelectedConfiguredInterface.getPriority());
        }

        // Remove selected lines starting from the end in the original interfaces configuration list
        for (int lIndex = interfacesTable.getSelectionModel().getSelectedIndices().size() - 1; lIndex >= 0; lIndex--) {
            int lIndice = interfacesTable.getSelectionModel().getSelectedIndices().get(lIndex);
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().removeNetworkInterfaceConfiguration(lIndice);
        }

        // Re-order interfaces
        int lPriority = 1;
        for (NetworkInterfaceConfiguration lNetworkInterfaceConfiguration:
                Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().getNetworkInterfaceConfigurations()) {
            lNetworkInterfaceConfiguration.changePriority(lPriority++);
        }

        // Display interfaces
        initializeConfiguredInterfacesValues();

        // Restore selection
        for (Integer lRow: lSelectedRows) {
            interfacesTable.getSelectionModel().select(lRow - 1);
        }

        validateConfiguration("Interfaces", Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration(), interfacesTable);
        addInterfaceButton.setDisable(configuredInterfaces.size() >= Constants.MAXIMUM_NUMBER_OF_MONITORED_INTERFACES);

    }

    /**
     * Moves interfaces selection up (increase priority)
     */
    @FXML private void upInterfaces() {

        // If first row of selection is already at the top, do nothing
        if (interfacesTable.getSelectionModel().getSelectedItems().get(0).getPriority() > 1) {

            List<Integer> lSelectedRows = new ArrayList<>();

            for (ConfiguredInterface lSelectedConfiguredInterface : interfacesTable.getSelectionModel().getSelectedItems()) {
                // Decrement priority (move up) in the selection of selected rows
                lSelectedConfiguredInterface.decrementPriority();
                // Memorize lines to reselect at the end
                lSelectedRows.add(lSelectedConfiguredInterface.getPriority());
            }

            for (NetworkInterfaceConfiguration lNetworkInterfaceConfiguration:
                    Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().getNetworkInterfaceConfigurations()) {
                for (ConfiguredInterface lSelectedConfiguredInterface : interfacesTable.getSelectionModel().getSelectedItems()) {
                    // Increment priority (move down) in the interfaces list of rows which are just before a selected line
                    if (!lNetworkInterfaceConfiguration.getName().equals(lSelectedConfiguredInterface.getName()) && lNetworkInterfaceConfiguration.getPriority() ==
                                                                                                                    lSelectedConfiguredInterface.getPriority())
                        lNetworkInterfaceConfiguration.incrementPriority();
                    // Decrement priority (move up) in the interfaces list of rows which are selected
                    if (lNetworkInterfaceConfiguration.getName().equals(lSelectedConfiguredInterface.getName()))
                        lNetworkInterfaceConfiguration.decrementPriority();
                }
            }

            // Sort network interfaces configuration
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().getNetworkInterfaceConfigurations()
                         .sort(Comparator.comparing(NetworkInterfaceConfiguration::getPriority));

            // Display interfaces
            initializeConfiguredInterfacesValues();

            // Restore selection
            for (Integer lRow: lSelectedRows) {
                interfacesTable.getSelectionModel().select(lRow - 1);
            }

            // Check changes
            validateConfiguration("Interfaces", Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration(), interfacesTable);
            checkConfigurationChanges();

        }
    }

    /**
     * Moves interfaces selection down (decreases priority)
     */
    @FXML private void downInterfaces() {

        // If last row of selection is already at the bottom, do nothing
        if (interfacesTable.getSelectionModel().getSelectedItems().get(interfacesTable.getSelectionModel().getSelectedItems().size() - 1).getPriority() <
            interfacesTable.getItems().size()) {

            List<Integer> lSelectedRows = new ArrayList<>();

            for (ConfiguredInterface lSelectedConfiguredInterface : interfacesTable.getSelectionModel().getSelectedItems()) {
                // Increment priority (move down) in the selection of selected rows
                lSelectedConfiguredInterface.incrementPriority();
                // Memorize lines to reselect at the end
                lSelectedRows.add(lSelectedConfiguredInterface.getPriority());
            }

            for (NetworkInterfaceConfiguration lNetworkInterfaceConfiguration:
                    Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().getNetworkInterfaceConfigurations()) {
                for (int lIndex = interfacesTable.getSelectionModel().getSelectedItems().size() - 1; lIndex >= 0; lIndex--) {
                    ConfiguredInterface lSelectedConfiguredInterface = interfacesTable.getSelectionModel().getSelectedItems().get(lIndex);
                    // Decrement priority (move up) in the interfaces list of rows 1927which are just before a selected line
                    if (!lNetworkInterfaceConfiguration.getName().equals(lSelectedConfiguredInterface.getName()) && lNetworkInterfaceConfiguration.getPriority() == lSelectedConfiguredInterface.getPriority())
                        lNetworkInterfaceConfiguration.decrementPriority();
                    // Increment priority (move down) in the interfaces list of rows which are selected
                    if (lNetworkInterfaceConfiguration.getName().equals(lSelectedConfiguredInterface.getName()))
                        lNetworkInterfaceConfiguration.incrementPriority();
                }
            }

            // Sort network interfaces configuration
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().getNetworkInterfaceConfigurations()
                         .sort(Comparator.comparing(NetworkInterfaceConfiguration::getPriority));

            // Display interfaces
            initializeConfiguredInterfacesValues();

            // Restore selection
            for (Integer lRow: lSelectedRows) {
                interfacesTable.getSelectionModel().select(lRow - 1);
            }

            // Check changes
            validateConfiguration("Interfaces", Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration(), interfacesTable);
            checkConfigurationChanges();

        }
    }

    /**
     * Deletes SMTP servers
     */
    @FXML private void deleteSmtpServers() {

        // Memorize lines to reselect at the end
        List<Integer> lSelectedRows = new ArrayList<>(smtpServersTable.getSelectionModel().getSelectedIndices());

        // Remove selected lines starting from the end in the original interfaces configuration list
        for (int lIndex = smtpServersTable.getSelectionModel().getSelectedIndices().size() - 1; lIndex >= 0; lIndex--) {
            int lIndice = smtpServersTable.getSelectionModel().getSelectedIndices().get(lIndex);
            // Eventually change preferred SMTP server
            boolean lChangePreferredSmtpServer =
            (Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer().equals(
                    Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations().get(lIndice).getUser() + "@" +
                    Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations().get(lIndice).getName()));
            Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().removeSmtpServerConfiguration(lIndice);
            if (lChangePreferredSmtpServer) {
                if (Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations().size() != 0) {
                    Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().setPreferredSmtpServer(
                            Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations().get(0).getUser() + "@" +
                            Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations().get(0).getName());
                } else {
                    Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().setPreferredSmtpServer("");
                }
            }
        }

        // Display SMTP servers
        initializeConfiguredSmtpServersValues();

        // Restore selection
        for (Integer lRow: lSelectedRows) {
            smtpServersTable.getSelectionModel().select(lRow - 1);
        }

        validateConfiguration("SmtpServers", Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration(), smtpServersTable);
        checkConfigurationChanges();

    }

    /**
     * Moves SMTP servers selection up
     */
    @FXML private void upSmtpServers() {

        // If last row of selection is already at the bottom, do nothing
        if (smtpServersTable.getSelectionModel().getSelectedIndices().get(0) != 0) {

            // Memorize lines to reselect at the end
            List<Integer> lSelectedRows = new ArrayList<>(smtpServersTable.getSelectionModel().getSelectedIndices());

            List<SmtpServerConfiguration> lSmtpServerConfigurations =
                    Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations();

            for (int lIndex = smtpServersTable.getSelectionModel().getSelectedIndices().size() - 1; lIndex >= 0; lIndex--) {
                // Swap selected row with the one above
                int lSelectedRow = smtpServersTable.getSelectionModel().getSelectedIndices().get(lIndex);
                Collections.swap(lSmtpServerConfigurations, lSelectedRow, lSelectedRow - 1);
            }

            // Display SMTP servers
            initializeConfiguredSmtpServersValues();

            // Restore selection
            for (Integer lRow: lSelectedRows) {
                smtpServersTable.getSelectionModel().select(lRow - 1);
            }

            // Check changes
            validateConfiguration("SmtpServers", Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration(), smtpServersTable);
            checkConfigurationChanges();

        }
    }

    /**
     * Moves SMTP servers selection down
     */
    @FXML private void downSmtpServers() {

        // If last row of selection is already at the bottom, do nothing
        if (smtpServersTable.getSelectionModel().getSelectedIndices().get(smtpServersTable.getSelectionModel().getSelectedIndices().size() - 1) !=
            smtpServersTable.getItems().size() - 1) {

            // Memorize lines to reselect at the end
            List<Integer> lSelectedRows = new ArrayList<>(smtpServersTable.getSelectionModel().getSelectedIndices());

            List<SmtpServerConfiguration> lSmtpServerConfigurations =
                    Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations();

            for (int lIndex = smtpServersTable.getSelectionModel().getSelectedIndices().size() - 1; lIndex >= 0; lIndex--) {
                // Swap selected row with the one below
                int lSelectedRow = smtpServersTable.getSelectionModel().getSelectedIndices().get(lIndex);
                Collections.swap(lSmtpServerConfigurations, lSelectedRow, lSelectedRow + 1);
            }

            // Display SMTP servers
            initializeConfiguredSmtpServersValues();

            // Restore selection
            for (Integer lRow: lSelectedRows) {
                smtpServersTable.getSelectionModel().select(lRow + 1);
            }

            // Check changes
            validateConfiguration("SmtpServers", Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration(), smtpServersTable);
            checkConfigurationChanges();

        }
    }

    /**
     * Sets preferred SMTP server
     */
    @FXML private void setPreferredSmtpServer() {

        // Set preferred SMTP server to the selected one if it is not already the case
        String lSelectedPreferredSmtpServer =
                smtpServersTable.getSelectionModel().getSelectedItem().getUser() + "@" + smtpServersTable.getSelectionModel().getSelectedItem().getName();
        if (Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer() == null ||
                !Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer().equals(lSelectedPreferredSmtpServer)) {

            // Memorize lines to reselect at the end
            int lSelectedRow = smtpServersTable.getSelectionModel().getSelectedIndex();

            Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().setPreferredSmtpServer(lSelectedPreferredSmtpServer);
            // Display SMTP servers
            initializeConfiguredSmtpServersValues();

            // Restore selection
            smtpServersTable.getSelectionModel().select(lSelectedRow);

            // Check changes
            validateConfiguration("SmtpServers", Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration(), smtpServersTable);
            checkConfigurationChanges();

        }
    }

    /**
     * Cancels configuration changes
     */
    @FXML private void cancel() {

        // Ask user for confirmation
        Alert lConfirmation =
                new Alert(Alert.AlertType.CONFIRMATION, Display.getViewResourceBundle().getString("confirm.cancelConfiguration.question"), ButtonType.YES, ButtonType.NO);
        lConfirmation.setHeaderText(Display.getViewResourceBundle().getString("confirm.cancelConfiguration.title"));
        lConfirmation.initModality(Modality.APPLICATION_MODAL);

        // Display confirmation dialog box
        Optional<ButtonType> lResponse = lConfirmation.showAndWait();

        // OK is pressed
        if (lResponse.isPresent() && lResponse.get().equals(ButtonType.YES)) {
            Configuration.resetConfiguration();
            dialogStage.close();
        }

    }

    /**
     * Saves configuration changes
     */
    @FXML private void save() {

        File lFile;

        // No file has been defined, open file dialog box to choose it
        if (Configuration.getCurrentConfiguration().getFile() == null) {

            FileChooser lFileChooser = new FileChooser();

            // Retrieve last path from preferences
            String lSaveFilePath = Preferences.getInstance().getValue("saveFilePath", null);
            File lInitialFilePath;
            if (lSaveFilePath == null) {
                lInitialFilePath = new File(new File("").getAbsolutePath());
            } else {
                lInitialFilePath = new File(lSaveFilePath);
            }

            // Set extension filter
            FileChooser.ExtensionFilter lExtensionFilter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
            lFileChooser.getExtensionFilters().add(lExtensionFilter);
            lFileChooser.setInitialDirectory(lInitialFilePath);

            // Show save file dialog
            lFile = lFileChooser.showSaveDialog(dialogStage);

        } else {
            lFile = new File(Configuration.getCurrentConfiguration().getFile());
        }

        // Save only if user did not press cancel
        if (lFile != null) {

            // Make sure file has the correct extension
            if (!lFile.getPath().endsWith(".xml")) {
                lFile = new File(lFile.getPath() + ".xml");
            }

            // Save and reset the configuration
            boolean lShouldRestart = saveState ||
                                     !Configuration.getCurrentConfiguration().isSameAs(Configuration.getInitialConfiguration());
            Configuration.getCurrentConfiguration().saveAs(lFile.getPath());
            Configuration.resetConfiguration();

            // Re-initialize values so that all styles are reset
            initializeSimpleFieldsValues();
            initializeComplexFieldValues();
            validateConfiguration("SmtpServers", Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration(), smtpServersTable);


            // Re-add volatile listeners
            addSimpleFieldListeners();

            // Save last chosen path and file in preferences
            Preferences.getInstance().saveValue("saveFilePath", lFile.getParent());
            Preferences.getInstance().saveValue("configurationFile", lFile.getPath());

            // Disable the save/cancel button
            saveButton.setDisable(true);
            cancelButton.setDisable(true);

            // Restart jobs depending on the changes
            if (lShouldRestart) Cat.restart();

        }

    }

    /**
     * Closes dialog box and saves configuration first if needed
     */
    @FXML public void close() {

        // If some fields are invalid, all changes are ignored
        if (invalidConfigurationData.size() != 0) {

            // Ask user for close confirmation
            Alert lConfirmation =
                    new Alert(Alert.AlertType.CONFIRMATION, Display.getViewResourceBundle().getString("confirm.exitConfigurationError.question"), ButtonType.YES, ButtonType.NO);
            lConfirmation.setHeaderText(Display.getViewResourceBundle().getString("confirm.exit.title"));
            lConfirmation.initModality(Modality.APPLICATION_MODAL);


            // Display confirmation dialog box
            Optional<ButtonType> lResponse = lConfirmation.showAndWait();

            // OK is pressed
            if (lResponse.isPresent() && lResponse.get().equals(ButtonType.YES)) {
                dialogStage.close();
                Configuration.resetConfiguration();
            }

        } else {

            // Check if configuration has changed
            if (!Configuration.getCurrentConfiguration().isSameAs(Configuration.getInitialConfiguration()) || saveState) {
                // Auto save
                if (Preferences.getInstance().getBooleanValue("autoSaveConfiguration")) {
                    save();
                    dialogStage.close();
                } else {

                    // Ask user for saving
                    Alert lConfirmation =
                            new Alert(Alert.AlertType.CONFIRMATION, Display.getViewResourceBundle().getString("confirm.saveConfiguration.question"),
                                      ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                    lConfirmation.setHeaderText(Display.getViewResourceBundle().getString("confirm.saveConfiguration.title"));
                    lConfirmation.initModality(Modality.APPLICATION_MODAL);

                    // Display confirmation dialog box
                    Optional<ButtonType> lResponse = lConfirmation.showAndWait();

                    // OK is pressed
                    if (lResponse.isPresent() && lResponse.get().equals(ButtonType.YES)) {
                        save();
                        dialogStage.close();
                    } else if (lResponse.isPresent() && lResponse.get().equals(ButtonType.NO)) {
                        Configuration.resetConfiguration();
                        dialogStage.close();
                    }

                }
            } else {
                dialogStage.close();
            }

        }

    }

    @FXML private void restoreDefaultConfiguration() {

        // RESTORE SPECIFIC
        Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().removeAllNetworkInterfaceConfigurations();
        Configuration.getCurrentConfiguration().getAlarmsConfiguration().removeAllAlarmsConfiguration();
        Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().removeAllSmtpServersConfiguration();
        initializeComplexFieldValues();
        validateConfiguration("Interfaces", Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration(), interfacesTable);
        validateConfiguration("Alarms", Configuration.getCurrentConfiguration().getAlarmsConfiguration(), alarmsTable);
        validateConfiguration("SmtpServers", Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration(), smtpServersTable);

        muteStartTimeHourSpinner.getEditor().textProperty().setValue(String.valueOf(AudibleAlarmsConfiguration.getDefaultMuteStartHour()));
        muteStartTimeMinuteSpinner.getEditor().textProperty().setValue(String.valueOf(AudibleAlarmsConfiguration.getDefaultMuteStartMinute()));
        muteEndTimeHourSpinner.getEditor().textProperty().setValue(String.valueOf(AudibleAlarmsConfiguration.getDefaultMuteEndHour()));
        muteEndTimeMinuteSpinner.getEditor().textProperty().setValue(String.valueOf(AudibleAlarmsConfiguration.getDefaultMuteStartMinute()));

        // RESTORE TEXT FIELDS

        // Parse configuration class for text fields
        for (AbstractConfiguration lConfiguration : textFields.keySet()) {

            // Retrieve method name prefixes for this configuration class
            for (String lMethodName : textFields.get(lConfiguration).keySet()) {

                // Retrieve text field for this configuration class and get method name
                TextField lTextField = textFields.get(lConfiguration).get(lMethodName);

                String lDefaultValue = AbstractConfiguration.findDefaultValue(lConfiguration, lMethodName);
                if (lDefaultValue != null) {
                    lTextField.textProperty().setValue(lDefaultValue);
                } else {
                    lTextField.textProperty().setValue("");
                }

            }

        }

        // INITIALIZE CHECK BOXES

        // Parse configuration class for check boxes
        for (AbstractConfiguration lConfiguration : checkBoxes.keySet()) {

            // Retrieve method name prefixes for this configuration class
            for (String lMethodName : checkBoxes.get(lConfiguration).keySet()) {

                // Retrieve check box for this configuration class and get method name
                CheckBox lCheckBox = checkBoxes.get(lConfiguration).get(lMethodName);

                Boolean lDefaultValue = Boolean.valueOf(AbstractConfiguration.findDefaultValue(lConfiguration, lMethodName));
                lCheckBox.setSelected(lDefaultValue);

            }

        }

    }

}
