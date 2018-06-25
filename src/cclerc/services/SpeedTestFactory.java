package cclerc.services;

import cclerc.cat.Cat;
import cclerc.cat.Configuration.Configuration;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.model.SpeedTestError;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeedTestFactory {

    private Map<String, SpeedTest> speedTests = new HashMap<>();

    private static SpeedTestFactory speedTestFactoryInstance = new SpeedTestFactory();

    // CONSTRUCTOR

    private SpeedTestFactory() {
        buildSpeedTest("periodic");
        buildSpeedTest("onRequest");
    }

    // SINGLETON

    /**
     * Returns the singleton
     * @return SpeedTestFactory singleton instance
     */
    public static SpeedTestFactory getInstance() {
        return speedTestFactoryInstance;
    }

    // PRIVATE

    /**
     * Builds periodic speed test
     * @param aInType Type of speed test (onRequest or periodic)
     */
    private void buildSpeedTest(String aInType) {

        speedTests.put(aInType, new SpeedTest(new SpeedTestInterface() {

            @Override
            public void reportStartTest() {
                Cat.getInstance().getController().switchStopStartSpeedTestButton();
                Cat.getInstance().getController().printSpeedTest(
                        new Message(
                                String.format(Display.getViewResourceBundle().getString("speedTest.start"),
                                              Display.getViewResourceBundle().getString("speedtest.type." + aInType).toUpperCase()), EnumTypes.MessageLevel.INFO));
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
                buildSpeedTest(aInType);
            }

            @Override
            public void reportProgress(String aInTransferMode, float aInProgress, Map<Integer, BigDecimal> aInBitRate, Map<Integer, BigDecimal> aInOctetRate) {
                String lMessage =  String.format(
                        Display.getViewResourceBundle().getString("speedTest.progress"),
                        Display.getViewResourceBundle().getString("speedtest.type." + aInType),
                        Display.getViewResourceBundle().getString("speedtest.mode." + aInTransferMode),
                        aInProgress,
                        aInOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRate.keySet().iterator().next()),
                        aInBitRate.values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + aInBitRate.keySet().iterator().next()));
                if (!speedTests.get(aInType).isFirstReport()) {
                    Cat.getInstance().getController().replaceLastSpeedTestMessage(new Message(lMessage, EnumTypes.MessageLevel.INFO));
                } else {
                    Cat.getInstance().getController().printSpeedTest(new Message(lMessage, EnumTypes.MessageLevel.INFO));
                }
            }

            @Override
            public void reportResult(String aInTransferMode, Map<Integer, BigDecimal> aInBitRate, Map<Integer, BigDecimal> aInOctetRate) {
                String lMessage = String.format(
                        Display.getViewResourceBundle().getString("speedTest.completed"),
                        Display.getViewResourceBundle().getString("speedtest.type." + aInType),
                        Display.getViewResourceBundle().getString("speedtest.mode." + aInTransferMode),
                        aInOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRate.keySet().iterator().next()),
                        aInBitRate.values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + aInBitRate.keySet().iterator().next()));
                Cat.getInstance().getController().replaceLastSpeedTestMessage(new Message(lMessage, EnumTypes.MessageLevel.INFO));
            }

            @Override
            public void reportFinalResult(List<Map<Integer, BigDecimal>> aInBitRates, List<Map<Integer, BigDecimal>> aInOctetRates) {
                String lMessage = String.format(
                        Display.getViewResourceBundle().getString("speedTest.report"),
                        Display.getViewResourceBundle().getString("speedtest.type." + aInType),
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
                buildSpeedTest(aInType);
            }

            @Override
            public void storeResult(SpeedTestReport report) {
                // TODO
            }

        },  (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN) == null) ? true :
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN).getUseProxy()));
    }

    // PUBLIC

    /**
     * Gets speed test of the required type
     * @param aInType Type of speed test (onRequest or periodic)
     * @return Speed test
     */
    public SpeedTest getSpeedTest(String aInType) {
        return speedTests.get(aInType);
    }

    /**
     * Stops all on-going speed tests
     */
    public void stopOnGoingSpeedTest() {
        for (SpeedTest lSpeedTest: speedTests.values()) {
            if (lSpeedTest.isTestRunning()) lSpeedTest.stop();
        }
    }

    public void resetFirstReports() {
        for (SpeedTest lSpeedTest: speedTests.values()) {
            lSpeedTest.resetFirstReport();
        }
    }

}
