package cclerc.cat;

import cclerc.cat.Configuration.*;
import cclerc.cat.model.Alarm;
import cclerc.cat.view.*;
import cclerc.services.*;
import cclerc.services.Network;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import org.apache.commons.cli.*;
import org.jdom2.JDOMException;

import javax.rmi.CORBA.Util;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.util.*;

import static java.lang.System.exit;

/**
 * Main application class
 */
public class Cat extends Application {

    // Application parameters
    private static String[] applicationArguments;
    private static boolean displayGraphicalInterface = true;
    private static boolean initializationInProgress = true;
    private static Cat cat;

    // Configuration
    private static String configurationFilePath;

    // Graphical components
    private static Stage mainStage;     // Main stage
    private static BorderPane rootPane; // Layout of the scene

    // Controllers
    private static RootLayout rootController;
    private static CatView catController;
    private static ConfigurationDialog configurationDialogController;
    //private static Map<EnumTypes.AddressType, Map<EnumTypes.InterfaceType, MonitoringJobView>> monitoringJobControllers = new HashMap<>();
    private static Map<EnumTypes.AddressType, List<MonitoringJobView>> monitoringJobControllers = new HashMap<>();

    // Language settings

    // Network interfaces
    private static HashMap<String, NetworkInterface> networkInterfaceList;

    // STATIC

    /**
     * Gets cat instance
     * @return cat instance
     */
    public static Cat getInstance() {
        return cat;
    }

    // PRIVATE

    /**
     * Start a new monitoring job
     * @param aInAddressType           Configured address type of the monitored server, should be the same as the actual address type of the monitored server
     * @param aInNetworkInterfaceIndex Index in the network interfaces configuration of the network interface to be used to monitor the server
     *
     */
    private void startMonitoringJob(EnumTypes.AddressType aInAddressType, int aInNetworkInterfaceIndex) {

        try {

            // Create the monitoring job
            MonitoringJob lMonitoringJob =
                    new MonitoringJob(aInAddressType, aInNetworkInterfaceIndex, networkInterfaceList,
                                      monitoringJobControllers.get(aInAddressType).get(aInNetworkInterfaceIndex), catController);

            // Create and start the monitoring thread
            Thread lThread = new Thread(lMonitoringJob);
            lThread.start();

        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.startJob"), Utilities.getStackTrace(e)));
        }

    }

    /**
     * Start the monitoring jobs
     */
    private void startMonitoringJobs() {

        MonitoringJobView.setCat(this);

        // Start a job for each server of the configuration
        if (Configuration.getCurrentConfiguration().getMonitoringConfiguration() != null) {

            // Start a job for each defined network interface
            for (int lNetworkInterfaceIndex = 0;
                 lNetworkInterfaceIndex < Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().getNetworkInterfaceConfigurations().size();
                 lNetworkInterfaceIndex++) {

                // Start wan servers
                if (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getWan() != null) {
                    startMonitoringJob(EnumTypes.AddressType.WAN, lNetworkInterfaceIndex);
                    Utilities.sleep(Constants.DELAY_BETWEEN_TWO_JOBS);
                }

                // Start lan servers
                if (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getLan() != null) {
                    startMonitoringJob(EnumTypes.AddressType.LAN, lNetworkInterfaceIndex);
                    Utilities.sleep(Constants.DELAY_BETWEEN_TWO_JOBS);
                }

            }

        }

    }

