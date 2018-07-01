package cclerc.cat.view;

import cclerc.cat.Cat;
import cclerc.cat.GlobalMonitoring;
import cclerc.cat.model.Alarm;
import cclerc.services.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AlarmDetailsDialog {

    private static AlarmDetailsDialog alarmDetailsDialogController;

    // Display management
    private static Stage dialogStage = new Stage();

    // AlarmConfiguration details dialog properties
    private List<Alarm> alarms;
    private boolean acknowledge = true;
//    private CatView catViewController;

    // FXML
    @FXML private Label internalId;
    @FXML private Label idLabel;
    @FXML private Label severityLabel;
    @FXML private Label occurrencesLabel;
    @FXML private Label stateLabel;
    @FXML private Label nameLabel;
    @FXML private Label siteLabel;
    @FXML private Label objectTypeLabel;
    @FXML private Label objectNameLabel;
    @FXML private Label raiseDateLabel;
    @FXML private Label modificationDateLabel;
    @FXML private Label clearDateLabel;
    @FXML private Label typeLabel;
    @FXML private Label probableCauseLabel;
    @FXML private Label additionalInformationLabel;
    @FXML private Label remedialActionLabel;
    @FXML private Button acknowledgeUnAcknowledgeButton;
    @FXML private Button clearButton;
    @FXML private Button closeButton;

    /**
     * Creates instance of AlarmDetailsDialog controller
     * @param aInParentStage Parent stage of alarm details dialog stage
     */
    public static AlarmDetailsDialog getInstance(Stage aInParentStage) {

        FXMLLoader lDialogLoader = new FXMLLoader();

        try {

            // Load the fxml file and create a new stage for the popup dialog.
            lDialogLoader.setLocation(Cat.class.getResource("view/AlarmDetailsDialog.fxml"));
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
            dialogStage.setTitle(Display.getViewResourceBundle().getString("alarmDetailsDialog.title"));
            alarmDetailsDialogController = lDialogLoader.getController();
        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.displayDialog"), Utilities.getStackTrace(e)));
        }

        return alarmDetailsDialogController;

    }

    // SETTERS

    /**
     * Sets the stage of this dialog
     * @param aInDialogStage Stage
     */
    public void setDialogStage(Stage aInDialogStage) {
        dialogStage = aInDialogStage;
    }

    // METHODS

    private String computeNewValue(String aInCurrentValue, String aInNewValue) {
        if (aInCurrentValue != null && !aInCurrentValue.equals(aInNewValue)) {
            aInCurrentValue = "";
        } else if (aInCurrentValue == null || !aInCurrentValue.equals("")) {
            aInCurrentValue = aInNewValue;
        }
        return aInCurrentValue;
    }

    /**
     * Opens alarm details dialog and displays common parameters of the alarms
     * @param aInAlarms Alarms to be displayed
     */
    public void displayAlarm(List<Alarm> aInAlarms) {

        alarms = aInAlarms;
        alarms.sort(new Comparator<Alarm>() {
            @Override
            public int compare(Alarm lAlarm1, Alarm lAlarm2)
            {

                return (lAlarm1.getInternalId().compareTo(lAlarm2.getInternalId()));
            }
        });

        // Fill fields
        String lInternalId = (aInAlarms.size() > 1) ?
                             Display.getViewResourceBundle().getString("alarmDetailsDialog.alarms") : Display.getViewResourceBundle().getString("alarmDetailsDialog.alarm");
        String lSeverity = null, lOccurrences = null, lState = null;
        String lId = null, lName = null, lSite = null, lObjectType = null, lObjectName = null;
        String lRaiseDate = null, lModificationDate = null, lClearDate = null;
        String lType = null, lProbableCause = null, lAdditionalInformation = null, lRemedialAction = null;

        for (Alarm lAlarm: alarms) {
            lInternalId += (lInternalId.contains("#")) ? ", #" + lAlarm.getInternalId() : " #" + lAlarm.getInternalId();
            lSeverity = computeNewValue(lSeverity, lAlarm.severityProperty().get());
            lOccurrences = computeNewValue(lOccurrences, String.valueOf(lAlarm.getOccurrences()));
            lState = computeNewValue(lState, lAlarm.stateProperty().get());
            lId = computeNewValue(lId, String.valueOf(lAlarm.getId()));
            lName = computeNewValue(lName, lAlarm.getName());
            lSite = computeNewValue(lSite, lAlarm.getSite());
            lObjectType = computeNewValue(lObjectType, lAlarm.objectTypeProperty().get());
            lRaiseDate = computeNewValue(lRaiseDate,
                    (lAlarm.getRaiseDate() == null) ? "-" : LocaleUtilities.getInstance().getDateFormat().format(lAlarm.getRaiseDate()) + " " +
                            LocaleUtilities.getInstance().getTimeFormat().format(lAlarm.getRaiseDate().getTime()));
            lModificationDate = computeNewValue(lModificationDate,
                    (lAlarm.getModificationDate() == null) ? "-" : LocaleUtilities.getInstance().getDateFormat().format(lAlarm.getModificationDate()) + " " +
                            LocaleUtilities.getInstance().getTimeFormat().format(lAlarm.getModificationDate().getTime()));
            lClearDate = computeNewValue(lClearDate,
                    (lAlarm.getClearDate() == null) ? "-" : LocaleUtilities.getInstance().getDateFormat().format(lAlarm.getClearDate()) + " " +
                            LocaleUtilities.getInstance().getTimeFormat().format(lAlarm.getClearDate().getTime()));
            lType = computeNewValue(lType, lAlarm.typeProperty().get());
            lProbableCause = computeNewValue(lProbableCause, lAlarm.getProbableCause());
            lAdditionalInformation = computeNewValue(lAdditionalInformation, lAlarm.getAdditionalInformation());
            lRemedialAction = computeNewValue(lRemedialAction, lAlarm.getRemedialAction());
        }

        if (lState == null || lState.equals("") || lState.equals("cleared")) {
            // Mask acknowledge/un-acknowledge buttons for historical alarms (i.e. cleared alarms)
            acknowledgeUnAcknowledgeButton.setVisible(false);
            clearButton.setVisible(false);
        } else {
            // Display acknowledge/un-acknowledge buttons label according to current alarms state
            acknowledgeUnAcknowledgeButton.setVisible(true);
            clearButton.setVisible(true);
            if (lState.equals("acknowledged")) {
                acknowledgeUnAcknowledgeButton.setText(Display.getViewResourceBundle().getString("alarmDetailsDialog.unAcknowledge"));
                acknowledge = false;
            } else {
                acknowledgeUnAcknowledgeButton.setText(Display.getViewResourceBundle().getString("alarmDetailsDialog.acknowledge"));
                acknowledge = true;
            }
        }

        // Display fields
        internalId.setText(lInternalId);
        severityLabel.setText((lSeverity == null || lSeverity.equals("")) ? "-" : Display.getViewResourceBundle().getString("catView.alarmView.severity." + lSeverity));
        switch (lSeverity) {
            case "info":
                severityLabel.setTextFill(Color.BLACK);
                break;
            case "warning":
                severityLabel.setTextFill(Color.BLUE);
                break;
            case "minor":
                severityLabel.setTextFill(Color.YELLOW);
                break;
            case "major":
                severityLabel.setTextFill(Color.ORANGE);
                break;
            case "critical":
                severityLabel.setTextFill(Color.RED);
                break;
            case "unknown":
                severityLabel.setTextFill(Color.BLACK);
                break;
        }
        occurrencesLabel.setText((lOccurrences == null) ? "-" : lOccurrences);
        stateLabel.setText((lState == null || lState.equals("")) ? "-" : Display.getViewResourceBundle().getString("catView.alarmView.state." + lState));
        idLabel.setText((lId == null) ? "-" : lId);
        nameLabel.setText((lName == null) ? "-" : lName);
        siteLabel.setText((lSite == null) ? "-" : lSite);
        objectTypeLabel.setText((lObjectType == null || lObjectType.equals("")) ? "-" : Display.getViewResourceBundle().getString("catView.alarmView.objectType." + lObjectType));
        objectNameLabel.setText((lObjectName == null) ? "-" : lObjectName);
        raiseDateLabel.setText((lRaiseDate == null) ? "-" : lRaiseDate);
        modificationDateLabel.setText((lModificationDate == null) ? "-" : lModificationDate);
        clearDateLabel.setText((lClearDate == null) ? "-" : lClearDate);
        typeLabel.setText((lType == null || lType.equals("")) ? "-" : Display.getViewResourceBundle().getString("catView.alarmView.type." + lType));
        probableCauseLabel.setText((lProbableCause == null) ? "-" : lProbableCause);
        additionalInformationLabel.setText((lAdditionalInformation == null) ? "-" : lAdditionalInformation);
        remedialActionLabel.setText((lRemedialAction == null) ? "-" : lRemedialAction);

        // Display dialog
        dialogStage.showAndWait();

    }

    /**
     * Acknowledges or un-acknowledges displayed alarms depending on the current state
     */
    public void acknowledgeUnAcknowledge() {
        if (acknowledge) {
            GlobalMonitoring.getInstance().acknowledgeAlarms(alarms);
            acknowledgeUnAcknowledgeButton.setText(Display.getViewResourceBundle().getString("alarmDetailsDialog.unAcknowledge"));
            stateLabel.setText(Display.getViewResourceBundle().getString("catView.alarmView.state.acknowledged"));
        } else {
            GlobalMonitoring.getInstance().unAcknowledgeAlarms(alarms);
            acknowledgeUnAcknowledgeButton.setText(Display.getViewResourceBundle().getString("alarmDetailsDialog.acknowledge"));
            stateLabel.setText(Display.getViewResourceBundle().getString("catView.alarmView.state.raised"));
        }
        additionalInformationLabel.setText(alarms.get(0).getAdditionalInformation());
        acknowledge = !acknowledge;
        Cat.getInstance().getController().refreshActiveAlarmsListAndRemoveSelection();

    }

    /**
     * Clears displayed alarms depending on the current state
     */
    public void clear() {
        GlobalMonitoring.getInstance().clearAlarms(alarms, String.format(Display.getViewResourceBundle().getString("globalMonitoring.alarms.manualClear"), System.getProperty("user.name")));
        stateLabel.setText(Display.getViewResourceBundle().getString("catView.alarmView.state.cleared"));
        String lClearDate = null;
        for (Alarm lAlarm: alarms) {
            lClearDate = computeNewValue(lClearDate,
                    (lAlarm.getClearDate() == null) ? "" : LocaleUtilities.getInstance().getDateFormat().format(lAlarm.getClearDate()) + " " +
                            LocaleUtilities.getInstance().getTimeFormat().format(lAlarm.getClearDate().getTime()));
        }
        clearDateLabel.setText(lClearDate);
        clearButton.setVisible(false);
        acknowledgeUnAcknowledgeButton.setVisible(false);
        Cat.getInstance().getController().refreshActiveAlarmsListAndRemoveSelection();
    }

    /**
     * Deletes displayed alarms depending on the current state
     */
    public void delete() {

        // Prepare confirmation dialog box
        Alert lConfirmation = new Alert(Alert.AlertType.CONFIRMATION,
                                        Display.getViewResourceBundle().getString(alarms.size() > 1 ? "confirm.deleteAlarms.question" : "confirm.deleteAlarm.question"),
                ButtonType.YES, ButtonType.NO);
        lConfirmation.setHeaderText(Display.getViewResourceBundle().getString(alarms.size() > 1 ? "confirm.deleteAlarms.title" : "confirm.deleteAlarm.title"));
        lConfirmation.initModality(Modality.APPLICATION_MODAL);

        // Display confirmation dialog box
        Optional<ButtonType> lResponse = lConfirmation.showAndWait();

        // OK is pressed
        if (lResponse.isPresent() && lResponse.get().equals(ButtonType.YES)) {

            // Remove selected alarms from the correct list
            if (!alarms.get(0).getState().equals(EnumTypes.AlarmState.CLEARED)) {
                GlobalMonitoring.getInstance().getActiveAlarmsList().removeAll(alarms);
                Cat.getInstance().getController().refreshActiveAlarmsListAndRemoveSelection();
            } else {
                GlobalMonitoring.getInstance().getHistoricalAlarmsList().removeAll(alarms);
                Cat.getInstance().getController().refreshHistoricalAlarmsListAndRemoveSelection();
            }
            dialogStage.close();
        }

    }

    /**
     * Close alarm details dialog
     */
    public void close() {
        dialogStage.close();
    }

}
