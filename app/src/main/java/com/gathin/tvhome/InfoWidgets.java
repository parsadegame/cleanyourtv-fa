package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/**
 * The two live cards under the header: Tehran weather from Open-Meteo and the TGJU market row (US
 * dollar, Emami gold coin, gold ounce).
 *
 * <p>Everything is cached in SharedPreferences, so Home paints the last values at boot and while the
 * network is down. Fetches run on one background thread, only while Home is resumed, at most once per
 * {@code info_refresh_minutes} (10 by default); a failure retries after two minutes. Nothing here
 * touches the UI off the main thread.
 */
final class InfoWidgets {

    private static final String TAG = "TvHome.Info";
    private static final String PREFS = "info_widgets";
    private static final String KEY_MARKET = "market";
    private static final String KEY_WEATHER = "weather";
    private static final long MINUTE = 60_000L;
    private static final long RETRY = 2 * MINUTE;
    private static final long RETRY_GAP = 30_000L;

    /** TGJU publishes the same feed on several hosts; the first that answers wins. */
    private static final String[] TGJU_URLS = {
            "https://call1.tgju.org/ajax.json",
            "https://call2.tgju.org/ajax.json",
            "https://call3.tgju.org/ajax.json"
    };

    private final Activity activity;
    private final Handler handler;
    private final SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "info-widgets");
        t.setDaemon(true);
        return t;
    });

    private final WeatherCard weatherCard;
    private final MarketCard usdCard;
    private final MarketCard coinCard;
    private final MarketCard ounceCard;

    private final int refreshMinutes;
    private final String city;
    private final double latitude;
    private final double longitude;

    private boolean started;
    private volatile boolean marketLoading;
    private volatile boolean weatherLoading;
    private boolean marketOk;
    private boolean weatherOk;
    /** When the values on screen were fetched (0: nothing cached yet). */
    private long marketAt;
    private long weatherAt;
    /** When a fetch was last started, so a failing endpoint is not hammered every minute. */
    private long marketTry;
    private long weatherTry;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!started) return;
            refreshIfStale();
            handler.postDelayed(this, MINUTE);
        }
    };

    InfoWidgets(Activity activity, Handler handler) {
        this.activity = activity;
        this.handler = handler;
        this.prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        this.weatherCard = activity.findViewById(R.id.weatherCard);
        this.usdCard = activity.findViewById(R.id.cardUsd);
        this.coinCard = activity.findViewById(R.id.cardCoin);
        this.ounceCard = activity.findViewById(R.id.cardOunce);
        this.refreshMinutes = Math.max(1, activity.getResources().getInteger(R.integer.info_refresh_minutes));
        this.city = activity.getResources().getString(R.string.weather_city);
        this.latitude = parseDouble(activity.getResources().getString(R.string.weather_latitude), 35.6892);
        this.longitude = parseDouble(activity.getResources().getString(R.string.weather_longitude), 51.3890);

        usdCard.bind("دلار آمریکا", "\u2014", "در حال دریافت", "", 0, MarketCard.ACCENT_USD);
        coinCard.bind("سکه امامی", "\u2014", "در حال دریافت", "", 0, MarketCard.ACCENT_COIN);
        ounceCard.bind("انس طلا", "\u2014", "در حال دریافت", "", 0, MarketCard.ACCENT_OUNCE);
        weatherCard.showPlaceholder(city, true);
        loadCache();
    }

    void start() {
        if (started) return;
        started = true;
        refreshIfStale();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, MINUTE);
    }

    void stop() {
        started = false;
        handler.removeCallbacks(tick);
    }

    void shutdown() {
        stop();
        io.shutdownNow();
    }

    private void loadCache() {
        String market = prefs.getString(KEY_MARKET, null);
        if (market != null) {
            try {
                applyMarket(new JSONObject(market));
            } catch (Exception e) {
                Log.w(TAG, "cached market unreadable: " + e);
            }
        }
        String weather = prefs.getString(KEY_WEATHER, null);
        if (weather != null) {
            try {
                applyWeather(new JSONObject(weather));
            } catch (Exception e) {
                Log.w(TAG, "cached weather unreadable: " + e);
            }
        }
    }

    private void refreshIfStale() {
        long now = System.currentTimeMillis();
        long marketWait = marketOk ? refreshMinutes * MINUTE : RETRY;
        if (now - marketAt >= marketWait && now - marketTry >= RETRY_GAP) fetchMarket();
        long weatherWait = weatherOk ? Math.max(15, refreshMinutes) * MINUTE : RETRY;
        if (now - weatherAt >= weatherWait && now - weatherTry >= RETRY_GAP) fetchWeather();
    }

    // ---------------------------------------------------------------- market

    private void fetchMarket() {
        if (marketLoading) return;
        marketLoading = true;
        marketTry = System.currentTimeMillis();
        io.execute(() -> {
            JSONObject result = null;
            for (String url : TGJU_URLS) {
                try {
                    result = parseMarket(http(url));
                    if (result != null) break;
                } catch (Exception e) {
                    Log.w(TAG, "market " + url + ": " + e);
                }
            }
            final JSONObject fetched = result;
            handler.post(() -> {
                marketLoading = false;
                if (fetched == null) {
                    marketOk = false;
                    return;
                }
                marketOk = true;
                prefs.edit().putString(KEY_MARKET, fetched.toString()).apply();
                applyMarket(fetched);
            });
        });
    }

    private void applyMarket(JSONObject o) {
        marketAt = o.optLong("at", 0);
        long usd = Math.round(o.optDouble("usd", 0));
        long coin = Math.round(o.optDouble("coin", 0));
        double ounce = o.optDouble("oz", 0);
        double usdPct = o.optDouble("usdPct", 0);
        double coinPct = o.optDouble("coinPct", 0);
        double ouncePct = o.optDouble("ozPct", 0);
        if (usd <= 0 || coin <= 0 || ounce <= 0) return;

        usdCard.bind("دلار آمریکا", PersianText.number(usd), "تومان",
                PersianText.percent(usdPct), direction(usdPct), MarketCard.ACCENT_USD);
        coinCard.bind("سکه امامی", PersianText.number(coin), "تومان",
                PersianText.percent(coinPct), direction(coinPct), MarketCard.ACCENT_COIN);
        ounceCard.bind("انس طلا", "$" + PersianText.decimal(ounce), "دلار",
                PersianText.percent(ouncePct), direction(ouncePct), MarketCard.ACCENT_OUNCE);
    }

    /**
     * TGJU's {@code p} is in rial for the Iranian quotes; Iranians read prices in toman, so the cards
     * divide by ten. The ounce is quoted in dollars and stays as it is.
     */
    private static JSONObject parseMarket(String body) throws Exception {
        JSONObject current = new JSONObject(body).getJSONObject("current");
        JSONObject usd = current.optJSONObject("price_dollar_rl");
        JSONObject coin = current.optJSONObject("sekee");
        JSONObject ounce = current.optJSONObject("ons");
        if (usd == null || coin == null || ounce == null) return null;
        double usdRial = num(usd.optString("p"));
        double coinRial = num(coin.optString("p"));
        double ounceUsd = num(ounce.optString("p"));
        if (usdRial <= 0 || coinRial <= 0 || ounceUsd <= 0) return null;

        JSONObject o = new JSONObject();
        o.put("usd", Math.round(usdRial / 10.0));
        o.put("usdPct", signedPercent(usd));
        o.put("coin", Math.round(coinRial / 10.0));
        o.put("coinPct", signedPercent(coin));
        o.put("oz", ounceUsd);
        o.put("ozPct", signedPercent(ounce));
        o.put("at", System.currentTimeMillis());
        return o;
    }

    private static double signedPercent(JSONObject quote) {
        double change = num(quote.optString("d"));
        double percent = Math.abs(num(quote.optString("dp")));
        if (change < 0) return -percent;
        if (change > 0) return percent;
        return 0;
    }

    private static double num(String text) {
        if (text == null) return 0;
        String clean = text.replace(",", "").replace("\u066C", "").replace("\u066B", ".")
                .replace(" ", "").trim();
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int direction(double percent) {
        return percent > 0.005 ? 1 : percent < -0.005 ? -1 : 0;
    }

    // --------------------------------------------------------------- weather

    private void fetchWeather() {
        if (weatherLoading) return;
        weatherLoading = true;
        weatherTry = System.currentTimeMillis();
        final String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,is_day,weather_code"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                + "&timezone=Asia%2FTehran&forecast_days=2";
        io.execute(() -> {
            JSONObject result = null;
            try {
                result = parseWeather(http(url));
            } catch (Exception e) {
                Log.w(TAG, "weather: " + e);
            }
            final JSONObject fetched = result;
            handler.post(() -> {
                weatherLoading = false;
                if (fetched == null) {
                    weatherOk = false;
                    return;
                }
                weatherOk = true;
                prefs.edit().putString(KEY_WEATHER, fetched.toString()).apply();
                applyWeather(fetched);
            });
        });
    }

    private void applyWeather(JSONObject o) {
        weatherAt = o.optLong("at", 0);
        int temp = (int) Math.round(o.optDouble("t", 0));
        int code = o.optInt("code", -1);
        boolean day = o.optBoolean("day", true);
        int max = (int) Math.round(o.optDouble("max", temp));
        int min = (int) Math.round(o.optDouble("min", temp));
        weatherCard.bind(city, temp, code, day, max, min);
    }

    private static JSONObject parseWeather(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject current = root.getJSONObject("current");
        JSONObject daily = root.getJSONObject("daily");
        JSONArray max = daily.getJSONArray("temperature_2m_max");
        JSONArray min = daily.getJSONArray("temperature_2m_min");
        JSONObject o = new JSONObject();
        o.put("t", current.getDouble("temperature_2m"));
        o.put("code", current.optInt("weather_code", -1));
        o.put("day", current.optInt("is_day", 1) == 1);
        o.put("max", max.getDouble(0));
        o.put("min", min.getDouble(0));
        o.put("at", System.currentTimeMillis());
        return o;
    }

    // ------------------------------------------------------------------ http

    private static String http(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 9; BRAVIA) TvHome/4.0");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            int status = connection.getResponseCode();
            InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (in == null) throw new IOException("HTTP " + status);
            if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) in = new GZIPInputStream(in);
            try (InputStream stream = in; ByteArrayOutputStream out = new ByteArrayOutputStream(16384)) {
                byte[] buffer = new byte[8192];
                int n;
                int total = 0;
                while ((n = stream.read(buffer)) > 0) {
                    total += n;
                    if (total > 3_000_000) throw new IOException("response too large");
                    out.write(buffer, 0, n);
                }
                return out.toString("UTF-8");
            }
        } finally {
            connection.disconnect();
        }
    }

    // ---------------------------------------------------------------- labels

    /** WMO weather code in Persian, the short form that fits one line. */
    static String condition(int code) {
        if (code == 0) return "صاف";
        if (code == 1) return "عمدتاً صاف";
        if (code == 2) return "نیمه‌ابری";
        if (code == 3) return "ابری";
        if (code == 45 || code == 48) return "مه‌آلود";
        if (code >= 51 && code <= 57) return "باران‌ریز";
        if (code >= 61 && code <= 67) return "بارانی";
        if (code >= 71 && code <= 77) return "برفی";
        if (code >= 80 && code <= 82) return "رگبار";
        if (code == 85 || code == 86) return "رگبار برف";
        if (code >= 95) return "رعدوبرق";
        return "\u2014";
    }

    private static double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
