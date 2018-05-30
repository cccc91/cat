package cclerc.cat.view;

import cclerc.services.Display;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class AboutDialog {

    // Display management
    private Stage dialogStage;

    @FXML private Label productDescription;
    @FXML private Label productVersion;
    @FXML private Label build;
    @FXML private Label buildDate;
    @FXML private Label javaVersion;
    @FXML private Label author;
    @FXML private Label copyright;

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
