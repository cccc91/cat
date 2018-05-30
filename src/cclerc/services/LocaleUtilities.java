package cclerc.services;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/**
 *   Singleton class implementing locale services
 */
public class LocaleUtilities {

    private Locale currentLocale = Locale.getDefault();
    private DateFormat dateFormat;
    private DateFormat timeFormat;
    private DateFormat mediumDateAndTimeFormat;

    // Locale instance
    private static LocaleUtilities localeUtilitiesInstance = new LocaleUtilities();

    /**
     * Current locale constructor
     */
    private LocaleUtilities() {
        dateFormat = DateFormat.getDateInstance(DateFormat.LONG, currentLocale);
        timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM, currentLocale);
        mediumDateAndTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, currentLocale);
    }

    // SINGLETON

    /**
     * Returns the singleton
     * @return Locale utility singleton instance
     */
    public static LocaleUtilities getInstance() {
        return localeUtilitiesInstance;
    }

    // GETTERS

    /**
     * Gets the current locale
     * @return Current locale
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Gets the date format
     * @return Date format
     */
    public DateFormat getDateFormat() {
        return dateFormat;
    }

    /**
     * Gets the time format
     * @return Time format
     */
    public DateFormat getTimeFormat() {
        return timeFormat;
    }

    /**
     * Gets the short date and time format
     * @return Short date and time format
     */
    public DateFormat getMediumDateAndTimeFormat() {
        return mediumDateAndTimeFormat;
    }

    /**
     * Gets decimal separator
     * @return Decimal separator
     */
    public String getDecimalSeparator() {
        DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(currentLocale);
        return "" + formatter.getDecimalFormatSymbols().getDecimalSeparator();
    }

    // SETTERS

    /**
     * Changes the local
     * @param aInCurrentLocale New locale
     */
    public void setCurrentLocale(Locale aInCurrentLocale) {
        currentLocale = aInCurrentLocale;
        dateFormat = DateFormat.getDateInstance(DateFormat.LONG, currentLocale);
        timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM, currentLocale);
    }


    public LocalDate getLocalDate(long aInEpoch) {
        return Instant.ofEpochMilli(aInEpoch).atZone(ZoneId.systemDefault()).toLocalDate();
    }

}
