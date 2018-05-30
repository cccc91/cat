package cclerc.services;

import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.util.Date;

/**
 *   Singleton class implementing messages display in graphical interface
 */
public class Message {

    private EnumTypes.MessageLevel messageLevel;
    private String text;
    private Date date;

    // CONSTRUCTOR

    /**
     * Message constructor
     * @param aInText          Text to display
     * @param aInMessageLevel  Level in which the text must be displayed
     */
    public Message(String aInText, EnumTypes.MessageLevel aInMessageLevel) {
        text = aInText;
        messageLevel = aInMessageLevel;
    }

    /**
     * Message constructor
     * @param aInText          Text to display
     * @param aInMessageLevel  Level in which the text must be displayed
     * @param aInDate          Date of the message
     */
    public Message(String aInText, EnumTypes.MessageLevel aInMessageLevel, Date aInDate) {
        text = aInText;
        messageLevel = aInMessageLevel;
        date = aInDate;
    }

    // METHODS

    /**
     * Prints a message in a pane with text formatting associated to message level
     * @param aInPane         Pane in which message must be displayed
     */
    public void print(Pane aInPane) {

        if (date == null) date = new Date();
        String lDate = LocaleUtilities.getInstance().getDateFormat().format(date);
        String lTime = LocaleUtilities.getInstance().getTimeFormat().format(date.getTime());

        Text lText = new Text();
        lText.setText(String.format("%s %s - %s", lDate, lTime, text));
        lText.getStyleClass().add("message-" + messageLevel.toString().toLowerCase());
        lText.setFill(messageLevel.getColor());

        // Display text in pane
        aInPane.getChildren().add(lText);

    }

    /**
     * Prints a message with a carriage return in a pane with text formatting associated to message level
     * @param aInPane         Pane in which message must be displayed
     */
    public void println(Pane aInPane) {

        if (date == null) date = new Date();
        String lDate = LocaleUtilities.getInstance().getDateFormat().format(date);
        String lTime = LocaleUtilities.getInstance().getTimeFormat().format(date.getTime());

        Text lText = new Text();
        lText.setText(String.format("%s %s - %s\n", lDate, lTime, text));
        lText.getStyleClass().add("message-" + messageLevel.toString().toLowerCase());
        lText.setFill(messageLevel.getColor());

        // Display text in pane
        aInPane.getChildren().add(lText);

    }

}
