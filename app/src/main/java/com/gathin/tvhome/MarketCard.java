package com.gathin.tvhome;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * One glass card in the info strip: a small label, a large number and its unit, plus the day's change.
 * Used three times (US dollar, Emami coin, gold ounce); the accent dot keeps them distinguishable at a
 * glance without another row of text.
 */
public final class MarketCard extends LinearLayout {

    static final int ACCENT_USD = 0xFF6EE7A8;
    static final int ACCENT_COIN = 0xFFF2C66D;
    static final int ACCENT_OUNCE = 0xFFE8A45C;

    private final View dot;
    private final TextView label;
    private final TextView value;
    private final TextView unit;
    private final TextView change;

    public MarketCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_VERTICAL);
        float d = getResources().getDisplayMetrics().density;
        setPadding(Math.round(12 * d), Math.round(6 * d), Math.round(12 * d), Math.round(6 * d));
        LayoutInflater.from(context).inflate(R.layout.card_market, this, true);
        dot = findViewById(R.id.marketDot);
        label = findViewById(R.id.marketLabel);
        value = findViewById(R.id.marketValue);
        unit = findViewById(R.id.marketUnit);
        change = findViewById(R.id.marketChange);
    }

    /** dir: 1 up, -1 down, 0 unchanged (colours the change line). */
    void bind(String labelText, String valueText, String unitText, String changeText, int dir, int accent) {
        label.setText(labelText);
        value.setText(valueText);
        unit.setText(unitText);
        change.setText(changeText);
        change.setTextColor(dir > 0 ? 0xFF7BE3A5 : dir < 0 ? 0xFFFF8B8B : 0x66FFFFFF);
        dot.setBackgroundTintList(ColorStateList.valueOf(accent));
    }
}
