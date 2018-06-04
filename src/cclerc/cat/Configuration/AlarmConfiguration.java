package cclerc.cat.Configuration;

import cclerc.services.EnumTypes;
import cclerc.services.Utilities;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

/**
 * Internal class for alarm configuration
 */
public class AlarmConfiguration extends AbstractConfiguration {

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(Arrays.asList("id", "isFiltered", "newSeverity"));

    // Accessed by introspection
    protected static final Boolean DEFAULT_IS_FILTERED = false;

    private Integer id;
    protected Boolean isFiltered;
    private EnumTypes.AlarmSeverity newSeverity;

    // SETTERS

    public void setId(String aInId) throws NumberFormatException {
        if (aInId == null)  throw new NullPointerException();
        if ((aInId != null) && !Integer.valueOf(aInId).equals(id)) {
            id = Integer.valueOf(aInId);
        }
    }

    public void setIsFiltered(String aInIsFiltered) throws IllegalArgumentException {
        if ((aInIsFiltered != null) && ! Boolean.valueOf(aInIsFiltered).equals(isFiltered)) {
            isFiltered = Boolean.valueOf(aInIsFiltered);
        }
    }

    public void setNewSeverity(String aInNewSeverity) throws IllegalArgumentException {
        String lNewSeverity = (aInNewSeverity != null) ? Utilities.separateAndCapitalizeWords(aInNewSeverity, "_") : "";
        if ((aInNewSeverity != null) && !EnumTypes.AlarmSeverity.valueOf(lNewSeverity).equals(newSeverity)) {
            newSeverity = EnumTypes.AlarmSeverity.valueOf(lNewSeverity);
        }
    }

    // GETTERS

    public Integer getId() {
        return id;
    }

    public Boolean getIsFiltered() {
        return isFiltered;
    }

    public EnumTypes.AlarmSeverity getNewSeverity() {
        return newSeverity;
    }

    // CONSTRUCTORS

    public AlarmConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes (ignore element on error)
        super("alarmConfiguration", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, true);

    }

    public AlarmConfiguration(Configuration aInConfiguration, Integer aInId, Boolean aInIsFiltered, EnumTypes.AlarmSeverity aInNewSeverity) {
        super("alarmConfiguration", aInConfiguration, "alarm", ATTRIBUTE_NAMES);
        id = aInId;
        isFiltered = aInIsFiltered;
        newSeverity = aInNewSeverity;
    }

    public AlarmConfiguration(Configuration aInConfiguration,  Integer aInId, EnumTypes.AlarmSeverity aInNewSeverity) {
        super("alarmConfiguration", aInConfiguration, "alarm", ATTRIBUTE_NAMES);
        id = aInId;
        newSeverity = aInNewSeverity;
    }

    public AlarmConfiguration(Configuration aInConfiguration, Integer aInId, Boolean aInIsFiltered) {
        super("alarmConfiguration", aInConfiguration, "alarm", ATTRIBUTE_NAMES);
        id = aInId;
        isFiltered = aInIsFiltered;
    }

}
