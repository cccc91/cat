package cclerc.cat.model;

import cclerc.cat.Configuration.AlarmConfiguration;
import cclerc.cat.Configuration.Configuration;
import cclerc.services.Display;
import cclerc.services.EnumTypes;
import cclerc.services.LocaleUtilities;
import cclerc.services.Utilities;
import javafx.beans.property.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Alarm {

    // Class variables
    private volatile static Integer lastInternalId = 0;

    // AlarmConfiguration properties
    private Integer internalId;

    private EnumTypes.AlarmState state;
    private EnumTypes.AlarmSeverity severity;
    private IntegerProperty id;
    private StringProperty name;
    private StringProperty site;
    private EnumTypes.AlarmObjectType objectType;
    private StringProperty objectName;
    private IntegerProperty occurrences;
    private ObjectProperty<Date> raiseDate;
    private ObjectProperty<Date> modificationDate;
    private ObjectProperty<Date> clearDate;
    private EnumTypes.AlarmType type;
    private StringProperty probableCause;
    private StringProperty additionalInformation;
    private StringProperty remedialAction;

    // Alarms dictionary content
    private static HashMap<Integer, Element> alarms = new HashMap<>();

    // GETTERS

    public Integer getInternalId() {
        return internalId;
    }

    public EnumTypes.AlarmState getState() {
        return state;
    }

    public EnumTypes.AlarmSeverity getSeverity() {
        return severity;
    }

    public int getId() {
        return id.get();
    }

    public String getName() {
        return name.get();
    }

    public String getSite() {
        return site.get();
    }

    public EnumTypes.AlarmObjectType getObjectType() {
        return objectType;
    }

    public String getObjectName() {
        return objectName.get();
    }

    public int getOccurrences() {
        return occurrences.get();
    }

    public Date getRaiseDate() {
        return raiseDate.get();
    }

    public Date getModificationDate() {
        return modificationDate.get();
    }

    public Date getClearDate() {
        return clearDate.get();
    }

    public EnumTypes.AlarmType getType() {
        return type;
    }

    public String getProbableCause() {
        return probableCause.get();
    }

    public String getAdditionalInformation() {
        return additionalInformation.get();
    }

    public String getRemedialAction() {
        return remedialAction.get();
    }

    // PROPERTY GETTERS

    public StringProperty stateProperty() {
        return new SimpleStringProperty(Utilities.capitalizeWordsFirstLetter(state.toString(), "_"));
    }

    public StringProperty severityProperty() {
        return new SimpleStringProperty(Utilities.capitalizeWordsFirstLetter(severity.toString(), "_"));
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty siteProperty() {
        return site;
    }

    public StringProperty objectTypeProperty() {
        return new SimpleStringProperty(Utilities.capitalizeWordsFirstLetter(objectType.toString(), "_"));
    }

    public StringProperty objectNameProperty() {
        return objectName;
    }

    public IntegerProperty occurrencesProperty() {
        return occurrences;
    }

    public ObjectProperty<Date> raiseDateProperty() {
        return raiseDate;
    }

    public ObjectProperty<Date> modificationDateProperty() {
        return modificationDate;
    }

    public ObjectProperty<Date> clearDateProperty() {
        return clearDate;
    }

    public StringProperty typeProperty() {
        return new SimpleStringProperty(Utilities.capitalizeWordsFirstLetter(type.toString(), "_"));
    }

    public StringProperty probableCauseProperty() {
        return probableCause;
    }

    public StringProperty additionalInformationProperty() {
        return additionalInformation;
    }

    public StringProperty remedialActionProperty() {
        return remedialAction;
    }

    // SETTERS

    public void setState(EnumTypes.AlarmState aInState) {
        state = aInState;
    }

    public void setSeverity(EnumTypes.AlarmSeverity aInSeverity) {
        severity = aInSeverity;
    }

    public void setId(int aInId) {
        id.set(aInId);
    }

    public void setName(String aInName) {
        name.set(aInName);
    }

    public void setSite(String aInSite) {
        site.set(aInSite);
    }

    public void setObjectType(EnumTypes.AlarmObjectType aInObjectType) {
        objectType = aInObjectType;
    }

    public void setObjectName(String aInObjectName) {
        objectName.set(aInObjectName);
    }

    public void setOccurrences(int aInOccurrences) {
        occurrences.set(aInOccurrences);
    }

    public void setRaiseDate(Date aInRaiseDate) {
        raiseDate.set(aInRaiseDate);
    }

    public void setModificationDate(Date aInModificationDate) {
        modificationDate.set(aInModificationDate);
    }

    public void setClearDate(Date aInClearDate) {
        clearDate.set(aInClearDate);
    }

    public void setType(EnumTypes.AlarmType aInType) {
        type = aInType;
    }

    public void setProbableCause(String aInProbableCause) {
        probableCause.set(aInProbableCause);
    }

    public void setAdditionalInformation(String aInAdditionalInfo) {
        additionalInformation.set(aInAdditionalInfo);
    }

    public void setRemedialAction(String aInRemedialAction) {
        remedialAction.set(aInRemedialAction);
    }

    // CONSTRUCTOR

    /**
     * AlarmConfiguration constructor
     * @param aInId Id of the alarm to raise
     */
    public Alarm(Integer aInId) {

        synchronized (this){
            lastInternalId++;
            internalId = lastInternalId;
        }

        // Search if alarm properties have been redefined in configuration
        id = new SimpleIntegerProperty(aInId);
        state = EnumTypes.AlarmState.RAISED;
        severity = (getNewSeverity() == null) ?
            EnumTypes.AlarmSeverity.valueOf(Utilities.separateAndCapitalizeWords(getAlarmDictionaryInformation(aInId, "severity"), "_")) :
            getNewSeverity();
        name = new SimpleStringProperty(getAlarmDictionaryInformation(aInId, "name"));
        site = new SimpleStringProperty("");
        objectType = EnumTypes.AlarmObjectType.valueOf(Utilities.separateAndCapitalizeWords(getAlarmDictionaryInformation(aInId, "objectType"), "_"));
        objectName = new SimpleStringProperty("");
        occurrences = new SimpleIntegerProperty(1);
        raiseDate = new SimpleObjectProperty<>(new Date());
        modificationDate = new SimpleObjectProperty<>(new Date());
        clearDate = new SimpleObjectProperty<>(null);
        type = EnumTypes.AlarmType.valueOf(Utilities.separateAndCapitalizeWords(getAlarmDictionaryInformation(aInId, "type"), "_"));
        probableCause = new SimpleStringProperty(getAlarmDictionaryInformation(aInId, "probableCause"));
        additionalInformation = new SimpleStringProperty(getAlarmDictionaryInformation(aInId, "additionalInformation"));
        remedialAction = new SimpleStringProperty(getAlarmDictionaryInformation(aInId, "remedialAction"));

        playSound();

    }

    // PRIVATE METHODS

    /**
     * Plays sound corresponding to current alarm severity if alarm is not filtered and sounds are not disabled in current time frame
     */
    private void playSound() {

        try {
            if (!isFiltered() && soundsAreAllowed()) {
                Utilities.playSound(getSoundFile(state, severity));
            }
        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }

    }

    /**
     * Returns the sound file to be played for a specific alarm severity or state, or null if audible alarms are disabled
     * @param aInState    State of the alarm
     * @param aInSeverity Severity of the alarm
     * @return Sound file to be played
     */
    private String getSoundFile(EnumTypes.AlarmState aInState, EnumTypes.AlarmSeverity aInSeverity) throws FileNotFoundException {

        try {
            if (aInState.equals(EnumTypes.AlarmState.CLEARED)) {
                return Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration().getClear();
            } else {
                return Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration().get(aInSeverity.toString().toLowerCase());
            }
        } catch (Exception e) {
            throw new FileNotFoundException();
        }

    }

    /**
     * Determines if sounds can be played or not at current time
     * @return true if sound can be played
     */
    private boolean soundsAreAllowed() {

        if (Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration().getAudibleEnabled()) return false;

        if (Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration().getMuteStartTime() == null ||
            Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration().getMuteEndTime() == null) return true;

        SimpleDateFormat lSimpleDateFormatter = new SimpleDateFormat("HH:mm");
        LocalDateTime lNow = LocalDateTime.now();

        try {
            long lMuteStartTime = lSimpleDateFormatter.parse(Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration().getMuteStartTime()).getTime();
            long lMuteEndTime = lSimpleDateFormatter.parse(Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAudibleAlarmsConfiguration().getMuteEndTime()).getTime();
            long lCurrentTime =  lSimpleDateFormatter.parse(String.format("%02d", lNow.getHour()) + ":" + String.format("%02d", lNow.getMinute())).getTime();
            long lDayEnd = lSimpleDateFormatter.parse("24:00").getTime();
            long lDayStart = lSimpleDateFormatter.parse("00:00").getTime();

            if (lMuteStartTime < lMuteEndTime) {
                return !(lCurrentTime >= lMuteStartTime && lCurrentTime <= lMuteEndTime);
            } else {
                return !((lCurrentTime >= lMuteStartTime && lCurrentTime <= lDayEnd) || (lCurrentTime >= lDayStart && lCurrentTime <= lMuteEndTime));
            }
        } catch (ParseException e) {
            // Cannot happen since date has been checked when reading config file
            return false;
        }

    }

    /**
     * Returns alarm new severity
     * @return New alarm severity
     */
    private EnumTypes.AlarmSeverity getNewSeverity() {
        for (AlarmConfiguration lAlarmConfiguration : Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations()) {
            if (lAlarmConfiguration.getId() == id.get()) return lAlarmConfiguration.getNewSeverity();
        }
        return null;
    }

    // PUBLIC METHODS

    public static void loadAlarmDictionary()  throws IOException, JDOMException {

        SAXBuilder lSaxBuilder = new SAXBuilder();

        // Load configuration file
        String lPath = Alarm.class.getClassLoader().getResource("resources/alarms").toExternalForm();
        Document lDocument = lSaxBuilder.build(lPath +  "/alarmDictionary_" + LocaleUtilities.getInstance().getCurrentLocale().getLanguage() + ".xml");
        Element lRoot = lDocument.getRootElement();

        // Build alarms list
        List<Element> lAlarms = lRoot.getChildren("alarm");
        if (lAlarms != null) {

            // Parse alarms
            for (Element lAlarmElement: lAlarms) {
                alarms.put(Integer.valueOf(lAlarmElement.getAttributeValue("id")), lAlarmElement);
            }

        }

    }

    /**
     * Gets alarm dictionary
     * @return alarm dictionary
     */
    public static HashMap<Integer, Element> getAlarmDictionary() {
        return alarms;
    }

    /**
     * Gets specific field of a given alarm in the alarm dictionary
     * @param aInId     alarm id to be retrieved
     * @param aInField  field for which the value is required
     * @return value of the field for the specified alarm
     */
    public static String getAlarmDictionaryInformation(Integer aInId, String aInField) {
        return alarms.get(aInId).getChildText(aInField);
    }

    /**
     * Indicates if alarm is filtered
     * @return Filter state
     */
    public boolean isFiltered() {
        for (AlarmConfiguration lAlarmConfiguration : Configuration.getCurrentConfiguration().getAlarmsConfiguration().getAlarmConfigurations()) {
            if (lAlarmConfiguration.getId() == id.get()) return lAlarmConfiguration.getIsFiltered();
        }
        return false;
    }

    /**
     * Clears an alarm
     */
    public void clear() {

        clearDate = new SimpleObjectProperty<>(new Date());
        state = EnumTypes.AlarmState.CLEARED;
        playSound();

    }

    /**
     * Acknowledges an alarm
     */
    public void acknowledge() {
        modificationDate = new SimpleObjectProperty<>(new Date());
        state = EnumTypes.AlarmState.ACKNOWLEDGED;
    }

    /**
     * Un-acknowledges an alarm
     */
    public void unAcknowledge() {
        modificationDate = new SimpleObjectProperty<>(new Date());
        state = EnumTypes.AlarmState.RAISED;
    }

    /**
     * Increment alarm occurrences
     */
    public void incrementOccurrences() {

        modificationDate = new SimpleObjectProperty<>(new Date());
        occurrences.set(occurrences.get() + 1);
        playSound();

    }

    /**
     * Appends additional information to an alarm
     * @param aInAdditionalInformation Additional information to appen
     */
    public void appendAdditionalInformation(String aInAdditionalInformation) {
        additionalInformation.set(
                (additionalInformation.isNull().getValue() || additionalInformation.get().equals("")) ?
                        aInAdditionalInformation :
                        aInAdditionalInformation + "\n" + additionalInformation.get());
    }

    /**
     * Changes severity of an alarm
     * @param aInNewSeverity
     */
    public void changeSeverity(EnumTypes.AlarmSeverity aInNewSeverity) {
        modificationDate = new SimpleObjectProperty<>(new Date());
        severity = aInNewSeverity;
    }

}
