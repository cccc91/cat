package cclerc.services;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

import cclerc.cat.Cat;
import javafx.scene.control.Tooltip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;

/**
 *   Static class implementing miscellaneous services
 */
public class Utilities {

    public final static long NO_DURATION = -1;

    /**
     * Returns a duration in a formatted way
     * @param aInDuration  Duration to format in ms - -1 mean no valid duration
     * @param aInPrecision Number of digits to be displayed for milliseconds
     * @return Formatted duration
     */
    public static String formatDuration(long aInDuration, int aInPrecision) {

        String lDuration = "-";

        if (aInDuration != NO_DURATION) {

            long lMilliseconds = aInDuration % 1000;
            aInDuration /= 1000;

            long lHours = aInDuration / 3600;
            long lMinutes = (aInDuration % 3600) / 60;
            long lSeconds = aInDuration % 60;

            if (aInPrecision == 0) {
                lDuration =
                        (lHours != 0) ?
                                ((lMinutes != 0) ?
                                        String.format("%2d%s%2d%s%2d%s", lHours, "h", lMinutes, "min", lSeconds, "s") :
                                        String.format("%2d%s%2d%s", lMinutes, "min", lSeconds, "s")) :
                                ((lMinutes != 0) ?
                                        String.format("%2d%s%2d%s", lMinutes, "min", lSeconds, "s") :
                                        String.format("%2d%s", lSeconds, "s"));
            } else {
                String lDecimalSeparator = LocaleUtilities.getInstance().getDecimalSeparator();
                lDuration =
                        (lHours != 0) ?
                                ((lMinutes != 0) ?
                                        String.format("%2d%s%2d%s%2d%s%0" + aInPrecision + "d%s", lHours, "h", lMinutes, "min", lSeconds, lDecimalSeparator, lMilliseconds, "s") :
                                        String.format("%2d%s%2d%s%0" + aInPrecision + "d%s", lMinutes, "min", lSeconds, lDecimalSeparator, lMilliseconds, "s")) :
                                ((lMinutes != 0) ?
                                        String.format("%2d%s%2d%s%0" + aInPrecision + "d%s", lMinutes, "min", lSeconds, lDecimalSeparator, lMilliseconds, "s") :
                                        String.format("%2d%s%0" + aInPrecision + "d%s", lSeconds, lDecimalSeparator, lMilliseconds, "s"));
            }

        }

        return lDuration;

    }

    public static String formatDuration(long aInDuration) {
        return formatDuration(aInDuration, 3);
    }

    /**
     * Returns stack trace exception as a string
     * @param aInException Exception
     * @return Stack trace exception as a string
     */
    public static String getStackTrace(Exception aInException) {

        StringWriter lStringWriter = new StringWriter();
        aInException.printStackTrace(new PrintWriter(lStringWriter));
        return lStringWriter.toString();

    }

    /**
     * Joins input by capitalizing first letter of all words except first
     * @param aInString    String to capitalize
     * @param aInSeparator Words separator
     * @return             Formatted string
     */
    public static String capitalizeWordsFirstLetter(String aInString, String aInSeparator) {
        String[] lWords = aInString.split(aInSeparator);
        StringBuilder lStringBuilder = new StringBuilder();
        int lWordsCount = 0;
        for (String lWord : lWords) {
            if (++lWordsCount == 1) {
                lStringBuilder.append(lWord.toLowerCase());
            } else {
                if (lWord.length() > 0)
                    lStringBuilder.append(lWord.substring(0, 1).toUpperCase()).append(lWord.substring(1).toLowerCase());
            }
        }
        return lStringBuilder.toString();
    }

    /**
     * Capitalize input and add separator between words
     * @param aInString    String to capitalize
     * @param aInSeparator Words separator
     * @return             Formatted string
     */
    public static String separateAndCapitalizeWords(String aInString, String aInSeparator) {
        String[] lChars = aInString.split("");
        StringBuilder lStringBuilder = new StringBuilder();
        for (String lChar : lChars) {
            if (lChar.toUpperCase().equals(lChar))
                lStringBuilder.append(aInSeparator).append(lChar);
            else
                lStringBuilder.append(lChar);
        }
        return lStringBuilder.toString().toUpperCase();
    }

    /**
     * Returns current list of any type if the list is not null, returns empty list otherwise
     * @param aInIterable List to make safe
     * @param <T>         Type of elements of the list
     * @return            Empty list or initial list
     */
    public static <T> Iterable<T> safeList(Iterable<T> aInIterable) {
        return aInIterable == null ? Collections.<T> emptyList() : aInIterable;
    }

