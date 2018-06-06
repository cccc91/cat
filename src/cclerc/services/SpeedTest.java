package cclerc.services;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.IRepeatListener;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import javax.rmi.CORBA.Util;
import java.net.Proxy;
import java.time.Instant;

public class SpeedTest {

    private SpeedTestSocket speedTestSocket = new SpeedTestSocket();
    private SpeedTestInterface speedTestInterface;
    private boolean repeat = false;
    private boolean testOnGoing = false;
    private boolean firstReport = true;
    private long timeout;

    /**
     * Speed test constructor
     * @param aInSpeedTestInterface Interface to be called for reporting progress, completion and error
     * @param aInUseProxy           // TODO: use configuration
     */
    public SpeedTest(SpeedTestInterface aInSpeedTestInterface, boolean aInUseProxy) {

        speedTestInterface = aInSpeedTestInterface;

        // TODO: proxy
        if (aInUseProxy) {
            Proxy lProxy = Network.findHttpProxy("http://st1.online.net/speedtest/speedtest");
            speedTestSocket.setProxyServer(lProxy.toString().replace(" @ ", "://"));
        }

        // TODO: use configuration
        speedTestSocket.setSocketTimeout(10000);
        timeout = 60000;

        // Add a listener to wait for speed test completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(SpeedTestReport report) {
                // Called when download/upload is complete
                if (!repeat) {
                    aInSpeedTestInterface.printMessage(String.format(Display.getViewResourceBundle().getString("speedTest.completed"),
                            Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                            Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                            report.getTransferRateOctet(), report.getTransferRateBit()));
                    aInSpeedTestInterface.storeResult(report);
                    testOnGoing = false;
                    firstReport = true;
                }
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                // Called when a download/upload error occur
                aInSpeedTestInterface.printError(String.format(Display.getViewResourceBundle().getString("speedTest.error"),
                                                                 Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                                                                 Display.getViewResourceBundle().getString("speedtest.mode." + speedTestSocket.getSpeedTestMode().toString().toLowerCase()),
                                                                 speedTestError + " - " + errorMessage));
                testOnGoing = false;
                firstReport = true;
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                if (!repeat) {
                    // Called to notify download/upload progress
                    aInSpeedTestInterface.printMessage(String.format(Display.getViewResourceBundle().getString("speedTest.progress"),
                            Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                            Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                            report.getProgressPercent(), report.getTransferRateOctet(), report.getTransferRateBit()));
                    firstReport = false;
                }
            }

        });

    }

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
     * Indicates if the report of the progress if the first one
     * @return true if the progress report is the first one, false otherwise
     */
    public boolean isFirstReport() {
        return firstReport;
    }

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
            speedTestSocket.startDownloadRepeat(aInUrl, 10000, 1000, new IRepeatListener() { // TODO: use configuration
                @Override
                public void onCompletion(SpeedTestReport report) {
                    speedTestInterface.printMessage("### " + String.format(Display.getViewResourceBundle().getString("speedTest.completed"),
                            Display.getViewResourceBundle().getString("speedtest.type." + speedTestInterface.getType()),
                            Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                            report.getTransferRateOctet(), report.getTransferRateBit()));
                    firstReport = true;
                    testOnGoing = false;
                    repeat = false;
                }

                @Override
                public void onReport(SpeedTestReport report) {
                    speedTestInterface.printMessage("### " + String.format(Display.getViewResourceBundle().getString("speedTest.progress"),
                            Display.getViewResourceBundle().getString("speedtest.type." + speedTestInterface.getType()),
                            Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                            report.getProgressPercent(), report.getTransferRateOctet(), report.getTransferRateBit()));
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
            speedTestSocket.startUploadRepeat(aInUrl, 10000, 1000, 100000000, new IRepeatListener() { // TODO: use configuration
                @Override
                public void onCompletion(SpeedTestReport report) {
                    speedTestInterface.printMessage("### " + String.format(Display.getViewResourceBundle().getString("speedTest.completed"),
                            Display.getViewResourceBundle().getString("speedtest.type." + speedTestInterface.getType()),
                            Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                            report.getTransferRateOctet(), report.getTransferRateBit()));
                    firstReport = true;
                    testOnGoing = false;
                    repeat = false;
                }

                @Override
                public void onReport(SpeedTestReport report) {
                    speedTestInterface.printMessage("### " + String.format(Display.getViewResourceBundle().getString("speedTest.progress"),
                            Display.getViewResourceBundle().getString("speedtest.type." + speedTestInterface.getType()),
                            Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                            report.getProgressPercent(), report.getTransferRateOctet(), report.getTransferRateBit()));
                    firstReport = false;
                }
            });
        }
    }

}
