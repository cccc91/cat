package cclerc.cat.view;

import cclerc.cat.Cat;
import cclerc.services.Constants;
import cclerc.services.Display;
import cclerc.services.Utilities;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AboutDialog {

    private static AboutDialog aboutDialogInstance;

    // Display management
    private static Stage dialogStage = new Stage();

    @FXML private Label productDescription;
    @FXML private Label productVersion;
    @FXML private Label build;
    @FXML private Label buildDate;
    @FXML private Label javaVersion;
    @FXML private Label author;
    @FXML private Label copyright;

    /**
     * Creates instance of AboutDialog controller
     * @param aInParentStage Parent stage of about dialog stage
     */
    public static AboutDialog getInstance(Stage aInParentStage) {

        FXMLLoader lDialogLoader = new FXMLLoader();

        try {

            // Load the fxml file and create a new stage for the popup dialog.
            lDialogLoader.setLocation(Cat.class.getResource("view/AboutDialog.fxml"));
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
            dialogStage.setTitle(Display.getViewResourceBundle().getString("aboutDialog.title") + " " + Display.getAboutResourceBundle().getString("product.name"));
            aboutDialogInstance = lDialogLoader.getController();
        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.displayDialog"), Utilities.getStackTrace(e)));
        }

        return aboutDialogInstance;

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

    /**
     * Displays dialog box
     */
    public void show() {
        productDescription.setText(Display.getAboutResourceBundle().getString("product.description"));
        productVersion.setText(Display.getAboutResourceBundle().getString("product.major") + "." + Display.getAboutResourceBundle().getString("product.minor"));
        build.setText(Display.getAboutResourceBundle().getString("product.build"));
        buildDate.setText(Display.getAboutResourceBundle().getString("product.build.date"));
        javaVersion.setText(Display.getAboutResourceBundle().getString("product.java.version"));
        author.setText(Display.getAboutResourceBundle().getString("product.author"));
        copyright.setText(Display.getAboutResourceBundle().getString("product.copyright"));
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    /**
     * Closes dialog box
     */
    public void close() {
        dialogStage.close();
    }

}
