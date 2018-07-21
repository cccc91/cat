package cclerc.services;

import javafx.scene.image.Image;
import org.apache.logging.log4j.Level;

import java.io.File;

public class Constants {

    // Exit statuses
    public static final int EXIT_OK = 0;
    public static final int EXIT_UNEXPECTED_EXCEPTION = 1;
    public static final int EXIT_INVALID_ARGUMENT = 2;
    public static final int EXIT_INVALID_JAVA_VERSION = 3;

    // Logs
    public static final Level FILE = Level.forName("file", 650);

    // Files
    public static final String CURRENT_DIRECTORY = new File("").getAbsolutePath();
    public static final String DEFAULT_CONFIGURATION_FILE = CURRENT_DIRECTORY + "/config/home.xml";

    // Emails
    public final static EnumTypes.TlsMode DEFAULT_SMTP_TLS_MODE = EnumTypes.TlsMode.starttls;
    public final static int DEFAULT_SMTP_PORT = 465;
    public final static int DEFAULT_SMTP_CONNECTION_TIMEOUT = 15;
    public final static int DEFAULT_SMTP_TIMEOUT = 10;
    public final static int MAX_RETRIES_SEND_MAIL = 3;

    // Jobs
    public final static int MAXIMUM_NUMBER_OF_MONITORED_INTERFACES = 2;
    public final static int DELAY_BETWEEN_TWO_JOBS = 300;

    public final static int DEFAULT_MAX_RETRIES = 1;
    public final static int MIN_MAX_RETRIES = 1;
    public final static int MAX_MAX_RETRIES = 10;
    public final static int DEFAULT_POLLING_PERIOD = 5000;
    public final static int MIN_POLLING_PERIOD = 1000;
    public final static int MAX_POLLING_PERIOD = 3600000;
    public final static int DEFAULT_TIMEOUT = 1000;
    public final static int MIN_TIMEOUT = 500;
    public final static int MAX_TIMEOUT = 60000;
    public final static int DEFAULT_CONNECTION_LOST_THRESHOLD = 2;
    public final static int MIN_CONNECTION_LOST_THRESHOLD  = 1;
    public final static int MAX_CONNECTION_LOST_THRESHOLD  = 5;

    // Units
    public static final Long Kbs = 1024L;
    public static final Long Mbs = 1024 * 1024L;
    public static final Long KBs = 8 * Kbs;
    public static final Long MBs = 8 * Mbs;

    public static final Integer MINUTES = 1;
    public static final Integer HOURS = 60;
    public static final Integer DAYS = 24 * HOURS;

    // Global statistics
    public final static int DEFAULT_GLOBAL_POLLING_PERIOD = 3000;
    public final static int DEFAULT_PERIODIC_SPEED_TEST_POLLING_PERIOD = 1000;
    public final static Long DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD1 = 15 * 60 * 1000L;     // 1 loss every 15 min max (warning)
    public final static Long DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD2 = 1 * 60 * 1000L;      // 1 loss every 1 min max  (minor)
    public final static Long DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD3 = 10 * 1000L;          // 1 loss every 10 sec max (major)
    public final static Long DEFAULT_CONNECTIONS_LOST_FORGET_TIME = 2 * 60 * 60 * 1000L;                      // Consider there were no losses after 2 hours w/o loss

    public static final Long DEFAULT_MAX_STORED_PING_DURATION = 3 * 24 * 60 * 60 * 1000L; // 3 days
    public static final Long DEFAULT_MAX_DISPLAYED_PING_DURATION = 60 * 60 * 1000L;       // 1 hour
    public static final Long DEFAULT_MIN_DISPLAYED_PING_DURATION = 15 * 1000L;            // 15 seconds

    public static final Long DEFAULT_MAX_STORED_SPEED_TEST_DURATION = 30 * 24 * 60 * 60 * 1000L; // 30 days
    public static final Long DEFAULT_MAX_DISPLAYED_SPEED_TEST_DURATION = 12 * 60 * 60 * 1000L;   // 12 hours
    public static final Long DEFAULT_MIN_DISPLAYED_SPEED_TEST_DURATION = 15 * 60 * 1000L;        // 15 min

