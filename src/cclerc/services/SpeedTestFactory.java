package cclerc.services;

import cclerc.cat.Cat;
import cclerc.cat.Configuration.Configuration;
import fr.bmartel.speedtest.SpeedTestReport;

public class SpeedTestFactory {

    private SpeedTest periodicSpeedTest;
    private SpeedTest onRequestSpeedTest;

    private static SpeedTestFactory speedTestFactoryInstance = new SpeedTestFactory();

    // CONSTRUCTOR

    private SpeedTestFactory() {
        buildPeriodicSpeedTest();
        buildOnRequestSpeedTest();
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
     */
    private void buildPeriodicSpeedTest() {

        periodicSpeedTest = new SpeedTest(new SpeedTestInterface() {

            @Override
            public void startTest() {
                Cat.getInstance().getController().switchStopStartSpeedTestButton();
            }

            @Override
            public void stopTest() {
                Cat.getInstance().getController().switchStopStartSpeedTestButton();
            }

            @Override
            public void interruptTest(String aInMessage) {
                Cat.getInstance().getController().switchStopStartSpeedTestButton();
                Cat.getInstance().getController().printSpeedTest(new Message(aInMessage, EnumTypes.MessageLevel.WARNING));
                buildPeriodicSpeedTest();
            }

            @Override
            public void printProgress(String aInMessage) {
                if (!periodicSpeedTest.isFirstReport()) {
                    Cat.getInstance().getController().replaceLastSpeedTestMessage(new Message(aInMessage, EnumTypes.MessageLevel.INFO));
                } else {
                    Cat.getInstance().getController().printSpeedTest(new Message(aInMessage, EnumTypes.MessageLevel.INFO));
                }
            }

            @Override
            public void printResult(String aInMessage) {
                Message lMessage = new Message(aInMessage, EnumTypes.MessageLevel.INFO);
                Cat.getInstance().getController().printConsole(lMessage);
                Cat.getInstance().getController().replaceLastSpeedTestMessage(lMessage);
            }

            @Override
            public void printError(String aInMessage) {
                Message lMessage = new Message(aInMessage, EnumTypes.MessageLevel.ERROR);
                Cat.getInstance().getController().printConsole(lMessage);
                Cat.getInstance().getController().printSpeedTest(lMessage);
            }

            @Override
            public void storeResult(SpeedTestReport report) {
                // TODO
            }

            @Override
            public String getType() {
                return "periodic";
            }
        },  (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN) == null) ? true :
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN).getUseProxy());
    }

    /**
     * Builds on request speed test
     */
    private void buildOnRequestSpeedTest() {

        onRequestSpeedTest = new SpeedTest(new SpeedTestInterface() {

            @Override
            public void startTest() {
                Cat.getInstance().getController().switchStopStartSpeedTestButton();
            }

            @Override
            public void stopTest() {
                Cat.getInstance().getController().switchStopStartSpeedTestButton();
            }

            @Override
            public void interruptTest(String aInMessage) {
                Cat.getInstance().getController().switchStopStartSpeedTestButton();
                Cat.getInstance().getController().printSpeedTest(new Message(aInMessage, EnumTypes.MessageLevel.WARNING));
                buildOnRequestSpeedTest();
            }

            @Override
            public void printProgress(String aInMessage) {
                if (!onRequestSpeedTest.isFirstReport()) {
                    Cat.getInstance().getController().replaceLastSpeedTestMessage(new Message(aInMessage, EnumTypes.MessageLevel.INFO));
                } else {
                    Cat.getInstance().getController().printSpeedTest(new Message(aInMessage, EnumTypes.MessageLevel.INFO));
                }
            }

            @Override
            public void printResult(String aInMessage) {
                Message lMessage = new Message(aInMessage, EnumTypes.MessageLevel.INFO);
                Cat.getInstance().getController().printConsole(lMessage);
                Cat.getInstance().getController().replaceLastSpeedTestMessage(lMessage);
            }

            @Override
            public void printError(String aInMessage) {
                Message lMessage = new Message(aInMessage, EnumTypes.MessageLevel.ERROR);
                Cat.getInstance().getController().printConsole(lMessage);
                Cat.getInstance().getController().printSpeedTest(lMessage);
            }

            @Override
            public void storeResult(SpeedTestReport report) {
                // TODO
            }

            @Override
            public String getType() {
                return "onRequest";
            }
        },  (Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN) == null) ? true :
            Configuration.getCurrentConfiguration().getMonitoringConfiguration().getNetworkConfiguration(EnumTypes.AddressType.WAN).getUseProxy());
    }

    // PUBLIC

    /**
     * Gets the periodic speed test object
     * @return Periodic speed test object
     */
    public SpeedTest getPeriodicSpeedTest() {
        return periodicSpeedTest;
    }

    /**
     * Gets the on request speed test object
     * @return On request speed test object
     */
    public SpeedTest getOnRequestSpeedTest() {
        return onRequestSpeedTest;
    }

}
