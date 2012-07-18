package org.teatrove.tea.runtime;

@Deprecated
public interface UtilityContext extends Context {

    @Deprecated java.util.Date currentDate();
    
    @Deprecated org.joda.time.DateTime currentDateTime();
    
    @Deprecated boolean startsWith(java.lang.String a, java.lang.String b);
    
    @Deprecated boolean endsWith(java.lang.String a, java.lang.String b);
    
    @Deprecated int[] find(java.lang.String a, java.lang.String b);
    
    @Deprecated int[] find(java.lang.String a, java.lang.String b, int c);
    
    @Deprecated int findFirst(java.lang.String a, java.lang.String b);

    @Deprecated int findFirst(java.lang.String a, java.lang.String b, int c);

    @Deprecated int findLast(java.lang.String[] a, java.lang.String b);
    
    @Deprecated int findLast(java.lang.String[] a, java.lang.String b, int c);
    
    @Deprecated java.lang.String substring(java.lang.String a, int b);
    
    @Deprecated java.lang.String substring(java.lang.String a, int b, int c);
    
    @Deprecated java.lang.String toLowerCase(java.lang.String a);
    
    @Deprecated java.lang.String toUpperCase(java.lang.String b);
    
    @Deprecated java.lang.String trim(java.lang.String a);
    
    @Deprecated java.lang.String trimLeading(java.lang.String b);
    
    @Deprecated java.lang.String trimTrailing(java.lang.String a);
    
    @Deprecated java.lang.String replace(java.lang.String a, java.lang.String b, java.lang.String c);
    
    @Deprecated java.lang.String replace(java.lang.String a, java.lang.String b, java.lang.String c, int d);
    
    @Deprecated java.lang.String replace(java.lang.String a, java.util.Map b);
    
    @Deprecated java.lang.String replaceFirst(java.lang.String a, java.lang.String b, java.lang.String c);
    
    @Deprecated java.lang.String replaceFirst(java.lang.String a, java.lang.String b, java.lang.String c, int d);
    
    @Deprecated java.lang.String replaceLast(java.lang.String a, java.lang.String b, java.lang.String c);
    
    @Deprecated java.lang.String replaceLast(java.lang.String a, java.lang.String b, java.lang.String c, int d);
    
    @Deprecated java.lang.String shortOrdinal(java.lang.Long a);

    @Deprecated java.lang.String shortOrdinal(long a);

    @Deprecated java.lang.String ordinal(java.lang.Long a);
    
    @Deprecated java.lang.String ordinal(long a);
    
    @Deprecated java.lang.String cardinal(java.lang.Long a);
    
    @Deprecated java.lang.String cardinal(long a);
    
    @Deprecated boolean isArray(java.lang.Object a);
    
    @Deprecated void sort(java.lang.Object[] a, java.lang.String b, boolean c);
    
    @Deprecated void sort(java.lang.Object[] a, java.lang.String[] b, boolean[] c);
    
    @Deprecated void sort(java.lang.String[] a, boolean b, boolean c);
    
    @Deprecated void sort(java.lang.Object[] a, boolean c);
    
    @Deprecated void sortAscending(java.lang.Object[] a);
    
    @Deprecated void sortAscending(int[] a);
    
    @Deprecated void sortAscending(double[] a);
    
    @Deprecated void sortAscending(float[] a);
    
    @Deprecated void sortAscending(byte[] a);
    
    @Deprecated void sortAscending(short[] a);

    @Deprecated void sortAscending(long[] a);
    
    @Deprecated java.lang.String[] split(java.lang.String a, java.lang.String b);
    
    @Deprecated java.lang.StringBuilder createStringBuilder();
    
    @Deprecated java.lang.StringBuilder createStringBuilder(int a);
    
    @Deprecated void append(java.lang.StringBuilder a, java.lang.Object b);
    
    @Deprecated void prepend(java.lang.StringBuilder a, java.lang.Object b);
    
    @Deprecated void insert(java.lang.StringBuilder a, java.lang.Object b, int c);
    
    @Deprecated java.lang.String toString(java.lang.StringBuilder a);
}
