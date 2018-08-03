package cclerc.cat.view;

import cclerc.cat.Cat;
import cclerc.cat.Configuration.AbstractConfiguration;
import cclerc.cat.Configuration.Configuration;
import cclerc.cat.Configuration.SmtpServerConfiguration;
import cclerc.cat.model.ConfiguredSmtpServer;
import cclerc.services.*;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.mail.MessagingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class AddEditSmtpServersDialog {

    private static AddEditSmtpServersDialog addEditSmtpServersDialogInstance;

    // Display management
    private static Stage dialogStage = new Stage();

    @FXML TextField name;
    @FXML ChoiceBox tlsMode;
    @FXML TextField port;
    @FXML TextField user;
    @FXML TextField login;
    @FXML PasswordField password;
    @FXML TextField connectionTimeout;
    @FXML TextField timeout;
    @FXML Button testButton;
    @FXML Button cancelButton;
    @FXML Button confirmButton;

    private static final String DUMMY_VALUE = "dummy#";
    private boolean initializationDone = false;
    private boolean newSmtpServer;
    private Map<Control, ChangeListener<? super String>> listeners = new HashMap<>();
    private List<Object> invalidConfigurationData = new ArrayList<>();
    private SmtpServerConfiguration initialSmtpServerConfiguration = new SmtpServerConfiguration(Configuration.getInitialConfiguration());
    private  SmtpServerConfiguration smtpServerConfiguration;

    /**
     * Creates instance of AddEditSmtpServersDialog controller
     * @param aInParentStage Parent stage of add edit smtp server dialog stage
     */
    public static AddEditSmtpServersDialog getInstance(Stage aInParentStage) {

        FXMLLoader lDialogLoader = new FXMLLoader();

        try {

            // Load the fxml file and create a new stage for the popup dialog.
            lDialogLoader.setLocation(Cat.class.getResource("view/AddEditSmtpServersDialog.fxml"));
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
            addEditSmtpServersDialogInstance = lDialogLoader.getController();
            dialogStage.setOnCloseRequest(event -> {
                addEditSmtpServersDialogInstance.cancel();
                event.consume();
            });
        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.displayDialog"), Utilities.getStackTrace(e)));
        }

        return addEditSmtpServersDialogInstance;

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

    private String findValue(ConfiguredSmtpServer aInConfiguredSmtpServer, String aInAttribute) {

        if (newSmtpServer) return null;

        try {
            Method lGetMethod = ConfiguredSmtpServer.class.getDeclaredMethod("get" + aInAttribute);
            try {
                Object lValueObject =  lGetMethod.invoke(aInConfiguredSmtpServer);
                if (lValueObject != null) return lValueObject.toString();
                return "";
            } catch (InvocationTargetException|IllegalAccessException ex) {
                return null;
            }
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Adds an invalid configuration data and changes buttons state accordingly
     * @param aInConfigurationData Configuration data object that is invalid (text field, check box, ...)
     */
    private void addInvalidConfigurationData(Object aInConfigurationData) {
        if (!invalidConfigurationData.contains(aInConfigurationData)) invalidConfigurationData.add(aInConfigurationData);
        confirmButton.setDisable(true);
        cancelButton.setDisable(false);
        testButton.setDisable(true);
    }

    /**
     * Removes an invalid configuration data and changes buttons state accordingly
     * @param aInConfigurationData Configuration data object that is invalid (text field, check box, ...)
     */
    private void removeInvalidConfigurationData(Object aInConfigurationData) {
        if (invalidConfigurationData.contains(aInConfigurationData)) invalidConfigurationData.remove(aInConfigurationData);
        if (invalidConfigurationData.size() == 0) {
            checkConfigurationChanges();
            testButton.setDisable(false);
        }
    }

    /**
     * Checks if the configuration has been modified
     */
    private void checkConfigurationChanges() {
        confirmButton.setDisable(smtpServerConfiguration.isSameAs(initialSmtpServerConfiguration));
    }

    private void configureField(Field aInField, ConfiguredSmtpServer aInConfiguredSmtpServer) {

        // Listener is added only on fields of desired type
        if (aInField.getType().equals(TextField.class) || aInField.getType().equals(PasswordField.class) || aInField.getType().equals(ChoiceBox.class)) {

            try {

                Property lProperty = null;

                String lMethodName = (aInField.getName().substring(0, 1).toUpperCase() + aInField.getName().substring(1));
                String lDefaultValue = AbstractConfiguration.findDefaultValue(smtpServerConfiguration, lMethodName);
                String lValue = findValue(aInConfiguredSmtpServer, lMethodName);
                String lInitialValue = (newSmtpServer) ? lDefaultValue : ((lValue == null) ? "" : lValue);

                // Force a set to null to force listener to be called on value initialization after listener has been added
                Control lControl = (Control) aInField.get(this);
                if (aInField.getType().equals(TextField.class)) {
                    lProperty = ((TextField) aInField.get(this)).textProperty();
                    ((TextField) lControl).setText(DUMMY_VALUE);
                } else if (aInField.getType().equals(PasswordField.class)) {
                    lProperty = ((PasswordField) aInField.get(this)).textProperty();
                    ((PasswordField) lControl).setText(DUMMY_VALUE);
                } else if (aInField.getType().equals(ChoiceBox.class)) {
                    if (((ChoiceBox) lControl).getItems().size() == 0) {
                        ((ChoiceBox) lControl).getItems().addAll(EnumTypes.findValues(lMethodName));
                    }
                    ((ChoiceBox) lControl).setValue(EnumTypes.findValues(lMethodName).get(0));
                    ((ChoiceBox) lControl).setValue(EnumTypes.findValues(lMethodName).get(1));
                    lProperty = ((ChoiceBox) aInField.get(this)).valueProperty();
                }

                // Add tooltip to field
                String lTooltipName = "addEditSmtpServers.tooltip." + aInField.getName().substring(0, 1).toLowerCase() + aInField.getName().substring(1);
                String lTooltipText = Display.getViewResourceBundle().getString(lTooltipName);
                Tooltip lTooltip = new Tooltip(lTooltipText);
                Tooltip.install(lControl, lTooltip);

                if (listeners.containsKey(lControl)) {
                    // Remove existing listener on the text field
                    lProperty.removeListener(listeners.get(lControl));
                    listeners.remove(lControl);
                }

                listeners.put(lControl, (observable, oldValue, newValue) -> {

                    try {
                        // Retrieve set method from configuration class and method name
                        Method lSetMethod = smtpServerConfiguration.getClass().getDeclaredMethod("set" + lMethodName, String.class);

                        // Specific case of password
                        String lNewValue = (lControl.equals(password)) ? Security.getInstance().encrypt(newValue) : newValue;

                        // Invoke set method
                        lSetMethod.invoke(smtpServerConfiguration, lNewValue);
                        // Set different style for attribute which is set to the default value
                        if (((newValue == null) && (lDefaultValue == null)) || ((newValue != null) && newValue.equals(lDefaultValue))) {
                            if (((newValue == null) && (lInitialValue == null)) || ((newValue != null) && newValue.equals(lInitialValue))) {
                                lControl.setId("default-value");
                            } else {
                                lControl.setId("default-new-value");
                            }
                        } else if ((newValue == null) || !newValue.equals(lInitialValue)) {
                            // Name change and name already exists
                            if (lControl.equals(name) &&
                                (newSmtpServer && (Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().countSmtpSmtpServerConfiguration(newValue) > 0) ||
                                 Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().countSmtpSmtpServerConfiguration(newValue) > 1)) {
                                lControl.setId("bad-value");
                                addInvalidConfigurationData(lControl);
                            } else  lControl.setId("new-value");
                        } else {
                            lControl.setId("");
                        }
                        removeInvalidConfigurationData(lControl);
                        //                           validateConfiguration(lMethodName, lConfiguration, lTextField);
                    } catch (InvocationTargetException e) {
                        // If the set method has raised an error (bad type for instance), change color and disable save
                        lControl.setId("bad-value");
                        addInvalidConfigurationData(lControl);
                    } catch (NoSuchMethodException | IllegalAccessException|NullPointerException e) {
                        Display.logUnexpectedError(e);

                    }
                });
                lProperty.addListener(listeners.get(lControl));

                if (aInField.getType().equals(TextField.class)) {
                    ((TextField) lControl).setText(lInitialValue);
                } else if (aInField.getType().equals(PasswordField.class)) {
                    ((PasswordField) lControl).setText(Security.getInstance().decrypt(lInitialValue));
                } else if (aInField.getType().equals(ChoiceBox.class)) {
                    ((ChoiceBox) lControl).setValue(lInitialValue);
                }

            } catch (Exception e) {
                Display.logUnexpectedError(e);
            }

        }

    }

    // METHODS

    /**
     * Displays the add or edit SMTP server dialog box
     * @param  aInConfiguredSmtpServer SMTP server configuration to modify in case of edition, null in case of addition
     */
    public void show(ConfiguredSmtpServer aInConfiguredSmtpServer) {

        newSmtpServer = (aInConfiguredSmtpServer == null);

        // Retrieve current SMTP server configuration
        try {
            if (!newSmtpServer)
                initialSmtpServerConfiguration.copy(Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration()
                                                                 .findSmtpServerConfiguration(aInConfiguredSmtpServer.getName()));
        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }
        smtpServerConfiguration =
                (newSmtpServer) ?
                new SmtpServerConfiguration(Configuration.getCurrentConfiguration()) :
                Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().findSmtpServerConfiguration(aInConfiguredSmtpServer.getName());

        // Set button states
        confirmButton.setDisable(true);

        dialogStage.setTitle(Display.getViewResourceBundle().getString((newSmtpServer) ? "addEditSmtpServers.add.title" : "addEditSmtpServers.edit.title"));

        // Parse all fields of current class and consider only text fields
        for (Field lField: getClass().getDeclaredFields()) {
            configureField(lField, aInConfiguredSmtpServer);
        }

        if (!initializationDone) {
            dialogStage.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == KeyCode.ENTER) {
                    confirmButton.fire();
                    ev.consume();
                }
            });
            initializationDone = true;
        }

        // Tooltips
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) {
            if (newSmtpServer) {
                Tooltip lTooltipSmtpServerConfirm = new Tooltip(Display.getViewResourceBundle().getString("addEditSmtpServers.add.tooltip.confirm"));
                Tooltip.install(confirmButton, lTooltipSmtpServerConfirm);
                Tooltip lTooltipSmtpServerCancel = new Tooltip(Display.getViewResourceBundle().getString("addEditSmtpServers.add.tooltip.cancel"));
                Tooltip.install(cancelButton, lTooltipSmtpServerCancel);
            } else {
                Tooltip lTooltipSmtpServerConfirm = new Tooltip(Display.getViewResourceBundle().getString("addEditSmtpServers.edit.tooltip.confirm"));
                Tooltip.install(confirmButton, lTooltipSmtpServerConfirm);
                Tooltip lTooltipSmtpServerCancel = new Tooltip(Display.getViewResourceBundle().getString("addEditSmtpServers.edit.tooltip.cancel"));
                Tooltip.install(cancelButton, lTooltipSmtpServerCancel);
            }
            Tooltip lTooltipSmtpServerTest = new Tooltip(Display.getViewResourceBundle().getString("addEditSmtpServers.tooltip.test"));
            Tooltip.install(confirmButton, lTooltipSmtpServerTest);
        }

        // Display
        dialogStage.setResizable(false);
        dialogStage.showAndWait();

    }

    /**
     * Tests the SMTP server
     */
    @FXML private void test() {

        SmtpServer lSmtpServer = new SmtpServer(
                smtpServerConfiguration.getName(), smtpServerConfiguration.getTlsMode(), String.valueOf(smtpServerConfiguration.getPort()),
                smtpServerConfiguration.getUser(), smtpServerConfiguration.getLogin(), Security.getInstance().decrypt(smtpServerConfiguration.getPassword()),
                String.valueOf(smtpServerConfiguration.getConnectionTimeout() * 1000), String.valueOf(smtpServerConfiguration.getTimeout() * 1000));

        ArrayList<String> lRecipientList = new ArrayList<>(Arrays.asList(smtpServerConfiguration.getUser()));

        Alert lResult =
                new Alert(Alert.AlertType.INFORMATION, "", ButtonType.CLOSE);
        lResult.initModality(Modality.APPLICATION_MODAL);
        lResult.setHeaderText(Display.getViewResourceBundle().getString("configuration.monitoringJobs.email.smtpServers.test.title"));
        WaitDialog lWaitDialog = Display.waitDialog(dialogStage, Display.getViewResourceBundle().getString("configuration.monitoringJobs.email.smtpServers.test.ongoing.title"),
                                                    Constants.IMAGE_EMAIL_TEST);
        lWaitDialog.show(new WaitDialogInterface() {
            @Override
            public void runAction() {
                try {


                    lSmtpServer.sendMail(lRecipientList, lSmtpServer.getUser(),
                                         Display.getMessagesResourceBundle().getString("smtpServer.configuration.test.subject"),
                                         Display.getMessagesResourceBundle().getString("smtpServer.configuration.test.body"));

                    Image image = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_EMAIL_OK).toString());
                    ImageView imageView = new ImageView(image);
                    lResult.setGraphic(imageView);
                    lResult.setContentText(Display.getViewResourceBundle().getString("configuration.monitoringJobs.email.smtpServers.test.ok"));
                } catch (MessagingException e) {
                    Image image = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_EMAIL_NOK).toString());
                    ImageView imageView = new ImageView(image);
                    lResult.setGraphic(imageView);
                    lResult.setContentText(String.format(Display.getViewResourceBundle().getString("configuration.monitoringJobs.email.smtpServers.test.nok"), e.getCause()));
                    lResult.setAlertType(Alert.AlertType.ERROR);
                } finally {
                    Platform.runLater(() -> {
                        lWaitDialog.close();
                    });
                    Platform.runLater(() -> {
                        lResult.showAndWait();
                    });
                }
            }
        });
    }

    /**
     * Confirms the changes
     */
    @FXML private void confirm() {

        // In case of new SMTP server, add it to the configuration (in case of edition, the configuration already contains modifications)
        if (newSmtpServer) {
            Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().addSmtpServerConfiguration(smtpServerConfiguration);
        }

        dialogStage.close();

    }

    /**
     * Cancels the changes
     */
    @FXML public void cancel() {

        Optional<ButtonType> lResponse = Optional.empty();

        // Ask user for confirmation if a change has been done
        if (((newSmtpServer && (
                ((smtpServerConfiguration.getName() != null) && !smtpServerConfiguration.getName().equals(DUMMY_VALUE)) ||
                ((smtpServerConfiguration.getTlsMode() != null) && !smtpServerConfiguration.getTlsMode().equals(Constants.DEFAULT_SMTP_TLS_MODE.toString())) ||
                ((smtpServerConfiguration.getPort() != null) && !smtpServerConfiguration.getPort().equals(Constants.DEFAULT_SMTP_PORT)) ||
                ((smtpServerConfiguration.getUser() != null) && !smtpServerConfiguration.getUser().equals(DUMMY_VALUE) ) ||
                ((smtpServerConfiguration.getLogin() != null) && !smtpServerConfiguration.getLogin().equals("") & !smtpServerConfiguration.getLogin().equals(DUMMY_VALUE)) ||
                ((smtpServerConfiguration.getPassword() != null) && !smtpServerConfiguration.getPassword().equals("") && !smtpServerConfiguration.getPassword().equals(DUMMY_VALUE)) ||
                ((smtpServerConfiguration.getConnectionTimeout() != null) && !smtpServerConfiguration.getConnectionTimeout().equals(Constants.DEFAULT_SMTP_CONNECTION_TIMEOUT)) ||
                ((smtpServerConfiguration.getTimeout() != null) && !smtpServerConfiguration.getTimeout().equals(Constants.DEFAULT_SMTP_TIMEOUT)))))
            || (!newSmtpServer && !smtpServerConfiguration.isSameAs(initialSmtpServerConfiguration))) {
            Alert lConfirmation =
                    new Alert(Alert.AlertType.CONFIRMATION, Display.getViewResourceBundle().getString("confirm.cancelConfiguration.question"), ButtonType.YES, ButtonType.NO);
            lConfirmation.setHeaderText(Display.getViewResourceBundle().getString("confirm.cancelConfiguration.title"));
            lConfirmation.initModality(Modality.APPLICATION_MODAL);

            // Display confirmation dialog box
            lResponse = lConfirmation.showAndWait();
        }

        // OK is pressed
        if (!lResponse.isPresent() || lResponse.get().equals(ButtonType.YES)) {
            if (!newSmtpServer)
                try {
                    smtpServerConfiguration.copy(initialSmtpServerConfiguration);
                } catch (Exception e) {
                    Display.logUnexpectedError(e);
                }

            dialogStage.close();
        }

    }

}
