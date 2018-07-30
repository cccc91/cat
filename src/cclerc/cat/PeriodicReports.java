package cclerc.cat;

import cclerc.cat.Configuration.Configuration;
import cclerc.services.*;

import java.net.InetAddress;
import java.util.Date;

public class PeriodicReports implements Runnable {

    private boolean running = true;
    private boolean periodicReportsEnabled;
    private int period;
    private int offset;
    private Long nextExecutionTime;
    private Email email;
    private boolean initialization = true;

    // Periodic speed test instance
    private static PeriodicReports periodicReports = new PeriodicReports();

    private PeriodicReports() {

    }

    public void start() {

        Thread lThread = new Thread(periodicReports);
        lThread.start();
    }

    @Override
    public void run() {

        // Name thread
        Thread.currentThread().setName("Periodic Reports Thread");

        while (Cat.getInstance().isInitializationInProgress()) {
            Utilities.sleep(1000);
        }

        // Initialize periodic reports data
        resetEmail();
        loadConfiguration();

        while (running) {

            // Run speed test if needed
            if (periodicReportsEnabled && System.currentTimeMillis() >= nextExecutionTime) {
                nextExecutionTime = Utilities.nextExecutionTime(nextExecutionTime, period, offset,
                                                                Preferences.getInstance().getIntegerValue(Constants.PERIODIC_REPORTS_HOUR_PREFERENCE,
                                                                                                          Constants.DEFAULT_PERIODIC_REPORTS_HOUR));
                sendReport();
            }

            Utilities.sleep(Configuration.getCurrentConfiguration().getGlobalMonitoringConfiguration().getPeriodicReportsPollingPeriod());

        }

    }

    // SINGLETON

    public static PeriodicReports getInstance() {
        return periodicReports;
    }

    /**
     * Loads periodic reports configuration
     */
    public void loadConfiguration() {

        periodicReportsEnabled = Preferences.getInstance().getBooleanValue(Constants.PERIODIC_REPORTS_ENABLED_PREFERENCE, Constants.DEFAULT_PERIODIC_REPORTS_ENABLED);
        period = Preferences.getInstance().getIntegerValue(Constants.PERIODIC_REPORTS_PERIOD_PREFERENCE, Constants.DEFAULT_PERIODIC_REPORTS_PERIOD) *
                 Preferences.getInstance().getIntegerValue(Constants.PERIODIC_REPORTS_PERIOD_DISPLAYED_UNIT_PREFERENCE, Constants.DEFAULT_PERIODIC_REPORTS_PERIOD_DISPLAYED_UNIT);
        offset = Preferences.getInstance().getIntegerValue(Constants.PERIODIC_REPORTS_OFFSET_PREFERENCE, Constants.DEFAULT_PERIODIC_REPORTS_OFFSET);

        nextExecutionTime = Utilities.nextExecutionTime((nextExecutionTime == null || nextExecutionTime > System.currentTimeMillis()) ? null : nextExecutionTime, period, offset,
                                                        Preferences.getInstance().getIntegerValue(Constants.PERIODIC_REPORTS_HOUR_PREFERENCE,
                                                                                                  Constants.DEFAULT_PERIODIC_REPORTS_HOUR));
        if (!initialization) {
            if (periodicReportsEnabled && email.isEnabled())
                Cat.getInstance().getController().printConsole(
                        new Message(String.format(Display.getViewResourceBundle().getString("globalMonitoring.reports.next"),
                                                  LocaleUtilities.getInstance().getDateFormat().format(new Date(nextExecutionTime)),
                                                  LocaleUtilities.getInstance().getTimeFormat().format(new Date(nextExecutionTime).getTime())),
                                    EnumTypes.MessageLevel.INFO));
            else
                Cat.getInstance().getController().printConsole(
                        new Message(Display.getViewResourceBundle().getString("globalMonitoring.reports.disable"), EnumTypes.MessageLevel.INFO));

        } else {
            if (periodicReportsEnabled && email.isEnabled())
                Cat.getInstance().getController().printConsole(
                        new Message(String.format(Display.getViewResourceBundle().getString("globalMonitoring.reports.start.enable"),
                                                  LocaleUtilities.getInstance().getDateFormat().format(new Date(nextExecutionTime)),
                                                  LocaleUtilities.getInstance().getTimeFormat().format(new Date(nextExecutionTime).getTime())),
                                    EnumTypes.MessageLevel.INFO));
            else
                Cat.getInstance().getController().printConsole(
                        new Message(Display.getViewResourceBundle().getString("globalMonitoring.reports.start.disable"), EnumTypes.MessageLevel.INFO));
        }
        initialization = false;

    }

    // METHODS

    /**
     * Builds a new report email
     */
    public void resetEmail() {
        email = new Email(
                States.getInstance().getBooleanValue((Cat.getInstance().getController().BuildStatePropertyName(Constants.SEND_MAIL_STATE)), true) &&
                Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getSmtpServerConfigurations().size() != 0 &&
                !Configuration.getCurrentConfiguration().getEmailConfiguration().getRecipientList().isEmpty(),
                Configuration.getCurrentConfiguration().getEmailConfiguration().getSmtpServersConfiguration().getPreferredSmtpServer());
    }

    /**
     * Disables email
     */
    public void disableEmail() {
        Cat.getInstance().getController().printConsole(
                new Message(Display.getViewResourceBundle().getString("globalMonitoring.reports.disable"), EnumTypes.MessageLevel.INFO));
        email.disable();

    }

    /**
     * Enables email
     */
    public void enableEmail() {
        Cat.getInstance().getController().printConsole(
                new Message(String.format(Display.getViewResourceBundle().getString("globalMonitoring.reports.next"),
                                          LocaleUtilities.getInstance().getDateFormat().format(new Date(nextExecutionTime)),
                                          LocaleUtilities.getInstance().getTimeFormat().format(new Date(nextExecutionTime).getTime())),
                            EnumTypes.MessageLevel.INFO));
        resetEmail();
    }

    /**
     * Sends report for current period and resets data
     */
    public void sendReport() {

        String lLocalHostName = "";
        try {
            lLocalHostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }

        String lEmailBody = "<meta http-equiv=\"Content-Type\" charset=\"UTF-16\">\n" +
                            GlobalMonitoring.getInstance().buildReport() + PeriodicSpeedTest.getInstance().buildReport();

        email.sendMail(String.format(Display.getMessagesResourceBundle().getString("generalEmail.periodicReports.subject"), lLocalHostName), lEmailBody);
        if (email.isEnabled()) {
            Display.getLogger().info(Display.getMessagesResourceBundle().getString("log.globalMonitoring.reports.new"));
            Cat.getInstance().getController().printConsole(new Message(String.format(Display.getViewResourceBundle().getString("globalMonitoring.reports.new"),
                                                                                     LocaleUtilities.getInstance().getDateFormat().format(new Date(nextExecutionTime)),
                                                                                     LocaleUtilities.getInstance().getTimeFormat().format(new Date(nextExecutionTime).getTime())),
                                                                       EnumTypes.MessageLevel.INFO));
        } else {
            Display.getLogger().info(Display.getMessagesResourceBundle().getString("log.globalMonitoring.reports.notSent"));
            Cat.getInstance().getController().printConsole(new Message(Display.getViewResourceBundle().getString("globalMonitoring.reports.notSent"),
                                                                       EnumTypes.MessageLevel.INFO));
        }


        // Reset all data
        GlobalMonitoring.getInstance().resetReport();
        PeriodicSpeedTest.getInstance().resetReport();
        resetEmail();

    }

}
