/*
 *  Copyright 1997-2011 teatrove.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teatrove.teaapps.contexts;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.teatrove.trove.log.Syslog;
import org.teatrove.trove.util.FastDateFormat;

/**
 * Custom Tea context that provides convenience methods for handling dates and
 * times in Tea including creating, parsing, formatting, adjustments, and
 * general helper methods.
 * 
 * @see Date
 */
public class DateContext {

    private static SimpleDateFormat cFormatter = 
        new SimpleDateFormat("yyyyMMdd");

    public static final String DAY = "d";
    public static final String HOUR = "H";
    public static final String MINUTE = "m";
    public static final String SECOND = "s";
    public static final String MONTH = "M";
    public static final String YEAR = "y";
    
    /**
     * Returns a standard Java Date object with the current date and time.
     */
    public java.util.Date currentDate() {
        return new Date();
    }

    /**
     * Returns a Joda DateTime object with the current date and time.
     */
    public org.joda.time.DateTime currentDateTime() {
        return new org.joda.time.DateTime();
    }

    /**
     * Create a {@link Date} instance from the given string using the default 
     * input format <code>yyyyMMdd</code>.
     * 
     * @param dateString The string from which to create a date
     * 
     * @return The {@link Date} instance representing date string
     * 
     * @throws ParseException if the date string is improperly formatted
     */
    public Date createDate(String dateString) throws ParseException {
        Date result = null;     
        synchronized (cFormatter) {
            result = cFormatter.parse(dateString);
        }
        return result;
    }

    /**
     * Create a {@link Date} instance from the given string using the default 
     * input format <code>yyyyMMdd</code>.  Exceptions are NOT thrown by this
     * method and instead <code>null</code> is returned to indicate errors.
     * 
     * @param dateString The string from which to create a date
     * 
     * @return The {@link Date} instance representing date string
     * 
     * @see #createDate(String)
     */
    public Date createDateWithValidation(String dateString) {
        try { return createDate(dateString); } 
        catch (Exception exception) {
            Syslog.debug("DateContext.createDateWithValidation:  Error " +
            		     "creating date with " + dateString + "; Exception: " + 
            		     exception.getMessage());
        }
        
        return null;
    }

    /**
     * Creates a {@link Date} instance from the given number of milliseconds
     * since 1970 (time zero).
     * 
     * @param date The number of milliseconds since 1970
     * 
     * @return The {@link Date} instance representing the date.
     */
    public Date createDate(long date) {
        Date result = new Date(date);
        return result;
    }
    
    /**
     * Create a {@link Date} instance for the given date string with the given
     * format.
     * 
     * <p>
     * <strong>Time Format Syntax:</strong>
     * <p>
     * To specify the time format use a <em>time pattern</em> string.
     * In this pattern, all ASCII letters are reserved as pattern letters,
     * which are defined as the following:
     * <blockquote>
     * <pre>
     * Symbol   Meaning                 Presentation        Example
     * ------   -------                 ------------        -------
     * G        era designator          (Text)              AD
     * y        year                    (Number)            1996
     * M        month in year           (Text & Number)     July & 07
     * d        day in month            (Number)            10
     * h        hour in am/pm (1~12)    (Number)            12
     * H        hour in day (0~23)      (Number)            0
     * m        minute in hour          (Number)            30
     * s        second in minute        (Number)            55
     * S        millisecond             (Number)            978
     * E        day in week             (Text)              Tuesday
     * D        day in year             (Number)            189
     * F        day of week in month    (Number)            2 (2nd Wed in July)
     * w        week in year            (Number)            27
     * W        week in month           (Number)            2
     * a        am/pm marker            (Text)              PM
     * k        hour in day (1~24)      (Number)            24
     * K        hour in am/pm (0~11)    (Number)            0
     * z        time zone               (Text)              Pacific Standard Time
     * '        escape for text         (Delimiter)
     * ''       single quote            (Literal)           '
     * </pre>
     * </blockquote>
     * The count of pattern letters determine the format.
     * <p>
     * <strong>(Text)</strong>: 4 or more pattern letters--use full form,
     * &lt; 4--use short or abbreviated form if one exists.
     * <p>
     * <strong>(Number)</strong>: the minimum number of digits. Shorter
     * numbers are zero-padded to this amount. Year is handled specially;
     * that is, if the count of 'y' is 2, the Year will be truncated to 2 digits.
     * <p>
     * <strong>(Text & Number)</strong>: 3 or over, use text, otherwise use number.
     * <p>
     * Any characters in the pattern that are not in the ranges of ['a'..'z']
     * and ['A'..'Z'] will be treated as quoted text. For instance, characters
     * like ':', '.', ' ', '#' and '@' will appear in the resulting time text
     * even they are not embraced within single quotes.
     * <p>
     * A pattern containing any invalid pattern letter will result in a thrown
     * exception during formatting or parsing, which will result in null being
     * returned to the template.
     *
     * <p>
     * <strong>Examples:</strong>
     * <blockquote>
     * <pre>
     * Format Pattern                         Result
     * --------------                         -------
     * "yyyy.MM.dd G 'at' hh:mm:ss z"    ->>  1996.07.10 AD at 15:08:56 PDT
     * "EEE, MMM d, ''yy"                ->>  Wed, July 10, '96
     * "h:mm a"                          ->>  12:08 PM
     * "hh 'o''clock' a, zzzz"           ->>  12 o'clock PM, Pacific Daylight Time
     * "K:mm a, z"                       ->>  0:00 PM, PST
     * "yyyyy.MMMMM.dd GGG hh:mm aaa"    ->>  1996.July.10 AD 12:08 PM
     * </pre>
     * </blockquote>
     * 
     * @param dateString The string from which to create a date
     * @param inputFormat The format of the date string
     * 
     * @return The {@link Date} instance representing the date
     * 
     * @throws ParseException if the date is not in the proper format
     */
    public Date createDate(String dateString, String inputFormat) 
        throws ParseException {
        
        SimpleDateFormat sdf = new SimpleDateFormat(inputFormat);
        return sdf.parse(dateString);
    }
    
