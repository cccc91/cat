package cclerc.cat.view;

import cclerc.cat.Cat;
import cclerc.services.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConfigureSpeedTestDialog {

    private static ConfigureSpeedTestDialog configureSpeedTestDialogInstance;

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

        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.displayDialog"), Utilities.getStackTrace(e)));
        }

        return configureSpeedTestDialogInstance;

    }

    /**
     * Closes dialog box
     */
    @FXML
    public void close() {
        dialogStage.close();
    }

    /**
     * Displays the dialog box
     */
    public void show() {
        GeoLocalization.getInstance().getLocalGeoLocalization();
        dialogStage.showAndWait();
    }

}
