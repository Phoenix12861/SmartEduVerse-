package modules.utility.numbersystem;

import java.util.TreeMap;

public class NumberSystemEngine {

    // ================= CONVERSIONS =================

    public String decimalToBinary(long num) {
        return Long.toBinaryString(num);
    }

    public String decimalToOctal(long num) {
        return Long.toOctalString(num);
    }

    public String decimalToHex(long num) {
        return Long.toHexString(num).toUpperCase();
    }

    // ================= WORDS CONVERSION =================

    private static final String[] units = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] tens = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public String convertToWords(long num) {
        if (num == 0) return "Zero";
        if (num < 0) return "Minus " + convertToWords(Math.abs(num));

        String words = "";

        if ((num / 1000000000) > 0) {
            words += convertToWords(num / 1000000000) + " Billion ";
            num %= 1000000000;
        }

        if ((num / 1000000) > 0) {
            words += convertToWords(num / 1000000) + " Million ";
            num %= 1000000;
        }

        if ((num / 1000) > 0) {
            words += convertToWords(num / 1000) + " Thousand ";
            num %= 1000;
        }

        if ((num / 100) > 0) {
            words += convertToWords(num / 100) + " Hundred ";
            num %= 100;
        }

        if (num > 0) {
            if (!words.equals("")) words += "and ";

            if (num < 20) {
                words += units[(int)num];
            } else {
                words += tens[(int)(num / 10)];
                if ((num % 10) > 0) words += "-" + units[(int)(num % 10)];
            }
        }

        return words.trim();
    }

    // ================= ROMAN NUMERALS =================

    private static final TreeMap<Long, String> romanMap = new TreeMap<>();
    static {
        romanMap.put(1000L, "M");
        romanMap.put(900L, "CM");
        romanMap.put(500L, "D");
        romanMap.put(400L, "CD");
        romanMap.put(100L, "C");
        romanMap.put(90L, "XC");
        romanMap.put(50L, "L");
        romanMap.put(40L, "XL");
        romanMap.put(10L, "X");
        romanMap.put(9L, "IX");
        romanMap.put(5L, "V");
        romanMap.put(4L, "IV");
        romanMap.put(1L, "I");
    }

    public String toRoman(long num) {
        if (num <= 0 || num > 3999) return "N/A (Limit 1-3999)";
        long l = romanMap.floorKey(num);
        if (num == l) return romanMap.get(num);
        return romanMap.get(l) + toRoman(num - l);
    }

    // ================= BITWISE =================

    public String andOperation(long a, long b) {
        return Long.toBinaryString(a & b);
    }

    public String orOperation(long a, long b) {
        return Long.toBinaryString(a | b);
    }

    public String xorOperation(long a, long b) {
        return Long.toBinaryString(a ^ b);
    }

    // ================= ASCII =================

    public String toAsciiString(long num) {
        if (num < 0 || num > 255) return "--";
        return String.valueOf((char)num);
    }
}