    /**
     * Gets logger file absolute path name
     * @param aInLogger Log for which file name is required
     * @return Log file absolute path name
     */
    public static String getLoggerFileName(Logger aInLogger) {

        org.apache.logging.log4j.core.Logger loggerImpl =  (org.apache.logging.log4j.core.Logger) aInLogger;
        Appender appender = loggerImpl.getAppenders().get("fileLogger");
        // Unfortunately, File is no longer an option to return, here.
        return new File(((RollingFileAppender) appender).getFileName()).getAbsolutePath();

    }

    /**
     * Hack allowing to modify the default behavior of the tooltips.
     * @param aInOpenDelay The open delay, knowing that by default it is set to 1000.
     * @param aInVisibleDuration The visible duration, knowing that by default it is set to 5000.
     * @param aInCloseDelay The close delay, knowing that by default it is set to 200.
     * @param aInHideOnExit Indicates whether the tooltip should be hidden on exit,
     * knowing that by default it is set to false.
     */
    public static void updateTooltipBehavior(double aInOpenDelay, double aInVisibleDuration, double aInCloseDelay, boolean aInHideOnExit) {

        try {

            // Get the non public field "BEHAVIOR"
            Field lFieldBehavior = Tooltip.class.getDeclaredField("BEHAVIOR");
            // Make the field accessible to be able to get and set its value
            lFieldBehavior.setAccessible(true);
            // Get the value of the static field
            Object lObjectBehavior = lFieldBehavior.get(null);
            // Get the constructor of the private static inner class TooltipBehavior
            Constructor<?> lConstructor = lObjectBehavior.getClass().getDeclaredConstructor(Duration.class, Duration.class, Duration.class, boolean.class);
            // Make the constructor accessible to be able to invoke it
            lConstructor.setAccessible(true);
            // Create a new instance of the private static inner class TooltipBehavior
            Object lTooltipBehavior = lConstructor.newInstance(new Duration(aInOpenDelay), new Duration(aInVisibleDuration), new Duration(aInCloseDelay), aInHideOnExit);
            // Set the new instance of TooltipBehavior
            lFieldBehavior.set(null, lTooltipBehavior);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    /**
     * Checks if current Java version is compatible with required one
     * @param aInMinJavaVersion Minimum Java version required
     * @return true if Java version is greater or equal to minimum Java version, false otherwise
     */
    public static boolean isJavaVersionValid(String aInMinJavaVersion) {

        String lCurrentJavaVersion[] = System.getProperty("java.version").split("[_.]");
        String lMinjavaVersion[] = aInMinJavaVersion.split("[_.]");
        return !((Integer.valueOf(lCurrentJavaVersion[0]) < Integer.valueOf(lMinjavaVersion[0])) ||
            (Integer.valueOf(lCurrentJavaVersion[0]) == Integer.valueOf(lMinjavaVersion[0]) &&
                Integer.valueOf(lCurrentJavaVersion[1]) < Integer.valueOf(lMinjavaVersion[1])) ||
            (Integer.valueOf(lCurrentJavaVersion[0]) == Integer.valueOf(lMinjavaVersion[0]) &&
                Integer.valueOf(lCurrentJavaVersion[1]) == Integer.valueOf(lMinjavaVersion[1]) &&
                Integer.valueOf(lCurrentJavaVersion[2]) < Integer.valueOf(lMinjavaVersion[2])) ||
            (Integer.valueOf(lCurrentJavaVersion[0]) == Integer.valueOf(lMinjavaVersion[0]) &&
                Integer.valueOf(lCurrentJavaVersion[1]) == Integer.valueOf(lMinjavaVersion[1]) &&
                Integer.valueOf(lCurrentJavaVersion[2]) == Integer.valueOf(lMinjavaVersion[2]) &&
                Integer.valueOf(lCurrentJavaVersion[3]) < Integer.valueOf(lMinjavaVersion[3])));

    }

    /** Plays a sound
     * @param aInSoundFilePath Sound file path to play (mp3, wav)
     */
    public static void playSound(String aInSoundFilePath) throws Exception {
        if (aInSoundFilePath.startsWith("resources/")) {
            aInSoundFilePath = Cat.class.getClassLoader().getResource(aInSoundFilePath).toURI().toString();
        } else {
            aInSoundFilePath = new File(aInSoundFilePath).toURI().toString();
        }
        Media lMedia = new Media(aInSoundFilePath);
        MediaPlayer lMediaPlayer = new MediaPlayer(lMedia);
        lMediaPlayer.play();
    }

    /**
     * Waits for the specified delay
     * @param aInDelay Delay to wait (ms)
     */
    public static void sleep(long aInDelay) {
        try {
            Thread.sleep(aInDelay);
        } catch (InterruptedException e) {
            Display.logUnexpectedError(e);
        }
    }

    /**
     * Converts a XYChart.Data coordinate to a long value
     * @param aInCoordinate X or Y coordinate to convert
     * @return Converted value of the coordinate
     */
    public static long convertXY(Object aInCoordinate) {
        if (aInCoordinate instanceof Number) return ((Number) aInCoordinate).longValue();
        else return 0L;
    }

    /**
     * Retrives the next time an event must be executed depending on its last execution time and period
     * The first time, last execution time is null, next execution time is computed so that it starts at an exact hour/minute if possible
     * @param aInLastExecutionTime Last execution time in epoch ms
     * @param aInPeriod            Period in min
     * @param aInOffset            Offset in min to be added to the first execution time
     * @return                     Next execution time in epoch ms
     */
    public static Long nextExecutionTime(Long aInLastExecutionTime, int aInPeriod, int aInOffset, int aInHour) {

        if (aInHour > 12) aInHour -= 12;

        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = now.getMinute();

        if (aInLastExecutionTime == null) {
            if (aInPeriod < 60) {
                return LocaleUtilities.getInstance().getLocalDate(LocalDateTime.of(year, month, day, hour, minute))
                       + (aInPeriod - (minute % aInPeriod) + aInOffset) * 60_000;
            } else if (aInPeriod == 60 * 12) {
                if (hour < aInHour || hour > aInHour + 12 || (hour == aInHour && minute < aInOffset) || (hour == aInHour + 12 && minute > aInOffset)) {
                    int lDayOffset = (hour > aInHour + 12 || (hour == aInHour + 12 && minute > aInOffset)) ? 1 : 0;
                    return LocaleUtilities.getInstance().getLocalDate(LocalDateTime.of(year, month, day, aInHour, aInOffset)) + 60_000 * 60 * 24 * lDayOffset;
                } else {
                    return LocaleUtilities.getInstance().getLocalDate(LocalDateTime.of(year, month, day, aInHour + 12, aInOffset));
                }
            } else if (aInPeriod < 60 * 24) {
                return LocaleUtilities.getInstance().getLocalDate(LocalDateTime.of(year, month, day, hour, aInOffset)) + 60_000 * 60;
            } else {
                return LocaleUtilities.getInstance().getLocalDate(LocalDateTime.of(year, month, day, 0, aInOffset)) + 60_000 * 60 * 24 * (aInPeriod / (24 * 60));
            }
        } else {
            return aInLastExecutionTime + aInPeriod * 60_000;
        }


    }
    public static Long nextExecutionTime(Long aInLastExecutionTime, int aInPeriod, int aInOffset) {
        return nextExecutionTime(aInLastExecutionTime, aInPeriod, aInOffset, 8);
    }

    /**
     * Exports a resource potentially embedded into a jar file to the local file path if it is not already exported.
     *
     * @param aInResourceName         Resource to extract
     * @param aInDestinationDirectory Destination directory
     * @return Path to the exported resource
     */
     public static String exportResource(String aInResourceName, String aInDestinationDirectory) throws Exception {

        InputStream lInputStream = null;
        OutputStream lOutputStream = null;
        String lDestinationPath;

        try {

            lInputStream = Utilities.class.getClassLoader().getResourceAsStream(aInResourceName);

            int lReadBytes;
            byte[] lBuffer = new byte[4096];

            lDestinationPath = new File(aInDestinationDirectory).getPath().replace('\\', '/') + "/" + new File(aInResourceName).getName();
            File lDestinationFile = new File(lDestinationPath);
            if (!lDestinationFile.exists()) {
                lOutputStream = new FileOutputStream(lDestinationPath);
                while ((lReadBytes = lInputStream.read(lBuffer)) > 0) {
                    lOutputStream.write(lBuffer, 0, lReadBytes);
                }
            }

        } catch (Exception e) {
            throw e;
        } finally {
            if (lInputStream != null) lInputStream.close();
            if (lOutputStream != null) lOutputStream.close();
        }

        return lDestinationPath;
    }

    /**
     * Rounds a double to the required decimal places
     * @param aInValue  Value to round
     * @param aInPlaces Number of decimals
     * @return
     */
    public static double round(double aInValue, int aInPlaces) {

        if (aInPlaces < 0) throw new IllegalArgumentException();

        BigDecimal lBigDecimal = new BigDecimal(aInValue);
        lBigDecimal = lBigDecimal.setScale(aInPlaces, RoundingMode.HALF_UP);
        return lBigDecimal.doubleValue();

    }

}
