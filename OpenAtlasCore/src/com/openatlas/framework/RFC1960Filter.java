/**
 * OpenAtlasForAndroid Project
 * The MIT License (MIT) Copyright (OpenAtlasForAndroid) 2015 Bunny Blue,achellies
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author BunnyBlue
 **/
package com.openatlas.framework;


import org.osgi.framework.Filter;
import org.osgi.framework.GetUserInfoRequest;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

final class RFC1960Filter implements Filter {
    private static final int AND_OPERATOR = 1;
    private static final int APPROX = 2;
    private static final int EQUALS = 0;
    private static final int GREATER = 3;
    private static final int LESS = 4;
    private static final int NOT_OPERATOR = 3;
    private static final Filter NULL_FILTER;
    private static final String[] OP;
    private static final int OR_OPERATOR = 2;
    private static final int PRESENT = 1;
    private static final Class<?>[] STRINGCLASS;
    private List operands;
    private int operator;

    private static final class RFC1960SimpleFilter implements Filter {
        private final int comparator;
        private final String id;
        private final String value;

        private RFC1960SimpleFilter(String id, int comparator, String value) {
            this.id = id;
            this.comparator = comparator;
            this.value = value;
        }

        @Override
        public boolean match(ServiceReference serviceReference) {
            try {
                return match(((ServiceReferenceImpl) serviceReference).properties);
            } catch (Exception e) {
                Dictionary<String, Object> hashtable = new Hashtable<String, Object>();
                String[] propertyKeys = serviceReference.getPropertyKeys();
                for (int i = EQUALS; i < propertyKeys.length; i++) {
                    hashtable.put(propertyKeys[i],
                            serviceReference.getProperty(propertyKeys[i]));
                }
                return match(hashtable);
            }
        }

        @Override
        public boolean match(Dictionary dictionary) {
            Object obj;
            Object obj2 = dictionary.get(this.id);
            if (obj2 == null) {
                obj2 = dictionary.get(this.id.toLowerCase());
            }
            if (obj2 == null) {
                Enumeration keys = dictionary.keys();
                while (keys.hasMoreElements()) {
                    String str = (String) keys.nextElement();
                    if (str.equalsIgnoreCase(this.id)) {
                        obj = dictionary.get(str);
                        break;
                    }
                }
            }
            obj = obj2;
            if (obj == null) {
                return false;
            }
            if (this.comparator == 1) {
                return true;
            }
            try {
                if (obj instanceof String) {
                    return compareString(this.value, this.comparator,
                            (String) obj);
                }
                if (obj instanceof Number) {
                    return compareNumber(this.value, this.comparator,
                            (Number) obj);
                }
                if (obj instanceof String[]) {
                    String[] strArr = (String[]) obj;
                    if (strArr.length == 0) {
                        return false;
                    }
                    String stripWhitespaces = this.comparator == 2 ? stripWhitespaces(this.value)
                            : this.value;
                    for (int i = 0; i < strArr.length; i++) {
                        if (compareString(stripWhitespaces, this.comparator,
                                strArr[i])) {
                            return true;
                        }
                    }
                    return false;
                } else if (obj instanceof Boolean) {
                    boolean z = true;
                    if ((this.comparator == 0 || this.comparator == 2)
                            && obj.equals(Boolean
                            .valueOf(this.value))) {
                        int i2 = 1;
                    } else {
                        z = false;
                    }
                    return z;
                } else if (obj instanceof Character) {
                    return this.value.length() == 1 ? compareTyped(
                            new Character(this.value.charAt(EQUALS)),
                            this.comparator, (Character) obj) : false;
                } else {
                    if (obj instanceof Vector) {
                        Vector vector = (Vector) obj;
                        Object[] objArr = new Object[vector.size()];
                        vector.copyInto(objArr);
                        return compareArray(this.value, this.comparator, objArr);
                    } else if (obj instanceof Object[]) {
                        return compareArray(this.value, this.comparator,
                                (Object[]) obj);
                    } else {
                        if (!obj.getClass().isArray()) {
                            return obj instanceof Comparable ? compareReflective(
                                    this.value, this.comparator,
                                    (Comparable) obj) : false;
                        } else {
                            int i3 = 0;
                            while (i3 < Array.getLength(obj)) {
                                Object obj3 = Array.get(obj, i3);
                                if (!(obj3 instanceof Number)
                                        || !compareNumber(this.value,
                                        this.comparator, (Number) obj3)) {
                                    if ((obj3 instanceof Comparable)
                                            && compareReflective(this.value,
                                            this.comparator,
                                            (Comparable) obj3)) {
                                    }
                                    i3++;
                                }
                                return true;
                            }
                            return false;
                        }
                    }
                }
            } catch (Throwable th) {
                return false;
            }
        }

