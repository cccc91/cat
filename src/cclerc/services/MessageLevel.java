package cclerc.services;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.util.HashMap;

/**
 * Internal class defining the text format a message level
 */
class TextFormat {

    private Font font = Font.getDefault();
    private Color color = Color.BLACK;

    /**
     * Constructor
     * @param aInFontFamily  Font family of the text, if null use the system font family
     * @param aInFontSize    Font size of the text, if 0 use the system font size
     * @param aInFontWeight  Font weight of the text, if null user normal
     * @param aInFontPosture Font posture of the text, if null user regular
     * @param aInColor       Color of the text, if null use black
     */
    public TextFormat(String aInFontFamily, double aInFontSize, FontWeight aInFontWeight, FontPosture aInFontPosture, Color aInColor) {
        if (aInFontFamily == null) {
            aInFontFamily = Font.getDefault().getName();
        }
        if (aInFontSize == 0.0) {
            aInFontSize = Font.getDefault().getSize();
        }
        if (aInFontWeight == null) {
            aInFontWeight = FontWeight.NORMAL;
        }
        if (aInFontPosture == null) {
            aInFontPosture = FontPosture.REGULAR;
        }
        font = Font.font(aInFontFamily, aInFontWeight, aInFontPosture, aInFontSize);

        if (aInColor != null) {
            color = aInColor;
        }
    }

    // GETTERS

    /**
     * Gets text font
     * @return Text font
     */
    public Font getFont() {
        return font;
    }

    /**
     * Gets text color
     * @return Text color
     */
    public Color getColor() {
        return color;
    }

}

/**
 * Class defining the characteristics of different message levels for display in user interface
 */
public class MessageLevel {

    // Message levels
    private final static Integer INFO = 0;
    private final static Integer OK = 1;
    private final static Integer WARNING = 2;
    private final static Integer ERROR = 3;

    // Message text formatting depending on the level
    private static HashMap<Integer, TextFormat> textFormat = new HashMap<>();
    static
    {
        textFormat.put(INFO, new TextFormat("Courier New", 0, FontWeight.NORMAL, FontPosture.REGULAR, Color.BLACK));
        textFormat.put(OK, new TextFormat("Courier New", 0, FontWeight.NORMAL, FontPosture.REGULAR, Color.GREEN));
        textFormat.put(WARNING, new TextFormat("Courier New", 0, FontWeight.NORMAL, FontPosture.REGULAR, Color.ORANGE));
        textFormat.put(ERROR, new TextFormat("Courier New", 0, FontWeight.BOLD, FontPosture.REGULAR, Color.RED));
    }


    private int level = INFO;

    // Pre defined instances
    public final static MessageLevel INFO_LEVEL = new MessageLevel(INFO);
    public final static MessageLevel OK_LEVEL = new MessageLevel(OK);
    public final static MessageLevel WARNING_LEVEL = new MessageLevel(WARNING);
    public final static MessageLevel ERROR_LEVEL = new MessageLevel(ERROR);

    /**
     * Constructor
     * @param aInLevel Message level
     */
    public MessageLevel (int aInLevel) {
        level = aInLevel;
    }

    // SETTERS

    /**
     * Sets message level
     * @param aInLevel Message level
     */
    public void setLevel(int aInLevel) {
        level = aInLevel;
    }

    // GETTERS

    /**
     * Gets message level
     * @return Message level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets message font
     * @return Message font
     */
    public Font getFont() {
        return textFormat.get(level).getFont();
    }

    /**
     * Gets message color
     * @return Message color
     */
    public Color getColor() {
        return textFormat.get(level).getColor();
    }

}