    // States
    public static final String PAUSE_STATE = "pause";
    public static final String SEND_MAIL_STATE = "sendMail";
    public static final String PING_CHART_ENABLE_STATE = "pingChart.enable";
    public static final String PING_CHART_DISPLAY_WAN_LINE_STATE = "pingChart.displayPingWanLine";
    public static final String PING_CHART_DISPLAY_LAN_LINE_STATE = "pingChart.displayPingLanLine";
    public static final String PING_CHART_DISPLAY_INTERFACE1_LINE_STATE = "pingChart.displayPingInterface1Line";
    public static final String PING_CHART_DISPLAY_INTERFACE2_LINE_STATE = "pingChart.displayPingInterface2Line";
    public static final String PING_CHART_HORIZONTAL_MOVE_SLIDER_STATE = "pingChart.horizontalMoveSliderPosition";
    public static final String PING_CHART_HORIZONTAL_ZOOM_SLIDER_STATE = "pingChart.horizontalZoomSliderPosition";
    public static final String PING_CHART_VERTICAL_ZOOM_SLIDER_STATE = "pingChart.verticalZoomSliderPosition";
    public static final String SPEED_TEST_CHART_ENABLE_STATE = "speedTestChart.enable";
    public static final String SPEED_TEST_CHART_DISPLAY_DOWNLOAD_STATE = "speedTestChart.displayDownloadSpeedBar";
    public static final String SPEED_TEST_CHART_DISPLAY_UPLOAD_STATE = "speedTestChart.displayUploadSpeedBar";
    public static final String SPEED_TEST_CHART_HORIZONTAL_ZOOM_SLIDER_STATE = "speedTestChart.horizontalZoomSliderPosition";
    public static final String SPEED_TEST_CHART_VERTICAL_ZOOM_SLIDER_STATE = "speedTestChart.verticalZoomSliderPosition";

    // Graphics
    public final static double DISABLED_IMAGE_TRANSPARENCY = 0.2d;

    public static final int TOOLTIP_OPEN_DELAY = 0;
    public static final int TOOLTIP_CLOSE_DELAY = 0;
    public static final int TOOLTIP_VISIBLE_DURATION= 10000;

    public static final String IMAGE_OK = "green.png";
    public static final String IMAGE_NOK = "red.png";
    public static final String IMAGE_PLAY = "play.png";
    public static final String IMAGE_PAUSE = "pause.png";
    public static final String IMAGE_EMAIL = "email.png";
    public static final String IMAGE_NOEMAIL = "no-email.png";
    public static final String IMAGE_EMAIL_TEST = "email_test.png";
    public static final String IMAGE_EMAIL_OK = "email_ok.png";
    public static final String IMAGE_EMAIL_NOK = "email_nok.png";
    public static final String IMAGE_WAN_OK = "wan_ok.png";
    public static final String IMAGE_WAN_NOK = "wan_nok.png";
    public static final String IMAGE_LAN_OK = "lan_ok.png";
    public static final String IMAGE_LAN_NOK = "lan_nok.png";
    public static final String IMAGE_ETH = "eth_std.png";
    public static final String IMAGE_ETH_OK = "eth_ok.png";
    public static final String IMAGE_ETH_NOK = "eth_nok.png";
    public static final String IMAGE_WIFI = "wifi_std.png";
    public static final String IMAGE_WIFI_OK = "wifi_ok.png";
    public static final String IMAGE_WIFI_NOK = "wifi_nok.png";
    public static final String IMAGE_SAVE = "save.png";
    public static final String IMAGE_SAVE_AS = "saveAs.png";
    public static final String IMAGE_CLOSE_APP = "closeApp.png";
    public static final String IMAGE_CONFIGURE = "configure.png";
    public static final String IMAGE_SPEED_TEST = "speedTest.png";

    public static final Image APPLICATION_IMAGE = new Image("resources/images/cat.png");

    // General preferences
    public static final boolean DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE = true;
    public static final boolean DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE = true;

    public static final String PERIODIC_REPORTS_ENABLED_PREFERENCE = "periodicReports.enable";
    public static final String PERIODIC_REPORTS_PERIOD_PREFERENCE = "periodicReports.period";
    public static final String PERIODIC_REPORTS_PERIOD_DISPLAYED_UNIT_PREFERENCE = "periodicReports.displayedPeriodUnit";
    public static final String PERIODIC_REPORTS_OFFSET_PREFERENCE = "periodicReports.offset";

    public static final Boolean DEFAULT_PERIODIC_REPORTS_ENABLED = false;
    public static final Integer DEFAULT_PERIODIC_REPORTS_PERIOD = 1;
    public static final Integer DEFAULT_PERIODIC_REPORTS_PERIOD_DISPLAYED_UNIT = HOURS;
    public static final Integer DEFAULT_PERIODIC_REPORTS_OFFSET = 0;