        private static boolean compareString(String str, int i, String str2) {
            if (i == 2) {
                str = stripWhitespaces(str).toLowerCase();
            }
            if (i == 2) {
                str2 = stripWhitespaces(str2).toLowerCase();
            }
            switch (i) {
                case EQUALS:
                case OR_OPERATOR:
                    return stringCompare(str.toCharArray(), EQUALS,
                            str2.toCharArray(), EQUALS) == 0;
                case NOT_OPERATOR:
                    return stringCompare(str.toCharArray(), EQUALS,
                            str2.toCharArray(), EQUALS) <= 0;
                case LESS:
                    return stringCompare(str.toCharArray(), EQUALS,
                            str2.toCharArray(), EQUALS) >= 0;
                default:
                    throw new IllegalStateException("Found illegal comparator.");
            }
        }

        private static boolean compareNumber(String id, int comparator, Number number) {
            if (number instanceof Integer) {
                int intValue = number.intValue();
                int parseInt = Integer.parseInt(id);
                switch (comparator) {
                    case NOT_OPERATOR:
                        return intValue >= parseInt;
                    case LESS:
                        return intValue <= parseInt;
                    default:
                        return intValue == parseInt;
                }
            } else if (number instanceof Long) {
                long longValue = number.longValue();
                long parseLong = Long.parseLong(id);
                switch (comparator) {
                    case NOT_OPERATOR:
                        return longValue >= parseLong;
                    case LESS:
                        return longValue <= parseLong;
                    default:
                        return longValue == parseLong;
                }
            } else if (number instanceof Short) {
                short shortValue = number.shortValue();
                short parseShort = Short.parseShort(id);
                switch (comparator) {
                    case NOT_OPERATOR:
                        return shortValue >= parseShort;
                    case LESS:
                        return shortValue <= parseShort;
                    default:
                        return shortValue == parseShort;
                }
            } else if (number instanceof Double) {
                double doubleValue = number.doubleValue();
                double parseDouble = Double.parseDouble(id);
                switch (comparator) {
                    case NOT_OPERATOR:
                        return doubleValue >= parseDouble;
                    case LESS:
                        return doubleValue <= parseDouble;
                    default:
                        return doubleValue == parseDouble;
                }
            } else if (number instanceof Float) {
                float floatValue = number.floatValue();
                float parseFloat = Float.parseFloat(id);
                switch (comparator) {
                    case NOT_OPERATOR:
                        return floatValue >= parseFloat;
                    case LESS:
                        return floatValue <= parseFloat;
                    default:
                        return floatValue == parseFloat;
                }
            } else {
                if (number instanceof Byte) {
                    try {
                        return compareTyped(Byte.decode(id), comparator, (Byte) number);
                    } catch (Throwable th) {
                    }
                }
                return compareReflective(id, comparator, (Comparable) number);
            }
        }

        private static boolean compareTyped(Object obj, int comparator,
                                            Comparable comparable) {
            switch (comparator) {
                case EQUALS:
                case OR_OPERATOR:
                    return comparable.equals(obj);
                case NOT_OPERATOR:
                    return comparable.compareTo(obj) >= 0;
                case LESS:
                    return comparable.compareTo(obj) <= 0;
                default:
                    throw new IllegalStateException("Found illegal comparator.");
            }
        }

