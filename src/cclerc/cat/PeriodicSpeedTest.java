package cclerc.cat;

import cclerc.cat.Configuration.Configuration;
import cclerc.services.*;
import fr.bmartel.speedtest.model.SpeedTestError;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PeriodicSpeedTest implements Runnable {

    class RawMeasurement {

        private String date;
        private String server;
        private String download;
        private String upload;
        private String error;
        private EnumTypes.SpeedTestMode transferMode;

        public RawMeasurement(long aInTime, List<Map<Integer, BigDecimal>> aInBitRates, List<Map<Integer, BigDecimal>> aInOctetRates) {

            date = LocaleUtilities.getInstance().getDateFormat().format(new Date(aInTime)) + " " +
                   LocaleUtilities.getInstance().getTimeFormat().format(new Date(aInTime).getTime());

            server = (Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_SPONSOR_PREFERENCE, "").equals(""))
                             ? Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_NAME_PREFERENCE)
                             : Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_SPONSOR_PREFERENCE);

            download = String.format(
                    "%.1f %s (%.0f %s)",
                    aInOctetRates.get(0).values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRates.get(0).keySet().iterator().next()),
                    aInBitRates.get(0).values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + aInBitRates.get(0).keySet().iterator().next()));

            upload = String.format(
                    "%.1f %s (%.0f %s)",
                    aInOctetRates.get(1).values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRates.get(1).keySet().iterator().next()),
                    aInBitRates.get(1).values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + aInBitRates.get(1).keySet().iterator().next()));

        }

        public RawMeasurement(long aInTime, EnumTypes.SpeedTestMode aInTransferMode, String aInError) {

            date = LocaleUtilities.getInstance().getDateFormat().format(new Date(aInTime)) + " " +
                   LocaleUtilities.getInstance().getTimeFormat().format(new Date(aInTime).getTime());

            server = (Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_SPONSOR_PREFERENCE, "").equals(""))
                     ? Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_NAME_PREFERENCE)
                     : Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_SPONSOR_PREFERENCE);

            error = aInError;
            transferMode = aInTransferMode;

        }

        public String getDate() {
            return date;
        }

        public String getServer() {
            return server;
        }

        public String getDownload() {
            return download;
        }

        public String getUpload() {
            return upload;
        }

        public String getError() {
            return error;
        }

        public EnumTypes.SpeedTestMode getTransferMode() {
            return transferMode;
        }

    }

    class BarChartMeasurement {

        private String category;
        private Double download = 0d;
        private Double upload = 0d;

        public BarChartMeasurement(String aInCategory, Double aInDownload, Double aInUpload) {
            category = aInCategory;
            download = aInDownload;
            upload = aInUpload;
        }

        public String getCategory() {
            return category;
        }

        public Double getDownload() {
            return download;
        }

        public Double getUpload() {
            return upload;
        }

    }

    private final int MAX_NUMBER_OF_RETRIES = 1;

    private boolean running = true;

    private SpeedTest speedTest;
    private boolean speedTestEnabled;
    private int period;
    private int offset;
    private boolean pause = false;
    private Long nextExecutionTime;
    private String uploadUrl;
    private String downloadUrl;
    private String report;
    private String reportBodyTemplate;
    private String rawResultTemplate;
    private String barChartMeasurementTemplate;
    private Double maxSpeed;
    private List<BarChartMeasurement> barChartMeasurements;
    private List<RawMeasurement> rawMeasurements;

    // Periodic speed test instance
    private static PeriodicSpeedTest periodicSpeedTest = new PeriodicSpeedTest();

    private PeriodicSpeedTest() {
        try {

            // Retrieve information from CSS
            InputStream lCssInputStream = getClass().getResourceAsStream("/resources/css/view.css");
            BufferedReader lCssBuffer = new BufferedReader(new InputStreamReader(lCssInputStream));
            String lCss = lCssBuffer.lines().collect(Collectors.joining());
            String lDownloadColor = lCss.replaceAll(".*chart-download-periodic", "").replaceAll("}.*", "").replaceAll(".*-fx-stroke[^:]*:[ ]*([^;]*);.*", "$1");
            String lUploadColor = lCss.replaceAll(".*chart-upload-periodic", "").replaceAll("}.*", "").replaceAll(".*-fx-stroke[^:]*:[ ]*([^;]*);.*", "$1");

            // Load report body template
            InputStream lReportBodyInputStream = getClass().getResourceAsStream("/resources/templates/speedTestReportBody.html");
            BufferedReader lReportBodyBuffer = new BufferedReader(new InputStreamReader(lReportBodyInputStream));
            reportBodyTemplate = lReportBodyBuffer.lines().collect(Collectors.joining("\n"));

            reportBodyTemplate = reportBodyTemplate.replaceAll("#DOWNLOAD_COLOR#", lDownloadColor).replaceAll("#UPLOAD_COLOR#", lUploadColor)
                           .replaceAll("#SPEED_TEST_TITLE#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.speedTest.title"))
                           .replaceAll("#RAW_RESULTS_TITLE#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.speedTest.rawResults"))
                           .replaceAll("#BAR_CHART_TITLE#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.speedTest.barChart"))
                           .replaceAll("#SPEEDTEST_DATE_HEADER#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.speedTest.rawResults.date"))
                           .replaceAll("#SPEEDTEST_SERVER_HEADER#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.speedTest.rawResults.server"))
                           .replaceAll("#SPEEDTEST_DOWNLOAD_HEADER#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.speedTest.rawResults.download"))
                           .replaceAll("#SPEEDTEST_UPLOAD_HEADER#", Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.speedTest.rawResults.upload"));

            // Load raw result template
            InputStream lRawResultInputStream = getClass().getResourceAsStream("/resources/templates/speedTestReportRawResult.html");
            BufferedReader lRawResultBuffer = new BufferedReader(new InputStreamReader(lRawResultInputStream));
            rawResultTemplate = lRawResultBuffer.lines().collect(Collectors.joining("\n"));

            // Load bar chart measurement template
            InputStream lBarChartMeasurementInputStream = getClass().getResourceAsStream("/resources/templates/speedTestReportBarChartMeasurement.html");
            BufferedReader lBarChartMeasurementBuffer = new BufferedReader(new InputStreamReader(lBarChartMeasurementInputStream));
            barChartMeasurementTemplate = lBarChartMeasurementBuffer.lines().collect(Collectors.joining("\n"));

            barChartMeasurementTemplate = barChartMeasurementTemplate.replaceAll("#DOWNLOAD_COLOR#", lDownloadColor).replaceAll("#UPLOAD_COLOR#", lUploadColor);

        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }
    }

    public void start() {

        Thread lThread = new Thread(periodicSpeedTest);
        lThread.start();
    }

    @Override
    public void run() {

        // Name thread
        Thread.currentThread().setName("Periodic SpeedTest Thread");

        while (Cat.getInstance().isInitializationInProgress()) {
            Utilities.sleep(1000);
        }

        // Initialize speed test data
        loadConfiguration();
        resetReport();

        while (running) {

            // Run speed test if needed
            if (speedTestEnabled && downloadUrl != null && uploadUrl != null && System.currentTimeMillis() >= nextExecutionTime) {
                nextExecutionTime = Utilities.nextExecutionTime(nextExecutionTime, period, offset);
                if (Cat.getInstance().displayGraphicalInterface()) {
                    Platform.runLater(() -> {
                        Cat.getInstance().getController().setSpeedTestNextPeriodLabel(LocaleUtilities.getInstance().getMediumDateAndTimeFormat().format(nextExecutionTime));
                    });
                }
                if (!pause) {
                    if ((Cat.getInstance().getController().getSpeedTest() != null && Cat.getInstance().getController().getSpeedTest().isTestRunning()) ||
                        (speedTest != null && speedTest.isTestRunning())) {
                        Cat.getInstance().getController().printConsole(
                                new Message(String.format(
                                        Display.getViewResourceBundle().getString("speedTest.running"),
                                        LocaleUtilities.getInstance().getMediumDateAndTimeFormat().format(new Date(nextExecutionTime))), EnumTypes.MessageLevel.WARNING));
                    } else {
                        if (speedTest == null || speedTest.isInterrupted()) speedTest = SpeedTestFactory.getInstance(EnumTypes.SpeedTestType.PERIODIC);
                        speedTest.start(downloadUrl, uploadUrl);
                    }
                }
            }

            Utilities.sleep(Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration().getPeriodicSpeedTestPollingPeriod());

        }

    }

    // SINGLETON

    public static PeriodicSpeedTest getInstance() {
        return periodicSpeedTest;
    }

    // GETTERS

    public SpeedTest getSpeedTest() {
        return speedTest;
    }

    // SETTERS

    public void setPause(boolean aInPause) {
        pause = aInPause;
    }


    // METHODS

    /**
     * Loads speed test configuration
     */
    public void loadConfiguration() {

        speedTestEnabled = Preferences.getInstance().getBooleanValue(
                Constants.SPEED_TEST_PERIODIC_TEST_ENABLED_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_ENABLED);

        uploadUrl = Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_URL_PREFERENCE);
        downloadUrl = (uploadUrl != null) ? uploadUrl.replaceAll("upload.php", "random4000x4000.jpg") : null;

        period =
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_PERIODIC_TEST_PERIOD_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_PERIOD) *
                Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_DISPLAY_UNIT_PERIOD_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DISPLAY_UNIT_PERIOD);
        offset = Preferences.getInstance().getIntegerValue(
                Constants.SPEED_TEST_PERIODIC_TEST_OFFSET_PREFERENCE, Constants.DEFAULT_SPEED_TEST_PERIODIC_TEST_OFFSET);
        nextExecutionTime = Utilities.nextExecutionTime(
                (nextExecutionTime == null || nextExecutionTime > System.currentTimeMillis()) ? null : nextExecutionTime, period, offset);
        if (Cat.getInstance().displayGraphicalInterface()) {
            Platform.runLater(() -> {
                Cat.getInstance().getController().setSpeedTestNextPeriodLabel(LocaleUtilities.getInstance().getMediumDateAndTimeFormat().format(nextExecutionTime));
            });
        }

    }

    /**
     * Builds a new report, all previous data are lost
     */
    public void resetReport() {
        maxSpeed = 0d;
        barChartMeasurements = new ArrayList<>();
        rawMeasurements = new ArrayList<>();
        report = reportBodyTemplate;
    }

    /**
     * Adds error to report report
     * @param aInTime           time of the error
     * @param aInTransferMode   transfer mode (download or upload) during which the error occurred
     * @param aInSpeedTestError speed test error
     * @param aInErrorMessage   detailed message
     */
    public void addErrorToReport(long aInTime, EnumTypes.SpeedTestMode aInTransferMode, SpeedTestError aInSpeedTestError, String aInErrorMessage) {

        rawMeasurements.add(new RawMeasurement(aInTime, aInTransferMode, String.format(
                Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.speedTest.error"), aInSpeedTestError, aInErrorMessage)));
    }

    /**
     * Adds a measurement to the report
     * @param aInTime          time of the measurement
     * @param aInBitRates      download (index 0) and upload (index 1) bit rates with their unit as a power of 1024 of bits (0 = bits, 1 = kbits, 2 = Mbits)
     * @param aInOctetRates    download (index 0) and upload (index 1) byte rates with their unit as a power of 1024 of bytes (0 = bytes, 1 = kbytes, 2 = Mbytes)
     * @param aInRawBitRates   download (index 0) and upload (index 1) bits per second raw value
     * @param aInRawOctetRates download (index 0) and upload (index 1) bytes per second raw value
     */
    public void addMeasurementToReport(long aInTime, List<Map<Integer, BigDecimal>> aInBitRates, List<Map<Integer, BigDecimal>> aInOctetRates,
                                       List<BigDecimal> aInRawBitRates, List<BigDecimal> aInRawOctetRates) {

        // Convert rates to the specified unit
        BigDecimal lRatio =
                BigDecimal.valueOf(Preferences.getInstance().getLongValue(Constants.SPEED_TEST_DISPLAY_UNIT_RATIO_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DISPLAY_UNIT));
        Double lDownload = aInRawBitRates.get(0).divide(lRatio, 1).doubleValue();
        Double lUpload = aInRawBitRates.get(1).divide(lRatio, 1).doubleValue();
        if (maxSpeed < lDownload) maxSpeed = lDownload;
        if (maxSpeed < lUpload) maxSpeed = lUpload;
        rawMeasurements.add(new RawMeasurement(aInTime, aInBitRates, aInOctetRates));
        barChartMeasurements.add(
                new BarChartMeasurement(LocaleUtilities.getInstance().getMediumDateAndTimeFormat().format(aInTime).replaceAll("\\d{4} ", "\\\n"), lDownload, lUpload));

    }

    /**
     * Builds the periodic speed test report
     * @return Periodic speed test report
     */
    public String buildReport() {

        Double lScale = Constants.REPORT_BAR_CHARTS_SCALE / maxSpeed;

        // Build final HTML
        int i = rawMeasurements.size() - 1;
        while (i >= 0) {

            RawMeasurement lRawMeasurement = rawMeasurements.get(i);

            String lRawResultReport = new String(rawResultTemplate);

            if (i == 0 || (i > 0 && rawMeasurements.get(i-1).getError() == null)) {
                lRawResultReport = lRawResultReport
                        .replaceAll("#SPEED_TEST_DATE#", lRawMeasurement.getDate())
                        .replaceAll("#SPEEDTEST_SERVER#", lRawMeasurement.getServer())
                        .replaceAll("#SPEEDTEST_DOWNLOAD#", lRawMeasurement.getDownload())
                        .replaceAll("#SPEEDTEST_UPLOAD#", lRawMeasurement.getUpload());
            } else {
                i--;
                if (rawMeasurements.get(i).getTransferMode().equals(EnumTypes.SpeedTestMode.DOWNLOAD)) {
                    lRawResultReport = lRawResultReport
                            .replaceAll("#SPEED_TEST_DATE#", lRawMeasurement.getDate())
                            .replaceAll("#SPEEDTEST_SERVER#", lRawMeasurement.getServer())
                            .replaceAll("#SPEEDTEST_DOWNLOAD#", rawMeasurements.get(i).getError())
                            .replaceAll("#SPEEDTEST_UPLOAD#", (lRawMeasurement.getUpload() == null) ? "" : lRawMeasurement.getUpload());
                } else {
                    lRawResultReport = lRawResultReport
                            .replaceAll("#SPEED_TEST_DATE#", lRawMeasurement.getDate())
                            .replaceAll("#SPEEDTEST_SERVER#", lRawMeasurement.getServer())
                            .replaceAll("#SPEEDTEST_UPLOAD#", rawMeasurements.get(i).getError())
                            .replaceAll("#SPEEDTEST_DOWNLOAD#", (lRawMeasurement.getDownload() == null) ? "" : lRawMeasurement.getDownload());
                }
            }
            report = report.replaceAll("#RAW_RESULT#", lRawResultReport + "\n#RAW_RESULT#");
            i--;

        }
        report = report.replaceAll("#RAW_RESULT#", "");

        Integer lMeasurementNumber = 1;
        for (BarChartMeasurement lBarChartMeasurement : barChartMeasurements) {
            Double lScaledDownload = lBarChartMeasurement.getDownload() * lScale;
            Double lScaledUpload = lBarChartMeasurement.getUpload() * lScale;
            String lBarChartMeasurementReport = new String(barChartMeasurementTemplate);
            lBarChartMeasurementReport = lBarChartMeasurementReport
                    .replaceAll("#MEASUREMENT#", lMeasurementNumber.toString())
                    .replaceAll("#DOWNLOAD#", String.format("%.1f", lBarChartMeasurement.getDownload()))
                    .replaceAll("#DOWNLOAD_HEIGHT#", String.format("%.1f", lScaledDownload).replaceAll(",", "."))
                    .replaceAll("#UPLOAD#", String.format("%.1f", lBarChartMeasurement.getUpload()))
                    .replaceAll("#UPLOAD_HEIGHT#", String.format("%.1f", lScaledUpload).replaceAll(",", "."))
                    .replace("#CATEGORY#", lBarChartMeasurement.getCategory().replaceAll("\\n", "<br>"));

            report = report.replaceAll("#MEASUREMENTS#", lBarChartMeasurementReport + "\n#MEASUREMENTS#");
            lMeasurementNumber++;

        }
        report = report.replaceAll("#MEASUREMENTS#", "");

        return report;

    }

}
