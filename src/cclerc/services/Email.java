package cclerc.services;

import javax.mail.MessagingException;
import java.util.*;

/**
 * Class implementing emails
 * Allows defining a set of SMTP servers and send emails with the preferred one or if failed, choose another one in the list
 * Allows enabling or disabling the sending of emails
 */
public class Email {

    // Duplicate SMTP server exception
    public static class AddSmtpServerException extends Exception {
        public AddSmtpServerException(Throwable cause) {
            super(cause);
        }
    }

    // Possible SMTP servers
    private static HashMap<String, SmtpServer> smtpServers = new HashMap<>();

    // Email properties
    private boolean enabled;
    private String preferredSmtpServer;
    ArrayList<String> recipientList = new ArrayList<>();

    // CONSTRUCTORS

    /**
     * Email constructor
     *
     * @param aInEnabled Email is audibleEnabled
     */
    public Email(boolean aInEnabled) {
        enabled = aInEnabled;
    }

    /**
     * Email constructor
     *
     * @param aInEnabled             Email is enabled
     * @param aInPreferredSmtpServer Preferred smtp server
     */
    public Email(boolean aInEnabled, String aInPreferredSmtpServer) {
        enabled = aInEnabled;
        if ((aInPreferredSmtpServer != null) && smtpServers.containsKey(aInPreferredSmtpServer)) {
            preferredSmtpServer = aInPreferredSmtpServer;
        } else {
            preferredSmtpServer = null;
        }
    }

    // GETTERS

    /**
     * Checks if email is enabled
     *
     * @return true if email is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    // SETTERS

    /**
     * Sets email recipient list
     * @param aInRecipientList       List of email recipient
     */
    public void setRecipients(List<String> aInRecipientList) {
        if (aInRecipientList.size() != 1 || !aInRecipientList.contains("")) {
            recipientList.addAll(aInRecipientList);
        }
    }

    public void setPreferredSmtpServer(String aInPreferredSmtpServer) {
        if ((aInPreferredSmtpServer != null) && smtpServers.containsKey(aInPreferredSmtpServer)) {
            preferredSmtpServer = aInPreferredSmtpServer;
        } else {
            Display.getLogger().warn(String.format(Display.getMessagesResourceBundle().getString("log.email.invalidPreferredSmtpServer"), aInPreferredSmtpServer));
            preferredSmtpServer = null;
        }
    }

    /**
     * Enables email
     */
    public void enable() {
        enabled = true;
    }

    /**
     * Disables email
     */
    public void disable() {
        enabled = false;
    }

    // METHODS

    /**
     * Adds a SMTP server to the list of usable SMTP servers
     *
     * @param aInHost              SMTP host - Mandatory
     * @param aInTlsMode           TLS mode - starttls for ssl connection, empty for non ssl connection
     * @param aInPort              Port - Can be empty
     * @param aInUser              User - Mandatory
     * @param aInLogin             Login - Can be empty (no authentication)
     * @param aInPassword          Password - Can be empty (no authentication)
     * @param aInConnectionTimeout Connection timeout (s) - Can be empty (default: infinite)
     * @param aInTimeout           Timeout (s) - Can be empty (default: infinite)
     */
    public static void addSmtpServer(
            String aInHost, String aInTlsMode, String aInPort, String aInUser, String aInLogin, String aInPassword, String aInConnectionTimeout, String aInTimeout)
            throws AddSmtpServerException {

        if (!smtpServers.containsKey(aInUser + '@' + aInHost)) {
            if (aInHost.equals("")) {
                throw new AddSmtpServerException(new Throwable("Invalid host, cannot be empty"));
            }
            if (aInUser.equals("")) {
                throw new AddSmtpServerException(new Throwable("Invalid user, cannot be empty"));
            }
            if (!aInPort.equals("") && !aInPort.matches("\\d+") && Integer.valueOf(aInPort) < 1 && Integer.valueOf(aInPort) > 65535) {
                throw new AddSmtpServerException(new Throwable("Invalid port " + aInPort));
            }
            if (!aInConnectionTimeout.equals("") && !aInConnectionTimeout.matches("\\d+") && Integer.valueOf(aInConnectionTimeout) < 0) {
                throw new AddSmtpServerException(new Throwable("Invalid connection timeout " + aInConnectionTimeout));
            }
            if (!aInTimeout.equals("") && !aInTimeout.matches("\\d+") && Integer.valueOf(aInTimeout) < 0) {
                throw new AddSmtpServerException(new Throwable("Invalid timeout " + aInTimeout));
            }

            aInConnectionTimeout = String.valueOf(Integer.valueOf(aInConnectionTimeout) * 1000);
            aInTimeout = String.valueOf(Integer.valueOf(aInTimeout) * 1000);
            smtpServers.put(aInUser + '@' + aInHost, new SmtpServer(aInHost, aInTlsMode, aInPort, aInUser, aInLogin, aInPassword, aInConnectionTimeout, aInTimeout));
        } else {
            throw new AddSmtpServerException(new Throwable("Duplicate entry " + aInUser + '@' + aInHost));
        }

    }

    /**
     * Sends email
     * @param aInSubject             Subject of the email
     * @param aInContent             Content of the email (HTML)
     * @param aInPreferredSmtpServer Preferred SMTP server identifier (email@host), must have been added in smtpServers
     * @return                       True if the email is correctly sent, false otherwise
     */
    public boolean sendMail(String aInSubject, String aInContent, String aInPreferredSmtpServer) {

        if (enabled) {

            HashMap<String, SmtpServer> lSmtpServers = new HashMap<>(smtpServers);
            if (lSmtpServers.keySet().size() != 0) {

                String lCurrentSmtpServerKey = (aInPreferredSmtpServer != null) ? aInPreferredSmtpServer : lSmtpServers.keySet().iterator().next();
                boolean lCompleted = false;
                boolean lSuccess = false;
                while (!lCompleted) {

                    SmtpServer lCurrentSmtpServer = lSmtpServers.remove(lCurrentSmtpServerKey);

                    try {

                        // Default recipient list
                        if (recipientList.size() == 0) {
                            recipientList.add(lCurrentSmtpServer.getUser());
                        }
                        lCurrentSmtpServer.sendMail(recipientList, lCurrentSmtpServer.getUser(), aInSubject, aInContent);
                        lCompleted = true;
                        lSuccess = true;

                    } catch (MessagingException e) {

                        Display.getLogger().error(String.format(Display.getMessagesResourceBundle().getString("log.email.sendMail"), lCurrentSmtpServerKey, e.getCause()));

                        if (lSmtpServers.keySet().size() != 0) {
                            lCurrentSmtpServerKey = new ArrayList<String>(lSmtpServers.keySet()).get(0);
                        } else {
                            Display.getLogger().error(Display.getMessagesResourceBundle().getString("log.email.noMoreServer"));
                            lCompleted = true;
                        }
                    }

                }

                return lSuccess;

            } else {
                Display.getLogger().warn(Display.getMessagesResourceBundle().getString("log.email.noServerDefined"));
                return false;
            }

        } else {
            return true;
        }

    }

    /**
     * Sends email. Preferred SMTP server is the first one in the list
     * @param aInSubject             Subject of the email
     * @param aInContent             Content of the email (HTML)
     * @return                       True if the email is correctly sent, false otherwise
     */
    public boolean sendMail(String aInSubject, String aInContent) {
        return sendMail(aInSubject, aInContent, preferredSmtpServer);
    }

}