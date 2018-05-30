package cclerc.cat.Configuration;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom2.Element;

/**
 * Internal class for audible alarms configuration
 */
public class AudibleAlarmsConfiguration extends AbstractConfiguration {

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(Arrays.asList("audibleEnabled", "muteStartTime", "muteEndTime", "critical", "major", "minor", "warning", "info", "clear"));

    // Accessed by introspection
    protected static final Boolean DEFAULT_AUDIBLE_ENABLED = false;
    protected static final String DEFAULT_CRITICAL = "resources/sounds/critical.mp3";
    protected static final String DEFAULT_MAJOR = "resources/sounds/major.mp3";
    protected static final String DEFAULT_MINOR = "resources/sounds/minor.mp3";
    protected static final String DEFAULT_WARNING = "resources/sounds/warning.mp3";
    protected static final String DEFAULT_INFO = "resources/sounds/info.mp3";
    protected static final String DEFAULT_CLEAR = "resources/sounds/clear.mp3";
    protected static final String DEFAULT_MUTE_START_TIME = "22:00";
    protected static final String DEFAULT_MUTE_END_TIME = "09:00";

    protected Boolean audibleEnabled;
    protected String critical;
    protected String major;
    protected String minor;
    protected String warning;
    protected String info;
    protected String clear;
    protected String muteStartTime;
    protected String muteEndTime;

    // SETTERS

    public void setAudibleEnabled(String aInEnabled) throws IllegalArgumentException {
        if ((aInEnabled != null) && !Boolean.valueOf(aInEnabled).equals(audibleEnabled)) {
            audibleEnabled = Boolean.valueOf(aInEnabled);
        }
    }

    public void setMuteStartTime(String aInTime) throws ParseException {
        if (aInTime != null) {
            SimpleDateFormat lSimpleDateFormatter = new SimpleDateFormat("HH:mm");
            lSimpleDateFormatter.parse(aInTime);
            if (!aInTime.equals(muteStartTime)) {
                muteStartTime = aInTime;
            }
        }
    }

    public void setMuteEndTime(String aInTime) throws ParseException {
        if (aInTime != null) {
            SimpleDateFormat lSimpleDateFormatter = new SimpleDateFormat("HH:mm");
            lSimpleDateFormatter.parse(aInTime);
            if (!aInTime.equals(muteEndTime)) {
                muteEndTime = aInTime;
            }
        }
    }

    public void setCritical(String aInFile) throws FileNotFoundException {
        String lNewFile = computeFileName(aInFile, critical);
        if (!lNewFile.equals(critical)) {
            critical = lNewFile;
        }

    }

    public void setMajor(String aInFile) throws FileNotFoundException {
        String lNewFile = computeFileName(aInFile, major);
        if (!lNewFile.equals(major)) {
            major = lNewFile;
        }
    }

    public void setMinor(String aInFile) throws FileNotFoundException {
        String lNewFile = computeFileName(aInFile, minor);
        if (!lNewFile.equals(minor)) {
            minor = lNewFile;
        }
    }

    public void setWarning(String aInFile) throws FileNotFoundException {
        String lNewFile = computeFileName(aInFile, warning);
        if (!lNewFile.equals(warning)) {
            warning = lNewFile;
        }
    }

    public void setInfo(String aInFile) throws FileNotFoundException {
        String lNewFile = computeFileName(aInFile, info);
        if (!lNewFile.equals(info)) {
            info = lNewFile;
        }
    }

    public void setClear(String aInFile) throws FileNotFoundException {
        String lNewFile = computeFileName(aInFile, clear);
        if (!lNewFile.equals(clear)) {
            clear = lNewFile;
        }
    }

    // GETTERS

    public Boolean getAudibleEnabled() {
        return audibleEnabled;
    }

    public String getMuteStartTime() {
        return muteStartTime;
    }

    public Integer getMuteStartHour() {
        return Integer.valueOf(muteStartTime.replaceAll(":.*", ""));
    }

    public Integer getMuteStartMinute() {
        return Integer.valueOf(muteStartTime.replaceAll(".*:", ""));
    }

    public static Integer getDefaultMuteStartHour() {
        return Integer.valueOf(DEFAULT_MUTE_START_TIME.replaceAll(":.*", ""));
    }

    public static Integer getDefaultMuteStartMinute() {
        return Integer.valueOf(DEFAULT_MUTE_START_TIME.replaceAll(".*:", ""));
    }

    public String getMuteEndTime() {
        return muteEndTime;
    }

    public Integer getMuteEndHour() {
        return Integer.valueOf(muteEndTime.replaceAll(":.*", ""));
    }

    public Integer getMuteEndMinute() {
        return Integer.valueOf(muteEndTime.replaceAll(".*:", ""));
    }

    public static Integer getDefaultMuteEndHour() {
        return Integer.valueOf(DEFAULT_MUTE_END_TIME.replaceAll(":.*", ""));
    }

    public static Integer getDefaultMuteEndMinute() {
        return Integer.valueOf(DEFAULT_MUTE_END_TIME.replaceAll(".*:", ""));
    }

    public String getCritical() {
        return critical;
    }

    public String getMajor() {
        return major;
    }

    public String getMinor() {
        return minor;
    }

    public String getWarning() {
        return warning;
    }

    public String getInfo() {
        return info;
    }

    public String getClear() {
        return clear;
    }

    // CONSTRUCTOR

    public AudibleAlarmsConfiguration(Configuration aInConfiguration, String aInConfigurationFile, boolean aInDisplayError, Element aInElement)
            throws Exception {

        // Add attributes (ignore element on error)
        super("audibleAlarms", aInConfiguration, aInConfigurationFile, aInDisplayError, aInElement, ATTRIBUTE_NAMES, false);

    }

}
