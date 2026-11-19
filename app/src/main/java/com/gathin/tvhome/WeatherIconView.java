package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * A small weather glyph drawn with the Canvas: no bitmaps, no animation, about 40dp square. WMO weather
 * codes from Open-Meteo map to a sun, a moon, clouds, rain, snow, fog or a bolt, in the same soft
 * palette as the rest of the launcher.
 */
public final class WeatherIconView extends View {

    private static final int SUN = 0xFFFFC96B;
    private static final int MOON = 0xFFDCE6FF;
    private static final int CLOUD = 0xFFE7EDF5;
    private static final int CLOUD_BACK = 0xFFA7B4C6;
    private static final int RAIN = 0xFF7FB8F0;
    private static final int SNOW = 0xFFEAF4FF;
    private static final int BOLT = 0xFFFFD34D;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private int code = 2;
    private boolean day = true;

    public WeatherIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    void setWeather(int code, boolean day) {
        this.code = code;
        this.day = day;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        float s = Math.min(w, h);
        canvas.save();
        canvas.translate((w - s) / 2f, (h - s) / 2f);
        canvas.scale(s / 48f, s / 48f);
        drawScene(canvas);
        canvas.restore();
    }

    private void drawScene(Canvas canvas) {
        boolean night = !day;
        if (code <= 1) {
            if (night) moon(canvas, 24, 24, 11);
            else sun(canvas, 24, 24, 10);
            return;
        }
        if (code == 2) {
            if (night) moon(canvas, 17, 17, 8.5f);
            else sun(canvas, 17, 17, 8f);
            cloud(canvas, 27, 29, 0.92f);
            return;
        }
        if (code == 3) {
            cloud(canvas, 19, 25, 0.78f, CLOUD_BACK);
            cloud(canvas, 28, 31, 1f, CLOUD);
            return;
        }
        if (code == 45 || code == 48) {
            fog(canvas);
            return;
        }
        if (code >= 71 && code <= 77 || code == 85 || code == 86) {
            cloud(canvas, 24, 24, 1f, CLOUD);
            snow(canvas);
            return;
        }
        if (code >= 95) {
            cloud(canvas, 24, 23, 1f, CLOUD);
            bolt(canvas);
            return;
        }
        // Drizzle, rain and showers.
        cloud(canvas, 24, 24, 1f, CLOUD);
        rain(canvas, code >= 80);
    }

    private void sun(Canvas canvas, float cx, float cy, float r) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(SUN);
        canvas.drawCircle(cx, cy, r * 0.62f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.4f);
        paint.setColor(0xE6FFC96B);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            float x1 = cx + (float) Math.cos(a) * (r * 0.88f);
            float y1 = cy + (float) Math.sin(a) * (r * 0.88f);
            float x2 = cx + (float) Math.cos(a) * (r * 1.22f);
            float y2 = cy + (float) Math.sin(a) * (r * 1.22f);
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void moon(Canvas canvas, float cx, float cy, float r) {
        path.reset();
        path.addCircle(cx, cy, r, Path.Direction.CW);
        Path cut = new Path();
        cut.addCircle(cx + r * 0.62f, cy - r * 0.30f, r * 0.92f, Path.Direction.CW);
        path.op(cut, Path.Op.DIFFERENCE);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(MOON);
        canvas.drawPath(path, paint);
        paint.setColor(0x99FFFFFF);
        canvas.drawCircle(cx - r * 0.55f, cy + r * 0.55f, 1.1f, paint);
    }

    private void cloud(Canvas canvas, float cx, float cy, float scale) {
        cloud(canvas, cx, cy, scale, CLOUD);
    }

    private void cloud(Canvas canvas, float cx, float cy, float scale, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        float r = 7.2f * scale;
        canvas.drawCircle(cx - 8.5f * scale, cy + 1.5f * scale, r * 0.92f, paint);
        canvas.drawCircle(cx, cy - 4.2f * scale, r * 1.18f, paint);
        canvas.drawCircle(cx + 9f * scale, cy + 1f * scale, r * 0.88f, paint);
        rect.set(cx - 16.5f * scale, cy + 0.5f * scale, cx + 16.5f * scale, cy + 8.2f * scale);
        canvas.drawRoundRect(rect, 4f * scale, 4f * scale, paint);
    }

    private void rain(Canvas canvas, boolean heavy) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.1f);
        paint.setColor(RAIN);
        for (int i = 0; i < 3; i++) {
            float x = 17f + i * 7f;
            float len = heavy ? 7f : 5f;
            canvas.drawLine(x, 35f, x - 1.4f, 35f + len, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void snow(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(SNOW);
        for (int i = 0; i < 3; i++) {
            float x = 17f + i * 7f;
            float y = 38f + (i == 1 ? 2.5f : 0f);
            canvas.drawCircle(x, y, 1.7f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            canvas.drawLine(x - 2.6f, y, x + 2.6f, y, paint);
            canvas.drawLine(x, y - 2.6f, x, y + 2.6f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void fog(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.6f);
        paint.setColor(0xCCD8E2EE);
        canvas.drawLine(11, 20, 37, 20, paint);
        canvas.drawLine(8, 27, 34, 27, paint);
        canvas.drawLine(14, 34, 40, 34, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void bolt(Canvas canvas) {
        path.reset();
        path.moveTo(26, 32);
        path.lineTo(19.5f, 42);
        path.lineTo(24.5f, 42);
        path.lineTo(22, 48);
        path.lineTo(30.5f, 38.5f);
        path.lineTo(25.5f, 38.5f);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(BOLT);
        canvas.drawPath(path, paint);
    }
}
