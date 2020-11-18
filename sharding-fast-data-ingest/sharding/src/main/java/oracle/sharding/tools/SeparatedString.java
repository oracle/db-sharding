/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.tools;

import java.nio.CharBuffer;

/**
 * Represents a char array, annotated by delimiter indices.
 */
public class SeparatedString {
    final private int []  tabs;
    final private char [] data;

    public static int[] splitArray(String s, char x, int limit) {
        ++limit;

        if (limit < 1) {
            limit = (int) s.chars().filter(c -> c == x).count();
        }

        int [] tabpos = new int[limit];
        int j = 0, pos = 0;

        tabpos[j] = -1;

        while (pos != -1 && j < (limit - 1)) {
            pos = tabpos[j+1] = s.indexOf(x, tabpos[j]+1);
            ++j;
        }

        tabpos[0] = -1;
        tabpos[j] = s.length();
        
        // deal with extra delimiter at the end of the line 
        if (s.charAt(s.length()-1)==x) tabpos[j] -= 1; 

        return tabpos;
    }

    public static String subString(int i, String s, int[] tabs)
    {
        return s.substring(tabs[i] + 1, tabs[i + 1]);
    }

    public String part(int i)
    {
        return new String(data, tabs[i] + 1, tabs[i + 1] - tabs[i] - 1);
    }

    public String part(int i, String s)
    {
        return s.substring(tabs[i] + 1, tabs[i + 1]);
    }

    public SeparatedString(String s) {
        this(s, '\t', -1);
    }

    public SeparatedString(String s, char separator) {
        this(s, separator, -1);
    }

    public SeparatedString(String s, char separator, int limit) {
        this.tabs = splitArray(s, separator, limit);
        this.data = s.toCharArray();
    }

    @Override
    public String toString() {
        return new String(data);
    }

    public int getOffset(int i) {
        return tabs[i] + 1;
    }

    public int getLength(int i) {
        return tabs[i + 1] - tabs[i] - 1;
    }

    public CharBuffer toCharSequence() {
        return CharBuffer.wrap(data);
    }
}
