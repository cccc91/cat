package cclerc.cat.view;

import cclerc.cat.Cat;
import cclerc.cat.MonitoringJob;
import cclerc.services.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;

public class MonitoringJobView {

    // Class properties
    private static Cat cat;

    // Instance properties
    private MonitoringJob monitoringJob;
    private EnumTypes.AddressType addressType;
    private EnumTypes.InterfaceType interfaceType;
    private int priority;
    private EnumTypes.HostState state = EnumTypes.HostState.REACHABLE;

    // Display management
    private boolean firstDisplay = true;
    private long pingsCount = 0;
    private long consecutiveLostPingsCount = 0;
    private long lostPingsCount = 0;

    // Pause management
    private boolean isButtonPauseDisplayed;

    // Email management
    private boolean isButtonEmailEnabled;

    // Blinking text
    private Timeline blinker ;

    // FXML
    @FXML private ImageView addressTypeStateImageView;
    @FXML private Label hostNameLabel;
    @FXML private Label hostIpLabel;
    @FXML private Label pingsCountLabel;
    @FXML private Label consecutiveLostPingsCountLabel;
    @FXML private Label lostPingsCountLabel;
    @FXML private Label roundTripLabel;
    @FXML private Label roundTripStatsLabel;
    @FXML private ImageView clearConsoleButtonImageView;
    @FXML private ImageView pauseButtonImageView;
    @FXML private ImageView emailButtonImageView;
    @FXML private Label lostConnectionsCountLabel;
    @FXML private Label lastTwoLostConnectionsIntervalLabel;
    @FXML private Label ongoingLossDurationTextLabel;
    @FXML private Label ongoingLossDurationLabel;
    @FXML private Label averageLossDurationLabel;
    @FXML private Label totalLossDurationLabel;
    @FXML private ScrollPane detailsScrollPane;
    @FXML private TextFlow detailsTextFlow;
    @FXML private Slider activeServerSlider;

