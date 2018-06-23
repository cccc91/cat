package cclerc.services;

import fr.bmartel.speedtest.SpeedTestReport;

public interface SpeedTestInterface {

    public void printProgress(String aInMessage);
    public void printResult(String aInMessage);
    public void printError(String aInMessage);
    public void storeResult(SpeedTestReport report);
    public String getType();

}
