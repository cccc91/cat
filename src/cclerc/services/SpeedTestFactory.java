package cclerc.services;

import cclerc.cat.Cat;
import cclerc.cat.Configuration.Configuration;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.model.SpeedTestError;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;

import java.math.BigDecimal;
import java.util.HashMap;
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
    public static SpeedTest getInstance(String aInType) {
        return new SpeedTest(new SpeedTestInterface() {

            @Override
            public void reportStartTest() {
                Cat.getInstance().getController().switchStopStartSpeedTestButton();
                Cat.getInstance().getController().printSpeedTest(
                        new Message(
                                String.format(Display.getViewResourceBundle().getString("speedTest.start"),
                                              Display.getViewResourceBundle().getString("speedtest.type." + aInType).toUpperCase()), EnumTypes.MessageLevel.INFO));
                Cat.getInstance().getController().printSpeedTest(new Message("", EnumTypes.MessageLevel.INFO));

                // Clear live speed test series
                if (Cat.getInstance().displayGraphicalInterface()) {
                    Platform.runLater(() -> {
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
                                              Display.getViewResourceBundle().getString("speedtest.type." + aInType).toUpperCase()), EnumTypes.MessageLevel.INFO));
            }

            @Override
            public void reportInterruption() {
                Cat.getInstance().getController().switchStopStartSpeedTestButton();
                Cat.getInstance().getController().printSpeedTest(new Message(
                        String.format(Display.getViewResourceBundle().getString("speedTest.interrupted"),
                                      Display.getViewResourceBundle().getString("speedtest.type." + aInType)), EnumTypes.MessageLevel.WARNING));
                Cat.getInstance().getController().printSpeedTest(
                        new Message(
                                String.format(Display.getViewResourceBundle().getString("speedTest.end"),
                                              Display.getViewResourceBundle().getString("speedtest.type." + aInType).toUpperCase()), EnumTypes.MessageLevel.INFO));
            }

            @Override
            public void reportProgress(String aInTransferMode, float aInProgress, Map<Integer, BigDecimal> aInBitRate, Map<Integer, BigDecimal> aInOctetRate) {

                BigDecimal lBitRate = aInBitRate.values().iterator().next();
                String lMessage =  String.format(
                        Display.getViewResourceBundle().getString("speedTest.progress"),
                        Display.getViewResourceBundle().getString("speedtest.type." + aInType),
                        Display.getViewResourceBundle().getString("speedtest.mode." + aInTransferMode),
                        aInProgress,
                        aInOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRate.keySet().iterator().next()),
                        lBitRate, Display.getViewResourceBundle().getString("bitRate." + aInBitRate.keySet().iterator().next()));
                    Cat.getInstance().getController().replaceLastSpeedTestMessage(new Message(lMessage, EnumTypes.MessageLevel.INFO));

                // Add point to speed test series
                if (Cat.getInstance().displayGraphicalInterface()) {
                    Platform.runLater(() -> {
                        XYChart.Data lPoint = new XYChart.Data(aInProgress, lBitRate);
                        if (aInTransferMode.equals("download"))
                            Cat.getInstance().getController().getLiveSpeedTestDownloadSeries().getData().add(lPoint);
                        else
                            Cat.getInstance().getController().getLiveSpeedTestUploadSeries().getData().add(lPoint);
                        lPoint.getNode().getStyleClass().add("chart-line-symbol-" + aInTransferMode);
                    });
                }

            }

            @Override
            public void reportResult(String aInTransferMode, Map<Integer, BigDecimal> aInBitRate, Map<Integer, BigDecimal> aInOctetRate) {

                BigDecimal lBitRate = aInBitRate.values().iterator().next();
                String lMessage = String.format(
                        Display.getViewResourceBundle().getString("speedTest.completed"),
                        Display.getViewResourceBundle().getString("speedtest.type." + aInType),
                        Display.getViewResourceBundle().getString("speedtest.mode." + aInTransferMode),
                        aInOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRate.keySet().iterator().next()),
                        lBitRate, Display.getViewResourceBundle().getString("bitRate." + aInBitRate.keySet().iterator().next()));
                Cat.getInstance().getController().replaceLastSpeedTestMessage(new Message(lMessage, EnumTypes.MessageLevel.INFO));
                if (aInTransferMode.equals("download")) Cat.getInstance().getController().printSpeedTest(new Message("", EnumTypes.MessageLevel.INFO));

                // Add point to speed test series
                if (Cat.getInstance().displayGraphicalInterface()) {
                    Platform.runLater(() -> {
                        XYChart.Data lPoint = new XYChart.Data(100, lBitRate);
                        if (aInTransferMode.equals("download"))
                            Cat.getInstance().getController().getLiveSpeedTestDownloadSeries().getData().add(lPoint);
                        else
                            Cat.getInstance().getController().getLiveSpeedTestUploadSeries().getData().add(lPoint);
                        lPoint.getNode().getStyleClass().add("chart-line-symbol-" + aInTransferMode);
                    });
                }

            }

            @Override
            public void reportFinalResult(List<Map<Integer, BigDecimal>> aInBitRates, List<Map<Integer, BigDecimal>> aInOctetRates) {
                String lMessage = String.format(
                        Display.getViewResourceBundle().getString("speedTest.report"),
                        Display.getViewResourceBundle().getString("speedtest.type." + aInType),
                        Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_NAME_PREFERENCE),
                        aInOctetRates.get(0).values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRates.get(0).keySet().iterator().next()),
                        aInBitRates.get(0).values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + aInBitRates.get(0).keySet().iterator().next()),
                        aInOctetRates.get(1).values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRates.get(1).keySet().iterator().next()),
                        aInBitRates.get(1).values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + aInBitRates.get(1).keySet().iterator().next()));
                Cat.getInstance().getController().printConsole(new Message(lMessage, EnumTypes.MessageLevel.INFO));
                Display.getLogger().info(lMessage);
            }

            @Override
            public void reportError(String aInTransferMode, SpeedTestError aInSpeedTestError, String aInErrorMessage) {
                Message lMessage = new Message(String.format(Display.getViewResourceBundle().getString("speedTest.error"),
                                                             Display.getViewResourceBundle().getString("speedtest.type." + aInType),
                                                             Display.getViewResourceBundle().getString("speedtest.mode." + aInTransferMode),
                                                             aInSpeedTestError + " - " + aInErrorMessage), EnumTypes.MessageLevel.ERROR);
                Cat.getInstance().getController().printConsole(lMessage);
                Cat.getInstance().getController().printSpeedTest(lMessage);
            }

            @Override
            public void storeResult(SpeedTestReport report) {
                // TODO
            }

        },  (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN) == null) ? true :
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN).getUseProxy());
    }


}