    /**
     * Displays Cat overview pane in root pane.
     */
    private void displayCatView() {

        try {

            // Load Cat overview pane and controller from xml file
            FXMLLoader lLoader = new FXMLLoader();
            lLoader.setLocation(Cat.class.getResource("view/CatView.fxml"));
            lLoader.setResources(Display.getViewResourceBundle());
            GridPane lCatViewPane = lLoader.load();
            catController = lLoader.getController();

            // Initialize general information
            catController.refreshJobsCount();
            catController.refreshPingsCount();
            catController.refreshConnectionsLostCount();

            // Display general pause image
            catController.setGeneralPlayPauseButtonImageView(States.getInstance().getBooleanValue(catController.BuildStatePropertyName(Constants.PAUSE_STATE), true));

            // Display general email image
            catController.setGeneralEmailButtonImageView(States.getInstance().getBooleanValue(catController.BuildStatePropertyName(Constants.SEND_MAIL_STATE), true));
            if (Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations().size() == 0 ||
                Configuration.getCurrentConfiguration().getEmailConfiguration().getRecipientList().isEmpty()) {
                catController.disableEmailButtons();
            }

            // Initialize alarms tables
            catController.initializeAlarmsTables();

            // Prepare alarms
            Alarm.loadAlarmDictionary();
            catController.selectActiveAlarms();

            // Set Cat overview pane into the center of root layout.
            rootPane.setCenter(lCatViewPane);

        } catch (IOException|JDOMException e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.displayCatView"), Utilities.getStackTrace(e)));
        }

    }

    /**
     * Displays monitoring job view in Cat overview
     */
    private void displayMonitoringJobView() {

        // Parse interface types ordered by priority
        int lPriority = 0;
        for (NetworkInterfaceConfiguration lNetworkInterfaceConfiguration:
             Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().getNetworkInterfaceConfigurations()) {

            lPriority++;
            EnumTypes.InterfaceType lInterfaceType = Network.getInterfaceType(lNetworkInterfaceConfiguration.getName());

            // Parse address types
            for (EnumTypes.AddressType lAddressType : EnumTypes.AddressType.values()) {

                try {

                    // Load monitoring job view controller
                    FXMLLoader lLoader = new FXMLLoader();
                    lLoader.setLocation(Cat.class.getResource("view/MonitoringJobView.fxml"));
                    lLoader.setResources(Display.getViewResourceBundle());
                    GridPane lMonitoringJobPane = lLoader.load();
                    MonitoringJobView lMonitoringJobController = lLoader.getController();

                    // Create monitoring job view
                    List<MonitoringJobView> lMonitoringJobViews;
                    if (monitoringJobControllers.containsKey(lAddressType)) {
                        lMonitoringJobViews = monitoringJobControllers.get(lAddressType);
                    } else {
                        lMonitoringJobViews = new ArrayList<>();
                        monitoringJobControllers.put(lAddressType, lMonitoringJobViews);
                    }
                    lMonitoringJobViews.add(lMonitoringJobController);
                    catController.addMonitoringJobView(lMonitoringJobPane, lAddressType, lInterfaceType, lPriority);

                    // configure ping line filters check boxes
                    catController.configurePingLineFilterCheckBox(lNetworkInterfaceConfiguration.getName(), lPriority);

                    // Set monitoring job view properties
                    lMonitoringJobController.setAddressType(lAddressType);
                    lMonitoringJobController.setInterfaceType(lInterfaceType);
                    lMonitoringJobController.setPriority(lPriority);
                    lMonitoringJobController.setActiveHostSliderPreferences();
                    lMonitoringJobController.installToolTips();

                } catch (Exception e) {
                    Display.getLogger().error(
                            String.format(Display.getMessagesResourceBundle().getString("log.cat.error.displayMonitoringJobView"),
                                          lAddressType.toString(), lInterfaceType.toString(), Utilities.getStackTrace(e)));
                }

            }

        }

        // Remove unused monitoring job views
        catController.removeUnusedMonitoringJobView(
                Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkInterfacesConfiguration().getNetworkInterfaceConfigurations().size());

    }

