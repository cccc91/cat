package cclerc.cat.Configuration;

import cclerc.services.Preferences;

public class ConfigurationChangeObserver implements ConfigurationChangeListener {

    @Override
    /**
     * Save configuration when it changes if auto save is audibleEnabled
     */
    public void configurationChanged(ChangeType aInChangeType, Object aInObjectChanged, String aInAttributeChanged, Object aInNewAttributeValue) {
        //System.out.println(aInChangeType.toString().toLowerCase() + " value of attribute " + aInAttributeChanged + " of object " + aInObjectChanged.getClass().getName() + " to value " + aInNewAttributeValue);
        if (Preferences.getInstance().getBooleanValue("autoSaveConfiguration") && !Configuration.getCurrentConfiguration().isSameAs(Configuration.getInitialConfiguration())) {
            Configuration.getCurrentConfiguration().save();
            Configuration.resetInitialConfiguration();
        }
    }

}
