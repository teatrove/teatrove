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

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;

import org.teatrove.trove.util.FastDateFormat;

public class DateContext {

    private static SimpleDateFormat cFormatter = new SimpleDateFormat("yyyyMMdd");
    private static SimpleDateFormat cDayOfYearFormatter = new SimpleDateFormat("D");
    
    private static final String DAY = "d";
    private static final String HOUR = "H";
    private static final String MINUTE = "m";
    private static final String SECOND = "s";
    private static final String MONTH = "M";
    private static final String YEAR = "y";
    
    /**
     * Create a date object (default input format: "YYYYMMDD")
     * @param dateString String from which to create a date.
     * @return Date object representing date string.
     */
    public Date createDate(String dateString) throws ParseException {
        Date result = null;     
        synchronized (cFormatter) {
            result = cFormatter.parse(dateString);
        }
        return result;
    }

    /**
     * Creates a date object from a long. (millis since 1970...)
     * @param date long the millis since 1970....
     * @return Date object representing the date.
     */
    public Date createDate(long date) {
        Date result = null;     
        result = new Date(date);
        return result;
    }
    
    /**
     * Create a date object
     * @param dateString String from which to create a date.
     * @param inputFormat Definition of dateString.
     * @return Date object representing date string.
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
     */
    public Date createDate(String dateString, String inputFormat) 
        throws ParseException 
    {
        SimpleDateFormat sdf = new SimpleDateFormat(inputFormat);
        return sdf.parse(dateString);
    }

    public Date createDate(String month, String day, String year, String hour, String minute, String second, String millisecond) 
    	throws NumberFormatException 
    {
        Date date = null;
        date = createDate(Integer.parseInt(month), Integer.parseInt(day), Integer.parseInt(year),
                          Integer.parseInt(hour), Integer.parseInt(minute), Integer.parseInt(second), Integer.parseInt(millisecond));
        return date;
    }

    public Date createDate(int month, int day, int year, int hour, int minute, int second, int millisecond) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(Calendar.MONTH, month-1);
        gc.set(Calendar.DAY_OF_MONTH, day);
        gc.set(Calendar.YEAR, year);
        gc.set(Calendar.HOUR_OF_DAY, hour);
        gc.set(Calendar.MINUTE, minute);
        gc.set(Calendar.SECOND, second);
        gc.set(Calendar.MILLISECOND, millisecond);
        return gc.getTime();
    }

    public boolean isValidDate(String month, String day, String year, String hour, String minute, String second, String millisecond) {
        boolean valid = true;
        try {
            valid = isValidDate(Integer.parseInt(month), Integer.parseInt(day), Integer.parseInt(year),
                                Integer.parseInt(hour), Integer.parseInt(minute), Integer.parseInt(second), Integer.parseInt(millisecond));
        }
        catch (NumberFormatException nfe) {
            valid = false;
        }

        return valid;
    }

    public boolean isValidDate(int month, int day, int year, int hour, int minute, int second, int millisecond) {
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
            if ( year % 4 == 0 ) {
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
     * Create a String object
     * @param date from which to create a String representation.
     * @param inputFormat Definition of dateString.
     * @return String object representing date.
     */
    public String dateToString(Date date, String inputFormat) {
		return FastDateFormat.getInstance(inputFormat).format(date);
    }

    /**
     * Roll a date forward or back a given number of days
     * @param date Date from which to start.
     * @param delta Positive or negative integer value to move.
     * @return Date object reflecting specified change.
     */
    public Date changeDate(Date date, int delta) {
        return adjustDate(date, Calendar.DATE, delta);
    }

    /**
     * Roll a date forwared or back based on day (d), years (y),
     * hours(H), minutes (m), seconds (s) or months (M).
     * @param date the date from which to start
     * @param type the type of change d for Day, M for month, y for Year
     * h for hour, m for minute, s for seconds
     * @param delta positive or negitive integer value to move
     * @return Date object reflecting the specific change
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
     * Compare one date to another for equality in year, month and date.
     * @param date1 One date to compare.
     * @param date2 The second date to compare.
     * @return True if dates are same year/month/date, false if not.
     */
    public boolean compareDate(Date date1, Date date2) {
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
     * Returns the age in years from today given a birthday.
     * @param birthday the birthdate
     * @return int the number of years that have passed 
     *                since the birthdate. (-1 if birthday is null)
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
            birthDayOfYear = processLeapYear(todayCal, birthdayCal, birthDayOfYear);
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
    
	public int dayOfYear(Date date) throws ParseException {
		return Integer.parseInt(cDayOfYearFormatter.format(date));

    }
}
