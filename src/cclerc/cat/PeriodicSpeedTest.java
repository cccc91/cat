package cclerc.cat;

import cclerc.cat.Configuration.Configuration;
import cclerc.services.*;
import fr.bmartel.speedtest.model.SpeedTestError;
import javafx.application.Platform;
import javafx.css.FontCssMetaData;
import javafx.css.PseudoClass;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PeriodicSpeedTest implements Runnable {

    class Measurement {

        private String category;
        private Double download = 0d;
        private Double upload = 0d;

        public Measurement(String aInCategory, Double aInDownload, Double aInUpload) {
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
    private Long nextExecutionTime;
    private Long nextEmailTime;
    private String uploadUrl;
    private String downloadUrl;
    private Email email;
    private String emailContentText;
    private String emailContentHTML;
    private String measurementTemplate;
    private Double maxSpeed;
    private List<Measurement> measurements;
    private String downloadColor;
    private String uploadColor;

    // Periodic speed test instance
    private static PeriodicSpeedTest periodicSpeedTest = new PeriodicSpeedTest();

    private PeriodicSpeedTest() {
        try {

            // Load measurement template
            InputStream lSpeedTestMeasurementInputStream = getClass().getResourceAsStream("/resources/templates/speedTestEmailMeasurement.html");
            BufferedReader lSpeedTestBuffer = new BufferedReader(new InputStreamReader(lSpeedTestMeasurementInputStream));
            measurementTemplate = lSpeedTestBuffer.lines().collect(Collectors.joining("\n"));

            // Retrieve information from CSS
            InputStream lCssInputStream = getClass().getResourceAsStream("/resources/css/view.css");
            BufferedReader lCssBuffer = new BufferedReader(new InputStreamReader(lCssInputStream));
            String lCss = lCssBuffer.lines().collect(Collectors.joining());
            downloadColor = lCss.replaceAll(".*chart-download-periodic", "").replaceAll("}.*", "").replaceAll(".*-fx-stroke[^:]*:[ ]*([^;]*);.*", "$1");
            uploadColor = lCss.replaceAll(".*chart-upload-periodic", "").replaceAll("}.*", "").replaceAll(".*-fx-stroke[^:]*:[ ]*([^;]*);.*", "$1");
            measurementTemplate = measurementTemplate.replaceAll("#DOWNLOAD_COLOR#", downloadColor).replaceAll("#UPLOAD_COLOR#", uploadColor);

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
        resetEmail();

        while (running) {

            // Run speed test if needed
            if (speedTestEnabled && downloadUrl != null && uploadUrl != null && System.currentTimeMillis() >= nextExecutionTime) {
                nextExecutionTime = Utilities.nextExecutionTime(nextExecutionTime, period, offset);
                if (Cat.getInstance().displayGraphicalInterface()) {
                    Platform.runLater(() -> {
                        Cat.getInstance().getController().setSpeedTestNextPeriodLabel(LocaleUtilities.getInstance().getMediumDateAndTimeFormat().format(nextExecutionTime));
                    });
                }
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

            Utilities.sleep(1000);

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

    public long getNextEmailTime() {
        return nextEmailTime;
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
        computeNextEmailTime();
        if (Cat.getInstance().displayGraphicalInterface()) {
            Platform.runLater(() -> {
                Cat.getInstance().getController().setSpeedTestNextPeriodLabel(LocaleUtilities.getInstance().getMediumDateAndTimeFormat().format(nextExecutionTime));
            });
        }

    }

    /**
     * Computes next time email should be sent
     */
    public  void computeNextEmailTime() {
        // TODO: use configuration if global monitoring email is enabled
        nextEmailTime = Utilities.nextExecutionTime(
                (nextEmailTime == null || nextEmailTime > System.currentTimeMillis()) ? null : nextEmailTime,
                period * Preferences.getInstance().getIntegerValue(Constants.SPEED_TEST_EMAIL_REPORT_PERIOD_PREFERENCE, Constants.DEFAULT_SPEED_TEST_EMAIL_PERIOD),
                offset);
    }

    /**
     * Builds a new report email, all previous data are lost
     */
    public void resetEmail() {
        email = new Email(
                States.getInstance().getBooleanValue((Cat.getInstance().getController().BuildStatePropertyName(Constants.SEND_MAIL_STATE)), true) &&
                Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations().size() != 0 &&
                !Configuration.getCurrentConfiguration().getEmailConfiguration().getRecipientList().isEmpty(),
                Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer());
        maxSpeed = 0d;
        measurements = new ArrayList<>();
        emailContentText = "";
        try {
            InputStream lInputStream = getClass().getResourceAsStream("/resources/templates/speedTestEmailBody.html");
            BufferedReader buffer = new BufferedReader(new InputStreamReader(lInputStream));
            emailContentHTML = buffer.lines().collect(Collectors.joining("\n"));
            emailContentHTML = emailContentHTML.replaceAll("#DOWNLOAD_COLOR#", downloadColor).replaceAll("#UPLOAD_COLOR#", uploadColor);
        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }


    }

    /**
     * Adds error to report email body
     * @param aInTime           time of the error
     * @param aInTransferMode   transfer mode (download or upload) during which the error occurred
     * @param aInSpeedTestError speed test error
     * @param aInErrorMessage   detailed message
     */
    public void addErrorToEmail(long aInTime, EnumTypes.SpeedTestMode aInTransferMode, SpeedTestError aInSpeedTestError, String aInErrorMessage) {

        String lDate = LocaleUtilities.getInstance().getDateFormat().format(new Date(aInTime));
        String lTime = LocaleUtilities.getInstance().getTimeFormat().format(new Date(aInTime).getTime());

        String lServer = (Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_SPONSOR_PREFERENCE, "").equals(""))
                         ? Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_NAME_PREFERENCE)
                         : Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_SPONSOR_PREFERENCE);

        emailContentText = String.format(
                Display.getMessagesResourceBundle().getString("generalEmail.speedTest.error"), lDate, lTime, lServer,
                aInTransferMode.toString().toUpperCase(), aInSpeedTestError, aInErrorMessage)
                           + "<br>\n" + emailContentText;

    }

    /**
     * Adds a measurement to email body
     * @param aInTime          time of the measurement
     * @param aInBitRates      download (index 0) and upload (index 1) bit rates with their unit as a power of 1024 of bits (0 = bits, 1 = kbits, 2 = Mbits)
     * @param aInOctetRates    download (index 0) and upload (index 1) byte rates with their unit as a power of 1024 of bytes (0 = bytes, 1 = kbytes, 2 = Mbytes)
     * @param aInRawBitRates   download (index 0) and upload (index 1) bits per second raw value
     * @param aInRawOctetRates download (index 0) and upload (index 1) bytes per second raw value
     */
    public void addMeasurementToEmail(long aInTime, List<Map<Integer, BigDecimal>> aInBitRates, List<Map<Integer, BigDecimal>> aInOctetRates,
                                      List<BigDecimal> aInRawBitRates, List<BigDecimal> aInRawOctetRates) {

        String lDate = LocaleUtilities.getInstance().getDateFormat().format(new Date(aInTime));
        String lTime = LocaleUtilities.getInstance().getTimeFormat().format(new Date(aInTime).getTime());

        String lServer = (Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_SPONSOR_PREFERENCE, "").equals(""))
                         ? Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_NAME_PREFERENCE)
                         : Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_SPONSOR_PREFERENCE);

        emailContentText = String.format(
                Display.getMessagesResourceBundle().getString("generalEmail.speedTest.text"), lDate, lTime, lServer,
                aInOctetRates.get(0).values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRates.get(0).keySet().iterator().next()),
                aInBitRates.get(0).values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + aInBitRates.get(0).keySet().iterator().next()),
                aInOctetRates.get(1).values().iterator().next(), Display.getViewResourceBundle().getString("octetRate." + aInOctetRates.get(1).keySet().iterator().next()),
                aInBitRates.get(1).values().iterator().next(), Display.getViewResourceBundle().getString("bitRate." + aInBitRates.get(1).keySet().iterator().next()))
                           + "<br>\n" + emailContentText;

        // Convert rates to the specified unit
        BigDecimal lRatio =
                BigDecimal.valueOf(Preferences.getInstance().getLongValue(Constants.SPEED_TEST_DISPLAY_UNIT_RATIO_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DISPLAY_UNIT));
        Double lDownload = aInRawBitRates.get(0).divide(lRatio, 1).doubleValue();
        Double lUpload = aInRawBitRates.get(1).divide(lRatio, 1).doubleValue();
        if (maxSpeed < lDownload) maxSpeed = lDownload;
        if (maxSpeed < lUpload) maxSpeed = lUpload;
        measurements.add(new Measurement(LocaleUtilities.getInstance().getMediumDateAndTimeFormat().format(aInTime).replaceAll("\\d{4} ", "\\\n"), lDownload, lUpload));

        // Send email if period is reached and compute next email time
        if (System.currentTimeMillis() >= getNextEmailTime()) {

            // TODO: in case periodic general email is enabled, don't send the mail but send info to GlobalMonitoring
            Double lScale = 400d / maxSpeed;

            // Build final HTML
            Integer i = 1;
            for (Measurement lMeasurement: measurements) {
                Double lScaledDownload = lMeasurement.getDownload() * lScale;
                Double lScaledUpload = lMeasurement.getUpload() * lScale;
                String lMeasurementHTML = new String(measurementTemplate);
                lMeasurementHTML = lMeasurementHTML
                        .replaceAll("#MEASUREMENT#", i.toString())
                        .replaceAll("#DOWNLOAD#", String.format("%.1f", lMeasurement.getDownload()))
                        .replaceAll("#DOWNLOAD_HEIGHT#", String.format("%.1f", lScaledDownload).replaceAll(",", "."))
                        .replaceAll("#UPLOAD#", String.format("%.1f", lMeasurement.getUpload()))
                        .replaceAll("#UPLOAD_HEIGHT#", String.format("%.1f", lScaledUpload).replaceAll(",", "."))
                        .replace("#CATEGORY#", lMeasurement.getCategory().replaceAll("\\n", "<br>"));

                emailContentHTML = emailContentHTML.replaceAll("#MEASUREMENTS#", lMeasurementHTML + "\n#MEASUREMENTS#");

            }
            emailContentHTML = emailContentHTML.replaceAll("#MEASUREMENTS#", "");

            sendEmail();
            computeNextEmailTime();
            resetEmail();
        }

    }

    /**
     * Sends report email
     */
    public void sendEmail() {

        String lLocalHostName = "";
        try {
            lLocalHostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }

        email.sendMail(
                String.format(Display.getMessagesResourceBundle().getString("generalEmail.speedTest.subject"), lLocalHostName),
                emailContentText + "<br>\n" + emailContentHTML);

    }

}