    @FXML private void initialize() {

        hostNameLabel.setTextFill(Color.web("green"));
        hostIpLabel.setTextFill(Color.web("green"));

        blinker = new Timeline(
            new KeyFrame(
                javafx.util.Duration.seconds(1.0),
                e -> {
                    hostNameLabel.pseudoClassStateChanged(PseudoClass.getPseudoClass("blinkText"), true);
                    hostIpLabel.pseudoClassStateChanged(PseudoClass.getPseudoClass("blinkText"), true);
                }
            ),
            new KeyFrame(
                javafx.util.Duration.seconds(2.0),
                e -> {
                    hostNameLabel.pseudoClassStateChanged(PseudoClass.getPseudoClass("blinkText"), false);
                    hostIpLabel.pseudoClassStateChanged(PseudoClass.getPseudoClass("blinkText"), false);
                })
        );
        blinker.setCycleCount(Animation.INDEFINITE);

        // Tooltips
        Tooltip lClearConsoleTooltip = new Tooltip(Display.getViewResourceBundle().getString("monitoringJob.tooltip.clearConsole"));
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE))
            Tooltip.install(clearConsoleButtonImageView, lClearConsoleTooltip);

    }

    // LISTENERS

    private ListChangeListener detailsScrollPaneChangeListener = (change) -> detailsScrollPane.setVvalue(1.0d);

    private ChangeListener<Number> activeServerSliderChangeListener = (obs, oldValue, newValue) -> {
        if (MonitoringJob.getMonitoringJobsByType().get(addressType) != null && MonitoringJob.getMonitoringJobsByType().get(addressType).get(interfaceType) != null) {
            MonitoringJob.getMonitoringJobsByType().get(addressType).get(interfaceType).changeActiveServer(newValue.intValue(), "manual");
        }
    };

    // PRIVATE METHODS

    /**
     * Checks pause state of summary views and cat overview
     */
    private void checkOtherViewsPauseState() {

        // Change state of general pause button
        cat.checkPauseState();

    }

    /**
     * Checks email state of summary views and cat overview
     */
    private void checkOtherViewsEmailState() {

        // Change state of general email button
        cat.checkEmailState();

    }

    @FXML private void clearConsole() {

        // Prepare confirmation dialog box
        Alert lConfirmation = new Alert(Alert.AlertType.CONFIRMATION, Display.getViewResourceBundle().getString("confirm.clearConsole.question"), ButtonType.YES, ButtonType.NO);
        lConfirmation.setHeaderText(Display.getViewResourceBundle().getString("confirm.clearConsole.title"));
        lConfirmation.initModality(Modality.APPLICATION_MODAL);

        // Display confirmation dialog box
        Optional<ButtonType> lResponse = lConfirmation.showAndWait();

        // OK is pressed
        if (lResponse.isPresent() && lResponse.get().equals(ButtonType.YES)) {
            monitoringJob.clearConsole();
        }

    }

    // GETTERS

    /**
     * Gets back reference to Cat main class
     */
    public Cat getCat() {
        return cat;
    }

    // SETTERS

    /**
     * Sets a back reference to Cat main class
     * @param aInCat Cat main class
     */
    public static void setCat(Cat aInCat) {
        cat = aInCat;
    }

    /**
     * Adds a monitoring job attached to current view
     * @param aInMonitoringJob Monitoring job
     */
    public void setMonitoringJob(MonitoringJob aInMonitoringJob) {
        monitoringJob = aInMonitoringJob;
        cat.getController().addMonitoringJob();
    }

    public void resetFirstDisplay() {
        firstDisplay = true;
    }

    /**
     * Sets address type
     * @param aInAddressType Address type
     */
    public void setAddressType(EnumTypes.AddressType aInAddressType) {
        addressType = aInAddressType;
        setAddressTypeStateImageView(true);
    }

    /**
     * Sets interface type
     * @param aInInterfaceType Interface type
     */
    public void setInterfaceType(EnumTypes.InterfaceType aInInterfaceType) {
        interfaceType = aInInterfaceType;
        setInterfaceTypeImageView(true);
    }

    /**
     * Sets priority
     * @param aInPriority Interface priority
     */
    public void setPriority(int aInPriority) {
        priority = aInPriority;
    }

    // FXML SETTERS

    /**
     * Sets host name label
     * @param aInHostName host name
     */
    public void setHostNameLabel(String aInHostName) {
        hostNameLabel.setText(aInHostName);
    }

    /**
     * Sets hostname basic tooltip
     * @param aInMaxRetries  Job maximum number of retries
     * @param aInPeriod      Job polling period
     * @param aInTimeout     Job ping timeout
     * @param aInRetriesCount Job retries count
     */
    public void setHostTooltip(int aInMaxRetries, int aInPeriod, int aInTimeout, long aInRetriesCount) {
        String lTooltipText = String.format(Display.getViewResourceBundle().getString("monitoringJob.tooltip.hostnameBasic"), aInMaxRetries, aInPeriod, aInTimeout);
        Tooltip lTooltip = (aInRetriesCount != 0) ?
                new Tooltip(lTooltipText + String.format(Display.getViewResourceBundle().getString("monitoringJob.tooltip.hostnameExtended"), aInRetriesCount)) :
                new Tooltip(lTooltipText);
        if (Preferences.getInstance().getBooleanValue("enableDetailTooltip", Constants.DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE)) {
            Tooltip.install(hostNameLabel, lTooltip);
            Tooltip.install(hostIpLabel, lTooltip);
        }
    }

    /**
     * Sets host IP label
     * @param aInHostIp host IP
     */
    public void setHostIpLabel(String aInHostIp) {
        hostIpLabel.setText(aInHostIp);
    }

    /**
     * Sets pings count
     * @param aInPingsCount                Pings count
     * @param aInConsecutiveLostPingsCount Consecutive lost pings count
     * @param aInLostPingsCount            Total lost pings count
     */
    public void setPingsCount(long aInPingsCount, long aInConsecutiveLostPingsCount, long aInLostPingsCount) {
        pingsCount = aInPingsCount;
        consecutiveLostPingsCount = aInConsecutiveLostPingsCount;
        lostPingsCount = aInLostPingsCount;
        pingsCountLabel.setText(String.valueOf(pingsCount));
        consecutiveLostPingsCountLabel.setText(String.valueOf(consecutiveLostPingsCount));
        lostPingsCountLabel.setText(String .valueOf(lostPingsCount));
    }

    /**
     * Sets the interval between the two last lost connections
     * @param aInLastTwoLostConnectionInterval Interval (ms) between the two last lost connections
     */
    public void setLastTwoLostConnectionsIntervalLabel(long aInLastTwoLostConnectionInterval) {
        lastTwoLostConnectionsIntervalLabel.setText(Utilities.formatDuration(aInLastTwoLostConnectionInterval, 0));
    }

    /**
     * Sets round trip
     * @param aInRoundTrip  Round trip (ms)
     */
    public void setRoundTrip(long aInRoundTrip) {
        if (aInRoundTrip == Integer.MAX_VALUE) {
            roundTripLabel.setText("-");
        } else {
            roundTripLabel.setText(String.format("%d",aInRoundTrip));
        }
    }

    /**
     * Sets min, max and average round trip
     * @param aInMinRoundTrip  Min round trip (ms)
     * @param aInMaxRoundTrip Max round trip (ms)
     * @param aInAvgRoundTrip Avg round trip (ms)
     */
    public void setRoundTripStats(long aInMinRoundTrip, long aInMaxRoundTrip, double aInAvgRoundTrip) {
        if (aInMinRoundTrip == Integer.MAX_VALUE) {
            roundTripStatsLabel.setText("-/-/-");
        } else {
            roundTripStatsLabel.setText(String.format("%d/%d/%.2f", aInMinRoundTrip, aInMaxRoundTrip, aInAvgRoundTrip));
        }
    }

    /**
     * Sets lost connections count
     * @param aInLostConnectionsCount Lost connections count
     */
    public void setLostConnectionsCount(long aInLostConnectionsCount) {
        lostConnectionsCountLabel.setText(Long.toString(aInLostConnectionsCount));
    }

    /**
     * Sets connection loss durations
     * @param aInOngoingLossDuration On-going loss duration
     * @param aInTotalLossDuration   Total loss duration
     * @param aInLostConnectionCount Lost connections count
     */
    public void setLostConnectionDuration(long aInOngoingLossDuration, long aInTotalLossDuration, long aInLostConnectionCount) {

        if (aInLostConnectionCount == 0) {
            ongoingLossDurationLabel.setText("-");
            totalLossDurationLabel.setText("-");
            averageLossDurationLabel.setText("-");
        } else {
            ongoingLossDurationLabel.setText(Utilities.formatDuration(aInOngoingLossDuration, 0));
            totalLossDurationLabel.setText(Utilities.formatDuration(aInTotalLossDuration, 0));
            averageLossDurationLabel.setText(Utilities.formatDuration((aInTotalLossDuration / aInLostConnectionCount), 0));
        }

    }

    /**
     * Sets active host slider preferences
     */
    public void setActiveHostSliderPreferences() {

        activeServerSlider.valueProperty().addListener(activeServerSliderChangeListener);

        // Set labels to the slider
        activeServerSlider.setLabelFormatter(new StringConverter<Double>() {

            @Override
            public String toString(Double n) {
                if (n == 0d) return Display.getViewResourceBundle().getString("monitoringJob.preferredHostSlider.preferred");
                if (n == 1d) return Display.getViewResourceBundle().getString("monitoringJob.preferredHostSlider.backup");
                return "";
            }

            @Override
            public Double fromString(String s) {
                if (s.equals(Display.getViewResourceBundle().getString("monitoringJob.preferredHostSlider.preferred"))) return 0d;
                else return 1d;
            }

        });

    }

    /**
     * Sets active server slider value
     * @param aInValue Slider value
     */
    public void setActiveServerSliderValue(int aInValue) {

        // Set value
        activeServerSlider.valueProperty().removeListener(activeServerSliderChangeListener);
        activeServerSlider.setValue(aInValue);
        activeServerSlider.valueProperty().addListener(activeServerSliderChangeListener);
    }

    // PUBLIC METHODS

    /**
     * Enables active server slider
     */
    public void enableActiveServerSlider() {
        activeServerSlider.setDisable(false);
    }

    /**
     * Prints a message with the required level in the monitoring job text flow
     * @param aInMessage      Message to display
     */
    public void printMessage(Message aInMessage) {
        aInMessage.println(detailsTextFlow);
    }

    /**
     * Removes last from the monitoring job text flow
     */
    public void removeLastMessage() {
        detailsTextFlow.getChildren().remove(detailsTextFlow.getChildren().size() - 1);
    }

    /**
     * Replaces monitoring job text flow content with current list of messages
     * @param aInMessages Messages
     */
    public void replaceMessages(List<Message> aInMessages) {
        // In case of new message on non active tab - except at job start-up and reset, display modification indicator
        if (aInMessages.size() != detailsTextFlow.getChildren().size() && ! firstDisplay)
            cat.getController().changeMonitoringJobTabModificationIndicator(addressType, priority, aInMessages.size() - detailsTextFlow.getChildren().size());
        firstDisplay = false;

        clearAllMessages();
        for (Message lMessage: aInMessages) {
            printMessage(lMessage);
        }
    }

    /**
     * Clears all messages from the monitoring job text flow
     */
    public void clearAllMessages() {
        detailsTextFlow.getChildren().clear();
    }

    /**
     * Sets listeners on detail scroll text and detailed scroll pane so that text auto scrolls to bottom
     * except if user scrolls to another place than bottom
     */
    public void setDetailsScrollPolicy() {

        // Add listener to detail text flow so that it auto scrolls by default to bottom each time the text changes
        detailsTextFlow.getChildren().removeListener(detailsScrollPaneChangeListener);
        detailsTextFlow.getChildren().addListener(detailsScrollPaneChangeListener);

        // Add listener to detail scroll pane so that auto scroll is enabled or disabled depending on user action
        detailsScrollPane.vvalueProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                if(oldValue.doubleValue() == 1.0d){
                    // If user scrolls to bottom, enable auto scroll to bottom
                    detailsTextFlow.getChildren().removeListener(detailsScrollPaneChangeListener);
                    detailsTextFlow.getChildren().addListener(detailsScrollPaneChangeListener);
                } else {
                    // If user scrolls up, disable auto scroll to bottom
                    detailsTextFlow.getChildren().removeListener(detailsScrollPaneChangeListener);
                }
            }
        );

    }

    /**
     * Changes state if the monitoring job, i.e. change color of host label depending on the job state
     * @param aInState Job state (HOST_REACHABLE, HOST_PING_LOST or HOST_UNREACHABLE)
     */
    public void changeState(EnumTypes.HostState aInState) {

        state = aInState;

        switch (aInState) {
            case REACHABLE:
                ongoingLossDurationTextLabel.setText(Display.getViewResourceBundle().getString("monitoringJob.lastLostConnectionsDurationLabel"));
                ongoingLossDurationTextLabel.setTextFill(Color.web("black"));
                ongoingLossDurationLabel.setTextFill(Color.web("black"));
                hostNameLabel.setTextFill(Color.web("green"));
                hostIpLabel.setTextFill(Color.web("green"));
                break;
            case PING_LOST:
                ongoingLossDurationTextLabel.setText(Display.getViewResourceBundle().getString("monitoringJob.lastLostConnectionsDurationLabel"));
                ongoingLossDurationTextLabel.setTextFill(Color.web("black"));
                ongoingLossDurationLabel.setTextFill(Color.web("black"));
                hostNameLabel.setTextFill(Color.web("orange"));
                hostIpLabel.setTextFill(Color.web("orange"));
                break;
            case UNREACHABLE:
                ongoingLossDurationTextLabel.setText(Display.getViewResourceBundle().getString("monitoringJob.ongoingLostConnectionsDurationLabel"));
                ongoingLossDurationTextLabel.setTextFill(Color.web("red"));
                ongoingLossDurationLabel.setTextFill(Color.web("red"));
                hostNameLabel.setTextFill(Color.web("red"));
                hostIpLabel.setTextFill(Color.web("red"));
                break;
            default:
                ongoingLossDurationTextLabel.setText(Display.getViewResourceBundle().getString("monitoringJob.lastLostConnectionsDurationLabel"));
                ongoingLossDurationTextLabel.setTextFill(Color.web("black"));
                ongoingLossDurationLabel.setTextFill(Color.web("black"));
                hostNameLabel.setTextFill(Color.web("black"));
                hostIpLabel.setTextFill(Color.web("black"));
                break;
        }

    }

    /**
     * Pauses or resumes jobs attached to current view
     */
    public void playPause() throws Exception {

        isButtonPauseDisplayed = !isButtonPauseDisplayed;

        // Pause or resume current job
        if (!isButtonPauseDisplayed) {
            monitoringJob.displayMessage(Display.getViewResourceBundle().getString("monitoringJob.console.pause"), EnumTypes.MessageLevel.INFO);
            monitoringJob.pause();
        } else {
            monitoringJob.displayMessage(Display.getViewResourceBundle().getString("monitoringJob.console.resume"), EnumTypes.MessageLevel.INFO);
            monitoringJob.resume();
        }

        // Check state of general pause button
        cat.checkPauseState();

    }

    /**
     * Set play/pause button to play or pause
     * @param aInPause true if pause must be displayed, false if play must be displayed
     */
    public void setPlayPauseButtonImageView(boolean aInPause) {

        isButtonPauseDisplayed = aInPause;

        Tooltip lTooltip;
        if (aInPause) {
            blinker.pause();
            hostNameLabel.pseudoClassStateChanged(PseudoClass.getPseudoClass("blinkText"), false);
            hostIpLabel.pseudoClassStateChanged(PseudoClass.getPseudoClass("blinkText"), false);
            changeState(state);
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("monitoringJob.tooltip.pause"));
        } else {
            blinker.play();
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("monitoringJob.tooltip.play"));
        }
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) Tooltip.install(pauseButtonImageView, lTooltip);

        // Compute image url and load it
        Image lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + ((aInPause) ? Constants.IMAGE_PAUSE : Constants.IMAGE_PLAY)).toString());
        pauseButtonImageView.setImage(lNewImage);

    }

    /**
     * Switches email of jobs attached to current view
     */
    public void switchEmail() throws Exception {

        isButtonEmailEnabled = !isButtonEmailEnabled;

        // Switches email
        if (!isButtonEmailEnabled) {
            monitoringJob.disableEmail();
        } else {
            monitoringJob.enableEmail();
        }

        // Change state of general email button
        cat.checkEmailState();

    }

    /**
     * Disables email button
     */
    public void disableEmailButton() {
        Image lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_NOEMAIL).toString());
        Tooltip lTooltip = new Tooltip(Display.getViewResourceBundle().getString("monitoringJob.tooltip.emailDisabled"));
        emailButtonImageView.setImage(lNewImage);
        emailButtonImageView.setOpacity(Constants.DISABLED_IMAGE_TRANSPARENCY);
        emailButtonImageView.setOnMouseClicked(null);
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) Tooltip.install(emailButtonImageView, lTooltip);
    }

    /**
     * Sets email button to audibleEnabled or disabled
     * @param aInEmail true if email must be audibleEnabled, false if email must be disabled
     */
    public void setEmailButtonImageView(boolean aInEmail) {

        isButtonEmailEnabled = aInEmail;

        // Compute image url and load it
        Image lNewImage;
        Tooltip lTooltip;
        if (aInEmail) {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_EMAIL).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("monitoringJob.tooltip.email"));
        } else {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_NOEMAIL).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("monitoringJob.tooltip.noemail"));
        }
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) Tooltip.install(emailButtonImageView, lTooltip);

        emailButtonImageView.setImage(lNewImage);

    }

    /**
     * Sets address type state image view
     * @param aInStateOk true if state is ok, false if state is nok
     */
    public void setAddressTypeStateImageView(boolean aInStateOk) {

        Tooltip lTooltip = (aInStateOk) ?
                   new Tooltip(String.format(Display.getViewResourceBundle().getString("monitoringJob.tooltip.addressTypeStateOk"), addressType)) :
                   new Tooltip(String.format(Display.getViewResourceBundle().getString("monitoringJob.tooltip.addressTypeStateNok"), addressType));
        if (Preferences.getInstance().getBooleanValue("enableDetailTooltip", Constants.DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE)) Tooltip.install(addressTypeStateImageView, lTooltip);

        addressTypeStateImageView.setImage(Graphics.getAddressTypeImage(addressType, aInStateOk));

    }

    /**
     * Sets interface type state image view
     * @param aInStateOk true if state is ok, false if state is nok
     */
    public void setInterfaceTypeImageView(boolean aInStateOk) {
        if (cat != null)  cat.getController().setMonitoringJobIcon(addressType, interfaceType, priority, aInStateOk);
    }

    /**
     * Disables current monitoring job tab (when no job can be created)
     */
    public void disableMonitoringJobTab() {
        if (cat != null)  cat.getController().disableMonitoringJobTab(addressType, priority);
    }

    /**
     * Install tooltips on different fields
     */
    public void installToolTips() {

        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) {
            Tooltip.install(activeServerSlider, new Tooltip(Display.getViewResourceBundle().getString("monitoringJob.tooltip.activeServer")));
            // TODO : add tooltips on all fields
        }

    }

}