    /**
     * Creates the scene with the root layout in the main stage and display it
     */
    private void createSceneWithRootLayout() {

        try {

            // Load root layout from fxml file
            FXMLLoader lLoader = new FXMLLoader();
            lLoader.setLocation(Cat.class.getResource("view/RootLayout.fxml"));
            lLoader.setResources(Display.getViewResourceBundle());
            rootPane = lLoader.load();
            rootController = lLoader.getController();
            rootController.setPreferences();

            // Create a scene containing the root pane
            Scene lScene = new Scene(rootPane);
            lScene.getStylesheets().add("resources/css/view.css");

            // Put the scene in the stage and display it
            mainStage.setScene(lScene);
            mainStage.setOnCloseRequest(confirmCloseCatEventHandler);
            mainStage.getIcons().add(Constants.APPLICATION_IMAGE);
            mainStage.show();


            // Get current screen of the stage
            ObservableList<Screen> screens = Screen.getScreensForRectangle(new Rectangle2D(mainStage.getX(), mainStage.getY(), mainStage.getWidth(), mainStage.getHeight()));

            // Change stage properties
            Rectangle2D bounds = screens.get(0).getVisualBounds();
            mainStage.setX(bounds.getMinX());
            mainStage.setY(bounds.getMinY());
            mainStage.setWidth(bounds.getWidth()*2/3);
            mainStage.setHeight(bounds.getHeight());

        } catch (IOException e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.createSceneWithRootLayout"), Utilities.getStackTrace(e)));
        }

    }

    /**
     * Creates the dialog stages depending on CAT main window
     */
    private void createDialogs() {

        // CONFIGURATION DIALOG
        configurationDialogController = ConfigurationDialog.getInstance(mainStage);
        rootController.setConfigurationMonitoringJobsDialogController(configurationDialogController);

        // ABOUT DIALOG
        rootController.setAboutDialogController(AboutDialog.getInstance(mainStage));

        // ALARM DETAILS DIALOG
        catController.setAlarmDetailsDialogController(AlarmDetailsDialog.getInstance(mainStage));

    }

    /**
     * Opens confirmation dialog box on application close when configuration is not modified
     * @param aInMessage Message to display in the alert box
     * @return true if exit is confirmed, false otherwise
     */
    private static boolean confirmExit(String aInMessage) {

        // Prepare confirmation dialog box
        Alert lConfirmation = new Alert(Alert.AlertType.CONFIRMATION, Display.getViewResourceBundle().getString(aInMessage), ButtonType.YES, ButtonType.NO);
        lConfirmation.setHeaderText(Display.getViewResourceBundle().getString("confirm.exit.title"));
        lConfirmation.initModality(Modality.APPLICATION_MODAL);
        lConfirmation.initOwner(mainStage);

        // Display confirmation dialog box
        Optional<ButtonType> lResponse = lConfirmation.showAndWait();

        // OK is pressed
        return lResponse.isPresent() && lResponse.get().equals(ButtonType.YES);
    }

    public static boolean confirmExit() {
        return confirmExit("confirm.exitConfigurationSaved.question");
    }

