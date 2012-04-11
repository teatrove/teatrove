package org.teatrove.teaapps.contexts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper context that provides the ability to manipulate and trim strings based
 * on HTML tags. This is a useful context in generating stories or short text
 * snippets that may contain HTML to help adjust the pagination, truncation,
 * etc.
 */
public class HTMLContext {

    protected static final Pattern HTML_TAGS =
        Pattern.compile("<(.|\n)+?>", Pattern.CASE_INSENSITIVE);

    /**
     * Get the character count of the given text excluding any HTML tags. This 
     * will first strip any HTML tags from the given string and then return the
     * number of characters in the result. For example, the input string
     * <code>&lt;em&gt;Hello&lt;/em&gt; World</code> would return 11 characters
     * as the <code>em</code> tags would be stripped.
     * 
     * @param text The text to evaluate
     * 
     * @return The number of characters in the string excluding HTML tags
     */
    public int getCharCount(String text) {
        String result = text.trim();
        Matcher matcher = HTML_TAGS.matcher(result);
        result = matcher.replaceAll("");
        return result.length();
    }

    /**
     * Paginate the given text input so that each page contains no more than the
     * given maximum number of characters. The length of each page is based on
     * the text only ignoring any HTML tags.
     * 
     * @param text The text to paginate against
     * @param maxChars The maximum characters per page
     * @param splitValue The split characters for splitting on page breaks
     * @param HTMLBalanceTags Set of tags to ensure are opened/closed properly
     * 
     * @return The array of strings per page
     * 
     * @see #getCharCount(String)
     */
    public String[] getPagination(String text, int maxChars, String splitValue,
                                  String[] HTMLBalanceTags) {
        int count;
        int lastSplitCount;
        String tempHolder = " ";
        List<String> list = new ArrayList<String>();
        boolean secondRunThrough = false;

        // if the story will not be broken up because it's too small, 
        // just return it without doing all the logic.
        if ((getCharCount(text) <= maxChars)) {
            list.add(text);
            return list.toArray(new String[list.size()]);
        }

        //JVM 1.4 version
        //String[] splits = text.split(splitValue);
        //Will need an HTMLBalance function if implemented.

        //JVM 1.3 version
        String[] splits = split(text, splitValue, HTMLBalanceTags);

        int len = splits.length;
        for (int i = 0; i < len;i++) {
            //Check to see if last page would not be at least half filled. 
            // If so, add to previous page.
            if (i == len-2){
                lastSplitCount = getCharCount(splits[i+1]);
                if (lastSplitCount <= maxChars/2) {
                    splits[i] = splits[i]+" "+splits[i+1];
                    list.add(tempHolder+splits[i]);
                    return list.toArray(new String[list.size()]);
                }
            }
            
            if (secondRunThrough){
                count = getCharCount(tempHolder+splits[i]);
            } else{
                count = getCharCount(splits[i]);
            }
            
            if (count >= maxChars || i+1 == len) {
                list.add(tempHolder+splits[i]);
                secondRunThrough = false;
                tempHolder = " ";
            }
            else{
                tempHolder = tempHolder+splits[i];
                secondRunThrough = true;
            }
        }
        
        return list.toArray(new String[list.size()]);
    }

    /**
     * Split the given string around the given split value.  This will also 
     * ensure any of the given HTML tags are properly closed.
     * 
     * @param text The text value to split
     * @param splitValue The values to be split on
     * @param HTMLBalanceTags The HTML tags to ensure are closed properly
     * 
     * @return The array of string values per the splitting
     */
    public String[] split(String text, String splitValue, 
                          String[] HTMLBalanceTags) {
        
        String restOfText = text;
        int nextBreakIndex = 0;
        List<String> list = new ArrayList<String>();
        int splitValueLength = splitValue.length();

        while (restOfText.length() > 0) {
            nextBreakIndex = restOfText.indexOf(splitValue);
            if (nextBreakIndex < 0) {
                list.add(restOfText);
                break;
            }
            
            list.add(restOfText.substring(0, nextBreakIndex+splitValueLength));
            restOfText = restOfText.substring(nextBreakIndex+splitValueLength);
        }
        
        // This code makes sure that no ending HTML </> tags are left out 
        // causing malfomed pages.
        if (HTMLBalanceTags.length > 0) {
            List<String> balancedList = new ArrayList<String>();
            for (int t = 0; t < HTMLBalanceTags.length; t++) {
                // first tag pass through
                if (t < 1) {
                    balancedList = getBalancedList(HTMLBalanceTags[t], list);
                } 
                else if (balancedList.size() > 1) {
                    // after the first pass through keep trying on each
                    // subsequent tag.
                    balancedList = 
                        getBalancedList(HTMLBalanceTags[t], balancedList);
                }
            }
            
            return balancedList.toArray(new String[balancedList.size()]);
        }
        else {
            return list.toArray(new String[list.size()]);
        }
    }

    protected List<String> getBalancedList(String tag, List<String> list) {
        int e = -1;
        int len = list.size();
        List<String> balancedList = new ArrayList<String>();
        for (int i = 0; i < len; i++) {
            String temp = list.get(i);
            //see if the element has the begin tag starting from right to left.
            if ((e = temp.lastIndexOf(tag)) > -1) {
                //If so, check to see if there is a matching end tag starting 
                //at the index above.
                String endTag = "</" + tag.substring(1, tag.length()) + ">";
                if (temp.indexOf(endTag, e) < 0) {
                    //if no matching end tag, keep looping through the 
                    //arrayElement and add it.
                    int ii = 1;
                    while (temp.indexOf(endTag) < 0) {
                        temp = temp+" "+list.get(i+1);
                        list.remove(1);
                        len = len-ii;
                        ++ii;
                    }
                    
                    balancedList.add(temp);
                }
                else {
                    balancedList.add(temp);
                }
            }
            else {
                if (!temp.equals("</p><p>")) {
                    balancedList.add(temp);
                }
            }
        }
        
        return balancedList;
    }

}
