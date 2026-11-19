package com.gathin.tvhome;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Numbers for the info strip in Persian: Western digits become Persian ones and the separators follow
 * the Iranian convention (U+066C for thousands, U+066B for the decimal point, U+066A for percent).
 * The launcher itself stays English; only these cards are Persian.
 */
final class PersianText {

    private PersianText() {
    }

    /** 270500 -> "۲۷۰٬۵۰۰" */
    static String number(long value) {
        DecimalFormat f = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));
        return digits(f.format(value));
    }

    /** 4031.77 -> "۴٬۰۳۱٫۸" (at most one decimal, as an ounce price reads) */
    static String decimal(double value) {
        DecimalFormat f = new DecimalFormat("#,###.#", DecimalFormatSymbols.getInstance(Locale.US));
        return digits(f.format(value));
    }

    /** 0.37 -> "+۰٫۳۷٪", -0.41 -> "−۰٫۴۱٪", 0 -> "۰٪" */
    static String percent(double value) {
        DecimalFormat f = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
        String sign = value > 0.005 ? "+" : value < -0.005 ? "\u2212" : "";
        return sign + digits(f.format(Math.abs(value))) + "\u066A";
    }

    /** A temperature in Persian digits with the degree sign: 21 -> "۲۱°" */
    static String degrees(long value) {
        return number(value) + "\u00B0";
    }

    static String digits(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') out.append((char) ('\u06F0' + (c - '0')));
            else if (c == ',') out.append('\u066C');
            else if (c == '.') out.append('\u066B');
            else if (c == '%') out.append('\u066A');
            else out.append(c);
        }
        return out.toString();
    }
}
