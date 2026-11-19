package com.gathin.tvhome;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The Tehran weather card: a drawn condition glyph, the current temperature in large light type, the
 * city and a one-line summary with today's high and low. Its glass takes a faint tint from the sky
 * outside, warm for sun, blue for rain, indigo after dark.
 */
public final class WeatherCard extends LinearLayout {

    private final float density;
    private final WeatherIconView icon;
    private final TextView cityView;
    private final TextView tempView;
    private final TextView detailView;

    public WeatherCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(Math.round(12 * density), Math.round(6 * density), Math.round(12 * density),
                Math.round(6 * density));
        LayoutInflater.from(context).inflate(R.layout.card_weather, this, true);
        icon = findViewById(R.id.weatherIcon);
        cityView = findViewById(R.id.weatherCity);
        tempView = findViewById(R.id.weatherTemp);
        detailView = findViewById(R.id.weatherDetail);
        setBackground(background(0x33243E78, 0xE60A0B13));
    }

    void bind(String city, int temp, int code, boolean day, int max, int min) {
        cityView.setText(city);
        tempView.setText(PersianText.degrees(temp));
        detailView.setText(InfoWidgets.condition(code) + "  \u00B7  "
                + PersianText.degrees(max) + " / " + PersianText.degrees(min));
        icon.setWeather(code, day);
        int[] palette = palette(code, day);
        setBackground(background(palette[0], palette[1]));
    }

    void showPlaceholder(String city, boolean loading) {
        cityView.setText(city);
        tempView.setText("\u2014");
        detailView.setText(loading ? "در حال دریافت" : "بدون اتصال");
        icon.setWeather(2, true);
    }

    private GradientDrawable background(int top, int bottom) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
        g.setCornerRadius(16 * density);
        g.setStroke(Math.max(1, Math.round(density)), 0x26FFFFFF);
        return g;
    }

    /** A faint wash of the sky's own colour, kept dark enough for white type. */
    private static int[] palette(int code, boolean day) {
        if (!day) return new int[]{0x33243E78, 0xE60A0B13};
        if (code <= 1) return new int[]{0x33E8A33C, 0xE6120F0B};
        if (code == 2) return new int[]{0x33416E96, 0xE60A0C11};
        if (code == 3) return new int[]{0x333B4653, 0xE60A0B0E};
        if (code == 45 || code == 48) return new int[]{0x334A5561, 0xE60A0B0E};
        if (code >= 51 && code <= 67 || code >= 80 && code <= 82) {
            return new int[]{0x332C5E8C, 0xE6090C12};
        }
        if (code >= 71 && code <= 77 || code == 85 || code == 86) {
            return new int[]{0x334B7392, 0xE6090C12};
        }
        if (code >= 95) return new int[]{0x333E357F, 0xE6090A12};
        return new int[]{0x33223A6B, 0xE60A0B13};
    }
}