    /**
     * Creates a date object based on the format passed in.  Exceptions are NOT 
     * thrown by this method and instead <code>null</code> is returned.  
     * Otherwise, this is identical to {@link #createDate(String, String)}.

     * @param dateString The string from which to create a date
     * @param inputFormat The format of the date string
     * 
     * @return The {@link Date} instance representing the date
     * 
     * @see #createDate(String, String)
     */
    public Date createDateWithValidation(String dateString, String inputFormat) {
        try { return createDate(dateString, inputFormat); } 
        catch (Exception exception) {
            Syslog.debug("DateContextImpl.createDateWithValidation:  Error " +
            		     "creating date with " + dateString + "/" + 
            		     inputFormat + "; Exception: " + exception.getMessage());
        }
        
        return null;
    }

    /**
     * Create a date from the given data. If any of the values are invalid
     * numbers, then a <code>NumberFormatException</code> is thrown.
     * 
     * @param month The month (1-12)
     * @param day The day (1-31)
     * @param year The year (4-digit)
     * @param hour The hour (0-23)
     * @param minute The minutes (0-59)
     * @param second The seconds (0-59)
     * @param millisecond The milliseconds (0-999)
     * 
     * @return The {@link Date} instance for the given date
     * 
     * @throws NumberFormatException if the values are invalid numbers
     */
    public Date createDate(String month, String day, String year, String hour, 
                           String minute, String second, String millisecond) 
    	throws NumberFormatException {
        
        Date date = null;
        date = createDate(Integer.parseInt(month), Integer.parseInt(day), 
                          Integer.parseInt(year), Integer.parseInt(hour), 
                          Integer.parseInt(minute), Integer.parseInt(second), 
                          Integer.parseInt(millisecond));
        
        return date;
    }

    /**
     * Create a date from the given data.
     * 
     * @param month The month (1-12)
     * @param day The day (1-31)
     * @param year The year (4-digit)
     * @param hour The hour (0-23)
     * @param minute The minutes (0-59)
     * @param second The seconds (0-59)
     * @param millisecond The milliseconds (0-999)
     * 
     * @return The {@link Date} instance for the given date
     * 
     * @throws NumberFormatException if the values are invalid numbers
     */
    public Date createDate(int month, int day, int year, int hour, int minute, 
                           int second, int millisecond) {
        
        GregorianCalendar gc = new GregorianCalendar();

        gc.clear();
        gc.set(Calendar.MONTH, month-1);
        gc.set(Calendar.DAY_OF_MONTH, day);
        gc.set(Calendar.YEAR, year);
        gc.set(Calendar.HOUR_OF_DAY, hour);
        gc.set(Calendar.MINUTE, minute);
        gc.set(Calendar.SECOND, second);
        gc.set(Calendar.MILLISECOND, millisecond);
        return gc.getTime();
    }

