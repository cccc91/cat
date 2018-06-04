package cclerc.cat.model;

import cclerc.cat.Configuration.AlarmConfiguration;
import cclerc.cat.Configuration.Configuration;
import cclerc.services.EnumTypes;
import cclerc.services.Network;
import cclerc.services.Utilities;
import javafx.beans.property.*;
import org.jdom2.Element;

public class ConfiguredAlarm {

    private IntegerProperty id;
    private StringProperty name;
    private SimpleBooleanProperty isFiltered;
    private EnumTypes.AlarmSeverity defaultSeverity;
    private EnumTypes.AlarmSeverity newSeverity;

    // GETTERS

    public int getId() {
        return id.get();
    }

    public String getName() {
        return name.get();
    }

    public Boolean getIsFiltered() {
        return isFiltered.get();
    }

    public EnumTypes.AlarmSeverity getDefaultSeverity() {
        return defaultSeverity;
    }

    public EnumTypes.AlarmSeverity getNewSeverity() {
        return newSeverity;
    }

    // PROPERTY GETTERS

    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public SimpleBooleanProperty isFilteredProperty() {
        return isFiltered;
    }

    public StringProperty defaultSeverityProperty() {
        return new SimpleStringProperty(Utilities.capitalizeWordsFirstLetter(defaultSeverity.toString(), "_"));
    }

    public StringProperty newSeverityProperty() {
        return new SimpleStringProperty(Utilities.capitalizeWordsFirstLetter(newSeverity.toString(), "_"));
    }

    public StringProperty newDisplayedSeverityProperty() {
        return new SimpleStringProperty(newSeverity.getDisplayedValue());
    }

    // SETTERS

    public void setId(int aInId) {
        id.set(aInId);
    }

    public void setName(String aInName) {
        name.set(aInName);
    }

    public void setIsFiltered(Boolean aInIsFiltered) {
        isFiltered.set(aInIsFiltered);
    }

    public void setDefaultSeverity(EnumTypes.AlarmSeverity aInSeverity) {
        defaultSeverity = aInSeverity;
    }

    public void setNewSeverity(EnumTypes.AlarmSeverity aInSeverity) {
        newSeverity = aInSeverity;
    }

    // CONSTRUCTORS

    /**
     * Builds the displayed configured alarm from alarm configuration
     * @param aInAlarmConfiguration Alarm configuration
     */
    public ConfiguredAlarm(AlarmConfiguration aInAlarmConfiguration) {

        id = new SimpleIntegerProperty(aInAlarmConfiguration.getId());
        name = new SimpleStringProperty(Alarm.getAlarmDictionaryInformation(id.get(), "name"));
        isFiltered = new SimpleBooleanProperty(aInAlarmConfiguration.getIsFiltered());
        defaultSeverity = EnumTypes.AlarmSeverity.valueOf(Utilities.separateAndCapitalizeWords(Alarm.getAlarmDictionaryInformation(id.get(), "severity"), "_"));
        newSeverity = (aInAlarmConfiguration.getNewSeverity() != null) ? aInAlarmConfiguration.getNewSeverity() : defaultSeverity;

    }

    public ConfiguredAlarm(Element aInAlarmConfiguration) {

        id = new SimpleIntegerProperty(Integer.valueOf(aInAlarmConfiguration.getAttributeValue("id")));
        name = new SimpleStringProperty(aInAlarmConfiguration.getChildText("name"));
        defaultSeverity = EnumTypes.AlarmSeverity.valueOf(Utilities.separateAndCapitalizeWords(aInAlarmConfiguration.getChildText("severity"), "_"));
        isFiltered = new SimpleBooleanProperty(false);
        newSeverity = defaultSeverity;
        for (AlarmConfiguration lAlarmConfiguration: Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations()) {
            if (lAlarmConfiguration.getId().equals(getId())) {
                isFiltered = new SimpleBooleanProperty(lAlarmConfiguration.getIsFiltered());
                if (lAlarmConfiguration.getNewSeverity() != null) newSeverity = lAlarmConfiguration.getNewSeverity();
            }
        }

    }
}
