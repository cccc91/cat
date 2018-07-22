package cclerc.services;

import cclerc.cat.Cat;
import cclerc.cat.Configuration.Configuration;
import cclerc.cat.PeriodicSpeedTest;
import fr.bmartel.speedtest.model.SpeedTestError;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SpeedTestFactory {

    private static SpeedTestFactory speedTestFactoryInstance = new SpeedTestFactory();

    // CONSTRUCTOR

    private SpeedTestFactory() {
    }

    // SINGLETON

    /**
     * Returns the singleton
     * @param aInType Type of speed test
     * @return SpeedTestFactory singleton instance
     */
    public static SpeedTest getInstance(EnumTypes.SpeedTestType aInType) {
        return new SpeedTest(new SpeedTestInterface() {

            @Override
            public void reportStartTest() {
                Cat.getInstance().getController().switchStopStartSpeedTestButton();
                Cat.getInstance().getController().printSpeedTest(
                        new Message(
                                String.format(Display.getViewResourceBundle().getString("speedTest.start"),
                                              Display.getViewResourceBundle().getString("speedtest.type." + EnumTypes.SpeedTestType.valueOf(aInType)).toUpperCase()),
                                EnumTypes.MessageLevel.INFO));
                Cat.getInstance().getController().printSpeedTest(new Message("", EnumTypes.MessageLevel.INFO));

                // Clear live speed test series
                if (Cat.getInstance().displayGraphicalInterface()) {
                    Platform.runLater(() -> {
                        // Prevent memory leaks
                        for (XYChart.Data lPoint: Cat.getInstance().getController().getLiveSpeedTestDownloadSeries().getData())  lPoint.setNode(null);
                        for (XYChart.Data lPoint: Cat.getInstance().getController().getLiveSpeedTestUploadSeries().getData())  lPoint.setNode(null);
                        // Reset data
                        Cat.getInstance().getController().getLiveSpeedTestDownloadSeries().getData().clear();
                        Cat.getInstance().getController().getLiveSpeedTestUploadSeries().getData().clear();
                    });
                }

            }

            @Override
            public void reportStopTest() {
                    Cat.getInstance().getController().switchStopStartSpeedTestButton();
                Cat.getInstance().getController().printSpeedTest(
                        new Message(
                                String.format(Display.getViewResourceBundle().getString("speedTest.end"),
                                              Display.getViewResourceBundle().getString("speedtest.type." + EnumTypes.SpeedTestType.valueOf(aInType)).toUpperCase()),
                                EnumTypes.MessageLevel.INFO));
            }

            @Override
            public void reportStartTransfer(EnumTypes.SpeedTestMode aInTransferMode) {
                if (Cat.getInstance().displayGraphicalInterface()) {
                    Platform.runLater(() -> {
                        Cat.getInstance().getController().setLiveSpeedTestStyle(aInTransferMode, aInType);
                    });
                }
            }

            @Override
            public void reportInterruption(long aInTime, EnumTypes.SpeedTestMode aInTransferMode) {
                Cat.getInstance().getController().switchStopStartSpeedTestButton();
                Cat.getInstance().getController().printConsole(new Message(
                        String.format(Display.getViewResourceBundle().getString("speedTest.interrupted"),
                                      Display.getViewResourceBundle().getString("speedtest.type." + EnumTypes.SpeedTestType.valueOf(aInType))
                                      + " ("  + aInTransferMode.toString().toLowerCase() + ')'),
                        EnumTypes.MessageLevel.WARNING));
                Cat.getInstance().getController().printSpeedTest(new Message(
                        String.format(Display.getViewResourceBundle().getString("speedTest.interrupted"),
                                      Display.getViewResourceBundle().getString("speedtest.type." + EnumTypes.SpeedTestType.valueOf(aInType))),
                        EnumTypes.MessageLevel.WARNING));
                Cat.getInstance().getController().printSpeedTest(
                        new Message(
                                String.format(Display.getViewResourceBundle().getString("speedTest.end"),
                                              Display.getViewResourceBundle().getString("speedtest.type." + EnumTypes.SpeedTestType.valueOf(aInType)).toUpperCase()),
                                EnumTypes.MessageLevel.INFO));

                // In case of periodic speed test, manage reports if reports are allowed
                if (Preferences.getInstance().getBooleanValue(Constants.PERIODIC_REPORTS_ENABLED_PREFERENCE, Constants.DEFAULT_PERIODIC_REPORTS_ENABLED) &&
                        aInType.equals(EnumTypes.SpeedTestType.PERIODIC)) {
                    PeriodicSpeedTest.getInstance().addErrorToReport(aInTime, aInTransferMode, null, Display.getMessagesResourceBundle().getString(
                            "generalEmail.periodicReports.speedTest.stop"));
                }

            }

            @Override
            public void reportProgress(EnumTypes.SpeedTestMode aInTransferMode, float aInProgress, Map<Integer, BigDecimal> aInBitRate, BigDecimal aInRawBitRate,
                                       Map<Integer, BigDecimal> aInOctetRate, BigDecimal aInRawOctetRate) {

                // Use converted rates for textual display and raw values converted to the configured unit for graphical display

                BigDecimal lBitRate = aInBitRate.values().iterator().next();
                String lMessage =  String.format(
                        Display.getViewResourceBundle().getString("speedTest.progress"),
                        Display.getViewResourceBundle().getString("speedtest.type." + EnumTypes.SpeedTestType.valueOf(aInType)),
                        Display.getViewResourceBundle().getString("speedtest.mode." + EnumTypes.SpeedTestMode.valueOf(aInTransferMode)),
                        aInProgress,
                        aInOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRate.keySet().iterator().next()),
                        lBitRate, Display.getViewResourceBundle().getString("bitRate." + aInBitRate.keySet().iterator().next()));
                    Cat.getInstance().getController().replaceLastSpeedTestMessage(new Message(lMessage, EnumTypes.MessageLevel.INFO));

                // Add point to speed test series - Filter first point on download and 2 first ones in upload (stabilization of throughput)
                if (Cat.getInstance().displayGraphicalInterface() &&
                    ((aInTransferMode.equals(EnumTypes.SpeedTestMode.DOWNLOAD) && aInProgress >= 0) ||
                     (aInTransferMode.equals(EnumTypes.SpeedTestMode.UPLOAD) && aInProgress >= 1))) {
                    Platform.runLater(() -> {
                        // Compute scale
                        BigDecimal lRatio =
                                BigDecimal.valueOf(Preferences.getInstance().getLongValue(Constants.SPEED_TEST_DISPLAY_UNIT_RATIO_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DISPLAY_UNIT));
                        XYChart.Data lPoint = new XYChart.Data(aInProgress, aInRawBitRate.divide(lRatio, 1));
                        if (aInTransferMode.equals(EnumTypes.SpeedTestMode.DOWNLOAD))
                            Cat.getInstance().getController().getLiveSpeedTestDownloadSeries().getData().add(lPoint);
                        else
                            Cat.getInstance().getController().getLiveSpeedTestUploadSeries().getData().add(lPoint);
                        lPoint.getNode().getStyleClass().add("chart-line-symbol-" + EnumTypes.SpeedTestMode.valueOf(aInTransferMode) + "-" + EnumTypes.SpeedTestType.valueOf(aInType));
                    });
                }

            }

            @Override
            public void reportResult(EnumTypes.SpeedTestMode aInTransferMode, Map<Integer, BigDecimal> aInBitRate, BigDecimal aInRawBitRate,
                                     Map<Integer, BigDecimal> aInOctetRate, BigDecimal aInRawOctetRate) {

                // Use converted rates for textual display and raw values converted to the configured unit for graphical display

                BigDecimal lBitRate = aInBitRate.values().iterator().next();
                String lMessage = String.format(
                        Display.getViewResourceBundle().getString("speedTest.completed"),
                        Display.getViewResourceBundle().getString("speedtest.type." + EnumTypes.SpeedTestType.valueOf(aInType)),
                        Display.getViewResourceBundle().getString("speedtest.mode." + EnumTypes.SpeedTestMode.valueOf(aInTransferMode)),
                        aInOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRate.keySet().iterator().next()),
                        lBitRate, Display.getViewResourceBundle().getString("bitRate." + aInBitRate.keySet().iterator().next()));
                Cat.getInstance().getController().replaceLastSpeedTestMessage(new Message(lMessage, EnumTypes.MessageLevel.INFO));
                if (aInTransferMode.equals(EnumTypes.SpeedTestMode.DOWNLOAD)) Cat.getInstance().getController().printSpeedTest(new Message("", EnumTypes.MessageLevel.INFO));

                // Add point to speed test series
                if (Cat.getInstance().displayGraphicalInterface()) {
                    Platform.runLater(() -> {
                        BigDecimal lRatio =
                                BigDecimal.valueOf(Preferences.getInstance().getLongValue(Constants.SPEED_TEST_DISPLAY_UNIT_RATIO_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DISPLAY_UNIT));
                        XYChart.Data lPoint = new XYChart.Data(100, aInRawBitRate.divide(lRatio, 1));
                        if (aInTransferMode.equals(EnumTypes.SpeedTestMode.DOWNLOAD))
                            Cat.getInstance().getController().getLiveSpeedTestDownloadSeries().getData().add(lPoint);
                        else
                            Cat.getInstance().getController().getLiveSpeedTestUploadSeries().getData().add(lPoint);
                        lPoint.getNode().getStyleClass().add("chart-line-symbol-" + EnumTypes.SpeedTestMode.valueOf(aInTransferMode) + "-" + EnumTypes.SpeedTestType.valueOf(aInType));
                    });
                }

            }

            @Override
            public void reportFinalResult(long aInTime, List<Map<Integer, BigDecimal>> aInBitRates, List<Map<Integer, BigDecimal>> aInOctetRates,
                                          List<BigDecimal> aInBitRawRates, List<BigDecimal> aInRawOctetRates) {

                String lServer = (Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_SPONSOR_PREFERENCE, "").equals(""))
                                 ? Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_NAME_PREFERENCE)
                                 : Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_SPONSOR_PREFERENCE);

                String lMessage = String.format(
                        Display.getViewResourceBundle().getString("speedTest.report"),
                        Display.getViewResourceBundle().getString("speedtest.type." + EnumTypes.SpeedTestType.valueOf(aInType)),
                        lServer,
                        aInOctetRates.get(0).values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRates.get(0).keySet().iterator().next()),
                        aInBitRates.get(0).values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + aInBitRates.get(0).keySet().iterator().next()),
                        aInOctetRates.get(1).values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRates.get(1).keySet().iterator().next()),
                        aInBitRates.get(1).values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + aInBitRates.get(1).keySet().iterator().next()));
                Cat.getInstance().getController().printConsole(new Message(lMessage, EnumTypes.MessageLevel.INFO));
                Thread.currentThread().setName(EnumTypes.SpeedTestType.valueOf(aInType).substring(0, 1).toUpperCase() + EnumTypes.SpeedTestType.valueOf(aInType).substring(1) +
                                               " SpeedTest Thread");
                Display.getLogger().info(lMessage);

                // In case of periodic speed test, manage report if reports are allowed
                if (Preferences.getInstance().getBooleanValue(Constants.PERIODIC_REPORTS_ENABLED_PREFERENCE, Constants.DEFAULT_PERIODIC_REPORTS_ENABLED) &&
                    aInType.equals(EnumTypes.SpeedTestType.PERIODIC)) {
                    PeriodicSpeedTest.getInstance().addMeasurementToReport(aInTime, aInBitRates, aInOctetRates, aInBitRawRates, aInRawOctetRates);
                }

            }

            @Override
            public void reportError(long aInTime, EnumTypes.SpeedTestMode aInTransferMode, SpeedTestError aInSpeedTestError, String aInErrorMessage) {

                Message lMessage = new Message(String.format(Display.getViewResourceBundle().getString("speedTest.error"),
                                                             Display.getViewResourceBundle().getString("speedtest.type." + EnumTypes.SpeedTestType.valueOf(aInType)),
                                                             Display.getViewResourceBundle().getString("speedtest.mode." + EnumTypes.SpeedTestMode.valueOf(aInTransferMode)),
                                                             aInSpeedTestError + " - " + aInErrorMessage), EnumTypes.MessageLevel.ERROR);
                Cat.getInstance().getController().printConsole(lMessage);
                Cat.getInstance().getController().printSpeedTest(lMessage);

                // In case of periodic speed test, manage report if reports are allowed
                if (Preferences.getInstance().getBooleanValue(Constants.PERIODIC_REPORTS_ENABLED_PREFERENCE, Constants.DEFAULT_PERIODIC_REPORTS_ENABLED) &&
                        aInType.equals(EnumTypes.SpeedTestType.PERIODIC)) {
                    PeriodicSpeedTest.getInstance().addErrorToReport(aInTime, aInTransferMode, aInSpeedTestError, aInErrorMessage);
                }

            }

            @Override
            public void storeResult(EnumTypes.SpeedTestMode aInSpeedTestMode, long aInTime, BigDecimal aInBitRate, BigDecimal aInOctetRate) {
                Cat.getInstance().getController().addSpeedTestSeriesData(aInSpeedTestMode, aInTime, aInBitRate.intValue(), aInType);
            }

        },  (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN) == null) ? true :
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN).getUseProxy());
    }


}