    // Speed test preferences
    public static final String SPEED_TEST_GET_SERVERS_URL = "http://c.speedtest.net/speedtest-servers-static.php";
    public static final String SPEED_TEST_NAME_FILTER_PREFERENCE = "speedTest.filter.name";
    public static final String SPEED_TEST_SPONSOR_FILTER_PREFERENCE = "speedTest.filter.sponsor";
    public static final String SPEED_TEST_COUNTRY_FILTER_PREFERENCE = "speedTest.filter.country";
    public static final String SPEED_TEST_CITY_FILTER_PREFERENCE = "speedTest.filter.city";
    public static final String SPEED_TEST_DISTANCE_FILTER_PREFERENCE = "speedTest.filter.distance";
    public static final String SPEED_TEST_SERVER_NAME_PREFERENCE = "speedTest.server";
    public static final String SPEED_TEST_SERVER_SPONSOR_PREFERENCE = "speedTest.server.sponsor";
    public static final String SPEED_TEST_SERVER_URL_PREFERENCE = "speedTest.url";
    public static final String SPEED_TEST_SOCKET_TIMEOUT_PREFERENCE = "speedTest.socketTimeout";
    public static final String SPEED_TEST_DOWNLOAD_SETUP_TIME_PREFERENCE = "speedTest.setupTime.download";
    public static final String SPEED_TEST_UPLOAD_SETUP_TIME_PREFERENCE = "speedTest.setupTime.upload";
    public static final String SPEED_TEST_PERIODIC_TEST_ENABLED_PREFERENCE = "speedTest.periodicTestEnabled";
    public static final String SPEED_TEST_PERIODIC_TEST_PERIOD_PREFERENCE = "speedTest.periodicTestPeriod";
    public static final String SPEED_TEST_PERIODIC_TEST_OFFSET_PREFERENCE = "speedTest.periodicTestOffset";
    public static final String SPEED_TEST_REPEAT_DURATION_PREFERENCE = "speedTest.repeatDuration";
    public static final String SPEED_TEST_REPORT_INTERVAL_PREFERENCE = "speedTest.reportInterval";
    public static final String SPEED_TEST_EMAIL_REPORT_ENABLED_PREFERENCE = "speedTest.emailReportEnabled";
    public static final String SPEED_TEST_EMAIL_REPORT_PERIOD_PREFERENCE = "speedTest.emailReportPeriod";
    public static final String SPEED_TEST_UPLOAD_FILE_SIZE_PREFERENCE = "speedTest.uploadFileSize";
    public static final String SPEED_TEST_DISPLAY_UNIT_RATIO_PREFERENCE = "speedTest.displayUnitRatio";
    public static final String SPEED_TEST_DISPLAYED_KEY_UNIT_PREFERENCE = "speedTest.displayedKeyUnit";
    public static final String SPEED_TEST_DISPLAY_UNIT_PERIOD_PREFERENCE = "speedTest.displayUnitPeriod";

    public static final Integer DEFAULT_SPEED_TEST_SOCKET_TIMEOUT = 10000;
    public static final Long DEFAULT_SPEED_TEST_DOWNLOAD_SETUP_TIME = 100L;
    public static final Long DEFAULT_SPEED_TEST_UPLOAD_SETUP_TIME = 100L;
    public static final Boolean DEFAULT_SPEED_TEST_PERIODIC_TEST_ENABLED = true;
    public static final Integer DEFAULT_SPEED_TEST_PERIODIC_TEST_PERIOD = 1;
    public static final Integer DEFAULT_SPEED_TEST_PERIODIC_TEST_OFFSET = 0;      // min
    public static final Integer DEFAULT_SPEED_TEST_REPEAT_DURATION = 15000;       // ms
    public static final Integer DEFAULT_SPEED_TEST_REPORT_INTERVAL = 150;         // ms
    public static final Boolean DEFAULT_SPEED_TEST_EMAIL_REPORT_ENABLED = false;
    public static final Integer DEFAULT_SPEED_TEST_EMAIL_PERIOD = 1;
    public static final Integer DEFAULT_SPEED_TEST_UPLOAD_FILE_SIZE = 100000000;
    public static final Long DEFAULT_SPEED_TEST_DISPLAY_UNIT = Mbs;
    public static final String DEFAULT_SPEED_TEST_DISPLAYED_KEY_UNIT = "bitRate.2";
    public static final Integer DEFAULT_SPEED_TEST_DISPLAY_UNIT_PERIOD = HOURS;


    public static final String GEO_LOC_DATABASE = "GeoLite2-City.mmdb";

}
