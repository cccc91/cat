package cclerc.services;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.IRepeatListener;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.model.SpeedTestMode;
import fr.bmartel.speedtest.model.UploadStorageType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeedTest {

    private boolean testRunning = false;

    private SpeedTestSocket speedTestSocket = new SpeedTestSocket();
    private SpeedTestInterface speedTestInterface;
    private boolean interrupted;
    private BigDecimal bitRate = new BigDecimal(0);
    private BigDecimal octetRate = new BigDecimal(0);
    private List<Map<Integer, BigDecimal>> bitRates = new ArrayList<>();
    private List<Map<Integer, BigDecimal>> octetRates = new ArrayList<>();
    private List<BigDecimal> rawBitRates = new ArrayList<>();
    private List<BigDecimal> rawOctetRates = new ArrayList<>();
    private int count = 0;
    private long startTime;
    private SpeedTestMode mode;

    /**
     * Speed test constructor
     * @param aInSpeedTestInterface Interface to be called for reporting progress, completion and error
     * @param aInUseProxy           // TODO: use configuration ?
     */
    public SpeedTest(SpeedTestInterface aInSpeedTestInterface, boolean aInUseProxy) {

        speedTestInterface = aInSpeedTestInterface;
        interrupted = false; // Must be set to false at each re-instantiation (re-instantiation is done after interruption)

        // Set proxy if needed
        Proxy lProxy = Proxy.NO_PROXY;
        if (aInUseProxy) {
            lProxy = Network.findHttpProxy(Constants.SPEED_TEST_GET_SERVERS_URL);
            speedTestSocket.setProxyServer(lProxy.toString().replace(" @ ", "://"));
        }

        // Initialise speed test socket
        speedTestSocket.setSocketTimeout(
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_SOCKET_TIMEOUT_PREFERENCE, Constants.DEFAULT_SPEED_TEST_SOCKET_TIMEOUT));
        speedTestSocket.setDownloadSetupTime(
                Preferences.getInstance().getLongValue(Constants.SPEED_TEST_DOWNLOAD_SETUP_TIME_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DOWNLOAD_SETUP_TIME));
        speedTestSocket.setUploadSetupTime(
                Preferences.getInstance().getLongValue(Constants.SPEED_TEST_UPLOAD_SETUP_TIME_PREFERENCE, Constants.DEFAULT_SPEED_TEST_UPLOAD_SETUP_TIME));
        speedTestSocket.setUploadStorageType(UploadStorageType.FILE_STORAGE);

        // Add a listener to wait for speed test completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(SpeedTestReport aInReport) {
            }

            @Override
            public void onError(SpeedTestError aInSpeedTestError, String aInErrorMessage) {
                if (!interrupted && testRunning) {
                    testRunning = false;
                    speedTestSocket.forceStopTask();
                    speedTestSocket.closeSocket();
                    aInSpeedTestInterface.reportError(startTime, convertSpeedTestMode(speedTestSocket.getSpeedTestMode()), aInSpeedTestError, aInErrorMessage);
                    speedTestInterface.reportStopTest();
                    fillRates();
                    speedTestInterface.reportFinalResult(startTime, bitRates, octetRates, rawBitRates, rawOctetRates);
                    speedTestInterface.storeResult(convertSpeedTestMode(mode), startTime, BigDecimal.ZERO, BigDecimal.ZERO);
                    if (mode.equals(SpeedTestMode.DOWNLOAD)) speedTestInterface.storeResult(EnumTypes.SpeedTestMode.UPLOAD, startTime, BigDecimal.ZERO, BigDecimal.ZERO);
                    resetRates();
                }
            }

            @Override
            public void onProgress(float percent, SpeedTestReport aInReport) {
            }

        });

    }

    public boolean isInterrupted() {
        return interrupted;
    }

    // PRIVATE METHODS

    private void fillRates() {
        for (int i = bitRates.size(); i <= 1; i++) bitRates.add(convertToBestUnit(BigDecimal.ZERO));
        for (int i = octetRates.size(); i <= 1; i++) octetRates.add(convertToBestUnit(BigDecimal.ZERO));
        for (int i = rawBitRates.size(); i <= 1; i++) rawBitRates.add(BigDecimal.ZERO);
        for (int i = rawOctetRates.size(); i <= 1; i++) rawOctetRates.add(BigDecimal.ZERO);
    }

    private void resetRates() {
        bitRates.clear(); octetRates.clear(); rawBitRates.clear(); rawOctetRates.clear();
    }

    /**
     * Converts mode from internal speed test representation to application representation
     * @param aInSpeedTestMode Speed test mode internal representation
     * @return Speed test mode application representation
     */
    private EnumTypes.SpeedTestMode convertSpeedTestMode(SpeedTestMode aInSpeedTestMode) {
        switch (aInSpeedTestMode) {
            case DOWNLOAD:
                return EnumTypes.SpeedTestMode.DOWNLOAD;
            case UPLOAD:
                return EnumTypes.SpeedTestMode.UPLOAD;
            default:
                return EnumTypes.SpeedTestMode.DOWNLOAD;

        }
    }

    /**
     * Displays current transfer rate and stores values for average computation on completion
     * @param aInReport Progress report
     */
    private void processProgressReport(SpeedTestReport aInReport) {

        if (testRunning) {
            Map<Integer, BigDecimal> lBitRate = convertToBestUnit(aInReport.getTransferRateBit());
            Map<Integer, BigDecimal> lOctetRate = convertToBestUnit(aInReport.getTransferRateOctet());
            // Ignore first reports for average
            if (aInReport.getProgressPercent() > 1f) {
                count++;
                bitRate = bitRate.add(aInReport.getTransferRateBit());
                octetRate = octetRate.add(aInReport.getTransferRateOctet());
            }
            speedTestInterface.reportProgress(convertSpeedTestMode(aInReport.getSpeedTestMode()), aInReport.getProgressPercent(), lBitRate, aInReport.getTransferRateBit(),
                                              lOctetRate, aInReport.getTransferRateOctet());
        }

    }

    /**
     * Displays final transfer rate by computing it from average of the different progresses
     * @param aInReport Final progress report
     */
    private void processCompletionReport(SpeedTestReport aInReport) {

        if (testRunning) {
            // Don't take into account completion report, just use average of progress reports
            if (count != 0) {
                bitRate = bitRate.divide(new BigDecimal(count), 2);
                octetRate = octetRate.divide(new BigDecimal(count), 2);
            }
            Map<Integer, BigDecimal> lBitRate = convertToBestUnit(bitRate);
            Map<Integer, BigDecimal> lOctetRate = convertToBestUnit(octetRate);
            bitRates.add(lBitRate); octetRates.add(lOctetRate);
            rawBitRates.add(bitRate); rawOctetRates.add(octetRate);
            speedTestInterface.reportResult(convertSpeedTestMode(aInReport.getSpeedTestMode()), lBitRate, bitRate, lOctetRate, octetRate);
            speedTestInterface.storeResult(convertSpeedTestMode(aInReport.getSpeedTestMode()), startTime, bitRate, octetRate);
            testRunning = false;
        }

    }

    /**
     * Converts a 1024 based octetRate to the best unit
     * @param aInRate Rate to convert
     * @return Map with key = power of 1024 used to divide the octetRate so that result is lower than 1024, and value is result of this division
     */
    private Map<Integer, BigDecimal> convertToBestUnit(BigDecimal aInRate) {

        Integer lPower = 0;
        while (aInRate.divide(BigDecimal.valueOf(Math.pow(1024, lPower++)), RoundingMode.FLOOR).compareTo(BigDecimal.valueOf(1024)) == 1);
        Map<Integer, BigDecimal> lResult = new HashMap<>();
        lResult.put(--lPower, aInRate.divide(BigDecimal.valueOf(Math.pow(1024, lPower)), RoundingMode.FLOOR));
        return lResult;

    }

    // PUBLIC METHODS

    /**
     * Start a speed test task, first download then upload speed test
     * NB: Must not be launched if another test is on-going
     * @param aInDownloadUrl Download URL
     * @param aInUploadUrl   Upload URL
     */
    public void start(String aInDownloadUrl, String aInUploadUrl) {

        // Force a shutdown in case socket is busy
        speedTestSocket.forceStopTask();
        speedTestSocket.shutdownAndWait();

        speedTestInterface.reportStartTest();
        testRunning = true;
        startTime = System.currentTimeMillis();

        // Start download
        bitRate = BigDecimal.ZERO; octetRate = BigDecimal.ZERO; count = 0;
        mode = SpeedTestMode.DOWNLOAD;
        speedTestInterface.reportStartTransfer(EnumTypes.SpeedTestMode.DOWNLOAD);
        speedTestSocket.startDownloadRepeat(
                aInDownloadUrl,
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_REPEAT_DURATION_PREFERENCE, Constants.DEFAULT_SPEED_TEST_REPEAT_DURATION),
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_REPORT_INTERVAL_PREFERENCE, Constants.DEFAULT_SPEED_TEST_REPORT_INTERVAL),
                new IRepeatListener() {

                    @Override
                    public void onReport(SpeedTestReport aInReport) {
                        processProgressReport(aInReport);
                    }

                    @Override
                    public void onCompletion(SpeedTestReport aInReport) {

                        processCompletionReport(aInReport);

                        // Start upload
                        mode = SpeedTestMode.UPLOAD;
                        speedTestInterface.reportStartTransfer(EnumTypes.SpeedTestMode.UPLOAD);
                        bitRate = BigDecimal.ZERO; octetRate = BigDecimal.ZERO; count = 0;
                        testRunning = true;

                        speedTestSocket.startUploadRepeat(
                                aInUploadUrl,
                                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_REPEAT_DURATION_PREFERENCE, Constants.DEFAULT_SPEED_TEST_REPEAT_DURATION),
                                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_REPORT_INTERVAL_PREFERENCE, Constants.DEFAULT_SPEED_TEST_REPORT_INTERVAL),
                                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_UPLOAD_FILE_SIZE_PREFERENCE, Constants.DEFAULT_SPEED_TEST_UPLOAD_FILE_SIZE),
                                new IRepeatListener() {
                                    @Override
                                    public void onReport(SpeedTestReport aInReport) {
                                        processProgressReport(aInReport);
                                    }
                                    @Override
                                    public void onCompletion(SpeedTestReport aInReport) {
                                        processCompletionReport(aInReport);
                                        speedTestInterface.reportFinalResult(startTime, bitRates, octetRates, rawBitRates, rawOctetRates);
                                        resetRates();
                                        speedTestInterface.reportStopTest();
                                    }

                                });

                    }

                });

    }

    /**
     * Stops current task
     */
    public void stop() {
        if (testRunning) {
            interrupted = true; // Must not be moved as onError callback is called after socked is closed and this flag is tested in this callback
            speedTestSocket.forceStopTask();
            speedTestSocket.closeSocket(); // Socket needs to be closed otherwise transfer goes on forever although no callback is no more called
            speedTestInterface.reportInterruption(startTime, convertSpeedTestMode(mode));
            testRunning = false;
            fillRates();
            speedTestInterface.reportFinalResult(startTime, bitRates, octetRates, rawBitRates, rawOctetRates);
            speedTestInterface.storeResult(convertSpeedTestMode(mode), startTime, BigDecimal.ZERO, BigDecimal.ZERO);
            if (mode.equals(SpeedTestMode.DOWNLOAD)) speedTestInterface.storeResult(EnumTypes.SpeedTestMode.UPLOAD, startTime, BigDecimal.ZERO, BigDecimal.ZERO);
            resetRates();
        }
    }

    /**
     * Indicates if a test is running
     * @return true if a test is running, false otherwise
     */
    public boolean isTestRunning() {
        return testRunning;
    }

    /**
     * Ends current speed test
     */
    public void end() {
        if (speedTestSocket != null) {
            speedTestSocket.forceStopTask();
            speedTestSocket.closeSocket();
        }
    }

}
