package cclerc.services;

import cclerc.cat.Cat;
import javafx.scene.image.Image;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.prefs.Preferences;

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

    // Global statistics
    public final static int DEFAULT_GLOBAL_MONITORING_POLLING_PERIOD = 3000;
    public final static long DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD1 = 1 * 60 * 60 * 1000; // 1 loss every 1 hours max (warning)
    public final static long DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD2 = 10 * 60 * 1000;     // 1 loss every 10 min max (minor)
    public final static long DEFAULT_MEAN_TIME_BETWEEN_TWO_CONNECTIONS_LOST_THRESHOLD3 = 1 * 60 * 1000;      // 1 loss every 1 min max (major)
    public final static long DEFAULT_CONNECTIONS_LOST_FORGET_TIME = 2 * 60 * 60 * 1000;                      // Consider there were no losses after 2 hours w/o loss

    // States
    public static final String PAUSE_STATE = "pause";
    public static final String SEND_MAIL_STATE = "sendMail";
    public static final String PING_CHART_ENABLE_STATE = "pingChart.enable";
    public static final String PING_CHART_DISPLAY_WAN_LINE_STATE = "pingChart.displayPingWanLine";
    public static final String PING_CHART_DISPLAY_LAN_LINE_STATE = "pingChart.displayPingLanLine";
    public static final String PING_CHART_DISPLAY_INTERFACE1_LINE_STATE = "pingChart.displayPingInterface1Line";
    public static final String PING_CHART_DISPLAY_INTERFACE2_LINE_STATE = "pingChart.displayPingInterface1Line";
    public static final String PING_CHART_HORIZONTAL_MOVE_SLIDER_STATE = "pingChart.horizontalMoveSliderPosition";
    public static final String PING_CHART_HORIZONTAL_ZOOM_SLIDER_STATE = "pingChart.horizontalZoomSliderPosition";
    public static final String PING_CHART_VERTICAL_ZOOM_SLIDER_STATE = "pingChart.verticalZoomSliderPosition";

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

    public static final Image APPLICATION_IMAGE = new Image("resources/images/cat.png");

    // Preferences
    public static final boolean DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE = true;
    public static final boolean DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE = true;

}
