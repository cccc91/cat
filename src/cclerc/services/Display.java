package cclerc.services;

import cclerc.cat.Cat;
import cclerc.cat.view.WaitDialog;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ResourceBundle;

public class Display {

    private static Logger logger;
    private static ResourceBundle viewResourceBundle;
    private static ResourceBundle messagesResourceBundle;
    private static ResourceBundle aboutResourceBundle;

    public static Logger getLogger() {
        if (logger == null) logger = LogManager.getLogger(Cat.class.getName());
         return logger;
    }

    public static ResourceBundle getAboutResourceBundle() {
        if (aboutResourceBundle == null) aboutResourceBundle = ResourceBundle.getBundle("resources/about/about");
        return aboutResourceBundle;
    }

    public static ResourceBundle getViewResourceBundle() {
        if (viewResourceBundle == null) viewResourceBundle = ResourceBundle.getBundle("resources/lang/view", LocaleUtilities.getInstance().getCurrentLocale());
        return viewResourceBundle;
    }

    public static ResourceBundle getMessagesResourceBundle() {
        if (messagesResourceBundle == null) messagesResourceBundle = ResourceBundle.getBundle("resources/lang/messages", LocaleUtilities.getInstance().getCurrentLocale());
        return messagesResourceBundle;
    }

    public static void resetMessagesResourceBundle() {
        messagesResourceBundle = ResourceBundle.getBundle("resources/lang/messages", LocaleUtilities.getInstance().getCurrentLocale());
    }

    public static void logUnexpectedError(Exception aInException) {
        logger.error(String.format(messagesResourceBundle.getString("log.unexpected"), Utilities.getStackTrace(aInException)));
    }

    public static WaitDialog waitDialog(Stage aInParentStage, String aInHeader, String aInLogo) {

        try {

            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader lLoader = new FXMLLoader();
            lLoader.setLocation(Cat.class.getResource("view/WaitDialog.fxml"));
            lLoader.setResources(Display.getViewResourceBundle());
            VBox lPane = lLoader.load();

            // Create the  stage
            Stage lDialogStage = new Stage();
            lDialogStage.setTitle(Display.getViewResourceBundle().getString("wait.title"));
            //lDialogStage.initModality(Modality.WINDOW_MODAL);
            lDialogStage.initOwner(aInParentStage);
            Scene lScene = new Scene(lPane);
            lScene.getStylesheets().add("resources/css/view.css");
            lDialogStage.setScene(lScene);
            lDialogStage.getIcons().add(Constants.APPLICATION_IMAGE);

            WaitDialog lController = lLoader.getController();
            lController.initializeContext(lDialogStage, aInHeader, aInLogo);

            return lController;

        } catch (Exception e) {
            Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.cat.error.displayDialog"), Utilities.getStackTrace(e)));
            return null;
        }

    }
    private Display() {
    }

}
