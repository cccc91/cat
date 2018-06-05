package cclerc.services;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import javax.rmi.CORBA.Util;

public class SpeedTest {

    private SpeedTestSocket speedTestSocket = new SpeedTestSocket();
    private boolean downloadOnGoing = false;
    private boolean uploadOnGoing = false;
    private boolean firstReport = true;

    public SpeedTest(SpeedTestInterface aInSpeedTestInterface) {

        // TODO: use configuration
        speedTestSocket.setSocketTimeout(30000);

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
                if (downloadOnGoing) downloadOnGoing = false;
                if (uploadOnGoing) uploadOnGoing = false;
                firstReport = true;
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                // Called when a download/upload error occur
                aInSpeedTestInterface.printError(String.format(Display.getViewResourceBundle().getString("speedTest.error"),
                                                                 Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                                                                 Display.getViewResourceBundle().getString("speedtest.mode." + speedTestSocket.getSpeedTestMode().toString().toLowerCase()),
                                                                 speedTestError + " - " + errorMessage));
                if (downloadOnGoing) downloadOnGoing = false;
                if (uploadOnGoing) uploadOnGoing = false;
                firstReport = true;
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                // Called to notify download/upload progress
                aInSpeedTestInterface.printMessage(String.format(Display.getViewResourceBundle().getString("speedTest.progress"),
                                                                 Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                                                                 Display.getViewResourceBundle().getString("speedtest.mode." + report.getSpeedTestMode().toString().toLowerCase()),
                                                                 percent, report.getTransferRateOctet(), report.getTransferRateBit()));
                firstReport = false;
            }

        });

    }

    public boolean isDownloadOnGoing() {
        return downloadOnGoing;
    }

    public boolean isUploadOnGoing() {
        return uploadOnGoing;
    }

    public boolean isFirstReport() {
        return firstReport;
    }

    // TODO: add configuration, add methods for different types of download/upload
    public void startHttpDownload(String aInUrl) {
        while (uploadOnGoing) {
            Utilities.sleep(100);
        }
        downloadOnGoing = true;
        speedTestSocket.startDownload(aInUrl);
    }

    public void startHttpUpload(String aInUrl) {
        while (downloadOnGoing) {
            Utilities.sleep(100);
        }
        uploadOnGoing = true;
        speedTestSocket.startUpload(aInUrl,  100000000);
    }

}