        private static boolean compareArray(String str, int comparator, Object[] objArr) {
            for (int i2 = 0; i2 < objArr.length; i2++) {
                Object obj = objArr[i2];
                if (obj instanceof String) {
                    if (compareString(str, comparator, (String) obj)) {
                        return true;
                    }
                } else if (obj instanceof Number) {
                    if (compareNumber(str, comparator, (Number) obj)) {
                        return true;
                    }
                } else if ((obj instanceof Comparable)
                        && compareReflective(str, comparator, (Comparable) obj)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean compareReflective(String str, int comparator,
                                                 Comparable comparable) {
            try {
                return compareTyped(
                        comparable.getClass().getConstructor(STRINGCLASS)
                                .newInstance(str), comparator,
                        comparable);
            } catch (Exception e) {
                return false;
            }
        }

        private static String stripWhitespaces(String str) {
            return str.replace(' ', '\u0000');
        }

        private static int stringCompare(char[] cArr, int comparator, char[] cArr2,
                                         int i2) {
            if (comparator == cArr.length) {
                return 0;
            }
            int length = cArr.length;
            int length2 = cArr2.length;
            int i3 = i2;
            while (comparator < length && i3 < length2) {
                if (cArr[comparator] == cArr2[i3]) {
                    comparator++;
                    i3++;
                } else {
                    if (cArr[comparator] > 'A' && cArr[comparator] < 'Z') {
                        cArr[comparator] = (char) (cArr[comparator] + 32);
                    }
                    if (cArr2[i3] > 'A' && cArr2[i3] < 'Z') {
                        cArr2[i3] = (char) (cArr2[i3] + 32);
                    }
                    if (cArr[comparator] == cArr2[i3]) {
                        comparator++;
                        i3++;
                    } else if (cArr[comparator] == '*') {
                        length = comparator + 1;
                        while (stringCompare(cArr, length, cArr2, i3) != 0) {
                            i3++;
                            if (length2 - i3 <= -1) {
                                return PRESENT;
                            }
                        }
                        return 0;
                    } else if (cArr[comparator] < cArr2[i3]) {
                        return -1;
                    } else {
                        if (cArr[comparator] > cArr2[i3]) {
                            return PRESENT;
                        }
                    }
                }
            }
            if (comparator == length && i3 == length2 && cArr[comparator - 1] == cArr2[i3 - 1]) {
                return 0;
            }
            if (cArr[comparator - 1] == '*' && comparator == length && i3 == length2) {
                return 0;
            }
            if (length < length2) {
                i3 = length;
            } else {
                i3 = length2;
            }
            if (length == i3) {
                i3 = -1;
            } else {
                i3 = 1;
            }
            return i3;
        }

        @Override
        public String toString() {
            return "(" + this.id + OP[this.comparator]
                    + (this.value == null ? "" : this.value) + ")";
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RFC1960SimpleFilter)) {
                return false;
            }
            RFC1960SimpleFilter rFC1960SimpleFilter = (RFC1960SimpleFilter) obj;
            return this.comparator == rFC1960SimpleFilter.comparator
                    && this.id.equals(rFC1960SimpleFilter.id)
                    && this.value.equals(rFC1960SimpleFilter.value);
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }


    }

    static {
        OP = new String[]{"=", "=*", "~=", ">=", "<="};
        STRINGCLASS = new Class[]{String.class};
        NULL_FILTER = new Filter() {
            @Override
            public boolean match(ServiceReference serviceReference) {
                return true;
            }

            @Override
            public boolean match(Dictionary dictionary) {
                return true;
            }


        };
    }

    private RFC1960Filter(int i) {
        this.operands = new ArrayList(1);
        this.operator = i;
    }