    /**
     * Check if all values in the given data are valid and that the result will
     * be a valid date.
     * 
     * @param month The month (1-12)
     * @param day The day (1-31)
     * @param year The year (4-digit)
     * @param hour The hour (0-23)
     * @param minute The minutes (0-59)
     * @param second The seconds (0-59)
     * @param millisecond The milliseconds (0-999)
     * 
     * @return <code>true</code> if all data represents a valid date,
     *         <code>false</code> otherwise
     */
    public boolean isValidDate(String month, String day, String year, 
                               String hour, String minute, String second, 
                               String millisecond) {
        boolean valid = true;
        try {
            valid = isValidDate(Integer.parseInt(month), Integer.parseInt(day), 
                                Integer.parseInt(year), Integer.parseInt(hour), 
                                Integer.parseInt(minute), 
                                Integer.parseInt(second), 
                                Integer.parseInt(millisecond));
        }
        catch (NumberFormatException nfe) {
            valid = false;
        }

        return valid;
    }

    /**
     * Check if all values in the given data are valid and that the result will
     * be a valid date.
     * 
     * @param month The month (1-12)
     * @param day The day (1-31)
     * @param year The year (4-digit)
     * @param hour The hour (0-23)
     * @param minute The minutes (0-59)
     * @param second The seconds (0-59)
     * @param millisecond The milliseconds (0-999)
     * 
     * @return <code>true</code> if all data represents a valid date,
     *         <code>false</code> otherwise
     */
    public boolean isValidDate(int month, int day, int year, int hour, 
                               int minute, int second, int millisecond) {
        boolean valid = true;
        if ( month < 1 || month > 12 ) {
            valid = false;
        } else if ( validDay(month, day, year) == false ) {
            valid = false;
        } else if ( hour < 0 || hour > 23 ) {
            valid = false;
        } else if ( minute < 0 || minute > 59 ) {
            valid = false;
        } else if ( second < 0 || second > 59 ) {
            valid = false;
        } else if ( millisecond < 0 || millisecond > 999 ) {
            valid = false;
        }
        return valid;
    }

    private boolean validDay(int month, int day, int year) {
        boolean valid = true;
        if ( month == 1 || month == 3 || month == 5 || month ==7 || month == 8 || month == 10 || month ==12 ) {
            if ( day < 0 || day > 31 ) {
                valid = false;
            }
        } else if ( month == 4 || month == 6 || month == 9 || month == 11 ) {
            if ( day < 0 || day > 30 ) {
                valid = false;
            }
        } else if ( month == 2 ) {
            // if it's a leap year, they can have 29 days...
            if ( isLeapYear(year) ) {
                if ( day < 0 || day > 29 ) {
                    valid = false;
                }
            } else {
                if ( day < 0 || day > 28 ) {
                    valid = false;
                }
            }
        } else {
            valid = false;
        }
        return valid;
    }

    /**
     * Determine if the given year is a leap year or not.
     * 
     * @param year The year to check
     * 
     * @return <code>true</code> if the year is a leap year, <code>false</code>
     *         otherwise
     */
    public boolean isLeapYear(int year) {
        return (year % 400 == 0) || ((year % 4 == 0) && (year % 100 != 0));
    }
    
    /**
     * Create a string of the given date using the given format. If the given
     * format is invalid, then {@link IllegalArgumentException} is thrown.
     * 
     * @param date The date to create the value from
     * @param inputFormat The format to use in formatting the date
     * 
     * @return The string instance representing the date in the given format
     */
    public String dateToString(Date date, String inputFormat) {
		return FastDateFormat.getInstance(inputFormat).format(date);
    }

    /**
     * Roll a date forward or back a given number of days. Only the days are
     * modified in the associated date. If the delta is positive, then the date
     * is rolled forward the given number of days. If the delta is negative,
     * then the date is rolled backward the given number of days.
     * 
     * @param date The initial date from which to start.
     * @param delta The positive or negative integer value of days to move
     * 
     * @return The new {@link Date} instance reflecting the specified change
     */
    public Date changeDate(Date date, int delta) {
        return adjustDate(date, Calendar.DATE, delta);
    }

