package cclerc.services;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class Styles {

    /**
     * Sets integer text fields style depending on the value
     * @param aInTextField    Text field to style
     * @param aInInitialValue Initial value of the text field
     * @param aInNewValue     New value of the text field
     * @param aInDefaultValue Default value of the text field
     */
    public static void setTextFieldStyle(TextField aInTextField, Integer aInInitialValue, Integer aInNewValue, Integer aInDefaultValue) {
        if (Integer.valueOf(aInNewValue).equals(aInDefaultValue)) {
            if (aInNewValue.equals(aInInitialValue)) {
                aInTextField.setId("default-value");
            } else {
                aInTextField.setId("default-new-value");
            }
        } else {
            if (aInNewValue.equals(aInInitialValue)) {
                aInTextField.setId("");
            } else {
                aInTextField.setId("new-value");
            }
        }
    }

    /**
     * Sets long text fields style depending on the value
     * @param aInTextField    Text field to style
     * @param aInInitialValue Initial value of the text field
     * @param aInNewValue     New value of the text field
     * @param aInDefaultValue Default value of the text field
     */
    public static void setTextFieldStyle(TextField aInTextField, Long aInInitialValue, Long aInNewValue, Long aInDefaultValue) {
        if (Long.valueOf(aInNewValue).equals(aInDefaultValue)) {
            if (aInNewValue.equals(aInInitialValue)) {
                aInTextField.setId("default-value");
            } else {
                aInTextField.setId("default-new-value");
            }
        } else {
            if (aInNewValue.equals(aInInitialValue)) {
                aInTextField.setId("");
            } else {
                aInTextField.setId("new-value");
            }
        }
    }

    /**
     * Sets check boxes style depending on the value
     * @param aInCheckBox     Check box to style
     * @param aInInitialValue Initial value of the check box
     * @param aInNewValue     New value of the check box
     * @param aInDefaultValue Default value of the check box
     */
    public static void setCheckBoxStyle(CheckBox aInCheckBox, Boolean aInInitialValue, Boolean aInNewValue, Boolean aInDefaultValue) {
        if (aInNewValue == aInDefaultValue) {
            if (aInNewValue == aInInitialValue) {
                aInCheckBox.setId("default-value");
            } else {
                aInCheckBox.setId("default-new-value");
            }
        } else {
            if (aInNewValue == aInInitialValue) {
                aInCheckBox.setId("");
            } else {
                aInCheckBox.setId("new-value");
            }
        }
    }

}
