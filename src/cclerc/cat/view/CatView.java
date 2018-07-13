package cclerc.cat.view;

import cclerc.cat.Cat;
import cclerc.cat.Configuration.Configuration;
import cclerc.cat.GlobalMonitoring;
import cclerc.cat.MonitoringJob;
import cclerc.cat.model.Alarm;
import cclerc.services.*;
import com.sun.javafx.charts.Legend;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.net.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatView {

    private final long MAX_STORED_PING_DURATION = Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration().getMaxStoredPingDuration();
    private final long MIN_DISPLAYED_PING_DURATION = Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration().getMinDisplayedPingDuration();
    private final long MAX_DISPLAYED_PING_DURATION = Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration().getMaxDisplayedPingDuration();
    private final long MAX_STORED_SPEED_TEST_DURATION = Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration().getMaxStoredSpeedTestDuration();
    private final long MIN_DISPLAYED_SPEED_TEST_DURATION = Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration().getMinDisplayedSpeedTestDuration();
    private final long MAX_DISPLAYED_SPEED_TEST_DURATION = Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration().getMaxDisplayedSpeedTestDuration();

    // Display management
    private Map<EnumTypes.AddressType, TabPane> monitoringTabPanes = new HashMap<>();
    private Map<EnumTypes.AddressType, List<Tab>> monitoringTabs = new HashMap<>();
    private Map<EnumTypes.AddressType, List<GridPane>> monitoringGridPanes = new HashMap<>();
    private List<CheckBox> pingLineFilterCheckBoxes = new ArrayList<>();
    private List<CheckBox> speedTestBarFilterCheckBoxes = new ArrayList<>();
    private List<String> networkInterfacesNames = new ArrayList<>();
    private Map<TextFlow, Boolean> firstDisplay = new HashMap<>();
    private Map<TextFlow, Tab> tabs = new HashMap<>();

    // Console management
    private volatile List<Message> messages = new ArrayList<>();

    // Jobs management
    private int jobsCount = 0;
    private long pingsCount = 0;
    private long lostPingsCount = 0;
    private long lostConnectionsCount = 0;
    private long ongoingLostConnectionsCount = 0;
    private String lostTime = "-";
    private String lostDetails = "-";

    // Pause management
    private boolean isButtonPauseDisplayed;
    private boolean isButtonPauseActive = true;

    // Email management
    private boolean isButtonGeneralEmailEnabled;
    private boolean isButtonEmailEnabled;
    private boolean isButtonEmailActive = true;

    // Controllers
    private AlarmDetailsDialog alarmDetailsDialogController;

    // Alarms management
    private SortedList<Alarm> sortedActiveAlarms;
    private SortedList<Alarm> sortedHistoricalAlarms;

    // Ping line chart
    class PingLine {

        private int id;
        private NetworkInterface networkInterface;
        private String interfaceName;
        private EnumTypes.AddressType addressType;
        private EnumTypes.InterfaceType interfaceType;
        private XYChart.Series<Number, Number> series = new XYChart.Series<>();
        private Number minX = Long.MAX_VALUE;
        private Number maxX = 0;
        private Number minY = Long.MAX_VALUE;
        private Number maxY = 0;

        PingLine(int aInId, EnumTypes.AddressType aInAddressType, NetworkInterface aInInterface) {
            id = aInId;
            addressType = aInAddressType;
            interfaceName = aInInterface.getName();
            interfaceType = Network.getInterfaceType(interfaceName);
            networkInterface = aInInterface;
            String lTitle = addressType + " " + interfaceType + " (" + interfaceName + ")";
            series.setName(lTitle);
            pingLineChart.getData().add(series);
        }

        public void delete() {
            pingLineChart.getData().remove(series);
        }

        public int getId() {
            return id;
        }

        public NetworkInterface getNetworkInterface() {
            return networkInterface;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public EnumTypes.AddressType getAddressType() {
            return addressType;
        }

        public EnumTypes.InterfaceType getInterfaceType() {
            return interfaceType;
        }

        public XYChart.Series<Number, Number> getSeries() {
            return series;
        }

        public Number getMinX() {
            return minX;
        }

        public Number getMaxX() {
            return maxX;
        }

        public Number getMinY() {
            return minY;
        }

        public Number getMaxY() {
            return maxY;
        }

        public void setMinX(Number minX) {
            this.minX = minX;
        }
        public void setMaxX(Number maxX) {
            this.maxX = maxX;
        }

        public void setMinY(Number minY) {
            this.minY = minY;
        }

        public void setMaxY(Number maxY) {
            this.maxY = maxY;
        }

    }
    class PingPoint {
        private XYChart.Data point;
        private boolean reachable;
        private EnumTypes.ServerType serverType;

        PingPoint(long aInX, long aInY, boolean aInReachable, EnumTypes.ServerType aInServerType) {
            point = new XYChart.Data(aInX, aInY);
            reachable = aInReachable;
            serverType = aInServerType;
        }

        public XYChart.Data getPoint() {
            return point;
        }

        public boolean isReachable() {
            return reachable;
        }

        public EnumTypes.ServerType getServerType() {
            return serverType;
        }

    }

    private NumberAxis pingLineChartXAxis = new NumberAxis();
    private NumberAxis pingLineChartYAxis = new NumberAxis();
    private LineChartWithMarkers<Number,Number> pingLineChart = new LineChartWithMarkers<>(pingLineChartXAxis, pingLineChartYAxis);

    private static int pingLinesCount = 0;
    private long timeReference = 0;
    private volatile Map<Integer, PingLine> pingLines = new HashMap<>();
    private volatile Map<Integer, List<PingPoint>> pingPoints = new HashMap<>();
    private volatile Map<Integer, List<XYChart.Data>> pingMarkers = new HashMap<>();
    private Double pingLineXMoveRatio = 0d;
    private Double pingLineXZoomRatio = 1d;
    private Double pingLineYZoomRatio = 1d;
    private long pingLineDuration = MAX_DISPLAYED_PING_DURATION;
    private long pingLineMinTime = 0L;
    private long pingLineMaxTime = MAX_DISPLAYED_PING_DURATION;

    // Speed test management
    private SpeedTest speedTest;
    private boolean speedTestStartState = false;
    private String speedTestServer;
    private String speedTestDownloadUrl;
    private String speedTestUploadUrl;

    // Speed test bar chart
    class SpeedTestBar {

        private int id;
        private EnumTypes.SpeedTestMode mode;
        private XYChart.Series<String, Number> series = new XYChart.Series<>();
        private Number minY = Long.MAX_VALUE;
        private Number maxY = 0;

        SpeedTestBar(int aInId, EnumTypes.SpeedTestMode aInMode) {
            id = aInId;
            mode = aInMode;
            String lTitle = Display.getViewResourceBundle().getString("catView.speedTestChart." + EnumTypes.SpeedTestMode.valueOf(aInMode));
            series.setName(lTitle);
            speedTestBarChart.getData().add(series);
        }

        public void delete() {
            speedTestBarChart.getData().remove(series);
        }

        public int getId() {
            return id;
        }

        public EnumTypes.SpeedTestMode getMode() {
            return mode;
        }

        public XYChart.Series<String, Number> getSeries() {
            return series;
        }

        public Number getMinY() {
            return minY;
        }

        public Number getMaxY() {
            return maxY;
        }

        public void setMinY(Number minY) {
            this.minY = minY;
        }

        public void setMaxY(Number maxY) {
            this.maxY = maxY;
        }

    }
    class SpeedTestPoint {

        private long x;
        private long y;
        private String category;
        private XYChart.Data point;
        private EnumTypes.SpeedTestType type;

        SpeedTestPoint(long aInX, long aInY, EnumTypes.SpeedTestType aInType) {
            x = aInX;
            y = aInY;
            category = (LocaleUtilities.getInstance().getMediumDateAndTimeFormat().format(aInX).replaceAll("\\d{4} ", "\\\n"));
            point = new XYChart.Data(category, aInY);
            type = aInType;
        }

        public XYChart.Data getPoint() {
            return point;
        }

        public long getX() {
            return x;
        }

        public long getY() {
            return y;
        }

        public String getCategory() {
            return category;
        }

        public EnumTypes.SpeedTestType getType() {
            return type;
        }

    }

    private static int speedTestBarsCount = 0;
    private CategoryAxis speedTestBarChartXAxis = new CategoryAxis();
    private NumberAxis speedTestBarChartYAxis = new NumberAxis();
    private BarChart<String, Number> speedTestBarChart = new BarChart<>(speedTestBarChartXAxis, speedTestBarChartYAxis);

    private volatile Map<Integer, SpeedTestBar> speedTestBars = new HashMap<>();
    private volatile Map<Integer, List<SpeedTestPoint>> speedTestPoints = new HashMap<>();
    private Double speedTestBarsXMoveRatio = 0d;
    private Double speedTestBarsXZoomRatio = 1d;
    private Double speedTestBarsYZoomRatio = 1d;
    private long speedTestBarsDuration = MAX_DISPLAYED_SPEED_TEST_DURATION;
    private long speedTestBarsMinTime = 0L;
    private long speedTestBarsMaxTime = MAX_DISPLAYED_PING_DURATION;

    // Speed test live line chart
    private NumberAxis liveSpeedTestChartXAxis = new NumberAxis();
    private NumberAxis liveSpeedTestChartYAxis = new NumberAxis();
    private LineChart<Number, Number> liveSpeedTestChart = new LineChart<>(liveSpeedTestChartXAxis, liveSpeedTestChartYAxis);
    private XYChart.Series<Number, Number> liveSpeedTestDownloadSeries = new XYChart.Series<>();
    private XYChart.Series<Number, Number> liveSpeedTestUploadSeries = new XYChart.Series<>();

    // FXML
    @FXML private Label nameLabel;
    @FXML private Label nameCountLabel;
    @FXML private Label pingsCountLabel;
    @FXML private Label lostConnectionsCountLabel;
    @FXML private Label lastLostConnectionTimeLabel;
    @FXML private Label lastLostConnectionDetailsLabel;

    @FXML private GridPane interfaceTypeSummaries;
    @FXML private TabPane wanMonitoring;
    @FXML private Tab wan1MonitoringTab;
    @FXML private Tab wan2MonitoringTab;
    @FXML private GridPane wan1MonitoringGridPane;
    @FXML private GridPane wan2MonitoringGridPane;
    @FXML private TabPane lanMonitoring;
    @FXML private Tab lan1MonitoringTab;
    @FXML private Tab lan2MonitoringTab;
    @FXML private GridPane lan1MonitoringGridPane;
    @FXML private GridPane lan2MonitoringGridPane;
    @FXML private ImageView pauseButtonImageView;
    @FXML private ImageView emailButtonImageView;

    @FXML private ImageView clearConsoleButtonImageView;
    @FXML private ImageView generalEmailButtonImageView;
    @FXML private RadioButton activeAlarmsButton;
    @FXML private RadioButton historicalAlarmsButton;
    @FXML private Label activeAlarmsCountLabel;
    @FXML private Label historicalAlarmsCountLabel;

    @FXML private ImageView globalStateImageView;
    @FXML private ImageView wanStateImageView;
    @FXML private ImageView lanStateImageView;
    @FXML private ImageView networkInterface1ImageView;
    @FXML private ImageView networkInterface2ImageView;

    @FXML private TextField activeAlarmsFilterTextField;
    @FXML private TextField historicalAlarmsFilterTextField;

    @FXML private TableView<Alarm> activateAlarmsTable;
    @FXML private TableColumn<Alarm, String> activeAlarmStateColumn;
    @FXML private TableColumn<Alarm, String> activeAlarmSeverityColumn;
    @FXML private TableColumn<Alarm, Integer> activeAlarmIdColumn;
    @FXML private TableColumn<Alarm, String> activeAlarmNameColumn;
    @FXML private TableColumn<Alarm, String> activeAlarmSiteColumn;
    @FXML private TableColumn<Alarm, String> activeAlarmObjectTypeColumn;
    @FXML private TableColumn<Alarm, String> activeAlarmObjectNameColumn;
    @FXML private TableColumn<Alarm, Integer> activeAlarmOccurrencesColumn;
    @FXML private TableColumn<Alarm, Date> activeAlarmRaiseDateColumn;
    @FXML private TableColumn<Alarm, Date> activeAlarmModificationDateColumn;
    @FXML private TableColumn<Alarm, Date> activeAlarmClearDateColumn;
    @FXML private TableColumn<Alarm, String> activeAlarmTypeColumn;
    @FXML private TableColumn<Alarm, String> activeAlarmProbableCauseColumn;
    @FXML private TableColumn<Alarm, String> activeAlarmAdditionalInfoColumn;
    @FXML private TableColumn<Alarm, String> activeAlarmRemedialActionColumn;

    @FXML private TableView<Alarm> historicalAlarmsTable;
    @FXML private TableColumn<Alarm, String> historicalAlarmStateColumn;
    @FXML private TableColumn<Alarm, String> historicalAlarmSeverityColumn;
    @FXML private TableColumn<Alarm, Integer> historicalAlarmIdColumn;
    @FXML private TableColumn<Alarm, String> historicalAlarmNameColumn;
    @FXML private TableColumn<Alarm, String> historicalAlarmSiteColumn;
    @FXML private TableColumn<Alarm, String> historicalAlarmObjectTypeColumn;
    @FXML private TableColumn<Alarm, String> historicalAlarmObjectNameColumn;
    @FXML private TableColumn<Alarm, Integer> historicalAlarmOccurrencesColumn;
    @FXML private TableColumn<Alarm, Date> historicalAlarmRaiseDateColumn;
    @FXML private TableColumn<Alarm, Date> historicalAlarmModificationDateColumn;
    @FXML private TableColumn<Alarm, Date> historicalAlarmClearDateColumn;
    @FXML private TableColumn<Alarm, String> historicalAlarmTypeColumn;
    @FXML private TableColumn<Alarm, String> historicalAlarmProbableCauseColumn;
    @FXML private TableColumn<Alarm, String> historicalAlarmAdditionalInfoColumn;
    @FXML private TableColumn<Alarm, String> historicalAlarmRemedialActionColumn;

    @FXML private TabPane generalTabPane;
    @FXML private TabPane chartTabPane;

    @FXML private ScrollPane consoleScrollPane;
    @FXML private TextFlow consoleTextFlow;
    @FXML private Tab consoleTab;

    @FXML private ScrollPane speedTestScrollPane;
    @FXML private TextFlow speedTestTextFlow;
    @FXML private Tab speedTestTab;
    @FXML private Label speedTestServerLabel;
    @FXML private Button speedTestStartStopButton;
    @FXML private Button speedTestConfigureButton;

    @FXML private HBox pingLineChartContainer;
    @FXML private CheckBox pingLineManageCheckBox;
    @FXML private CheckBox pingLineLanFilterCheckBox;
    @FXML private CheckBox pingLineWanFilterCheckBox;
    @FXML private CheckBox pingLineInterface1FilterCheckBox;
    @FXML private CheckBox pingLineInterface2FilterCheckBox;
    @FXML private Slider pingLineChartVerticalZoomSlider;
    @FXML private Slider pingLineChartHorizontalMoveSlider;
    @FXML private Slider pingLineChartHorizontalZoomSlider;

    @FXML private HBox liveSpeedTestChartContainer;

    @FXML private HBox speedTestBarChartContainer;
    @FXML private CheckBox speedTestManageCheckBox;
    @FXML private CheckBox speedTestDownloadFilterCheckBox;
    @FXML private CheckBox speedTestUploadFilterCheckBox;
    @FXML private Slider speedTestBarChartVerticalZoomSlider;
    @FXML private Slider speedTestBarChartHorizontalMoveSlider;
    @FXML private Slider speedTestBarChartHorizontalZoomSlider;

    @FXML private void initialize() {

        // Initialize monitoring tab panes hash map
        monitoringTabPanes.put(EnumTypes.AddressType.WAN, wanMonitoring);
        monitoringTabPanes.put(EnumTypes.AddressType.LAN, lanMonitoring);

        // Initialize monitoring tabs by address list
        List<Tab> wanMonitoringTabs = new ArrayList<>();
        wanMonitoringTabs.add(wan1MonitoringTab);
        wanMonitoringTabs.add(wan2MonitoringTab);
        monitoringTabs.put(EnumTypes.AddressType.WAN, wanMonitoringTabs);
        List<Tab> lanMonitoringTabs = new ArrayList<>();
        lanMonitoringTabs.add(lan1MonitoringTab);
        lanMonitoringTabs.add(lan2MonitoringTab);
        monitoringTabs.put(EnumTypes.AddressType.LAN, lanMonitoringTabs);

        // Initialize monitoring grid panes by address list
        List<GridPane> wanMonitoringGridPanes = new ArrayList<>();
        wanMonitoringGridPanes.add(wan1MonitoringGridPane);
        wanMonitoringGridPanes.add(wan2MonitoringGridPane);
        monitoringGridPanes.put(EnumTypes.AddressType.WAN, wanMonitoringGridPanes);
        List<GridPane> lanMonitoringGridPanes = new ArrayList<>();
        lanMonitoringGridPanes.add(lan1MonitoringGridPane);
        lanMonitoringGridPanes.add(lan2MonitoringGridPane);
        monitoringGridPanes.put(EnumTypes.AddressType.LAN, lanMonitoringGridPanes);

        // Reset style of monitoring job tabs title when it is selected
        for (EnumTypes.AddressType lAddressType : EnumTypes.AddressType.values()) {
            monitoringTabPanes.get(lAddressType).getSelectionModel().selectedItemProperty().addListener(
                    new ChangeListener<Tab>() {
                        @Override
                        public void changed(ObservableValue<? extends Tab> ov, Tab oldTab, Tab newTab) {
                            newTab.setStyle("-fx-font-style: normal;");
                            newTab.setText(newTab.getText().replaceAll(" \\(-*[0-9]+\\)$", ""));
                        }
                    });
        }

        // Tabs
        generalTabPane.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTab, newTab) -> States.getInstance().saveValue("general.selectedTab", newTab.getTabPane().getSelectionModel().getSelectedIndex()));
        chartTabPane.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTab, newTab) -> States.getInstance().saveValue("charts.selectedTab", newTab.getTabPane().getSelectionModel().getSelectedIndex()));
        generalTabPane.getSelectionModel().select(States.getInstance().getIntegerValue("general.selectedTab", 0));
        chartTabPane.getSelectionModel().select(States.getInstance().getIntegerValue("charts.selectedTab", 0));

        // Consoles
        firstDisplay.put(consoleTextFlow, true);
        tabs.put(consoleTextFlow, consoleTab);

        // Add listener to detail text flow so that it auto scrolls by default to bottom each time the text changes
        consoleTextFlow.getChildren().addListener(consoleScrollPaneChangeListener);

        // Add listener to console scroll pane so that auto scroll is enabled or disabled depending on user action
        consoleScrollPane.vvalueProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    if(oldValue.doubleValue() == 1.0d){
                        // If user scrolls to bottom, enable auto scroll to bottom
                        consoleTextFlow.getChildren().removeListener(consoleScrollPaneChangeListener);
                        consoleTextFlow.getChildren().addListener(consoleScrollPaneChangeListener);
                    } else {
                        // If user scrolls up, disable auto scroll to bottom
                        consoleTextFlow.getChildren().removeListener(consoleScrollPaneChangeListener);
                    }
                }
                                                      );

        // Reset style of the console tab title when it is selected
        consoleTab.getTabPane().getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<Tab>() {
                    @Override
                    public void changed(ObservableValue<? extends Tab> ov, Tab oldTab, Tab newTab) {
                        if (newTab.equals(consoleTab)) {
                            newTab.setStyle("-fx-font-style: normal;");
                            newTab.setText(newTab.getText().replaceAll(" \\(-*[0-9]+\\)$", ""));
                        }
                    }
                });

        printConsole(new Message(Display.getViewResourceBundle().getString("catView.console.startApplication"), EnumTypes.MessageLevel.INFO));

        firstDisplay.put(speedTestTextFlow, true);
        tabs.put(speedTestTextFlow, speedTestTab);

        // Add listener to speed test text flow so that it auto scrolls by default to bottom each time the text changes
        speedTestTextFlow.getChildren().addListener(speedTestScrollPaneChangeListener);

        // Add listener to speed test scroll pane so that auto scroll is enabled or disabled depending on user action
        speedTestScrollPane.vvalueProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    if(oldValue.doubleValue() == 1.0d){
                        // If user scrolls to bottom, enable auto scroll to bottom
                        speedTestTextFlow.getChildren().removeListener(speedTestScrollPaneChangeListener);
                        speedTestTextFlow.getChildren().addListener(speedTestScrollPaneChangeListener);
                    } else {
                        // If user scrolls up, disable auto scroll to bottom
                        speedTestTextFlow.getChildren().removeListener(speedTestScrollPaneChangeListener);
                    }
                });

        // Initialize ping line chart
        pingLineFilterCheckBoxes.add(pingLineInterface1FilterCheckBox);
        pingLineInterface1FilterCheckBox.setVisible(false);
        pingLineFilterCheckBoxes.add(pingLineInterface2FilterCheckBox);
        pingLineInterface2FilterCheckBox.setVisible(false);

        pingLineChart.getYAxis().setLabel(Display.getViewResourceBundle().getString("catView.pingChartView.lineChart.yAxis.title"));
        pingLineChart.setAnimated(false);
        HBox.setHgrow(pingLineChart, Priority.ALWAYS);
        pingLineChartContainer.getChildren().add(pingLineChart);
        pingLineChartXAxis.setAutoRanging(false);
        pingLineChartXAxis.setTickUnit(5000);
        pingLineChartXAxis.setMinorTickVisible(false);
        pingLineChartYAxis.setAutoRanging(false);
        pingLineChartYAxis.setMinorTickVisible(false);

        pingLineChartHorizontalMoveSlider.valueProperty().addListener(pingLinesHorizontalSliderChangeListener);
        pingLineChartHorizontalZoomSlider.valueProperty().addListener(pingLinesHorizontalSliderChangeListener);
        pingLineChartVerticalZoomSlider.valueProperty().addListener(pingLinesVerticalSliderChangeListener);

        pingLineManageCheckBox.setSelected(States.getInstance().getBooleanValue(Constants.PING_CHART_ENABLE_STATE, true));
        pingLineWanFilterCheckBox.setSelected(States.getInstance().getBooleanValue(Constants.PING_CHART_DISPLAY_WAN_LINE_STATE, true));
        pingLineLanFilterCheckBox.setSelected(States.getInstance().getBooleanValue(Constants.PING_CHART_DISPLAY_LAN_LINE_STATE, true));
        pingLineInterface1FilterCheckBox.setSelected(States.getInstance().getBooleanValue(Constants.PING_CHART_DISPLAY_INTERFACE1_LINE_STATE, true));
        pingLineInterface2FilterCheckBox.setSelected(States.getInstance().getBooleanValue(Constants.PING_CHART_DISPLAY_INTERFACE2_LINE_STATE, true));
        pingLineChartHorizontalZoomSlider.setValue(States.getInstance().getDoubleValue(Constants.PING_CHART_HORIZONTAL_ZOOM_SLIDER_STATE, 100d));
        pingLineChartVerticalZoomSlider.setValue(States.getInstance().getDoubleValue(Constants.PING_CHART_VERTICAL_ZOOM_SLIDER_STATE, 100d));

        pingLineChartXAxis.setTickLabelFormatter(new StringConverter<Number>() {

            @Override
            public String toString(Number value) {
                return (LocaleUtilities.getInstance().getMediumDateAndTimeFormat().format(value.intValue() + timeReference).replaceAll("\\d{4} ", "\\\n"));
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });

        checkPingChartState();

        // Initialize speed test bar chart
        speedTestBarFilterCheckBoxes.add(speedTestDownloadFilterCheckBox);
        speedTestBarFilterCheckBoxes.add(speedTestUploadFilterCheckBox);

        speedTestBarChart.getYAxis().setLabel(Display.getViewResourceBundle().getString("catView.speedTestChart.barChart.yAxis.title"));
        speedTestBarChart.setAnimated(false);
        HBox.setHgrow(speedTestBarChart, Priority.ALWAYS);
        speedTestBarChartContainer.getChildren().add(speedTestBarChart);
        speedTestBarChartXAxis.setAutoRanging(true);
        speedTestBarChartYAxis.setAutoRanging(false);
        speedTestBarChartYAxis.setMinorTickVisible(false);

        speedTestBarChartHorizontalMoveSlider.valueProperty().addListener(speedTestBarsHorizontalSliderChangeListener);
        speedTestBarChartHorizontalZoomSlider.valueProperty().addListener(speedTestBarsHorizontalSliderChangeListener);
        speedTestBarChartVerticalZoomSlider.valueProperty().addListener(speedTestBarsVerticalSliderChangeListener);

        speedTestManageCheckBox.setSelected(States.getInstance().getBooleanValue(Constants.SPEED_TEST_CHART_ENABLE_STATE, true));
        speedTestDownloadFilterCheckBox.setSelected(States.getInstance().getBooleanValue(Constants.SPEED_TEST_CHART_DISPLAY_DOWNLOAD_STATE, true));
        speedTestUploadFilterCheckBox.setSelected(States.getInstance().getBooleanValue(Constants.SPEED_TEST_CHART_DISPLAY_UPLOAD_STATE, true));
        speedTestBarChartHorizontalZoomSlider.setValue(States.getInstance().getDoubleValue(Constants.SPEED_TEST_CHART_HORIZONTAL_ZOOM_SLIDER_STATE, 100d));
        speedTestBarChartVerticalZoomSlider.setValue(States.getInstance().getDoubleValue(Constants.SPEED_TEST_CHART_VERTICAL_ZOOM_SLIDER_STATE, 100d));
        speedTestBarChart.setLegendVisible(false);

        addSpeedTestSeries(EnumTypes.SpeedTestMode.DOWNLOAD);
        addSpeedTestSeries(EnumTypes.SpeedTestMode.UPLOAD);

        checkSpeedTestChartState();

        // Initialize live speed test chart
        liveSpeedTestChart.getYAxis().setLabel(Display.getViewResourceBundle().getString("catView.liveSpeedTestChartView.lineChart.yAxis.title"));
        liveSpeedTestChart.setAnimated(false);
        HBox.setHgrow(liveSpeedTestChart, Priority.ALWAYS);
        liveSpeedTestChart.setLegendSide(Side.RIGHT);
        liveSpeedTestChartContainer.getChildren().add(liveSpeedTestChart);
        liveSpeedTestChartXAxis.setAutoRanging(false);
        liveSpeedTestChartXAxis.setUpperBound(100);
        liveSpeedTestChartXAxis.setTickMarkVisible(false);
        liveSpeedTestChartXAxis.setTickLabelsVisible(false);
        liveSpeedTestChartXAxis.setMinorTickVisible(false);
        liveSpeedTestChart.setVerticalGridLinesVisible(false);
        liveSpeedTestChartYAxis.setAutoRanging(true);
        liveSpeedTestChartYAxis.setMinorTickVisible(false);
        liveSpeedTestChart.getData().add(liveSpeedTestDownloadSeries);
        liveSpeedTestChart.getData().add(liveSpeedTestUploadSeries);
        liveSpeedTestDownloadSeries.setName(Display.getViewResourceBundle().getString("speedtest.mode.download"));
        liveSpeedTestUploadSeries.getNode().getStyleClass().add("chart-upload");
        liveSpeedTestUploadSeries.setName(Display.getViewResourceBundle().getString("speedtest.mode.upload"));

        for (Node lNode : liveSpeedTestChart.getChildrenUnmodifiable()) {
            if (lNode instanceof Legend) {
                int i = 0;
                for (Legend.LegendItem lLegendItem: ((Legend) lNode).getItems()) {
                    lLegendItem.getSymbol().getStyleClass().add("chart-legend-speedtest-" + i++);
                }
            }
        }

        reloadSpeedTestConfiguration();
        switchStopStartSpeedTestButton();
        ImageView lImageViewConfigure = new ImageView(new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_CONFIGURE).toString()));
        lImageViewConfigure.setFitHeight(20d); lImageViewConfigure.setFitWidth(20d);
        speedTestConfigureButton.setGraphic(lImageViewConfigure);
        ImageView lImageViewSpeedTest = new ImageView(new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_SPEED_TEST).toString()));
        lImageViewSpeedTest.setFitHeight(20d); lImageViewSpeedTest.setFitWidth(20d);
        speedTestStartStopButton.setGraphic(lImageViewSpeedTest);

        // Tooltips
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) {

            Tooltip lClearConsoleTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.clearConsole"));
            Tooltip.install(clearConsoleButtonImageView, lClearConsoleTooltip);

            Tooltip lConfigureSpeedTestTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.speedTest.tooltip.configure"));
            Tooltip.install(speedTestConfigureButton, lConfigureSpeedTestTooltip);

            Tooltip lManageCheckBoxTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.pingChartView.tooltip.manage"));
            pingLineManageCheckBox.setTooltip(lManageCheckBoxTooltip);
            Tooltip lWanFilterCheckBoxTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.pingChartView.tooltip.WAN"));
            pingLineWanFilterCheckBox.setTooltip(lWanFilterCheckBoxTooltip);
            Tooltip lLanFilterCheckBoxTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.pingChartView.tooltip.LAN"));
            pingLineLanFilterCheckBox.setTooltip(lLanFilterCheckBoxTooltip);
            Tooltip lHorizontalMoveSliderTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.pingChartView.tooltip.horizontalMoveSlider"));
            pingLineChartHorizontalMoveSlider.setTooltip(lHorizontalMoveSliderTooltip);
            Tooltip lHorizontalZoomSliderTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.pingChartView.tooltip.horizontalZoomSlider"));
            pingLineChartHorizontalZoomSlider.setTooltip(lHorizontalZoomSliderTooltip);
            Tooltip lVerticalZoomSliderTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.pingChartView.tooltip.verticalZoomSlider"));
            pingLineChartVerticalZoomSlider.setTooltip(lVerticalZoomSliderTooltip);

            Tooltip lSpeedTestManageCheckBoxTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.speedTestChart.tooltip.manage"));
            speedTestManageCheckBox.setTooltip(lSpeedTestManageCheckBoxTooltip);
            Tooltip lSpeedTestDownloadFilterCheckBoxTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.speedTestChart.tooltip.download"));
            speedTestDownloadFilterCheckBox.setTooltip(lSpeedTestDownloadFilterCheckBoxTooltip);
            Tooltip lSpeedTestUploadFilterCheckBoxTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.speedTestChart.tooltip.upload"));
            speedTestUploadFilterCheckBox.setTooltip(lSpeedTestUploadFilterCheckBoxTooltip);
            Tooltip lSpeedTestHorizontalMoveSliderTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.speedTestChart.tooltip.horizontalMoveSlider"));
            speedTestBarChartHorizontalMoveSlider.setTooltip(lSpeedTestHorizontalMoveSliderTooltip);
            Tooltip lSpeedTestHorizontalZoomSliderTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.speedTestChart.tooltip.horizontalZoomSlider"));
            speedTestBarChartHorizontalZoomSlider.setTooltip(lSpeedTestHorizontalZoomSliderTooltip);
            Tooltip lSpeedTestVerticalZoomSliderTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.speedTestChart.tooltip.verticalZoomSlider"));
            speedTestBarChartVerticalZoomSlider.setTooltip(lSpeedTestVerticalZoomSliderTooltip);

        }

    }

    /**
     * Clears all consoles
     */
    @FXML private void clearConsoles() {

        // Prepare confirmation dialog box
        Alert lConfirmation = new Alert(Alert.AlertType.CONFIRMATION, Display.getViewResourceBundle().getString("confirm.clearConsole.question"), ButtonType.YES, ButtonType.NO);
        lConfirmation.setHeaderText(Display.getViewResourceBundle().getString("confirm.clearConsole.title"));
        lConfirmation.initModality(Modality.APPLICATION_MODAL);

        // Display confirmation dialog box
        Optional<ButtonType> lResponse = lConfirmation.showAndWait();

        // OK is pressed
        if (lResponse.isPresent() && lResponse.get().equals(ButtonType.YES)) {
            clearAllMessages();
            firstDisplay.put(consoleTextFlow, true);
            tabs.put(consoleTextFlow, consoleTab);
        }

    }

    /**
     * Starts a new speed test if no speed test is currently running otherwise stops the running speed test
     */
    @FXML private void startStopSpeedTest() {

        if (speedTestStartState) {
            if (speedTestDownloadUrl != null && speedTestUploadUrl != null) {
                if (speedTest == null || speedTest.isInterrupted()) speedTest = SpeedTestFactory.getInstance(EnumTypes.SpeedTestType.ON_REQUEST);
                speedTest.start(speedTestDownloadUrl, speedTestUploadUrl);
            }

        } else {
            // Stops current speed test (only one is potentially on-going, nothing is done if trying to stop a not on-going test)
            if (speedTest != null) speedTest.stop();
            if (GlobalMonitoring.getSpeedTest() != null) GlobalMonitoring.getSpeedTest().stop();
        }
    }

    @FXML private void configureSpeedTest() {
        ConfigureSpeedTestDialog.getInstance(Cat.getInstance().getMainStage()).show();
    }

    /**
     * Initializes alarms tables
     */
    public void initializeAlarmsTables() {

        // ACTIVE ALARMS TABLE

        // Initialize table fields
        activeAlarmStateColumn.setCellValueFactory(cellData -> cellData.getValue().stateProperty());
        activeAlarmStateColumn.setCellFactory(column -> internationalizedFormatter("state"));
        activeAlarmSeverityColumn.setCellValueFactory(cellData -> cellData.getValue().severityProperty());
        activeAlarmSeverityColumn.setCellFactory(column -> severityFormatter());
        activeAlarmIdColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        activeAlarmIdColumn.setCellFactory(column -> integerFormatter());
        activeAlarmNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        activeAlarmNameColumn.setCellFactory(column -> stringFormatter());
        activeAlarmSiteColumn.setCellValueFactory(cellData -> cellData.getValue().siteProperty());
        activeAlarmSiteColumn.setCellFactory(column -> stringFormatter());
        activeAlarmObjectTypeColumn.setCellValueFactory(cellData -> cellData.getValue().objectTypeProperty());
        activeAlarmObjectTypeColumn.setCellFactory(column -> internationalizedFormatter("objectType"));
        activeAlarmObjectNameColumn.setCellValueFactory(cellData -> cellData.getValue().objectNameProperty());
        activeAlarmObjectNameColumn.setCellFactory(column -> stringFormatter());
        activeAlarmOccurrencesColumn.setCellValueFactory(cellData -> cellData.getValue().occurrencesProperty().asObject());
        activeAlarmOccurrencesColumn.setCellFactory(column -> integerFormatter());
        activeAlarmRaiseDateColumn.setCellValueFactory(cellData -> cellData.getValue().raiseDateProperty());
        activeAlarmRaiseDateColumn.setCellFactory(column -> dateAndTimeFormatter());
        activeAlarmModificationDateColumn.setCellValueFactory(cellData -> cellData.getValue().modificationDateProperty());
        activeAlarmModificationDateColumn.setCellFactory(column -> dateAndTimeFormatter());
        activeAlarmClearDateColumn.setCellValueFactory(cellData -> cellData.getValue().clearDateProperty());
        activeAlarmClearDateColumn.setCellFactory(column -> dateAndTimeFormatter());
        activeAlarmTypeColumn.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        activeAlarmTypeColumn.setCellFactory(column -> internationalizedFormatter("type"));
        activeAlarmProbableCauseColumn.setCellValueFactory(cellData -> cellData.getValue().probableCauseProperty());
        activeAlarmProbableCauseColumn.setCellFactory(column -> stringFormatter());
        activeAlarmAdditionalInfoColumn.setCellValueFactory(cellData -> cellData.getValue().additionalInformationProperty());
        activeAlarmAdditionalInfoColumn.setCellFactory(column -> stringFormatter());
        activeAlarmRemedialActionColumn.setCellValueFactory(cellData -> cellData.getValue().remedialActionProperty());
        activeAlarmRemedialActionColumn.setCellFactory(column -> stringFormatter());

        // Wrap the ObservableList in a FilteredList (initially display all data).
        FilteredList<Alarm> lFilteredActiveAlarms = new FilteredList<>(GlobalMonitoring.getInstance().getActiveAlarmsList(), p -> true);

        // Set the filter Predicate whenever the filter changes.
        activeAlarmsFilterTextField.textProperty().addListener(alarmFilterListener(lFilteredActiveAlarms));

        // Wrap the FilteredList in a SortedList.
        sortedActiveAlarms = new SortedList<>(lFilteredActiveAlarms);

        // Bind the SortedList comparator to the TableView comparator.
        sortedActiveAlarms.comparatorProperty().bind(activateAlarmsTable.comparatorProperty());

        // Add sorted (and filtered) data to the table.
        activateAlarmsTable.setItems(sortedActiveAlarms);

        // Set the alarms count whenever the sorted alarms list changes
        sortedActiveAlarms.addListener(alarmsTableChangeListener(sortedActiveAlarms, activeAlarmsCountLabel));

        // Apply preferences and build contextual menus
        applyAlarmsTablePreferences(activateAlarmsTable);
        activateAlarmsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        buildTableBehaviour(activateAlarmsTable);
        buildRowsBehaviour(activateAlarmsTable);

        // Sort by default on raise date descending
        activeAlarmRaiseDateColumn.setSortType(TableColumn.SortType.DESCENDING);
        activateAlarmsTable.getSortOrder().add(activeAlarmRaiseDateColumn);
        activeAlarmRaiseDateColumn.setSortable(true);
        activateAlarmsTable.sort();

        // HISTORICAL ALARMS

        // Initialize alarms table fields
        historicalAlarmStateColumn.setCellValueFactory(cellData -> cellData.getValue().stateProperty());
        historicalAlarmStateColumn.setCellFactory(column -> internationalizedFormatter("state"));
        historicalAlarmSeverityColumn.setCellValueFactory(cellData -> cellData.getValue().severityProperty());
        historicalAlarmSeverityColumn.setCellFactory(column -> severityFormatter());
        historicalAlarmIdColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        historicalAlarmIdColumn.setCellFactory(column -> integerFormatter());
        historicalAlarmNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        historicalAlarmNameColumn.setCellFactory(column -> stringFormatter());
        historicalAlarmSiteColumn.setCellValueFactory(cellData -> cellData.getValue().siteProperty());
        historicalAlarmSiteColumn.setCellFactory(column -> stringFormatter());
        historicalAlarmObjectTypeColumn.setCellValueFactory(cellData -> cellData.getValue().objectTypeProperty());
        historicalAlarmObjectTypeColumn.setCellFactory(column -> internationalizedFormatter("objectType"));
        historicalAlarmObjectNameColumn.setCellValueFactory(cellData -> cellData.getValue().objectNameProperty());
        historicalAlarmObjectNameColumn.setCellFactory(column -> stringFormatter());
        historicalAlarmOccurrencesColumn.setCellValueFactory(cellData -> cellData.getValue().occurrencesProperty().asObject());
        historicalAlarmOccurrencesColumn.setCellFactory(column -> integerFormatter());
        historicalAlarmRaiseDateColumn.setCellValueFactory(cellData -> cellData.getValue().raiseDateProperty());
        historicalAlarmRaiseDateColumn.setCellFactory(column -> dateAndTimeFormatter());
        historicalAlarmModificationDateColumn.setCellValueFactory(cellData -> cellData.getValue().modificationDateProperty());
        historicalAlarmModificationDateColumn.setCellFactory(column -> dateAndTimeFormatter());
        historicalAlarmClearDateColumn.setCellValueFactory(cellData -> cellData.getValue().clearDateProperty());
        historicalAlarmClearDateColumn.setCellFactory(column -> dateAndTimeFormatter());
        historicalAlarmTypeColumn.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        historicalAlarmTypeColumn.setCellFactory(column -> internationalizedFormatter("type"));
        historicalAlarmProbableCauseColumn.setCellValueFactory(cellData -> cellData.getValue().probableCauseProperty());
        historicalAlarmProbableCauseColumn.setCellFactory(column -> stringFormatter());
        historicalAlarmAdditionalInfoColumn.setCellValueFactory(cellData -> cellData.getValue().additionalInformationProperty());
        historicalAlarmAdditionalInfoColumn.setCellFactory(column -> stringFormatter());
        historicalAlarmRemedialActionColumn.setCellValueFactory(cellData -> cellData.getValue().remedialActionProperty());
        historicalAlarmRemedialActionColumn.setCellFactory(column -> stringFormatter());

        // Wrap the ObservableList in a FilteredList (initially display all data).
        FilteredList<Alarm> lFilteredHistoricalAlarms = new FilteredList<>(GlobalMonitoring.getInstance().getHistoricalAlarmsList(), p -> true);

        // Set the filter Predicate whenever the filter changes.
        historicalAlarmsFilterTextField.textProperty().addListener(alarmFilterListener(lFilteredHistoricalAlarms));

        // Wrap the FilteredList in a SortedList.
        sortedHistoricalAlarms = new SortedList<>(lFilteredHistoricalAlarms);

        // Bind the SortedList comparator to the TableView comparator.
        sortedHistoricalAlarms.comparatorProperty().bind(historicalAlarmsTable.comparatorProperty());

        // Add sorted (and filtered) data to the table.
        historicalAlarmsTable.setItems(sortedHistoricalAlarms);

        // Set the alarms count whenever the sorted alarms list changes
        sortedHistoricalAlarms.addListener(alarmsTableChangeListener(sortedHistoricalAlarms, historicalAlarmsCountLabel));

        // Apply preferences and build contextual menus
        applyAlarmsTablePreferences(historicalAlarmsTable);
        historicalAlarmsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        buildTableBehaviour(historicalAlarmsTable);
        buildRowsBehaviour(historicalAlarmsTable);

        // Sort by default on cleared date descending
        historicalAlarmRaiseDateColumn.setSortType(TableColumn.SortType.DESCENDING);
        historicalAlarmsTable.getSortOrder().add(historicalAlarmRaiseDateColumn);
        historicalAlarmRaiseDateColumn.setSortable(true);
        historicalAlarmsTable.sort();

    }

    // PREFERENCES

    /**
     * Saves alarms table preferences (visibility, width and position of columns)
     * @param aInAlarmsTable Alarms Table of which the preferences must be saved
     */
    private void saveAlarmsTablePreferences(TableView<Alarm> aInAlarmsTable) {
        int lIndex = 0;
        for (TableColumn lColumn: aInAlarmsTable.getColumns()) {
            Preferences.getInstance().setValue(lColumn.getId() + "Displayed", lColumn.isVisible());
            Preferences.getInstance().setValue(lColumn.getId() + "Width", lColumn.getWidth());
            Preferences.getInstance().setValue(lColumn.getId() + "Position", lIndex++);
        }
        Preferences.getInstance().savePropertyFile();
    }

    /**
     * Applies preferences to an alarms table (visibility, width and position of columns)
     * @param aInAlarmsTable Alarms Table to which the preferences must be applied
     */
    private void applyAlarmsTablePreferences(TableView<Alarm> aInAlarmsTable) {

        // Parse all columns
        int lIndex = 0;
        HashMap<Integer, TableColumn<Alarm, ?>> lOrderedColumns = new HashMap<>();
        for (TableColumn<Alarm, ?> lColumn: aInAlarmsTable.getColumns()) {
            lColumn.setVisible(Boolean.valueOf(Preferences.getInstance().getValue(lColumn.getId() + "Displayed", String.valueOf(lColumn.isVisible()))));
            lColumn.setPrefWidth(Double.valueOf(Preferences.getInstance().getValue(lColumn.getId() + "Width", String.valueOf(lColumn.getWidth()))));
            lOrderedColumns.put(Integer.valueOf(Preferences.getInstance().getValue(lColumn.getId() + "Position", String.valueOf(lIndex++))), lColumn);
        }

        aInAlarmsTable.getColumns().clear();

        for (int lColumnIndex = 0; lColumnIndex < lOrderedColumns.keySet().size(); lColumnIndex++) {
            aInAlarmsTable.getColumns().add(lOrderedColumns.get(lColumnIndex));
        }

    }

    // BEHAVIOURS (CONTEXT MENUS, MOUSE ACTIONS, SPECIFIC FORMATTING, ...)

    /**
     * Builds alarms table context menu (display/hide columns, save and reset preferences)
     * Displayed only on table header because context menu on rows hide this menu for empty/non empty rows
     * @param aInAlarmsTable Alarms Table the context menu applies to
     */
    private void buildTableBehaviour(TableView<Alarm> aInAlarmsTable) {

        // Contextual menu
        ContextMenu lContextMenu = new ContextMenu();

        // Add title item and separator
        MenuItem lTitleItem = new MenuItem(Display.getViewResourceBundle().getString("catView.alarmView.contextMenu.displayColumnsMenuItem"));
        lTitleItem.setDisable(true);
        lTitleItem.getStyleClass().add("context-menu-title");
        lContextMenu.getItems().add(lTitleItem);
        SeparatorMenuItem lSeparatorMenuItem = new SeparatorMenuItem();
        lContextMenu.getItems().add(lSeparatorMenuItem);

        // Parse all table columns
        for (TableColumn lColumn: aInAlarmsTable.getColumns()) {

            // Add a check menu item for each column
            CheckMenuItem lCheckMenuItem = new CheckMenuItem(lColumn.getText());

            // Set the tick with the current state of the column
            lCheckMenuItem.setSelected(lColumn.isVisible());

            // Define the behaviour when the menu is selected
            lCheckMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    // Retrieve the column corresponding to the current menu item
                    for (TableColumn lColumn1 : aInAlarmsTable.getColumns()) {
                        if (lColumn1.getText().equals(lCheckMenuItem.getText())) {
                            // Set the column state to the current menu item state
                            lColumn1.setVisible(lCheckMenuItem.isSelected());
                            break;
                        }
                    }
                }
            });

            // Add the menu item for current column to the context menu
            lContextMenu.getItems().add(lCheckMenuItem);

        }

        lSeparatorMenuItem = new SeparatorMenuItem();
        lContextMenu.getItems().add(lSeparatorMenuItem);

        MenuItem lSaveDispositionMenuItem = new MenuItem(Display.getViewResourceBundle().getString("catView.alarmView.contextMenu.saveDispositionMenuItem"));
        lContextMenu.getItems().add(lSaveDispositionMenuItem);
        lSaveDispositionMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                // Retrieve the column corresponding to the current menu item
                saveAlarmsTablePreferences(aInAlarmsTable);
            }
        });

        MenuItem lRestoreDispositionMenuItem = new MenuItem(Display.getViewResourceBundle().getString("catView.alarmView.contextMenu.restoreDispositionMenuItem"));
        lContextMenu.getItems().add(lRestoreDispositionMenuItem);
        lRestoreDispositionMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                // Retrieve the column corresponding to the current menu item
                for (TableColumn lColumn: aInAlarmsTable.getColumns()) {
                    Preferences.getInstance().removeKey(lColumn.getId() + "Displayed");
                    Preferences.getInstance().removeKey(lColumn.getId() + "Width");
                    Preferences.getInstance().removeKey(lColumn.getId() + "Position");
                }
                Preferences.getInstance().savePropertyFile();
                Cat.restart();
            }
        });

        // Add the context menu to the table
        aInAlarmsTable.setContextMenu(lContextMenu);

    }

    /**
     * Builds alarms table rows context menu (actions on specific alarms)
     * @param aInAlarmsTable Alarms Table the context menu applies to
     */
    private void buildRowsBehaviour(TableView<Alarm> aInAlarmsTable) {

        final PseudoClass lAcknowledgedAlarmPseudoClass = PseudoClass.getPseudoClass("acknowledged");
        aInAlarmsTable.setRowFactory(new Callback<TableView<Alarm>, TableRow<Alarm>>() {

            @Override
            public TableRow<Alarm> call(TableView<Alarm> lTableView) {

                final TableRow<Alarm> lRow = new TableRow<Alarm>() {
                    @Override
                    protected void updateItem(Alarm aInAlarm, boolean aInEmpty) {
                        super.updateItem(aInAlarm, aInEmpty);
                        // Change style of acknowledged alarms using .table-row-cell:acknowledged pseudo class
                        boolean lAcknowledged = (!aInEmpty && aInAlarm.getState() == EnumTypes.AlarmState.ACKNOWLEDGED);
                        pseudoClassStateChanged(lAcknowledgedAlarmPseudoClass, lAcknowledged);
                    }
                };

                // Double click event
                lRow.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && (! lRow.isEmpty()) ) {
                        ArrayList<Alarm> lAlarms = new ArrayList<>();
                        lAlarms.add(lRow.getItem());
                        alarmDetailsDialogController.displayAlarm(lAlarms);
                    }
                });

                // Context menu
                ContextMenu lContextMenu = new ContextMenu();

                // Add title item and separator
                MenuItem lTitleItem = new MenuItem(Display.getViewResourceBundle().getString("catView.alarmView.contextMenu.alarmsActionsMenuItem"));
                lTitleItem.setDisable(true);
                lTitleItem.getStyleClass().add("context-menu-title");
                lContextMenu.getItems().add(lTitleItem);
                SeparatorMenuItem lSeparatorMenuItem = new SeparatorMenuItem();
                lContextMenu.getItems().add(lSeparatorMenuItem);

                // Add acknowledge/un-acknowledge alarm menu item if needed (active alarms table only)
                if (aInAlarmsTable.equals(activateAlarmsTable)) {

                    // Add listener on selection change to display the correct acknowledgement menu item
                    aInAlarmsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {

                        // Check if the whole selection is acknowledged or not acknowledged
                        boolean lAllAcknowledged = true;
                        boolean lAllUnacknowledged = true;
                        for (Alarm lAlarm: aInAlarmsTable.getSelectionModel().getSelectedItems()) {

                            if (lAlarm.getState().equals(EnumTypes.AlarmState.ACKNOWLEDGED)) {
                                lAllUnacknowledged = false;
                            }
                            if (!lAlarm.getState().equals(EnumTypes.AlarmState.ACKNOWLEDGED)) {
                                lAllAcknowledged = false;
                            }
                            lContextMenu.getItems().clear();
                            lContextMenu.getItems().add(lTitleItem);
                            lContextMenu.getItems().add(lSeparatorMenuItem);

                            // If whole selection is either acknowledged or not acknowledged, menu item must be added
                            if (lAllAcknowledged || lAllUnacknowledged) {

                                final MenuItem lAcknowledgeUnAcknowledgeAlarmMenuItem = (lAllAcknowledged) ?
                                        new MenuItem(Display.getViewResourceBundle().getString("catView.alarmView.contextMenu.unAcknowledgeAlarmsMenuItem")) :
                                        new MenuItem(Display.getViewResourceBundle().getString("catView.alarmView.contextMenu.acknowledgeAlarmsMenuItem"));

                                // Define the behaviour when the menu is selected
                                lAcknowledgeUnAcknowledgeAlarmMenuItem.setOnAction(new EventHandler<ActionEvent>() {

                                    @Override
                                    public void handle(ActionEvent event) {
                                        List<Alarm> lAlarms = new ArrayList<>(aInAlarmsTable.getSelectionModel().getSelectedItems());
                                        if (lAlarms.get(0).getState().equals(EnumTypes.AlarmState.ACKNOWLEDGED)) {
                                            GlobalMonitoring.getInstance().unAcknowledgeAlarms(lAlarms);
                                        } else {
                                            GlobalMonitoring.getInstance().acknowledgeAlarms(lAlarms);
                                        }
                                        aInAlarmsTable.refresh();
                                        aInAlarmsTable.getSelectionModel().clearSelection();
                                    }
                                });

                                // Add the menu item for current row to the context menu
                                lContextMenu.getItems().add(lAcknowledgeUnAcknowledgeAlarmMenuItem);

                            }

                            // Add clear and delete items
                            updateRowsContextMenu(aInAlarmsTable, lContextMenu);

                        }

                    });

                }

                // Add clear and delete items
                updateRowsContextMenu(aInAlarmsTable, lContextMenu);

                // Add the context menu to the row if needed
                lRow.contextMenuProperty().bind(
                        Bindings.when(Bindings.isNotNull(lRow.itemProperty()))
                                .then(lContextMenu)
                                .otherwise(new ContextMenu())
                );

                return lRow;

            }
        });

    }

    /**
     * Update alarms table rows context menu (actions on specific alarms) with unchangeable items
     * @param aInAlarmsTable Alarms Table the context menu applies to
     * @param aInContextMenu Context menu to update
     */
    private void updateRowsContextMenu(TableView<Alarm> aInAlarmsTable, ContextMenu aInContextMenu ) {

        // Add clear alarm menu item if needed
        if (aInAlarmsTable.equals(activateAlarmsTable)) {

            final MenuItem lClearAlarmMenuItem = new MenuItem(Display.getViewResourceBundle().getString("catView.alarmView.contextMenu.clearAlarmsMenuItem"));

            // Define the behaviour when the menu is selected
            lClearAlarmMenuItem.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    List<Alarm> lAlarms = new ArrayList<>(aInAlarmsTable.getSelectionModel().getSelectedItems());
                    GlobalMonitoring.getInstance().clearAlarms(lAlarms,
                            String.format(Display.getViewResourceBundle().getString("globalMonitoring.alarms.manualClear"), System.getProperty("user.name")));
                    aInAlarmsTable.getSelectionModel().clearSelection();
                }
            });

            // Add the menu item for current row to the context menu
            aInContextMenu.getItems().add(lClearAlarmMenuItem);

        }

        // Add delete alarm menu item
        final MenuItem lDeleteAlarmMenuItem = new MenuItem(Display.getViewResourceBundle().getString("catView.alarmView.contextMenu.deleteAlarmsMenuItem"));

        // Define the behaviour when the menu is selected
        lDeleteAlarmMenuItem.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {

                List<Alarm> lAlarms =  new ArrayList<> (aInAlarmsTable.getSelectionModel().getSelectedItems());

                // Prepare confirmation dialog box
                Alert lConfirmation = new Alert(Alert.AlertType.CONFIRMATION,
                                                Display.getViewResourceBundle().getString(lAlarms.size() > 1 ? "confirm.deleteAlarms.question" : "confirm.deleteAlarm.question"),
                        ButtonType.YES, ButtonType.NO);
                lConfirmation.setHeaderText(Display.getViewResourceBundle().getString(lAlarms.size() > 1 ? "confirm.deleteAlarms.title" : "confirm.deleteAlarm.title"));
                lConfirmation.initModality(Modality.APPLICATION_MODAL);

                // Display confirmation dialog box
                Optional<ButtonType> lResponse = lConfirmation.showAndWait();

                // OK is pressed
                if (lResponse.isPresent() && lResponse.get().equals(ButtonType.YES)) {

                    // Remove selected alarms from the correct list
                    if (aInAlarmsTable.equals(activateAlarmsTable)) {
                        GlobalMonitoring.getInstance().getActiveAlarmsList().removeAll(lAlarms);
                    } else {
                        GlobalMonitoring.getInstance().getHistoricalAlarmsList().removeAll(lAlarms);
                    }
                    aInAlarmsTable.getSelectionModel().clearSelection();

                }

            }
        });

        // Add the menu item for current row to the context menu
        aInContextMenu.getItems().add(lDeleteAlarmMenuItem);

        // Add display alarm menu item
        final MenuItem lDisplayAlarmMenuItem = new MenuItem(Display.getViewResourceBundle().getString("catView.alarmView.contextMenu.displayAlarmsMenuItem"));

        // Define the behaviour when the menu is selected
        lDisplayAlarmMenuItem.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                List<Alarm> lAlarms = new ArrayList<>(aInAlarmsTable.getSelectionModel().getSelectedItems());
                alarmDetailsDialogController.displayAlarm(lAlarms);
            }

        });

        SeparatorMenuItem lSeparatorMenuItem = new SeparatorMenuItem();
        aInContextMenu.getItems().add(lSeparatorMenuItem);

        // Add the menu item for current row to the context menu
        aInContextMenu.getItems().add(lDisplayAlarmMenuItem);

    }

    // FORMATTERS

    /**
     * Formats integers for table view columns display
     *
     * @return Formatted integer
     */
    private TableCell<Alarm, Integer> integerFormatter() {
        return new TableCell<Alarm, Integer>() {
            @Override
            protected void updateItem(Integer aInItem, boolean aInEmpty) {
                super.updateItem(aInItem, aInEmpty);
                if (aInItem != null && !aInEmpty) {
                    setText(aInItem.toString());
                    setStyle("-fx-alignment: top-center");
                } else {
                    setText("");
                }
            }
        };
    }

    /**
     * Formats strings for table view columns display
     *
     * @return Formatted string
     */
    private TableCell<Alarm, String> stringFormatter() {
        return new TableCell<Alarm, String>() {
            @Override
            protected void updateItem(String aInItem, boolean aInEmpty) {
                super.updateItem(aInItem, aInEmpty);
                if (aInItem != null && !aInEmpty) {
                    setText(aInItem);
                } else {
                    setText("");
                }
            }
        };
    }

    /**
     * Formats date and time for table view columns display
     *
     * @return Formatted date and time
     */
    private TableCell<Alarm, Date> dateAndTimeFormatter() {
        return new TableCell<Alarm, Date>() {
            @Override
            protected void updateItem(Date aInItem, boolean aInEmpty) {
                super.updateItem(aInItem, aInEmpty);
                if (aInItem != null && !aInEmpty) {
                    setText(LocaleUtilities.getInstance().getDateFormat().format(aInItem) + " " + LocaleUtilities.getInstance().getTimeFormat().format(aInItem.getTime()));
                    setStyle("-fx-alignment: top-center");
                } else {
                    setText("");
                }
            }
        };
    }

    /**
     * Formats severity for table view columns display
     *
     * @return Formatted severity
     */
    private TableCell<Alarm, String> severityFormatter() {
        return new TableCell<Alarm, String>() {
            @Override
            protected void updateItem(String aInItem, boolean aInEmpty) {
                super.updateItem(aInItem, aInEmpty);
                if (aInItem != null && !aInEmpty) {
                    String lKey = "catView.alarmView.severity." + aInItem;
                    String lSeverity = (Display.getViewResourceBundle().containsKey(lKey)) ? Display.getViewResourceBundle().getString(lKey) :
                                       Display.getViewResourceBundle().getString("catView.alarmView.severity.unknown");
                    setText(lSeverity);
                    for (int i = 0; i < this.getStyleClass().size(); i++) {
                        String lStyleClass = this.getStyleClass().get(i);
                        if (lStyleClass.contains("alarm")) this.getStyleClass().remove(lStyleClass);
                    }
                    this.getStyleClass().add("alarm-" + aInItem);
                } else {
                    setStyle("-fx-background-color: transparent;");
                    setText("");
                }
            }
        };
    }

    /**
     * Formats internationalized strings for table view columns display
     *
     * @return Formatted internationalized strings
     */
    private TableCell<Alarm, String> internationalizedFormatter(String aInField) {
        return new TableCell<Alarm, String>() {
            @Override
            protected void updateItem(String aInItem, boolean aInEmpty) {
                super.updateItem(aInItem, aInEmpty);
                if (aInItem != null && !aInEmpty) {
                    String lKey = "catView.alarmView." + aInField + "." + aInItem;
                    String lValue = (Display.getViewResourceBundle().containsKey(lKey)) ? Display.getViewResourceBundle().getString(lKey) :
                                    Display.getViewResourceBundle().getString("catView.alarmView." + aInField + ".unknown");
                    setText(lValue);
                } else {
                    setText("");
                }
            }
        };
    }

    // LISTENERS

    private ListChangeListener consoleScrollPaneChangeListener = (change) -> consoleScrollPane.setVvalue(1.0d);
    private ListChangeListener speedTestScrollPaneChangeListener = (change) -> speedTestScrollPane.setVvalue(1.0d);

    /**
     * Listener on changes on alarm filter text field
     * @param aInFilteredAlarms Filtered alarms list to which the change of filter applies
     * @return Listener
     */
    private ChangeListener<String> alarmFilterListener(FilteredList<Alarm> aInFilteredAlarms) {

        return (observable, oldValue, newValue) -> {

            aInFilteredAlarms.setPredicate(alarm -> {

                // If filter text is empty, display all alarms.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lLowerCaseFilter = newValue.toLowerCase();

                if (alarm.getState().toString().toLowerCase().contains(lLowerCaseFilter)) {
                    return true;
                } else if (alarm.getSeverity().toString().toLowerCase().contains(lLowerCaseFilter)) {
                    return true;
                } else if (String.valueOf(alarm.getId()).toLowerCase().contains(lLowerCaseFilter)) {
                    return true;
                } else if (alarm.getName().toLowerCase().contains(lLowerCaseFilter)) {
                    return true;
                } else if (alarm.getSite() != null && alarm.getSite().toLowerCase().contains(lLowerCaseFilter)) {
                    return true;
                } else if (alarm.getObjectType() != null && alarm.getObjectType().toString().toLowerCase().contains(lLowerCaseFilter)) {
                    return true;
                } else if (alarm.getObjectName() != null && alarm.getObjectName().toLowerCase().contains(lLowerCaseFilter)) {
                    return true;
                }
                return false; // Does not match.

            });

        };

    }

    /**
     * Listener on changes on a sorted alarms list
     * @param aInSortedAlarms Sorted alarms list the listener is applied to
     * @param aInCountLabel   Alarms count label to recompute
     * @return Listener
     */
    private ListChangeListener<Alarm> alarmsTableChangeListener(SortedList<Alarm> aInSortedAlarms, Label aInCountLabel) {
        return new ListChangeListener<Alarm>(){

            @Override
            public void onChanged(Change<? extends Alarm> aInChanges) {
                Platform.runLater(() -> {
                    aInCountLabel.setText(String.valueOf(aInSortedAlarms.size()));
                });
            }
        };
    }

    private ChangeListener<Number> pingLinesHorizontalSliderChangeListener = (obs, oldValue, newValue) -> {
        refreshAllPingSeries();
    };

    private ChangeListener<Number> pingLinesVerticalSliderChangeListener = (obs, oldValue, newValue) -> {
        zoomPingYAxis();
    };

    private ChangeListener<Number> speedTestBarsHorizontalSliderChangeListener = (obs, oldValue, newValue) -> {
        refreshAllSpeedTestSeries();
    };

    private ChangeListener<Number> speedTestBarsVerticalSliderChangeListener = (obs, oldValue, newValue) -> {
        zoomSpeedTestYAxis();
    };

    // GETTERS

    /**
     * Gets the speed test instance
     * @return Speed test instance
     */
    public SpeedTest getSpeedTest() {
        return speedTest;
    }

    /**
     * Gets the live speed test download series
     * @return Live speed test download series
     */
    public XYChart.Series<Number, Number> getLiveSpeedTestDownloadSeries() {
        return liveSpeedTestDownloadSeries;
    }

    /**
     * Gets the live speed test upload series
     * @return Live speed test upload series
     */
    public XYChart.Series<Number, Number> getLiveSpeedTestUploadSeries() {
        return liveSpeedTestUploadSeries;
    }

    /**
     * Gets the grid pane for displaying the summaries by interface type (eth, wifi)
     * @return Interface type summaries grid pane
     */
    public GridPane getInterfaceTypeSummaries() {
        return interfaceTypeSummaries;
    }

    /**
     * Indicates if general email is audibleEnabled
     * @return true if general email is audibleEnabled, false otherwise
     */
    public boolean isButtonGeneralEmailEnabled() {
        return isButtonGeneralEmailEnabled;
    }

    // SETTERS

    /**
     * Refreshes jobs count
     */
    public void refreshJobsCount() {
        nameCountLabel.setText(Long.toString(jobsCount));
    }

    /**
     * Refreshes pings count (lost and total)
     */
    public void refreshPingsCount() {

        NumberFormat lPercentageFormat = NumberFormat.getPercentInstance();
        lPercentageFormat.setMinimumFractionDigits(0);
        String lText = String.format(LocaleUtilities.getInstance().getCurrentLocale(), "%d/%d ", lostPingsCount, pingsCount);
        if (lostPingsCount != 0) {
            lText += String.format(LocaleUtilities.getInstance().getCurrentLocale(), "(%s)",lPercentageFormat.format((float) lostPingsCount / (float) pingsCount));
        }
        pingsCountLabel.setText(lText);

    }

    /**
     * Increments pings count
     */
    public void incrementPingsCount() {
        pingsCount++;
        refreshPingsCount();
    }

    /**
     * Increments lost pings count
     */
    public void incrementLostPingsCount() {
        lostPingsCount++;
        refreshPingsCount();
    }

    /**
     * Refreshes lost connections count (on-going and total)
     */
    public void refreshConnectionsLostCount() {

        lostConnectionsCountLabel.setText(Long.toString(ongoingLostConnectionsCount) + '/' + Long.toString(lostConnectionsCount));
        lastLostConnectionTimeLabel.setText(lostTime);
        lastLostConnectionDetailsLabel.setText(lostDetails);
        if (ongoingLostConnectionsCount > 0) {
            nameLabel.setTextFill(Color.web("red"));
            nameCountLabel.setTextFill(Color.web("red"));
        } else {
            nameLabel.setTextFill(Color.web("green"));
            nameCountLabel.setTextFill(Color.web("green"));
        }

    }

    /**
     * Indicates that a connection is lost
     */
    public void loseConnection(Date aInLostConnectionDate, String aInLostConnectionHost, EnumTypes.AddressType aInAddressType, EnumTypes.InterfaceType aInInterfaceType) {

        ongoingLostConnectionsCount++;
        lostConnectionsCount++;

        // Compute text for last lost time and last lost details labels
        lostTime =
                LocaleUtilities.getInstance().getDateFormat().format(aInLostConnectionDate) +
                        " " + Display.getViewResourceBundle().getString("lastLostConnectionDetailTime") + " " +
                        LocaleUtilities.getInstance().getTimeFormat().format(aInLostConnectionDate);
        lostDetails = (aInAddressType == null || aInInterfaceType == null) ? aInLostConnectionHost : aInLostConnectionHost + " (" + aInInterfaceType + ", " + aInAddressType + ")";

        refreshConnectionsLostCount();
    }

    /**
     * Indicates that a connection is recovered
     */
    public void recoverConnection() {
        ongoingLostConnectionsCount--;
        refreshConnectionsLostCount();
    }

    /**
     * Sets alarm details controller
     * @param aInAlarmDetailsDialogController AlarmConfiguration details controller
     */
    public void setAlarmDetailsDialogController(AlarmDetailsDialog aInAlarmDetailsDialogController) {
        alarmDetailsDialogController = aInAlarmDetailsDialogController;
    }

    /**
     * Sets active alarms count
     */
    public void setActiveAlarmsCount() {
        activeAlarmsCountLabel.setText(String.valueOf(sortedActiveAlarms.size()));
    }

    /**
     * Sets historical alarms count
     */
    public void setHistoricalAlarmsCount() {
        historicalAlarmsCountLabel.setText(String.valueOf(sortedHistoricalAlarms.size()));
    }

    // METHODS

    /**
     * Indicates if active alarms table is selected (if no, historical alarms table is selected)
     * @return true if active alarms table is selected, false if historical alarms table is selected
     */
    public boolean isActiveAlarmsTableSelected() {
        return activeAlarmsButton.isSelected();
    }

    /**
     * Refreshes active alarms table and removes selection
     */
    public void refreshActiveAlarmsListAndRemoveSelection() {
        activateAlarmsTable.getSelectionModel().clearSelection();
        activateAlarmsTable.refresh();
    }

    /**
     * Refreshes active alarms table and removes selection
     */
    public void refreshHistoricalAlarmsListAndRemoveSelection() {
        historicalAlarmsTable.getSelectionModel().clearSelection();
        historicalAlarmsTable.refresh();
    }

    /**
     * Adds a monitoring jobs view to Cat view
     * @param aInMonitoringJobsView       Monitoring jobs view to add to the Cat overview
     * @param aInAddressType              Monitored address type (wan, lan)
     * @param aInInterfaceType            Monitored interface type (eth, wifi)
     * @param aInPriority                 Monitored interface priority
     */
    public void addMonitoringJobView(
            GridPane aInMonitoringJobsView, EnumTypes.AddressType aInAddressType, EnumTypes.InterfaceType aInInterfaceType, int aInPriority) {

        // Add the monitoring job view to the relevant grid pane and set title to the tab
        monitoringGridPanes.get(aInAddressType).get(aInPriority - 1).getChildren().add(aInMonitoringJobsView);
        monitoringTabs.get(aInAddressType).get(aInPriority - 1).setText(aInInterfaceType.toString());

    }

    /**
     * Disables a monitoring job view tab
     * @param aInAddressType   Monitored address type (wan, lan)
     * @param aInPriority Monitored interface priority
     */
    public void disableMonitoringJobTab(EnumTypes.AddressType aInAddressType, int aInPriority) {
        monitoringTabs.get(aInAddressType).get(aInPriority - 1).setDisable(true);
    }

    /**
     * Sets icon in monitoring job view tab
     * @param aInAddressType   Monitored address type (wan, lan)
     * @param aInInterfaceType Monitored interface type (eth, wifi)
     * @param aInPriority      Monitored interface priority
     * @param aInState         true if job is ok, false otherwise
     */
    public void setMonitoringJobIcon(EnumTypes.AddressType aInAddressType, EnumTypes.InterfaceType aInInterfaceType, int aInPriority, boolean aInState) {

        ImageView lImageView = new ImageView();
        lImageView.setImage(Graphics.getInterfaceTypeImage(aInInterfaceType, aInState));
        lImageView.setFitHeight(20);
        lImageView.setFitWidth(20);
        monitoringTabs.get(aInAddressType).get(aInPriority - 1).setGraphic(lImageView);

    }

    /**
     * Sets the tooltip on the tab icon
     * @param aInAddressType        Monitored address type (wan, lan)
     * @param aInPriority           Monitored interface priority
     * @param aInMonitoredInterface Monitored interface
     * @param aInActiveServerIp     Active server IP address
     */
    public void setMonitoringJobTooltip(
            EnumTypes.AddressType aInAddressType, int aInPriority, NetworkInterface aInMonitoredInterface, String aInActiveServerIp) {

        if (Preferences.getInstance().getBooleanValue("enableDetailTooltip", Constants.DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE)) {

            try {

                // Compute tooltip with interface displayed name, interface ip address with ip version matching monitored ip version, and interface mac address
                String lNetworkInterfaceIp =
                        ((Network.isIPv4Address(aInActiveServerIp)) ?
                         Network.getNetworkInterfaceInet4Address(aInMonitoredInterface) : Network.getNetworkInterfaceInet6Address(aInMonitoredInterface)).getHostAddress();

                StringBuilder lMacAddress = new StringBuilder();
                for (int i = 0; i < aInMonitoredInterface.getHardwareAddress().length; i++) {
                    lMacAddress.append(String.format("%02X%s", aInMonitoredInterface.getHardwareAddress()[i], (i < aInMonitoredInterface.getHardwareAddress().length - 1) ? "-" : ""));

                }
                monitoringTabs.get(aInAddressType).get(aInPriority - 1).setTooltip(
                        new Tooltip(aInMonitoredInterface.getDisplayName() + " (" + aInMonitoredInterface.getName() + ")" + "\n(" + lNetworkInterfaceIp + " - " + lMacAddress + ")"));

            } catch (Exception e) {
                monitoringTabs.get(aInAddressType).get(aInPriority - 1).setTooltip(
                        new Tooltip(aInMonitoredInterface.getDisplayName() + " (" + aInMonitoredInterface.getName() + ")"));
            }

        }

    }

    /**
     * Changes the modification flag of a monitoring job tab
     * @param aInAddressType        Monitored address type (wan, lan)
     * @param aInPriority           Monitored interface priority
     * @param aInIncrement          Flag increment
     */
    public void changeMonitoringJobTabModificationIndicator(EnumTypes.AddressType aInAddressType, int aInPriority, int aInIncrement) {

        if (aInIncrement < 0) aInIncrement = 0;
        if (!monitoringTabs.get(aInAddressType).get(aInPriority - 1).isSelected()) {
            monitoringTabs.get(aInAddressType).get(aInPriority - 1).setStyle("-fx-font-style: italic");
            Pattern lPattern = Pattern.compile("\\((-*[0-9]+)\\)$");
            Matcher lMatcher = lPattern.matcher(monitoringTabs.get(aInAddressType).get(aInPriority - 1).getText());
            if (!lMatcher.find()) {
                monitoringTabs.get(aInAddressType).get(aInPriority - 1).setText(
                        monitoringTabs.get(aInAddressType).get(aInPriority - 1).getText() + " (" + aInIncrement + ")");
            } else {
                aInIncrement += Integer.valueOf(lMatcher.group(1));
                monitoringTabs.get(aInAddressType).get(aInPriority - 1).setText(
                        monitoringTabs.get(aInAddressType).get(aInPriority - 1).getText().replaceAll(" \\(-*[0-9]+\\)$", "") + " (" + aInIncrement + ")");
            }
        }
    }

    /**
     * Removes unused monitoring job views from Cat view
     * @param aInNumberOfInterfaceTypes Number of used interface types
     */
    public void removeUnusedMonitoringJobView(int aInNumberOfInterfaceTypes) {
        for (EnumTypes.AddressType lAddressType: EnumTypes.AddressType.values()) {
            for (int lIndex = monitoringTabPanes.get(lAddressType).getTabs().size() - 1; lIndex >= aInNumberOfInterfaceTypes; lIndex--) {
                monitoringTabPanes.get(lAddressType).getTabs().remove(lIndex);
            }
        }
    }

    /**
     * Adds a monitoring job
     */
    public void addMonitoringJob() {
        jobsCount++;
        refreshJobsCount();
    }

    /**
     * Pauses or resumes all the jobs
     */
    public void playPause() {

        // Set on mouse click event handler in case it has been removed due to no more jobs
        if (!isButtonPauseActive) {
            isButtonPauseActive = true;
            pauseButtonImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    playPause();
                }
            });
        }

        isButtonPauseDisplayed = !isButtonPauseDisplayed;

        // Compute image url and load it
        Image lNewImage;
        Tooltip lTooltip;
        if (isButtonPauseDisplayed) {
            printConsole(new Message(Display.getViewResourceBundle().getString("catView.console.resume"), EnumTypes.MessageLevel.INFO));
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_PAUSE).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.pause"));
        } else {
            printConsole(new Message(Display.getViewResourceBundle().getString("catView.console.pause"), EnumTypes.MessageLevel.INFO));
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_PLAY).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.play"));
        }
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) Tooltip.install(pauseButtonImageView, lTooltip);
        pauseButtonImageView.setImage(lNewImage);

        // Pause all jobs
        for (MonitoringJob lMonitoringJob : Utilities.safeList(MonitoringJob.getMonitoringJobs())) {
            if (!isButtonPauseDisplayed) {
                lMonitoringJob.pause();
            } else {
                lMonitoringJob.resume();
            }
        }

    }

    /**
     * Remove play/pause button
     */
    public void removePlayPauseButtonImageView() {
        isButtonPauseActive = false;
        pauseButtonImageView.setImage(null);
        pauseButtonImageView.setOnMouseClicked(null);
    }

    /**
     * Set play/pause button to play or pause
     * @param aInPause true if pause must be displayed, false if play must be displayed
     */
    public void setPlayPauseButtonImageView(boolean aInPause) {

        isButtonPauseDisplayed = aInPause;

        // Compute image url and load it
        Image lNewImage;
        Tooltip lTooltip;
        if (aInPause) {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_PAUSE).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.pause"));
        } else {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_PLAY).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.play"));
        }
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) Tooltip.install(pauseButtonImageView, lTooltip);
        pauseButtonImageView.setImage(lNewImage);

    }

    /**
     * Disables email button
     */
    public void disableEmailButtons() {
        Image lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_NOEMAIL).toString());
        Tooltip lTooltip = new Tooltip(Display.getViewResourceBundle().getString("monitoringJob.tooltip.emailDisabled"));
        emailButtonImageView.setImage(lNewImage);
        emailButtonImageView.setOpacity(Constants.DISABLED_IMAGE_TRANSPARENCY);
        emailButtonImageView.setOnMouseClicked(null);
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) Tooltip.install(emailButtonImageView, lTooltip);
        generalEmailButtonImageView.setImage(lNewImage);
        generalEmailButtonImageView.setOpacity(Constants.DISABLED_IMAGE_TRANSPARENCY);
        generalEmailButtonImageView.setOnMouseClicked(null);
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) Tooltip.install(generalEmailButtonImageView, lTooltip);
    }

    /**
     * Switches email of all the jobs
     */
    public void switchEmail() {

        // Set on mouse click event handler in case it has been removed due to no more jobs
        if (!isButtonEmailActive) {
            isButtonEmailActive = true;
            emailButtonImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    switchEmail();
                }
            });
        }

        isButtonEmailEnabled = !isButtonEmailEnabled;

        // Compute image url and load it
        Image lNewImage;
        Tooltip lTooltip;
        if (isButtonEmailEnabled) {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_EMAIL).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.email"));
        } else {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_NOEMAIL).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.noemail"));
        }
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) Tooltip.install(emailButtonImageView, lTooltip);
        emailButtonImageView.setImage(lNewImage);

        // Switch email of all jobs
        for (MonitoringJob lMonitoringJob : Utilities.safeList(MonitoringJob.getMonitoringJobs())) {
            if (!isButtonEmailEnabled) {
                lMonitoringJob.disableEmail();
            } else {
                lMonitoringJob.enableEmail();
            }
        }

        // Switch general email
        if (isButtonEmailEnabled != isButtonGeneralEmailEnabled) switchGeneralEmail();

    }

    /**
     * Removes email button
     */
    public void removeEmailButtonImageView() {
        isButtonEmailActive = false;
        emailButtonImageView.setImage(null);
        emailButtonImageView.setOnMouseClicked(null);
    }

    /**
     * Sets email button to audibleEnabled or disabled
     * @param aInEmail true if email must be audibleEnabled, false if email must be disabled
     */
    public void setEmailButtonImageView(boolean aInEmail) {

        isButtonEmailEnabled = aInEmail;

        // Compute image url and load it
        Image lNewImage;
        Tooltip lTooltip;
        if (aInEmail) {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_EMAIL).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.email"));
        } else {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_NOEMAIL).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.noemail"));
        }
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) Tooltip.install(emailButtonImageView, lTooltip);
        emailButtonImageView.setImage(lNewImage);

    }

    public String BuildStatePropertyName(String aInState) {
        return new StringBuilder("General").append('.').append(aInState).toString();
    }

    /**
     * Switches general email
     */
    public void switchGeneralEmail() {

        isButtonGeneralEmailEnabled = !isButtonGeneralEmailEnabled;

        // Compute image url and load it
        Image lNewImage;
        Tooltip lTooltip;
        if (isButtonGeneralEmailEnabled) {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_EMAIL).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.generalEmail"));
        } else {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_NOEMAIL).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.nogeneralEmail"));
        }
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) Tooltip.install(generalEmailButtonImageView, lTooltip);
        generalEmailButtonImageView.setImage(lNewImage);

        States.getInstance().saveValue(BuildStatePropertyName(Constants.SEND_MAIL_STATE), isButtonGeneralEmailEnabled);

        Cat.getInstance().checkEmailState();
    }

    /**
     * Sets general email button to audibleEnabled or disabled
     * @param aInEmail true if email must be audibleEnabled, false if email must be disabled
     */
    public void setGeneralEmailButtonImageView(boolean aInEmail) {

        isButtonGeneralEmailEnabled = aInEmail;

        // Compute image url and load it
        Image lNewImage;
        Tooltip lTooltip;
        if (aInEmail) {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_EMAIL).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.generalEmail"));
        } else {
            lNewImage = new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_NOEMAIL).toString());
            lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.tooltip.nogeneralEmail"));
        }
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) Tooltip.install(generalEmailButtonImageView, lTooltip);
        generalEmailButtonImageView.setImage(lNewImage);

    }

    /**
     * Resets statistics for all jobs
     */
    public void resetStatistics() {

        // Prepare confirmation dialog box
        Alert lConfirmation = new Alert(Alert.AlertType.CONFIRMATION, Display.getViewResourceBundle().getString("confirm.resetStatistics.question"), ButtonType.YES, ButtonType.NO);
        lConfirmation.setHeaderText(Display.getViewResourceBundle().getString("confirm.resetStatistics.title"));
        lConfirmation.initModality(Modality.APPLICATION_MODAL);

        // Display confirmation dialog box
        Optional<ButtonType> lResponse = lConfirmation.showAndWait();

        // OK is pressed
        if (lResponse.isPresent() && lResponse.get().equals(ButtonType.YES)) {

            firstDisplay.put(consoleTextFlow, true);
            firstDisplay.put(speedTestTextFlow, true);

            clearAllMessages();
            consoleTab.setText(consoleTab.getText().replaceAll(" \\(-*[0-9]+\\)$", ""));
            printConsole(new Message(Display.getViewResourceBundle().getString("catView.console.resetStatistics"), EnumTypes.MessageLevel.INFO));

            // Reset current statistics
            pingsCount = 0;
            lostPingsCount = 0;
            lostConnectionsCount = 0;
            ongoingLostConnectionsCount = 0;
            lostTime = "-";
            lostDetails = "-";
            refreshPingsCount();
            refreshConnectionsLostCount();

            // Reset chart data
            for (int lKey : pingLines.keySet()) {

                // Clear vertical markers
                for (XYChart.Data lMarker : pingMarkers.get(lKey)) {
                    pingLineChart.removeVerticalValueMarker(lMarker);
                    lMarker.setNode(null);
                }
                pingMarkers.get(lKey).clear();

                // Clear points on chart
                pingLines.get(lKey).getSeries().getData().clear();
                pingLines.get(lKey).setMinX(Long.MAX_VALUE);
                pingLines.get(lKey).setMaxX(Long.MIN_VALUE);
                pingLines.get(lKey).setMinY(Long.MAX_VALUE);
                pingLines.get(lKey).setMaxY(Long.MIN_VALUE);

                // Clear stored points
                pingPoints.get(lKey).clear();

            }
            refreshPingAxisBounds();

            // Reset statistics for all jobs
            for (MonitoringJob lMonitoringJob : MonitoringJob.getMonitoringJobs()) {
                lMonitoringJob.resetStatistics();
            }

        }

    }

    /**
     * Displays active alarms list when active alarms button is pressed
     */
    public void selectActiveAlarms() {

        activeAlarmsButton.setSelected(true);
        historicalAlarmsButton.setSelected(false);
        activateAlarmsTable.setVisible(true);
        historicalAlarmsTable.setVisible(false);
        activeAlarmsCountLabel.setVisible(true);
        historicalAlarmsCountLabel.setVisible(false);
        activeAlarmsFilterTextField.setVisible(true);
        historicalAlarmsFilterTextField.setVisible(false);
        activateAlarmsTable.refresh();

    }

    /**
     * Displays historical alarms list when historical alarms button is pressed
     */
    public void selectHistoricalAlarms() {

        historicalAlarmsButton.setSelected(true);
        activeAlarmsButton.setSelected(false);
        historicalAlarmsTable.setVisible(true);
        activateAlarmsTable.setVisible(false);
        historicalAlarmsCountLabel.setVisible(true);
        activeAlarmsCountLabel.setVisible(false);
        historicalAlarmsFilterTextField.setVisible(true);
        activeAlarmsFilterTextField.setVisible(false);
        historicalAlarmsTable.refresh();

    }

    /**
     * Sets address type state image view
     * @param aInAddressType address type
     * @param aInStateOk     true if state is ok, false if state is nok
     */
    public void setAddressTypeStateImageView(EnumTypes.AddressType aInAddressType, boolean aInStateOk) {

        // Compute image url and load it
        Image lNewImage;
        Tooltip lTooltip;

        if (aInAddressType != null) {

            lTooltip = (aInStateOk) ?
                       new Tooltip(String.format(Display.getViewResourceBundle().getString("monitoringJob.tooltip.addressTypeStateOk"), aInAddressType)) :
                       new Tooltip(String.format(Display.getViewResourceBundle().getString("monitoringJob.tooltip.addressTypeStateNok"), aInAddressType));

            switch (aInAddressType) {
                case WAN:
                    lNewImage = (aInStateOk) ?
                                new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_WAN_OK).toString()) :
                                new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_WAN_NOK).toString());
                    if (Preferences.getInstance().getBooleanValue("enableDetailTooltip", Constants.DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE)) Tooltip.install(wanStateImageView, lTooltip);
                    wanStateImageView.setImage(lNewImage);
                    break;
                case LAN:
                    lNewImage = (aInStateOk) ?
                                new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_LAN_OK).toString()) :
                                new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_LAN_NOK).toString());
                    if (Preferences.getInstance().getBooleanValue("enableDetailTooltip", Constants.DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE)) Tooltip.install(lanStateImageView, lTooltip);
                    lanStateImageView.setImage(lNewImage);
                    break;
                default:
                    break;
            }
        }

    }

    /**
     * Sets interface type state image view
     * @param aInInterfaceType interface type
     * @param aInStateOk       true if state is ok, false if state is nok
     */
    public void setInterfaceTypeImageView(EnumTypes.InterfaceType aInInterfaceType, boolean aInStateOk) {

        // Compute image url and load it
        Image lNewImage;
        Tooltip lTooltip;

        if (aInInterfaceType != null) {

            lTooltip = (aInStateOk) ?
                       new Tooltip(String.format(Display.getViewResourceBundle().getString("monitoringJob.tooltip.interfaceTypeStateOk"), aInInterfaceType)) :
                       new Tooltip(String.format(Display.getViewResourceBundle().getString("monitoringJob.tooltip.interfaceTypeStateNok"), aInInterfaceType));

            switch (aInInterfaceType) {
                case ETH:
                    lNewImage = (aInStateOk) ?
                                new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_ETH_OK).toString()) :
                                new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_ETH_NOK).toString());
                    if (Preferences.getInstance().getBooleanValue("enableDetailTooltip", Constants.DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE)) Tooltip.install(networkInterface1ImageView, lTooltip);
                    networkInterface1ImageView.setImage(lNewImage);
                    break;
                case WIFI:
                    lNewImage = (aInStateOk) ?
                                new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_WIFI_OK).toString()) :
                                new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_WIFI_NOK).toString());
                    if (Preferences.getInstance().getBooleanValue("enableDetailTooltip", Constants.DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE)) Tooltip.install(networkInterface2ImageView, lTooltip);
                    networkInterface2ImageView.setImage(lNewImage);
                    break;
                default:
                    break;
            }
        }

    }

    /**
     * Sets global state image view
     * @param aInStateOk true if state is ok, false if state is nok
     */
    public void setGlobalStateImageView(boolean aInStateOk) {

        // Compute image url and load it
        Image lNewImage = (aInStateOk) ?
                    new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_OK).toString()) :
                    new Image(getClass().getClassLoader().getResource("resources/images/" + Constants.IMAGE_NOK).toString());
        Tooltip lTooltip = (aInStateOk) ?
                   new Tooltip(Display.getViewResourceBundle().getString("monitoringJob.tooltip.globalStatusOk")) :
                   new Tooltip(Display.getViewResourceBundle().getString("monitoringJob.tooltip.globalStatusNok"));
        if (Preferences.getInstance().getBooleanValue("enableDetailTooltip", Constants.DEFAULT_ENABLE_DETAIL_TOOLTIP_PREFERENCE)) Tooltip.install(globalStateImageView, lTooltip);
        globalStateImageView.setImage(lNewImage);

    }

    /**
     * Configures check boxes allowing to filter ping lines on interface (filter on address type is constant)
     * Check boxes are ordered by interface priority and must contain the type and the name of the interface
     * @param aInInterfaceName N
     * @param aInPriority
     */
    public void configurePingLineFilterCheckBox(String aInInterfaceName, int aInPriority) {
        pingLineFilterCheckBoxes.get(aInPriority - 1).setVisible(true);
        pingLineFilterCheckBoxes.get(aInPriority - 1).setText(Network.getInterfaceType(aInInterfaceName).toString() + " (" + aInInterfaceName + ")");
        Tooltip lTooltip = new Tooltip(String.format(Display.getViewResourceBundle().getString(
                "catView.pingChartView.tooltip." +  EnumTypes.InterfaceType.valueOf(Network.getInterfaceType(aInInterfaceName))), aInInterfaceName));
        if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE)) pingLineFilterCheckBoxes.get(aInPriority - 1).setTooltip(lTooltip);
        networkInterfacesNames.add(aInPriority - 1, aInInterfaceName);
    }

    // PINGS CHARTS

    /**
     * Creates a new line in the ping line chart
     * @param aInAddressType   Address type of the server the new line applies to
     * @param aInInterface     Interface of the server the new line applies to
     */
    public void addPingSeries(EnumTypes.AddressType aInAddressType, NetworkInterface aInInterface) {
        int lKey = Objects.hash(aInAddressType, aInInterface);
        pingLines.put(lKey, new PingLine(++pingLinesCount, aInAddressType, aInInterface));
        checkPingChartState();
    }

    /**
     * Creates a new bar in the speed test bar chart
     * @param aInMode Name of the new bar
     */
    public void addSpeedTestSeries(EnumTypes.SpeedTestMode aInMode) {
        int lKey = Objects.hash(aInMode);
        speedTestBars.put(lKey, new SpeedTestBar(++speedTestBarsCount, aInMode));
        checkSpeedTestChartState();
    }

    /**
     * Checks if a ping line can be displayed depending on the states of the filter check boxes
     * @param aInPingLine Ping line to check
     * @return true if the ping line can be displayed, false otherwise
     */
    private boolean isPingLineDisplayedAllowed(PingLine aInPingLine) {

        // Retrieve filters state
        int lInterfaceNameIndex = 0;
        while (lInterfaceNameIndex < networkInterfacesNames.size()) {
            if (networkInterfacesNames.get(lInterfaceNameIndex).equals(aInPingLine.getInterfaceName())) break;
            lInterfaceNameIndex++;
        }
        final boolean lAddressTypeEnabled =
                (aInPingLine.getAddressType().equals(EnumTypes.AddressType.WAN) ? pingLineWanFilterCheckBox.isSelected() : pingLineLanFilterCheckBox.isSelected());
        final boolean lInterfaceEnabled = pingLineFilterCheckBoxes.get(lInterfaceNameIndex).isSelected();

        return lInterfaceEnabled && lAddressTypeEnabled;

    }

    /**
     * Checks if a speed test bar can be displayed depending on the states of the filter check boxes
     * @param aInSpeedTestBar Spped test bar to check
     * @return true if the speed test bar can be displayed, false otherwise
     */
    private boolean isSpeedTestBarDisplayedAllowed(SpeedTestBar aInSpeedTestBar) {

        final boolean lNameEnabled =
                (aInSpeedTestBar.getMode().equals(EnumTypes.SpeedTestMode.DOWNLOAD) ? speedTestDownloadFilterCheckBox.isSelected() : speedTestUploadFilterCheckBox.isSelected());

        return lNameEnabled;

    }

    /**
     * Refreshes a ping series with the current data depending on the move and zoom sliders positions
     * @param aInKey   Key of the pingLine to which the series needs to be refreshed
     */
    private void refreshPingSeries(int aInKey) {

        PingLine lPingLine = pingLines.get(aInKey);

        if (!pingPoints.containsKey(aInKey)) return;
        List<PingPoint> lPingPoints = pingPoints.get(aInKey);
        if (pingPoints.size() == 0) return;

        XYChart.Data lFirstPoint = lPingPoints.get(0).getPoint();
        XYChart.Data lLastPoint = lPingPoints.get(lPingPoints.size() - 1).getPoint();

        // Retrieve min and max time to be displayed if last measurements need to be displayed
        if (pingLineXMoveRatio == 0) {
            pingLineMaxTime = Utilities.convertXY(lLastPoint.getXValue()) -
                              Math.round((Utilities.convertXY(lLastPoint.getXValue()) - Utilities.convertXY(lFirstPoint.getXValue())) * pingLineXMoveRatio);
            pingLineMinTime = Math.max(0, pingLineMaxTime - pingLineDuration);
        }

        // Build the series
        for (XYChart.Data lMarker: pingMarkers.get(aInKey)) {
            pingLineChart.removeVerticalValueMarker(lMarker);
            lMarker.setNode(null);
        }
        pingMarkers.get(aInKey).clear();
        lPingLine.getSeries().getData().clear();

        if (isPingLineDisplayedAllowed(lPingLine) && pingLineManageCheckBox.isSelected()) {

            for (int lIndex = 0; lIndex < lPingPoints.size(); lIndex++) {

                PingPoint lPingPoint = lPingPoints.get(lIndex);

                // Compute style
                String lSymbolStyle = "chart-line-symbol-" + lPingLine.getId() + "-" + EnumTypes.ServerType.valueOf(lPingPoint.getServerType());

                if (lPingPoint.getPoint().getNode() != null) lPingPoint.getPoint().getNode().getStyleClass().clear();
                if ((Utilities.convertXY(lPingPoint.getPoint().getXValue()) >= pingLineMinTime) && (Utilities.convertXY(lPingPoint.getPoint().getXValue()) <= pingLineMaxTime)) {
                    if (lPingPoint.isReachable()) {
                        lPingLine.getSeries().getData().add(lPingPoint.getPoint());
                        lPingPoint.getPoint().getNode().getStyleClass().add(lSymbolStyle);
                    } else {
                        pingMarkers.get(aInKey).add(pingLineChart.addVerticalValueMarker(
                                lPingPoint.getPoint(), "line-" + lPingLine.getId() + "-" + EnumTypes.ServerType.valueOf(lPingPoint.getServerType())));
                    }
                } else {
                    lPingPoint.getPoint().setNode(null);
                }

            }

            if (lPingLine.getSeries().getData().size() - 1 > 0) {

                lLastPoint = lPingLine.getSeries().getData().get(lPingLine.getSeries().getData().size() - 1);
                String lLegendStyle = lLastPoint.getNode().getStyleClass().get(lLastPoint.getNode().getStyleClass().size() - 1);

                // Style the legend element corresponding to the current series
                for (Node lNode : pingLineChart.getChildrenUnmodifiable()) {
                    if (lNode instanceof Legend) {
                        Legend.LegendItem lLegendItem = ((Legend) lNode).getItems().get(lPingLine.getId() - 1);
                        lLegendItem.getSymbol().getStyleClass().remove(lLegendItem.getSymbol().getStyleClass().size() - 1);
                        lLegendItem.getSymbol().getStyleClass().add(lLegendStyle);
                    }
                }

                // Style the line - Note, should be done only once after series is added to the chart, but does not work
                lPingLine.getSeries().getNode().getStyleClass().remove(lPingLine.getSeries().getNode().getStyleClass().size() - 1);
                lPingLine.getSeries().getNode().getStyleClass().add("chart-" + lPingLine.getId());

            }

            // Recompute bounds for current series
            lPingLine.setMinX(Long.MAX_VALUE);
            lPingLine.setMaxX(Long.MIN_VALUE);
            lPingLine.setMinY(Long.MAX_VALUE);
            lPingLine.setMaxY(Long.MIN_VALUE);
            for (XYChart.Data lPoint : lPingLine.getSeries().getData()) {
                if ((Long) lPoint.getXValue() < lPingLine.getMinX().longValue()) lPingLine.setMinX((Long) lPoint.getXValue());
                if ((Long) lPoint.getXValue() > lPingLine.getMaxX().longValue()) lPingLine.setMaxX((Long) lPoint.getXValue());
                if ((Long) lPoint.getYValue() < lPingLine.getMinY().longValue()) lPingLine.setMinY((Long) lPoint.getYValue());
                if ((Long) lPoint.getYValue() > lPingLine.getMaxY().longValue()) lPingLine.setMaxY((Long) lPoint.getYValue());
            }
            for (XYChart.Data lPoint : pingMarkers.get(aInKey)) {
                if ((Long) lPoint.getXValue() < lPingLine.getMinX().longValue()) lPingLine.setMinX((Long) lPoint.getXValue());
                if ((Long) lPoint.getXValue() > lPingLine.getMaxX().longValue()) lPingLine.setMaxX((Long) lPoint.getXValue());
            }



            }

        refreshPingAxisBounds();

    }

    /**
     * Refreshes a speed test series with the current data depending on the move and zoom sliders positions
     * @param aInKey   Key of the speedTestBar to which the series needs to be refreshed
     */
    private void refreshSpeedTestSeries(int aInKey) {

        SpeedTestBar lSpeedTestBar = speedTestBars.get(aInKey);

        if (!speedTestPoints.containsKey(aInKey)) return;
        List<SpeedTestPoint> lSpeedTestPoints = speedTestPoints.get(aInKey);
        if (speedTestPoints.size() == 0) return;

        SpeedTestPoint lFirstPoint = lSpeedTestPoints.get(0);
        SpeedTestPoint lLastPoint = lSpeedTestPoints.get(lSpeedTestPoints.size() - 1);

        // Retrieve min and max time to be displayed if last measurements need to be displayed
        if (speedTestBarsXMoveRatio == 0) {
            speedTestBarsMaxTime = lLastPoint.getX() - Math.round((lLastPoint.getX() - lFirstPoint.getX()) * speedTestBarsXMoveRatio);
            speedTestBarsMinTime = Math.max(0, speedTestBarsMaxTime - speedTestBarsDuration);
        }

        // Build the series
        Set<String> lCategories = new LinkedHashSet<>();
        lSpeedTestBar.getSeries().getData().clear();
        if (isSpeedTestBarDisplayedAllowed(lSpeedTestBar) && speedTestManageCheckBox.isSelected()) {

            // Compute scale
            Long lRatio = Preferences.getInstance().getLongValue(Constants.SPEED_TEST_DISPLAY_UNIT_PREFERENCE, Constants.DEFAULT_SPEED_TEST_DISPLAY_UNIT);

            long lMaxY = 0;
            for (int lIndex = 0; lIndex < lSpeedTestPoints.size(); lIndex++) {

                SpeedTestPoint lSpeedTestPoint = lSpeedTestPoints.get(lIndex);

                // Compute style
                String lSymbolStyle = "chart-bar-" + EnumTypes.SpeedTestMode.valueOf(lSpeedTestBar.getMode()) + "-" + EnumTypes.SpeedTestType.valueOf(lSpeedTestPoint.getType());

                if (lSpeedTestPoint.getPoint().getNode() != null) lSpeedTestPoint.getPoint().getNode().getStyleClass().clear();
                if ((lSpeedTestPoint.getX() >= speedTestBarsMinTime) && (lSpeedTestPoint.getX() <= speedTestBarsMaxTime)) {
                    lSpeedTestPoint.getPoint().setYValue(lSpeedTestPoint.getY() / lRatio);
                    lSpeedTestBar.getSeries().getData().add(lSpeedTestPoint.getPoint());
                    lCategories.add(lSpeedTestPoint.getCategory());
                    lSpeedTestPoint.getPoint().getNode().getStyleClass().add(lSymbolStyle);
                    if (lSpeedTestPoint.getY() > lMaxY) lMaxY = lSpeedTestPoint.getY();
                } else {
                    lSpeedTestPoint.getPoint().setNode(null);
                }

            }
            speedTestBarChartYAxis.setTickUnit(lMaxY / 5);
            // Order categories
            final ObservableList<String> c = FXCollections.observableArrayList(lCategories);
            Collections.sort(c);
            speedTestBarChartXAxis.setCategories(c);

            if (lSpeedTestBar.getSeries().getData().size() -1 > 0) {

                speedTestBarChart.setLegendVisible(true);
                XYChart.Data lLastChartPoint = lSpeedTestBar.getSeries().getData().get(lSpeedTestBar.getSeries().getData().size() - 1);
                String lLegendStyle = lLastChartPoint.getNode().getStyleClass().get(lLastChartPoint.getNode().getStyleClass().size() - 1);

                // Style the legend element corresponding to the current series
                for (Node lNode : speedTestBarChart.getChildrenUnmodifiable()) {
                    if (lNode instanceof Legend) {
                        Legend.LegendItem lLegendItem = ((Legend) lNode).getItems().get(lSpeedTestBar.getId() - 1);
                        lLegendItem.getSymbol().getStyleClass().remove(lLegendItem.getSymbol().getStyleClass().size() - 1);
                        lLegendItem.getSymbol().getStyleClass().add(lLegendStyle);
                    }
                }

            }

            // Recompute bounds for current series
            lSpeedTestBar.setMinY(Long.MAX_VALUE);
            lSpeedTestBar.setMaxY(Long.MIN_VALUE);
            for (XYChart.Data lPoint : lSpeedTestBar.getSeries().getData()) {
                if ((Long) lPoint.getYValue() < lSpeedTestBar.getMinY().longValue()) lSpeedTestBar.setMinY((Long) lPoint.getYValue());
                if ((Long) lPoint.getYValue() > lSpeedTestBar.getMaxY().longValue()) lSpeedTestBar.setMaxY((Long) lPoint.getYValue());
            }

        }

        refreshSpeedTestAxisBounds();

    }

    /**
     * Refreshes axis when zooming on ping Y axis
     */
    private void zoomPingYAxis() {
        if (pingLineManageCheckBox.isSelected()) {
            pingLineYZoomRatio = pingLineChartVerticalZoomSlider.getValue() / 100;
            refreshPingAxisBounds();
        }
    }

    /**
     * Refreshes axis when zooming on speed test Y axis
     */
    private void zoomSpeedTestYAxis() {
        if (speedTestManageCheckBox.isSelected()) {
            speedTestBarsYZoomRatio = speedTestBarChartVerticalZoomSlider.getValue() / 100;
            refreshSpeedTestAxisBounds();
        }
    }

    /**
     * Enables or disables controls related to ping chart depending on Manage ping chart check box
     */
    public void checkPingChartState() {

        pingLineWanFilterCheckBox.setDisable(!pingLineManageCheckBox.isSelected());
        pingLineLanFilterCheckBox.setDisable(!pingLineManageCheckBox.isSelected());
        pingLineInterface1FilterCheckBox.setDisable(!pingLineManageCheckBox.isSelected());
        pingLineInterface2FilterCheckBox.setDisable(!pingLineManageCheckBox.isSelected());
        pingLineChartHorizontalMoveSlider.setDisable(!pingLineManageCheckBox.isSelected());
        pingLineChartHorizontalZoomSlider.setDisable(!pingLineManageCheckBox.isSelected());
        pingLineChartVerticalZoomSlider.setDisable(!pingLineManageCheckBox.isSelected());
        pingLineChart.setDisable(!pingLineManageCheckBox.isSelected());

        // Hide or display legend symbol depending on filters
        for (Node lNode : pingLineChart.getChildrenUnmodifiable()) {
            if (lNode instanceof Legend) {
                for (Legend.LegendItem lLegendItem : ((Legend) lNode).getItems()) {
                    for (PingLine lPingLine: pingLines.values()) {
                        if (lPingLine.getSeries().getName().equals(lLegendItem.getText())) {
                            lLegendItem.getSymbol().setVisible(isPingLineDisplayedAllowed(lPingLine));
                        }
                    }
                }
            }
        }

    }

    /**
     * Enables or disables controls related to speed test chart depending on Manage speed test chart check box
     */
    public void checkSpeedTestChartState() {

        speedTestDownloadFilterCheckBox.setDisable(!speedTestManageCheckBox.isSelected());
        speedTestUploadFilterCheckBox.setDisable(!speedTestManageCheckBox.isSelected());
        speedTestBarChartHorizontalMoveSlider.setDisable(!speedTestManageCheckBox.isSelected());
        speedTestBarChartHorizontalZoomSlider.setDisable(!speedTestManageCheckBox.isSelected());
        speedTestBarChartVerticalZoomSlider.setDisable(!speedTestManageCheckBox.isSelected());
        speedTestBarChart.setDisable(!speedTestManageCheckBox.isSelected());

        for (Node lNode : speedTestBarChart.getChildrenUnmodifiable()) {
            if (lNode instanceof Legend) {
                for (Legend.LegendItem lLegendItem : ((Legend) lNode).getItems()) {
                    for (SpeedTestBar lSpeedTestBar: speedTestBars.values()) {
                        if (lSpeedTestBar.getSeries().getName().equals(lLegendItem.getText())) {
                            lLegendItem.getSymbol().setVisible(isSpeedTestBarDisplayedAllowed(lSpeedTestBar));
                        }
                    }
                }
            }
        }

    }

    /**
     * Refreshes all ping series with the current data depending on the move and zoom sliders positions
     */
    public void refreshAllPingSeries() {

        // Save sliders and check boxes states
        States.getInstance().saveValue(Constants.PING_CHART_ENABLE_STATE, pingLineManageCheckBox.isSelected());
        States.getInstance().saveValue(Constants.PING_CHART_DISPLAY_WAN_LINE_STATE, pingLineWanFilterCheckBox.isSelected());
        States.getInstance().saveValue(Constants.PING_CHART_DISPLAY_LAN_LINE_STATE, pingLineLanFilterCheckBox.isSelected());
        States.getInstance().saveValue(Constants.PING_CHART_DISPLAY_INTERFACE1_LINE_STATE, pingLineInterface1FilterCheckBox.isSelected());
        States.getInstance().saveValue(Constants.PING_CHART_DISPLAY_INTERFACE2_LINE_STATE, pingLineInterface2FilterCheckBox.isSelected());
        States.getInstance().saveValue(Constants.PING_CHART_HORIZONTAL_ZOOM_SLIDER_STATE, pingLineChartHorizontalZoomSlider.getValue());
        States.getInstance().saveValue(Constants.PING_CHART_VERTICAL_ZOOM_SLIDER_STATE, pingLineChartVerticalZoomSlider.getValue());

        checkPingChartState();

        // Recompute ratios depending on sliders position
        pingLineXMoveRatio = (100 - pingLineChartHorizontalMoveSlider.getValue()) / 100;
        pingLineXZoomRatio = pingLineChartHorizontalZoomSlider.getValue() / 100;

        // Recompute duration to be displayed
        pingLineDuration = Math.max(MIN_DISPLAYED_PING_DURATION, Math.round(MAX_DISPLAYED_PING_DURATION * pingLineXZoomRatio));

        // Recompute min and max time to be displayed
        long lMinX = Long.MAX_VALUE;
        long lMaxX = Long.MIN_VALUE;
        for (int lKey : pingLines.keySet()) {
            if (pingPoints.containsKey(lKey) && pingPoints.get(lKey).size() != 0) {
                XYChart.Data lFirstPoint = pingPoints.get(lKey).get(0).getPoint();
                XYChart.Data lLastPoint = pingPoints.get(lKey).get(pingPoints.get(lKey).size() - 1).getPoint();
                if (Utilities.convertXY(lFirstPoint.getXValue()) < lMinX) lMinX = Utilities.convertXY(lFirstPoint.getXValue());
                if (Utilities.convertXY(lLastPoint.getXValue()) > lMaxX) lMaxX = Utilities.convertXY(lLastPoint.getXValue());
            }
        }
        pingLineMaxTime = lMaxX - Math.round((lMaxX - lMinX) * pingLineXMoveRatio);
        pingLineMinTime = Math.max(0, pingLineMaxTime - pingLineDuration);

        for (int lKey : pingLines.keySet()) {
            refreshPingSeries(lKey);
        }
        refreshPingAxisBounds();

    }

    /**
     * Refreshes all speed test series with the current data depending on the move and zoom sliders positions
     */
    public void refreshAllSpeedTestSeries() {

        // Save sliders and check boxes states
        States.getInstance().saveValue(Constants.SPEED_TEST_CHART_ENABLE_STATE, speedTestManageCheckBox.isSelected());
        States.getInstance().saveValue(Constants.SPEED_TEST_CHART_DISPLAY_DOWNLOAD_STATE, speedTestDownloadFilterCheckBox.isSelected());
        States.getInstance().saveValue(Constants.SPEED_TEST_CHART_DISPLAY_UPLOAD_STATE, speedTestUploadFilterCheckBox.isSelected());
        States.getInstance().saveValue(Constants.SPEED_TEST_CHART_HORIZONTAL_ZOOM_SLIDER_STATE, speedTestBarChartHorizontalZoomSlider.getValue());
        States.getInstance().saveValue(Constants.SPEED_TEST_CHART_VERTICAL_ZOOM_SLIDER_STATE, speedTestBarChartVerticalZoomSlider.getValue());

        checkSpeedTestChartState();

        // Recompute ratios depending on sliders position
        speedTestBarsXMoveRatio = (100 - speedTestBarChartHorizontalMoveSlider.getValue()) / 100;
        speedTestBarsXZoomRatio = speedTestBarChartHorizontalZoomSlider.getValue() / 100;

        // Recompute min and max time to be displayed
        long lMinX = Long.MAX_VALUE;
        long lMaxX = Long.MIN_VALUE;
        for (int lKey : speedTestBars.keySet()) {
            if (speedTestPoints.containsKey(lKey) && speedTestPoints.get(lKey).size() != 0) {
                SpeedTestPoint lFirstPoint = speedTestPoints.get(lKey).get(0);
                SpeedTestPoint lLastPoint = speedTestPoints.get(lKey).get(speedTestPoints.get(lKey).size() - 1);
                if (lFirstPoint.getX() < lMinX) lMinX = lFirstPoint.getX();
                if (lLastPoint.getX() > lMaxX) lMaxX = lLastPoint.getX();
            }
        }

        // Recompute duration to be displayed
        speedTestBarsDuration = Math.max(MIN_DISPLAYED_SPEED_TEST_DURATION, Math.round((lMaxX - lMinX) * speedTestBarsXZoomRatio));

        speedTestBarsMaxTime = lMaxX - Math.round((lMaxX - lMinX) * speedTestBarsXMoveRatio);
        speedTestBarsMinTime = Math.max(0, speedTestBarsMaxTime - speedTestBarsDuration);

        for (int lKey : speedTestBars.keySet()) {
            refreshSpeedTestSeries(lKey);
        }
        refreshSpeedTestAxisBounds();

    }

    /**
     * Refresh min and max values of ping chart axis
     */
    private void refreshPingAxisBounds() {

        // Retrieve duration to be displayed
        long lDuration = Math.max(MIN_DISPLAYED_PING_DURATION, Math.round(MAX_DISPLAYED_PING_DURATION * (pingLineChartHorizontalZoomSlider.getValue() / 100)));

        // Recompute global bounds
        long lMaxMinX = Long.MIN_VALUE;
        long lMinX = Long.MAX_VALUE;
        long lMaxX = Long.MIN_VALUE;
        long lMinY = Long.MAX_VALUE;
        long lMaxY = Long.MIN_VALUE;
        for (PingLine lPingLine : pingLines.values()) {
            if (isPingLineDisplayedAllowed(lPingLine) && pingLineManageCheckBox.isSelected()) {
                if (lPingLine.getMinX().longValue() < lMinX) lMinX = lPingLine.getMinX().longValue();
                if ((lMaxMinX < lPingLine.getMinX().longValue()) && (lPingLine.getMinX().longValue() != Long.MAX_VALUE)) lMaxMinX = lPingLine.getMinX().longValue();
                lMaxX = lMaxMinX + lDuration;
                if (lPingLine.getMinY().longValue() < lMinY) lMinY = lPingLine.getMinY().longValue();
                if (lPingLine.getMaxY().longValue() > lMaxY) lMaxY = lPingLine.getMaxY().longValue();
            }
        }

        if (lMaxMinX == Long.MIN_VALUE) lMaxMinX = 0;
        if (lMinX == Long.MAX_VALUE) lMinX = 0;
        if (lMaxX == Long.MIN_VALUE) lMaxX = 0;
        if (lMinY == Long.MAX_VALUE) lMinY = 0;
        if (lMaxY == Long.MIN_VALUE) lMaxY = 0;

        // Resize axis
        pingLineChartXAxis.setLowerBound(Math.floor(lMinX / 1000f) * 1000);
        pingLineChartXAxis.setUpperBound(Math.ceil(lMaxX / 1000f) * 1000);
        pingLineChartYAxis.setLowerBound(Math.floor(lMinY / 10f) * 10);
        pingLineChartYAxis.setUpperBound(Math.max(lMinY, Math.ceil(lMaxY * pingLineYZoomRatio / 10f) * 10));
        pingLineChartXAxis.setTickUnit(5000 / Math.pow(10, 5 - String.valueOf(Math.round(lDuration)).length()));
        if (Math.floor(lMaxY / Math.pow(10, String.valueOf(Math.round(lMaxY)).length() - 1)) >= 2)
            pingLineChartYAxis.setTickUnit(Math.pow(10, String.valueOf(Math.round(lMaxY)).length() - 1));
        else
            pingLineChartYAxis.setTickUnit(Math.pow(10, String.valueOf(Math.round(lMaxY)).length() - 2));

    }

    /**
     * Refresh min and max values of speed test chart axis
     */
    private void refreshSpeedTestAxisBounds() {

        // Recompute global bounds
        long lMaxY = Long.MIN_VALUE;
        for (SpeedTestBar lSpeedTestBar : speedTestBars.values()) {
            if (isSpeedTestBarDisplayedAllowed(lSpeedTestBar) && speedTestManageCheckBox.isSelected()) {
                if (lSpeedTestBar.getMaxY().longValue() > lMaxY) lMaxY = lSpeedTestBar.getMaxY().longValue();
            }
        }

        if (lMaxY == Long.MIN_VALUE) lMaxY = 0;

        // Resize Y axis
        speedTestBarChartYAxis.setLowerBound(0);
        speedTestBarChartYAxis.setUpperBound(lMaxY * speedTestBarsYZoomRatio);

    }

    /**
     * Add a new point in a ping line series
     * @param aInServerType    Type of server the new data applies to
     * @param aInAddressType   Address type of the server the new data applies to
     * @param aInInterface     Interface of the server the new data applies to
     * @param aInEpoch         Current time in epoch ms
     * @param aInX             X coordinate of the new data
     * @param aInY             Y coordinate of the new data
     * @param aInReachable     false if ping is lost, true otherwise
     */
    public void addPingSeriesData(EnumTypes.ServerType aInServerType, EnumTypes.AddressType aInAddressType, NetworkInterface aInInterface,
                                  long aInEpoch, long aInX, long aInY, boolean aInReachable) {

        // Initialize time reference
        if (timeReference == 0) timeReference = aInEpoch;

        if (pingLineManageCheckBox.isSelected()) {

            int lKey = Objects.hash(aInAddressType, aInInterface);

            // Initialize set of markers
            if (pingMarkers.get(lKey) == null) {
                List<XYChart.Data> lMarkers = new ArrayList<>();
                pingMarkers.put(lKey, lMarkers);
            }

            // Initialize set of points
            if (pingPoints.get(lKey) == null) {
                List<PingPoint> lPoints = new ArrayList<>();
                pingPoints.put(lKey, lPoints);
            }

            // Add the new point to set of points
            pingPoints.get(lKey).add(new PingPoint(aInX, aInY, aInReachable, aInServerType));

            // Remove oldest points from the set of points and the series
            for (int lIndex = 0; lIndex < pingPoints.get(lKey).size(); lIndex++) {
                PingPoint lPingPoint = pingPoints.get(lKey).get(lIndex);
                XYChart.Data lPoint = lPingPoint.getPoint();
                if (aInX - Utilities.convertXY(lPoint.getXValue()) > MAX_STORED_PING_DURATION) {
                    pingPoints.get(lKey).remove(lPingPoint);
                } else break;
            }
            for (int lIndex = 0; lIndex < pingMarkers.get(lKey).size(); lIndex++) {
                XYChart.Data lPingMarker = pingMarkers.get(lKey).get(lIndex);
                if (aInX - Utilities.convertXY(lPingMarker.getXValue()) > MAX_STORED_PING_DURATION) {
                    pingPoints.get(lKey).remove(lPingMarker);
                } else break;
            }

            // Refresh display
            Platform.runLater(() -> {
                refreshPingSeries(lKey);
                refreshPingAxisBounds();
            });

        }

    }

    /**
     * Add a new point in the speed test bar series
     * @param aInMode    Mode (download, upload) of the speed test series
     * @param aInX       X coordinate of the new data
     * @param aInY       Y coordinate of the new data
     * @param aInType    Type of speed test (manual or periodic)
     */
    public void addSpeedTestSeriesData(EnumTypes.SpeedTestMode aInMode, long aInX, long aInY, EnumTypes.SpeedTestType aInType) {

        if (speedTestManageCheckBox.isSelected()) {

            int lKey = Objects.hash(aInMode);

            // Initialize set of points
            if (speedTestPoints.get(lKey) == null) {
                List<SpeedTestPoint> lPoints = new ArrayList<>();
                speedTestPoints.put(lKey, lPoints);
            }
            // Add the new point to set of points
            speedTestPoints.get(lKey).add(new SpeedTestPoint(aInX, aInY, aInType));

            // Sort speedTestPoints by date
            speedTestPoints.get(lKey).sort(Comparator.comparing(SpeedTestPoint::getX));

            // Remove oldest points from the set of points and the series
            for (int lIndex = 0; lIndex < speedTestPoints.get(lKey).size(); lIndex++) {
                SpeedTestPoint lPingSpeedTestPoint = speedTestPoints.get(lKey).get(lIndex);
                if (aInX - lPingSpeedTestPoint.getX() > MAX_STORED_SPEED_TEST_DURATION) {
                    speedTestPoints.get(lKey).remove(lPingSpeedTestPoint);
                } else break;
            }

            // Refresh display
            Platform.runLater(() -> {
                refreshSpeedTestSeries(lKey);
                refreshSpeedTestAxisBounds();
            });

        }

    }

    // MESSAGES

    /**
     * Prints a message with the required level in the required text flow
     * @param aInMessage  Message to display
     * @param aInTextFlow Text flow the message must be printed into
     */
    public void printMessage(Message aInMessage, TextFlow aInTextFlow) {
        Platform.runLater(() -> {
            aInMessage.println(aInTextFlow);
            if (!firstDisplay.get(aInTextFlow)) changeConsoleTabModificationIndicator(1, tabs.get(aInTextFlow));
            firstDisplay.put(aInTextFlow, false);
        });
    }


    /**
     * Prints a message with the required level in the console text flow
     * @param aInMessage      Message to display
     */
    public void printConsole(Message aInMessage) {
        printMessage(aInMessage, consoleTextFlow);
    }

    /**
     * Prints a message with the required level in the speed test text flow - No change indicator for in tab for this console, so don't call generic printMessage
     * @param aInMessage      Message to display
     */
    public void printSpeedTest(Message aInMessage) {
        Platform.runLater(() -> {
            aInMessage.println(speedTestTextFlow);
        });
    }

    /**
     * Replaces last message with the required level in the required text flow
     * @param aInMessage  Message to display in replacement of last displayed message
     * @param aInTextFlow Text flow the message must be printed into
     */
    public void replaceLastMessage(Message aInMessage, TextFlow aInTextFlow) {
        Platform.runLater(() -> {
            if (aInTextFlow.getChildren().size() > 0) aInTextFlow.getChildren().remove(aInTextFlow.getChildren().size() - 1);
            aInMessage.println(aInTextFlow);
        });
    }

    /**
     * Replaces last message with the required level in the console text flow
     * @param aInMessage  Message to display in replacement of last displayed message
     */
    public void replaceLastConsoleMessage(Message aInMessage) {
        replaceLastMessage(aInMessage, consoleTextFlow);
    }

    /**
     * Replaces last message with the required level in the speed test text flow
     * @param aInMessage  Message to display in replacement of last displayed message
     */
    public void replaceLastSpeedTestMessage(Message aInMessage) {
        replaceLastMessage(aInMessage, speedTestTextFlow);
    }

    /**
     * Clears all messages from the text flows
     */
    public void clearAllMessages() {
        consoleTextFlow.getChildren().clear();
        speedTestTextFlow.getChildren().clear();
    }

    /**
     * Gets last console message
     * @return Last console message
     */
    public String getLastMessage() {
        return (consoleTextFlow.getChildren().size() == 0) ? "" : consoleTextFlow.getChildren().get(consoleTextFlow.getChildren().size() - 1).toString();
    }

    /**
     * Changes the modification flag of the required text flow tab
     * @param aInIncrement  Flag increment
     * @param aInTab        Tab the modification flag must be displayed
     */
    public void changeConsoleTabModificationIndicator(int aInIncrement, Tab aInTab) {

        if (aInIncrement < 0) aInIncrement = 0;
        if (!aInTab.isSelected()) {
            aInTab.setStyle("-fx-font-style: italic");
            Pattern lPattern = Pattern.compile("\\((-*[0-9]+)\\)$");
            Matcher lMatcher = lPattern.matcher(aInTab.getText());
            if (!lMatcher.find()) {
                aInTab.setText(aInTab.getText() + " (" + aInIncrement + ")");
            } else {
                aInIncrement += Integer.valueOf(lMatcher.group(1));
                aInTab.setText(aInTab.getText().replaceAll(" \\(-*[0-9]+\\)$", "") + " (" + aInIncrement + ")");
            }
        }
    }

    /**
     * Sets the speed test server
     */
    public void setSpeedTestServer() {
        if (speedTestServer != null) {
            speedTestServerLabel.setText(speedTestServer);
        } else {
            speedTestServerLabel.setText("");
        }
    }

    public void reloadSpeedTestConfiguration() {
        speedTestServer = Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_NAME_PREFERENCE);
        speedTestUploadUrl = Preferences.getInstance().getValue(Constants.SPEED_TEST_SERVER_URL_PREFERENCE);
        if (speedTestUploadUrl != null) speedTestDownloadUrl = speedTestUploadUrl.replaceAll("upload.php", "random4000x4000.jpg");
        setSpeedTestServer();
        speedTestStartStopButton.setDisable(false);
        refreshAllSpeedTestSeries();
    }

    /**
     * Switches speed test button between start and stop
     */
    public void switchStopStartSpeedTestButton() {

        Platform.runLater(() -> {
            if (speedTestStartState) {
                speedTestStartStopButton.setText(Display.getViewResourceBundle().getString("catView.speedTest.stop"));
                speedTestConfigureButton.setDisable(true);
                speedTestStartStopButton.getStyleClass().add("buttonWarning");
                speedTestStartStopButton.setDisable(false);
                Tooltip lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.speedTest.tooltip.stop"));
                if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE))
                    Tooltip.install(speedTestStartStopButton, lTooltip);
            } else {
                speedTestStartStopButton.setText(Display.getViewResourceBundle().getString("catView.speedTest.start"));
                speedTestConfigureButton.setDisable(false);
                if (speedTestStartStopButton.getStyleClass().contains("buttonWarning")) {
                    speedTestStartStopButton.getStyleClass().removeAll("buttonWarning");
                }
                speedTestStartStopButton.setDisable(speedTestDownloadUrl == null || speedTestUploadUrl == null);
                Tooltip lTooltip = new Tooltip(Display.getViewResourceBundle().getString("catView.speedTest.tooltip.start"));
                if (Preferences.getInstance().getBooleanValue("enableGeneralTooltip", Constants.DEFAULT_ENABLE_GENERAL_TOOLTIP_PREFERENCE))
                    Tooltip.install(speedTestStartStopButton, lTooltip);
            }
            speedTestStartState = !speedTestStartState;
        });
    }

    public void setLiveSpeedTestStyle(EnumTypes.SpeedTestMode aInMode, EnumTypes.SpeedTestType aInType) {
        XYChart.Series<Number, Number> lLiveSpeedTestSeries;
        if (aInMode == EnumTypes.SpeedTestMode.DOWNLOAD) {
            lLiveSpeedTestSeries = liveSpeedTestDownloadSeries;
        } else {
            lLiveSpeedTestSeries = liveSpeedTestUploadSeries;
        }
        String lStyle = "chart-" + EnumTypes.SpeedTestMode.valueOf(aInMode) + "-" + EnumTypes.SpeedTestType.valueOf(aInType);
        for (String lStyleClass: lLiveSpeedTestSeries.getNode().getStyleClass()) {
            if (lStyleClass.contains("chart-" + EnumTypes.SpeedTestMode.valueOf(aInMode))) {
                lLiveSpeedTestSeries.getNode().getStyleClass().remove(lStyleClass);
                break;
            }
        }
        lLiveSpeedTestSeries.getNode().getStyleClass().add(lStyle);
    }

}
