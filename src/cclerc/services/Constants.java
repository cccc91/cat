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
    public final static int DEFAULT_PERIODIC_REPORTS_POLLING_PERIOD = 1000;
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

    public static final Image IMAGE_PLAY = new Image(Constants.class.getClassLoader().getResource("resources/images/play.png").toString());
    public static final Image IMAGE_PAUSE = new Image(Constants.class.getClassLoader().getResource("resources/images/pause.png").toString());
    public static final Image IMAGE_EMAIL = new Image(Constants.class.getClassLoader().getResource("resources/images/email.png").toString());
    public static final Image IMAGE_NOEMAIL = new Image(Constants.class.getClassLoader().getResource("resources/images/no-email.png").toString());
    public static final Image IMAGE_EMAIL_TEST = new Image(Constants.class.getClassLoader().getResource("resources/images/email_test.png").toString());
    public static final Image IMAGE_EMAIL_OK = new Image(Constants.class.getClassLoader().getResource("resources/images/email_ok.png").toString());
    public static final Image IMAGE_EMAIL_NOK = new Image(Constants.class.getClassLoader().getResource("resources/images/email_nok.png").toString());
    public static final Image IMAGE_OK = new Image(Constants.class.getClassLoader().getResource("resources/images/green.png").toString());
    public static final Image IMAGE_DEGRADED = new Image(Constants.class.getClassLoader().getResource("resources/images/orange.png").toString());
    public static final Image IMAGE_NOK = new Image(Constants.class.getClassLoader().getResource("resources/images/red.png").toString());
    public static final Image IMAGE_WAN_OK = new Image(Constants.class.getClassLoader().getResource("resources/images/wan_ok.png").toString());
    public static final Image IMAGE_WAN_NOK = new Image(Constants.class.getClassLoader().getResource("resources/images/wan_nok.png").toString());
    public static final Image IMAGE_LAN_OK = new Image(Constants.class.getClassLoader().getResource("resources/images/lan_ok.png").toString());
    public static final Image IMAGE_LAN_NOK = new Image(Constants.class.getClassLoader().getResource("resources/images/lan_nok.png").toString());
    public static final Image IMAGE_ETH = new Image(Constants.class.getClassLoader().getResource("resources/images/eth_std.png").toString());
    public static final Image IMAGE_ETH_OK = new Image(Constants.class.getClassLoader().getResource("resources/images/eth_ok.png").toString());
    public static final Image IMAGE_ETH_NOK = new Image(Constants.class.getClassLoader().getResource("resources/images/eth_nok.png").toString());
    public static final Image IMAGE_WIFI = new Image(Constants.class.getClassLoader().getResource("resources/images/wifi_std.png").toString());
    public static final Image IMAGE_WIFI_OK = new Image(Constants.class.getClassLoader().getResource("resources/images/wifi_ok.png").toString());
    public static final Image IMAGE_WIFI_NOK = new Image(Constants.class.getClassLoader().getResource("resources/images/wifi_nok.png").toString());
    public static final Image IMAGE_SAVE = new Image(Constants.class.getClassLoader().getResource("resources/images/save.png").toString());
    public static final Image IMAGE_SAVE_AS = new Image(Constants.class.getClassLoader().getResource("resources/images/saveAs.png").toString());
    public static final Image IMAGE_CLOSE_APP = new Image(Constants.class.getClassLoader().getResource("resources/images/closeApp.png").toString());
    public static final Image IMAGE_CONFIGURE = new Image(Constants.class.getClassLoader().getResource("resources/images/configure.png").toString());
    public static final Image IMAGE_SPEED_TEST = new Image(Constants.class.getClassLoader().getResource("resources/images/speedTest.png").toString());

    public static final Image APPLICATION_IMAGE = new Image("resources/images/cat.png");

    public static final Double REPORT_BAR_CHARTS_SCALE = 400d;

    public static final int STATE_OK = 0;
    public static final int STATE_DEGRADED = 1;
    public static final int STATE_NOK = 2;

    // General preferences
    public static final boolean DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE = true;
    public static final boolean DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE = true;

    public static final String PERIODIC_REPORTS_ENABLED_PREFERENCE = "periodicReports.enable";
    public static final String PERIODIC_REPORTS_PERIOD_PREFERENCE = "periodicReports.period";
    public static final String PERIODIC_REPORTS_HOUR_PREFERENCE = "periodicReports.hour";
    public static final String PERIODIC_REPORTS_PERIOD_DISPLAYED_UNIT_PREFERENCE = "periodicReports.displayedPeriodUnit";
    public static final String PERIODIC_REPORTS_OFFSET_PREFERENCE = "periodicReports.offset";

    public static final Boolean DEFAULT_PERIODIC_REPORTS_ENABLED = true;
    public static final Integer DEFAULT_PERIODIC_REPORTS_PERIOD = 12;
    public static final Integer DEFAULT_PERIODIC_REPORTS_HOUR = 8;
    public static final Integer DEFAULT_PERIODIC_REPORTS_PERIOD_DISPLAYED_UNIT = HOURS;
    public static final Integer DEFAULT_PERIODIC_REPORTS_OFFSET = 1;

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
    public static final Integer DEFAULT_SPEED_TEST_UPLOAD_FILE_SIZE = 100000000;
    public static final Long DEFAULT_SPEED_TEST_DISPLAY_UNIT = Mbs;
    public static final String DEFAULT_SPEED_TEST_DISPLAYED_KEY_UNIT = "bitRate.2";
    public static final Integer DEFAULT_SPEED_TEST_DISPLAY_UNIT_PERIOD = HOURS;


    public static final String GEO_LOC_DATABASE = "GeoLite2-City.mmdb";

}
