package cclerc.services;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Class implementing sending mail with SMTP server
 */
public class SmtpServer {

    private static final Properties systemProperties = System.getProperties();

    // SMTP properties
    private Session mailSession;
    private String user;

    /**
     * Constructor
     * @param aInHost              SMTP host - Mandatory
     * @param aInTlsMode           TLS mode - starttls for ssl connection, empty for non ssl connection
     * @param aInPort              Port - Can be empty
     * @param aInUser              User - Mandatory
     * @param aInLogin             Login - Can be empty (no authentication)
     * @param aInPassword          Password - Can be empty (no authentication)
     * @param aInConnectionTimeout Connection timeout (s) - Can be empty (default: infinite)
     * @param aInTimeout           Timeout (s) - Can be empty (default: infinite)
     */
    public SmtpServer(String aInHost, String aInTlsMode, String aInPort, String aInUser, String aInLogin, String aInPassword, String aInConnectionTimeout, String aInTimeout) {

        // Setup mail properties
        Properties lProperties = new Properties();
        lProperties.putAll(systemProperties);
        user = aInUser;

        // Host
        try {
            String lSmtpHostIp = InetAddress.getByName(aInHost).getHostAddress();
            lProperties.setProperty("mail.smtp.host", lSmtpHostIp);
        } catch (Exception e) {
            lProperties.setProperty("mail.smtp.host", aInHost);
        }

        // Encryption
        if (aInTlsMode.equals("starttls")) {
            lProperties.setProperty("mail.smtp.starttls.enable", "true");
            lProperties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            lProperties.setProperty("mail.smtp.socketFactory.fallback", "false");
        }

        // Port
        if (!aInPort.equals("")) {
            lProperties.setProperty("mail.smtp.port", aInPort);
        }

        // Timeouts
        if (!aInConnectionTimeout.equals("")) {
            lProperties.setProperty("mail.smtp.connectiontimeout", aInConnectionTimeout);
        }
        if (!aInTimeout.equals("")) {
            lProperties.setProperty("mail.smtp.timeout", aInTimeout);
        }

        // User
        lProperties.setProperty("mail.smtp.user", aInUser);

        // Authentication
        if (!aInPassword.equals("")) {
            lProperties.setProperty("mail.smtp.auth", "true");
            mailSession = Session.getInstance(lProperties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(aInLogin, aInPassword);
                }
            });
        } else {
            mailSession = Session.getDefaultInstance(lProperties);
        }

    }

    // GETTERS

    public String getUser() {
        return user;
    }

    // METHODS

    public void sendMail(ArrayList<String> aInRecipientList, String aInSender, String aInSubject, String aInContent) throws MessagingException {

        // Create message to send
        MimeMessage lMessage = new MimeMessage(mailSession);

        // Add sender
        lMessage.setFrom(new InternetAddress(aInSender));

        // Add recipients
        Iterator<String> lIterator = aInRecipientList.iterator();
        while (lIterator.hasNext()) {
            String lRecipient = lIterator.next();
            lMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(lRecipient));
        }

        // Add subject
        lMessage.setSubject(aInSubject, "UTF-8");

        // Add content
        //lMessage.setContent(aInContent, "text/html");
        Multipart lMultiPart = new MimeMultipart();
        MimeBodyPart lMultiPartBody = new MimeBodyPart();
        lMultiPartBody.setContent(aInContent, "text/html;charset=utf-8");
        lMultiPart.addBodyPart(lMultiPartBody);
        lMessage.setContent(lMultiPart);

        // Add date
        lMessage.setSentDate(new java.util.Date());

        // Send mail
        Transport.send(lMessage);

    }
}
