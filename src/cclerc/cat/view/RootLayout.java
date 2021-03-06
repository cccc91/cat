package cclerc.cat.view;

import cclerc.cat.Cat;
import cclerc.cat.Configuration.Configuration;
import cclerc.services.Constants;
import cclerc.services.Preferences;
import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;

public class RootLayout {

    private static AboutDialog aboutDialog;
    private static ConfigurationDialog configurationDialogController;

    @FXML private MenuItem save;
    @FXML private MenuItem saveAs;
    @FXML private MenuItem close;
    @FXML private MenuItem configure;
    @FXML private CheckMenuItem autoSaveConfigurationPreference;
    @FXML private CheckMenuItem enableGeneralTooltipPreference;
    @FXML private CheckMenuItem enableDetailTooltipPreference;

    @FXML private void initialize() {

        // Menu images
        ImageView lImageViewSave = new ImageView(Constants.IMAGE_SAVE);
        lImageViewSave.setFitHeight(20d); lImageViewSave.setFitWidth(20d);
        save.setGraphic(lImageViewSave);
        ImageView lImageViewSaveAs = new ImageView(Constants.IMAGE_SAVE_AS);
        lImageViewSaveAs.setFitHeight(20d); lImageViewSaveAs.setFitWidth(20d);
        saveAs.setGraphic(lImageViewSaveAs);
        ImageView lImageViewClose = new ImageView(Constants.IMAGE_CLOSE_APP);
        lImageViewClose.setFitHeight(20d); lImageViewClose.setFitWidth(20d);
        close.setGraphic(lImageViewClose);
        ImageView lImageViewConfigure = new ImageView(Constants.IMAGE_CONFIGURE);
        lImageViewConfigure.setFitHeight(20d); lImageViewConfigure.setFitWidth(20d);
        configure.setGraphic(lImageViewConfigure);

    }

        // SETTERS

    /**
     * Sets a reference to AboutDialog controller
     * @param aInAboutDialogController AboutDialog controller
     */
    public void setAboutDialogController(AboutDialog aInAboutDialogController) {
        aboutDialog = aInAboutDialogController;
    }

    /**
     * Sets a reference to ConfigurationDialog controller
     * @param aInConfigurationDialogController ConfigurationDialog controller
     */
    public void setConfigurationMonitoringJobsDialogController(ConfigurationDialog aInConfigurationDialogController) {
        configurationDialogController = aInConfigurationDialogController;
    }

    // FXML

    /**
     * Save configuration in current file
     */
    @FXML private void saveMenuItem() {
        Configuration.getCurrentConfiguration().save();
        Configuration.resetInitialConfiguration();
    }

    /**
     * Save configuration in a new file
     */
    @FXML private void saveAsMenuItem() {

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
        File lFile = lFileChooser.showSaveDialog(Cat.getInstance().getMainStage());

        if (lFile != null) {

            // Make sure file has the correct extension
            if (!lFile.getPath().endsWith(".xml")) {
                lFile = new File(lFile.getPath() + ".xml");
            }

            // Save last chosen path in preferences
            Preferences.getInstance().saveValue("saveFilePath", lFile.getParent());

            // Save the configuration
            Configuration.getCurrentConfiguration().saveAs(lFile.getPath());
            Configuration.resetInitialConfiguration();

        }
    }

    /**
     * Exit the application
     */
    @FXML private void closeMenuItem() {

        // Check if configuration has been modified
        if (!Configuration.getCurrentConfiguration().isSameAs(Configuration.getInitialConfiguration())) {
            if (Cat.confirmSaveAndExit()) {
                Cat.getInstance().end();
            }
        } else {
            if (Cat.confirmExit()) {
                Cat.getInstance().end();
            }
        }

    }

    /**
     * Displays About dialog box
     */
    @FXML private void about() {
        aboutDialog.show();
    }

    /**
     * Displays Monitoring Jobs Configuration dialog box
     */
    @FXML private void configureMonitoringJobs() {
        configurationDialogController.show(false);
    }

    /**
     * Switches auto save configuration preference
     */
    @FXML private void setAutoSaveConfiguration() {
        Preferences.getInstance().saveValue("autoSaveConfiguration", autoSaveConfigurationPreference.isSelected());
        if (autoSaveConfigurationPreference.isSelected() && !Configuration.getCurrentConfiguration().equals(Configuration.getInitialConfiguration())) {
            Configuration.getCurrentConfiguration().save();
            Configuration.resetInitialConfiguration();        }
    }

    /**
     * Switches enable general tooltip preference
     */
    @FXML private void setEnableGeneralTooltip() {
        Preferences.getInstance().saveValue("enableGeneralTooltip", enableGeneralTooltipPreference.isSelected());
        Cat.restart();
    }

    /**
     * Switches enable detail tooltip preference
     */
    @FXML private void setEnableDetailTooltip() {
        Preferences.getInstance().saveValue("enableDetailTooltip", enableDetailTooltipPreference.isSelected());
        Cat.restart();
    }

    public void setPreferences() {
        autoSaveConfigurationPreference.setSelected(Preferences.getInstance().getBooleanValue("autoSaveConfiguration"));
        enableGeneralTooltipPreference.setSelected(Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE));
        enableDetailTooltipPreference.setSelected(Preferences.getInstance().getBooleanValue("enableDetailTooltip", Constants.DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE));
    }

}
