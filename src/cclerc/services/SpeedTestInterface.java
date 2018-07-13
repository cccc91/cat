package cclerc.services;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.model.SpeedTestError;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Services called during a speed test, i.e. a download followed by a upload
 */
public interface SpeedTestInterface {

    // Invoked when speed test begins, i.e. before download
    public void reportStartTest();
    // Invoked when speed test finishes, i.e after upload (after reportResult), or when an error occurs (after reportError), but not when test is interrupted
    public void reportStopTest();
    // Invoked when speed test is interrupted by user - After an interruption, speed test is no more usable and needs to be re-instantiated
    public void reportInterruption();
    // Invoked when starting download and when starting upload
    public void reportStartTransfer(EnumTypes.SpeedTestMode aInTransferMode);
    // Invoked when download or upload progress is reported (but not on completion)
    public void reportProgress(EnumTypes.SpeedTestMode aInTransferMode, float aInProgress, Map<Integer, BigDecimal> aInBitRate, Map<Integer, BigDecimal> aInOctetRate);
    // Invoked when download or upload finishes
    public void reportResult(EnumTypes.SpeedTestMode aInTransferMode, Map<Integer, BigDecimal> aInBitRate, Map<Integer, BigDecimal> aInOctetRate);
    // Invoked when upload finishes, after reportResult and before reportStopTest
    public void reportFinalResult(List<Map<Integer, BigDecimal>> aInBitRates, List<Map<Integer, BigDecimal>> aInOctetRates);
    // Invoked when an error occurs - After an error, speed test is no more usable and needs to be re-instantiated
    public void reportError(EnumTypes.SpeedTestMode aInTransferMode, SpeedTestError aInSpeedTestError, String aInErrorMessage);
    // Invoked when download or upload finishes, after reportResult and before reportStopTest
    public void storeResult(EnumTypes.SpeedTestMode aInSpeedTestMode, long aInStartTime, BigDecimal aInBitRate, BigDecimal aInOctetRate);

}
