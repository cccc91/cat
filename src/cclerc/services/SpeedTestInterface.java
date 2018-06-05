package cclerc.services;

import fr.bmartel.speedtest.SpeedTestReport;

public interface SpeedTestInterface {

    public void printMessage(String aInMessage);
    public void printError(String aInError);
    public void storeResult(SpeedTestReport report);
    public String getType();

}
