package cclerc.services;

import cclerc.cat.Configuration.AbstractConfiguration;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileSelector implements EventHandler<Event> {

    public enum FileSelectorType {CONFIGURATION};

    private Stage stage;
    private FileSelectorType fileSelectorType;
    private String fileIdentification;
    private TextField filenameTextField;
    private Map<String, List<String>> patterns = new HashMap<>();
    private String defaultPattern;

    // CONSTRUCTORS
    private FileSelector() {
    }

    private FileSelector(FileSelectorBuilder aInFileSelectorBuilder) {
        stage = aInFileSelectorBuilder.stage;
        fileSelectorType = aInFileSelectorBuilder.fileSelectorType;
        fileIdentification = (aInFileSelectorBuilder.fileIdentification == null) ? "default" :aInFileSelectorBuilder.fileIdentification;
        filenameTextField = aInFileSelectorBuilder.filenameTextField;
        patterns = aInFileSelectorBuilder.patterns;
        defaultPattern = aInFileSelectorBuilder.defaultPattern;
    }

    // BUILDER CLASS
    public static class FileSelectorBuilder {

        private Stage stage;
        private FileSelectorType fileSelectorType;
        private String fileIdentification;
        private TextField filenameTextField;
        private Map<String, List<String>> patterns = new HashMap<>();
        private String defaultPattern;

        // CONSTRUCTORS
        private FileSelectorBuilder() {
        }

        public FileSelectorBuilder(Stage aInStage, FileSelectorType aInFileSelectorType) {
            stage = aInStage;
            fileSelectorType = aInFileSelectorType;
        }

        // SETTERS

        public FileSelectorBuilder setFileIdentification(String aInFileChooserTitle) {
            fileIdentification = aInFileChooserTitle;
            return this;
        }

        public FileSelectorBuilder setFilenameTextField(TextField aInFilenameTextField) {
            filenameTextField = aInFilenameTextField;
            return this;
        }

        public FileSelectorBuilder addPattern(String aInText, List<String> aInPatterns) {
            patterns.put(aInText, aInPatterns);
            return this;
        }

        public FileSelectorBuilder setDefaultPattern(String aInPattern) {
            defaultPattern = aInPattern;
            return this;
        }

        // BUILDER

        public FileSelector build() {
            if (fileSelectorType.equals(FileSelectorType.CONFIGURATION) && (filenameTextField == null)) throw new IllegalArgumentException();
            return new FileSelector(this);
        }

    }

    @Override
    public void handle(Event aInEvent) {

        FileChooser lFileChooser = new FileChooser();
        lFileChooser.setTitle(Display.getViewResourceBundle().getString("fileSelector.title." + fileIdentification));

        // Add extension filters
        for (String lPattern: patterns.keySet()) {
            lFileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(lPattern, patterns.get(lPattern)));
        }

        // Select eventually default extension filter
        if (defaultPattern != null) {
            for (FileChooser.ExtensionFilter lExtensionFilter : lFileChooser.getExtensionFilters()) {
                if (lExtensionFilter.getExtensions().contains(defaultPattern)) {
                    lFileChooser.setSelectedExtensionFilter(lExtensionFilter);
                    break;
                }
            }
        }
        lFileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*"));

        // Open file chooser on directory of last selected file from state properties
        lFileChooser.setInitialDirectory(new File(States.getInstance().getValue("fileSelector." + fileIdentification + ".directory", System.getProperty("user.home"))));

        if (fileSelectorType.equals(FileSelectorType.CONFIGURATION)) {
            String lCurrentFileName = filenameTextField.getText();
            if (!lCurrentFileName.startsWith("resources")) {
                lFileChooser.setInitialDirectory(new File(lCurrentFileName).getParentFile());
            }
        }

        // Open single selection file chooser
        File lSelectedFile = lFileChooser.showOpenDialog(stage);

        // A file has been selected
        if (lSelectedFile != null) {

            // Save file directory in states properties
            States.getInstance().saveValue("fileSelector." + fileIdentification + ".directory", lSelectedFile.getParent());

            // Update configuration
            if (fileSelectorType.equals(FileSelectorType.CONFIGURATION)) {
                String lSelectedFilePath = lSelectedFile.getPath().replaceAll("\\\\", "/");
                filenameTextField.setText(lSelectedFilePath);
            }

        }

    }

}