    static Filter fromString(String str) throws InvalidSyntaxException {
        if (str == null) {
            return NULL_FILTER;
        }
        Stack stack = new Stack();
        try {
            int length = str.length();
            int i = -1;
            int i2 = EQUALS;
            String str2 = null;
            int i3 = -1;
            char[] toCharArray = str.toCharArray();
            stack.clear();
            int i4 = EQUALS;
            while (i4 < toCharArray.length) {
                int i5;
                int i6;
                String str3;
                int i7;
                String trim;
                Object obj;
                switch (toCharArray[i4]) {
                    case 40:
                        char c = toCharArray[i4 + 1];
                        i5 = i4;
                        while (Character.isWhitespace(c)) {
                            i4 = i5 + 1;
                            c = toCharArray[i4 + 1];
                            i5 = i4;
                        }
                        if (c == '&') {
                            stack.push(new RFC1960Filter(1));
                            i6 = i3;
                            str3 = str2;
                            i3 = i2;
                            i7 = i;
                        } else if (c == '|') {
                            stack.push(new RFC1960Filter(2));
                            i6 = i3;
                            str3 = str2;
                            i3 = i2;
                            i7 = i;
                        } else if (c == '!') {
                            stack.push(new RFC1960Filter(3));
                            i6 = i3;
                            str3 = str2;
                            i3 = i2;
                            i7 = i;
                        } else if (i == -1) {
                            i6 = i3;
                            str3 = str2;
                            i3 = i2;
                            i7 = i5;
                        } else {
                            throw new InvalidSyntaxException(
                                    "Surplus left paranthesis at: "
                                            + str.substring(i5), str);
                        }
                    case 41:
                        RFC1960Filter rFC1960Filter;
                        if (i == -1) {
                            rFC1960Filter = (RFC1960Filter) stack.pop();
                            if (stack.isEmpty()) {
                                return rFC1960Filter;
                            }
                            RFC1960Filter rFC1960Filter2 = (RFC1960Filter) stack
                                    .peek();
                            if (rFC1960Filter2.operator != 3
                                    || rFC1960Filter2.operands.isEmpty()) {
                                rFC1960Filter2.operands.add(rFC1960Filter);
                                if (i4 == length - 1) {
                                    throw new InvalidSyntaxException(
                                            "Missing right paranthesis at the end.",
                                            str);
                                }
                                i5 = i4;
                                i6 = i3;
                                str3 = str2;
                                i3 = i2;
                                i7 = i;
                            } else {
                                throw new InvalidSyntaxException(
                                        "Unexpected literal: " + str.substring(i4),
                                        str);
                            }
                        } else if (i2 == 0) {
                            throw new InvalidSyntaxException("Missing operator.",
                                    str);
                        } else if (!stack.isEmpty()) {

                            rFC1960Filter = (RFC1960Filter) stack.peek();
                            String substring = str.substring(i2 + 1, i4);
                            if (substring.equals(GetUserInfoRequest.version)
                                    && i3 == 0) {
                                i3 = PRESENT;
                                substring = null;
                            }
                            rFC1960Filter.operands.add(new RFC1960SimpleFilter(
                                    null, i3, substring));
                            Object obj2 = null;
                            Object obj3 = -1;
                            int i8 = i4;
                            Object obj4 = null;
                            i6 = -1;
                            i5 = i8;
                        } else if (i4 == length - 1) {
                            String substring = str.substring(i2 + 1, length - 1);
                            if (substring.equals(GetUserInfoRequest.version)
                                    && i3 == 0) {
                                i3 = PRESENT;
                                substring = null;
                            } else {
                                substring = substring;
                            }
                            return new RFC1960SimpleFilter(null, i3, substring);
                        } else {
                            throw new InvalidSyntaxException("Unexpected literal: "
                                    + str.substring(i4), str);
                        }
                    case 60:
                        if (i2 == 0 && toCharArray[i4 + 1] == '=') {
                            trim = str.substring(i + 1, i4).trim();
                            obj = LESS;
                            i5 = i4 + 1;
                            str3 = trim;
                            i7 = i;
                            i3 = i5;
                        }
                        throw new InvalidSyntaxException("Unexpected character "
                                + toCharArray[i4 + 1], str);
                    case 61:
                        i3 = i4;
                        i7 = i;
                        obj = null;
                        i5 = i4;
                        str3 = str.substring(i + 1, i4).trim();
                        break;
                    case 62:
                        if (i2 == 0 && toCharArray[i4 + 1] == '=') {
                            trim = str.substring(i + 1, i4).trim();
                            obj = NOT_OPERATOR;
                            i5 = i4 + 1;
                            str3 = trim;
                            i7 = i;
                            i3 = i5;
                        }
                        throw new InvalidSyntaxException("Unexpected character "
                                + toCharArray[i4 + 1], str);
                    case 126:
                        if (i2 == 0 && toCharArray[i4 + 1] == '=') {
                            trim = str.substring(i + 1, i4).trim();
                            obj = OR_OPERATOR;
                            i5 = i4 + 1;
                            str3 = trim;
                            i7 = i;
                            i3 = i5;
                        }
                        throw new InvalidSyntaxException("Unexpected character "
                                + toCharArray[i4 + 1], str);
                    default:
                        i5 = i4;
                        i6 = i3;
                        str3 = str2;
                        i3 = i2;
                        i7 = i;
                        break;
                }
                i2 = i3;
                i = i7;
                str2 = str3;
                // i3 = i6;
                i4 = i5 + 1;
            }
            return (RFC1960Filter) stack.pop();
        } catch (EmptyStackException e) {
            throw new InvalidSyntaxException(
                    "Filter expression not well-formed.", str);
        }
    }


