package cclerc.services;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

public class SpeedTest {

    SpeedTestSocket speedTestSocket = new SpeedTestSocket();

    public SpeedTest(SpeedTestInterface aInSpeedTestInterface) {

        // Add a listener to wait for speed test completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(SpeedTestReport report) {
                // Called when download/upload is complete
                aInSpeedTestInterface.printMessage(String.format(Display.getViewResourceBundle().getString("speedTest.completed"),
                                                                 Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                                                                 Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                                                                 report.getTransferRateOctet(), report.getTransferRateBit()));
                aInSpeedTestInterface.storeResult(report);
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                // Called when a download/upload error occur
                aInSpeedTestInterface.printError(String.format(Display.getViewResourceBundle().getString("speedTest.error"),
                                                                 Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                                                                 Display.getViewResourceBundle().getString("speedtest.mode." + speedTestSocket.getSpeedTestMode().toString().toLowerCase()),
                                                                 speedTestError + " - " + errorMessage));
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                // Called to notify download/upload progress
                aInSpeedTestInterface.printMessage(String.format(Display.getViewResourceBundle().getString("speedTest.progress"),
                                                                 Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                                                                 Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                                                                 percent, report.getTransferRateOctet(), report.getTransferRateBit()));
            }

        });

    }

    // TODO: add configuration, add methods for different types of download/upload
    public void startHttpDownload(String aInUrl) {
        speedTestSocket.startDownload(aInUrl);
    }

    public void startHttpUpload(String aInUrl) {
        speedTestSocket.startUpload(aInUrl,  100000000);
    }

}
