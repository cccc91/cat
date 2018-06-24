package cclerc.services;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.IRepeatListener;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import org.jdom2.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class SpeedTest {

    private final String URL = "http://c.speedtest.net/speedtest-servers-static.php";

    private boolean testOnGoing = false;

    private SpeedTestSocket speedTestSocket = new SpeedTestSocket();
    private SpeedTestInterface speedTestInterface;
    private boolean interrupted;
    private boolean firstReport = true;
    private BigDecimal bitRate = new BigDecimal(0);
    private BigDecimal octetRate = new BigDecimal(0);
    private int count = 0;
    private Proxy proxy = Proxy.NO_PROXY;

    /**
     * Speed test constructor
     * @param aInSpeedTestInterface Interface to be called for reporting progress, completion and error
     * @param aInUseProxy           // TODO: use configuration
     */
    public SpeedTest(SpeedTestInterface aInSpeedTestInterface, boolean aInUseProxy) {

        speedTestInterface = aInSpeedTestInterface;
        interrupted = false; // Must be set to false at each re-instantiation (re-instantiation is done after interruption)

        // Set proxy if needed
        if (aInUseProxy) {
            proxy = Network.findHttpProxy(URL);
            speedTestSocket.setProxyServer(proxy.toString().replace(" @ ", "://"));
        }

        // TODO : uncomment
        // buildServersList();

        // Initialise speed test socket
        speedTestSocket.setSocketTimeout(Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_SOCKET_TIMEOUT_PREFERENCE, 10000));
        speedTestSocket.setDownloadSetupTime(Preferences.getInstance().getLongValue(Constants.SPEED_TEST_DOWNLOAD_SETUP_TIME_PREFERENCE, 100L));
        speedTestSocket.setUploadSetupTime(Preferences.getInstance().getLongValue(Constants.SPEED_TEST_UPLOAD_SETUP_TIME_PREFERENCE, 100L));

        // Add a listener to wait for speed test completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(SpeedTestReport aInReport) {
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                if (!interrupted) {
                    aInSpeedTestInterface.printError(
                            String.format(Display.getViewResourceBundle().getString("speedTest.error"),
                                          Display.getViewResourceBundle().getString("speedtest.type." + aInSpeedTestInterface.getType()),
                                          Display.getViewResourceBundle().getString("speedtest.mode." + speedTestSocket.getSpeedTestMode().toString().toLowerCase()),
                                          speedTestError + " - " + errorMessage));
                    testOnGoing = false;
                    firstReport = true;
                    speedTestInterface.stopTest();
                }
            }

            @Override
            public void onProgress(float percent, SpeedTestReport aInReport) {
            }

        });

    }

    // PRIVATE METHODS

    /**
     * Displays current transfer rate and stores values for average computation on completion
     * @param aInReport Progress report
     */
    private void processProgressReport(SpeedTestReport aInReport) {

        Map<Integer, BigDecimal> lBitRate = convertToBestUnit(aInReport.getTransferRateBit());
        Map<Integer, BigDecimal> lOctetRate = convertToBestUnit(aInReport.getTransferRateOctet());
        count++;
        bitRate = bitRate.add(aInReport.getTransferRateBit());
        octetRate = octetRate.add(aInReport.getTransferRateOctet());
        speedTestInterface.printProgress(
                String.format(Display.getViewResourceBundle().getString("speedTest.progress"),
                              Display.getViewResourceBundle().getString("speedtest.type." + speedTestInterface.getType()),
                              Display.getViewResourceBundle().getString("speedtest.mode." + aInReport.getSpeedTestMode().toString().toLowerCase()),
                              aInReport.getProgressPercent(),
                              lOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + lOctetRate.keySet().iterator().next()),
                              lBitRate.values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + lBitRate.keySet().iterator().next())));
        firstReport = false;

    }

    /**
     * Displays final transfer rate by computing it from average of the different progresses
     * @param aInReport Final progress report
     */
    private void processCompletionReport(SpeedTestReport aInReport) {

        count++;
        bitRate = bitRate.add(aInReport.getTransferRateBit()).divide(new BigDecimal(count), 2);
        octetRate = octetRate.add(aInReport.getTransferRateOctet()).divide(new BigDecimal(count), 2);
        Map<Integer, BigDecimal> lBitRate = convertToBestUnit(bitRate);
        Map<Integer, BigDecimal> lOctetRate = convertToBestUnit(octetRate);
        speedTestInterface.printResult(
                String.format(Display.getViewResourceBundle().getString("speedTest.completed"),
                              Display.getViewResourceBundle().getString("speedtest.type." + speedTestInterface.getType()),
                              Display.getViewResourceBundle().getString("speedtest.mode." + aInReport.getSpeedTestMode().toString().toLowerCase()),
                              lOctetRate.values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + lOctetRate.keySet().iterator().next()),
                              lBitRate.values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + lBitRate.keySet().iterator().next())));
        speedTestInterface.storeResult(aInReport);
        testOnGoing = false;
        firstReport = true;

    }

    /**
     * Converts a 1024 based octetRate to the best unit
     * @param aInRate Rate to convert
     * @return Map with key = power of 1024 used to divide the octetRate so that result is lower than 1024, and value is result of this division
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
     * Start a speed test task, first download then upload speed test
     * NB: Must not be launched if another test is on-going
     * @param aInDownloadUrl Download URL
     * @param aInUploadUrl   Upload URL
     */
    public void start(String aInDownloadUrl, String aInUploadUrl) {

        speedTestInterface.startTest();
        testOnGoing = true;

        // Start download
        bitRate = BigDecimal.ZERO; octetRate = BigDecimal.ZERO; count = 0;
        speedTestSocket.startDownloadRepeat(aInDownloadUrl, 30000, 1000, new IRepeatListener() { // TODO: use preferences

            @Override
            public void onReport(SpeedTestReport aInReport) {
                processProgressReport(aInReport);
            }

            @Override
            public void onCompletion(SpeedTestReport aInReport) {

                processCompletionReport(aInReport);

                // Start upload
                bitRate = BigDecimal.ZERO; octetRate = BigDecimal.ZERO; count = 0;
                testOnGoing = true;

                speedTestSocket.startUploadRepeat(aInUploadUrl, 30000, 1000, 100000000, new IRepeatListener() { // TODO: use configuration
                    @Override
                    public void onReport(SpeedTestReport aInReport) {
                        processProgressReport(aInReport);
                    }
                    @Override
                    public void onCompletion(SpeedTestReport aInReport) {
                        processCompletionReport(aInReport);
                        speedTestInterface.stopTest();
                    }
                });

            }
        });

    }

    /**
     * Stops current task
     */
    public void stop() {
        if (testOnGoing) {
            interrupted = true; // Must not be moved as onError callback is called after socked is closed and this flag is tested in this callback
            speedTestSocket.forceStopTask();
            speedTestSocket.closeSocket(); // Socket needs to be closed otherwise transfer goes on forever although no callback is no more called
            speedTestInterface.interruptTest(
                    String.format(Display.getViewResourceBundle().getString("speedTest.interrupted"),
                                  Display.getViewResourceBundle().getString("speedtest.type." + speedTestInterface.getType())));
            testOnGoing = false;
        }
    }

    public void buildServersList() {

        try {

            // Build HTTP GET request to retrieve servers list from speedtest.net
            URL lUrl = new URL(URL);
            HttpURLConnection lConnection = (HttpURLConnection) lUrl.openConnection(proxy);
            lConnection.setRequestMethod("GET");
            lConnection.setRequestProperty("Accept", "application/json");

            if (lConnection.getResponseCode() != 200) {
                throw new ConnectException(lConnection.getResponseCode() + ": " + lConnection.getResponseMessage());
            }

            SAXBuilder lBuilder = new SAXBuilder();
//            Document lDocument = (Document) lBuilder.build(lConnection.getInputStream());

            // TODO: construire classe speedTestServer + GUI pour lancer et configurer le serveur
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (lConnection.getInputStream())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                if (output.contains("FR")) System.out.println(output);
            }

            lConnection.disconnect();

        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }

    }

}
