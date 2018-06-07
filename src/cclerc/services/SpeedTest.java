package cclerc.services;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.IRepeatListener;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class SpeedTest {

    private SpeedTestSocket speedTestSocket = new SpeedTestSocket();
    private SpeedTestInterface speedTestInterface;
    private boolean repeat = false;
    private boolean testOnGoing = false;
    private boolean firstReport = true;
    private long timeout;
    private Proxy proxy = Proxy.NO_PROXY;

    /**
     * Speed test constructor
     * @param aInSpeedTestInterface Interface to be called for reporting progress, completion and error
     * @param aInUseProxy           // TODO: use configuration
     */
    public SpeedTest(SpeedTestInterface aInSpeedTestInterface, boolean aInUseProxy) {

        speedTestInterface = aInSpeedTestInterface;

        // TODO: proxy
        if (aInUseProxy) {
            proxy = Network.findHttpProxy("http://st1.online.net/speedtest/speedtest");
            speedTestSocket.setProxyServer(proxy.toString().replace(" @ ", "://"));
        }

        test();

        // TODO: use configuration
        speedTestSocket.setSocketTimeout(10000);
        speedTestSocket.setDownloadSetupTime(100);
        speedTestSocket.setUploadSetupTime(100);
        timeout = 60000;

        // Add a listener to wait for speed test completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(SpeedTestReport report) {
                // Called when download/upload is complete
                if (!repeat) {
                    Map<Integer, BigDecimal> lBitRate = convertToBestUnit(report.getTransferRateBit());
                    Map<Integer, BigDecimal> lOctetRate = convertToBestUnit(report.getTransferRateOctet());
                    aInSpeedTestInterface.printMessage(
                            String.format(Display.getViewResourceBundle().getString("speedTest.completed"),
                                          Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                                          Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                                          lOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + lOctetRate.keySet().iterator().next()),
                                          lBitRate.values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + lBitRate.keySet().iterator().next())));
                    aInSpeedTestInterface.storeResult(report);
                    testOnGoing = false;
                    firstReport = true;
                }
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                // Called when a download/upload error occur
                aInSpeedTestInterface.printError(
                        String.format(Display.getViewResourceBundle().getString("speedTest.error"),
                                      Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                                      Display.getViewResourceBundle().getString("speedtest.mode." + speedTestSocket.getSpeedTestMode().toString().toLowerCase()),
                                      speedTestError + " - " + errorMessage));
                testOnGoing = false;
                firstReport = true;
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                // Called to notify download/upload progress
                if (!repeat) {
                    Map<Integer, BigDecimal> lBitRate = convertToBestUnit(report.getTransferRateBit());
                    Map<Integer, BigDecimal> lOctetRate = convertToBestUnit(report.getTransferRateOctet());
                    aInSpeedTestInterface.printMessage(
                            String.format(Display.getViewResourceBundle().getString("speedTest.progress"),
                                          Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                                          Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                                          report.getProgressPercent(),
                                          lOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + lOctetRate.keySet().iterator().next()),
                                          lBitRate.values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + lBitRate.keySet().iterator().next())));
                    firstReport = false;
                }
            }

        });

    }

    // PRIVATE METHODS
    /**
     * Waits for on-going test to be completed until timeout is reached
     * @return true if the test is completed before timeout, false otherwise
     */
    private boolean waitForOnGoingTestCompletion() {

        long lStartTime = Instant.now().toEpochMilli();
        while (testOnGoing && (Instant.now().toEpochMilli() - lStartTime < timeout)) {
            Utilities.sleep(100);
        }
        return Instant.now().toEpochMilli() - lStartTime < timeout;

    }

    /**
     * Converts a 1024 based rate to the best unit
     * @param aInRate Rate to convert
     * @return Map with key = power of 1024 used to divide the rate so that result is lower than 1024, and value is result of this division
     */
    private Map<Integer, BigDecimal> convertToBestUnit(BigDecimal aInRate) {

        Integer lPower = 0;
        while (aInRate.divide(BigDecimal.valueOf(Math.pow(1024, lPower++))).compareTo(BigDecimal.valueOf(1024)) == 1);
        Map<Integer, BigDecimal> lResult = new HashMap<>();
        lResult.put(--lPower, aInRate.divide(BigDecimal.valueOf(Math.pow(1024, lPower))));
        return lResult;

    }

    // GETTERS

    /**
     * Indicates if the report of the progress if the first one
     * @return true if the progress report is the first one, false otherwise
     */
    public boolean isFirstReport() {
        return firstReport;
    }

    // PUBLIC METHODS

    /**
     * Starts download speed test as soon as no other test is on-going
     * @param aInUrl TODO: parameters to be changed (use configuration)
     */
    public void startDownload(String aInUrl) {
        if (waitForOnGoingTestCompletion()) {
            testOnGoing = true;
            speedTestSocket.startDownload(aInUrl);
        }
    }

    /**
     * Starts upload speed test as soon as no other test is on-going
     * @param aInUrl TODO: parameters to be changed (use configuration)
     */
    public void startUpload(String aInUrl) {
        if (waitForOnGoingTestCompletion()) {
            testOnGoing = true;
            speedTestSocket.startUpload(aInUrl, 100000000);
        }
    }

    /**
     * Start repeated download test as no other test is on-going
     * @param aInUrl TODO: parameters to be changed (use configuration)
     */
    public void startDownloadRepeat(String aInUrl) {
        if (waitForOnGoingTestCompletion()) {
            repeat = true;
            testOnGoing = true;
            speedTestSocket.startDownloadRepeat(aInUrl, 30000, 1000, new IRepeatListener() { // TODO: use configuration
                @Override
                public void onCompletion(SpeedTestReport report) {
                    Map<Integer, BigDecimal> lBitRate = convertToBestUnit(report.getTransferRateBit());
                    Map<Integer, BigDecimal> lOctetRate = convertToBestUnit(report.getTransferRateOctet());
                    speedTestInterface.printMessage(
                            String.format(Display.getViewResourceBundle().getString("speedTest.completed"),
                                          Display.getViewResourceBundle().getString("speedtest.type." + speedTestInterface.getType()),
                                          Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                                          lOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + lOctetRate.keySet().iterator().next()),
                                          lBitRate.values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + lBitRate.keySet().iterator().next())));
                    firstReport = true;
                    testOnGoing = false;
                    repeat = false;
                    speedTestInterface.storeResult(report);
                }

                @Override
                public void onReport(SpeedTestReport report) {
                    Map<Integer, BigDecimal> lBitRate = convertToBestUnit(report.getTransferRateBit());
                    Map<Integer, BigDecimal> lOctetRate = convertToBestUnit(report.getTransferRateOctet());
                    speedTestInterface.printMessage(
                            String.format(Display.getViewResourceBundle().getString("speedTest.progress"),
                                          Display.getViewResourceBundle().getString("speedtest.type." + speedTestInterface.getType()),
                                          Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                                          report.getProgressPercent(),
                                          lOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + lOctetRate.keySet().iterator().next()),
                                          lBitRate.values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + lBitRate.keySet().iterator().next())));
                    firstReport = false;
                }
            });
        }
    }

    /**
     * Start repeated upload test as no other test is on-going
     * @param aInUrl TODO: parameters to be changed (use configuration)
     */
    public void startUploadRepeat(String aInUrl) {
        if (waitForOnGoingTestCompletion()) {
            repeat = true;
            testOnGoing = true;
            speedTestSocket.startUploadRepeat(aInUrl, 30000, 1000, 100000000, new IRepeatListener() { // TODO: use configuration
                @Override
                public void onCompletion(SpeedTestReport report) {
                    Map<Integer, BigDecimal> lBitRate = convertToBestUnit(report.getTransferRateBit());
                    Map<Integer, BigDecimal> lOctetRate = convertToBestUnit(report.getTransferRateOctet());
                    speedTestInterface.printMessage(
                            String.format(Display.getViewResourceBundle().getString("speedTest.completed"),
                                          Display.getViewResourceBundle().getString("speedtest.type." + speedTestInterface.getType()),
                                          Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                                          lOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + lOctetRate.keySet().iterator().next()),
                                          lBitRate.values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + lBitRate.keySet().iterator().next())));
                    firstReport = true;
                    testOnGoing = false;
                    repeat = false;
                    speedTestInterface.storeResult(report);
                }

                @Override
                public void onReport(SpeedTestReport report) {
                    Map<Integer, BigDecimal> lBitRate = convertToBestUnit(report.getTransferRateBit());
                    Map<Integer, BigDecimal> lOctetRate = convertToBestUnit(report.getTransferRateOctet());
                    speedTestInterface.printMessage(
                            String.format(Display.getViewResourceBundle().getString("speedTest.progress"),
                                          Display.getViewResourceBundle().getString("speedtest.type." + speedTestInterface.getType()),
                                          Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                                          report.getProgressPercent(),
                                          lOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + lOctetRate.keySet().iterator().next()),
                                          lBitRate.values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + lBitRate.keySet().iterator().next())));
                    firstReport = false;
                }
            });
        }
    }

    public void test() {
        try {

            URL url = new URL("http://c.speedtest.net/speedtest-servers-static.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                if (output.contains("FR")) System.out.println(output);
            }

            conn.disconnect();

        } catch (IOException e) {
            Display.logUnexpectedError(e);
        }

    }

}
