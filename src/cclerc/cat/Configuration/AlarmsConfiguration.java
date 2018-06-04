package cclerc.cat.Configuration;

import cclerc.services.Display;
import cclerc.services.Utilities;
import org.jdom2.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Internal class for alarms configuration
 */
public class AlarmsConfiguration extends AbstractConfiguration {

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>();

    private List<AlarmConfiguration> alarmConfigurations = new ArrayList<>();
    private AudibleAlarmsConfiguration audibleAlarmsConfiguration;

    // SETTERS

    public void addAlarmConfiguration(AlarmConfiguration aInAlarmConfigurations) {
        alarmConfigurations.add(aInAlarmConfigurations);
        aInAlarmConfigurations.setParentConfiguration(this);
    }

    public void removeAlarmConfiguration(AlarmConfiguration aInAlarmConfigurations) {
        alarmConfigurations.remove(aInAlarmConfigurations);
    }

    public void removeAlarmConfiguration(int lIndex) {
        alarmConfigurations.remove(lIndex);
    }

    public void removeAllAlarmsConfiguration() {
        alarmConfigurations.clear();
    }

    public void setAudibleAlarmsConfiguration(AudibleAlarmsConfiguration aInAudibleAlarmsConfiguration) throws NullPointerException {
        if (aInAudibleAlarmsConfiguration == null)  throw new NullPointerException();
        audibleAlarmsConfiguration = aInAudibleAlarmsConfiguration;
        audibleAlarmsConfiguration.setParentConfiguration(this);
    }

    // GETTERS

    public List<AlarmConfiguration> getAlarmConfigurations() {
        return alarmConfigurations;
    }

    public AudibleAlarmsConfiguration getAudibleAlarmsConfiguration() {
         return audibleAlarmsConfiguration;
    }

    // CONSTRUCTOR

    public AlarmsConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes (ignore element on error)
        super("alarmsConfiguration", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, true);

        if (aInElement != null) {

            // Add audible element
            Element lAudibleAlarmsConfigurationElement = aInElement.getChild("audible");
            if (lAudibleAlarmsConfigurationElement != null)
                setAudibleAlarmsConfiguration(new AudibleAlarmsConfiguration(aInConfiguration, aInConfigurationFile, aInDisplayError, lAudibleAlarmsConfigurationElement));

            // Add all alarm elements
            for (Element lAlarmConfigurationElement : aInElement.getChildren("alarm")) {
                // Add alarm only if it contains no error
                try {
                    AlarmConfiguration lAlarmConfiguration =
                            new AlarmConfiguration(aInConfiguration, configurationFile, displayError, lAlarmConfigurationElement);
                    addAlarmConfiguration(lAlarmConfiguration);
                } catch (Exception e) {
                    Display.logUnexpectedError(e);
                }
            }

            // Sort alarms by id
            Collections.sort(alarmConfigurations, new Comparator<AlarmConfiguration>() {
                @Override
                public int compare(AlarmConfiguration o1, AlarmConfiguration o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
        }

    }

    public AlarmsConfiguration(Configuration aInConfiguration) {
        super("alarmsConfiguration", aInConfiguration, "alarms", ATTRIBUTE_NAMES);
        setAudibleAlarmsConfiguration(new AudibleAlarmsConfiguration(aInConfiguration));
    }

    // METHODS

    public Element save() {

        Element lAlarmsConfigurationElement = super.save();

        // Save audibleAlarmsConfiguration
        if (audibleAlarmsConfiguration != null) lAlarmsConfigurationElement.addContent(audibleAlarmsConfiguration.save());

        // Save all alarmConfiguration
        for (AlarmConfiguration lAlarmConfiguration : alarmConfigurations) {
            lAlarmsConfigurationElement.addContent(lAlarmConfiguration.save());
        }

        return lAlarmsConfigurationElement;

    }

    public boolean hasSameAlarmConfigurations(AlarmsConfiguration aInAlarmsConfiguration) {

        // Compare alarmConfigurations - If they have not the same size, configurations are not the same. If they have, compare each alarm configuration
        boolean lSameAlarmConfigurations = (alarmConfigurations == null && aInAlarmsConfiguration == null) ||
                (alarmConfigurations != null && aInAlarmsConfiguration != null && alarmConfigurations.size() == aInAlarmsConfiguration.getAlarmConfigurations().size());
        if (lSameAlarmConfigurations && alarmConfigurations != null) {
            for (AlarmConfiguration lLocalAlarmConfiguration : alarmConfigurations) {
                boolean lFound = false;
                for (AlarmConfiguration lRemoteAlarmConfigurations : aInAlarmsConfiguration.getAlarmConfigurations()) {
                    if (lLocalAlarmConfiguration.isSameAs(lRemoteAlarmConfigurations)) {
                        lFound = true;
                        break;
                    }
                }
                if (!lFound) {
                    lSameAlarmConfigurations = false;
                    break;
                }
            }
        }

        return lSameAlarmConfigurations;

    }

    public boolean isSameAs(AlarmsConfiguration aInAlarmsConfiguration) {

        return super.isSameAs(aInAlarmsConfiguration) &&
                ((audibleAlarmsConfiguration == null && aInAlarmsConfiguration.getAudibleAlarmsConfiguration() == null) ||
                        (audibleAlarmsConfiguration != null && audibleAlarmsConfiguration.isSameAs(aInAlarmsConfiguration.getAudibleAlarmsConfiguration()))) &&
               hasSameAlarmConfigurations(aInAlarmsConfiguration);

    }

}