    /**
     * Roll a date forward or back based on the given type where type is one of
     * the following. Note that an {@link IllegalArgumentException} is thrown if
     * the given type is invalid.
     * 
     * <blockquote>
     * <pre>
     * Type     Meaning     Constant
     * ----     -------     --------
     *  y       years       {@link DateContext#YEAR}
     *  M       months      {@link DateContext#MONTH}
     *  d       days        {@link DateContext#DAY}
     *  H       hours       {@link DateContext#HOUR}
     *  m       minutes     {@link DateContext#MINUTE}
     *  s       seconds     {@link DateContext#SECOND}
     * </pre>
     * </blockquote>
     * 
     * If the delta is positive, then the given date is rolled forward the given 
     * value and type. If the delta is negative, then the date is rolled 
     * backward the given value and type.
     * 
     * @param date The initial date from which to start
     * @param type the type of change based on the above types
     * @param delta The positive or negative integer value to move
     * 
     * @return The new {@link Date} instance reflecting the specified change
     */
    public Date adjustDate(Date date, String type, int delta) {
        Date result = date;
        if (date != null) {
            if (DAY.equals(type)) {
                result = adjustDate(date, Calendar.DATE, delta);
            } else if (MONTH.equals(type)) {
                result = adjustDate(date, Calendar.MONTH, delta);
            } else if (HOUR.equals(type)) {
                result = adjustDate(date, Calendar.HOUR, delta);
            } else if (YEAR.equals(type)) {
                result = adjustDate(date, Calendar.YEAR, delta);
            } else if (MINUTE.equals(type)) {
                result = adjustDate(date, Calendar.MINUTE, delta);
            } else if (SECOND.equals(type)) {
                result = adjustDate(date, Calendar.SECOND, delta);
            } else {
                throw new IllegalArgumentException("unsupported type: " + type);
            }
        }
        
        return result;
    }
    
    private Date adjustDate(Date date, int type, int delta) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.add(type, delta);
        return cal.getTime();
    }   

    /**
     * Compare one date to another for equality in year, month and date. Note
     * that this ignores all other values including the time.  If either value
     * is <code>null</code>, including if both are <code>null</code>, then
     * <code>false</code> is returned.
     * 
     * @param date1 The first date to compare.
     * @param date2 The second date to compare.
     * 
     * @return <code>true</code> if both dates have the same year/month/date, 
     *         <code>false</code> if not.
     */
    public boolean compareDate(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        
        boolean result;
        GregorianCalendar cal1 = new GregorianCalendar();
        GregorianCalendar cal2 = new GregorianCalendar();
        cal1.setTime(date1);
        cal2.setTime(date2);
        
        if (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.DATE) == cal2.get(Calendar.DATE)) {
            result = true;
        } else {
            result = false;
        }
        
        return result;
    }   
    
    /**
     * Calcualte the age in years from today given a birthday.
     * 
     * @param birthday the birthdate to calculate from
     * 
     * @return The number of years that have passed since the birthdate or
     *         <code>-1</code> if birthday is <code>null</code>
     */
    public int calculateAge(Date birthday) {
        int result = -1;
        if (birthday != null) {
            GregorianCalendar todayCal = new GregorianCalendar();
            todayCal.setTime(new Date());
            int dayOfYear = todayCal.get(Calendar.DAY_OF_YEAR);
            int year = todayCal.get(Calendar.YEAR);
            GregorianCalendar birthdayCal = new GregorianCalendar();
            birthdayCal.setTime(birthday);
            int birthDayOfYear = birthdayCal.get(Calendar.DAY_OF_YEAR);
            int birthYear = birthdayCal.get(Calendar.YEAR);
            birthDayOfYear = 
                processLeapYear(todayCal, birthdayCal, birthDayOfYear);
            
            result = year - birthYear;
            if (dayOfYear < birthDayOfYear) {
                result--;
            }
        }
        return result;
    }
    
    private int processLeapYear(GregorianCalendar today, GregorianCalendar birthdayCal, int value) {    
        int result = value;
        boolean isLeapYear = today.isLeapYear(today.get(Calendar.YEAR));
        boolean isFebruary = (birthdayCal.get(Calendar.MONTH) == Calendar.FEBRUARY);
        boolean isLeapDay = (birthdayCal.get(Calendar.DAY_OF_MONTH) == 29);
        if (isFebruary && (!isLeapYear) && isLeapDay) {
            result--;
        }
        return result;
    }
    
    /**
     * Get the day of the year for the given date. The day of the year is
     * between 1-365 (366 for leap years). Note that if the given date is 
     * <code>null</code>, then a {@link NullPointerException} is thrown.
     * 
     * @param date The date to get the day of the year from
     * 
     * @return The day within the year for the given date
     */
	public int dayOfYear(Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_YEAR);
    }
}
