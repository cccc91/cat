package cclerc.cat.Configuration;

public interface ConfigurationChangeListener {
    public enum ChangeType {CREATE, DELETE, MODIFY};

    /**
     * Listener invoked on a configuration element change
     * @param aInChangeType         Change type (create, delete, modify)
     * @param aInObjectChanged      Object that has been created, deleted or modified
     * @param aInAttributeChanged   Attribute the has changed in chase of modification
     * @param aInNewAttributeValue  New value of attribute that has changed in case of modification
     */
    public void configurationChanged(ChangeType aInChangeType, Object aInObjectChanged, String aInAttributeChanged, Object aInNewAttributeValue);
}