    @Override
    public String toString() {
        int i = EQUALS;
        if (this.operator == 3) {
            return "(!" + this.operands.get(EQUALS) + ")";
        }
        StringBuffer stringBuffer = new StringBuffer(this.operator == 1 ? "(&"
                : "(|");
        Filter[] filterArr = (Filter[]) this.operands
                .toArray(new Filter[this.operands.size()]);
        while (i < filterArr.length) {
            stringBuffer.append(filterArr[i]);
            i++;
        }
        stringBuffer.append(")");
        return stringBuffer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RFC1960Filter)) {
            return false;
        }
        RFC1960Filter rFC1960Filter = (RFC1960Filter) obj;
        if (this.operands.size() != rFC1960Filter.operands.size()) {
            return false;
        }
        Filter[] filterArr = (Filter[]) this.operands
                .toArray(new Filter[this.operands.size()]);
        Filter[] filterArr2 = (Filter[]) rFC1960Filter.operands
                .toArray(new Filter[this.operands.size()]);
        for (int i = 0; i < filterArr.length; i++) {
            if (!filterArr[i].equals(filterArr2[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean match(ServiceReference reference) {
        try {
            return match(((ServiceReferenceImpl) reference).properties);
        } catch (Exception e) {
            Dictionary<String, Object> hashtable = new Hashtable<String, Object>();
            String[] propertyKeys = reference.getPropertyKeys();
            for (int i = EQUALS; i < propertyKeys.length; i++) {
                hashtable.put(propertyKeys[i],
                        reference.getProperty(propertyKeys[i]));
            }
            return match(hashtable);
        }
    }

    @Override
    public boolean match(Dictionary dictionary) {
        Filter[] filterArr;
        int i;
        if (this.operator == 1) {
            filterArr = (Filter[]) this.operands
                    .toArray(new Filter[this.operands.size()]);
            for (i = 0; i < filterArr.length; i++) {
                if (!filterArr[i].match(dictionary)) {
                    return false;
                }
            }
            return true;
        } else if (this.operator == 2) {
            filterArr = (Filter[]) this.operands
                    .toArray(new Filter[this.operands.size()]);
            for (i = 0; i < filterArr.length; i++) {
                if (filterArr[i].match(dictionary)) {
                    return true;
                }
            }
            return false;
        } else if (this.operator == 3) {
            return !((Filter) this.operands.get(EQUALS)).match(dictionary);
        } else {
            throw new IllegalStateException("PARSER ERROR");
        }
    }

}