        /**
         * Opens confirmation dialog box on application close when configuration is modified
         * @return true if exit is confirmed, false otherwise
         */
    public static boolean confirmSaveAndExit() {

        // Prepare confirmation dialog box
        Alert lConfirmation = new Alert(Alert.AlertType.CONFIRMATION, Display.getViewResourceBundle().getString("confirm.exitConfigurationNotSaved.question"),
                                        ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        lConfirmation.setHeaderText(Display.getViewResourceBundle().getString("confirm.exit.title"));
        lConfirmation.initModality(Modality.APPLICATION_MODAL);
        lConfirmation.initOwner(mainStage);

        // Display confirmation dialog box
        Optional<ButtonType> lResponse = lConfirmation.showAndWait();

        if (lResponse.isPresent() && lResponse.get().equals(ButtonType.NO)) {
            // NO is pressed
            return true;
        } else if (lResponse.isPresent() && lResponse.get().equals(ButtonType.YES)) {
            // NO is pressed
            Configuration.getCurrentConfiguration().save();
            return true;
        } else {
            // Cancel is pressed, consume the event so that the user interface doesn't exit
            return false;
        }
    }

    /**
     * Manages close event
     */
    private EventHandler<WindowEvent> confirmCloseCatEventHandler = event -> {

        if (!Configuration.getCurrentConfiguration().isSameAs(Configuration.getInitialConfiguration())) {
            if (confirmSaveAndExit()) {
                end();
            } else {
                event.consume();
            }
        } else {
            if (confirmExit()) {
                end();
            } else {
                event.consume();
            }
        }

    };

    /**
     * Ends the program
     */
    public static void end() {

        Platform.exit();
        Display.getLogger().trace(Display.getMessagesResourceBundle().getString("log.cat.stop"));
        exit(Constants.EXIT_OK);

    }

    // GETTERS

    /**
     * Gets the cat overview controller
     * @return Controller
     */
    public CatView getController() {
        return catController;
    }

    /**
     * Gets the cat overview main stage
     * @return Main stage
     */
    public Stage getMainStage() {
        return mainStage;
    }

    // METHODS

    /**
     * Initializes and displays the graphical interface
     *
     * @param aInMainStage Main state to be displayed
     */
    @Override
    public void start(Stage aInMainStage) throws Exception {

        // Back reference to this, must not be moved
        cat = this;

        // Memorize and set title to the main stage
        mainStage = aInMainStage;
        mainStage.setTitle(Display.getAboutResourceBundle().getString("product.name"));

        // Display the user interface if required
        if (displayGraphicalInterface) {

            // Create the scene with the root layout
            createSceneWithRootLayout();

            // Tune tooltip behavior
            Utilities.updateTooltipBehavior(Constants.TOOLTIP_OPEN_DELAY, Constants.TOOLTIP_VISIBLE_DURATION, Constants.TOOLTIP_CLOSE_DELAY, true);

            // Display overview
            displayCatView();

            // Add monitoring job views
            displayMonitoringJobView();

            // Create the dialogs
            createDialogs();

        }

        // Build network interfaces list
        networkInterfaceList = Network.buildNetworkInterfaceList();

        // No configuration file has been provided, default one is used, force user to customize it
        if (configurationFilePath == null) {

            configurationDialogController.show(true);

            // User has cancelled the configuration, then exit
            configurationFilePath = Configuration.getCurrentConfiguration().getFile();
            if (configurationFilePath == null) end();

            // Finalize new configuration
            Configuration.addConfigurationChangeObserver();
            Preferences.getInstance().saveValue("configurationFile", configurationFilePath);

        }

        // Initialize SMTP servers
        for (SmtpServerConfiguration lSmtpServerConfiguration :
                Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations()) {
            Email.addSmtpServer(
                    lSmtpServerConfiguration.getName(), lSmtpServerConfiguration.getTlsMode(), String.valueOf(lSmtpServerConfiguration.getPort()),
                    lSmtpServerConfiguration.getUser(), lSmtpServerConfiguration.getLogin(), Security.getInstance().decrypt(lSmtpServerConfiguration.getPassword()),
                    String.valueOf(lSmtpServerConfiguration.getConnectionTimeout()), String.valueOf(lSmtpServerConfiguration.getTimeout()));
        }

        // Start the monitoring jobs
        startMonitoringJobs();

        // Check state of general pause and email buttons
        checkPauseState();
        if (Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations().size() != 0 &&
                !Configuration.getCurrentConfiguration().getEmailConfiguration().getRecipientList().isEmpty())
            checkEmailState();

        // End of initialization
        initializationInProgress = false;

    }

    /**
     * Changes the play / pause button state depending on the state of related summaries
     */
    public void checkPauseState() {

        // Check if all summary jobs are paused or not paused
        boolean lAllPaused = true;
        boolean lAllNotPaused = true;
        for (MonitoringJob lMonitoringjob : MonitoringJob.getMonitoringJobs()) {
            if (!lMonitoringjob.isPaused()) {
                lAllPaused = false;
            } else {
                lAllNotPaused = false;
            }
        }
        if (catController.isButtonGeneralPlayPausedDisplayed()) lAllPaused = false;
        if (!catController.isButtonGeneralPlayPausedDisplayed()) lAllNotPaused = false;

        if (displayGraphicalInterface) {
            if (MonitoringJob.getMonitoringJobs().size() == 0) {
                // If there's no more monitoring job, remove the Play / Pause button
                catController.removePlayPauseButtonImageView();
            } else if (lAllPaused) {
                // If all jobs of the current connectionTypeIdentifier are paused, set the image button to Play
                catController.setPlayPauseButtonImageView(false);
            } else if (lAllNotPaused) {
                // If all jobs of the current connectionTypeIdentifier are not paused, set the image button to Pause
                catController.setPlayPauseButtonImageView(true);
            } else {
                // If some jobs are paused, other are not, set the image button to play
                catController.setPlayPauseButtonImageView(false);
            }
        }

    }

    /**
     * Switches email button state depending on the state of related summaries
     */
    public void checkEmailState() {

        if (displayGraphicalInterface) {

            // Check if all summary jobs have emails enabled or not
            boolean lAllEnabled = true;
            boolean lAllDisabled = true;
            for (MonitoringJob lMonitoringJob : MonitoringJob.getMonitoringJobs()) {
                if (!lMonitoringJob.isEmailEnabled()) {
                    lAllEnabled = false;
                } else {
                    lAllDisabled = false;
                }
            }
            if (catController.isButtonGeneralEmailEnabled()) lAllDisabled = false;
            if (!catController.isButtonGeneralEmailEnabled()) lAllEnabled = false;

            if (MonitoringJob.getMonitoringJobs().size() == 0) {
                // If there's no more monitoring job, remove the email button
                catController.removeEmailButtonImageView();
            } else if (lAllEnabled) {
                // If all jobs have email audibleEnabled, set the image button to email
                catController.setEmailButtonImageView(true);
            } else if (lAllDisabled) {
                // If all jobs have email disabled, set the image button to no email
                catController.setEmailButtonImageView(false);
            } else {
                // If some jobs have email audibleEnabled, other disabled, set the image button to no email
                catController.setEmailButtonImageView(false);
            }
        }

    }

    /**
     * Indicates if graphical interface must be displayed
     * @return Display graphical interface indicator
     */
    public boolean displayGraphicalInterface() {
        return displayGraphicalInterface;
    }

    /**
     * Indicates if initialization is in progress
     * @return Initialization in progress indicator
     */
    public boolean isInitializationInProgress() {
        return initializationInProgress;
    }

    // MAIN
    public static void main(String[] args) {

        applicationArguments = args;

        // Default language settings
        Locale lCurrentLocale;

        // Analyze command line
        CommandLine lCommandLine;
        Options lOptions = new Options();

        Option lLocaleOption = new Option("loc", "locale", true, Display.getMessagesResourceBundle().getString("help.locale"));
        lLocaleOption.setRequired(false);
        lOptions.addOption(lLocaleOption);

        Option lDisplayGraphicalInterfaceOption = new Option("nogui", "noGraphicalInterface", false, Display.getMessagesResourceBundle().getString("help.nogui"));
        lDisplayGraphicalInterfaceOption.setRequired(false);
        lOptions.addOption(lDisplayGraphicalInterfaceOption);

        Option lConfigurationFileOption = new Option("config", "configurationFile", true, Display.getMessagesResourceBundle().getString("help.config"));
        lConfigurationFileOption.setRequired(false);
        lOptions.addOption(lConfigurationFileOption);

        Option lLogDirectoryOption = new Option("log", "logDirectory", true, Display.getMessagesResourceBundle().getString("help.log"));
        lLogDirectoryOption.setRequired(false);
        lOptions.addOption(lLogDirectoryOption);

        CommandLineParser lParser = new DefaultParser();
        HelpFormatter lFormatter = new HelpFormatter();

        try {
            lCommandLine = lParser.parse(lOptions, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage() + '\n');
            lFormatter.printHelp(Display.getAboutResourceBundle().getString("product.name"), lOptions);
            exit(Constants.EXIT_INVALID_ARGUMENT);
            return;
        }

        // Start
        String lArgs = "";
        for (String lArg : args) {
            lArgs = lArgs.concat(lArg + ' ');
        }

        // Get log directory
        if (lCommandLine.hasOption("log")) {
            // Check if directory exists
            if (new File(lCommandLine.getOptionValue("log")).exists()) {
                System.setProperty("log4j.saveDirectory", lCommandLine.getOptionValue("log"));
            } else {
                System.err.println(String.format("Invalid logs directory %s, please create it first", lCommandLine.getOptionValue("log")));
                exit(Constants.EXIT_INVALID_ARGUMENT);
                return;
            }
        } else {
            System.setProperty("log4j.saveDirectory", "logs");
        }

        // Get configuration file name
        if (lCommandLine.hasOption("config")) {
            configurationFilePath = lCommandLine.getOptionValue("config");
            // Configuration file path is relative
            if ((!configurationFilePath.substring(0, 1).equals("/")) && (!configurationFilePath.substring(1, 2).equals(":"))) {
                configurationFilePath = new File("").getAbsolutePath() + '/' + configurationFilePath;
            }
        }

        // Check if graphical interface must be displayed
        displayGraphicalInterface = !lCommandLine.hasOption("nogui");

        // Load language bundles
        if (lCommandLine.hasOption("loc")) {
            lCurrentLocale = new Locale(lCommandLine.getOptionValue("loc"));
            LocaleUtilities.getInstance().setCurrentLocale(lCurrentLocale);
            Display.resetMessagesResourceBundle();
        }

        // Check Java version
        if (!Utilities.isJavaVersionValid(Display.getAboutResourceBundle().getString("product.java.version"))) {
            Display.getLogger().fatal(String.format(Display.getMessagesResourceBundle().getString(
                    "log.cat.error.incorrectJavaVersion"), System.getProperty("java.version"), Display.getAboutResourceBundle().getString("product.java.version")));
            exit(Constants.EXIT_INVALID_JAVA_VERSION);
        }

        // Name thread
        Thread.currentThread().setName(String.format("Main %s Thread", Thread.currentThread().getId()));

        Display.getLogger().trace(String.format(Display.getMessagesResourceBundle().getString("log.cat.start"), lArgs));
        Display.getLogger().info(String.format(Display.getMessagesResourceBundle().getString("log.cat.logFileName"), Utilities.getLoggerFileName(Display.getLogger())));
        try {

            // Load configuration
            if (configurationFilePath == null) {
                configurationFilePath = Preferences.getInstance().getValue("configurationFile");
            }
            if (configurationFilePath == null) {
                Configuration.createDefaultConfiguration();
            } else {
                Configuration.loadConfiguration(configurationFilePath);
                Preferences.getInstance().saveValue("configurationFile", configurationFilePath);
            }

            // Initialize interface and start monitoring
            launch(args);

        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.incorrectConfigurationFile"), configurationFilePath));
            Display.getLogger().trace(Display.getMessagesResourceBundle().getString("log.cat.stop"));
            Display.logUnexpectedError(e);
            exit(Constants.EXIT_OK);
        }

    }

    public static void restart() {

        StringBuilder lCommand = new StringBuilder();
        lCommand.append(System.getProperty("java.home"));
        if (!lCommand.toString().contains(File.separator + "bin")) lCommand.append(File.separator).append("bin");
        lCommand.append(File.separator).append("java");
        StringBuilder lArgs = new StringBuilder();
        for (String lJvmArgs : ManagementFactory.getRuntimeMXBean().getInputArguments()) lArgs.append(lJvmArgs).append(" ");

        lArgs.append("-cp ").append(ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");
        lArgs.append(Cat.class.getName()).append(" ");
        for (String arg : applicationArguments)  lArgs.append(arg).append(" ");

        try {
            // Restart only if program is launched from jar, otherwise display an alert
            if (lArgs.toString().contains("cat.jar")) {
                if (confirmExit("confirm.restart.question")) {
                    Process lProcess = Runtime.getRuntime().exec(lCommand.toString() + " " + lArgs.toString());
                    System.exit(0);
                }
            } else {
                if (confirmExit("confirm.restart.question")) end();
            }
        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }
    }

}
